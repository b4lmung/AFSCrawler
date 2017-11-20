package com.job.ic.ml.classifiers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.utils.FileUtils;

public class TestPageClassifierPool {

	private static Logger logger = Logger.getLogger(TestPageClassifierPool.class);

	private static ThreadPoolExecutor executor;
	private static int numThreads = Math.min(CrawlerConfig.getConfig().getNumThreads() * 2, 8);
//	private static boolean isInit = false;

	private static LinkedBlockingQueue<WekaClassifier> pages = new LinkedBlockingQueue<>();

	// TODO: writing code to update weight adaptively

	private static ConfusionMatrixObj all = new ConfusionMatrixObj();

	static {
		executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
	}

	public static void terminatePredictorPool() {
		pages.clear();
//		isInit = false;

		executor.shutdown();
	}

	public static ConfusionMatrixObj getConfusionMatrix() {
		return all;
	}

	public static void shutdown() {
		executor.shutdown();
	}

	// public static synchronized void predict(ArrayList<String[]> instances)
	// throws Exception {
	// if (!isInit)
	// throw new Exception("predictor pool is empty");
	//
	// logger.info("#instances :" + instances.size());
	// CountDownLatch cd = new CountDownLatch(instances.size());
	// for (String[] instance : instances) {
	//// executor.execute();
	// }
	//
	// cd.await();
	// logger.info("finished");
	// }

	public static void trainClassifier(String path, String targetLang) {
		
		pages.clear();
		
		int[] rm = { 1 };// {1,4,5,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24};

		String algo = "weka.classifiers.bayes.NaiveBayesMultinomialText";

		String option = null;
		try {

			if (targetLang.equals("en"))
				option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -lowercase -stopwords-handler weka.core.stopwords.Rainbow -tokenizer \"com.job.ic.ml.classifiers.MyTextTokenizer\" -stemmer weka.core.stemmers.IteratedLovinsStemmer";
			else if (targetLang.equals("ja"))
				option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -lowercase -stopwords-handler weka.core.stopwords.Rainbow -tokenizer \"com.job.ic.ml.classifiers.MyTextTokenizerJp\" -stemmer weka.core.stemmers.NullStemmer";
			else
				throw new Exception("unknown language " + targetLang);

		} catch (Exception e) {
			e.printStackTrace();
		}

		WekaClassifier checker = new WekaClassifier(algo, option);
		checker.train(path, rm, false, null, true, false);

		for (int i = 0; i < numThreads; i++) {
			pages.add((WekaClassifier) FileUtils.deepClone(checker));
		}
	}

