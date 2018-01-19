package com.job.ic.ml.classifiers;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.HttpSegmentCrawler;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.DirectoryTree;
import com.job.ic.crawlers.models.DirectoryTreeNode;
import com.job.ic.crawlers.models.SegmentQueueModel;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;

import weka.core.Instances;

public class NeighborhoodPredictor {
	private static Logger logger = Logger.getLogger(NeighborhoodPredictor.class);

	private static ConcurrentHashMap<String, DirectoryTree> hostData = new ConcurrentHashMap<String, DirectoryTree>();

	private static ArrayList<String> rel = new ArrayList<>();
	private static ArrayList<String> non = new ArrayList<>();
	private static ArrayList<String> all = new ArrayList<>();

	private static ArrayList<String> allDup = new ArrayList<>();
	private static ArrayList<String> results = new ArrayList<>();

	// private static HashMap<String, HashSet<String>> dupDb = new HashMap<>();
	

	private static final boolean useNeighborhoodPredictor = CrawlerConfig.getConfig().useNeighborhoodPredictor();
	private static HashMap<String, HashSet<String>> dupDb = new HashMap<>();
	private static final String header = "@relation 'history'" + "\n@attribute segpath string\n" + "@attribute HostrelPagesRatio numeric\n" + "@attribute HostrelSegsRatio numeric\n"
			+ "@attribute relPagesRatio numeric\n" + "@attribute relSegsRatio numeric\n" + "@attribute ParentRelPagesRatio numeric\n" + "@attribute ParentRelSegsRatio numeric\n"
			+ "@attribute ChildRelPagesRatio numeric\n" + "@attribute ChildRelSegsRatio numeric\n" + "@attribute SiblingRelPagesRatio numeric\n" + "@attribute SiblingRelSegsRatio numeric\n"
			+ "@attribute class {thai,non}\n" + "@data";

	private static WekaClassifier neighborhoodPredictor = null;
	
	private static ArrayList<String> ns = new ArrayList<>();


//	public static ConcurrentHashMap<String, DirectoryTree> getHostData() {
//		return hostData;
//	}
	
	public static boolean useNeighborhoodPredictor() {
		return useNeighborhoodPredictor;
	}

	public static String getNeighborhoodFeaturesHeader() {
		return header;
	}

	public static DirectoryTree get(String hostname) {
		if (hostname == null)
			return null;

		return hostData.get(hostname);
	}

	public static void clear() {
		hostData.clear();
	}

	public static synchronized void record(SegmentQueueModel input, int relPages, int nonPages) {
		//neighborhood feature
		record(input.getSegmentName(), input.getNeighborhoodPredictions(), relPages, nonPages);
		//history feature
		HistoryPredictor.record(input.getSegmentName(), input.getHistoryPredictions(), relPages, nonPages);
		
		if(CrawlerConfig.getConfig().useHistoryPredictor() && CrawlerConfig.getConfig().useNeighborhoodPredictor()) {
			ArrayList<ClassifierOutput> n = input.getNeighborhoodPredictions();
			ArrayList<ClassifierOutput> h = input.getHistoryPredictions();
			
			if (relPages + nonPages == 0)
				return;

			boolean isRelevant = false;
			double degree = HttpSegmentCrawler.calcRelevanceDegree(relPages, nonPages);
			if (degree > CrawlerConfig.getConfig().getRelevanceDegreeThreshold()) {
				isRelevant = true;
			}
			
			
			int max = Math.min(n.size(), h.size());
			for(int i=0; i<max && n.size() > 0 && h.size() > 0; i++) {
				String result = n.remove(0).toString() + "\t" + h.remove(0).toString() + "\t" + isRelevant + "\t" + degree;
				ns.add(result);
			}
				
		}
	}

