package com.job.ic.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecordBase;

import com.job.ic.crawlers.daos.UrlDAO;
import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.extraction.FeaturesExtraction;
import com.job.ic.ml.classifiers.PageClassifier;
import com.job.ic.nlp.services.Checker;
import com.job.ic.nlp.services.LanguageIdentifier;
import com.job.ic.proxy.dao.ProxyDao;
import com.job.ic.proxy.model.ProxyModel;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;

//import crawler.data.DownloadedData;
//import crawler.tools.DownloadFileReader;

public class PageIndexer implements Runnable {

	private ProxyDao proxy;
	private String filename;
	private CountDownLatch cd;
	private static Logger logger = Logger.getLogger(PageIndexer.class);

	private LinkedBlockingQueue<PageClassifier> checkerPool;

	public PageIndexer(String filename, ProxyDao proxy, LinkedBlockingQueue<PageClassifier> checkerPool, CountDownLatch cd) {
		this.filename = filename;
		this.proxy = proxy;
		this.checkerPool = checkerPool;
		this.cd = cd;
	}

	public void runArcReader() {

		if (!this.filename.contains(".arc"))
			return;

		UrlDAO ud = UrlDb.getUrlDAO();
		ArcReader reader;
		PageClassifier checker = null;

		if (this.checkerPool != null) {
			try {
				checker = checkerPool.take();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		try {
			File f = new File(this.filename);
			logger.info("processing " + this.filename);
			reader = ArcReaderFactory.getReader(new FileInputStream(f));
			
			
			ArcRecordBase rec;
			String url;
			float score;
			HtmlParser parser = new HtmlParser();
			ArrayList<LinksModel> links;

			reader.getNextRecord();
			String targetLang = CrawlerConfig.getConfig().getTargetLang();
			
			while ((rec = reader.getNextRecord()) != null) {
				url = rec.getUrlStr();

				if (url == null)
					continue;

				byte[] tmp = FileUtils.readBytesFromStream(rec.getPayloadContent());

				links = parser.parse(tmp, url);

				String dh = HttpUtils.getHost(url);

				if (FeaturesExtraction.blackListHost.contains(dh.replace("www.", "")) || url.toLowerCase().contains("utah") || url.toLowerCase().contains("dubai")
						|| url.toLowerCase().contains("cyprus") || url.toLowerCase().contains("australia") || url.toLowerCase().contains("lasvegas")){
					score = 0;
				}else{
					if(checker == null)
						score = (float) LanguageIdentifier.identifyLanguage(tmp, targetLang);
					else
						score = checker.checkHtmlContent(tmp);
				}

				logger.info(url + "\t" + score);
				ud.checkAndAddUrl(url, true);
				proxy.add(new ProxyModel(url, score, links));

			}

			logger.warn(String.format("FINISHED - %s \t %d", filename, cd.getCount()));
			if (reader != null)
				reader.close();

		} catch (Exception e) {
			logger.error(e.toString() + "\t" + this.filename);
			e.printStackTrace();
		} finally {
			if (checker != null) {
				checkerPool.add(checker);
			}
			cd.countDown();

		}

	}
/*
	public void runBsonReader() {

		UrlDAO ud = UrlDb.getUrlDAO();
		DownloadFileReader reader;
		PageClassifier checker = null;
		try {
			checker = checkerPool.take();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		logger.info("processing " + this.filename);
		try {

			reader = new DownloadFileReader(this.filename);
			DownloadedData data = null;
			if (checker == null)
				return;

			String url;
			float score;
			HtmlParser parser = new HtmlParser();
			ArrayList<LinksModel> links;
			int count = 0;
			while ((data = reader.next()) != null) {
				url = data.getURL();

				if (url == null)
					continue;

				links = null;
				try {

					if (ud.checkAndAddUrl(url, false))
						continue;

					// if (ProxyService.urlDao.containUrl(url))
					// continue;
					if (data.getData() == null) {
						score = 0;
					} else {
						links = parser.parse(data.getData(), url);
						score = checker.checkHtmlContent(data.getData());
					}

					// logger.info(url + "\t" + score + "\t" + count++);

					proxy.add(new ProxyModel(url, score, links));

					ud.checkAndAddUrl(url, true);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			logger.warn(String.format("FINISHED %s \t %d", filename, cd.getCount()));

			if (reader != null)
				reader.close();
		} catch (Exception e) {
			logger.error(e.toString() + "\t" + this.filename);
			e.printStackTrace();
		} finally {
			if (checker != null) {
				checkerPool.add(checker);
			}

			cd.countDown();
		}

	}*/

	@Override
	public void run() {
		if (FileUtils.isDirectory(this.filename)) {
			cd.countDown();
			return;
		}

		if (this.filename.contains(".arc.gz"))
			runArcReader();
//		else
//			runBsonReader();
	}

}
