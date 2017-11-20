package com.job.ic.proxy;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.daos.ProxyDb;
import com.job.ic.crawlers.daos.UrlDAO;
import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.sites.Crawler;
import com.job.ic.extraction.FeaturesExtraction;
import com.job.ic.ml.classifiers.PageClassifier;
import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.nlp.services.Checker;
import com.job.ic.proxy.dao.ProxyDao;
import com.job.ic.proxy.model.ProxyModel;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.sleepycat.persist.EntityCursor;

import net.htmlparser.jericho.LoggerProvider;
import net.sf.javainetlocator.InetAddressLocator;

public class ProxyService {

	private static Logger logger = Logger.getLogger(ProxyService.class);
	public static HashSet<String> completed;
	private static ProxyDao proxyDao;
	public static UrlDAO urlDao;

	private static LinkedBlockingQueue<PageClassifier> checkerPool = new LinkedBlockingQueue<>();

	private static boolean inMemoryMode = false;
	private static HashMap<String, Integer> mapTable = new HashMap<String, Integer>();
	private static ArrayList<ProxyModel> proxyModels = new ArrayList<ProxyModel>();

	public static void main(String[] args) {
//		ProxyService.setupProxy(args[0]);
//		getAll();
//		ProxyService.terminateProxy();

		CrawlerConfig.getConfig().setPageModel("page-baseball.arff");
		CrawlerConfig.getConfig().setTargetLang("ja");
		net.htmlparser.jericho.Config.LoggerProvider = LoggerProvider.DISABLED;
		ProxyService.importToProxy(args[0], args[1]);

	}

