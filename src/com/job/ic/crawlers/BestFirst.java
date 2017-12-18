package com.job.ic.crawlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.PriorityQueue;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
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
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.job.ic.crawlers.daos.ARCFileWriter;
import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.models.PageObject;
import com.job.ic.crawlers.models.Queue;
import com.job.ic.crawlers.models.QueueObj;
import com.job.ic.crawlers.models.QueueProxyObj;
import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.ml.classifiers.ClassifierOutput;
import com.job.ic.ml.classifiers.HistoryPredictor;
import com.job.ic.ml.classifiers.NeighborhoodPredictor;
import com.job.ic.ml.classifiers.PageClassifier;
import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.nlp.services.Checker;
import com.job.ic.nlp.services.LexToChecker;
import com.job.ic.proxy.ProxyService;
import com.job.ic.proxy.model.ProxyModel;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.Status;

import net.htmlparser.jericho.LoggerProvider;

public class BestFirst {
	private static Logger logger = Logger.getLogger(BestFirst.class);
	public static int countPage = 0;

	private static double rel = 0;
	private static double non = 0;

	public static String seedPath = CrawlerConfig.getConfig().getSeedPath();

	public static boolean isProxy = false;
	public static boolean isBFS = false;
	// public static UrlDAO ud;
	private static long limitPage = CrawlerConfig.getConfig().getLimitDownloadedPages();
	private static Checker page;

	public static PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager();
	public static CloseableHttpClient client;

	public static synchronized void addPage(final double score, final String url) {

		if (score > 0.5) {
			rel++;
		} else {
			non++;
		}

		if (BestFirst.rel + BestFirst.non > BestFirst.limitPage) {
			System.out.println(BestFirst.rel);
			System.out.println(BestFirst.non);
			System.out.println(BestFirst.limitPage);

			System.exit(0);
		}

	}

	public static String progress() {
		double hv = rel / (rel + non);
		// double total = rel + non;
		hv *= 100;

		return String.format("%d\t%.3f", (int) (rel + non), hv);
	}

	public static void main(final String[] args) throws Exception {

		if (args.length > 0 && args[0].equals("true"))
			isBFS = true;
		else
			isBFS = false;

		net.htmlparser.jericho.Config.LoggerProvider = LoggerProvider.DISABLED;

		if (CrawlerConfig.getConfig().getLocalProxyPath() != null
				&& !CrawlerConfig.getConfig().getLocalProxyPath().trim().equals("")) {
			ProxyService.setupProxy(CrawlerConfig.getConfig().getLocalProxyPath());
			isProxy = true;

			limitPage = Integer.MAX_VALUE;
		}

		RequestConfig config;

		if (CrawlerConfig.getConfig().getProxyServer().trim().length() != 0) {
			// set proxy
			HttpHost proxy = new HttpHost(CrawlerConfig.getConfig().getProxyServer(),
					CrawlerConfig.getConfig().getProxyPort());
			config = RequestConfig.custom().setStaleConnectionCheckEnabled(false).setProxy(proxy)
					.setSocketTimeout((int) CrawlerConfig.getConfig().getSoTimeout())
					.setConnectTimeout((int) CrawlerConfig.getConfig().getTimeout()).build();
		} else {
			config = RequestConfig.custom().setStaleConnectionCheckEnabled(false)
					.setSocketTimeout((int) CrawlerConfig.getConfig().getSoTimeout())
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

			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("https", sslsf).register("http", new PlainConnectionSocketFactory()).build();
			mgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

		}

		mgr.setMaxTotal(300);
		mgr.setDefaultMaxPerRoute(10);
		client = HttpClients.custom().setUserAgent(CrawlerConfig.getConfig().getUsrAgent()).setConnectionManager(mgr)
				.setDefaultRequestConfig(config).build();

		page = null;
		if (CrawlerConfig.getConfig().getLocalProxyPath() == null
				|| CrawlerConfig.getConfig().getLocalProxyPath().equals(""))
			if (CrawlerConfig.getConfig().getCheckerType() != null
					&& CrawlerConfig.getConfig().getCheckerType().trim().length() > 0) {

				if (CrawlerConfig.getConfig().getCheckerType().equals("weka")) {
					try {
						page = new PageClassifier();
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(1);
					}
				} else {
					page = new LexToChecker();
				}

			}

		if (CrawlerConfig.getConfig().isTrainingMode()) {
			NeighborhoodPredictor.trainNeighborhoodPredictor(null);
			HistoryPredictor.trainHistoryPredictor(null);
		} else {
			System.out.println("train");
			NeighborhoodPredictor.trainNeighborhoodPredictor(
					"n" + CrawlerConfig.getConfig().getPredictorTrainingPath().replace(".arff", "-bf.arff"));
			HistoryPredictor.trainHistoryPredictor(
					"h" + CrawlerConfig.getConfig().getPredictorTrainingPath().replace(".arff", "-bf.arff"));

		}

		softFocusedThread();

		System.out.println(String.valueOf(BestFirst.rel) + "\t" + BestFirst.non);
		NeighborhoodPredictor.backupAllNeighborhoodData("logs/neighborhood.arff");
		HistoryPredictor.backupAllHistoryData("logs/history.arff");
	}

