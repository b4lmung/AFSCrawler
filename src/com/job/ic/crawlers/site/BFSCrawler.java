/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.site;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.archive.crawler.datamodel.RobotsDirectives;
import org.archive.crawler.datamodel.Robotstxt;
import org.archive.io.arc.ARCWriter;
import org.archive.util.ArchiveUtils;

import com.job.ic.crawlers.daos.ARCFileWriter;
import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.fetcher.SocketHTTPFetcher;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.models.PageObject;
import com.job.ic.crawlers.models.Queue;
import com.job.ic.crawlers.models.QueueObj;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.nlp.services.Checker;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.Status;

import net.sf.javainetlocator.InetAddressLocator;
import weka.gui.SysErrLog;

public class BFSCrawler extends Thread {

	// protected CrawlerConfig config;

	/**
	 * seed is the seed URLs
	 */
	protected String[] seed;
	/**
	 * the logger for crawling process
	 */

	protected ARCFileWriter writer;

	protected Queue nodeq;

	protected Checker checker;

	protected static Logger logger = Logger.getLogger(BFSCrawler.class);

	public BFSCrawler(String[] seed, Checker checker, ARCFileWriter writer) {
		this.seed = seed;
		this.nodeq = new Queue();
		this.checker = checker;
		this.writer = writer;
	}

	@Override
	public void run() {
		crawl();
	}

	public void crawl() {
		// String siteurl = HttpUtils.getHost(seed[0]);
		String siteurl = HttpUtils.getBasePath(seed[0]);

		String country = null;
		int thai = -1;
		int non = -1;

		SocketHTTPFetcher hp = new SocketHTTPFetcher(Core.getClient());
		HtmlParser ps = new HtmlParser();
		HashSet<String> destUris = new HashSet<>();
		HashSet<String> destHost = new HashSet<>();
		HashDb db = new HashDb();

		PageObject fp = null;
		QueueObj obj;
		String base = "";
		try {
			// base = HttpUtils.getBasePath(seed[0]);
			// System.out.println(">>>" + base);
			try {
				siteurl = siteurl.trim();
			} catch (Exception e) {
			}

			logger.info("Start collecting webpage from " + siteurl);

			// Initialize
			// Checker checker = Core.checker.cloneObject();

			// Handle robots.txt
			for (String s : seed) {
				nodeq.enQueue(new QueueObj(HttpUtils.getStaticUrl(s), null, 0, 0, 1.0f));
				// System.out.println(s + "\t" + HttpUtils.getStaticUrl(s));
			}

			RobotsDirectives rd = null;
			Robotstxt r = null;
			if (!CrawlerConfig.getConfig().isIgnoreRobot()) {
				try {
					byte[] robot = hp.downloadAsBytes(HttpUtils.getHost(seed[0]));
					ByteArrayInputStream bi = new ByteArrayInputStream(robot);
					InputStreamReader is = new InputStreamReader(bi);
					r = new Robotstxt(new BufferedReader(is));
					rd = r.getDirectivesFor(CrawlerConfig.getConfig().getUsrAgent());
					is.close();
					bi.close();
				} catch (Exception e) {

				}
			}

			// Start crawling
			int nonThreshold = 0;
			thai = 0;
			non = 0;

			while (nodeq.size() > 0) {
				try {
					obj = nodeq.deQueue();

					// if (CrawlerConfig.getConfig().getSegmentThreshold() >= 0 && nonThreshold >=
					// CrawlerConfig.getConfig().getSegmentThreshold()) {
					// break;
					// }

					if (CrawlerConfig.getConfig().getMaxDepth() >= 0
							&& obj.getDepth() > CrawlerConfig.getConfig().getMaxDepth()) {
						continue;
					}

					if (CrawlerConfig.getConfig().getMaxPagePerSite() >= 0
							&& (thai + non) >= CrawlerConfig.getConfig().getMaxPagePerSite()) {
						break;
					}
					// Check robots.txt exculsion path
					if (rd != null && !rd.allows(obj.getUrl())) {
						continue;
					}
					
					

					fp = hp.download(obj.getUrl(), UrlDb.getUrlDAO());

					
					
					// fp = hp.download(obj, db, siteurl);

					if (fp == null) {
						continue;
					}

					ArrayList<LinksModel> links = ps.parse(checker, fp);

					// System.out.println(links.size());
					for (LinksModel l : links) {
						// System.out.println("pass " + siteurl + "\t" + l.getLinkUrl());

						String host = HttpUtils.getHost(l.getLinkUrl());
						destHost.add(host);

						// System.out.println(siteurl + "\t" + l.getLinkUrl() + "\t" +
						// l.getLinkUrl().replace("http://",
						// "").replace("https://","").startsWith(siteurl));
						// System.out.println(l.getLinkUrl().startsWith(siteurl));
						if (l.getLinkUrl().startsWith(siteurl)) {
							nodeq.enQueue(
									new QueueObj(l.getLinkUrl(), fp.getUrl(), fp.getDepth() + 1, 0, fp.getPageScore()));
						}
					}

					if (Checker.getResultClass(fp.getPageScore()) == ResultClass.RELEVANT) {
						thai++;
						nonThreshold = 0;
						Status.addPage(true);
					} else {
						non++;
						nonThreshold++;
						Status.addPage(false);

					}

					writer.writeRecord(fp);
					logger.info("DOWNLOADED\t" + fp.getPageScore() + "\t" + obj.toString() + "\t" + (thai + non)
							+ "\tfrom:\t" + base);

				} catch (Exception e) {
					e.printStackTrace();
					logger.error(e.getCause() + ">>" + e.getMessage());
				}
			}

			// save features, siteq, pageresult
			country = "other";
			try {
				country = InetAddressLocator.getLocale(siteurl).getCountry().toUpperCase();
			} catch (Exception e) {
			}

			String[] dest = new String[destHost.size()];
			destHost.toArray(dest);

			logger.info("FINISHED :" + siteurl.toLowerCase());

		} catch (Exception e) {
			logger.error("Thread Exception [" + siteurl.toLowerCase() + "]  " + e.getMessage());

		} finally {

			// finalize
			destHost.clear();
			destUris.clear();
			nodeq.clear();
			db.clear();
			destHost = null;
			destUris = null;
			nodeq = null;
			checker = null;
			db = null;
			ps = null;
			fp = null;
			obj = null;
			hp = null;
		}
	}

}