	public static void createIpCache(String dbPath) {

		ProxyService.setupProxy(dbPath);

		exportUrls("urls.txt");

		ProxyService.terminateProxy();

		String[] all = FileUtils.readFile("urls.txt");
		HashSet<String> hosts = new HashSet<String>();
		for (String s : all) {
			String[] d = s.split("\t");
			hosts.add(HttpUtils.getHost(d[0]));
		}

		ThreadPoolExecutor exe = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
		CountDownLatch cd = new CountDownLatch(hosts.size());
		for (String s : hosts) {
			exe.execute(new IPThread(s, cd));
		}
		exe.shutdown();

		try {
			cd.await(Long.MAX_VALUE, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		InetAddressLocator.exportCahceTable("ip_cache");
	}

	public static boolean contains(String url) {
		if (url == null)
			return false;

		url = url.toLowerCase().trim();
		url = HttpUtils.getStaticUrl(url);

		if (url == null)
			return false;

		if (inMemoryMode) {
			return mapTable.containsKey(url) || mapTable.containsKey(url.substring(0, url.length() - 1)) || mapTable.containsKey(url + "/");

		} else {
			return proxyDao.index.contains(url) || proxyDao.index.contains(url.substring(0, url.length() - 1)) || proxyDao.index.contains(url + "/");
		}

	}

	public static ProxyModel retreiveContentByURL(String url, UrlDAO urlDao) {
		ProxyModel model = null;
		if (url == null)
			return null;

		url = url.toLowerCase().trim();
		url = HttpUtils.getStaticUrl(url);

		if (url == null)
			return null;

		if (urlDao != null && urlDao.checkAndAddUrl(url, true))
			return null;

		try {

			String dh = HttpUtils.getHost(url);

			// first try
			if (inMemoryMode) {
				if (mapTable.containsKey(url)) {
					model = proxyModels.get(mapTable.get(url));
				} else {
					if (url.charAt(url.length() - 1) == '/')
						;
					{
						String tmp = url.substring(0, url.length() - 1);
						if (mapTable.containsKey(tmp)) {
							model = proxyModels.get(mapTable.get(tmp));
						}
					}

					if (model == null) {
						url = url + "/";

						if (mapTable.containsKey(url)) {
							model = proxyModels.get(mapTable.get(url));
						}
					}

				}

			} else {
				model = proxyDao.getByPrimaryKey(url);

				if (model == null && url.charAt(url.length() - 1) == '/') {
					model = proxyDao.getByPrimaryKey(url.substring(0, url.length() - 1));
				}

				if (model == null) {
					model = proxyDao.getByPrimaryKey(url + "/");
				}

			}

			if (model != null) {
				if (FeaturesExtraction.blackListHost.contains(dh.replace("www.", "")) || url.toLowerCase().contains("utah") || url.toLowerCase().contains("dubai")
						|| url.toLowerCase().contains("qatar") || url.toLowerCase().contains("bahrain") || url.toLowerCase().contains("cyprus") || url.toLowerCase().contains("australia")
						|| url.toLowerCase().contains("lasvegas")) {
					model.setScore(0);
				}
			}

			return model;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return null;
	}

	public static void importToProxy(String dlPath, String proxyPath) {
		
//		if(CrawlerConfig.getConfig().getNumThreads() == 1)
//			CrawlerConfig.getConfig().setNumThreads(4);/
		
		net.htmlparser.jericho.Config.LoggerProvider = LoggerProvider.DISABLED;

		// clear classifier
		checkerPool.clear();

		if (!CrawlerConfig.getConfig().getPageModel().trim().equals("")) {
			for (int i = 0; i < CrawlerConfig.getConfig().getNumThreads(); i++) {
				try {
					checkerPool.add(new PageClassifier());
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					System.exit(1);
				}
			}
		}else{
			checkerPool = null;
		}

		UrlDb.createEnvironment("proxyDb");
		try {
			logger.info("Start....");

			// Config.loadProperties();
			File f;
			if (!(f = new File(proxyPath)).exists()) {
				f.mkdir();
			}

			logger.info("Setup database");
			ProxyDb.createEnvironment(proxyPath, true);

			proxyDao = new ProxyDao(ProxyDb.getStore());

			ExecutorService execSvc = Executors.newFixedThreadPool(CrawlerConfig.getConfig().getNumThreads());
			File[] fileList = FileUtils.getAllFile(dlPath);

			int count = 0;

			for (int i = 0; i < fileList.length; i++) {
				// if (fileList[i].getName().contains(".arc"))
				count++;
			}

			CountDownLatch cd = new CountDownLatch(count);

			for (int i = 0; i < fileList.length; i++) {

				// if (fileList[i].getName().contains(".arc"))
				execSvc.submit(new PageIndexer(fileList[i].getAbsolutePath(), ProxyService.proxyDao, checkerPool, cd));
			}

			execSvc.shutdown();
			cd.await(Long.MAX_VALUE, TimeUnit.HOURS);

			// while (!execSvc.isTerminated()) {
			// try {
			// Thread.sleep(5000);
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			// }
			// System.out.println("Count>>" + proxyDao.count());
			logger.info("Complete.");
			// FileUtils.saveObjFile(completed, "complete.db");
			ProxyDb.close();
			UrlDb.close();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
	}

	public static void exportUrls(String filePath) {
		try (BufferedWriter bw = FileUtils.getBufferedFileWriter(filePath)) {
			EntityCursor<String> keyList = proxyDao.index.keys();
			Iterator<String> i = keyList.iterator();

			// list all key
			ArrayList<String> keys = new ArrayList<>();
			int count = 0;

			while (i.hasNext()) {
				if (count++ % 1000 == 0)
					System.out.println(count);

				keys.add(i.next());
			}

			keyList.close();
			// iterate each key and grab data from proxy
			for (String k : keys) {
//				bw.write(k + "\n");
				
				ProxyModel m = proxyDao.index.get(k);
				if(m == null)
					continue;
				
				bw.write(k + "\t" + m.getScore() + "\n");
				
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// public static void loadAllProxyModel() {
	//
	// EntityCursor<String> keyList = proxyDao.index.keys();
	// Iterator<String> i = keyList.iterator();
	//
	// long total = proxyDao.index.count();
	//
	//
	// // list all key
	// ArrayList<String> keys = new ArrayList<>();
	// while (i.hasNext()) {
	// keys.add(i.next());
	// }
	//
	// keyList.close();
	// int count = 0;
	// // iterate each key and grab data from proxy
	// for (String k : keys) {
	// ProxyModel m = retreiveContentByURL(k, null);
	// if (m == null) {
	// logger.error("cannot retreive " + k);
	//
	// System.exit(1);
	// continue;
	// }
	//
	//
	// if (count++ % 1000 == 0)
	// logger.info(count + "/" + total);
	//
	// proxyModels.add(m);
	// mapTable.put(k, proxyModels.size()-1);
	// }
	//
	//
	// }

	public static ArrayList<String> getAll() {

		EntityCursor<String> keyList = proxyDao.index.keys();
		Iterator<String> i = keyList.iterator();

		long total = proxyDao.index.count();

		ArrayList<String> output = new ArrayList<>();
		// list all key
		ArrayList<String> keys = new ArrayList<>();
		while (i.hasNext()) {
			keys.add(i.next());
		}

		keyList.close();
		int count = 0;
		// iterate each key and grab data from proxy
		int thai = 0, non = 0;
		for (String k : keys) {
			ProxyModel m = retreiveContentByURL(k, null);
			if (m == null) {
				logger.error("cannot retreive");
				continue;
			}

			if (Checker.getResultClass(m.getScore()) == ResultClass.RELEVANT)
				thai++;
			else
				non++;

			if (count++ % 1000 == 0)
				logger.info(count + "/" + total);

			output.add(k.toLowerCase());
		}

		logger.info("Thai: " + thai + "\tNon: " + non);
		return output;

	}

	public static void setupProxy(String dbPath) {
		try {
			logger.info("Start....");
			logger.info("Setup database " + dbPath);

			// System.out.println(dbPath);
			ProxyDb.createEnvironment(dbPath);
			proxyDao = new ProxyDao(ProxyDb.getStore());
			logger.info("Total pages: " + proxyDao.count());

			// long total = Runtime.getRuntime().maxMemory()/(1024*1024) -
			// (org.apache.commons.io.FileUtils.sizeOfDirectory(new
			// File(dbPath))/(1024*1024) + 8);
			// if(total > 0){
			// inMemoryMode = true;
			// loadAllProxyModel();
			//
			// //close proxy
			// terminateProxy();
			// }

		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}

	public static void terminateProxy() {
		ProxyDb.close();
	}

	public static double estimateRelevance(ArrayList<String> urls) {
		if(urls == null)
			return 0;
		
		double rel=0, total=0;
		
		for(String s: urls){
			ProxyModel m = ProxyService.retreiveContentByURL(s, null);
			if(m != null){
				if(Checker.getResultClass(m.getScore()) == ResultClass.RELEVANT){
					rel++;
				}
				
				total++;
			}
		}
		
		if(total != 0){
			return rel/total;
		}
		
		return 0;
	}

}
