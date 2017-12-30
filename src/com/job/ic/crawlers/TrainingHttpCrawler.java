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
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

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
import com.job.ic.crawlers.daos.ResultDAO;
import com.job.ic.crawlers.daos.ResultDb;
import com.job.ic.crawlers.daos.SegmentGraphDAO;
import com.job.ic.crawlers.daos.UrlDAO;
import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.ml.classifiers.PageClassifier;
import com.job.ic.nlp.services.Checker;
import com.job.ic.nlp.services.LexToChecker;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.Status;

public class TrainingHttpCrawler extends Thread {

	private static Logger logger = Logger.getLogger(TrainingHttpCrawler.class);
	private static int numThreads;
	public static PoolingHttpClientConnectionManager mgr;
	public static CloseableHttpClient client;
	public static HashSet<String> lexitron;
	
	private static ARCFileWriter writer;
//	public static boolean isProxy = false;
	public static ARCFileWriter getWriter(){
		return writer;
	}
	
	
	public static void closeWriter(){
		if(writer != null)
			writer.close();
	}
	
	public static void runCrawler(ArrayList<WebsiteSegment> queue, int numThreads, Checker checker) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		TrainingHttpCrawler.numThreads = numThreads;
		System.setProperty("sun.net.client.defaultConnectTimeout ", String.valueOf(CrawlerConfig.getConfig().getTimeout()));
		System.setProperty("sun.net.client.defaultReadTimeout", String.valueOf(CrawlerConfig.getConfig().getSoTimeout()));
		
		if (CrawlerConfig.getConfig().isAllowHttps()) {
			SSLContextBuilder builder = SSLContexts.custom();
			builder.loadTrustMaterial(null, (chain, authType) -> true);
			SSLContext sslContext = builder.build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {

				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					// TODO Auto-generated method stub
					return true;
				}

				@Override
				public void verify(String arg0, String[] arg1, String[] arg2) throws SSLException {
					// TODO Auto-generated method stub

				}

				@Override
				public void verify(String arg0, X509Certificate arg1) throws SSLException {
					// TODO Auto-generated method stub

				}

				@Override
				public void verify(String arg0, SSLSocket arg1) throws IOException {
					// TODO Auto-generated method stub

				}
			});
			
			
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("https", sslsf)
					.register("http", new PlainConnectionSocketFactory()).build();
			mgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			
			
		}else{
			mgr = new PoolingHttpClientConnectionManager();
		}
		
		mgr.setMaxTotal(300);
		mgr.setDefaultMaxPerRoute(10);
	
		
		RequestConfig config = RequestConfig.custom().setStaleConnectionCheckEnabled(false).setSocketTimeout((int) CrawlerConfig.getConfig().getSoTimeout()).setConnectTimeout((int) CrawlerConfig.getConfig().getTimeout()).build();
		client = HttpClients.custom().setUserAgent(CrawlerConfig.getConfig().getUsrAgent()).setConnectionManager(mgr).setDefaultRequestConfig(config).build();

		
		try {
			writer = new ARCFileWriter();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		Status.clear();

		// Read crawler config file;
		
		logger.info("Use modified BFS crawler");
		
		File f = new File(CrawlerConfig.getConfig().getDbPath());
		if (!f.exists()) {
			f.mkdir();
		}

		f = new File(CrawlerConfig.getConfig().getDownloadPath());
		if (!f.exists()) {
			f.mkdir();
		}
		
	
		
		// Create enviroment
		LexToChecker.createEnvironment();
		ResultDb.createEnvironment();
		UrlDb.createEnvironment("urlDb");
		UrlDAO urlDao = UrlDb.getUrlDAO();

		ResultDAO result = ResultDb.getResultDAO();
		SegmentGraphDAO hostgraph = ResultDb.getSegmentGraphDAO();
		
		logger.info("START CRAWLING");

		
		Status.setTOTAL(queue.size());
		WebsiteSegment tmp;
		String seg;
		for (int i = 0; i < queue.size(); i++) {
			tmp = queue.get(i);
			seg = HttpUtils.getBasePath(tmp.getUrls().get(0));
			if(seg == null)
				continue;
			//System.out.println(tmp[0]);
			if (result.isDuplicate(seg) ) {
				queue.remove(i);
				i--;
				logger.info(HttpUtils.getBasePath(tmp.getUrls().get(0)) + " is already downloaded");
				Status.SUCCESS();
			}
		}

		if (queue.size() == 0) {
			return;
		}

		if (queue.size() < TrainingHttpCrawler.numThreads) {
			TrainingHttpCrawler.numThreads = queue.size();
		}

		
		CountDownLatch cd = new CountDownLatch(queue.size());
		// Create threads
		ExecutorService sv = Executors.newFixedThreadPool(TrainingHttpCrawler.numThreads);
		logger.info("REMAINING SITES :" + queue.size());
		while (queue.size() > 0) {
			tmp = queue.remove(0);
			sv.execute(new TrainingSegmentCrawler(result, hostgraph, urlDao, tmp, checker, writer, cd));
		}
		sv.shutdown();

		Status.setLastDLTime(System.currentTimeMillis());
		// Wait until all threads finished
		try {
			cd.await();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			mgr.shutdown();
			Status.clear();
			UrlDb.close();
			ResultDb.close();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		logger.info("finished");
		
	}



}
