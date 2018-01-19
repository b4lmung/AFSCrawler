package com.job.ic.ml.classifiers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.SegmentFrontier;
import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.experiments.ParseLog;
import com.job.ic.extraction.FeaturesExtraction;
import com.job.ic.utils.FileUtils;

import weka.core.Instances;

public class PredictorPool {

	private static Logger logger = Logger.getLogger(PredictorPool.class);

	private static ThreadPoolExecutor executor;
	private static int numThreads = 8;
	private static boolean isInit = false;

	private static boolean useAdaptiveWeighting = true;

	private static LinkedBlockingQueue<Predictor> predictorPool = new LinkedBlockingQueue<>();
	// private static LinkedBlockingQueue<WekaClassifier> anchorPredictorPool =
	// new LinkedBlockingQueue<>();
	// private static LinkedBlockingQueue<WekaClassifier> urlPredictorPool = new
	// LinkedBlockingQueue<>();

	// TODO: writing code to update weight adaptively
	private static ConfusionMatrixObj link = new ConfusionMatrixObj();
	private static ConfusionMatrixObj anchor = new ConfusionMatrixObj();
	private static ConfusionMatrixObj url = new ConfusionMatrixObj();

	private static ConfusionMatrixObj all = new ConfusionMatrixObj();

	static {
		executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
	}

	public static void terminatePredictorPool() {
		predictorPool.clear();
		// anchorPredictorPool.clear();
		// urlPredictorPool.clear();
		isInit = false;

		executor.shutdown();
	}

	public static ConfusionMatrixObj getLinkConfusionMatrix() {
		return link;
	}

	public static ConfusionMatrixObj getAnchorConfusionMatrix() {
		return anchor;
	}

	public static ConfusionMatrixObj getUrlConfusionMatrix() {
		return url;
	}

	public static ConfusionMatrixObj getSegmentPredictorConfusionMatrix() {
		return all;
	}

	public static synchronized void predict(ArrayList<WebsiteSegment> instances, SegmentFrontier frontier) throws Exception {

		if (!isInit)
			throw new Exception("predictor pool is empty");

		CountDownLatch cd = new CountDownLatch(instances.size());
		for (WebsiteSegment instance : instances) {
			executor.execute(new PredictionTask(cd, instance, predictorPool));
		}
		cd.await(10, TimeUnit.MINUTES);
		executor.getQueue().clear();

		frontier.enQueue(instances);
	}

	public static void shutdown() {
		executor.shutdown();
	}

	public static synchronized void predict(ArrayList<String[]> instances) throws Exception {
		if (!isInit)
			throw new Exception("predictor pool is empty");

		logger.info("#instances :" + instances.size());
		CountDownLatch cd = new CountDownLatch(instances.size());
		for (String[] instance : instances) {
			executor.execute(new PredictionTaskForFeatureAnalyzer(cd, instance, predictorPool));
		}

		cd.await();
		logger.info("finished");
	}

	public static void write(ArrayList<String> data, String output, String header) {

		ArrayList<String> buffer = new ArrayList<String>();
		for (int i = 0; i < data.size(); i++) {
			buffer.add(data.get(i));
		}

		if (header != null) {
			FileUtils.writeTextFile(output, header, false);
			FileUtils.writeTextFile(output, buffer, true);
		} else {
			FileUtils.writeTextFile(output, buffer, false);
		}

	}

	public static double[] normalizeWeights(double[] precisionWeights) {

		// transform
		int[] values = new int[precisionWeights.length];

		// find min

		int min = (int) precisionWeights[0] * 100;

		for (int i = 0; i < precisionWeights.length; i++) {
			values[i] = (int) precisionWeights[i] * 100;

			if (min > precisionWeights[i]) {
				min = values[i];
			}

		}

		min--;

		double[] newWeights = new double[precisionWeights.length];
		double totalWeights = 0;
		for (int i = 0; i < values.length; i++) {
			values[i] -= min;
			totalWeights += values[i];
		}

		for (int i = 0; i < newWeights.length; i++) {
			newWeights[i] = values[i] / totalWeights;
		}

		return newWeights;
	}

