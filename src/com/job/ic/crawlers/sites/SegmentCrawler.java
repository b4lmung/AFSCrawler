/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.sites;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;

import org.archive.crawler.datamodel.RobotsDirectives;
import org.archive.crawler.datamodel.Robotstxt;

import com.job.ic.crawlers.TrainingHttpCrawler;
import com.job.ic.crawlers.daos.ARCFileWriter;
import com.job.ic.crawlers.daos.ResultDAO;
import com.job.ic.crawlers.daos.SegmentGraphDAO;
import com.job.ic.crawlers.daos.UrlDAO;
import com.job.ic.crawlers.fetcher.SocketHTTPFetcher;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.models.PageObject;
import com.job.ic.crawlers.models.QueueObj;
import com.job.ic.crawlers.models.ResultModel;
import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.nlp.services.Checker;
import com.job.ic.proxy.ProxyService;
import com.job.ic.proxy.model.ProxyModel;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.Status;

import net.sf.javainetlocator.InetAddressLocator;

public class SegmentCrawler extends Crawler {

	public SegmentCrawler(ResultDAO result, SegmentGraphDAO hostgraph, UrlDAO urlDao, WebsiteSegment segment, Checker checker, ARCFileWriter writer) {
		this.seed = segment.getUrls();
		this.result = result;
		this.seggraph = hostgraph;
		this.checker = checker;
		this.writer = writer;
		this.extras = "";
		this.urlDao = urlDao;
	}

	public void crawl() {
		String country = null;
		int thai = 0;
		int non = 0;

		HtmlParser ps = new HtmlParser();
		SocketHTTPFetcher hp = new SocketHTTPFetcher(TrainingHttpCrawler.client);
		ArrayList<LinksModel> destLinks = null;
		HashSet<String> childSegments = null;

		// HashDb db = new HashDb();

		Robotstxt r = null;
		RobotsDirectives rd = null;
		PageObject fp = null;
		QueueObj obj;
		boolean isCheckOnSegment = false;
		int nonThreshold;

		// extract country features
		country = "other";
		try {
			country = InetAddressLocator.getLocale(HttpUtils.getHost(seed.get(0).split("\t")[0])).getCountry().toUpperCase();
		} catch (Exception e) {
		}

		if (!CrawlerConfig.getConfig().isIgnoreRobot()) {
			try {
				String h = "http://" + HttpUtils.getHost(seed.get(0).split("\t")[0]);
				byte[] robot = hp.downloadAsBytes(h);
				ByteArrayInputStream bi = new ByteArrayInputStream(robot);
				InputStreamReader is = new InputStreamReader(bi);
				r = new Robotstxt(new BufferedReader(is));
				rd = r.getDirectivesFor(CrawlerConfig.getConfig().getUsrAgent());
				is.close();
				bi.close();
			} catch (Exception e) {

			}
		}

		int maxPage = CrawlerConfig.getConfig().getMaxPagePerSite();
		String basePath = HttpUtils.getBasePath(seed.get(0));
		double relScore = 0;
		
		try {
			while (seed.size() > 0) {
				String url = seed.remove(0);
				this.extras = "";
				
				if (childSegments != null)
					childSegments.clear();
				childSegments = null;
				childSegments = new HashSet<>();

				if (destLinks != null)
					destLinks.clear();
				destLinks = null;
				destLinks = new ArrayList<>();

				// Start crawling
				nonThreshold = 0;

				// crawling loop

				int k, window = 0;

				try {

					obj = new QueueObj(HttpUtils.getStaticUrl(url), null, 0, 0, 1.0f);

					if (obj.getUrl() == null)
						continue;

					// Check max page conditions for site
//					if (!isCheckOnSegment && this.urlDao.countSite(obj.getUrl()) >= CrawlerConfig.getConfig().getMaxPagePerSite())
//						break;

					
					if (maxPage != -1 && (thai + non) >= maxPage)
						break;

					if (!CrawlerConfig.getConfig().isIgnoreRobot() && rd != null && !rd.allows(obj.getUrl()))
						continue;

					// TODO: Proxy
					if (CrawlerConfig.getConfig().getLocalProxyPath() != null && CrawlerConfig.getConfig().getLocalProxyPath().length() != 0) {
						ProxyModel mp = ProxyService.retreiveContentByURL(obj.getUrl(), this.urlDao);

						if (mp == null)
							continue;

						ArrayList<LinksModel> links = mp.getLinks();
						if (links == null)
							continue;

						if (Checker.getResultClass(mp.getScore()) == ResultClass.RELEVANT) {
							thai++;
							nonThreshold = 0;
							Status.addPage(true);
							this.extras += "1";
						} else {
							non++;
							nonThreshold++;
							Status.addPage(false);
							this.extras += "0";
						}

						relScore += mp.getScore();

						for (LinksModel l : links) {
							String anchor = l.getAnchorText();

							if (anchor != null && anchor.contains("-mikelinkstartmike-") && anchor.contains("-mikelinkendmike-")) {
								k = anchor.indexOf("-mikelinkstartmike-");
								if (k > window)
									anchor = anchor.substring(k - window);

								k = anchor.lastIndexOf("-mikelinkendmike-");

								if (anchor.length() - (k + "-mikelinkendmike-".length()) > window)
									anchor = anchor.substring(0, (k + "-mikelinkendmike-".length()) + window);

								anchor = anchor.replace("-mikelinkstartmike-", "").replace("-mikelinkendmike-", "");
								l.setAnchorText(anchor);
							}

							destLinks.add(l);
						}

						logger.info("DOWNLOADED\t" + obj.getUrl() + "\t" + mp.getScore() + "\t" + links.size());

					} else {
						fp = hp.download(obj.getUrl(), this.urlDao);
						if (fp == null) {
							continue;
						}

						writer.writeRecord(fp);
						
						ps.parse(checker, fp, destLinks, urlDao);
						if (Checker.getResultClass(fp.getPageScore()) == ResultClass.RELEVANT) {
							thai++;
							nonThreshold = 0;
							Status.addPage(true);
							this.extras += "1";
						} else {
							non++;
							nonThreshold++;
							Status.addPage(false);
							this.extras += "0";
						}
						logger.info("DOWNLOADED\t" + fp.getPageScore() + "\tSize:" + fp.getContent().length + "\t" + obj.toString() + "\t" + (thai + non));
						relScore += fp.getPageScore();
						obj = null;
						fp = null;
					}

				} catch (Exception e) {

					e.printStackTrace();
					logger.error(e.getCause() + ">>" + e.getMessage());
					// System.exit(1);;
				}

			}

			if (thai + non > 0) {
				this.result.addData(new ResultModel(basePath, thai, non, relScore / (thai + non), country, extras));
				processSegment(basePath, destLinks);
				thai = 0;
				non = 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Thread Exception [" + "]  " + e.getMessage());

		} finally {
			// finalize
			destLinks.clear();
			childSegments.clear();

			destLinks = null;
			childSegments = null;
			seggraph = null;
			checker = null;
			ps = null;
			fp = null;
		}
	}
}
