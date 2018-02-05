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

public class PredictorPoolMulti {

	private static Logger logger = Logger.getLogger(PredictorPoolMulti.class);

	private static ThreadPoolExecutor executor;
	private static int numThreads = 12;
	private static boolean isInit = false;

	private static boolean useAdaptiveWeighting = true;

	private static LinkedBlockingQueue<Predictor> relPredictorPool = new LinkedBlockingQueue<>();

	private static LinkedBlockingQueue<Predictor> nonPredictorPool = new LinkedBlockingQueue<>();

	// private static LinkedBlockingQueue<WekaClassifier> linkRelPredictorPool =
	// new LinkedBlockingQueue<>();
	// private static LinkedBlockingQueue<WekaClassifier> anchorRelPredictorPool
	// = new LinkedBlockingQueue<>();
	// private static LinkedBlockingQueue<WekaClassifier> urlRelPredictorPool =
	// new LinkedBlockingQueue<>();
	//
	// private static LinkedBlockingQueue<WekaClassifier> linkNonPredictorPool =
	// new LinkedBlockingQueue<>();
	// private static LinkedBlockingQueue<WekaClassifier> anchorNonPredictorPool
	// = new LinkedBlockingQueue<>();
	// private static LinkedBlockingQueue<WekaClassifier> urlNonPredictorPool =
	// new LinkedBlockingQueue<>();

	// TODO: writing code to update weight adaptively

	static {
		executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
	}

