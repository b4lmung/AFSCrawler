package com.job.ic.ml.classifiers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.HttpCrawler;
import com.job.ic.crawlers.HttpSegmentCrawler;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.SegmentQueueModel;
import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.extraction.FeaturesExtraction;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.StringUtils;

public class FeaturesCollectors {
	private static LinkedBlockingQueue<String> relFeaturesFromRelSource = new LinkedBlockingQueue<>();
	private static LinkedBlockingQueue<String> nonFeaturesFromRelSource = new LinkedBlockingQueue<>();
	private static LinkedBlockingQueue<String> relFeaturesFromNonSource = new LinkedBlockingQueue<>();
	private static LinkedBlockingQueue<String> nonFeaturesFromNonSource = new LinkedBlockingQueue<>();

	private static boolean useDup = CrawlerConfig.getConfig().useDuplicateData();
	private static Logger logger = Logger.getLogger(HttpCrawler.class);

	// dupfeatureDb
	private static HashMap<String, ArrayList<String>> dupRelDb = new HashMap<>();
	private static HashMap<String, ArrayList<String>> dupNonDb = new HashMap<>();

	private static LinkedBlockingQueue<String> dupRelFeaturesFromRelSource = new LinkedBlockingQueue<>();
	private static LinkedBlockingQueue<String> dupNonFeaturesFromRelSource = new LinkedBlockingQueue<>();
	private static LinkedBlockingQueue<String> dupRelFeaturesFromNonSource = new LinkedBlockingQueue<>();
	private static LinkedBlockingQueue<String> dupNonFeaturesFromNonSource = new LinkedBlockingQueue<>();

	private static LinkedBlockingQueue<String> allFeatures = new LinkedBlockingQueue<>();
	private static LinkedBlockingQueue<String> allFeaturesWithoutFilter = new LinkedBlockingQueue<>();

	private static LinkedBlockingQueue<String> tmpResult = new LinkedBlockingQueue<>();

	private static int countFin = 0;

