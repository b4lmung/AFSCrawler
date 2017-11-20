/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;

import com.job.ic.crawlers.daos.ARCFileWriter;
import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.daos.WebsiteSegmentDb;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.SegmentFrontier;
import com.job.ic.crawlers.models.SegmentQueueModel;
import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.crawlers.sites.Crawler;
import com.job.ic.extraction.FeaturesExtraction;
import com.job.ic.ml.classifiers.AccuracyTracker;
import com.job.ic.ml.classifiers.NeighborhoodPredictor;
import com.job.ic.ml.classifiers.FeaturesCollectors;
import com.job.ic.ml.classifiers.HistoryPredictor;
import com.job.ic.ml.classifiers.PageClassifier;
import com.job.ic.ml.classifiers.PredictorPool;
import com.job.ic.ml.classifiers.PredictorPoolMulti;
import com.job.ic.nlp.services.Checker;
import com.job.ic.nlp.services.LexToChecker;
import com.job.ic.proxy.ProxyService;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.Status;

import net.htmlparser.jericho.LoggerProvider;

public class HttpCrawler {

	private static Logger logger = Logger.getLogger(HttpCrawler.class);

	private PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager();
	private CloseableHttpClient client;
	private ARCFileWriter writer;
	private SegmentFrontier queue = new SegmentFrontier();
	private Checker checker;

	private boolean useLocalProxy = false;
	protected boolean isMulti = false;

	public static void main(String[] args) {
		HttpCrawler c = new HttpCrawler();
		c.start();
	}

	public CloseableHttpClient getHttpClient() {
		return this.client;
	}

	public HttpCrawler(boolean isMulti) {
		this.isMulti = isMulti;

		try {
			initialize();
		} catch (Exception e) {
			System.exit(1);
		}
	}

	public HttpCrawler() {
		this.isMulti = false;
		try {
			initialize();
		} catch (Exception e) {
			System.exit(1);
		}
	}