	public static void prepareKFold(String trainPath, int k, int totalTimes) {

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(trainPath)))) {

			String tmp;
			String[] data;
			boolean isData = false;
			ArrayList<String> thai = new ArrayList<>();
			ArrayList<String> non = new ArrayList<>();

			while ((tmp = br.readLine()) != null) {

				if (tmp.contains("@data")) {
					isData = true;
					continue;
				}

				if (!isData)
					continue;

				data = tmp.split(",");

				if (data[data.length - 1].equals("non")) {
					non.add(tmp);
				} else {
					thai.add(tmp);
				}
			}

			logger.info("finished reading data");

			int nonPerFile = (int) Math.ceil(non.size() * 1.0 / k);
			int thaiPerFile = (int) Math.ceil(thai.size() * 1.0 / k);

			for (int a = 0; a < totalTimes; a++) {
				
				logger.info("preparing " + k + " folds @" + a);
				FileUtils.mkdir(a + "tmp");
				for (int i = 0; i < k; i++) {

					ArrayList<String> tt = new ArrayList<>();
					ArrayList<String> nt = new ArrayList<>();

					Collections.shuffle(non);
					Collections.shuffle(thai);

					for (int n = 0; n < nonPerFile && non.size() > 0; n++) {
						nt.add(non.get(n));
					}

					for (int n = 0; n < thaiPerFile && thai.size() > 0; n++) {
						tt.add(thai.get(n));
					}

					FileUtils.writeTextFile(a + "tmp/non-" + i + ".csv", nt, false);
					FileUtils.writeTextFile(a + "tmp/thai-" + i + ".csv", tt, false);
					nt.clear();
					tt.clear();
				}

//				FileUtils.rename("tmp/", a + "tmp/");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

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

	private static void testKFold(int k, String dirPath) {
		
		if(dirPath.charAt(dirPath.length()-1) != '/' || dirPath.charAt(dirPath.length()-1) != '\\' ){
			if(System.getProperty("os.name").toLowerCase().contains("win"))
				dirPath += "\\";
			else
				dirPath += "/";
		}
		
		ArrayList<String> train = new ArrayList<String>();
		ArrayList<String> trainThai = new ArrayList<String>();
		ArrayList<String> trainNon = new ArrayList<String>();

		ArrayList<String> test = new ArrayList<String>();

		String[] lines;

		double avg = 0;

		// perform k fold
		for (int i = 0; i < k; i++) {

			train.clear();
			test.clear();

			logger.info("=========== fold" + i + "==========");

			for (int j = 0; j < k; j++) {
				if (i == j) {
					lines = FileUtils.readFile(dirPath + "thai-" + i + ".csv");
					for (String s : lines) {
						test.add(s);
					}

					lines = FileUtils.readFile(dirPath + "non-" + i + ".csv");
					for (String s : lines) {
						test.add(s);
					}
				} else {
					lines = FileUtils.readFile(dirPath + "thai-" + j + ".csv");
					for (String s : lines) {
						trainThai.add(s);
					}

					lines = FileUtils.readFile(dirPath + "non-" + j + ".csv");
					for (String s : lines) {
						trainNon.add(s);
					}

				}

			}

			// logger.info(test.size() + "\t" + train.size());

			// undersampling
			System.out.println(trainThai.size() + "\t" + trainNon.size() + "\t");
			train = underSampling(trainThai, trainNon);

			trainThai.clear();
			trainNon.clear();

			write(test, dirPath + "test.arff", ClassifierUtils.headerPageClassifier);
			write(train, dirPath + "train.arff", ClassifierUtils.headerPageClassifier);
			LinkedBlockingQueue<PageClassifierTaskResult> results = new LinkedBlockingQueue<PageClassifierTaskResult>();

			trainClassifier(dirPath + "train.arff", CrawlerConfig.getConfig().getTargetLang());

			try {

				CountDownLatch cd = new CountDownLatch(test.size());
				for (String instance : test) {
					executor.execute(new PageClassifierTask(pages, cd, instance, results));
				}

				cd.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			test.clear();
			train.clear();

			// analyze the result
			ArrayList<PageClassifierTaskResult> rs = new ArrayList<>();
			results.drainTo(rs);

			ConfusionMatrixObj all = new ConfusionMatrixObj();

			for (PageClassifierTaskResult r : rs) {

				if (r.getRealOutput() == ResultClass.RELEVANT) {

					if (r.getPrediction().getResultClass() == ResultClass.RELEVANT) {
						all.incTp();
					} else {
						all.incFn();
					}

				} else {

					if (r.getPrediction().getResultClass() == ResultClass.RELEVANT) {
						all.incFp();
					} else {
						all.incTn();
					}

				}

			}

			logger.info("Gmean :\t" + all.getGmean());

			results.clear();

		}

		avg /= k;

		logger.info("Average Gmean :\t" + avg);

		logger.info("Finished");
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

	public static void separate(String trainingSinglePath) {

		try {
			BufferedWriter bwt = FileUtils.getBufferedFileWriter("features_relSrc.arff");
			BufferedWriter bwn = FileUtils.getBufferedFileWriter("features_nonSrc.arff");

			bwt.write(ClassifierUtils.headerPageClassifier);
			bwn.write(ClassifierUtils.headerPageClassifier);

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

		ArrayList<String> rel = new ArrayList<>();
		ArrayList<String> non = new ArrayList<>();

		String[] lines;
		for (int j = 0; j < k; j++) {
			lines = FileUtils.readFile(dirPath + "/thai-" + j + ".csv");
			for (String s : lines) {
				rel.add(s);
			}

			lines = FileUtils.readFile(dirPath + "/non-" + j + ".csv");
			for (String s : lines) {
				non.add(s);
			}

		}

		// logger.info(test.size() + "\t" + train.size());

		// undersampling
		ArrayList<String> train = underSampling(rel, non);

		write(train, outputPath, ClassifierUtils.headerPageClassifier);

	}

	public static void main(String[] args) {

		int k = 10;

		prepareKFold("gaming.arff", k, 10);
		
		for (int i = 0; i < 10; i++) {
			testKFold(k, i + "tmp/");
		}
	}

}