	public static synchronized void recordMulti(SegmentQueueModel input, int rel, int non) {
		ArrayList<WebsiteSegment> allSegs = input.getAllSegments();
		boolean isRelevant = false;
		double degree = HttpSegmentCrawler.calcRelevanceDegree(rel, non);
		if (degree > CrawlerConfig.getConfig().getRelevanceDegreeThreshold())
			isRelevant = true;

		if (rel + non > 0)
			incCountFin();

		if (allSegs == null || allSegs.size() == 0)
			return;
		for (WebsiteSegment seg : allSegs) {
			if (!CrawlerConfig.getConfig().isTrainingMode() && seg.getLinkPrediction() == null && seg.getAnchorPrediction() == null && seg.getUrlPrediction() == null)
				continue;

			String features = seg.getLinkFeatureString().toLowerCase();
			features = (isRelevant ? features.substring(0, features.lastIndexOf("non")).trim() + "thai" : features);

			boolean isRelSrc = seg.getSrcRelDegree() > CrawlerConfig.getConfig().getRelevanceDegreeThreshold();

			String[] tmp = features.split(",");

			if (tmp.length != 13)
				continue;

			// System.err.println(tmp[0] + "\t" + seg.getLinkPrediction() + "\t"
			// + seg.getAnchorPrediction() + "\t" + seg.getDirPrediction());
			tmpResult.add(tmp[0] + "\t" + (seg.getLinkPrediction() == null ? "[null]" : seg.getLinkPrediction()) + "\t" + (seg.getAnchorPrediction() == null ? "[null]" : seg.getAnchorPrediction())
					+ "\t" + (seg.getUrlPrediction() == null ? "[null]" : seg.getUrlPrediction()) + "\t" + isRelevant);
			allFeaturesWithoutFilter.add(features + "\tpredicted:\t"
					+ (WekaClassifier.average(seg.getLinkPrediction(), seg.getAnchorPrediction(), seg.getUrlPrediction()).getResultClass() == ResultClass.RELEVANT ? "rel" : "non") + "\t"
					+ (seg.getLinkPrediction() == null ? "[null]" : seg.getLinkPrediction()) + "\t" + (seg.getAnchorPrediction() == null ? "[null]" : seg.getAnchorPrediction()) + "\t"
					+ (seg.getUrlPrediction() == null ? "[null]" : seg.getUrlPrediction()));

			// filtering

			if (HtmlParser.shouldFilter(seg.getSegmentName()))
				continue;

			if (seg.getSegmentName().contains("/tag/") || seg.getSegmentName().contains("/tags/"))
				continue;

			if (isRelevant && StringUtils.isDuplicate(dupRelDb, tmp[0], tmp, tmp[10])) {
				if (useDup) {
					if (isRelSrc)
						dupRelFeaturesFromRelSource.add(features);
					else
						dupRelFeaturesFromNonSource.add(features);
				}
				continue;
			} else if (!isRelevant && StringUtils.isDuplicate(dupNonDb, tmp[0], tmp, tmp[10])) {
				if (useDup) {
					if (isRelSrc)
						dupNonFeaturesFromRelSource.add(features);
					else
						dupNonFeaturesFromNonSource.add(features);
				}
				continue;
			}

			allFeatures.add(features + "\tpredicted:\t"
					+ (WekaClassifier.average(seg.getLinkPrediction(), seg.getAnchorPrediction(), seg.getUrlPrediction()).getResultClass() == ResultClass.RELEVANT ? "rel" : "non") + "\t"
					+ (seg.getLinkPrediction() == null ? "[null]" : seg.getLinkPrediction()) + "\t" + (seg.getAnchorPrediction() == null ? "[null]" : seg.getAnchorPrediction()) + "\t"
					+ (seg.getUrlPrediction() == null ? "[null]" : seg.getUrlPrediction()));

			// allFeatures.add(features + "\tpredicted:\t" +
			// ((WekaClassifier.average(seg.getLinkPrediction(),
			// seg.getAnchorPrediction(),
			// seg.getUrlPrediction())).getResultClass() == ResultClass.RELEVANT
			// ? "rel" : "non"));
			// System.err.println(features +"\tpredicted:\t" +
			// (seg.getPredictedScore() > 0.5?"rel":"non"));

			// destination is relevant
			if (isRelevant) {

				// in case that src is relevant
				if (seg.getSrcRelDegree() > CrawlerConfig.getConfig().getRelevanceDegreeThreshold()) {
					relFeaturesFromRelSource.add(features);

					// classifier predict as relevant
					if (WekaClassifier.average(seg.getLinkPrediction(), seg.getAnchorPrediction(), seg.getUrlPrediction()).getResultClass() == ResultClass.RELEVANT) {
						AccuracyTracker.getRelSegmentPredictorConfusionMatrix().incTp();
					} else {
						AccuracyTracker.getRelSegmentPredictorConfusionMatrix().incFn();
					}

					if (seg.getLinkPrediction() != null) {
						if (seg.getLinkPrediction().getResultClass() == ResultClass.RELEVANT) {
							AccuracyTracker.getLinkRelConfusionMatrix().incTp();
						} else {
							AccuracyTracker.getLinkRelConfusionMatrix().incFn();
						}
					}

					if (seg.getAnchorPrediction() != null) {
						if (seg.getAnchorPrediction().getResultClass() == ResultClass.RELEVANT) {
							AccuracyTracker.getAnchorRelConfusionMatrix().incTp();
						} else {
							AccuracyTracker.getAnchorRelConfusionMatrix().incFn();
						}
					}

					if (seg.getUrlPrediction() != null) {
						if (seg.getUrlPrediction().getResultClass() == ResultClass.RELEVANT) {
							AccuracyTracker.getUrlRelConfusionMatrix().incTp();
						} else {
							AccuracyTracker.getUrlRelConfusionMatrix().incFn();
						}
					}

				} else {
					// src is tunnel node

					relFeaturesFromNonSource.add(features);

					if (WekaClassifier.average(seg.getLinkPrediction(), seg.getAnchorPrediction(), seg.getUrlPrediction()).getResultClass() == ResultClass.RELEVANT) {
						AccuracyTracker.getNonSegmentPredictorConfusionMatrix().incTp();
					} else {
						AccuracyTracker.getNonSegmentPredictorConfusionMatrix().incFn();
					}

					if (seg.getLinkPrediction() != null) {
						if (seg.getLinkPrediction().getResultClass() == ResultClass.RELEVANT) {
							AccuracyTracker.getLinkNonConfusionMatrix().incTp();
						} else {
							AccuracyTracker.getLinkNonConfusionMatrix().incFn();
						}
					}

					if (seg.getAnchorPrediction() != null) {
						if (seg.getAnchorPrediction().getResultClass() == ResultClass.RELEVANT) {
							AccuracyTracker.getAnchorNonConfusionMatrix().incTp();
						} else {
							AccuracyTracker.getAnchorNonConfusionMatrix().incFn();
						}
					}

					if (seg.getUrlPrediction() != null) {
						if (seg.getUrlPrediction().getResultClass() == ResultClass.RELEVANT) {
							AccuracyTracker.getUrlNonConfusionMatrix().incTp();
						} else {
							AccuracyTracker.getUrlNonConfusionMatrix().incFn();
						}
					}

				}

			} else {
				// destination is irrelevant

				// src is relvant
				if (seg.getSrcRelDegree() > CrawlerConfig.getConfig().getRelevanceDegreeThreshold()) {
					nonFeaturesFromRelSource.add(features);

					// predict as relevant
					if (WekaClassifier.average(seg.getLinkPrediction(), seg.getAnchorPrediction(), seg.getUrlPrediction()).getResultClass() == ResultClass.RELEVANT) {
						AccuracyTracker.getRelSegmentPredictorConfusionMatrix().incFp();
					} else {
						AccuracyTracker.getRelSegmentPredictorConfusionMatrix().incTn();
					}

					if (seg.getLinkPrediction() != null) {
						if (seg.getLinkPrediction().getResultClass() == ResultClass.RELEVANT) {
							AccuracyTracker.getLinkRelConfusionMatrix().incFp();
						} else {
							AccuracyTracker.getLinkRelConfusionMatrix().incTn();
						}
					}

					if (seg.getAnchorPrediction() != null) {
						if (seg.getAnchorPrediction() != null && seg.getAnchorPrediction().getResultClass() == ResultClass.RELEVANT) {
							AccuracyTracker.getAnchorRelConfusionMatrix().incFp();
						} else {
							AccuracyTracker.getAnchorRelConfusionMatrix().incTn();
						}
					}

					if (seg.getUrlPrediction() != null) {
						if (seg.getUrlPrediction() != null && seg.getUrlPrediction().getResultClass() == ResultClass.RELEVANT) {
							AccuracyTracker.getUrlRelConfusionMatrix().incFp();
						} else {
							AccuracyTracker.getUrlRelConfusionMatrix().incTn();
						}
					}

				} else {
					// source is irrelevant

					nonFeaturesFromNonSource.add(features);

					if (WekaClassifier.average(seg.getLinkPrediction(), seg.getAnchorPrediction(), seg.getUrlPrediction()).getResultClass() == ResultClass.RELEVANT) {
						AccuracyTracker.getNonSegmentPredictorConfusionMatrix().incFp();
					} else {
						AccuracyTracker.getNonSegmentPredictorConfusionMatrix().incTn();
					}

					if (seg.getLinkPrediction() != null) {
						if (seg.getLinkPrediction() != null && seg.getLinkPrediction().getResultClass() == ResultClass.RELEVANT) {
							AccuracyTracker.getLinkNonConfusionMatrix().incFp();
						} else {
							AccuracyTracker.getLinkNonConfusionMatrix().incTn();
						}
					}
					if (seg.getAnchorPrediction() != null) {
						if (seg.getAnchorPrediction() != null && seg.getAnchorPrediction().getResultClass() == ResultClass.RELEVANT) {
							AccuracyTracker.getAnchorNonConfusionMatrix().incFp();
						} else {
							AccuracyTracker.getAnchorNonConfusionMatrix().incTn();
						}
					}
					if (seg.getUrlPrediction() != null) {
						if (seg.getUrlPrediction() != null && seg.getUrlPrediction().getResultClass() == ResultClass.RELEVANT) {
							AccuracyTracker.getUrlNonConfusionMatrix().incFp();
						} else {
							AccuracyTracker.getUrlNonConfusionMatrix().incTn();
						}
					}

				}

			}
		}

		allFeaturesWithoutFilter.add("--------------------------");
		allFeatures.add("--------------------------");

		allSegs.clear();
	}