	public void start() {
		ThreadPoolExecutor sv = (ThreadPoolExecutor) Executors.newFixedThreadPool(CrawlerConfig.getConfig().getNumThreads());
		logger.info("start crawling " + queue.size());
		long remain;
		Status.setLastDLTime(System.currentTimeMillis());
		int previous = -1;
		while (true) {

			if (CrawlerConfig.getConfig().getLimitDownloadedPages() > 0 && Status.getTotalPage() > CrawlerConfig.getConfig().getLimitDownloadedPages())
				break;

			remain = sv.getTaskCount() - sv.getCompletedTaskCount();

			if (FeaturesCollectors.getCountFin() != 0 && FeaturesCollectors.getCountFin() % Math.abs(CrawlerConfig.getConfig().getUpdateInterval()) == 0 && FeaturesCollectors.getCountFin() != previous) {
				previous = FeaturesCollectors.getCountFin();
				while (remain != 0) {
					try {
						Thread.sleep(10000);
						logger.info("remain : " + remain);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					remain = sv.getTaskCount() - sv.getCompletedTaskCount();

				}

				if (this.isMulti)
					onlineUpdateMulti();
				else
					onlineUpdateSingle();
			}

			if (queue.size() > 0) {

				if (Status.getLastDLTime() != 0 && TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - Status.getLastDLTime()) >= 90) {
					((ThreadPoolExecutor) sv).getQueue().clear();

					sv.shutdownNow();
					mgr.closeIdleConnections(CrawlerConfig.getConfig().getConnMgrTimeout(), TimeUnit.MILLISECONDS);
					mgr.closeExpiredConnections();
//					logger.info("crawler is not responsed for 90 mins, program will exit now");
//					break;
				}

				if (remain > CrawlerConfig.getConfig().getNumThreads() * 2) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}

					mgr.closeIdleConnections(CrawlerConfig.getConfig().getConnMgrTimeout(), TimeUnit.MILLISECONDS);
					mgr.closeExpiredConnections();
					continue;
				}

				sv.execute(new HttpSegmentCrawler(this, queue, checker, writer, this.isMulti));

			} else if (queue.size() == 0) {

				if (Status.getLastDLTime() != 0 && TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - Status.getLastDLTime()) >= 3) {
					((ThreadPoolExecutor) sv).getQueue().clear();

					sv.shutdownNow();
					mgr.closeIdleConnections(CrawlerConfig.getConfig().getConnMgrTimeout(), TimeUnit.MILLISECONDS);
					mgr.closeExpiredConnections();
					logger.info("no new segments in queue for more than 3 mins, program will exit now");
					break;
				}

				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}

				logger.info("Queue size:" + queue.size());

			}

		}

		sv.shutdownNow();

		try {
			sv.awaitTermination(3, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		cleanUp();

	}

	private void cleanUp() {
		
		logger.info("cleaning up");
		FeaturesCollectors.exportPredictionResult("logs/prediction_result.txt");

		if (writer != null)
			writer.close();

		if (isMulti)
			onlineUpdateMulti();
		else
			onlineUpdateSingle();

		FileUtils.deleteFile("tmp.arff");
		FileUtils.deleteFile("tmp2.arff");
		FileUtils.deleteFile("tmp3.arff");
		FileUtils.deleteFile("features.arff");
		FileUtils.deleteFile("features_relSrc.arff");
		FileUtils.deleteFile("features_nonSrc.arff");

		logger.info("finished");
		UrlDb.close();
		// WebsiteSegmentDb.close();
		PredictorPool.shutdown();
		PredictorPoolMulti.shutdown();
		
		System.exit(0);
	}

	private void onlineUpdateSingle() {
		NeighborhoodPredictor.onlineUpdate();
		HistoryPredictor.onlineUpdate();
		
		
		FeaturesCollectors.backupAllFeatures("logs/all.arff");
		FeaturesCollectors.backupAllFeaturesWithoutFilter("logs/all_not_filtered.arff");

		logger.info("use duplicate data -->" + CrawlerConfig.getConfig().useDuplicateData());
		ArrayList<String> newTrainingSet = FeaturesCollectors.createDatasetForSinglePredictor(true);

		if (newTrainingSet == null)
			return;

		logger.info("DS Rel Size : " + newTrainingSet.size());
		FileUtils.writeTextFile("logs/features_update.arff", newTrainingSet, true);
		FileUtils.writeTextFile("logs/features_update.arff", "--------------------------------------------------------------------------------\n", true);
		// FileUtils.copyFile("features.arff", "logs/features.arff");

		logger.info("Link weight: " + PredictorPool.getLinkConfusionMatrix().toString());
		logger.info("Anchor weight: " + PredictorPool.getAnchorConfusionMatrix().toString());
		logger.info("Url weight: " + PredictorPool.getUrlConfusionMatrix().toString());
		logger.info("History weight: " + AccuracyTracker.getHistoryConfusionMatrix().toString());
		logger.info("segment predictor precision: " + PredictorPool.getSegmentPredictorConfusionMatrix().toString());

		if (CrawlerConfig.getConfig().getUpdateInterval() <= 0) {
			logger.info("no-update");
			return;
		}

		try {
			PredictorPool.onlineUpdatePredictor(newTrainingSet);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void onlineUpdateMulti() {
		NeighborhoodPredictor.onlineUpdate();
		HistoryPredictor.onlineUpdate();
		
		FeaturesCollectors.backupAllFeatures("logs/all.arff");
		FeaturesCollectors.backupAllFeaturesWithoutFilter("logs/all_not_filtered.arff");

		// srcThai
		ArrayList<String> newDataSetFromRelSrc = FeaturesCollectors.createRelDatasetForMultiPredictor(true);

		// srcNon
		ArrayList<String> newDataSetFromNonSrc = FeaturesCollectors.createNonDatasetForMultiPredictor(true);

		if (newDataSetFromRelSrc != null) {
			logger.info("DS Rel Size : " + newDataSetFromRelSrc.size());
			FileUtils.writeTextFile("logs/features_relSrc_update.arff", newDataSetFromRelSrc, true);
			FileUtils.writeTextFile("logs/features_relSrc_update.arff", "--------------------------------------------------------------------------------\n", true);
		}

		if (newDataSetFromNonSrc != null) {
			logger.info("DS Non Size : " + newDataSetFromNonSrc.size());
			FileUtils.writeTextFile("logs/features_nonSrc_update.arff", newDataSetFromNonSrc, true);
			FileUtils.writeTextFile("logs/features_nonSrc_update.arff", "--------------------------------------------------------------------------------\n", true);
		}

		if (newDataSetFromRelSrc == null && newDataSetFromNonSrc == null)
			return;

		logger.info("Link weight: " + AccuracyTracker.getLinkRelConfusionMatrix().toString() + "\t|\t" + AccuracyTracker.getLinkNonConfusionMatrix().toString());
		logger.info("Anchor weight: " + AccuracyTracker.getAnchorRelConfusionMatrix().toString() + "\t|\t" + AccuracyTracker.getAnchorNonConfusionMatrix().toString());
		logger.info("Url weight: " + AccuracyTracker.getUrlRelConfusionMatrix().toString() + "\t|\t" + AccuracyTracker.getUrlNonConfusionMatrix().toString());
		logger.info("History weight: " + AccuracyTracker.getHistoryConfusionMatrix().toString());

		logger.info(
				"segment predictor precision: " + AccuracyTracker.getRelSegmentPredictorConfusionMatrix().toString() + "\t|\t" + AccuracyTracker.getNonSegmentPredictorConfusionMatrix().toString());

		if (CrawlerConfig.getConfig().getUpdateInterval() <= 0)
			return;

		try {
			PredictorPoolMulti.onlineUpdatePredictor(newDataSetFromRelSrc, newDataSetFromNonSrc);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// FileUtils.copyFile("features_relSrc.arff",
		// "logs/features_relSrc.arff");
		// FileUtils.copyFile("features_nonSrc.arff",
		// "logs/features_nonSrc.arff");

	}

	private void initialize() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		Locale.setDefault(Locale.US);
		System.setProperty("file.encoding", "UTF-8");
		java.security.Security.setProperty("networkaddress.cache.ttl", "-1");

		// CrawlerConfig.loadConfig("crawler.conf");
		net.htmlparser.jericho.Config.LoggerProvider = LoggerProvider.DISABLED;

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				logger.warn("Recieved Shutdown signal (CTRL + C) ");
				Runtime.getRuntime().halt(0);
			}
		});

		logger.info("preparing proxy / http connection pool");
		if (CrawlerConfig.getConfig().getLocalProxyPath().trim().length() != 0) {
			// use proxy
			logger.info("loading proxy");
			ProxyService.setupProxy(CrawlerConfig.getConfig().getLocalProxyPath());
			useLocalProxy = true;
			logger.info("finished loading proxy");

		} else {

			RequestConfig config;

			if (CrawlerConfig.getConfig().getProxyServer().trim().length() != 0) {
				// set proxy
				HttpHost proxy = new HttpHost(CrawlerConfig.getConfig().getProxyServer(), CrawlerConfig.getConfig().getProxyPort());
				config = RequestConfig.custom().setStaleConnectionCheckEnabled(false).setProxy(proxy).setSocketTimeout((int) CrawlerConfig.getConfig().getSoTimeout())
						.setConnectTimeout((int) CrawlerConfig.getConfig().getTimeout()).build();
			} else {
				config = RequestConfig.custom().setStaleConnectionCheckEnabled(false).setSocketTimeout((int) CrawlerConfig.getConfig().getSoTimeout())
						.setConnectTimeout((int) CrawlerConfig.getConfig().getTimeout()).build();
			}

			// handle ssl connection
			if (CrawlerConfig.getConfig().isAllowHttps()) {
				SSLContextBuilder builder = SSLContexts.custom();
				builder.loadTrustMaterial(null, (chain, authType) -> true);
				SSLContext sslContext = builder.build();
				SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {

					@Override
					public boolean verify(String arg0, SSLSession arg1) {
						return true;
					}

					@Override
					public void verify(String arg0, String[] arg1, String[] arg2) throws SSLException {
					}

					@Override
					public void verify(String arg0, X509Certificate arg1) throws SSLException {
					}

					@Override
					public void verify(String arg0, SSLSocket arg1) throws IOException {
					}
				});

				Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("https", sslsf)
						.register("http", new PlainConnectionSocketFactory()).build();
				mgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

			}

			mgr.setMaxTotal(300);
			mgr.setDefaultMaxPerRoute(10);
			client = HttpClients.custom().setUserAgent(CrawlerConfig.getConfig().getUsrAgent()).setConnectionManager(mgr).setDefaultRequestConfig(config).build();

		}

		logger.info("preparing urldb");
		FileUtils.mkdir("urlDb");
		UrlDb.createEnvironment("urlDb");
		WebsiteSegmentDb.createEnvironment();

		logger.info("preparing web page classifier");

		checker = null;
		if (!useLocalProxy && CrawlerConfig.getConfig().getCheckerType() != null && CrawlerConfig.getConfig().getCheckerType().trim().length() > 0) {

			if (CrawlerConfig.getConfig().getCheckerType().equals("weka")) {
				try {
					checker = new PageClassifier();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			} else {
				checker = new LexToChecker();
			}

		}

		logger.info("preparing arc file writer");
		try {
			writer = new ARCFileWriter();
		} catch (Exception e) {
			e.printStackTrace();
		}

		logger.info("importing queue from " + CrawlerConfig.getConfig().getSeedPath());
		queue.enqueueSeeds(CrawlerConfig.getConfig().getSeedPath());

		String modelDir = "";
		if (CrawlerConfig.getConfig().getPredictorTrainingPath() != null) {
			File file = new File(CrawlerConfig.getConfig().getPredictorTrainingPath());
			String fullPath = file.getAbsolutePath();

			file = new File(fullPath);

			if (file.getParentFile() != null) {
				modelDir = file.getParentFile().getAbsolutePath();
			}
		}

		if (modelDir != null)
			modelDir += "/";

		logger.info("Model dir: " + modelDir);

		logger.info("training the predictor");

		if(CrawlerConfig.getConfig().isTrainingMode()) {
			NeighborhoodPredictor.trainNeighborhoodPredictor(null);
			HistoryPredictor.trainHistoryPredictor(null);
		}
		else {
			NeighborhoodPredictor.trainNeighborhoodPredictor("n" + CrawlerConfig.getConfig().getPredictorTrainingPath());
			
			HistoryPredictor.trainHistoryPredictor("h" + CrawlerConfig.getConfig().getPredictorTrainingPath());
		}
		
		
		if (CrawlerConfig.getConfig().isTrainingMode()) {
			String[] tmp = FileUtils.readResourceFile("/resources/classifiers/online_initial.arff");
			FileUtils.writeTextFile("init.arff", tmp, false);

			if (this.isMulti)
				PredictorPoolMulti.trainPredictor("init.arff", "init.arff");
			else
				PredictorPool.trainPredictor("init.arff");
		} else {

			String filename = CrawlerConfig.getConfig().getPredictorTrainingPath();
			if (filename.trim().equals("")) {
				logger.error("No input training path");
				System.exit(1);
			}

			if (this.isMulti) {
				filename = filename.substring(0, filename.lastIndexOf("."));
				PredictorPoolMulti.trainPredictor(filename + "_rel.arff", filename + "_non.arff");
			} else {
				PredictorPool.trainPredictor(CrawlerConfig.getConfig().getPredictorTrainingPath());
			}

		}

		FileUtils.writeTextFile("logs/all.arff", FeaturesExtraction.getHeader(), false);
		FileUtils.writeTextFile("logs/all_not_filtered.arff", FeaturesExtraction.getHeader(), false);

		if (this.isMulti) {
			FileUtils.writeTextFile("logs/features_relSrc_update.arff", FeaturesExtraction.getHeader(), false);
			FileUtils.writeTextFile("logs/features_nonSrc_update.arff", FeaturesExtraction.getHeader(), false);
		} else {
			FileUtils.writeTextFile("logs/features_update.arff", FeaturesExtraction.getHeader(), false);
		}

	}
}