	public static synchronized void predict(ArrayList<WebsiteSegment> instances, SegmentFrontier frontier) throws Exception {
		if (!isInit)
			throw new Exception("predictor pool is empty");

		CountDownLatch cd = new CountDownLatch(instances.size());
		for (WebsiteSegment instance : instances) {
			if (instance.getSrcRelDegree() > CrawlerConfig.getConfig().getRelevanceDegreeThreshold())
				executor.execute(new PredictionTask(cd, instance, relPredictorPool));
			else
				executor.execute(new PredictionTask(cd, instance, nonPredictorPool));
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
			if (Double.parseDouble(instance[1]) > CrawlerConfig.getConfig().getRelevanceDegreeThreshold())
				executor.execute(new PredictionTaskForFeatureAnalyzer(cd, instance, relPredictorPool));
			else
				executor.execute(new PredictionTaskForFeatureAnalyzer(cd, instance, nonPredictorPool));
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

		int minIndex = 0;
		int min = (int) precisionWeights[0] * 100;

		for (int i = 0; i < precisionWeights.length; i++) {
			values[i] = (int) precisionWeights[i] * 100;

			if (min > precisionWeights[i]) {
				min = values[i];
				minIndex = i;
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

	public static synchronized void onlineUpdatePredictor(ArrayList<String> instancesFromRelSrc, ArrayList<String> instancesFromNonSrc) throws Exception {

		double[] weightRelClassifiers = null;
		double[] weightNonClassifiers = null;

		if (useAdaptiveWeighting) {
			weightRelClassifiers = new double[] { AccuracyTracker.calcWeight(AccuracyTracker.getLinkRelConfusionMatrix().getAccuracy()),
					AccuracyTracker.calcWeight(AccuracyTracker.getAnchorRelConfusionMatrix().getAccuracy()), AccuracyTracker.calcWeight(AccuracyTracker.getUrlRelConfusionMatrix().getAccuracy()),
					AccuracyTracker.calcWeight(AccuracyTracker.getHistoryConfusionMatrix().getAccuracy()) };
			weightNonClassifiers = new double[] { AccuracyTracker.calcWeight(AccuracyTracker.getLinkNonConfusionMatrix().getAccuracy()),
					AccuracyTracker.calcWeight(AccuracyTracker.getAnchorNonConfusionMatrix().getAccuracy()), AccuracyTracker.calcWeight(AccuracyTracker.getUrlNonConfusionMatrix().getAccuracy()),
					AccuracyTracker.calcWeight(AccuracyTracker.getHistoryConfusionMatrix().getAccuracy()) };
		} else {
			weightRelClassifiers = CrawlerConfig.getConfig().getClassifierWeights();
			weightNonClassifiers = CrawlerConfig.getConfig().getClassifierWeights();
		}

		assert (weightRelClassifiers != null);
		assert (weightNonClassifiers != null);

		logger.info("Weights rel classifiers: " + Arrays.toString(weightRelClassifiers));

		if (instancesFromRelSrc != null) {

			FileUtils.writeTextFile("logs/tmp.arff", FeaturesExtraction.getHeader(), false);
			FileUtils.writeTextFile("logs/tmp.arff", instancesFromRelSrc, true);

			Instances newDataset = new Instances(new FileReader("logs/tmp.arff"));
			newDataset.setClassIndex(newDataset.numAttributes() - 1);

			for (Predictor p : relPredictorPool) {
				p.getLinkClassifier().updateClassifier(newDataset);
				p.getLinkClassifier().setWeight(weightRelClassifiers[0]);

				p.getAnchorClassifier().updateClassifier(newDataset);
				p.getAnchorClassifier().setWeight(weightRelClassifiers[1]);

				p.getUrlClassifier().updateClassifier(newDataset);
				p.getUrlClassifier().setWeight(weightRelClassifiers[2]);
			}

		}

		logger.info("Weights non classifiers: " + Arrays.toString(weightNonClassifiers));
		if (instancesFromNonSrc != null) {

			FileUtils.writeTextFile("logs/tmp2.arff", FeaturesExtraction.getHeader(), false);
			FileUtils.writeTextFile("logs/tmp2.arff", instancesFromNonSrc, true);

			Instances newDataset2 = new Instances(new FileReader("logs/tmp2.arff"));
			newDataset2.setClassIndex(newDataset2.numAttributes() - 1);

			for (Predictor p : nonPredictorPool) {
				p.getLinkClassifier().updateClassifier(newDataset2);
				p.getLinkClassifier().setWeight(weightNonClassifiers[0]);

				p.getAnchorClassifier().updateClassifier(newDataset2);
				p.getAnchorClassifier().setWeight(weightNonClassifiers[1]);

				p.getUrlClassifier().updateClassifier(newDataset2);
				p.getUrlClassifier().setWeight(weightNonClassifiers[2]);

			}

		}

		FileUtils.deleteFile("logs/tmp.arff");
		FileUtils.deleteFile("logs/tmp2.arff");
	}

	public static void trainPredictor(String relTrainPath, String nonTrainPath) {

		double[] weightRelClassifiers = CrawlerConfig.getConfig().getClassifierWeights();
		double[] weightNonClassifiers = CrawlerConfig.getConfig().getClassifierWeights();

		boolean isKnn = false;

		try {

			relPredictorPool.clear();
			nonPredictorPool.clear();

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

			WekaClassifier linkRel = new WekaClassifier(algo, option, weightRelClassifiers[0]);
			WekaClassifier linkNon = new WekaClassifier(algo, option, weightNonClassifiers[0]);

			if (isKnn) {
				linkRel.train(relTrainPath, rm, false, null, true, false);
				linkNon.train(nonTrainPath, rm, false, null, true, false);
			} else if (algo.contains("NaiveBayes")) {
				if (option.contains("-K")) {
					linkRel.train(relTrainPath, rm, false, null, true, false);
					linkNon.train(nonTrainPath, rm, false, null, true, false);
				} else {
					linkRel.train(relTrainPath, rm, true, null, true, false);
					linkNon.train(nonTrainPath, rm, true, null, true, false);
				}
			}

			rm = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12 };

			logger.info("=========Anchor==========");
			algo = "weka.classifiers.bayes.NaiveBayesMultinomialText";

			if (CrawlerConfig.getConfig().getTargetLang().equals("ja"))
				option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.CharacterNgramTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";
			else
				option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.MyTextTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";

			WekaClassifier anchorRel = new WekaClassifier(algo, option, weightRelClassifiers[1]);
			WekaClassifier anchorNon = new WekaClassifier(algo, option, weightNonClassifiers[1]);

			anchorRel.train(relTrainPath, rm, false, null, true, false);
			anchorNon.train(nonTrainPath, rm, false, null, true, false);

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

			WekaClassifier urlRel = new WekaClassifier(algo, option, weightRelClassifiers[2]);
			WekaClassifier urlNon = new WekaClassifier(algo, option, weightNonClassifiers[2]);

			urlRel.train(relTrainPath, rm, false, null, true, false);
			urlNon.train(nonTrainPath, rm, false, null, true, false);

			Predictor pRel = new Predictor(linkRel, anchorRel, urlRel);
			Predictor pNon = new Predictor(linkNon, anchorNon, urlNon);
			// clear old predictor
			for (int i = 0; i < numThreads; i++) {
				relPredictorPool.add((Predictor) FileUtils.deepClone(pRel));
				nonPredictorPool.add((Predictor) FileUtils.deepClone(pNon));
			}

			logger.info("finished loading predictor models");

		} catch (Exception e) {
			e.printStackTrace();
		}

		isInit = true;

	}

	public static void prepareKFold(String trainRelPath, String trainNonPath, int k, int a) {
		FileUtils.cleanArffData(trainRelPath, FeaturesExtraction.getHeader());
		FileUtils.cleanArffData(trainNonPath, FeaturesExtraction.getHeader());

		
		System.out.println("Preparing training rel ");
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(trainRelPath)))) {
			ArrayList<String> rel = new ArrayList<>();
			ArrayList<String> non = new ArrayList<>();

			String tmp;
			String[] data;
			boolean isData = false;
			while ((tmp = br.readLine()) != null) {
				if (tmp.contains("@data")) {
					isData = true;
					continue;
				}

				if (!isData)
					continue;

				data = tmp.split(",");

				String url = data[0];
				if (url.contains("{") || url.contains("}") || url.contains("\"")) {
					continue;
				}

				// if(url.length() > 2)
				// url = url.substring(1, url.lastIndexOf("'"));

				if (HtmlParser.shouldFilter(url)) {
					continue;
				}

				if (data[data.length - 1].trim().equals("non")) {
					non.add(tmp);
				} else {
					rel.add(tmp);
				}

			}

			int nonPerFile = (int) Math.ceil(non.size() * 1.0 / k);
			int thaiPerFile = (int) Math.ceil(rel.size() * 1.0 / k);

			FileUtils.mkdir("tmp");
			for (int i = 0; i < k; i++) {

				ArrayList<String> tt = new ArrayList<>();
				ArrayList<String> nt = new ArrayList<>();

				Collections.shuffle(non);
				Collections.shuffle(rel);

				for (int n = 0; n < nonPerFile && non.size() > 0; n++) {
					nt.add(non.remove(0));
				}

				for (int n = 0; n < thaiPerFile && rel.size() > 0; n++) {
					tt.add(rel.remove(0));
				}

				System.out.println("Thai size " + tt.size());
				System.out.println("Non size " + nt.size());

				FileUtils.writeTextFile("tmp/non-rel-" + i + ".csv", nt, false);
				FileUtils.writeTextFile("tmp/rel-rel-" + i + ".csv", tt, false);

				FileUtils.writeTextFile("tmp/non-" + i + ".csv", nt, false);
				FileUtils.writeTextFile("tmp/rel-" + i + ".csv", tt, false);
			}

			// FileUtils.deleteDir("?tmp");
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("-------");
		System.out.println("Preparing training non");
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(trainNonPath)))) {
			ArrayList<String> rel = new ArrayList<>();
			ArrayList<String> non = new ArrayList<>();

			String tmp;
			String[] data;
			boolean isData = false;
			while ((tmp = br.readLine()) != null) {
				if (tmp.contains("@data")) {
					isData = true;
					continue;
				}

				if (!isData)
					continue;

				data = tmp.split(",");

				String url = data[0];

				if (url.contains("{") || url.contains("}") || url.contains("\"")) {
					continue;
				}

				if (HtmlParser.shouldFilter(url)) {
					continue;
				}

				if (data[data.length - 1].trim().equals("non")) {
					non.add(tmp);
				} else {
					rel.add(tmp);
				}

			}

			int nonPerFile = (int) Math.ceil(non.size() * 1.0 / k);
			int thaiPerFile = (int) Math.ceil(rel.size() * 1.0 / k);
			// System.err.println("Thai size " + rel.size() + "\t" +
			// thaiPerFile);
			// System.err.println("Non size " + non.size() + "\t" + nonPerFile);

			FileUtils.mkdir("tmp");
			for (int i = 0; i < k; i++) {

				ArrayList<String> tt = new ArrayList<>();
				ArrayList<String> nt = new ArrayList<>();

				Collections.shuffle(non);
				Collections.shuffle(rel);

				for (int n = 0; n < nonPerFile && non.size() > 0; n++) {
					nt.add(non.remove(0));
				}

				for (int n = 0; n < thaiPerFile && rel.size() > 0; n++) {
					tt.add(rel.remove(0));
				}

				System.out.println("Thai size " + tt.size());
				System.out.println("Non size " + nt.size());

				FileUtils.writeTextFile("tmp/non-non-" + i + ".csv", nt, false);
				FileUtils.writeTextFile("tmp/rel-non-" + i + ".csv", tt, false);

				FileUtils.writeTextFile("tmp/non-" + i + ".csv", nt, true);
				FileUtils.writeTextFile("tmp/rel-" + i + ".csv", tt, true);
			}

			// FileUtils.deleteDir("?tmp");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// try {
		// DataSource data = new DataSource(trainNonPath);
		// Instances in = data.getDataSet();
		// ArrayList<String> thai = new ArrayList<>();
		// ArrayList<String> non = new ArrayList<>();
		//
		// in.setClassIndex(in.numAttributes() - 1);
		// for (int j = 0; j < in.numInstances(); j++) {
		//
		// String host = HttpUtils.getHost(in.instance(j).stringValue(0));
		// if (FeaturesExtraction.blackListHost.contains(host.replace("www.",
		// ""))
		// || in.instance(j).stringValue(0).toLowerCase().contains("utah")
		// || in.instance(j).stringValue(0).toLowerCase().contains("gay")
		// || in.instance(j).stringValue(0).toLowerCase().contains("dubai")
		// || in.instance(j).stringValue(0).toLowerCase().contains("qatar")
		// || in.instance(j).stringValue(0).toLowerCase().contains("bahrain")
		// || in.instance(j).stringValue(0).toLowerCase().contains("cyprus")
		// ||
		// in.instance(j).stringValue(0).toLowerCase().contains("greatestdivesites")
		// || in.instance(j).stringValue(0).toLowerCase().contains("croatia")
		// ||
		// in.instance(j).stringValue(0).toLowerCase().contains("politiken.dk")
		// || in.instance(j).stringValue(0).toLowerCase().contains("australia")
		// || in.instance(j).stringValue(0).toLowerCase().contains("lasvegas"))
		// continue;
		//
		// // if(in.instance(i).classAttribute().toString())
		// if (in.instance(j).stringValue(in.numAttributes() - 1).equals("non"))
		// {
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
		//
		// System.out.println("Thai size " + tt.size());
		// System.out.println("Non size " + nt.size());
		//
		// FileUtils.writeTextFile("tmp/non-non-" + i + ".csv", nt, false);
		// FileUtils.writeTextFile("tmp/thai-non-" + i + ".csv", tt, false);
		//
		// FileUtils.writeTextFile("tmp/non-" + i + ".csv", nt, true);
		// FileUtils.writeTextFile("tmp/thai-" + i + ".csv", tt, true);
		// }
		//
		//// testKFold(k, "tmp/");
		//// FileUtils.deleteDir("?tmp");
		//
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		FileUtils.rename("tmp", a + "tmp");

	}

	public static void testKFold(int k, String dirPath) {

		ArrayList<String> trainRelSrc = new ArrayList<String>();
		ArrayList<String> test = new ArrayList<String>();

		ArrayList<String> trainNonSrc = new ArrayList<String>();

		String[] lines;

		double avgLc = 0;
		double avgAc = 0;
		double avgUc = 0;
		double avg = 0;
		double avgLcn = 0;
		double avgAcn = 0;
		double avgUcn = 0;
		// perform k fold
		for (int i = 0; i < k; i++) {

			trainRelSrc.clear();
			test.clear();

			ArrayList<String> rel = new ArrayList<String>();
			ArrayList<String> non = new ArrayList<String>();

			rel.clear();
			non.clear();

			logger.info("=========== fold" + i + "==========");

			for (int j = 0; j < k; j++) {
				if (i == j) {
					lines = FileUtils.readFile(dirPath + "rel-rel-" + i + ".csv");
					for (String s : lines) {
						if(s.trim().length() == 0)
							continue;
						
						test.add(s);
					}

					lines = FileUtils.readFile(dirPath + "non-rel-" + i + ".csv");
					for (String s : lines) {
						if(s.trim().length() == 0)
							continue;
						
						test.add(s);
					}
				} else {
					lines = FileUtils.readFile(dirPath + "rel-rel-" + j + ".csv");
					for (String s : lines) {
						if(s.trim().length() == 0)
							continue;
						
						rel.add(s);
					}

					lines = FileUtils.readFile(dirPath + "non-rel-" + j + ".csv");
					for (String s : lines) {
						if(s.trim().length() == 0)
							continue;
						
						non.add(s);
					}

				}

			}

			logger.info(rel.size() + "\t" + non.size());
			trainRelSrc = FeaturesCollectors.underSampling(rel, non);
			logger.info(trainRelSrc.size());
			rel.clear();
			non.clear();

			for (int j = 0; j < k; j++) {
				if (i == j) {
					lines = FileUtils.readFile(dirPath + "rel-non-" + i + ".csv");
					for (String s : lines) {
						if(s.trim().length() == 0)
							continue;
						
						test.add(s);
					}

					lines = FileUtils.readFile(dirPath + "non-non-" + i + ".csv");
					for (String s : lines) {
						if(s.trim().length() == 0)
							continue;
						
						test.add(s);
					}
				} else {
					lines = FileUtils.readFile(dirPath + "rel-non-" + j + ".csv");
					for (String s : lines) {
						if(s.trim().length() == 0)
							continue;
						
						rel.add(s);
					}

					lines = FileUtils.readFile(dirPath + "non-non-" + j + ".csv");
					for (String s : lines) {
						if(s.trim().length() == 0)
							continue;
						
						non.add(s);
					}

				}

			}

			logger.info(rel.size() + "\t" + non.size());
			trainNonSrc = FeaturesCollectors.underSampling(rel, non);

			logger.info(trainNonSrc.size());
			// logger.info(test.size() + "\t" + train.size());

			write(test, dirPath + "test.arff", FeaturesExtraction.getHeader());
			write(trainRelSrc, dirPath + "train-rel.arff", FeaturesExtraction.getHeader());
			write(trainNonSrc, dirPath + "train-non.arff", FeaturesExtraction.getHeader());
			LinkedBlockingQueue<KFoldTaskResult> results = new LinkedBlockingQueue<KFoldTaskResult>();

			PredictorPoolMulti.trainPredictor(dirPath + "train-rel.arff", dirPath + "train-non.arff");

			try {

				CountDownLatch cd = new CountDownLatch(test.size());
				int count = 0;
				for (String instance : test) {

					try {
						String[] data = instance.split(",");
						count++;
						if (Double.parseDouble(data[1]) > CrawlerConfig.getConfig().getRelevanceDegreeThreshold())
							executor.execute(new kFoldTask(cd, instance, relPredictorPool, results));
						else
							executor.execute(new kFoldTask(cd, instance, nonPredictorPool, results));
					} catch (Exception e) {
						System.out.println(instance +"\t" + count);
						e.printStackTrace();
					}
				}

				cd.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// analyze the result
			ArrayList<KFoldTaskResult> rs = new ArrayList<>();
			results.drainTo(rs);

			ConfusionMatrixObj lc = new ConfusionMatrixObj();
			ConfusionMatrixObj ac = new ConfusionMatrixObj();
			ConfusionMatrixObj uc = new ConfusionMatrixObj();

			ConfusionMatrixObj lcn = new ConfusionMatrixObj();
			ConfusionMatrixObj acn = new ConfusionMatrixObj();
			ConfusionMatrixObj ucn = new ConfusionMatrixObj();

			ConfusionMatrixObj all = new ConfusionMatrixObj();

			for (KFoldTaskResult r : rs) {

				boolean isSrcRel = r.getFeatures()[9].equals("0");

				if (r.getRealOutput() == ResultClass.RELEVANT) {

					// in case that src is relevant
					if (isSrcRel) {

						// classifier predict as relevant
						if (WekaClassifier.average(r.getLinkOutput(), r.getAnchorOutput(), r.getUrlOutput()).getResultClass() == ResultClass.RELEVANT) {
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
						// src is tunnel node

						if (WekaClassifier.average(r.getLinkOutput(), r.getAnchorOutput(), r.getUrlOutput()).getResultClass() == ResultClass.RELEVANT) {
							all.incTp();
						} else {
							all.incFn();
						}

						if (r.getLinkOutput() != null && r.getLinkOutput().getResultClass() == ResultClass.RELEVANT) {
							lcn.incTp();
						} else {
							lcn.incFn();
						}

						if (r.getAnchorOutput() != null && r.getAnchorOutput().getResultClass() == ResultClass.RELEVANT) {
							acn.incTp();
						} else {
							acn.incFn();
						}

						if (r.getUrlOutput() != null && r.getUrlOutput().getResultClass() == ResultClass.RELEVANT) {
							ucn.incTp();
						} else {
							ucn.incFn();
						}

					}

				} else {
					// destination is irrelevant

					// src is relvant
					if (isSrcRel) {

						// predict as relevant
						if (WekaClassifier.average(r.getLinkOutput(), r.getAnchorOutput(), r.getUrlOutput()).getResultClass() == ResultClass.RELEVANT) {
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

					} else {
						// source is irrelevant

						if (WekaClassifier.average(r.getLinkOutput(), r.getAnchorOutput(), r.getUrlOutput()).getResultClass() == ResultClass.RELEVANT) {
							all.incFp();
						} else {
							all.incTn();
						}

						if (r.getLinkOutput() != null && r.getLinkOutput().getResultClass() == ResultClass.RELEVANT) {
							lcn.incFp();
						} else {
							lcn.incTn();
						}

						if (r.getAnchorOutput() != null && r.getAnchorOutput().getResultClass() == ResultClass.RELEVANT) {
							acn.incFp();
						} else {
							acn.incTn();
						}

						if (r.getUrlOutput() != null && r.getUrlOutput().getResultClass() == ResultClass.RELEVANT) {
							ucn.incFp();
						} else {
							ucn.incTn();
						}

					}

				}

			}

			// logger.info(lc.toString());
			// logger.info(ac.toString());
			// logger.info(uc.toString());
			//
			// logger.info(lcn.toString());
			// logger.info(acn.toString());
			// logger.info(ucn.toString());
			// logger.info(all.toString());

			logger.info("Gmean :\t" + all.getGmean() + "\t" + lc.getGmean() + "\t" + ac.getGmean() + "\t" + uc.getGmean() + "\t" + lcn.getGmean() + "\t" + acn.getGmean() + "\t" + ucn.getGmean());

			avgLc += lc.getGmean();
			avgAc += ac.getGmean();
			avgUc += uc.getGmean();
			avgLcn += lcn.getGmean();
			avgAcn += acn.getGmean();
			avgUcn += ucn.getGmean();

			avg += all.getGmean();

			results.clear();

		}

		avgLc /= k;
		avgAc /= k;
		avgUc /= k;

		avgLcn /= k;
		avgAcn /= k;
		avgUcn /= k;

		avg /= k;
		logger.info("Average Gmean :\t" + avg + "\t" + avgLc + "\t" + avgAc + "\t" + avgUc + "\t" + avgLcn + "\t" + avgAcn + "\t" + avgUcn);
		logger.info("Finished");
	}

	public static void separate(String trainingSinglePath) {

		try {
			BufferedWriter bwt = FileUtils.getBufferedFileWriter("training_relSrc.arff");
			BufferedWriter bwn = FileUtils.getBufferedFileWriter("training_nonSrc.arff");

			bwt.write(FeaturesExtraction.getHeader());
			bwn.write(FeaturesExtraction.getHeader());

			for (String[] s : FileUtils.readArffData(trainingSinglePath)) {
				String features = s[0] + "," + s[1] + "," + s[2] + "," + s[3] + "," + s[4] + "," + s[5] + "," + s[6] + "," + s[7] + "," + s[8] + "," + s[9] + "," + s[10] + "," + s[11] + "," + s[12]
						+ "\n";
				if (s[9].equals("0")) {
					bwt.write(features);
				} else {
					bwn.write(features);
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
		ArrayList<String> relFromRel = new ArrayList<>();
		ArrayList<String> nonFromRel = new ArrayList<>();

		ArrayList<String> relFromNon = new ArrayList<>();
		ArrayList<String> nonFromNon = new ArrayList<>();

		String[] lines;
		for (int i = 0; i < k; i++) {
			lines = FileUtils.readFile(dirPath + "/rel-rel-" + i + ".csv");
			for (String s : lines) {
				relFromRel.add(s);
			}

			lines = FileUtils.readFile(dirPath + "/non-rel-" + i + ".csv");
			for (String s : lines) {
				nonFromRel.add(s);
			}

			lines = FileUtils.readFile(dirPath + "/rel-non-" + i + ".csv");
			for (String s : lines) {
				relFromNon.add(s);
			}

			lines = FileUtils.readFile(dirPath + "/non-non-" + i + ".csv");
			for (String s : lines) {
				nonFromNon.add(s);
			}

		}

		System.out.println("-----------");
		System.out.println(relFromRel.size() + "\t" + relFromNon.size());
		System.out.println(nonFromRel.size() + "\t" + nonFromNon.size());

		write(FeaturesCollectors.underSampling(relFromRel, nonFromRel), outputPath.substring(0, outputPath.indexOf(".arff")) + "_rel.arff", FeaturesExtraction.getHeader());
		write(FeaturesCollectors.underSampling(relFromNon, nonFromNon), outputPath.substring(0, outputPath.indexOf(".arff")) + "_non.arff", FeaturesExtraction.getHeader());

	}

	public static void main(String[] args) {
		// buildTrainingFile("6tmp/", "dive.arff", 5);
		// System.exit(0);

//		separate("all.arff");
//		System.exit(0);
		// int k = Integer.parseInt(args[0]);
		// int k = Integer.parseInt(args[0]);
		int k = 3;
		for (int i = 0; i < 10; i++)
			prepareKFold("training_relSrc.arff", "training_nonSrc.arff", k, i);

		for (int i = 0; i < 10; i++)
			testKFold(k, i + "tmp/");

		ParseLog.parsePredictorPoolLog("logs");

		// buildTrainingFile("5tmp", "tourism.arff");

	}

}
