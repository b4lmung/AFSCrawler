package com.job.ic.experiments;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Discretize;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.ml.classifiers.ClassifierUtils;
import com.job.ic.ml.classifiers.WekaClassifier;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;

public class AnalyzeMyLog {

	private String key;
	private int count;

	public static void main(String[] args) {
		// TODO Auto-generated method stub

//		CrawlerConfig.loadConfig("crawler.conf");
//		CrawlerConfig.config.setTargetLang("ja");
		String[] topics = {"tourism", "estate", "diving", "tourism-jp"};
		
		for(int t=0; t<=3; t++){
			String topic = topics[t];
		String path = "C:/Users/b4lmung/Documents/Research/current-research/" + topic + "/results/";
		String trainPath = "C:/Users/b4lmung/Documents/Research/current-research/" + topic + "/training-" + topic + ".arff";

		
		
		System.out.println("===========" + topic + "===========");
		ClassifierUtils.evaluatePredictor(trainPath, path + "/testing.arff");

//		 int feature = 5;
//		 mergeTestFile(path + "my-segment-" + topic, path + "bfs-segment-" + topic,  path + "best-segment-" + topic, trainPath);

		 // 0 = source, 1 = degree, 2 = domain, 3 = ip, 4 = anchor, 5 = url
//		 System.out.println("-----------training------------");
//		 analyzeFeatures(path + "/training_r.arff", feature);
//		 System.out.println("-----------testing------------");
//		 analyzeFeatures(path + "/testing_r.arff", feature);
		}
		
	}

	public static void analyzeFeatures(String training, String testing, int feature, boolean isThai) {

		String[] lines = FileUtils.readFile(training);

		Hashtable<String, Integer> trainCounter = new Hashtable<>();
		Hashtable<String, Integer> testCounter = new Hashtable<>();
		ArrayList<String> keys = new ArrayList<>();
		String target;
		String[] tmp;

		boolean isData = false;
		double countTrain = 0, countTest = 0;
		for (String s : lines) {
			tmp = s.split(",");
			if (s.contains("@data")) {
				isData = true;
				continue;
			}

			if (!isData)
				continue;

			if (isThai && s.toLowerCase().trim().endsWith(",non"))
				continue;

			if (!isThai && s.toLowerCase().trim().endsWith(",thai"))
				continue;

			countTrain++;

			target = tmp[feature];

			if (trainCounter.containsKey(target)) {
				trainCounter.put(target, trainCounter.get(target) + 1);
			} else {
				trainCounter.put(target, 1);

			}

			if (!keys.contains(target))
				keys.add(target);
		}

		// test
		lines = FileUtils.readFile(testing);

		isData = false;
		for (String s : lines) {
			tmp = s.split(",");
			if (s.contains("@data")) {
				isData = true;
				continue;
			}

			if (!isData)
				continue;

			if (isThai && s.toLowerCase().trim().endsWith(",non"))
				continue;

			if (!isThai && s.toLowerCase().trim().endsWith(",thai"))
				continue;

			countTest++;

			target = tmp[feature];

			if (testCounter.containsKey(target)) {
				testCounter.put(target, testCounter.get(target) + 1);
			} else {
				testCounter.put(target, 1);
			}

			if (!keys.contains(target))
				keys.add(target);
		}

		Collections.sort(keys);

		// if(feature == 2 || feature == 3){
		// //domain or ip
		// //list top 5 from train and test set
		//
		// ArrayList<AnalyzeMyLog> trainLog = new ArrayList<>();
		// ArrayList<AnalyzeMyLog> testLog = new ArrayList<>();
		//
		//
		// //train
		// Enumeration<String> e = trainCounter.keys();
		// while(e.hasMoreElements()){
		// target = e.nextElement();
		// trainLog.add(new AnalyzeMyLog(target, trainCounter.get(target)));
		// }
		//
		// //test
		// e = testCounter.keys();
		// while(e.hasMoreElements()){
		// target = e.nextElement();
		// testLog.add(new AnalyzeMyLog(target, testCounter.get(target)));
		// }
		//
		//
		// Collections.sort(trainLog, new Comparator<AnalyzeMyLog>() {
		//
		// @Override
		// public int compare(AnalyzeMyLog o1, AnalyzeMyLog o2) {
		// if(o1.getCount() > o2.getCount())
		// return -1;
		// else if(o1.getCount() < o2.getCount())
		// return 1;
		//
		// return 0;
		// }
		// });
		//
		// Collections.sort(testLog, new Comparator<AnalyzeMyLog>() {
		//
		// @Override
		// public int compare(AnalyzeMyLog o1, AnalyzeMyLog o2) {
		// if(o1.getCount() > o2.getCount())
		// return -1;
		// else if(o1.getCount() < o2.getCount())
		// return 1;
		//
		// return 0;
		// }
		// });
		//
		// ArrayList<AnalyzeMyLog> testLog = new ArrayList<>();
		//
		//
		// for(int i=0; i<5; i++){
		//
		//
		// }
		//
		// }else{
		// other number feature

		System.out.println("==========");
		for (String s : keys) {
			double train = 0, test = 0;

			// System.out.println(s);
			if (trainCounter.containsKey(s)) {
				train = trainCounter.get(s) / countTrain;
				// System.out.println(">>>" + trainCounter.get(s));
			}

			if (testCounter.containsKey(s)) {
				test = testCounter.get(s) / countTest;
				// System.out.println(">>>" + testCounter.get(s));
			}

			System.out.printf("%s\t%.3f\t%.3f\n", s, train, test);
		}

	}

