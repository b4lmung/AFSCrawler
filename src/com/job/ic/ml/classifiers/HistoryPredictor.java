package com.job.ic.ml.classifiers;

import com.job.ic.crawlers.HttpSegmentCrawler;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.DirectoryTree;
import com.job.ic.crawlers.models.DirectoryTreeNode;
import com.job.ic.crawlers.models.SegmentQueueModel;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;
import org.apache.log4j.Logger;
import weka.core.Instances;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class HistoryPredictor {
	private static final boolean useHistoryPredictor = CrawlerConfig.getConfig().useHistoryPredictor();
	private static final String header = "@relation 'history'\n" + "@attribute segpath string\n"
			+ "@attribute slopRel numeric\n" + "@attribute slopNon numeric\n" + "@attribute slopRatio numeric\n"
			+ "@attribute meanHV numeric\n" + "@attribute class {thai,non}\n" + "@data\n";
	private static Logger logger = Logger.getLogger(HistoryPredictor.class);
	private static ArrayList<String> rel = new ArrayList<>();
	private static ArrayList<String> non = new ArrayList<>();
	private static ArrayList<String> all = new ArrayList<>();

	// private static HashMap<String, HashSet<String>> dupDb = new HashMap<>();
	private static ArrayList<String> allDup = new ArrayList<>();
	private static ArrayList<String> results = new ArrayList<>();
	private static HashSet<String> blacklistHost = new HashSet<>();
	private static HashMap<String, HashSet<String>> dupDb = new HashMap<>();
	private static WekaClassifier historyPredictor = null;

	private static int num = 10;
	
	public static boolean useHistoryPredictor() {
		return useHistoryPredictor;
	}

	public static String getHistoryFeaturesHeader() {
		return header;
	}

	public static synchronized void record(SegmentQueueModel input, int relPages, int nonPages) {
		record(input.getSegmentName(), input.getHistoryPredictions(), relPages, nonPages);
	}

	public static synchronized void record(String segmentName, ArrayList<ClassifierOutput> dirPredictions, int relPages,
			int nonPages) {

		if (relPages + nonPages == 0)
			return;

		boolean isRelevant = false;
		double degree = HttpSegmentCrawler.calcRelevanceDegree(relPages, nonPages);
		if (degree > CrawlerConfig.getConfig().getRelevanceDegreeThreshold()) {
			isRelevant = true;
		}

		DirectoryTreeNode node = NeighborhoodPredictor.getDirectoryNode(segmentName);

		// Do not need to add data --> neighborhoodpredictor already do that
		// node.getDirectoryTree().addCrawledNode(segmentName, relPages, nonPages);

		if (dirPredictions != null) {
			for (ClassifierOutput predict : dirPredictions) {
				if (predict == null)
					continue;

				if (predict.getResultClass() == ResultClass.RELEVANT) {
					if (isRelevant) {
						AccuracyTracker.getHistoryConfusionMatrix().incTp();
					} else {
						AccuracyTracker.getHistoryConfusionMatrix().incFn();
					}
				} else {
					if (isRelevant) {
						AccuracyTracker.getHistoryConfusionMatrix().incFp();
					} else {
						AccuracyTracker.getHistoryConfusionMatrix().incTn();
					}
				}

				String features = predict.getFeatures();
				if (isRelevant) {
					if (features.lastIndexOf(",non") < 0)
						continue;
					features = features.substring(0, features.lastIndexOf(",non")) + ",thai";
				}

				if (features.contains("?,?,?,?,?,?,"))
					continue;

				String host = HttpUtils.getHost(segmentName);
				if (host == null)
					continue;

				if (!dupDb.containsKey(host))
					dupDb.put(host, new HashSet<>());

				String data = features.substring(features.indexOf(",") + 1);
				if (dupDb.get(host).contains(data))
					continue;

				dupDb.get(host).add(data);

				allDup.add(features);

				if (isRelevant) {
					rel.add(features);
				} else {
					non.add(features);
				}
				all.add(features);

				// System.err.println(features);
				results.add(features + "\tPrediction:\t" + predict.getResultClass() + "\t" + predict.getRelevantScore()
						+ "\tActual:\t" + isRelevant);
			}
		}
	}

	public static synchronized void onlineUpdate() {

		backupAllHistoryData("logs/history.arff");

		Instances hi;
		ArrayList<String> historyDataset = createHistoryDataSet();
		if (historyDataset != null) {
			logger.info("History Size : " + historyDataset.size());
			FileUtils.deleteFile("logs/htmp.arff");
			FileUtils.writeTextFile("logs/htmp.arff", HistoryPredictor.getHistoryFeaturesHeader() + "\n", false);
			FileUtils.writeTextFile("logs/htmp.arff", historyDataset, true);

			FileUtils.writeTextFile("logs/hupdate.arff", historyDataset, true);
			FileUtils.writeTextFile("logs/hupdate.arff",
					"--------------------------------------------------------------------------------\n", true);

			// logger.info(CrawlerConfig.getConfig().isTrainingMode());
			if (CrawlerConfig.getConfig().isTrainingMode())
				return;
			
			if (CrawlerConfig.getConfig().getPredictorTrainingPath().trim().length() == 0)
				return;
			

			if (CrawlerConfig.getConfig().getUpdateInterval() < 0)
				return;

			try {
				hi = new Instances(new FileReader("logs/htmp.arff"));
				hi.setClassIndex(hi.numAttributes() - 1);

				if (historyPredictor == null)
					return;

				historyPredictor.updateClassifier(hi);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	public static synchronized ClassifierOutput predict(String basePath) {
		if (basePath == null)
			return null;

		DirectoryTreeNode node = NeighborhoodPredictor.getDirectoryNode(basePath);
		if (node == null) {
			return null;
		}

		String features = getHistoryFeatures(node);
		if (features == null) {
			return null;
		}

		if (CrawlerConfig.getConfig().isTrainingMode() || CrawlerConfig.getConfig().getPredictorTrainingPath().trim().length() == 0) {
			ClassifierOutput output = new ClassifierOutput(0.5, 0.5, 1.0);
			output.setFeatures(features);
			return output;
		}

		if (useHistoryPredictor == false)
			return null;

		String[] tmp = features.split(",");
		ClassifierOutput output = historyPredictor.predict(basePath, tmp);
		output.setFeatures(features);

		double total = tmp.length - 1;
		output.setWeight(1);

		if (total == 0)
			return null;

		if (output.getResultClass() == ResultClass.IRRELEVANT)
			blacklistHost.add(HttpUtils.getHost(basePath.toLowerCase()));

		return output;
	}

	private static synchronized String getHistoryFeatures(DirectoryTreeNode node) {
		return getHistoryFeatures(node, false);
	}

	private static synchronized String getHistoryFeatures(DirectoryTreeNode node, boolean isRel) {
		if (node == null)
			return null;

		DirectoryTree tree = node.getDirectoryTree();

		/* new features */

		double slopeRel = -1;
		double slopeNon = -1;
		double slopeRatio = -1;
		double avgRatio = -1;

		
		if(tree.getCumulativeRelPages().size() < num)
			return null;

		if (tree.getCumulativeRelPages().size() >= num) {
			ArrayList<Integer> tmpRel = tree.getCumulativeRelPages();
			ArrayList<Integer> tmpNon = tree.getCumulativeNonPages();

			int start = tmpRel.size();
			slopeRel = tmpRel.get(start - 1) - tmpRel.get(start - num);
			slopeNon = tmpNon.get(start - 1) - tmpNon.get(start - num);
			slopeRatio = HttpSegmentCrawler.calcRelevanceDegree(tmpRel.get(start - 1), tmpNon.get(start - 1))
					- HttpSegmentCrawler.calcRelevanceDegree(tmpRel.get(start - num), tmpNon.get(start - num));
			slopeRel /= num;
			slopeNon /= num;
			slopeRatio /= num;

			avgRatio = 0;
			for (int i = start - num; i < start; i++) {
				double degree = HttpSegmentCrawler.calcRelevanceDegree(tmpRel.get(i), tmpNon.get(i));
				avgRatio += degree;
			}

			avgRatio /= num;

		}

		String features = String.format("%s,%s,%s,%s,%s,%s",
				StringUtils.cleanUrlDataForPrediction(tree.getHostname() + node.getPathName()),
				slopeRel != -1 ? String.valueOf(slopeRel) : "?", slopeNon != -1 ? String.valueOf(slopeNon) : "?",
				slopeRatio != -1 ? String.valueOf(slopeRatio) : "?", avgRatio != -1 ? String.valueOf(avgRatio) : "?",
				isRel ? "thai" : "non");

		return features;

	}

	private static ArrayList<String> createHistoryDataSet() {
		if (!useHistoryPredictor)
			return null;

		logger.info("rel/non\t" + rel.size() + "/" + non.size());
		return FeaturesCollectors.underSampling(rel, non);
	}

	public static synchronized void backupAllHistoryData(String path) {
		if (!useHistoryPredictor)
			return;

		ArrayList<String> output = new ArrayList<>();
		output.add(getHistoryFeaturesHeader());
		output.addAll(all);
		FileUtils.writeTextFile(path, output, false);

		FileUtils.writeTextFile(path + ".results.txt", results, false);
		FileUtils.writeTextFile(path + ".allDup.txt", allDup, false);
		// FileUtils.writeTextFile(path + ".previousHistory.txt",
		// previousHistory, false);

	}

	private static void cleanHistoryFeature(String path) {
		ArrayList<String> output = new ArrayList<>();

		HashMap<String, HashSet<String>> rel = new HashMap<>();

		for (String[] s : FileUtils.readArffData(path)) {
			String features = "";
			s[0] = StringUtils.cleanUrlDataForPrediction(s[0]);

			String host = HttpUtils.getHost(s[0]);

			if (!rel.containsKey(host))
				rel.put(host, new HashSet<>());

			for (int i = 0; i < s.length; i++) {
				if (i != s.length - 1)
					features += s[i] + ",";
				else
					features += s[i];
			}

			if (features.trim().length() == 0)
				continue;

			String data = features.substring(features.indexOf(","));
			// System.out.println(data);
			if (rel.get(host).contains(data))
				continue;

			if (features.contains(",?,?,?,?,"))
				continue;

			rel.get(host).add(data);
			output.add(features);
		}

		FileUtils.writeTextFile(path, header + "\n", false);
		FileUtils.writeTextFile(path, output, true);
	}

	public static void parseLogTrainingHistory(String path) {

		double d = 0;
		int count = 0;
		for (String s : FileUtils.readFile(path)) {
			s = s.toLowerCase();
			if (s.contains("tp") && s.contains("tn") && s.contains("fp") && s.contains("fn")) {
				s = s.substring(s.indexOf("tp"));
				String[] tmp = s.replace("tp", "").replace("tn", "").replace("fp", "").replace("fn", "").split("\t");
				ConfusionMatrixObj o = new ConfusionMatrixObj(Integer.parseInt(tmp[0].trim()),
						Integer.parseInt(tmp[1].trim()), Integer.parseInt(tmp[2].trim()),
						Integer.parseInt(tmp[3].trim()));
				System.out.println(o.getGmean());
				count++;
				d += o.getGmean();
			}

		}

		System.out.println("Average:\t" + d / count);
	}

	public static void trainHistoryPredictorForSegmentCrawler() {

		String path = CrawlerConfig.getConfig().getPredictorTrainingPath();
		if (path != null || path.trim().length() != 0) {
			path = "h" + path;
			trainHistoryPredictor(path);
		}
	}

	public static void trainHistoryPredictor(String path) {

		if (!useHistoryPredictor)
			return;

		if (path == null) {
			String[] tmp = FileUtils.readResourceFile("/resources/classifiers/history_initial.arff");
			FileUtils.writeTextFile("logs/htmp.arff", tmp, false);
			path = "logs/htmp.arff";
		}

		if (!FileUtils.exists(path)) {
			return;
		}
		

		// history
		logger.info("=========History==========");

		String algo = "weka.classifiers.bayes.NaiveBayesUpdateable";
		String option = null;

		// algo = "weka.classifiers.trees.J48";
		// algo = "weka.classifiers.trees.RandomForest";

		// option = "-C 0.5";

		int[] rm = new int[] { 1, 2, 3 };

		// only slope;
		// rm = new int[] { 1,2,3,5 };

		historyPredictor = new WekaClassifier(algo, option, 1.0);
		historyPredictor.setDiscretize(10);

		historyPredictor.train(path, rm, true, null, true, false);

		// FileUtils.deleteFile("htmp.arff");
	}

	public static void main(String[] args) {
		
		buildTrainingFile("hgaming-page6", "hgaming-page.arff", 3);
//		buildTrainingFile("htourism-bf8", "htourism-page.arff", 3);
//		
//		
		System.exit(0);
		String training = "hgaming-raw-page.arff";
		cleanHistoryFeature(training);
		// System.exit(0)

		double max = 0, avg = 0;
		int index = 0;
		try {
			double result = 0;
			for (int i = 0; i < 10; i++) {
				result = kFold(training, training.replace(".arff", "").replace("-raw", "")  + i, true);
				
//				result = kFold(training, training.replace(".arff", "").replace("-raw", "") + i, false);
				avg += result;
				// result = kFold(training, training.replace("-raw",
				// "").replace(".arff", "") + i, false);
				if (result > max) {
					max = result;
					index = i;
				}
			}

			avg /= 10;
			System.out.println(avg + "\t" + max + "\t" + index);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static double kFold(String filename, String destPath, boolean split) throws IOException {
		int k = 3;

		if (!FileUtils.exists(destPath)) {
			FileUtils.mkdir(destPath);
		}

		ArrayList<String> all = FileUtils.readArffDataWithoutSplit(filename);
		// String header =
		// FileUtils.readResourceFileAsString("/resources/classifiers/hprofile.arff");

		if (split) {
			ArrayList<String> rel = new ArrayList<>();
			ArrayList<String> non = new ArrayList<>();

			for (String s : all) {

				if (s.contains("?,?,?,?,?,?,"))
					continue;

				String[] tmp = s.split(",");

				if (tmp[tmp.length - 1].trim().equals("non")) {
					non.add(s);
				} else {
					rel.add(s);
				}
			}

			logger.info(rel.size() + "\t" + non.size());
			System.err.println("rel\t" + rel.size());
			System.err.println("non\t" + non.size());
			Collections.shuffle(rel);
			Collections.shuffle(non);

			int nonPerFile = (int) Math.ceil(non.size() * 1.0 / k);
			int thaiPerFile = (int) Math.ceil(rel.size() * 1.0 / k);

			for (int i = 0; i < k; i++) {
				BufferedWriter br = FileUtils.getBufferedFileWriter(destPath + "/history-" + i + "-rel.arff");
				BufferedWriter bn = FileUtils.getBufferedFileWriter(destPath + "/history-" + i + "-non.arff");
				br.write(header);
				bn.write(header);

				for (int j = 0; j < nonPerFile && non.size() > 0; j++) {
					bn.write(non.remove(0) + "\n");
				}

				for (int j = 0; j < thaiPerFile && rel.size() > 0; j++) {
					br.write(rel.remove(0) + "\n");
				}

				br.close();
				bn.close();
			}
		}

		double g = 0;

		for (int i = 0; i < k; i++) {
			ArrayList<String> rtrain = new ArrayList<>();
			ArrayList<String> ntrain = new ArrayList<>();

			ArrayList<String> rtest = new ArrayList<>();
			ArrayList<String> ntest = new ArrayList<>();

			for (int j = 0; j < k; j++) {

				ArrayList<String> r = FileUtils.readArffDataWithoutSplit(destPath + "/history-" + i + "-rel.arff");
				ArrayList<String> n = FileUtils.readArffDataWithoutSplit(destPath + "/history-" + i + "-non.arff");

				if (i != j) {
					// for(String s: r)
					// rtrain.add(s.replace("?", "-1"));
					//
					// for(String s: n)
					// ntrain.add(s.replace("?", "-1"));

					rtrain.addAll(r);
					ntrain.addAll(n);

				} else {

					// for(String s: r)
					// rtest.add(s.replace("?", "-1"));

					// for(String s: n)
					// ntest.add(s.replace("?", "-1"));

					rtest.addAll(r);
					ntest.addAll(n);

					FileUtils.writeTextFile(destPath + "/test.arff", header, false);
					FileUtils.writeTextFile(destPath + "/test.arff", rtest, true);
					FileUtils.writeTextFile(destPath + "/test.arff", ntest, true);
					ArrayList<String> testU = FeaturesCollectors.underSampling(r, n);

					// System.out.println(rtest.size() + "\t" + ntest.size() +
					// "\t" + (testU == null));
					FileUtils.writeTextFile(destPath + "/test_u.arff", header, false);
					FileUtils.writeTextFile(destPath + "/test_u.arff", testU, true);
				}

			}

			System.out.println(rtrain.size() + "\t" + ntrain.size());
			ArrayList<String> output = FeaturesCollectors.underSampling(rtrain, ntrain);
			FileUtils.writeTextFile(destPath + "/train.arff", header, false);
			FileUtils.writeTextFile(destPath + "/train.arff", output, true);

			FileUtils.copyFile(destPath + "/train.arff", destPath + "/" + filename.replace("-raw", ""));
			FileUtils.writeTextFile(destPath + "/" + filename.replace("-raw", ""),
					FileUtils.readArffDataWithoutSplit(destPath + "/test_u.arff"), true);

			// train

			trainHistoryPredictor(destPath + "/train.arff");
			g += historyPredictor.getGMeanFromTestSet(destPath + "/test.arff");
			// WekaClassifier link = new WekaClassifier(algo, option, 1.0);
			//
			// try {
			// link.train(destPath + "/train.arff", rm, true, null, true,
			// false);
			// g += link.getGMeanFromTestSet(destPath + "/test.arff");
			//
			// } catch (Exception e) {
			// System.exit(0);
			// }
		}

		logger.info(g / k);
		return g / k;
	}

	public static void parseLogSeries(String input, boolean onlyRel) {
		ArrayList<Double> degrees = new ArrayList<>();
		ArrayList<Double> avgs = new ArrayList<>();
		ArrayList<Integer> lengths = new ArrayList<>();
		ArrayList<Double> relevancy = new ArrayList<>();

		int rel = 0, non = 0;
		int trel = 0, tnon = 0;

		int l = 0, t = 0;

		for (String s : FileUtils.readFile(input)) {

			if (s.contains("DOWNLOADED")) {

				s = s.substring(s.indexOf("DOWNLOADED") + "DOWNLOADED".length());
				s = s.trim();
				String[] ts = s.split("\t");

				double score = Double.parseDouble(ts[0]);
				if (score > 0.5)
					trel++;
				else
					tnon++;
			}

			if (!s.contains("HttpSegmentCrawler:305-"))
				continue;
			s = s.substring(s.indexOf("HttpSegmentCrawler:305-") + "HttpSegmentCrawler:305-".length()).trim();

			String[] data = s.split("\t");

			double degree = Double.parseDouble(data[1]);
			boolean isRel = Boolean.parseBoolean(data[2]);

			if (onlyRel != isRel)
				continue;

			ArrayList<Integer> relPages = new ArrayList<>();
			ArrayList<Integer> nonPages = new ArrayList<>();

			ArrayList<Integer> relSegs = new ArrayList<>();
			ArrayList<Integer> nonSegs = new ArrayList<>();

			if (data[3].equals("[]"))
				continue;

			for (String tmp : data[3].replace("[", "").replace("]", "").split(",")) {
				relPages.add(Integer.parseInt(tmp.trim()));
			}

			for (String tmp : data[4].replace("[", "").replace("]", "").split(",")) {
				nonPages.add(Integer.parseInt(tmp.trim()));
			}

			for (String tmp : data[5].replace("[", "").replace("]", "").split(",")) {
				relSegs.add(Integer.parseInt(tmp.trim()));
			}

			for (String tmp : data[6].replace("[", "").replace("]", "").split(",")) {
				nonSegs.add(Integer.parseInt(tmp.trim()));
			}

			lengths.add(relPages.size());

			double current_relevancy = HttpSegmentCrawler.calcRelevanceDegree(relPages.get(relPages.size() - 1),
					nonPages.get(relPages.size() - 1));
			double diff = current_relevancy - HttpSegmentCrawler.calcRelevanceDegree(relPages.get(0), nonPages.get(0));
			diff /= relPages.size();

			avgs.add(diff);
			degrees.add(degree);

			relevancy.add(current_relevancy);

			int idegree = (int) (current_relevancy * 10);
			// System.out.println(current_relevancy + "\t" + idegree);
			if (idegree > 0 && idegree <= 2) {
				lengths.add(relPages.size());

				if (relPages.size() > 1) {
					int diffRel = relPages.get(relPages.size() - 1) - relPages.get(relPages.size() - 2);

					int diffNon = nonPages.get(nonPages.size() - 1) - nonPages.get(nonPages.size() - 2);

					rel += diffRel;
					non += diffNon;

				}
			}
		}

		System.out.println(rel + "\t" + non);
		System.out.println(trel + "\t" + tnon);
		System.out.println("---");
		// FileUtils.writeTextFile("length-non.txt",
		// output.stream().map(a->String.valueOf(a)).collect(Collectors.toList()),
		// false);

		HashMap<Integer, Integer> count = new HashMap<>();
		for (double a : lengths) {
			int key = (int) (a * 10);

			if (!count.containsKey(key))
				count.put(key, 0);

			count.put(key, count.get(key) + 1);
		}

		System.out.println("-------------");

		for (int i = 0; i <= 10; i++) {
			if (count.containsKey(i))
				System.out.println(i + "\t" + count.get(i));
			else
				System.out.println(i + "\t" + 0);

		}

	}

	public static void parsePreviousHistory(String inputPath, String outputPath) {
		ArrayList<String> rel = new ArrayList<>();
		ArrayList<String> non = new ArrayList<>();

		for (String s : FileUtils.readFile(inputPath)) {

			String[] t = s.split("\t");
			String seg = t[0];

			String data = t[1].split("]")[0].replace("[", "");
			// String hostPages = d[0];
			// String hostSegs = d[1];
			// String pathPages = d[2];
			// String pathSegs = d[3];
			if (data.trim().length() == 0)
				continue;

			boolean isRel = t[2].contains("true");
			if (isRel) {
				rel.add(data);
			} else {
				non.add(data);
			}

			// System.out.println(seg + "\t" + hostPages + "\t" + hostSegs +
			// "\t" + pathPages + "\t" + pathSegs + "\t" + isRel);;
		}

		FileUtils.writeTextFile(outputPath + ".rel.txt", rel, false);
		FileUtils.writeTextFile(outputPath + ".non.txt", non, false);

	}

	public static void anaylyzeDup(String path) {

		boolean first = true;

		HashMap<Integer, Integer> numFeatures = new HashMap<>();
		HashMap<Integer, Integer> numFeaturesNon = new HashMap<>();
		HashMap<Integer, Integer> numFeaturesRel = new HashMap<>();

		int dup = 0, dupRel = 0, dupNon = 0, uRel = 0, uNon = 0, all = 0;
		for (String features : FileUtils.readFile(path)) {

			String hostname = features.substring(0, features.indexOf(","));
			String host = HttpUtils.getHost(hostname);

			if (host != null) {
				if (!dupDb.containsKey(host)) {
					dupDb.put(host, new HashSet<>());
				}

				all++;
				// ตัด segname ออก
				String tmp = features.substring(features.indexOf(",") + 1);

				boolean isRel = false;

				if (tmp.contains(",thai")) {
					isRel = true;
				}

				// filter first

				if (first) {
					// TODO: Dup
					if (dupDb.get(host).contains(tmp)) {
						dup++;

						if (isRel)
							dupRel++;
						else
							dupNon++;
						continue;
					} else {
						if (isRel)
							uRel++;
						else
							uNon++;
					}

				}

				int count = StringUtils.countWordInStr(tmp, "?");

				// System.err.println(tmp + "\t" + count);
				if (!numFeatures.containsKey(count)) {
					numFeatures.put(count, 0);
					numFeaturesRel.put(count, 0);
					numFeaturesNon.put(count, 0);
				}

				if (isRel)
					numFeaturesRel.put(count, numFeaturesRel.get(count) + 1);
				else
					numFeaturesNon.put(count, numFeaturesNon.get(count) + 1);

				numFeatures.put(count, numFeatures.get(count) + 1);

				// filter later

				if (!first) {
					// TODO: Dup
					if (dupDb.get(host).contains(tmp)) {
						dup++;

						if (isRel)
							dupRel++;
						else
							dupNon++;
						continue;
					} else {
						if (isRel)
							uRel++;
						else
							uNon++;
					}

				}

				dupDb.get(host).add(tmp);

			}
		}

		System.out.println("#All data:\t" + all);
		System.out.println("#Dup data:\t" + dup + "\tRel:\t" + dupRel + "\tNon:\t" + dupNon);
		System.out.println("#Usable data:\t" + (all - dup) + "\tRel:\t" + uRel + "\tNon:\t" + uNon);

		System.out.println("--------------");

		for (int i : numFeatures.keySet()) {
			System.out.printf("Features count: %d\t%d\tRel:\t%d\tNon:\t%d\n", i, numFeatures.get(i),
					numFeaturesRel.get(i), numFeaturesNon.get(i));
		}
	}
	
	
	public static void buildTrainingFile(String dirPath, String outputPath, int k) {

		ArrayList<String> rel = new ArrayList<>();
		ArrayList<String> non = new ArrayList<>();

		ArrayList<String> lines;
		for (int j = 0; j < k; j++) {
			lines = FileUtils.readArffDataWithoutSplit(dirPath + "/history-" + j + "-rel.arff");
			for (String s : lines) {
				rel.add(s);
			}

			lines = FileUtils.readArffDataWithoutSplit(dirPath + "/history-" + j + "-non.arff");
			for (String s : lines) {
				non.add(s);
			}

		}

		// logger.info(test.size() + "\t" + train.size());
		System.out.println(rel.size() + "\t" + non.size());
		// undersampling
		ArrayList<String> train = FeaturesCollectors.underSampling(rel, non);
		System.err.println(outputPath);
		write(train, outputPath, HistoryPredictor.header);

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
