/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.sites;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.TrainingHttpCrawler;
import com.job.ic.crawlers.daos.ResultDAO;
import com.job.ic.crawlers.daos.SegmentGraphDAO;
import com.job.ic.crawlers.daos.UrlDAO;
import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.nlp.services.Checker;
import com.job.ic.utils.Status;

public class SiteThread extends Thread {

	private volatile boolean isInterrupted = false;

	/**
	 * seed is the seed URLs
	 */
	private WebsiteSegment segment;
	/**
	 * result collect the downloaded URLs
	 */
	private ResultDAO result;
	/**
	 * hostgraph collect the relation between the source and the target web site
	 */
	private SegmentGraphDAO hostgraph;
	
	private UrlDAO urlDao;

	private Checker checker;

	private boolean isProxy;

	/**
	 * the logger for crawling process
	 */
	private static Logger logger = Logger.getLogger(SiteThread.class);
	protected long threadLastDLTime;

	public boolean isSiteInterrupted() {
		return isInterrupted;
	}

	public void setIsInterrupted(boolean isInterrupted) {
		this.isInterrupted = isInterrupted;
	}

	/**
	 * The constructor method
	 * 
	 * @param config
	 *            is the crawler configuration file
	 * @param result
	 *            collect the downloaded URLs
	 * @param hostgraph
	 * @param tmp
	 *            is the seed URLs
	 */
	public SiteThread(ResultDAO result, SegmentGraphDAO hostgraph, UrlDAO urlDao, WebsiteSegment s, boolean isProxy) {
		this.segment = s;
		this.result = result;
		this.hostgraph = hostgraph;
		this.isInterrupted = false;
		this.setProxy(isProxy);
		this.urlDao = urlDao;
	}

	/**
	 * To start crawling web page
	 */
	public void run() {

		RunHttpCrawler();
	}

	public void RunHttpCrawler() {
		Crawler nc = null;
		
		// TODO: Do something
		nc = new SegmentCrawler(result, hostgraph, urlDao, segment , TrainingHttpCrawler.checker, TrainingHttpCrawler.getWriter());

		this.threadLastDLTime = System.currentTimeMillis();

		try {
			nc.crawl();
		} catch (Exception e) {
			logger.error(e.toString() + "\t" + e.getCause());
		}

		nc = null;
		segment = null;
		Status.SUCCESS();
		logger.warn(Status.progressReport());
	}

	public Checker getChecker() {
		return checker;
	}

	public void setChecker(Checker checker) {
		this.checker = checker;
	}

	public boolean isProxy() {
		return isProxy;
	}

	public void setProxy(boolean isProxy) {
		this.isProxy = isProxy;
	}

}