	public static synchronized void recordSingle(SegmentQueueModel input, int rel, int non) {
		ArrayList<WebsiteSegment> allSegs = input.getAllSegments();
		boolean isRelevant = false;
		double degree = HttpSegmentCrawler.calcRelevanceDegree(rel, non);
		if (degree > CrawlerConfig.getConfig().getRelevanceDegreeThreshold())
			isRelevant = true;

		if (rel + non > 0)
			incCountFin();

		if (allSegs == null || allSegs.size() == 0)
			return;

		for (WebsiteSegment seg : allSegs) {
			if (!CrawlerConfig.getConfig().isTrainingMode() && seg.getLinkPrediction() == null && seg.getAnchorPrediction() == null && seg.getUrlPrediction() == null)
				continue;

			String features = seg.getLinkFeatureString().toLowerCase();
			features = (isRelevant ? features.substring(0, features.lastIndexOf("non")) + "thai" : features);

			String[] tmp = features.split(",");

			if (tmp.length != 13)
				continue;

			tmpResult.add(tmp[0] + "\t" + (seg.getLinkPrediction() == null ? "[null]" : seg.getLinkPrediction()) + "\t" + (seg.getAnchorPrediction() == null ? "[null]" : seg.getAnchorPrediction())
					+ "\t" + (seg.getUrlPrediction() == null ? "[null]" : seg.getUrlPrediction()) + "\t" + isRelevant);
			// tmpResult.add(tmp[0] + "\t" +
			// seg.getLinkPrediction().getResultClass() + "\t" +
			// seg.getAnchorPrediction() + "\t" + seg.getDirPrediction());
			allFeaturesWithoutFilter.add(features + "\tpredicted:\t"
					+ (WekaClassifier.average(seg.getLinkPrediction(), seg.getAnchorPrediction(), seg.getUrlPrediction()).getResultClass() == ResultClass.RELEVANT ? "rel" : "non") + "\t"
					+ (seg.getLinkPrediction() == null ? "[null]" : seg.getLinkPrediction()) + "\t" + (seg.getAnchorPrediction() == null ? "[null]" : seg.getAnchorPrediction()) + "\t"
					+ (seg.getUrlPrediction() == null ? "[null]" : seg.getUrlPrediction()));

			// allFeaturesWithoutFilter.add(features + "\tpredicted:\t" +
			// ((WekaClassifier.average(seg.getLinkPrediction(),
			// seg.getAnchorPrediction(),
			// seg.getUrlPrediction())).getResultClass() == ResultClass.RELEVANT
			// ? "rel" : "non"));

			if (HtmlParser.shouldFilter(seg.getSegmentName()))
				continue;

			if (seg.getSegmentName().contains("/tag/") || seg.getSegmentName().contains("/tags/"))
				continue;

			// StringUtils.isDuplicate(dup, destSeg, features, anchor)
			if (isRelevant && StringUtils.isDuplicate(dupRelDb, tmp[0], tmp, tmp[10])) {
				if (useDup)
					dupRelFeaturesFromRelSource.add(features);
				continue;
			} else if (!isRelevant && StringUtils.isDuplicate(dupRelDb, tmp[0], tmp, tmp[10])) {
				if (useDup)
					dupNonFeaturesFromRelSource.add(features);
				continue;
			}

			// allFeatures.add(features + "\tpredicted:\t" +
			// ((WekaClassifier.average(seg.getLinkPrediction(),
			// seg.getAnchorPrediction(),
			// seg.getUrlPrediction())).getResultClass() == ResultClass.RELEVANT
			// ? "rel" : "non"));
			allFeatures.add(features + "\tpredicted:\t"
					+ (WekaClassifier.average(seg.getLinkPrediction(), seg.getAnchorPrediction(), seg.getUrlPrediction()).getResultClass() == ResultClass.RELEVANT ? "rel" : "non") + "\t"
					+ (seg.getLinkPrediction() == null ? "[null]" : seg.getLinkPrediction()) + "\t" + (seg.getAnchorPrediction() == null ? "[null]" : seg.getAnchorPrediction()) + "\t"
					+ (seg.getUrlPrediction() == null ? "[null]" : seg.getUrlPrediction()));

			// destination is relevant
			if (isRelevant) {

				// in case that src is relevant
				relFeaturesFromRelSource.add(features);

				// classifier predict as relevant
				if (WekaClassifier.average(seg.getLinkPrediction(), seg.getAnchorPrediction(), seg.getUrlPrediction()).getResultClass() == ResultClass.RELEVANT) {
					PredictorPool.getSegmentPredictorConfusionMatrix().incTp();
				} else {
					PredictorPool.getSegmentPredictorConfusionMatrix().incFn();
				}

				if (seg.getLinkPrediction() != null && seg.getLinkPrediction().getResultClass() == ResultClass.RELEVANT) {
					PredictorPool.getLinkConfusionMatrix().incTp();
				} else {
					PredictorPool.getLinkConfusionMatrix().incFn();
				}

				if (seg.getAnchorPrediction() != null && seg.getAnchorPrediction().getResultClass() == ResultClass.RELEVANT) {
					PredictorPool.getAnchorConfusionMatrix().incTp();
				} else {
					PredictorPool.getAnchorConfusionMatrix().incFn();
				}

				if (seg.getUrlPrediction() != null && seg.getUrlPrediction().getResultClass() == ResultClass.RELEVANT) {
					PredictorPool.getUrlConfusionMatrix().incTp();
				} else {
					PredictorPool.getUrlConfusionMatrix().incFn();
				}

			} else {
				// destination is irrelevant
				// src is relevant
				nonFeaturesFromRelSource.add(features);

				// predict as relevant
				if (WekaClassifier.average(seg.getLinkPrediction(), seg.getAnchorPrediction(), seg.getUrlPrediction()).getResultClass() == ResultClass.RELEVANT) {
					PredictorPool.getSegmentPredictorConfusionMatrix().incFp();
				} else {
					PredictorPool.getSegmentPredictorConfusionMatrix().incTn();
				}

				if (seg.getLinkPrediction() != null && seg.getLinkPrediction().getResultClass() == ResultClass.RELEVANT) {
					PredictorPool.getLinkConfusionMatrix().incFp();
				} else {
					PredictorPool.getLinkConfusionMatrix().incTn();
				}

				if (seg.getAnchorPrediction() != null && seg.getAnchorPrediction().getResultClass() == ResultClass.RELEVANT) {
					PredictorPool.getAnchorConfusionMatrix().incFp();
				} else {
					PredictorPool.getAnchorConfusionMatrix().incTn();
				}

				if (seg.getUrlPrediction() != null && seg.getUrlPrediction().getResultClass() == ResultClass.RELEVANT) {
					PredictorPool.getUrlConfusionMatrix().incFp();
				} else {
					PredictorPool.getUrlConfusionMatrix().incTn();
				}

			}
		}

		allFeaturesWithoutFilter.add("--------------------------");
		allFeatures.add("-----------------------");

		allSegs.clear();

	}

