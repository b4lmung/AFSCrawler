package com.job.ic.crawlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.job.ic.crawlers.daos.ARCFileWriter;
import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.models.PageObject;
import com.job.ic.crawlers.models.QueueProxyObj;
import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.ml.classifiers.ClassifierOutput;
import com.job.ic.ml.classifiers.FeaturesCollectors;
import com.job.ic.ml.classifiers.MyStringToWordVector;
import com.job.ic.ml.classifiers.MyTextTokenizer;
import com.job.ic.ml.classifiers.PageClassifier;
import com.job.ic.ml.classifiers.WekaClassifier;
import com.job.ic.nlp.services.Checker;
import com.job.ic.nlp.services.LexToChecker;
import com.job.ic.proxy.ProxyService;
import com.job.ic.proxy.model.ProxyModel;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;

import net.htmlparser.jericho.LoggerProvider;
import weka.core.stemmers.IteratedLovinsStemmer;
import weka.core.stopwords.Rainbow;
import weka.core.tokenizers.WordTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.Reorder;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class WangCrawlerExtreme {
	private static Logger logger = Logger.getLogger(WangCrawlerExtreme.class);
	public static int countPage = 0;

	private static double rel = 0;
	private static double non = 0;

	public static String seedPath = CrawlerConfig.getConfig().getSeedPath();
	
	public static boolean isBFS = false;
	private static long limitPage = CrawlerConfig.getConfig().getLimitDownloadedPages();
	private static Checker page;
	

	public static PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager();
	public static CloseableHttpClient client;

	private static String header = "@relation 'crawler'\n@attribute src string\n@attribute dest string\n@attribute anchor string\n@attribute class {thai,non}\n@data\n";

	public static synchronized void addPage(final double score, final String url) {

		if (score > 0.5) {
			rel++;
		} else {
			non++;
		}

		if (WangCrawlerExtreme.rel + WangCrawlerExtreme.non > WangCrawlerExtreme.limitPage) {
			System.exit(0);
		}

	}

	public static String progress() {
		double hv = rel / (rel + non);
		double total = rel + non;

		return String.format("%.2f\t%.2f", hv, total);
	}

	public static void main(final String[] args) throws Exception {
		// trainD3Classifier();
		
		// analyzingTrainingData();
		CrawlerConfig.getConfig().setWindowSize(0);
		// System.exit(0);
		if (args.length > 0 && args[0].equals("true"))
			isBFS = true;
		else
			isBFS = false;

		page = new PageClassifier();

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

			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().register("https", sslsf).register("http", new PlainConnectionSocketFactory())
					.build();
			mgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

		}

		mgr.setMaxTotal(300);
		mgr.setDefaultMaxPerRoute(10);
		client = HttpClients.custom().setUserAgent(CrawlerConfig.getConfig().getUsrAgent()).setConnectionManager(mgr).setDefaultRequestConfig(config).build();

		page = null;
		if (CrawlerConfig.getConfig().getCheckerType() != null && CrawlerConfig.getConfig().getCheckerType().trim().length() > 0) {

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

		softFocusedThread();

		System.out.println(String.valueOf(WangCrawlerExtreme.rel) + "\t" + WangCrawlerExtreme.non);

	}

	public static void softFocusedThread() {

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

		WekaClassifier anchor = trainClassifier(CrawlerConfig.getConfig().getPredictorTrainingPath());

		PriorityQueue<QueueProxyObj> queue = new PriorityQueue<>(1, (o1, o2) -> {

			if (isBFS) {
				if (o1.getDepth() <= 0 && o2.getDepth() <= 0)
					return -1;
				else if (o1.getDepth() <= 0 && o2.getDepth() > 0)
					return -1;
				else if (o1.getDepth() > 0 && o2.getDepth() <= 0)
					return 1;
			}

			int p1 = (int) (o1.getScore() * 100);
			int p2 = (int) (o2.getScore() * 100);

			return -1 * Integer.compare(p1, p2);

		});

		ArrayList<WebsiteSegment> segments = FileUtils.readSegmentFile(seedPath);
		System.out.println(segments.size());
		for (int i = 0; i < segments.size(); i++) {
			WebsiteSegment s = segments.get(i);
			for (String url : s.getUrls()) {
				queue.add(new QueueProxyObj(url, null, (segments.size() - i) * 100.0, -1, -1));
			}
		}

		logger.info("start");

		String host = null;
		HtmlParser ps = new HtmlParser();

		while (queue.size() > 0) {

			QueueProxyObj qObj = queue.remove();

			if (qObj == null)
				continue;

			String url = qObj.getUrl();

			if (url == null)
				continue;

			double score = 0.0;

			ArrayList<LinksModel> links = null;

			byte[] p = download(url);

			if (p == null)
				continue;

			if (writer != null)
				writer.writeRecord(new PageObject(p, url));

			links = ps.parse(p, url);
			score = page.checkHtmlContent(p);

			addPage(score, url);

			WangCrawlerExtreme.logger.info(String.format("%s\t%.2f\t%s\tParent:\t%.2f", url, score, progress(), qObj.getScore()));

			if (links == null)
				continue;

			for (final LinksModel l : links) {

				if (l.getAnchorText() == null || l.getAnchorText().trim().length() == 0)
					continue;

				if (UrlDb.getUrlDAO().checkAndAddUrl(l.getLinkUrl(), false))
					continue;

				if (WangCrawlerExtreme.isBFS) {
					queue.add(new QueueProxyObj(l.getLinkUrl(), url, 0, qObj.getDepth() + 1, 0));
				} else {
					try {
					
						String[] features = { "a", "0","0","com-com","us-us","0","0","other","other","0", l.getAnchorText(), "", "non" };
						ClassifierOutput prediction = anchor.predict(l.getLinkUrl(), features);

						double pscore = Math.max(0.1, prediction.getRelevantScore());

//						if (score > 0.5)
						queue.add(new QueueProxyObj(l.getLinkUrl(), url, pscore, qObj.getDepth() + 1, 0));
						// else
						// queue.add(new QueueProxyObj(l.getLinkUrl(), url,
						// pscore, qObj.getDepth() + 1,
						// qObj.getDistanceFromRelevantPage() + 1));
					} catch (Exception e) {

					}

				}

			}

		}

		if (writer != null)
			writer.close();

		WangCrawlerExtreme.logger.info("finished");

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

			if (!HttpUtils.isDownloadFileType(HttpUtils.getContentType(contentType), CrawlerConfig.getConfig().getAllowFileType())) {
				request.abort();
				return null;
			}

			is = entity.getContent();

			String header = response.getStatusLine() + "\n" + response.getFirstHeader("Date") + "\n" + response.getFirstHeader("Server") + "\n" + response.getFirstHeader("Content-type")
					+ " Last-modified: " + response.getFirstHeader("Last-modified") + "\n" + "Content-length: " + entity.getContentLength() + "\n\n";

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

	public static WekaClassifier trainClassifier(String trainPath) {

		WekaClassifier c = new WekaClassifier("weka.classifiers.bayes.NaiveBayes", "-K");
		Remove rm = new Remove();
//		rm.setAttributeIndices("weka.filters.unsupervised.attribute.Remove -R 1,2,3,4,5,6,7,8,9,10,12");
		rm.setAttributeIndicesArray(new int[]{0,1,2,3,4,5,6,7,8,9,11});

		MyStringToWordVector vector = new MyStringToWordVector(10000);
		vector.setAttributeIndices("1");
		vector.setIDFTransform(true);
		vector.setOutputWordCounts(true);
		vector.setStopwordsHandler(new Rainbow());
		vector.setLowerCaseTokens(true);
		vector.setAttributeNamePrefix("sv_");
		vector.setTokenizer(new MyTextTokenizer());
		vector.setStemmer(new IteratedLovinsStemmer());
		
		
		Reorder order = new Reorder();
		try {
			order.setAttributeIndices("2-last,first");
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		Filter[] filters = new Filter[] { rm, vector, order };
		c.train(trainPath, null, true, filters, true, false);

		return c;
	}


}