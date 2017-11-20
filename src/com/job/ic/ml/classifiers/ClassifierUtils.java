package com.job.ic.ml.classifiers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.experiments.ParseLog;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;

import org.apache.log4j.Logger;

import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;

public class ClassifierUtils {

	private static Logger logger = Logger.getLogger(ClassifierUtils.class);

	private static String path = "page-baseball/";

	
	public static String[] config = { "word", "word" };
	private static boolean incRatio = true;
	
	private static boolean linkNb = true;
	private static boolean discritize = true;
	private static int k = 10;

	private static boolean useWeight = false;

	private static String targetLang = "ja";
	
	
	private static double weightLink = 0.872;
	private static double weightAnchor = 0.819;
	private static double weightUrl = 0.818;
	
	private static boolean isPageClassifier = true;

	public static boolean[] classifier = { true, true, true };

	public static ArrayList<WekaClassifier> wcs = new ArrayList<>();
	

	public static String headerPageClassifier = "@relation 'pageClassifier'\n@attribute url string\n@attribute body string\n@attribute title string\n@attribute anchor string\n@attribute class {thai,non}\n@data\n";
	private static String header = headerPageClassifier;


	public static void cleanPageArffData(String input){
		ArrayList<String> output = new ArrayList<>();
		for(String[] tmp : FileUtils.readArffData(input))
		{	
			if(tmp.length <=1)
				continue;
			
			tmp[0] = tmp[0].substring(1, tmp[0].length()-1);
			tmp[0] = StringUtils.cleanUrlDataForPrediction(tmp[0]);
			String f = "";
			
			for(int i=0; i<tmp.length; i++){
				if(i == tmp.length-1)
					f+= tmp[i];
				else
					f+= tmp[i] + ",";
			}
			
			output.add(f);
		}

		FileUtils.writeTextFile(input, headerPageClassifier, false);
		FileUtils.writeTextFile(input, output, true);
	}
	
	public static void main(String[] args) throws Exception {
		ParseLog.parsePageClassifier("logs-page-baseball/ic.log");
		
		System.exit(0);
		
//		buildTrainingFile("0-page-gaming/", "page-gaming.arff", true);
//		CrawlerConfig.loadConfig("crawler.conf");
//		
		kFold(path, 10, true);
		
		System.exit(0);
		int[] rm = { 1 };// {1,4,5,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24};

		String algo = "weka.classifiers.bayes.NaiveBayesMultinomialText";

		
		String option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -lowercase -stopwords-handler weka.core.stopwords.Rainbow -tokenizer \"com.job.ic.ml.classifiers.CharacterNgramTokenizer\" -stemmer weka.core.stemmers.NullStemmer";
		
		WekaClassifier page = new WekaClassifier(algo, option);
		page.train("train.arff", rm, false, null, true, false);

		ArrayList<String> inc = new ArrayList<>();
		for(String[] in: FileUtils.readArffData("test.arff")){
			ClassifierOutput output = page.predict(in[0], in);
			boolean isRel = !in[in.length-1].trim().equals("non");
			ResultClass c;
			if(isRel)
				c = ResultClass.RELEVANT;
			else
				c = ResultClass.IRRELEVANT;
			
			
			if(c != output.getResultClass()){
				inc.add(Arrays.deepToString(in));
			}
			
		}
		
		FileUtils.writeTextFile("false-gram.txt", inc, false);
		
		option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -lowercase -stopwords-handler weka.core.stopwords.Rainbow -tokenizer \"com.job.ic.ml.classifiers.JapaneseTokenizer\" -stemmer weka.core.stemmers.NullStemmer";
		
		page = new WekaClassifier(algo, option);
		page.train("train.arff", rm, false, null, true, false);

		inc.clear();
		for(String[] in: FileUtils.readArffData("test.arff")){
			ClassifierOutput output = page.predict(in[0], in);
			boolean isRel = !in[in.length-1].trim().equals("non");
			ResultClass c;
			if(isRel)
				c = ResultClass.RELEVANT;
			else
				c = ResultClass.IRRELEVANT;
			
			
			if(c != output.getResultClass()){
				inc.add(Arrays.deepToString(in));
			}
			
		}
		
		FileUtils.writeTextFile("false-word.txt", inc, false);
		
		
//		System.exit(0);
//		PageClassifier.trainClassifier("page-tourism-en.arff", "en", "page-tourism.model");
//		buildTrainingFile("0tmp", "test.arff", false);
//		
//		if(true)
//			return;
		
//		
//		kFold(path, 10, false);
//		ParseLog.parsePageClassifier("logs/ic.log");
//		ParseLog.parsePredictorLog("logs/ic.log", 10);
//		buildTrainingFile("6-predictor-diving", "predictor-diving.arff", false);
		// parseLog.parseMLLogv2("logs/ic.log");

//		 buildTrainingFile("predictor-estate/2-predictor-estate/", "training-estate-new.arff", false);
		// String topic = "diving";
		// String path = "C:/Users/b4lmung/Documents/Research/current-research/"
		// + topic + "/";
		// if (false)
		// evaluatePredictor(path + "training-" + topic + ".arff", path +
		// "results/testing.arff");

	}

	
	