	public static ArrayList<String> createDatasetForSinglePredictor(boolean underSampling) {
		return createRelDatasetForMultiPredictor(underSampling);
	}

	public static ArrayList<String> createRelDatasetForMultiPredictor(boolean underSampling) {
		ArrayList<String> newDataSet = null;

		if (underSampling) {
			if (useDup)
				newDataSet = underSamplingDup(dupRelFeaturesFromRelSource, dupNonFeaturesFromRelSource, relFeaturesFromRelSource, nonFeaturesFromRelSource);
			else
				newDataSet = underSampling(relFeaturesFromRelSource, nonFeaturesFromRelSource);
		} else {
			newDataSet = new ArrayList<>();
			relFeaturesFromRelSource.drainTo(newDataSet);
			nonFeaturesFromRelSource.drainTo(newDataSet);
		}
		return newDataSet;
	}

	public static ArrayList<String> createNonDatasetForMultiPredictor(boolean underSampling) {
		ArrayList<String> newDataSet = null;

		if (underSampling) {
			if (useDup)
				newDataSet = underSamplingDup(dupRelFeaturesFromNonSource, dupNonFeaturesFromNonSource, relFeaturesFromNonSource, nonFeaturesFromNonSource);
			else
				newDataSet = underSampling(relFeaturesFromNonSource, nonFeaturesFromNonSource);
		} else {
			newDataSet = new ArrayList<>();
			relFeaturesFromNonSource.drainTo(newDataSet);
			nonFeaturesFromNonSource.drainTo(newDataSet);
		}
		return newDataSet;
	}