	public static void softFocusedThread() {

		HashMap<String, Integer> visited = new HashMap<>();
		UrlDb.createEnvironment("urlDb");
		ARCFileWriter writer = null;
		try {
			writer = new ARCFileWriter();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Locale.setDefault(Locale.US);
		System.setProperty("file.encoding", "UTF-8");

		Queue queue = new Queue();

		ArrayList<WebsiteSegment> segments = FileUtils.readSegmentFile(seedPath);
		System.out.println(segments.size());
		for (int i = 0; i < segments.size(); i++) {
			WebsiteSegment s = segments.get(i);
			for (String url : s.getUrls()) {
				queue.enQueue(new QueueObj(url, "", 100, -1, (segments.size() - 1) * 100.0));
			}
		}

		logger.info("start " + queue.size());

		String host = null;
		HtmlParser ps = new HtmlParser();
		int t = 0;

		while (queue.size() > 0) {

			QueueObj qObj = queue.deQueue();

			if (qObj == null)
				continue;

			String url = qObj.getUrl();

			if (url == null)
				continue;

			double score = 0.0;

			ArrayList<LinksModel> links = null;

			if (!isProxy) {
				byte[] p = download(url);

				if (p == null)
					continue;

				if (writer != null)
					writer.writeRecord(new PageObject(p, url));

				links = ps.parse(p, url);
				score = page.checkHtmlContent(p);
			} else {

				ProxyModel m = ProxyService.retreiveContentByURL(url, UrlDb.getUrlDAO());
				if (m == null)
					continue;

				links = m.getLinks();
				score = m.getScore();

			}

			if (url.toLowerCase().contains("moscow"))
				score = 0;

			if (url.toLowerCase().contains("canada"))
				score = 0;

			if (url.toLowerCase().contains("florida"))
				score = 0;

			if (url.toLowerCase().contains("byowner."))
				score = 0;

			if (url.toLowerCase().contains("kiev"))
				score = 0;

			if (url.toLowerCase().contains("whistlerforsale"))
				score = 0;

			if (url.toLowerCase().contains("hellenic-realty"))
				score = 0;

			if (url.toLowerCase().contains("kiev"))
				score = 0;

			if (url.toLowerCase().contains("whistlerforsale"))
				score = 0;

			if (url.toLowerCase().contains("whistler"))
				score = 0;

			if (url.toLowerCase().contains("landntours"))
				score = 0;

			if (url.toLowerCase().contains("intlistings.com"))
				score = 0;

			if (url.toLowerCase().contains("united-states"))
				score = 0;

			if (url.toLowerCase().contains("texas"))
				score = 0;

			// if (url.toLowerCase().contains("anantara."))
			// score = 0;

			if (url.toLowerCase().contains("justrealestate"))
				score = 0;

			String seg = HttpUtils.getBasePath(url);
			ArrayList<ClassifierOutput> neighborPredictions = new ArrayList<>();
			ArrayList<ClassifierOutput> historyPredictions = new ArrayList<>();

			if (qObj.getNeighborPrediction() != null)
				neighborPredictions.add(qObj.getNeighborPrediction());

			if (qObj.getHistoryPrediction() != null) {
				historyPredictions.add(qObj.getHistoryPrediction());
			}

			if (score > 0.5) {
				NeighborhoodPredictor.record(seg, neighborPredictions, 1, 0);
				HistoryPredictor.record(seg, historyPredictions, 1, 0);
			} else {
				NeighborhoodPredictor.record(seg, neighborPredictions, 0, 1);
				HistoryPredictor.record(seg, historyPredictions, 0, 1);
			}

			if ((int) (rel + non) % 50 == 0) {

				if (CrawlerConfig.getConfig().useNeighborhoodPredictor())
					NeighborhoodPredictor.onlineUpdate();

				if (CrawlerConfig.getConfig().useHistoryPredictor())
					HistoryPredictor.onlineUpdate();

			}

			host = HttpUtils.getHost(url);
			if (host != null) {
				if (!visited.containsKey(host)) {
					visited.put(host, 1);
				}

				visited.put(host, visited.get(host) + 1);
			}

			addPage(score, url);

			logger.info(String.format(
					"DOWNLOADED\t%.2f\tDepth:%d\tDistance:%d\t%s\t%d\tHV:\t%s\tqScore:\t%.3f\tpScore:%.3f\tsrcScore:%.3f\tqSize:\t%d",
					score, qObj.getDepth(), 0, url, (int) (rel + non), progress(), qObj.getScore(), 0.0, 0.0,
					queue.size()));

			if (links == null)
				continue;

			for (final LinksModel l : links) {

				if (UrlDb.getUrlDAO().checkAndAddUrl(l.getLinkUrl(), false))
					continue;

				if (BestFirst.isBFS) {
					queue.enQueue(new QueueObj(l.getLinkUrl(), url, qObj.getDepth() + 1, 0, 0));
				} else {

					double linkscore = score;
					ClassifierOutput resultN = null, resultH = null;

					String h = HttpUtils.getHost(url);
					if (h != null) {
						if (!visited.containsKey(h)) {
							continue;
						}

						String targetSeg = HttpUtils.getBasePath(url);

						if (CrawlerConfig.getConfig().useNeighborhoodPredictor()) {
							resultN = NeighborhoodPredictor.predict(targetSeg);
						}

						if (CrawlerConfig.getConfig().useHistoryPredictor()) {
							resultH = HistoryPredictor.predict(targetSeg);
						}

						if (visited.get(h) == 0) {
							if (CrawlerConfig.getConfig().useNeighborhoodPredictor())
								linkscore += 0.5;

							if (CrawlerConfig.getConfig().useHistoryPredictor())
								linkscore += 0.5;
						} else {
							if (resultN != null)
								linkscore += resultN.getRelevantScore();

							if (resultH != null)
								linkscore += resultH.getRelevantScore();
						}
					} else {
						continue;
					}

					if (CrawlerConfig.getConfig().isTrainingMode()) {
						ProxyModel m = ProxyService.retreiveContentByURL(l.getLinkUrl(), null);
						if (m != null) {
							linkscore = m.getScore();
							if (score > 0.5)
								queue.enQueue(new QueueObj(l.getLinkUrl(), url, qObj.getDepth() + 1, 0, linkscore,
										resultN, resultH));
							else
								queue.enQueue(new QueueObj(l.getLinkUrl(), url, qObj.getDepth() + 1,
										qObj.getDistanceFromThai() + 1, linkscore, resultN, resultH));
						}
						continue;
					} else {

						if (score > 0.5)
							queue.enQueue(new QueueObj(l.getLinkUrl(), url, qObj.getDepth() + 1, 0, linkscore, resultN,
									resultH));
						else
							queue.enQueue(new QueueObj(l.getLinkUrl(), url, qObj.getDepth() + 1,
									qObj.getDistanceFromThai() + 1, linkscore, resultN, resultH));
					}
				}

			}

		}

		if (writer != null)
			writer.close();

		BestFirst.logger.info("finished");

	}

	public static byte[] download(String url) {

		InputStream is = null;

		if (url.indexOf("http://") < 0) {
			url = "http://" + url;
		}

		if (UrlDb.getUrlDAO().checkAndAddUrl(url, true)) {
			return null;
		}

		HttpGet request = null;

		try {
			request = new HttpGet(url);
			HttpResponse response = client.execute(request);
			int status = response.getStatusLine().getStatusCode();

			if (status != 200 && status != 302) {
				request.abort();
				return null;
			}

			HttpEntity entity = response.getEntity();
			String contentType = entity.getContentType().getValue();

			if (contentType != null) {
				contentType = contentType.toLowerCase();
			}

			if (!HttpUtils.isDownloadFileType(HttpUtils.getContentType(contentType),
					CrawlerConfig.getConfig().getAllowFileType())) {
				request.abort();
				return null;
			}

			is = entity.getContent();

			String header = response.getStatusLine() + "\n" + response.getFirstHeader("Date") + "\n"
					+ response.getFirstHeader("Server") + "\n" + response.getFirstHeader("Content-type")
					+ " Last-modified: " + response.getFirstHeader("Last-modified") + "\n" + "Content-length: "
					+ entity.getContentLength() + "\n\n";

			byte[] b = writeStreamToByteArray(header, is, CrawlerConfig.getConfig().getMaxFileSize());

			if (b == null) {
				request.abort();
				return null;
			}

			is.close();
			EntityUtils.consume(entity);

			return b;

		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			if (request != null) {
				request.abort();
				request = null;
			}

			final byte[] b = null;
			is = null;
		}

		return null;
	}

	private static byte[] writeStreamToByteArray(String header, InputStream fis, long limit) {
		try (ByteArrayOutputStream tmp = new ByteArrayOutputStream()) {

			tmp.write(header.getBytes());
			byte[] buffer = new byte[1024];
			int n;

			while ((n = fis.read(buffer)) != -1) {

				tmp.write(buffer, 0, n);
				if (tmp.size() > limit) {
					return null;
				}

				if (Thread.currentThread().isInterrupted())
					break;
			}

			buffer = null;
			buffer = tmp.toByteArray();
			tmp.close();
			return buffer;
		} catch (IOException e) {
			// e.printStackTrace();
			logger.error(e.getMessage());
		}

		return null;
	}
}