	public static void buildTrainingFile(String foldPath, String outputPath, boolean isPageClassifier) {
		File[] fs = FileUtils.getAllFile(foldPath);
		ArrayList<String> thai = new ArrayList<>();
		ArrayList<String> non = new ArrayList<>();

		int countNon = 0;
		for (File f : fs) {
			if (f.isDirectory())
				continue;

			if (f.getName().contains("thai-fold")) {

				for (String tmp : FileUtils.readFile(f.getPath())) {

					thai.add(tmp);

				}
			} else if (f.getName().contains("fold-train")) {
				for (String tmp : FileUtils.readFile(f.getPath())) {

					non.add(tmp);

				}
			} else {
				countNon += FileUtils.readFile(f.getPath()).length;
			}
		}

		System.out.println("Thai: " + thai.size());
		System.out.println("Non: " + non.size());
		System.out.println("TNonL " + countNon);
		String[] header;
		if(isPageClassifier){
			header = ClassifierUtils.headerPageClassifier.split("\n");
		}else{
			header = FileUtils.readResourceFile("/resources/classifiers/header-cf.txt");
		}
		
		try (BufferedWriter bw = FileUtils.getBufferedFileWriter(outputPath)) {

			// write header
			for (String s : header) {
				bw.write(s + "\n");
			}

			for (String s : thai) {
				bw.write(s + "\n");
			}

			for (String s : non) {
				bw.write(s + "\n");
			}

		} catch (Exception e) {

		}
	}

	public static void mergeTrainingFile(String filePath) throws IOException {

		for (int i = 1; i <= 9; i++) {
			if (i == 1) {
				mergeFile(filePath + "thai-fold-0.csv", filePath + "thai-fold-1.csv", filePath + "training-thai.arff", false);
				mergeFile(filePath + "non-fold-train-0.csv", filePath + "non-fold-train-1.csv", filePath + "training-non.arff", false);

			} else {
				mergeFile(filePath + "training-thai.arff", filePath + "thai-fold-" + i + ".csv", filePath + "training-thai.arff.tmp", false);
				mergeFile(filePath + "training-non.arff", filePath + "non-fold-train-" + i + ".csv", filePath + "training-non.arff.tmp", false);
				break;
			}
		}
	}

	public static void kFold(String path, int k, boolean sampling) throws Exception {
		for (int time = 0; time < 1; time++) {
			if (sampling) {
				simpleSampling(k);
				FileUtils.copyDirectory(path, time + "-" + path + "/");
				FileUtils.deleteDir(time + "-" + path + "/thai/");
				FileUtils.deleteDir(time + "-" + path + "/non/");
			}
//			logger.info("Run " + time);
			Test(k, time + "-" + path);
			
		}

	}

	public static void Test(int k, String dirPath) {

		ArrayList<String> train = new ArrayList<String>();
		ArrayList<String> test = new ArrayList<String>();

		String[] lines;

		// perform k fold
		for (int i = 0; i < k; i++) {

			train.clear();
			test.clear();

			logger.info("=========== fold" + i + "==========");

			for (int j = 0; j < k; j++) {
				if (i == j) {
					lines = FileUtils.readFile(dirPath + "thai-fold-" + i + ".csv");
					for (String s : lines) {
						test.add(s);
					}

					lines = FileUtils.readFile(dirPath + "non-fold-" + i + ".csv");
					for (String s : lines) {
						test.add(s);
					}
				} else {
					lines = FileUtils.readFile(dirPath + "thai-fold-" + j + ".csv");
					for (String s : lines) {
						train.add(s);
					}

					lines = FileUtils.readFile(dirPath + "non-fold-train-" + j + ".csv");
					for (String s : lines) {
						train.add(s);
					}

				}

			}


			logger.info(test.size() + "\t" + train.size());
			
			
			if (isPageClassifier) {
				write(test, dirPath + "test.arff", ClassifierUtils.headerPageClassifier);
				write(train, dirPath + "train.arff", ClassifierUtils.headerPageClassifier);
			} else {
				write(test, dirPath + "test.arff", header);
				write(train, dirPath + "train.arff", header);
			}

			double g = 0;
			if (isPageClassifier)
				evaluatePageClassifier(dirPath + "train.arff", dirPath + "test.arff");
			else
				evaluatePredictor(dirPath + "train.arff", dirPath + "test.arff");
		}

	}