	public static synchronized void onlineUpdatePredictor(ArrayList<String> instances) throws Exception {

		// double[] weightRelClassifiers = new double[] {
		// PredictorPool.link.getRelevantPrecision(),
		// PredictorPool.anchor.getRelevantPrecision(),
		// PredictorPool.url.getRelevantPrecision() };
		// weightRelClassifiers = normalizeWeights(weightRelClassifiers);

		double[] weightRelClassifiers = null;

		if (useAdaptiveWeighting) {
			weightRelClassifiers = new double[] { AccuracyTracker.calcWeight(AccuracyTracker.getLinkRelConfusionMatrix().getAccuracy()),
					AccuracyTracker.calcWeight(AccuracyTracker.getAnchorRelConfusionMatrix().getAccuracy()), AccuracyTracker.calcWeight(AccuracyTracker.getUrlRelConfusionMatrix().getAccuracy()),
					AccuracyTracker.calcWeight(AccuracyTracker.getHistoryConfusionMatrix().getAccuracy()) };
		} else {
			weightRelClassifiers = CrawlerConfig.getConfig().getClassifierWeights();
		}

		assert (weightRelClassifiers != null);
		// weightClassifiers = new double[]{PredictorPool.link.getGmean(),
		// PredictorPool.anchor.getGmean(), PredictorPool.url.getGmean()};

		logger.info("Weights rel classifiers: " + Arrays.toString(weightRelClassifiers));

		System.err.println(instances.size());
		if (instances != null) {

			FileUtils.writeTextFile("logs/tmp.arff", FeaturesExtraction.getHeader(), false);
			FileUtils.writeTextFile("logs/tmp.arff", instances, true);

			Instances newDataset = new Instances(new FileReader("logs/tmp.arff"));
			newDataset.setClassIndex(newDataset.numAttributes() - 1);

			for (Predictor p : predictorPool) {
				p.getLinkClassifier().updateClassifier(newDataset);
				p.getLinkClassifier().setWeight(weightRelClassifiers[0]);

				p.getAnchorClassifier().updateClassifier(newDataset);
				p.getAnchorClassifier().setWeight(weightRelClassifiers[1]);

				p.getUrlClassifier().updateClassifier(newDataset);
				p.getUrlClassifier().setWeight(weightRelClassifiers[2]);

			}

		}

		FileUtils.deleteFile("logs/tmp.arff");
		// FileUtils.deleteFile("htmp.arff");
	}

	public static void trainPredictor(String trainPath) {

		double[] weightRelClassifiers = CrawlerConfig.getConfig().getClassifierWeights();

		boolean isKnn = false;

		try {

			// clear
			predictorPool.clear();

			String option = null;

			int[] rm = { 1, 11, 12 };
			if (CrawlerConfig.getConfig().getTargetLang().equals("ja")) {
				rm = new int[] { 1, 6, 7, 10, 11, 12 };
			}

			logger.info("=========Link==========");
			// link based
			String algo = CrawlerConfig.getConfig().getLinkClassifierAlgo();

			if (algo.contains("IBk"))
				isKnn = true;

			option = CrawlerConfig.getConfig().getLinkClassifierParams();

			WekaClassifier link = new WekaClassifier(algo, option, weightRelClassifiers[0]);

			if (isKnn) {
				link.train(trainPath, rm, false, null, true, false);
			} else if (algo.contains("NaiveBayes")) {
				if (option.contains("-K"))
					link.train(trainPath, rm, false, null, true, false);
				else
					link.train(trainPath, rm, true, null, true, false);

			}

			rm = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12 };

			logger.info("=========Anchor==========");
			algo = "weka.classifiers.bayes.NaiveBayesMultinomialText";

			if (CrawlerConfig.getConfig().getTargetLang().equals("ja"))
				option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.CharacterNgramTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";
			else
				option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.MyTextTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";

			WekaClassifier anchor = new WekaClassifier(algo, option, weightRelClassifiers[1]);

			anchor.train(trainPath, rm, false, null, true, false);

			// logger.info("anchor G-mean:\t" +
			// anchor.getGMeanFromTestSet(testPath));

			// FileUtils.saveObjFile(anchor, modelPath + "/anchor.model");

			logger.info("=========URL==========");

			algo = "weka.classifiers.bayes.NaiveBayesMultinomialText";

			rm = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };

			if (CrawlerConfig.getConfig().getTargetLang().equals("ja"))
				option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.CharacterNgramTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";
			else
				option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.MyTextTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";

			WekaClassifier url = new WekaClassifier(algo, option, weightRelClassifiers[2]);

			url.train(trainPath, rm, false, null, true, false);