	public static void analyzeFeatures(String testing, int feature) {
		System.out.println("key\tthai percent\tnon percent\tweight thai percent\tweight non percent\tthai\tnon");
		String[] lines = FileUtils.readFile(testing);

		Hashtable<String, Integer> thaiCounter = new Hashtable<>();
		Hashtable<String, Integer> nonCounter = new Hashtable<>();
		ArrayList<String> keys = new ArrayList<>();
		String target;
		String[] tmp;

		boolean isThai = true;
		boolean isData = false;
		double countThai = 0, countNon = 0;
		for (String s : lines) {
		
			
			
			if(s.toLowerCase().trim().contains("pictaero") || 
					s.toLowerCase().trim().contains(".com.vu") ||
					s.toLowerCase().trim().contains("colourlovers")){
				
				continue;
			}
			
			tmp = s.split(",");
			if (s.contains("@data")) {
				isData = true;
				continue;
			}

			if (!isData)
				continue;
			
			if(s.contains("pictaero.com"))
				continue;
	
			

			if (isThai && s.toLowerCase().trim().endsWith(",non"))
				continue;

			if (!isThai && s.toLowerCase().trim().endsWith(",thai"))
				continue;

			countThai++;

			target = tmp[feature];

			if (thaiCounter.containsKey(target)) {
				thaiCounter.put(target, thaiCounter.get(target) + 1);
			} else {
				thaiCounter.put(target, 1);

			}

			if (!keys.contains(target))
				keys.add(target);
		}

		// test
		lines = FileUtils.readFile(testing);
		isThai = false;
		isData = false;
		for (String s : lines) {
			

			if(s.toLowerCase().trim().contains("pictaero") || 
					s.toLowerCase().trim().contains(".com.vu") ||
					s.toLowerCase().trim().contains("colourlovers")){
				
				continue;
			}
			
			tmp = s.split(",");
			if (s.contains("@data")) {
				isData = true;
				continue;
			}

			if (!isData)
				continue;

			if (isThai && s.toLowerCase().trim().endsWith(",non"))
				continue;

			if (!isThai && s.toLowerCase().trim().endsWith(",thai"))
				continue;

			countNon++;

			target = tmp[feature];

			if (nonCounter.containsKey(target)) {
				nonCounter.put(target, nonCounter.get(target) + 1);
			} else {
				nonCounter.put(target, 1);
			}

			if (!keys.contains(target))
				keys.add(target);
		}

		if (feature == 2 || feature == 3) {
			ArrayList<AnalyzeMyLog> thaiLog = new ArrayList<>();
			ArrayList<AnalyzeMyLog> nonLog = new ArrayList<>();

			Enumeration<String> t = thaiCounter.keys();
			while (t.hasMoreElements()) {
				String key = t.nextElement();
				thaiLog.add(new AnalyzeMyLog(key, thaiCounter.get(key)));
			}

			t = nonCounter.keys();
			while (t.hasMoreElements()) {
				String key = t.nextElement();
				nonLog.add(new AnalyzeMyLog(key, nonCounter.get(key)));
			}

			Collections.sort(thaiLog, new Comparator<AnalyzeMyLog>() {

				@Override
				public int compare(AnalyzeMyLog o1, AnalyzeMyLog o2) {
					if (o1.getCount() > o2.getCount())
						return -1;
					else if (o1.getCount() < o2.getCount())
						return 1;

					return 0;
				}
			});

			Collections.sort(nonLog, new Comparator<AnalyzeMyLog>() {

				@Override
				public int compare(AnalyzeMyLog o1, AnalyzeMyLog o2) {
					if (o1.getCount() > o2.getCount())
						return -1;
					else if (o1.getCount() < o2.getCount())
						return 1;

					return 0;
				}
			});

			// get top 5 key
			ArrayList<String> key = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				if (!key.contains(thaiLog.get(i).getKey()))
					key.add(thaiLog.get(i).getKey());

				if (!key.contains(nonLog.get(i).getKey()))
					key.add(nonLog.get(i).getKey());
			}

			for (String s : key) {
				int thai = 0, non = 0;

				// System.out.println(s);
				if (thaiCounter.containsKey(s)) {
					thai = thaiCounter.get(s);
					// System.out.println(">>>" + trainCounter.get(s));
				}

				if (nonCounter.containsKey(s)) {
					non = nonCounter.get(s);
					// System.out.println(">>>" + testCounter.get(s));
				}

				double percentThai = 1.0 * thai / countThai;
				double percentNon = 1.0 * non / countNon;

				System.out.printf("%s\t%.3f\t%.3f\t%.3f\t%.3f\t%d\t%d\n", s, percentThai, percentNon, percentThai / (percentThai + percentNon), percentNon / (percentThai + percentNon), thai, non);
			}

		} else {

			Collections.sort(keys);

			for (String s : keys) {
				int thai = 0, non = 0;

				// System.out.println(s);
				if (thaiCounter.containsKey(s)) {
					thai = thaiCounter.get(s);
					// System.out.println(">>>" + trainCounter.get(s));
				}

				if (nonCounter.containsKey(s)) {
					non = nonCounter.get(s);
					// System.out.println(">>>" + testCounter.get(s));
				}

				double percentThai = 1.0 * thai / countThai;
				double percentNon = 1.0 * non / countNon;

				System.out.printf("%s\t%.5f\t%.5f\t%.5f\t%.5f\t%d\t%d\n", s, percentThai, percentNon, percentThai / (percentThai + percentNon), percentNon / (percentThai + percentNon), thai, non);
			}
		}