	public static double evaluatePageClassifier(String trainPath, String testPath) {
		int[] rm = { 1 };// {1,4,5,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24};

		String algo = "weka.classifiers.bayes.NaiveBayesMultinomialText";

		String option = null;
		// n-gram word
		if (targetLang.equals("en"))
			option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -lowercase -stopwords-handler weka.core.stopwords.Rainbow -tokenizer \"com.job.ic.ml.classifiers.MyTextTokenizer\" -stemmer weka.core.stemmers.IteratedLovinsStemmer";
		else if (targetLang.equals("ja"))
			option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -lowercase -stopwords-handler weka.core.stopwords.Rainbow -tokenizer \"com.job.ic.ml.classifiers.JapaneseTokenizer\" -stemmer weka.core.stemmers.NullStemmer";
		
//		com.job.ic.ml.classifiers.CharacterNgramTokenizer
		WekaClassifier page = new WekaClassifier(algo, option);
		page.train(trainPath, rm, false, null, true, false);

		return page.getGMeanFromTestSet(testPath);

	}
	
	public static synchronized ArrayList<String> underSampling(LinkedBlockingQueue<String> relFeatures,
			LinkedBlockingQueue<String> nonFeatures) {

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

	public static void simpleSampling(int k) throws Exception {

		logger.info(path);
		ArrayList<String> foldThai = new ArrayList<String>();
		ArrayList<String> foldNon = new ArrayList<String>();
		ArrayList<String> foldNonTrain = new ArrayList<String>();

		File[] thaiFiles = FileUtils.getAllFile(path + "thai/");
		File[] nonFiles = FileUtils.getAllFile(path + "non/");

		ArrayList<ArrayList<String>> allThaiData = new ArrayList<>();
		ArrayList<ArrayList<String>> allNonData = new ArrayList<>();

		int countThai = 0;
		int countNon = 0;

		System.out.println("--thai--");
		for (File f : thaiFiles) {
			ArrayList<String> tmp = new ArrayList<>();
			
			FileUtils.cleanArffData(f.getPath(), headerPageClassifier);
			
			
			DataSource ds = new DataSource(f.getPath());
			Instances dataset = ds.getDataSet();
			for (int i = 0; i < dataset.numInstances(); i++) {
				tmp.add(dataset.instance(i).toString());
				countThai++;
			}

			Collections.shuffle(tmp);
			allThaiData.add(tmp);
			tmp = null;
		}

		System.out.println("--non--");
		for (File f : nonFiles) {
			FileUtils.cleanArffData(f.getPath(), headerPageClassifier);
			
			
			ArrayList<String> tmp = new ArrayList<>();
			DataSource ds = new DataSource(f.getPath());
			Instances dataset = ds.getDataSet();
			System.out.println(f.getName());
			try{
			for (int i = 0; i < dataset.numInstances(); i++) {
				tmp.add(dataset.instance(i).toString());
				countNon++;
			}
			}catch(Exception e){
				e.printStackTrace();
				System.out.println(">>" + f.getName());
				System.exit(1);
			}
			Collections.shuffle(tmp);
			allNonData.add(tmp);
			tmp = null;
		}

		// System.out.println("read !!!");

		// quota

		int[] quotaThai = new int[allThaiData.size()];
		for (int i = 0; i < quotaThai.length; i++) {
			quotaThai[i] = (int) Math.floor(1.0*allThaiData.get(i).size() / k);
		}

		int[] quotaNon = new int[allNonData.size()];
		for (int i = 0; i < quotaNon.length; i++) {
			quotaNon[i] = (int) Math.floor(1.0*allNonData.get(i).size() / k);
		}

		int[] quotaNonTraining = new int[allNonData.size()];
		for (int i = 0; i < quotaNonTraining.length; i++) {
			quotaNonTraining[i] = (int) Math.floor(((countThai / k) * (1.0 * allNonData.get(i).size() / countNon)));
		}

		// split data to k fold
		int cc = 0;
		for (int i = 0; i < k; i++) {

			foldThai.clear();
			foldNon.clear();
			foldNonTrain.clear();

			for (int j = 0; j < allThaiData.size(); j++) {
				cc = 0;
				int size = allThaiData.get(j).size();
				for (int z = 0; z < size && z < quotaThai[j]; z++) {
					String data = allThaiData.get(j).remove(0);
					foldThai.add(data);
					cc++;
				}

			}

			for (int j = 0; j < allNonData.size(); j++) {
				cc = 0;

				int size = allNonData.get(j).size();
				for (int z = 0; z < size && z < quotaNon[j]; z++) {
					String data = allNonData.get(j).get(z);
					foldNon.add(data);

					if (cc <= quotaNonTraining[j]) {
						foldNonTrain.add(data);
						cc++;
					}

				}

			}

			write(foldThai, path + "thai-fold-" + i + ".csv", null);
			write(foldNon, path + "non-fold-" + i + ".csv", null);
			write(foldNonTrain, path + "non-fold-train-" + i + ".csv", null);
		}
	}

	public static double evaluatePredictor(String trainPath, String testPath) {

		try {
			int[] rm = { 1, 11, 12 };

			// int[] rm = { 1, 10, 8, 9, 11 };
			// ไม่ใช้ ratio
			if (!incRatio)
				rm = new int[] { 1, 6, 7, 10, 11 };

			String algo;
			String option = null;

			WekaClassifier link = null, anchor = null, url = null;

			int total = 0;
			if (classifier[0]) {
				logger.info("=========Link==========");
				// link based
				algo = "weka.classifiers.bayes.NaiveBayes";

				if (linkNb) {
					link = new WekaClassifier(algo, option);
					link.train(trainPath, rm, true, null, true, false);
				} else {
					algo = "weka.classifiers.lazy.IBk";
					option = "-K " + k + " -W 0";
					link = new WekaClassifier(algo, option);
					link.train(trainPath, rm, discritize, null, true, false);
				}

				// System.out.println(testPath);

				weightLink = link.getGMeanFromTestSet(testPath);
				logger.info("link G-mean:\t" + weightLink);
				total++;
			}

			if (classifier[1]) {
				logger.info("=========Anchor==========");
				// anchor
				rm = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12 };

				algo = "weka.classifiers.bayes.NaiveBayesMultinomialText";

				if (CrawlerConfig.getConfig().getTargetLang().equals("en"))
					option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.MyTextTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";
				else
					option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.JapaneseTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";

				if (config[0].equals("gram"))
					option = "-W -C -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.CharacterNgramTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";

				anchor = new WekaClassifier(algo, option);
				anchor.train(trainPath, rm, false, null, true, false);

				weightAnchor = anchor.getGMeanFromTestSet(testPath);

				logger.info("anchor G-mean:\t" + weightAnchor);
				total++;
			}

			if (classifier[2]) {
				logger.info("=========URL==========");

				algo = "weka.classifiers.bayes.NaiveBayesMultinomialText";

				rm = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };

				if (CrawlerConfig.getConfig().getTargetLang().equals("en"))
					option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.MyTextTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";
				else
					option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.JapaneseTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";

				if (config[1].equals("gram"))
					option = "-W -C -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.CharacterNgramTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";

				url = new WekaClassifier(algo, option);
				url.train(trainPath, rm, false, null, true, false);

				weightUrl = url.getGMeanFromTestSet(testPath);
				logger.info("url G-mean:\t" + weightUrl);
				total++;
			}

			// set weight

//			if (useWeight) {
//				if (link != null)
//					link.setWeight(weightLink);
//
//				if (anchor != null)
//					anchor.setWeight(weightAnchor);
//
//				if (url != null)
//					url.setWeight(weightUrl);
//			}

			// classified

			ClassifierOutput[] cfs = new ClassifierOutput[total];
			String[] instance;

			ConfusionMatrixObj[] ma = new ConfusionMatrixObj[6];
			for (int q = 0; q < ma.length; q++) {
				ma[q] = new ConfusionMatrixObj();
			}

			boolean start = false;
			String[] instances = FileUtils.readFile(testPath);

			for (int b = 0; b < instances.length; b++) {
				if (instances[b].equals("@data")) {
					start = true;
					continue;
				}

				if (!start)
					continue;

				instance = instances[b].split(",");

				int c = 0;

				if (link != null)
					cfs[c++] = link.predict(instance[0], instance);

				if (anchor != null && instance[10].replace("'", "").trim().length() > 0)
					cfs[c++] = anchor.predict(instance[0], instance);
				else
					cfs[c++] = null;

				if (url != null && instance[11].replace("'", "").trim().length() > 0 )
					cfs[c++] = url.predict(instance[0], instance);
				else
					cfs[c++] = null;

				// logger.info(b + "/" + instances.length);

				ClassifierOutput finalOutput = WekaClassifier.min(cfs);

				if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[0].incTp();
				} else if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[0].incFp();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[0].incFn();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[0].incTn();
				}
				// System.out.println("Min " + finalOutput);

