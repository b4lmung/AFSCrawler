package com.job.ic.crawlers;

import java.util.ArrayList;
import java.util.Locale;

import net.htmlparser.jericho.LoggerProvider;
import net.sf.javainetlocator.InetAddressLocator;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.daos.ResultDb;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.ml.classifiers.PageClassifier;
import com.job.ic.nlp.services.Checker;
import com.job.ic.proxy.ProxyService;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.SegmentExtractor;
import com.job.ic.utils.Status;

public class TrainingMultiHopSegmentCrawler {
	private static Logger logger = Logger.getLogger(TrainingMultiHopSegmentCrawler.class);
	public static boolean flagServiceStop = false;
	public static int c = 0;
	public static int maxRelevant = -1;

	private static String dbPath = "g:/diving/db-train-diving/";
	private static String dlPath = "g:/diving/dl-tmp";
	private static String seeds = "back-diving-s.txt";
	// next is 5

	// private static String seeds = "dest-from-4-3.txt";

	// public static ProxyDao proxyDao;
	// private static boolean isUpdatable = false;
	// private static boolean incRatio = false;
	// private static boolean proxy = false;

	/**** Please configure this before you run it ****/
	// TODO: set it as true if you want to use page classifier to check web page
	public static boolean usePageClassifier = true;
	public static boolean extractFrontier = true;

	private static int start = 0;
	private static int end = 3;

	// test set = 0, 4 (//estate =5 เพราะ ข้อมูลน้อยมากไม่ถึงแสนเพจ)
	// train set = 0, 2

	public static void main(String[] args) throws Exception {

		if (args.length > 0) {
			dbPath = args[0];
			dlPath = args[1];
			seeds = args[2];
		}

		if (args.length == 4) {
			maxRelevant = Integer.parseInt(args[3]);
		} else if (args.length == 5) {
			start = Integer.parseInt(args[3]);
			end = Integer.parseInt(args[4]);
		}

		net.htmlparser.jericho.Config.LoggerProvider = LoggerProvider.DISABLED;

		// CrawlerConfig.loadConfig("crawler.conf");

		if (CrawlerConfig.getConfig().getLocalProxyPath() != null
				&& CrawlerConfig.getConfig().getLocalProxyPath().length() != 0)
			ProxyService.setupProxy(CrawlerConfig.getConfig().getLocalProxyPath());

		if (!FileUtils.exists(dlPath))
			FileUtils.mkdir(dlPath);

		if (!FileUtils.exists(dbPath))
			FileUtils.mkdir(dbPath);

		Checker checker = null;
		try {
			if (CrawlerConfig.getConfig().getLocalProxyPath().equals("")
					&& TrainingMultiHopSegmentCrawler.usePageClassifier)
				checker = new PageClassifier();
			else {
				checker = new Checker() {
					@Override
					public float checkHtmlContent(byte[] content) {
						return 0;
					}
				};
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(1);
		}

		for (int i = start; i <= end; i++) {
			try {
				test(i, checker);
				c = i;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (CrawlerConfig.getConfig().getLocalProxyPath() != null
				&& CrawlerConfig.getConfig().getLocalProxyPath().length() != 0)
			ProxyService.terminateProxy();

	}

	public static void test(int hop, Checker checker) throws Exception {
		// CrawlerConfig.loadConfig("crawler.conf");
		// CrawlerConfig.getConfig().setNumThreads(24);

		logger.info("start");
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				logger.warn("Recieve Shutdown signal (CTRL + C) ");
				try {
					ResultDb.close();
				} catch (Exception e) {

				}
				Runtime.getRuntime().halt(0);
			}
		});

		Status.clear();
		if (!FileUtils.exists("urlDb"))
			FileUtils.mkdir("urlDb");

		Locale.setDefault(Locale.US);
		System.setProperty("file.encoding", "UTF-8");

		logger.info("Start crawling");

		CrawlerConfig.getConfig().setDbPath(dbPath + "db-" + hop + "/");
		CrawlerConfig.getConfig().setDownloadPath(dlPath);
		// if(hop == 0)
		// CrawlerConfig.getConfig().setMaxPage(500);

		System.out.println("Db path :" + CrawlerConfig.getConfig().getDbPath());

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				TrainingMultiHopSegmentCrawler.logger.warn("Recieved Shutdown signal (CTRL + C) ");
				Runtime.getRuntime().halt(0);
			}
		});
		Status.clear();

		ArrayList<WebsiteSegment> queue;

		// queue = FileUtils.readSegmentFile(seeds);

		if (hop == 0)
			queue = FileUtils.readSegmentFile(seeds);
		else
			queue = FileUtils.readSegmentFile("dest-from-" + (hop - 1) + ".txt");

		TrainingHttpCrawler.runCrawler(queue, CrawlerConfig.getConfig().getNumThreads(), checker);

		logger.info("extracting frontier");
		if (extractFrontier) {
			SegmentExtractor.extractFrontier(dbPath + "db-" + hop, "tmp-frontier.txt");
			SegmentExtractor.extractSegment("tmp-frontier.txt", "dest-from-" + hop + ".txt");
		}

		if (CrawlerConfig.getConfig().getLocalProxyPath().equals(""))
			InetAddressLocator.exportCahceTable("ip_cache");
	}
}