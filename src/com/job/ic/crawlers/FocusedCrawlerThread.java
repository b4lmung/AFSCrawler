package com.job.ic.crawlers;

import java.util.ArrayList;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.daos.UrlDAO;
import com.job.ic.crawlers.fetcher.SocketHTTPFetcher;
import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.models.PageObject;
import com.job.ic.crawlers.models.QueueObj;
import com.job.ic.crawlers.models.QueueProxyObj;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.nlp.services.Checker;
import com.job.ic.utils.Status;

public class FocusedCrawlerThread extends Thread {

	private static Logger logger = Logger.getLogger(FocusedCrawlerThread.class);

	private UrlDAO ud;
	private PriorityQueue<QueueProxyObj> queue;
	private SocketHTTPFetcher hp;
	private HtmlParser ps;
	private Checker checker;

	public FocusedCrawlerThread(UrlDAO ud, PriorityQueue<QueueProxyObj> queue, SocketHTTPFetcher hp, HtmlParser ps, Checker checker) {
		this.ud = ud;
		this.queue = queue;
		this.hp = hp;
		this.ps = ps;
		this.checker = checker;
	}

	public void run() {
		
		QueueProxyObj target = queue.remove();

		if (target == null)
			return;

		if (!HttpFocusedCrawler.isSoftLog && target.getDistanceFromRelevantPage() > 0)
			return;


		if (target.getUrl() == null) {
			return;
		}

		PageObject fp = hp.download(target.getUrl(), ud);
		if (fp == null) {
			return;
		}

		ArrayList<LinksModel> links = ps.parse(checker, fp);

		double score = 0;
		if (target.getDepth() == -1)
			score = 1;
		else
			score = fp.getPageScore();
		

		if (links == null)
			return;

		boolean isThai = false;
		if (score > 0.5)
			isThai = true;

		for (LinksModel tt : links) {
			String t = tt.getLinkUrl();

			if (t == null)
				continue;

			if (!ud.checkAndAddUrl(t.toLowerCase(), false)) {

//				if (target.getDepth() == -1) {
//					queue.add(new QueueProxyObj(t, target.getUrl(), 1.0, target.getDepth() + 1, 0));
//				} else {
					if (HttpFocusedCrawler.isSoftLog || isThai) {
						queue.add(new QueueProxyObj(t, target.getUrl(), score, target.getDepth() + 1, 0));
					}
//				}

			}
			// logger.info("finished");
		}

		HttpFocusedCrawler.addPage(score, target.getUrl());
		logger.info(String.format("%s\t%.2f\t%d\t%s", target.getUrl(), score, queue.size(), HttpFocusedCrawler.progress()));
	}
}
