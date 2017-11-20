/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.PriorityQueue;
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

import com.job.ic.crawlers.daos.UrlDAO;
import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.fetcher.SocketHTTPFetcher;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.QueueProxyObj;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.ml.classifiers.PageClassifier;
import com.job.ic.nlp.services.Checker;
import com.job.ic.nlp.services.LexToChecker;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.Status;

import net.htmlparser.jericho.LoggerProvider;

public class HttpFocusedCrawler {

	public static boolean isSoftLog = true;

	private static Logger logger = Logger.getLogger(HttpFocusedCrawler.class);

	public static int countPage = 0;
	private static Checker checker = null;

	private static double rel = 0, non = 0;

	public static String seedPath;
	public static PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager();
	public static CloseableHttpClient client;

	public static UrlDAO ud;
	private static long limitPage = CrawlerConfig.getConfig().getLimitDownloadedPages();

	// private static int maxPagePerSite = 1000;

	public static synchronized void addPage(double score, String url) {
		if (score > 0.5){
			rel++;
			Status.addPage(true);
		}
		else{
			non++;
			Status.addPage(false);
		}
		if (limitPage >= 0  && rel + non > limitPage)
			System.exit(0);
		
	

	}

	public static String progress() {
		double hv = HttpFocusedCrawler.rel / (HttpFocusedCrawler.rel + HttpFocusedCrawler.non);
		double total = rel + non;

		return String.format("%.2f\t%.2f", hv, total);
	}

	public static void main(String[] args) throws Exception {

//		CrawlerConfig.loadConfig("crawler.conf");

		seedPath = CrawlerConfig.getConfig().getSeedPath();
		net.htmlparser.jericho.Config.LoggerProvider = LoggerProvider.DISABLED;

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

			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("https", sslsf).register("http", new PlainConnectionSocketFactory())
					.build();
			mgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

		}

		mgr.setMaxTotal(300);
		mgr.setDefaultMaxPerRoute(10);
		client = HttpClients.custom().setUserAgent(CrawlerConfig.getConfig().getUsrAgent()).setConnectionManager(mgr).setDefaultRequestConfig(config).build();

		checker = null;
		if (CrawlerConfig.getConfig().getCheckerType() != null && CrawlerConfig.getConfig().getCheckerType().trim().length() > 0) {

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

		FileUtils.mkdir("urlDb");
		softFocusedThread();
		System.out.println(rel + "\t" + non);

		mgr.close();
		client.close();
	}

	public static void softFocusedThread() {

		HtmlParser ps = new HtmlParser();

		SocketHTTPFetcher hp = new SocketHTTPFetcher(client);

		UrlDb.createEnvironment("urlDb");
		ud = UrlDb.getUrlDAO();

		Locale.setDefault(Locale.US);
		System.setProperty("file.encoding", "UTF-8");

		PriorityQueue<QueueProxyObj> queue = new PriorityQueue<>(1, (o1, o2) -> -1 * Double.compare(o1.getScore(), o2.getScore()));

		// input seed urls
		for (String s : FileUtils.readFile(seedPath)) {
			if (s.contains("=="))
				continue;

			String[] tmp = s.split("\t");

			for(String t: tmp)
				queue.add(new QueueProxyObj(t, null, 1.0, -1, -1));

		}

		QueueProxyObj qObj;

		// crawling part
		logger.info("start");

		ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(CrawlerConfig.getConfig().getNumThreads());
		while (queue.size() > 0) {
			

			long remain = pool.getTaskCount() - pool.getCompletedTaskCount();

			if (queue.size() > 0) {
				if (remain > CrawlerConfig.getConfig().getNumThreads() * 2) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}

					mgr.closeIdleConnections(CrawlerConfig.getConfig().getConnMgrTimeout(), TimeUnit.MILLISECONDS);
					mgr.closeExpiredConnections();
					continue;
				}

				pool.execute(new FocusedCrawlerThread(ud, queue, hp, ps, checker));

			} else if (queue.size() == 0) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}

				logger.info("Queue size:" + queue.size());
			}

			// PageObject fp = hp.download(url, ud);
			// if (fp == null) {
			// continue;
			// }
			//
			// links = ps.parse(checker, fp);
			//
			// if(qObj.getDepth() == -1)
			// score = 1;
			// else
			// score = fp.getPageScore();
			// if (Checker.getResultClass(score) == ResultClass.RELEVANT) {
			// thai++;
			// Status.addPage(true);
			//
			// } else {
			// non++;
			// Status.addPage(false);
			// }
			//
			//
			// if (links == null)
			// continue;
			//
			// if (score > 0.5)
			// isThai = true;
			//
			// for (LinksModel tt : links) {
			// String t = tt.getLinkUrl();
			//
			// if (t == null)
			// continue;
			//
			// if (!ud.containUrl(t.toLowerCase())) {
			//
			// if (qObj.getDepth() == -1) {
			// queue.add(new QueueProxyObj(t, url, 1.0, qObj.getDepth() + 1,
			// 0));
			// } else {
			//
			// if(isSoftLog || isThai){
			// queue.add(new QueueProxyObj(t, url, score, qObj.getDepth() + 1,
			// 0));
			// }
			//
			//
			// }
			//
			// }
			// // logger.info("finished");
			// }
			// HttpFocusedCrawler.addPage(score, url);
			// logger.info(String.format("%s\t%.2f\t%d\t%s", url, score,
			// queue.size(), HttpFocusedCrawler.progress()));
		}

		logger.info("finished");

	}

}
