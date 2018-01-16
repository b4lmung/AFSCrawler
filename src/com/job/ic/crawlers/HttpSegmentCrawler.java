package com.job.ic.crawlers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.archive.crawler.datamodel.RobotsDirectives;
import org.archive.crawler.datamodel.Robotstxt;

import com.job.ic.crawlers.daos.ARCFileWriter;
import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.fetcher.SocketHTTPFetcher;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.DirectoryTree;
import com.job.ic.crawlers.models.DirectoryTreeNode;
import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.models.PageObject;
import com.job.ic.crawlers.models.SegmentFrontier;
import com.job.ic.crawlers.models.SegmentQueueModel;
import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.extraction.FeaturesExtraction;
import com.job.ic.ml.classifiers.ClassifierOutput;
import com.job.ic.ml.classifiers.NeighborhoodPredictor;
import com.job.ic.ml.classifiers.FeaturesCollectors;
import com.job.ic.ml.classifiers.JapaneseTokenizer;
import com.job.ic.ml.classifiers.MyTextTokenizer;
import com.job.ic.ml.classifiers.PredictorPool;
import com.job.ic.ml.classifiers.PredictorPoolMulti;
import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.ml.classifiers.TokenizedOutput;
import com.job.ic.nlp.services.Checker;
import com.job.ic.proxy.ProxyService;
import com.job.ic.proxy.model.ProxyModel;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.Status;
import com.job.ic.utils.StringUtils;

import net.sf.javainetlocator.InetAddressLocator;

public class HttpSegmentCrawler extends Thread {
	private static Logger logger = Logger.getLogger(HttpSegmentCrawler.class);

	private SegmentFrontier frontier;
	private Checker checker;
	private HttpCrawler caller;

	private ARCFileWriter writer;
	private boolean useLocalProxy;
	private boolean isMulti;

	public HttpSegmentCrawler(HttpCrawler caller, SegmentFrontier q, Checker checker, ARCFileWriter writer, boolean isMulti) {
		this.frontier = q;
		this.isMulti = isMulti;
		this.checker = checker;
		this.writer = writer;
		this.caller = caller;
		if (CrawlerConfig.getConfig().getLocalProxyPath().equals(""))
			this.useLocalProxy = false;
		else
			this.useLocalProxy = true;

	}