				finalOutput = WekaClassifier.max(cfs);
				// System.out.println("Max " + finalOutput);

				// System.exit(0);

				if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[1].incTp();
				} else if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[1].incFp();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[1].incFn();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[1].incTn();
				}

				finalOutput = WekaClassifier.average(cfs);

				if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[2].incTp();
				} else if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[2].incFp();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[2].incFn();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[2].incTn();
				}

				finalOutput = WekaClassifier.product(cfs);
				if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[3].incTp();
				} else if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[3].incFp();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[3].incFn();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[3].incTn();
				}

				finalOutput = WekaClassifier.majority(cfs);

				if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[4].incTp();
				} else if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[4].incFp();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[4].incFn();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[4].incTn();
				}

				if (instance[instance.length - 1].toLowerCase().equals("thai")) {
					if (cfs[0].getResultClass() == ResultClass.RELEVANT || (cfs[1] != null && cfs[1].getResultClass() == ResultClass.RELEVANT) || (cfs[2] != null && cfs[2].getResultClass() == ResultClass.RELEVANT)) {
						ma[5].incTp();
					} else {
						ma[5].incFn();
					}

				} else {
					if (cfs[0].getResultClass() == ResultClass.IRRELEVANT || (cfs[1] != null && cfs[1].getResultClass() == ResultClass.IRRELEVANT) || (cfs[2] != null && cfs[2].getResultClass() == ResultClass.IRRELEVANT)) {
						ma[5].incTn();
					} else {
						ma[5].incFp();
					}

				}

			}

			logger.info("final min g-mean:\t" + ma[0].getGmean());
			logger.info("final max g-mean:\t" + ma[1].getGmean());
			logger.info("final average g-mean:\t" + ma[2].getGmean());
			logger.info("final product g-mean:\t" + ma[3].getGmean());
			logger.info("final majority g-mean:\t" + ma[4].getGmean());
			logger.info("final best-fusion g-mean\t" + ma[5].getGmean());
			return ma[1].getGmean();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1;

	}

	

	public static double evaluateMultiPredictor(String trainRelPath, String trainNonPath, String testPath) {

		try {
			int[] rm = { 1, 10, 11 };

			// int[] rm = { 1, 10, 8, 9, 11 };
			// ไม่ใช้ ratio
			if (!incRatio)
				rm = new int[] { 1, 6, 7, 10, 11 };

			String algo;
			String option = null;

			WekaClassifier linkRel = null, anchorRel = null, urlRel = null;
			WekaClassifier linkNon = null, anchorNon = null, urlNon = null;
			int total = 0;
			
			
			if (classifier[0]) {
				logger.info("=========Link==========");
				// link based
				algo = "weka.classifiers.bayes.NaiveBayes";

				if (linkNb) {
					linkRel = new WekaClassifier(algo, option);
					linkRel.train(trainRelPath, rm, true, null, true, false);
					
					
					linkNon = new WekaClassifier(algo, option);
					linkNon.train(trainNonPath, rm, true, null, true, false);
				} else {
					algo = "weka.classifiers.lazy.IBk";
					option = "-K " + k + " -W 0";
					linkRel = new WekaClassifier(algo, option);
					linkRel.train(trainRelPath, rm, discritize, null, true, false);
					
					linkNon = new WekaClassifier(algo, option);
					linkNon.train(trainNonPath, rm, discritize, null, true, false);
				}

				// System.out.println(testPath);
				total++;
			}

			if (classifier[1]) {
				logger.info("=========Anchor==========");
				// anchor
				rm = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 11 };

				algo = "weka.classifiers.bayes.NaiveBayesMultinomialText";

				if (CrawlerConfig.getConfig().getTargetLang().equals("en"))
					option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.MyTextTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";
				else
					option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.JapaneseTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";

				if (config[0].equals("gram"))
					option = "-W -C -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.CharacterNgramTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";

				anchorRel = new WekaClassifier(algo, option);
				anchorRel.train(trainRelPath, rm, false, null, true, false);
				
				anchorNon = new WekaClassifier(algo, option);
				anchorNon.train(trainNonPath, rm, false, null, true, false);
				
				total++;
			}

			if (classifier[2]) {
				logger.info("=========URL==========");

				algo = "weka.classifiers.bayes.NaiveBayesMultinomialText";

				rm = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

				if (CrawlerConfig.getConfig().getTargetLang().equals("en"))
					option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.MyTextTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";
				else
					option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.JapaneseTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";

				if (config[1].equals("gram"))
					option = "-W -C -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.CharacterNgramTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";

				urlRel = new WekaClassifier(algo, option);
				urlRel.train(trainRelPath, rm, false, null, true, false);

				
				urlNon = new WekaClassifier(algo, option);
				urlNon.train(trainNonPath, rm, false, null, true, false);

				total++;
			}

			// classified

			ClassifierOutput[] cfs = new ClassifierOutput[total];
			String[] instance;

			ConfusionMatrixObj[] ma = new ConfusionMatrixObj[6];
			for (int q = 0; q < ma.length; q++) {
				ma[q] = new ConfusionMatrixObj();
			}

			boolean start = false;
			String[] instances = FileUtils.readFile(testPath);

			for (int b = 0; b < instances.length; b++) {
				if (instances[b].equals("@data")) {
					start = true;
					continue;
				}

				if (!start)
					continue;

				instance = instances[b].split(",");

				int c = 0;

				
				if (instance[9].equals("0")) {
					if (linkRel != null)
						cfs[c++] = linkRel.predict(instance[0], instance);

					if (anchorRel != null && instance[10].replace("'", "").trim().length() > 0)
						cfs[c++] = anchorRel.predict(instance[0], instance);
					else
						cfs[c++] = null;

					if (urlRel != null && instance[11].replace("'", "").trim().length() > 0 )
						cfs[c++] = urlRel.predict(instance[0], instance);
					else
						cfs[c++] = null;
					
				}else{
					if (linkNon != null)
						cfs[c++] = linkNon.predict(instance[0], instance);

					if (anchorNon != null && instance[10].replace("'", "").trim().length() > 0)
						cfs[c++] = anchorNon.predict(instance[0], instance);
					else
						cfs[c++] = null;

					if (urlNon != null && instance[11].replace("'", "").trim().length() > 0 )
						cfs[c++] = urlNon.predict(instance[0], instance);
					else
						cfs[c++] = null;
					
				}
			

				// logger.info(b + "/" + instances.length);

				ClassifierOutput finalOutput = WekaClassifier.min(cfs);

				if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[0].incTp();
				} else if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[0].incFp();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[0].incFn();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[0].incTn();
				}
				// System.out.println("Min " + finalOutput);

				finalOutput = WekaClassifier.max(cfs);
				// System.out.println("Max " + finalOutput);

				// System.exit(0);

				if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[1].incTp();
				} else if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[1].incFp();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[1].incFn();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[1].incTn();
				}

				finalOutput = WekaClassifier.average(cfs);

				if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[2].incTp();
				} else if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[2].incFp();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[2].incFn();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[2].incTn();
				}

				finalOutput = WekaClassifier.product(cfs);
				if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[3].incTp();
				} else if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[3].incFp();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[3].incFn();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[3].incTn();
				}

				finalOutput = WekaClassifier.majority(cfs);

				if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[4].incTp();
				} else if (finalOutput.getResultClass() == ResultClass.RELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[4].incFp();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("thai")) {
					ma[4].incFn();
				} else if (finalOutput.getResultClass() == ResultClass.IRRELEVANT && instance[instance.length - 1].toLowerCase().equals("non")) {
					ma[4].incTn();
				}

				if (instance[instance.length - 1].toLowerCase().equals("thai")) {
					if (cfs[0].getResultClass() == ResultClass.RELEVANT || (cfs[1] != null && cfs[1].getResultClass() == ResultClass.RELEVANT) || (cfs[2] != null && cfs[2].getResultClass() == ResultClass.RELEVANT)) {
						ma[5].incTp();
					} else {
						ma[5].incFn();
					}

				} else {
					if (cfs[0].getResultClass() == ResultClass.IRRELEVANT || (cfs[1] != null && cfs[1].getResultClass() == ResultClass.IRRELEVANT) || (cfs[2] != null && cfs[2].getResultClass() == ResultClass.IRRELEVANT)) {
						ma[5].incTn();
					} else {
						ma[5].incFp();
					}

				}

			}

			logger.info("final min g-mean:\t" + ma[0].getGmean());
			logger.info("final max g-mean:\t" + ma[1].getGmean());
			logger.info("final average g-mean:\t" + ma[2].getGmean());
			logger.info("final product g-mean:\t" + ma[3].getGmean());
			logger.info("final majority g-mean:\t" + ma[4].getGmean());
			logger.info("final best-fusion g-mean\t" + ma[5].getGmean());
			return ma[1].getGmean();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1;

	}

	// public static ClassifierOutput average(ClassifierOutput[] cf) {
	//
	// double thai = 0, non = 0;
	//
	// non += 0.75 * cf[0].getNonThaiScore();
	// thai += 0.75 * cf[0].getThaiScore();
	//
	// non += 0.25 * cf[1].getNonThaiScore();
	// thai += 0.25 * cf[1].getThaiScore();
	//
	// // thai /= cf.length;
	// // non /= cf.length;
	// return new ClassifierOutput(thai, non);
	// }

	// public static double trainPredictor(String trainPath, boolean incRatio,
	// boolean isKnn) {
	//
	// File f = new File(trainPath);
	// String modelPath = f.getParent();
	// try {
	// String t;
	// // int[] rm = { 1, 7, 8, 10, 11, 13, 14 };
	// int[] rm = { 1, 7, 8 };
	//
	// // ไม่ใช้ ratio
	// if (!incRatio)
	// rm = new int[] { 1, 5, 6, 7, 8 };
	//
	// // rm = new int[] { 1,2,3,4,5,6,8 7, 8, 9, 10, 11, 12, 13, 14 };
	//
	// logger.info("=========Link==========");
	// // link based
	// String algo = "weka.classifiers.bayes.NaiveBayes";
	// String[] option = null;
	//
	// if (isKnn) {
	// algo = "weka.classifiers.lazy.IBk";
	// t = "-K 3 -W 0";
	// option = Utils.splitOptions(t);
	// }
	//
	// WekaClassifier link = new WekaClassifier(algo, option);
	// if (isKnn)
	// link.train(trainPath, rm, false, null, true, false);
	// else
	// link.train(trainPath, rm, true, null, true, false);
	//
	// FileUtils.saveObjFile(link, modelPath + "/link.model");
	//
	// // anchor
	// rm = new int[] { 1, 2, 3, 4, 5, 6, 8 };
	//
	// logger.info("=========Anchor==========");
	// algo = "weka.classifiers.bayes.NaiveBayesMultinomialText";
	//
	// if(CrawlerConfig.getConfig().getTargetLang().equals("ja"))
	// t =
	// "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.JapaneseTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";
	// else
	// t =
	// "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.MyTextTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";
	//
	// if (config[0].equals("gram"))
	// t =
	// "-W -C -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -stoplist -tokenizer \"com.job.ic.ml.classifiers.CharacterNgramTokenizer\" -stemmer \"weka.core.stemmers.NullStemmer\"";
	//
	// option = Utils.splitOptions(t);
	// WekaClassifier anchor = new WekaClassifier(algo, option);
	// anchor.train(trainPath, rm, false, null, true, false);
	// // logger.info("anchor G-mean:\t" +
	// // anchor.getGMeanFromTestSet(testPath));
	//
	// FileUtils.saveObjFile(anchor, modelPath + "/anchor.model");
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// return -1;
	//
	// }

	public static void mergeFeatures(String dirPath) throws IOException {

		File[] fi = FileUtils.getAllFile(dirPath);
		String[] tmp, t;
		BufferedWriter th = FileUtils.getBufferedFileWriter(dirPath + "thai.csv");
		BufferedWriter non = FileUtils.getBufferedFileWriter(dirPath + "non.csv");

		HashSet<String> previousHost = new HashSet<>();
		HashSet<String> newHost = new HashSet<>();

		int count = 0;
		String header = "";
		HashSet<String> set = new HashSet<String>();
		for (File f : fi) {
			count = 0;
			tmp = FileUtils.readFile(f.getAbsolutePath());
			for (String s : tmp) {
				// remove header
				if (count == 0) {
					count++;
					continue;
				}

				t = s.split(",");

				if (set.contains(t[0].trim()))
					continue;

				if (!t[t.length - 1].equals("NON") && !t[t.length - 1].equals("THAI"))
					continue;

				if (previousHost.contains(HttpUtils.getHost(t[0].trim())))
					continue;

				if (t[t.length - 1].trim().equals("THAI")) {
					th.write(s.concat("\n").replace(t[0], URLDecoder.decode(t[0].replace(",", "-mikecommamike-").replace("%", "").replace("\"", ""), "UTF-8")));
				} else {
					non.write(s.concat("\n").replace(t[0], URLDecoder.decode(t[0].replace(",", "-mikecommamike-").replace("%", "").replace("\"", ""), "UTF-8")));
				}

				newHost.add(HttpUtils.getHost(t[0].trim()));
				set.add(t[0].trim());

			}

			previousHost.addAll(newHost);
			newHost.clear();
		}

		th.close();
		non.close();

		BufferedWriter list = FileUtils.getBufferedFileWriter(dirPath + "list.txt");

		for (String s : set) {
			list.write(s + "\t" + HttpUtils.getHost(s) + "\n");
		}
		list.close();

	}

	public static ArrayList<String> downsampling(ArrayList<String> thaiData, ArrayList<String> nonData, int limit) {

		Collections.shuffle(nonData);
		Collections.shuffle(thaiData);
		ArrayList<String> buffer = new ArrayList<String>();

		System.out.println(thaiData.size() + "\t" + nonData.size());
		if (limit == -1) {
			for (int i = 0; i < thaiData.size() && i < nonData.size(); i++) {
				buffer.add(thaiData.get(i).toLowerCase());
				buffer.add(nonData.get(i).toLowerCase());
			}
		} else {
			for (int i = 0; i < thaiData.size() && i < nonData.size() && i < limit; i++) {
				buffer.add(thaiData.get(i).toLowerCase());
				buffer.add(nonData.get(i).toLowerCase());
			}
		}

		return buffer;
	}

	public static void downsampling(ArrayList<String> thaiData, ArrayList<String> nonData, String output, String header) {

		ArrayList<String> buffer = downsampling(thaiData, nonData, -1);

		if (thaiData.size() < nonData.size()) {
			for (int i = 0; i < thaiData.size() && nonData.size() > 0; i++) {
				buffer.add(thaiData.get(i));
				buffer.add(nonData.remove(0));
			}
		} else {
			for (int i = 0; i < nonData.size() && thaiData.size() > 0; i++) {
				buffer.add(thaiData.remove(0));
				buffer.add(nonData.get(i));
			}
		}

		if (header != null) {
			FileUtils.writeTextFile(output, header, false);
			FileUtils.writeTextFile(output, buffer, true);
		} else {
			FileUtils.writeTextFile(output, buffer, false);
		}
		buffer.clear();

	}

	public static void mergeData(ArrayList<String> thaiData, ArrayList<String> nonData, String output, boolean incHeader) {

		ArrayList<String> buffer = new ArrayList<String>();
		for (int i = 0; i < thaiData.size(); i++) {
			buffer.add(thaiData.get(i));
		}
		for (int i = 0; i < nonData.size(); i++) {
			buffer.add(nonData.get(i));
		}

		if (incHeader) {
			FileUtils.writeTextFile(output, header, false);
			FileUtils.writeTextFile(output, buffer, true);
		} else {
			FileUtils.writeTextFile(output, buffer, false);
		}

	}

	public static void mergeFile(String source1, String source2, String dest, boolean isDownsampling) {
		try {
			// new one
			boolean isStart = false;
			ArrayList<String> thai = new ArrayList<>();
			ArrayList<String> non = new ArrayList<>();

			for (String s : FileUtils.readFile(source2)) {
				if (source2.contains(".csv"))
					isStart = true;

				if (s.contains("@data")) {
					isStart = true;
					continue;
				}

				if (!isStart)
					continue;

				String label = s.substring(s.lastIndexOf(",") + 1);
				label = label.trim().toLowerCase();

				if (label.equals("non")) {
					non.add(s);
				} else {
					thai.add(s);
				}
			}

			BufferedWriter bw = FileUtils.getBufferedFileWriter(dest);

			isStart = false;
			String[] tmp = FileUtils.readFile(source1);
			if (!tmp[0].startsWith("@relation"))
				bw.write(header);

			for (String s : tmp) {
				bw.write(s + "\n");
			}

			// downsampling new one
			// adding downsampling new one to original source
			if (isDownsampling) {
				for (String s : downsampling(thai, non, 10000)) {
					bw.write(s + "\n");
				}
			} else {
				for (String s : thai) {
					bw.write(s + "\n");
				}

				for (String s : non) {
					bw.write(s + "\n");
				}
			}

			bw.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
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
}