	public static ArrayList<String> underSampling(ArrayList<String> rel, ArrayList<String> non) {

		ArrayList<String> output = new ArrayList<>();
		int min = Math.min(rel.size(), non.size());

		if (min == 0)
			return null;

		for (int i = 0; i < min; i++) {
			output.add(rel.remove(rel.size() - 1));
			output.add(non.remove(non.size() - 1));
		}

		return output;
	}

	public static ArrayList<String> underSampling(LinkedBlockingQueue<String> relFeatures, LinkedBlockingQueue<String> nonFeatures) {

		ArrayList<String> rel = new ArrayList<>();
		ArrayList<String> non = new ArrayList<>();

		relFeatures.drainTo(rel);
		nonFeatures.drainTo(non);

		ArrayList<String> output = new ArrayList<>();
		int min = Math.min(rel.size(), non.size());

		if (min == 0)
			return null;

		for (int i = 0; i < min; i++) {
			output.add(rel.remove(rel.size() - 1));
			output.add(non.remove(non.size() - 1));
		}

		if (rel.size() > 0)
			relFeatures.addAll(rel);

		if (non.size() > 0)
			nonFeatures.addAll(non);

		return output;
	}

	public static ArrayList<String> underSamplingDup(LinkedBlockingQueue<String> relDupFeatures, LinkedBlockingQueue<String> nonDupFeatures, LinkedBlockingQueue<String> relFeatures,
			LinkedBlockingQueue<String> nonFeatures) {

		ArrayList<String> output = new ArrayList<>();

		logger.info("RelFeatures / Dup:\t" + relFeatures.size() + "\t" + relDupFeatures.size());
		logger.info("NonFeatures / Dup:\t" + nonFeatures.size() + "\t" + nonDupFeatures.size());
		
		// quota for rel and non samples
		int m = Math.min(relFeatures.size() + relDupFeatures.size(), nonDupFeatures.size() + nonFeatures.size());

		// process dup sample
		ArrayList<String> rel = new ArrayList<>();
		ArrayList<String> non = new ArrayList<>();

		relDupFeatures.drainTo(rel);
		nonDupFeatures.drainTo(non);
		relDupFeatures.clear();
		nonDupFeatures.clear();
		
		int quotaRel = m;
		int quotaNon = m;

		while (quotaNon > 0 && non.size() > 0) {
			output.add(non.remove(non.size() - 1));
			quotaNon--;
		}
		
		logger.info("Added nonDup:\t" + (m-quotaNon) + "\tquota:" + m);

		while (quotaRel > 0 && rel.size() > 0) {
			output.add(rel.remove(rel.size() - 1));
			quotaRel--;
		}
		logger.info("Added relDup:\t" + (m-quotaRel) + "\tquota:" + m);

		if (rel.size() > 0)
			relDupFeatures.addAll(rel);
		if (non.size() > 0)
			nonDupFeatures.addAll(non);
		
		rel.clear();
		non.clear();

		relFeatures.drainTo(rel);
		nonFeatures.drainTo(non);

		relFeatures.clear();
		nonFeatures.clear();

		int r = 0,n =0;
		while (quotaNon > 0 && non.size() > 0) {
			output.add(non.remove(non.size() - 1));
			quotaNon--;
			n++;
		}
		logger.info("Added non:\t" + n + "\tquota:" + m);

		while (quotaRel > 0 && rel.size() > 0) {
			r++;
			output.add(rel.remove(rel.size() - 1));
			quotaRel--;
		}
		logger.info("Added rel:\t" + r + "\tquota:" + m);

		
		if (rel.size() > 0)
			relFeatures.addAll(rel);

		if (non.size() > 0)
			nonFeatures.addAll(non);
		
		logger.info("------------ Remaining in buffer-------------");
		logger.info("RelFeatures / Dup:\t" + relFeatures.size() + "\t" + relDupFeatures.size());
		logger.info("NonFeatures / Dup:\t" + nonFeatures.size() + "\t" + nonDupFeatures.size());
		
		return output;
	}

	public static void exportPredictionResult(String filePath) {
		ArrayList<String> result = new ArrayList<>();
		tmpResult.drainTo(result);
		FileUtils.writeTextFile(filePath, result, false);
	}

	public static void backupAllFeatures(String filePath) {

		try (BufferedWriter bw = FileUtils.getBufferedFileWriter(filePath)) {
			bw.write(FeaturesExtraction.getHeader());
			for (String s : allFeatures) {
				bw.write(s + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void backupAllFeaturesWithoutFilter(String filePath) {
		try (BufferedWriter bw = FileUtils.getBufferedFileWriter(filePath)) {
			bw.write(FeaturesExtraction.getHeader());

			for (String s : allFeaturesWithoutFilter) {
				bw.write(s + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int getCountFin() {
		return countFin;
	}

	public static synchronized void incCountFin() {
		FeaturesCollectors.countFin++;
	}

}