	public static synchronized void record(String segmentName, ArrayList<ClassifierOutput> dirPredictions, int relPages, int nonPages) {

		if (relPages + nonPages == 0)
			return;

		boolean isRelevant = false;
		double degree = HttpSegmentCrawler.calcRelevanceDegree(relPages, nonPages);
		if (degree > CrawlerConfig.getConfig().getRelevanceDegreeThreshold()) {
			isRelevant = true;
		}

		DirectoryTreeNode node = getDirectoryNode(segmentName);
		node.getDirectoryTree().addCrawledNode(segmentName, relPages, nonPages);

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

				if (features.contains("?,?,?,?,?,?,?,?,?,?,"))
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
				results.add(features + "\tPrediction:\t" + predict.getResultClass() + "\t" + predict.getRelevantScore() + "\tActual:\t" + isRelevant);
				
			}
			
		}
	}

	public static synchronized void onlineUpdate() {
		
		backupAllNeighborhoodData("logs/neighborhood.arff");

		Instances hi;
		ArrayList<String> historyDataset = createNeighborhoodDataset();
		if (historyDataset != null) {
			logger.info("Neighborhood Size : " + historyDataset.size());
			FileUtils.deleteFile("logs/htmp.arff");
			FileUtils.writeTextFile("logs/ntmp.arff", NeighborhoodPredictor.getNeighborhoodFeaturesHeader() + "\n", false);
			FileUtils.writeTextFile("logs/ntmp.arff", historyDataset, true);

			FileUtils.writeTextFile("logs/nupdate.arff", historyDataset, true);
			FileUtils.writeTextFile("logs/nupdate.arff", "--------------------------------------------------------------------------------\n", true);

			// logger.info(CrawlerConfig.getConfig().isTrainingMode());
			if (CrawlerConfig.getConfig().isTrainingMode())
				return;
			
			if (CrawlerConfig.getConfig().getPredictorTrainingPath().trim().length() == 0)
				return;
			

			if (CrawlerConfig.getConfig().getUpdateInterval() < 0)
				return;

			try {
				hi = new Instances(new FileReader("logs/ntmp.arff"));
				hi.setClassIndex(hi.numAttributes() - 1);

				if (neighborhoodPredictor == null)
					return;

				// historyPredictor.updateClassifier(hi);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	public static synchronized ClassifierOutput predict(String basePath) {
		if (basePath == null)
			return null;

		DirectoryTreeNode node = getDirectoryNode(basePath);
		if (node == null) {
			return null;
		}

		String features = getNeighborhoodFeature(node);
		if (features == null) {
			return null;
		}
		
		if (CrawlerConfig.getConfig().isTrainingMode() || CrawlerConfig.getConfig().getPredictorTrainingPath().trim().length() == 0) {
			ClassifierOutput output = new ClassifierOutput(0.5, 0.5, 1.0);
			output.setFeatures(features);
			return output;
		}

		if (useNeighborhoodPredictor == false)
			return null;

		String[] tmp = features.split(",");
		ClassifierOutput output = neighborhoodPredictor.predict(basePath, tmp);
		output.setFeatures(features);

		double total = tmp.length - 1;
		output.setWeight(1);

		if (total == 0)
			return null;

		// node.setDirPrediction(output);
		return output;
	}

	public static synchronized DirectoryTreeNode getDirectoryNode(String basePath) {
		// if (!useHistoryPredictor)
		// return null;

		DirectoryTree tree = null;
		DirectoryTreeNode node = null;
		String hostname = HttpUtils.getHost(basePath);

		if (hostname == null)
			return null;

		if (hostData.containsKey(hostname.toLowerCase())) {
			tree = hostData.get(hostname);
			node = tree.addEmptyNode(basePath);
		} else {
			tree = new DirectoryTree(hostname);
			node = tree.addEmptyNode(basePath);
			hostData.put(hostname, tree);
		}

		return node;
	}

	private static synchronized String getNeighborhoodFeature(DirectoryTreeNode node) {
		return getNeighborhoodFeature(node, false);
	}

	private static synchronized String getNeighborhoodFeature(DirectoryTreeNode node, boolean isRel) {
		if (node == null)
			return null;

		DirectoryTree tree = node.getDirectoryTree();

		double relSegsChildNodes = DirectoryTree.getAvgRelSegsRatioChildNodes(node);
		double relPagesChildNodes = DirectoryTree.getAvgRelPagesRatioChildNodes(node);
		double relSegsSiblingNodes = DirectoryTree.getAvgRelSegsRatioSiblingNodes(node);
		double relPagesSiblingNodes = DirectoryTree.getAvgRelPagesRatioSiblingNodes(node);

		
		String features = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", StringUtils.cleanUrlDataForPrediction(tree.getHostname() + node.getPathName()),
				tree != null && tree.getOverallRelPagesRatio() != -1 ? String.valueOf(tree.getOverallRelPagesRatio()) : "?",
				tree != null && tree.getOverallRelSegsRatio() != -1 ? String.valueOf(tree.getOverallRelSegsRatio()) : "?",
				node != null && node.getRelPagesRatio() != -1 ? String.valueOf(node.getRelPagesRatio()) : "?",
				node != null && node.getRelSegsRatio() != -1 ? String.valueOf(node.getRelSegsRatio()) : "?",
				node.getParentNode() != null && node.getParentNode().getRelPagesRatio() != -1 ? String.valueOf(node.getParentNode().getRelPagesRatio()) : "?",
				node.getParentNode() != null && node.getParentNode().getRelSegsRatio() != -1 ? String.valueOf(node.getParentNode().getRelSegsRatio()) : "?",
				relSegsChildNodes != -1 ? relSegsChildNodes : "?", relPagesChildNodes != -1 ? relPagesChildNodes : "?", relSegsSiblingNodes != -1 ? relSegsSiblingNodes : "?",
				relPagesSiblingNodes != -1 ? relPagesSiblingNodes : "?", isRel ? "thai" : "non");

		
		return features;

	}

	private static ArrayList<String> createNeighborhoodDataset() {
		
		if(!useNeighborhoodPredictor)
			return null;
	
		logger.info("rel/non\t" + rel.size() + "/" + non.size());
		return underSampling(rel, non, true);
	}

	
	public static ArrayList<String> underSampling(ArrayList<String> rel, ArrayList<String> non, boolean sortMissingValues) {

		ArrayList<String> output = new ArrayList<>();
		int min = Math.min(rel.size(), non.size());
		
		if(sortMissingValues){
			Collections.sort(rel, (a,b)->Integer.compare(StringUtils.countWordInStr(a, "?"), StringUtils.countWordInStr(b, "?")));
			Collections.sort(non, (a,b)->Integer.compare(StringUtils.countWordInStr(a, "?"), StringUtils.countWordInStr(b, "?")));

		}
		
		if (min == 0)
			return null;

		for (int i = 0; i < min; i++) {
			output.add(rel.remove(rel.size() - 1));
			output.add(non.remove(non.size() - 1));
		}

		return output;
	}
	
	public static synchronized void backupAllNeighborhoodData(String path) {
		if(!useNeighborhoodPredictor)
			return;
	
		ArrayList<String> output = new ArrayList<>();
		output.add(getNeighborhoodFeaturesHeader());
		output.addAll(all);
		FileUtils.writeTextFile(path, output, false);

		FileUtils.writeTextFile(path + ".results.txt", results, false);
		FileUtils.writeTextFile(path + ".allDup.txt", allDup, false);
		FileUtils.writeTextFile(path + ".ns.txt", ns, false);
		
		// FileUtils.writeTextFile(path + ".previousHistory.txt",
		// previousHistory, false);

	}

	public static void trainNeighborhoodPredictorForSegmentCrawler() {

		String path = CrawlerConfig.getConfig().getPredictorTrainingPath();
		if (path != null || path.trim().length() != 0) {
			path = "n" + path;
			trainNeighborhoodPredictor(path);
		}
	}

	public static void trainNeighborhoodPredictor(String path) {

		if (!useNeighborhoodPredictor)
			return;

		if (path == null) {
			String[] tmp = FileUtils.readResourceFile("/resources/classifiers/neighborhood_initial.arff");
			FileUtils.writeTextFile("logs/ntmp.arff", tmp, false);
			path = "logs/ntmp.arff";
		}
		

		if (!FileUtils.exists(path)) {
			return;
		}

		// history
		logger.info("=========Neighborhood==========");

		String algo = "weka.classifiers.bayes.NaiveBayesUpdateable";
		String option = null;
		// option = "-K";
		// algo = "weka.classifiers.lazy.IBk";
		// option = "-K " + 3 + " -W 0";

		int[] rm = new int[] {1};
	
//		logger.info("Remove all neighborhood feature except website relevancy");
//		rm = new int[] { 1,3,4,5,6,7,8,9,10,11};
		
		
		neighborhoodPredictor = new WekaClassifier(algo, option, 1.0);
		// String[] tmp =
		// FileUtils.readResourceFile("/resources/classifiers/history_initial.arff");
		// FileUtils.writeTextFile("htourism.arff", tmp, false);

		// if (option == null || !option.contains("-K"))
		neighborhoodPredictor.setDiscretize(5);

		neighborhoodPredictor.train(path, rm, true, null, true, false);

		// FileUtils.deleteFile("htmp.arff");
	}

	private static void parseDirectory(String path) {

		ArrayList<String> output = new ArrayList<>();
		int i = 0;
		for (String s : FileUtils.readFile(path)) {
			i++;
			if (i % 1000 == 0)
				System.out.println(i);
			if (!s.contains("relPagesRatio")) {
				continue;
			}

			try {
				s = s.substring(s.indexOf("INFO")).substring(s.indexOf("[") + 1);
				String result = s.substring(0, s.indexOf("]"));
				String segname = result.split("\t")[0];
				result = result.split("\t")[2];
				boolean isRel = result.contains("true");

				String data = s.substring(s.indexOf("]") + 1).trim();
				String[] d = data.split("\t");

				// current 1 3
				// host 5 7
				// parent 9 11
				// child 13 15
				// sibling 17 19

				String features = segname.trim() + "," + d[5].trim() + "," + d[7].trim();
				features += "," + d[1].trim() + "," + d[3].trim();
				features += "," + d[9].trim() + "," + d[11].trim();
				features += "," + d[13].trim() + "," + d[15].trim();
				features += "," + d[17].trim() + "," + d[19].trim();
				features += "," + (isRel ? "thai" : "non");
				// System.out.println(features);
				output.add(features);
			} catch (Exception e) {
				e.printStackTrace();
			}

			FileUtils.writeTextFile("nestate-raw.arff", output, false);
		}

	}

	private static void cleanDirectoryFeature(String path) {
		ArrayList<String> output = new ArrayList<>();

		HashMap<String, HashSet<String>> rel = new HashMap<>();

		for (String[] s : FileUtils.readArffData(path)) {
			String features = "";
			s[0] = StringUtils.cleanUrlDataForPrediction(s[0]);

			String host = HttpUtils.getHost(s[0]);

			if (!rel.containsKey(host))
				rel.put(host, new HashSet<>());

			// s[0] = "'" + s[0].replace("{", "").replace("}", "").replace(",",
			// "").replace("%", "").replace("'", "").replace("`",
			// "").replace("\"", "") + "'";
			for (int i = 0; i < s.length; i++) {
				if (i != s.length - 1)
					features += s[i] + ",";
				else
					features += s[i];
			}

			String data = features.substring(features.indexOf(","));
			// System.out.println(data);
			if (rel.get(host).contains(data))
				continue;

			rel.get(host).add(data);
			output.add(features);
		}

		FileUtils.writeTextFile(path, header + "\n", false);
		FileUtils.writeTextFile(path, output, true);
	}

	private static double kFold(String filename, String destPath, boolean split) throws IOException {
		int k = 3;

		if (!FileUtils.exists(destPath))
			FileUtils.mkdir(destPath);

		ArrayList<String> all = FileUtils.readArffDataWithoutSplit(filename);
		String header = "@relation 'history'\n@attribute segpath string\n@attribute HostrelPagesRatio numeric\n@attribute HostrelSegsRatio numeric\n@attribute relPagesRatio numeric\n@attribute relSegsRatio numeric\n@attribute ParentRelPagesRatio numeric\n@attribute ParentRelSegsRatio numeric\n@attribute ChildRelPagesRatio numeric\n@attribute ChildRelSegsRatio numeric\n@attribute SiblingRelPagesRatio numeric\n@attribute SiblingRelSegsRatio numeric\n@attribute class {thai,non}\n@data\n";

		if (split) {
			ArrayList<String> rel = new ArrayList<>();
			ArrayList<String> non = new ArrayList<>();

			for (String s : all) {

				if (s.contains(",?,?,?,?,?,?,?,?,?,?,"))
					continue;

				String[] tmp = s.split(",");

				if (tmp[tmp.length - 1].trim().equals("non")) {
					non.add(s);
				} else {
					rel.add(s);
				}
			}

			logger.info(rel.size() + "\t" + non.size());
			System.out.println(rel.size());
			System.out.println(non.size());
			Collections.shuffle(rel);
			Collections.shuffle(non);

			int nonPerFile = (int) Math.ceil(non.size() * 1.0 / k);
			int thaiPerFile = (int) Math.ceil(rel.size() * 1.0 / k);

			for (int i = 0; i < k; i++) {
				BufferedWriter br = FileUtils.getBufferedFileWriter(destPath + "/n-" + i + "-rel.arff");
				BufferedWriter bn = FileUtils.getBufferedFileWriter(destPath + "/n-" + i + "-non.arff");
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

				ArrayList<String> r = FileUtils.readArffDataWithoutSplit(destPath + "/n-" + i + "-rel.arff");
				ArrayList<String> n = FileUtils.readArffDataWithoutSplit(destPath + "/n-" + i + "-non.arff");

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
					ArrayList<String> testU = underSampling(r, n, true);

					// System.out.println(rtest.size() + "\t" + ntest.size() +
					// "\t" + (testU == null));
					FileUtils.writeTextFile(destPath + "/test_u.arff", header, false);
					FileUtils.writeTextFile(destPath + "/test_u.arff", testU, true);
				}

			}

			System.out.println(rtrain.size() + "\t" + ntrain.size());
			ArrayList<String> output = underSampling(rtrain, ntrain, true);
			FileUtils.writeTextFile(destPath + "/train.arff", header, false);
			FileUtils.writeTextFile(destPath + "/train.arff", output, true);

			FileUtils.copyFile(destPath + "/train.arff", destPath + "/" + filename.replace("-raw", ""));
			FileUtils.writeTextFile(destPath + "/" + filename.replace("-raw", ""), FileUtils.readArffDataWithoutSplit(destPath + "/test_u.arff"), true);

			// train

			String algo = "weka.classifiers.bayes.NaiveBayesUpdateable";
			int[] rm = { 1 };
			WekaClassifier link = new WekaClassifier(algo, null, 1.0);
			try {
				link.train(destPath + "/train.arff", rm, true, null, true, false);

				g += link.getGMeanFromTestSet(destPath + "/test.arff");
			} catch (Exception e) {
				System.exit(0);
			}

		}

		logger.info(g / k);
		return g / k;
	}

	public static void parseLogTrainingNeighborhood(String path) {

		double d = 0;
		int count = 0;
		for (String s : FileUtils.readFile(path)) {
			s = s.toLowerCase();
			if (s.contains("tp") && s.contains("tn") && s.contains("fp") && s.contains("fn")) {
				s = s.substring(s.indexOf("tp"));
				String[] tmp = s.replace("tp", "").replace("tn", "").replace("fp", "").replace("fn", "").split("\t");
				ConfusionMatrixObj o = new ConfusionMatrixObj(Integer.parseInt(tmp[0].trim()), Integer.parseInt(tmp[1].trim()), Integer.parseInt(tmp[2].trim()), Integer.parseInt(tmp[3].trim()));
				System.out.println(o.getGmean());
				count++;
				d += o.getGmean();
			}

		}

		System.out.println("Average:\t" + d / count);
	}

	public static void main(String[] args) {

		// parseLogSeries("history.log", true);
		// parseLogSeries("history.log", false);
		// parseLogSeries("history.log", false);
		// parseLogSeries("history.log", false);

		// System.out.println(StringUtils.countWordInStr("www.phuket-pride.org/,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,thai",
		// "?"));

		String training = "ntourism-raw.arff";
		cleanDirectoryFeature(training);

		double max = 0;
		int index = 0;
		try {
			double result = 0;
			for (int i = 0; i < 10; i++) {
				result = kFold(training, training.replace(".arff", "").replace("-raw", "") + i, true);

				// result = kFold(training, training.replace("-raw",
				// "").replace(".arff", "") + i, false);
				if (result > max) {
					max = result;
					index = i;
				}
			}

			System.out.println(max + "\t" + index);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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

			double current_relevancy = HttpSegmentCrawler.calcRelevanceDegree(relPages.get(relPages.size() - 1), nonPages.get(relPages.size() - 1));
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

	public static void parsePreviousNeighborhood(String inputPath, String outputPath) {
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
			System.out.printf("Features count: %d\t%d\tRel:\t%d\tNon:\t%d\n", i, numFeatures.get(i), numFeaturesRel.get(i), numFeaturesNon.get(i));
		}
	}
}
