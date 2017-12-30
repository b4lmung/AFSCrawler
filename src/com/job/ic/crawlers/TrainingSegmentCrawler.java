/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import net.sf.javainetlocator.InetAddressLocator;

import org.apache.log4j.Logger;
import org.archive.crawler.datamodel.RobotsDirectives;
import org.archive.crawler.datamodel.Robotstxt;

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
import com.job.ic.ml.classifiers.JapaneseTokenizer;
import com.job.ic.ml.classifiers.MyTextTokenizer;
import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.ml.classifiers.TokenizedOutput;
import com.job.ic.nlp.services.Checker;
import com.job.ic.proxy.ProxyService;
import com.job.ic.proxy.model.ProxyModel;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.Status;

public class TrainingSegmentCrawler extends Thread {

	protected ResultDAO result;
	protected SegmentGraphDAO seggraph;
	protected UrlDAO urlDao;
	protected ArrayList<String> seed;
	protected String extras;
	protected Checker checker;
	protected ARCFileWriter writer;
	private CountDownLatch cd;

	protected static Logger logger = Logger.getLogger(TrainingSegmentCrawler.class);

	public TrainingSegmentCrawler(ResultDAO result, SegmentGraphDAO hostgraph, UrlDAO urlDao, WebsiteSegment segment, Checker checker, ARCFileWriter writer, CountDownLatch cd) {
		this.seed = segment.getUrls();
		this.result = result;
		this.seggraph = hostgraph;
		this.checker = checker;
		this.writer = writer;
		this.extras = "";
		this.urlDao = urlDao;
		this.cd = cd;
	}

	public void run() {
		crawl();
		Status.SUCCESS();
		logger.warn(Status.progressReport());
	}

	public void crawl() {
		HtmlParser ps = new HtmlParser();
		SocketHTTPFetcher hp = new SocketHTTPFetcher(TrainingHttpCrawler.client);
		ArrayList<LinksModel> destLinks = new ArrayList<>();

		if (TrainingMultiHopSegmentCrawler.usePageClassifier && TrainingMultiHopSegmentCrawler.maxRelevant > 0 
				&& TrainingMultiHopSegmentCrawler.maxRelevant < Status.getCumulativeRelevantPages()) {
			logger.info("#Relevant pages exceed the threshold --> Exiting " + TrainingMultiHopSegmentCrawler.maxRelevant + "\t" + Status.getCumulativeRelevantPages());
			return;
		}

		try {
			String country = null;
			int thai = 0;
			int non = 0;

			// HashDb db = new HashDb();

			Robotstxt r = null;
			RobotsDirectives rd = null;
			PageObject fp = null;
			QueueObj obj;
			boolean isCheckOnSegment = false;

			// extract country features
			country = "other";
			try {
				country = InetAddressLocator.getCountry(HttpUtils.getHost(seed.get(0).split("\t")[0]), true);
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

			// sort url by number of relevant tokens found in url
			MyTextTokenizer tk = null;
			if (CrawlerConfig.getConfig().getTargetLang().equals("en")) {
				tk = new MyTextTokenizer();
			} else if (CrawlerConfig.getConfig().getTargetLang().equals("ja")) {
				tk = new JapaneseTokenizer();
			}

			HashMap<String, Integer> cntTk = new HashMap<String, Integer>();

			for (String s : seed) {
				TokenizedOutput to = tk.tokenizeString(s, false);
				cntTk.put(s, to.getKnown());
			}

			Collections.sort(seed, (s1, s2) -> -1 * Integer.compare(cntTk.get(s1), cntTk.get(s2)));
			this.extras = "";

			for (String url : seed) {

				// Start crawling

				// crawling loop
				int k, window = 0;
				try {

					obj = new QueueObj(HttpUtils.getStaticUrl(url), null, 0, 0, 1.0f);

					if (obj.getUrl() == null)
						continue;

					// Check max page conditions for site
					// if (CrawlerConfig.getConfig().getMaxPagePerSite() > 0 && !isCheckOnSegment &&
					// this.urlDao.countSite(obj.getUrl()) >=
					// CrawlerConfig.getConfig().getMaxPagePerSite())
					// break;

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
							Status.addPage(true);
							this.extras += "1";
						} else {
							non++;
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
							// logger.info(l.getLinkUrl());
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
							Status.addPage(true);
							this.extras += "1";
						} else {
							non++;
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
					System.exit(1);
				}

			}

			if (thai + non > 0) {
				// logger.info("add");
				this.result.addData(new ResultModel(basePath, thai, non, relScore / (thai + non), country, extras));
				processSegment(basePath, destLinks);
				thai = 0;
				non = 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Thread Exception [" + "]  " + e.getMessage());

		} finally {
			this.cd.countDown();
			// finalize
			if (destLinks != null)
				destLinks.clear();
			destLinks = null;
			seggraph = null;
			checker = null;
			ps = null;
		}
	}

	public void processSegment(String sourceSeg, ArrayList<LinksModel> destLinks) {

		HashMap<String, ArrayList<LinksModel>> destSegs = new HashMap<>();

		for (LinksModel m : destLinks) {
			String base = HttpUtils.getBasePath(m.getLinkUrl());
			if (destSegs.containsKey(base)) {
				destSegs.get(base).add(m);
			} else {
				ArrayList<LinksModel> nl = new ArrayList<LinksModel>();
				nl.add(m);
				destSegs.put(base, nl);
			}
		}

		for (String s : destSegs.keySet()) {
			seggraph.addSegLinks(sourceSeg, s, destSegs.get(s));
		}

		destSegs.clear();

	}
}