		System.out.println("\t" + countThai + "\t" + countNon);
	}

	public static void mergeTestFile(String myPath, String bfsPath, String besMytPath, String trainingPath) {
		ArrayList<String> thai = new ArrayList<>();
		ArrayList<String> non = new ArrayList<>();

		HashSet<String> data = new HashSet<>();
		HashSet<String> global = new HashSet<>();

		int ct = 0, nt=0;
		for (File f : FileUtils.getAllFile(myPath)) {
			if (f.getName().contains(".log") || f.getName().contains(".txt"))
				continue;
			if (f.isDirectory())
				continue;

			String[] lines = FileUtils.readFile(f.getPath());

			boolean isThai = false;
			if (f.getName().toLowerCase().contains("thai"))
				isThai = true;

			for (String line : lines) {
				String[] at = line.split(",");

				if (at.length != 11) {
					continue;
				}

				String srcHost = HttpUtils.getHost(at[0]);
				String destHost = HttpUtils.getHost(at[1]);

				if (srcHost == null || destHost == null)
					continue;

				boolean sameHost = false;
				if (srcHost.toLowerCase().trim().equals(destHost.toLowerCase().trim()))
					sameHost = true;

				String features = at[2].replace("'", "") + "," + at[3] + "," + at[4] + "," + at[5] + "," + at[6] + "," + at[7] + ",'" + at[8].replace("'", "") + "','" + at[9].replace("'", "") + "',"
						+ at[10];
				features = features.replaceAll("%", "_percent_").replace("\t", "");

				if (sameHost && data.contains(features)) {
					continue;
				}

				data.add(features);

				global.add(features.trim());

				if(features.contains("challenging-economic-times"))
					continue;
				
				if (isThai) {
					thai.add(features.substring(0, features.lastIndexOf(",") + 1) + "thai");
					ct++;
				} else {
					non.add(features.substring(0, features.lastIndexOf(",") + 1) + "non");
					nt++;
				}

			}
		}
		
		System.out.printf("My-segment:\t%s\t%s\n", ct, nt);
		ct = nt = 0;

		for (File f : FileUtils.getAllFile(bfsPath)) {
			boolean isThai = false;
			if (f.getName().toLowerCase().contains("thai"))
				isThai = true;

			if (f.getName().contains(".log") || f.getName().contains(".txt"))
				continue;

			if (f.isDirectory())
				continue;

			String[] lines = FileUtils.readFile(f.getPath());

			for (String line : lines) {

				String[] at = line.split(",");

				if (at.length != 11) {
					continue;
				}

				String srcHost = HttpUtils.getHost(at[0]);
				String destHost = HttpUtils.getHost(at[1]);

				if (srcHost == null || destHost == null)
					continue;

				boolean sameHost = false;
				if (srcHost.toLowerCase().trim().equals(destHost.toLowerCase().trim()))
					sameHost = true;

				String features = at[2].replace("'", "") + "," + at[3] + "," + at[4] + "," + at[5] + "," + at[6] + "," + at[7] + ",'" + at[8].replace("'", "") + "','" + at[9].replace("'", "") + "',"
						+ at[10];
				features = features.replaceAll("%", "_percent_").replace("\t", "");

				if (global.contains(features.trim()))
					continue;

				if (sameHost && data.contains(features)) {
					continue;
				}

				data.add(features);
				

				if(features.contains("challenging-economic-times"))
					continue;
				
				if (isThai) {
					ct++;
					thai.add(features.substring(0, features.lastIndexOf(",") + 1) + "thai");
				} else {
					nt++;
					non.add(features.substring(0, features.lastIndexOf(",") + 1) + "non");
				}

			}
		}
		
		System.out.printf("bfs-segment:\t%s\t%s\n", ct, nt);
		ct = nt = 0;
		
//		System.out.println(">>>" + besMytPath);
		for (File f : FileUtils.getAllFile(besMytPath)) {
			boolean isThai = false;
			if (f.getName().toLowerCase().contains("thai"))
				isThai = true;

			if (f.getName().contains(".log") || f.getName().contains(".txt"))
				continue;

			if (f.isDirectory())
				continue;

			String[] lines = FileUtils.readFile(f.getPath());

			for (String line : lines) {

				String[] at = line.split(",");

				if (at.length != 11) {
					continue;
				}

				String srcHost = HttpUtils.getHost(at[0]);
				String destHost = HttpUtils.getHost(at[1]);

				if (srcHost == null || destHost == null)
					continue;

				boolean sameHost = false;
				if (srcHost.toLowerCase().trim().equals(destHost.toLowerCase().trim()))
					sameHost = true;

				String features = at[2].replace("'", "") + "," + at[3] + "," + at[4] + "," + at[5] + "," + at[6] + "," + at[7] + ",'" + at[8].replace("'", "") + "','" + at[9].replace("'", "") + "',"
						+ at[10];
				features = features.replaceAll("%", "_percent_").replace("\t", "");

				if (global.contains(features.trim()))
					continue;

				if (sameHost && data.contains(features)) {
					continue;
				}

				data.add(features);
				

				if(features.contains("challenging-economic-times"))
					continue;
				
				if (isThai) {
					ct++;
					thai.add(features.substring(0, features.lastIndexOf(",") + 1) + "thai");
				} else {
					nt++;
					non.add(features.substring(0, features.lastIndexOf(",") + 1) + "non");
				}

			}
		}
		System.out.printf("best-segment:\t%s\t%s\n", ct, nt);
		ct = nt = 0;
	

		File f = new File(myPath);
		String[] t = FileUtils.readResourceFile("/header-cf.txt");
		String header = "";
		for (String s : t) {
			header += s + "\n";
		}
		FileUtils.writeTextFile(f.getParent() + "/testing.arff", header, false);
		FileUtils.writeTextFile(f.getParent() + "/testing.arff", thai, true);

		FileUtils.writeTextFile(f.getParent() + "/testing.arff", non, true);

		System.out.println("analyze " + trainingPath);
		try {
			DataSource training = new DataSource(trainingPath);

			DataSource testing = new DataSource(f.getParent() + "/testing.arff");

			Discretize disc = new Discretize();
			disc.setAttributeIndices("first-last");
			disc.setBins(10);
			disc.setInputFormat(training.getDataSet());

			Instances trainingr = Filter.useFilter(training.getDataSet(), disc);
			Instances testingr = Filter.useFilter(testing.getDataSet(), disc);

			FileUtils.writeTextFile(f.getParent() + "/testing_r.arff", testingr.toString(), false);
			FileUtils.writeTextFile(f.getParent() + "/training_r.arff", trainingr.toString(), false);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public AnalyzeMyLog(String key, int count) {
		super();
		this.key = key;
		this.count = count;
	}
}
