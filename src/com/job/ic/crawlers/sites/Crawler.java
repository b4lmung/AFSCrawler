/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.sites;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.daos.ARCFileWriter;
import com.job.ic.crawlers.daos.ResultDAO;
import com.job.ic.crawlers.daos.SegmentGraphDAO;
import com.job.ic.crawlers.daos.UrlDAO;
import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.nlp.services.Checker;
import com.job.ic.utils.HttpUtils;

public abstract class Crawler {

	// protected CrawlerConfig config;
	/**
	 * result collect the downloaded URLs
	 */
	protected ResultDAO result;
	/**
	 * hostgraph collect the relation between the source and the target web site
	 */
	protected SegmentGraphDAO seggraph;

	protected UrlDAO urlDao;
	public UrlDAO getUrlDao() {
		return urlDao;
	}

	public void setUrlDao(UrlDAO urlDao) {
		this.urlDao = urlDao;
	}

	/**
	 * seed is the seed URLs
	 */
	protected ArrayList<String> seed;

	protected String extras;

	/**
	 * the logger for crawling process
	 */


	protected Checker checker;

	protected static Logger logger = Logger.getLogger(Crawler.class);

	protected ARCFileWriter writer;

	/**
	 * Abstract class for every crawler to be implement
	 * 
	 * @throws Exception
	 */
	public abstract void crawl();


	public void processSegment(String sourceSeg, ArrayList<LinksModel> destLinks) {
		
		HashMap<String, ArrayList<LinksModel>> destSegs = new HashMap<>();
		
		for(LinksModel m : destLinks){
			String base = HttpUtils.getBasePath(m.getLinkUrl());
			if(destSegs.containsKey(base)){
				destSegs.get(base).add(m);
			}else{
				ArrayList<LinksModel> nl = new ArrayList<LinksModel>();
				nl.add(m);
				destSegs.put(base, nl);
			}
		}
		
		for(String s: destSegs.keySet()){
			seggraph.addSegLinks(sourceSeg, s, destSegs.get(s));
		}
		
		destSegs.clear();
		
	
		
	}

}