	public void run() {

		// long start = System.currentTimeMillis();

		SegmentQueueModel data;

		try {
			// data=this.frontier.deQueue();
			// System.err.println(data.getAllSegments().size());
			// logger.info(data == null);
			// logger.info(data.getWebsiteSegment() == null);
			while ((data = this.frontier.deQueue()) == null || data.getWebsiteSegment().getUrls().size() == 0)
				;

			ArrayList<WebsiteSegment> allSegs = data.getAllSegments();
			WebsiteSegment segment = data.getWebsiteSegment();

			if (segment == null || segment.getUrls().size() == 0) {
				// logger.info("case 1");
				return;
			}

			if (!CrawlerConfig.getConfig().isTrainingMode()) {
				if (CrawlerConfig.getConfig().getDistanceFromRelevantSeg() >= 0 && segment.getDepth() > 0
						&& segment.getDistanceFromRelevantSeg() > CrawlerConfig.getConfig().getDistanceFromRelevantSeg()) {
					// logger.info("case 2");
					return;
				}

				if (CrawlerConfig.getConfig().getMaxDepth() >= 0 && segment.getDepth() > CrawlerConfig.getConfig().getMaxDepth()) {
					// logger.info("case 3");
					return;
				}

			}

			HtmlParser ps = new HtmlParser();
			SocketHTTPFetcher hp = null;
			hp = new SocketHTTPFetcher(caller.getHttpClient());

			HashMap<String, ArrayList<LinksModel>> destSegments = new HashMap<>();

			// HashDb db = new HashDb();
			Robotstxt r = null;
			RobotsDirectives rd = null;
			PageObject fp = null;

			int rel = 0;
			int non = 0;

			// extract srccountry features
			if (segment.getCountry() == null || segment.getCountry().equals("other")) {

				String country = "other";
				try {
					country = InetAddressLocator.getCountry(HttpUtils.getHost(segment.getUrls().get(0)), true);
				} catch (Exception e) {
				}

				if (country.trim().equals(""))
					country = "other";

				segment.setCountry(country);
			}

			if (segment.getDomain() == null || segment.getDomain().equals("other")) {

				// src domain features
				String domain = HttpUtils.getDomain(segment.getUrls().get(0));

				if (domain == null)
					domain = "other";

				segment.setDomain(domain);
			}

			if (!CrawlerConfig.getConfig().isIgnoreRobot()) {
				try {
					String h = "http://" + HttpUtils.getHost(segment.getUrls().get(0));
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

			Hashtable<String, Double> results = new Hashtable<>();
			int nonThreshold = 0;

			// System.out.println("urls " + segment.getUrls().size());
			// ArrayList<Double> srcRelScores = new ArrayList<>();
			double score = 0;
			int k, window = CrawlerConfig.getConfig().getWindowSize();

			MyTextTokenizer tk = null;
			if (CrawlerConfig.getConfig().getTargetLang().equals("en")) {
				tk = new MyTextTokenizer();
			} else if (CrawlerConfig.getConfig().getTargetLang().equals("ja")) {
				tk = new JapaneseTokenizer();
			}

			assert (tk != null);

			HashMap<String, Integer> cntTk = new HashMap<String, Integer>();

			for (String s : segment.getUrls()) {
				TokenizedOutput to = tk.tokenizeString(s, false);
				cntTk.put(s, to.getKnown());
			}

			// boolean isSeed = segment.getDistanceFromRelevantSeg() == -1;

			// sort url by number of relevant tokens found in url
			Collections.sort(segment.getUrls(), (s1, s2) -> -1 * Integer.compare(cntTk.get(s1), cntTk.get(s2)));

			String order = "";
			for (String s : segment.getUrls()) {

				if (!CrawlerConfig.getConfig().isTrainingMode() && CrawlerConfig.getConfig().getSegmentThreshold() >= 0 && nonThreshold > CrawlerConfig.getConfig().getSegmentThreshold())
					break;

				if (rd != null && !rd.allows(s))
					continue;

				if (useLocalProxy) {

					if (!ProxyService.contains(s)) {
						// logger.info("continue 0 " + s);
						continue;
					}

					ProxyModel m = ProxyService.retreiveContentByURL(s, UrlDb.getUrlDAO());
					if (m == null) {
						// logger.info("continue 1 " + s + "\t" +
						// urlDao.checkAndAddUrl(s, false));
						continue;
					}

					ArrayList<LinksModel> link = m.getLinks();

					if (link == null) {
						// logger.info("continue 2");
						continue;
					}

					for (LinksModel l : link) {

						if (HtmlParser.shouldFilter(l.getLinkUrl()))
							continue;

						String seg = HttpUtils.getBasePath(l.getLinkUrl());

						if (seg != null) {
							if (destSegments.containsKey(seg)) {
								destSegments.get(seg).add(l);
							} else {
								ArrayList<LinksModel> tmp = new ArrayList<>();
								tmp.add(l);
								destSegments.put(seg, tmp);
							}
						}

					}

					score = m.getScore();

					// if (data.getDepth() < 0)
					// score = 1;

					results.put(s, score);

					if (Checker.getResultClass(score) == ResultClass.RELEVANT) {
						order += "1";

						rel++;
						nonThreshold = 0;

						Status.addPage(true);

					} else {
						order += "0";

						non++;
						nonThreshold++;
						Status.addPage(false);
					}

				} else {
					fp = hp.download(s, UrlDb.getUrlDAO());
					if (fp == null) {
						continue;
					}

					ps.parse(this.checker, fp, destSegments, UrlDb.getUrlDAO());

					if (Checker.getResultClass(fp.getPageScore()) == ResultClass.RELEVANT) {
						rel++;
						nonThreshold = 0;
						Status.addPage(true);

					} else {
						non++;
						nonThreshold++;
						Status.addPage(false);
					}

					score = fp.getPageScore();
					// if (data.getDepth() < 0)
					// score = 1;

					results.put(s, score);

					this.writer.writeRecord(fp);

				}

				logger.info(String.format("DOWNLOADED\t%.2f\tDepth:%d\tDistance:%d\t%s\t%d\tHV:\t%s\tqScore:\t%.3f\tpScore:%.3f\tsrcScore:%.3f\tqSize:\t%d", score, data.getDepth(),
						segment.getDistanceFromRelevantSeg(), s, rel + non, Status.getHarvestRate(), data.getScore(), data.getPredictionScore(), data.getAvgRelScore(), this.frontier.size()));

			}

			if (rel + non > 0) {
				logger.info("Finished downloading\t" + segment.getSegmentName() + "\tRel:\t" + rel + "\tNon:\t" + non + "\tTotal:\t" + (rel + non) + "\tPredicted:\t" + data.getPredictionScore()
						+ "\tCrawlingOrder:\t" + order + "\tInLink:\t" + data.getAllSegments().size() + "\t"
						+ data.getAllSegments().stream().map(a -> String.valueOf(a.getSrcRelDegree())).collect(Collectors.toList()));
				finishedTask(data, rel, non);
				processSegment(segment, destSegments, results);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		// System.out.println(System.currentTimeMillis() - start);
	}

	public void processSegment(WebsiteSegment sourceSeg, HashMap<String, ArrayList<LinksModel>> destSegments, Hashtable<String, Double> results) {

		if (destSegments.size() == 0)
			return;

		ArrayList<WebsiteSegment> sms = new ArrayList<>();
		Set<String> dests = destSegments.keySet();

		boolean srcThai = false;
		double srcRelDegree = 0;
		Enumeration<Double> ex = results.elements();
		double thai = 0, non = 0;
		ArrayList<Double> srcRelScores = new ArrayList<>();
		while (ex.hasMoreElements()) {
			double d = ex.nextElement();
			srcRelScores.add(d);
			if (d > 0.5)
				thai++;
			else
				non++;
		}

		srcRelDegree = thai / (thai + non);

		if (srcRelDegree >= CrawlerConfig.getConfig().getRelevanceDegreeThreshold()) {
			srcThai = true;
		}

		if (!srcThai && CrawlerConfig.getConfig().getDistanceFromRelevantSeg() >= 0 && sourceSeg.getDistanceFromRelevantSeg() + 1 > CrawlerConfig.getConfig().getDistanceFromRelevantSeg())
			return;

		WebsiteSegment sm = null;

		for (String s : dests) {
			String anchor = "", url = "";

			StringBuilder anchorB = new StringBuilder();
			StringBuilder urlB = new StringBuilder();

			anchorB.append(StringUtils.extractWordFromURL(s, null) + " ");
			int count = 0;

			ArrayList<LinksModel> links = destSegments.get(s);
			ArrayList<LinksModel> destUrls = new ArrayList<>();
			for (LinksModel l : links) {
				urlB.append(StringUtils.extractWordFromURL(l.getLinkUrl(), s) + " ");

				if (l.getAnchorText() != null)
					anchorB.append(StringUtils.cleanAnchorText(l.getAnchorText()) + " ");

				if (useLocalProxy) {
					if (ProxyService.contains(l.getLinkUrl()))
						count++;
					else
						continue;
				}

				if (UrlDb.getUrlDAO().checkAndAddUrl(l.getLinkUrl(), false)) {
					continue;
				}

				destUrls.add(l);
			}

			if (useLocalProxy && count == 0)
				continue;

			if (destUrls.size() == 0)
				continue;

			anchor = anchorB.toString();
			url = urlB.toString();

			anchorB = null;
			urlB = null;

			// dest domain
			String destDomain = HttpUtils.getDomain(s);
			if (destDomain == null || destDomain.trim().equals(""))
				destDomain = "other";
			if (!Arrays.asList(FeaturesExtraction.domain).contains(destDomain.toLowerCase()))
				destDomain = "other";

			// dest country
			String destCountry = "other";
			try {
				destCountry = InetAddressLocator.getCountry(HttpUtils.getHost(s), true);
			} catch (Exception e) {
			}

			if (destCountry == null || destCountry.trim().equals(""))
				destCountry = "other";

			if (destCountry.trim().equals(""))
				destCountry = "other";

			if (!Arrays.asList(FeaturesExtraction.country).contains(destCountry.toLowerCase()))
				destCountry = "other";

			// src domain
			String srcDomain = sourceSeg.getDomain();

			// src country
			String srcCountry = sourceSeg.getCountry();

			if (srcDomain == null || srcDomain.trim().equals(""))
				srcDomain = "other";
			if (!Arrays.asList(FeaturesExtraction.domain).contains(srcDomain.toLowerCase()))
				srcDomain = "other";

			if (srcCountry == null || srcCountry.trim().equals(""))
				srcCountry = "other";
			if (!Arrays.asList(FeaturesExtraction.country).contains(srcCountry.toLowerCase()))
				srcCountry = "other";

			String segName = StringUtils.cleanUrlDataForPrediction(s);

			boolean sameHost = false;
			if (HttpUtils.getHost(sourceSeg.getSegmentName()).equals(HttpUtils.getHost(s)))
				sameHost = true;

			if (srcThai || sourceSeg.getDepth() == 0) {
				sm = new WebsiteSegment(segName, sourceSeg.getSegmentName(), sourceSeg.getDepth() + 1, destUrls, 0, srcDomain, srcCountry, destDomain, destCountry, url, anchor, srcRelScores,
						sameHost);
			} else {
				sm = new WebsiteSegment(segName, sourceSeg.getSegmentName(), sourceSeg.getDepth() + 1, destUrls, sourceSeg.getDistanceFromRelevantSeg() + 1, srcDomain, srcCountry, destDomain,
						destCountry, url, anchor, srcRelScores, sameHost);
			}

			sms.add(sm);
		}

		if (CrawlerConfig.getConfig().isTrainingMode()) {

			for (WebsiteSegment s : sms) {
				double score = ProxyService.estimateRelevance(s.getUrls());
				s.setPrediction(new ClassifierOutput(score, 1 - score, 1), null, null);
				this.frontier.enQueue(s);
			}
		} else if (CrawlerConfig.getConfig().getPredictorTrainingPath().equals("")) {
			this.frontier.enQueue(sms);
		} else {
			predict(sms, frontier);
		}

		sms.clear();

	}

	private void predict(ArrayList<WebsiteSegment> segments, SegmentFrontier frontier) {
		try {
			if (this.isMulti) {
				PredictorPoolMulti.predict(segments, frontier);
			} else {
				PredictorPool.predict(segments, frontier);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static double calcRelevanceDegree(int thai, int non) {
		int total = thai + non;
		return (1.0 * thai) / total;
	}

	public synchronized void finishedTask(SegmentQueueModel input, int rel, int non) {
		if (input == null || input.getAllSegments() == null || input.getAllSegments().size() == 0)
			return;

		NeighborhoodPredictor.record(input, rel, non);

		if (isMulti)
			FeaturesCollectors.recordMulti(input, rel, non);
		else
			FeaturesCollectors.recordSingle(input, rel, non);

	}

}