			Predictor p = new Predictor(link, anchor, url);
			// clear old predictor
			for (int i = 0; i < numThreads; i++) {
				predictorPool.add((Predictor) FileUtils.deepClone(p));
			}
			logger.info("finished loading predictor models");

		} catch (Exception e) {
			e.printStackTrace();
		}

		isInit = true;

	}

	public static void prepareKFold(String trainPath, int k, int totalTimes) {

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(trainPath)))) {

			String tmp;
			String[] data;
			boolean isData = false;
			ArrayList<String> rel = new ArrayList<>();
			ArrayList<String> non = new ArrayList<>();

			while ((tmp = br.readLine()) != null) {

				if (tmp.contains("@data")) {
					isData = true;
					continue;
				}

				if (!isData)
					continue;

				data = tmp.split(",");

				String url = data[0];
				if (url.contains("{") || url.contains("}") || url.contains("'") || url.contains("\"")) {
					continue;
				}

				if (HtmlParser.shouldFilter(url))
					continue;

				if (data[data.length - 1].equals("non")) {
					non.add(tmp);
				} else {
					rel.add(tmp);
				}
			}

			logger.info("finished reading data");

			int nonPerFile = (int) Math.ceil(non.size() * 1.0 / k);
			int relPerFile = (int) Math.ceil(rel.size() * 1.0 / k);

			for (int a = 0; a < totalTimes; a++) {

				logger.info("preparing " + k + " folds @" + a);
				FileUtils.mkdir(a + "tmp");
				for (int i = 0; i < k; i++) {

					ArrayList<String> tt = new ArrayList<>();
					ArrayList<String> nt = new ArrayList<>();

					Collections.shuffle(non);
					Collections.shuffle(rel);

					for (int n = 0; n < nonPerFile && non.size() > 0; n++) {
						nt.add(non.get(n));
					}

					for (int n = 0; n < relPerFile && rel.size() > 0; n++) {
						tt.add(rel.get(n));
					}

					FileUtils.writeTextFile(a + "tmp/non-" + i + ".csv", nt, false);
					FileUtils.writeTextFile(a + "tmp/rel-" + i + ".csv", tt, false);
					nt.clear();
					tt.clear();
				}

				// FileUtils.rename("tmp/", a + "tmp/");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// public static void prepareKFold(String trainPath, int k, int a) {
	// executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
	//
	// try {
	// DataSource data = new DataSource(trainPath);
	// Instances in = data.getDataSet();
	// ArrayList<String> thai = new ArrayList<>();
	// ArrayList<String> non = new ArrayList<>();
	//
	// in.setClassIndex(in.numAttributes() - 1);
	// for (int j = 0; j < in.numInstances(); j++) {
	//
	// String host = HttpUtils.getHost(in.instance(j).stringValue(0));
	//
	// if (FeaturesExtraction.blackListHost.contains(host.replace("www.", ""))
	// || in.instance(j).stringValue(0).toLowerCase().contains("utah")
	// || in.instance(j).stringValue(0).toLowerCase().contains("gay") ||
	// in.instance(j).stringValue(0).toLowerCase().contains("dubai")
	// || in.instance(j).stringValue(0).toLowerCase().contains("qatar") ||
	// in.instance(j).stringValue(0).toLowerCase().contains("bahrain")
	// || in.instance(j).stringValue(0).toLowerCase().contains("cyprus") ||
	// in.instance(j).stringValue(0).toLowerCase().contains("greatestdivesites")
	// || in.instance(j).stringValue(0).toLowerCase().contains("croatia") ||
	// in.instance(j).stringValue(0).toLowerCase().contains("politiken.dk")
	// || in.instance(j).stringValue(0).toLowerCase().contains("australia") ||
	// in.instance(j).stringValue(0).toLowerCase().contains("lasvegas"))
	// continue;
	//
	// // if(in.instance(i).classAttribute().toString())
	// if (in.instance(j).stringValue(in.numAttributes() - 1).equals("non")) {
	// non.add(in.instance(j).toString());
	// } else {
	// thai.add(in.instance(j).toString());
	// }
	//
	// }
	//
	// int nonPerFile = (int) Math.ceil(non.size() * 1.0 / k);
	// int thaiPerFile = (int) Math.ceil(thai.size() * 1.0 / k);
	//
	// FileUtils.mkdir("tmp");
	// for (int i = 0; i < k; i++) {
	//
	// ArrayList<String> tt = new ArrayList<>();
	// ArrayList<String> nt = new ArrayList<>();
	//
	// Collections.shuffle(non);
	// Collections.shuffle(thai);
	//
	// for (int n = 0; n < nonPerFile && non.size() > 0; n++) {
	// nt.add(non.remove(0));
	// }
	//
	// for (int n = 0; n < thaiPerFile && thai.size() > 0; n++) {
	// tt.add(thai.remove(0));
	// }
	//
	// FileUtils.writeTextFile("tmp/non-" + i + ".csv", nt, false);
	// FileUtils.writeTextFile("tmp/thai-" + i + ".csv", tt, false);
	// }
	//
	// FileUtils.rename("tmp/", a + "tmp/");
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// }

	private static void testKFold(int k, String dirPath) {
		ArrayList<String> train = new ArrayList<String>();
		ArrayList<String> trainThai = new ArrayList<String>();
		ArrayList<String> trainNon = new ArrayList<String>();

		ArrayList<String> test = new ArrayList<String>();

		String[] lines;

		double avgLc = 0;
		double avgAc = 0;
		double avgUc = 0;

		double avg = 0;

		// perform k fold
		for (int i = 0; i < k; i++) {

			train.clear();
			test.clear();

			logger.info("=========== fold" + i + "==========");

			for (int j = 0; j < k; j++) {
				if (i == j) {
					lines = FileUtils.readFile(dirPath + "rel-" + i + ".csv");
					for (String s : lines) {
						if(s.trim().length() == 0)continue;
						test.add(s);
					}

					lines = FileUtils.readFile(dirPath + "non-" + i + ".csv");
					for (String s : lines) {
						if(s.trim().length() == 0)continue;
						test.add(s);
					}
				} else {
					lines = FileUtils.readFile(dirPath + "rel-" + j + ".csv");
					for (String s : lines) {
						if(s.trim().length() == 0)continue;
						trainThai.add(s);
					}

					lines = FileUtils.readFile(dirPath + "non-" + j + ".csv");
					for (String s : lines) {
						if(s.trim().length() == 0)continue;
						trainNon.add(s);
					}

				}

			}

			// logger.info(test.size() + "\t" + train.size());

			// undersampling
			System.out.println(trainThai.size() + "\t" + trainNon.size() + "\t");
			train = FeaturesCollectors.underSampling(trainThai, trainNon);
			trainThai.clear();
			trainNon.clear();

			write(test, dirPath + "test.arff", FeaturesExtraction.getHeader());
			write(train, dirPath + "train.arff", FeaturesExtraction.getHeader());
			LinkedBlockingQueue<KFoldTaskResult> results = new LinkedBlockingQueue<KFoldTaskResult>();

			PredictorPool.trainPredictor(dirPath + "train.arff");
			try {

				CountDownLatch cd = new CountDownLatch(test.size());
				for (String instance : test) {
					executor.execute(new kFoldTask(cd, instance, predictorPool, results));
				}

				cd.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			test.clear();
			train.clear();

			// analyze the result
			ArrayList<KFoldTaskResult> rs = new ArrayList<>();
			results.drainTo(rs);

			ConfusionMatrixObj lc = new ConfusionMatrixObj();
			ConfusionMatrixObj ac = new ConfusionMatrixObj();
			ConfusionMatrixObj uc = new ConfusionMatrixObj();

			ConfusionMatrixObj all = new ConfusionMatrixObj();

			for (KFoldTaskResult r : rs) {

				if (r.getRealOutput() == ResultClass.RELEVANT) {

					if (r.getAvgOutput().getResultClass() == ResultClass.RELEVANT) {
						all.incTp();
					} else {
						all.incFn();
					}

					if (r.getLinkOutput() != null && r.getLinkOutput().getResultClass() == ResultClass.RELEVANT) {
						lc.incTp();
					} else {
						lc.incFn();
					}

					if (r.getAnchorOutput() != null && r.getAnchorOutput().getResultClass() == ResultClass.RELEVANT) {
						ac.incTp();
					} else {
						ac.incFn();
					}

					if (r.getUrlOutput() != null && r.getUrlOutput().getResultClass() == ResultClass.RELEVANT) {
						uc.incTp();
					} else {
						uc.incFn();
					}

				} else {

					if (r.getAvgOutput().getResultClass() == ResultClass.RELEVANT) {
						all.incFp();
					} else {
						all.incTn();
					}

					if (r.getLinkOutput() != null && r.getLinkOutput().getResultClass() == ResultClass.RELEVANT) {
						lc.incFp();
					} else {
						lc.incTn();
					}

					if (r.getAnchorOutput() != null && r.getAnchorOutput().getResultClass() == ResultClass.RELEVANT) {
						ac.incFp();
					} else {
						ac.incTn();
					}

					if (r.getUrlOutput() != null && r.getUrlOutput().getResultClass() == ResultClass.RELEVANT) {
						uc.incFp();
					} else {
						uc.incTn();
					}

				}

			}

			// logger.info(lc.toString());
			// logger.info(ac.toString());
			// logger.info(uc.toString());
			// logger.info(all.toString());

			logger.info("Gmean :\t" + all.getGmean() + "\t" + lc.getGmean() + "\t" + ac.getGmean() + "\t" + uc.getGmean());
			avgLc += lc.getGmean();
			avgAc += ac.getGmean();
			avgUc += uc.getGmean();
			avg += all.getGmean();

			results.clear();

		}

		avgLc /= k;
		avgAc /= k;
		avgUc /= k;
		avg /= k;

		logger.info("Average Gmean :\t" + avg + "\t" + avgLc + "\t" + avgAc + "\t" + avgUc);
		// CrawlerConfig.getConfig().setClassifierWeights(new double[] { avgLc,
		// avgAc, avgUc });

		logger.info("Finished");
	}

	public static void separate(String trainingSinglePath) {

		try {
			BufferedWriter bwt = FileUtils.getBufferedFileWriter("features_relSrc.arff");
			BufferedWriter bwn = FileUtils.getBufferedFileWriter("features_nonSrc.arff");

			bwt.write(FeaturesExtraction.getHeader());
			bwn.write(FeaturesExtraction.getHeader());

			for (String[] s : FileUtils.readArffData(trainingSinglePath)) {

				if (s.length <= 1)
					continue;

				try {
					s[0] = s[0].replace("{", "").replace(" ", "").replace("}", "").replace(",", "").replace("%", "").replace("'", "").replace("`", "").replace("\"", "");
					String features = s[0] + "," + s[1] + "," + s[2] + "," + s[3] + "," + s[4] + "," + s[5] + "," + s[6] + "," + s[7] + "," + s[8] + "," + s[9] + "," + s[10] + "," + s[11] + ","
							+ s[12] + "\n";

					if (s[9].equals("0")) {
						bwt.write(features);
					} else {
						bwn.write(features);
					}

				} catch (Exception e) {
					System.out.println(Arrays.deepToString(s));
				}
			}

			bwt.close();
			bwn.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void buildTrainingFile(String dirPath, String outputPath, int k) {

		ArrayList<String> rel = new ArrayList<>();
		ArrayList<String> non = new ArrayList<>();

		String[] lines;
		for (int j = 0; j < k; j++) {
			lines = FileUtils.readFile(dirPath + "/rel-" + j + ".csv");
			for (String s : lines) {
				rel.add(s);
			}

			lines = FileUtils.readFile(dirPath + "/non-" + j + ".csv");
			for (String s : lines) {
				non.add(s);
			}

		}

		// logger.info(test.size() + "\t" + train.size());
		System.out.println(rel.size() + "\t" + non.size());
		// undersampling
		ArrayList<String> train = FeaturesCollectors.underSampling(rel, non);

		write(train, outputPath, FeaturesExtraction.getHeader());

	}

	// public static void clean(String path) {
	// String[] lines = FileUtils.readFile(path);
	// ArrayList<String> output = new ArrayList<>();
	// HashSet<String> dup = new HashSet<>();
	// boolean isData = false;
	// for (String s : lines) {
	//
	// if (s.contains("@data")) {
	// isData = true;
	// output.add(s);
	// continue;
	// }
	//
	// if (!isData) {
	// output.add(s);
	// continue;
	// }
	//
	//
	// if(s.split(",")[0].contains(".blogspot."))
	// continue;
	//
	// String md5 = StringUtils.hashSegmentFeatures(s);
	// if (dup.contains(md5))
	// continue;
	//
	// dup.add(md5);
	// output.add(s);
	//
	// }
	//
	// FileUtils.writeTextFile(path + ".clean", output, false);
	// }

	public static void main(String[] args) {
//		 PredictorPool.buildTrainingFile("1tmp", "gaming-page.arff", 3);
//		 PredictorPoolMulti.buildTrainingFile("5tmp", "gaming.arff", 3);
//		 PredictorPool.buildTrainingFile("5tmp", "gaming.arff", 3);
			
		ParseLog.parsePredictorPoolLog("predictor-tourism-seg/logs-predictor-single-tourism/");

//		FileUtils.cleanArffData("all.arff", FeaturesExtraction.getHeader());
//		separate("all.arff");
		System.exit(0);
		int k = 10;

		for (int i = 0; i < 1; i++) {
			PredictorPoolMulti.prepareKFold("features_relSrc.arff", "features_nonSrc.arff", k, i);
			PredictorPoolMulti.testKFold(k, i + "tmp/");
//			testKFold(k, i + "tmp/");
		}
		
		ParseLog.parsePredictorPoolLog("logs");

		System.exit(0);

	}

}
