/**
 * @author Thanaphon Suebchua
 */
package net.mikelab.ic.crawlers;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import net.htmlparser.jericho.LoggerProvider;

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
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;
import org.archive.io.arc.ARCConstants;
import org.archive.io.arc.ARCWriter;

import com.job.ic.crawlers.daos.ARCFileWriter;
import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.ml.classifiers.PageClassifier;
import com.job.ic.nlp.services.Checker;
import com.job.ic.nlp.services.LexToChecker;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.Status;

public class SiteCrawler {

	private static Logger logger = Logger.getLogger(SiteCrawler.class);
	private LinkedList<String[]> queue;
	private int numThreads;
	public static HashSet<String> lexitron;

	private PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager();
	public static CloseableHttpClient client;

	public static CloseableHttpClient getClient() {
		return client;
	}

	public static void main(String[] args) throws Exception {
		logger.info("Hello world");
		FileUtils.deleteDir("db-tmp");
//		FileUtils.deleteDir("urlDb");
		FileUtils.deleteDir("error");
		
		Status.clear();

		System.setProperty("file.encoding", "UTF-8");

		logger.info("Start crawling");

		// config file
		// config = new CrawlerConfig("crawler.conf");
		net.htmlparser.jericho.Config.LoggerProvider = LoggerProvider.DISABLED;

		UrlDb.createEnvironment("urlDb");
		
		// seeds
		LinkedList<String[]> queue = com.job.ic.utils.FileUtils.importQueueFromFile("seed.txt", null);

		System.out.println(queue.size());
		// number of threads = 8
		final SiteCrawler crawler = new SiteCrawler(queue, CrawlerConfig.getConfig().getNumThreads());

		crawler.start();

	}

	public SiteCrawler(LinkedList<String[]> queue, int numThreads) {

		super();
		this.queue = queue;
		this.numThreads = numThreads;
	}

	public void start() throws Exception {

		Status.clear();

		// Read crawler config file;

		// if (config.getSpiderAlgo().equals("lswc")) {
		// logger.info("Use modified LSWC crawler");
		// } else {
		logger.info("Use modified BFS crawler");
		// }

		File f = new File(CrawlerConfig.getConfig().getDbPath());
		if (!f.exists()) {
			f.mkdir();
		}

		f = new File(CrawlerConfig.getConfig().getDownloadPath());
		if (!f.exists()) {
			f.mkdir();
		}

		f = new File("errors");
		if (!f.exists()) {
			f.mkdir();
		}

		// HTTP Client
		RequestConfig config;

		config = RequestConfig.custom().setStaleConnectionCheckEnabled(false)
				.setSocketTimeout((int) CrawlerConfig.getConfig().getSoTimeout())
				.setConnectTimeout((int) CrawlerConfig.getConfig().getTimeout()).build();

		// handle ssl connection
		if (CrawlerConfig.getConfig().isAllowHttps()) {
			SSLContextBuilder builder = SSLContexts.custom();
			builder.loadTrustMaterial(null, (chain, authType) -> true);
			SSLContext sslContext = builder.build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {

				@Override
				public boolean verify(String hostname, SSLSession session) {
					// TODO Auto-generated method stub
					return true;
				}

				@Override
				public void verify(String arg0, SSLSocket arg1) throws IOException {
					// TODO Auto-generated method stub

				}

				@Override
				public void verify(String arg0, X509Certificate arg1) throws SSLException {
					// TODO Auto-generated method stub

				}

				@Override
				public void verify(String arg0, String[] arg1, String[] arg2) throws SSLException {
					// TODO Auto-generated method stub

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

		// Create enviroment
		// TODO: lang and cf;

		Checker checker = null;

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

		// ARCFileWriter writer = new ARCFileWriter(Core.config.isGzip());

		logger.info("START CRAWLING");

		Status.setTOTAL(queue.size());
		String[] tmp;

		if (queue.size() == 0) {
			return;
		}

		if (queue.size() < this.numThreads) {
			this.numThreads = queue.size();
		}

		ARCFileWriter writer = new ARCFileWriter();

		// Create threads
		ExecutorService sv = Executors.newFixedThreadPool(this.numThreads);
		logger.info("REMAINING SITES :" + queue.size());
		while (queue.size() > 0) {
			tmp = queue.remove();
			sv.execute(new BFSCrawler(tmp, checker, writer));
		}
		sv.shutdown();

		Status.setLastDLTime(System.currentTimeMillis());
		// Wait until all threads finished

		try {
			sv.awaitTermination(200, TimeUnit.DAYS);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		writer.close();
		try {
			mgr.shutdown();
			Status.clear();
			// writer.close();
		} catch (Exception e) {
		}

	}
}
