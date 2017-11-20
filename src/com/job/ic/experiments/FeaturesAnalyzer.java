package com.job.ic.experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.job.ic.extraction.FeaturesExtraction;
import com.job.ic.ml.classifiers.ConfusionMatrixObj;
import com.job.ic.ml.classifiers.PredictorPool;
import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.nlp.services.Checker;
import com.job.ic.utils.FileUtils;

public class FeaturesAnalyzer {
	private static Logger logger = Logger.getLogger(FeaturesAnalyzer.class);

	public static void main(String[] args) throws IOException {
//		analyzeLinkPredictionFromLog("logs/ic.log");
//		System.out.println("---");
//		analyzeAnchorPredictionFromLog("logs/ic.log");
//		System.out.println("---");
//		analyzeURLPredictionFromLog("logs/ic.log");
//		System.out.println("---");
//		analyzePredictionFromLog("logs/ic.log");
	
//		parseCurrentDirectoryLogB("logs-estate/ic.log", true);
//		parseCurrentDirectoryLogB("logs-estate/ic.log", false);
		
		
		System.exit(0);
		predict("test/estate", "predictor-estate.arff"); 
//		System.exit(0);
		
		String topic = "estate";
		
		ArrayList<String[]> data = FileUtils.readArffData("predictor-" + topic + ".arff");
		ArrayList<String[]> hop0 = loadFeaturesFromTestSet("test/" + topic + "", 0);
		ArrayList<String[]> hop1 = loadFeaturesFromTestSet("test/" + topic + "", 1);
		ArrayList<String[]> hop2 = loadFeaturesFromTestSet("test/" + topic + "", 2);
		ArrayList<String[]> hop3 = loadFeaturesFromTestSet("test/" + topic + "", 3);
//		ArrayList<String[]> hop4 = loadFeaturesFromTestSet("test/" + topic, 4);
		//
		ArrayList<String[]> testData = new ArrayList<String[]>(hop0);
		testData.addAll(hop1);
		testData.addAll(hop2);
		testData.addAll(hop3);

//		testData.addAll(hop4);
		//
		ArrayList<ArrayList<String[]>> all = new ArrayList<ArrayList<String[]>>();
		all.add(data);
		all.add(hop0);
		all.add(hop1);
		all.add(hop2);
		all.add(hop3);

//		all.add(hop4);
		all.add(testData);

		for (int i = 0; i < all.size(); i++) {
			analyzeSrcRelDegree(all.get(i), false);
//			analyzeSrcRelDegree(all.get(i), true);
//			analyzeSrcAvgRelScore(all.get(i));
//			analyzeThaiWordRatioFromAnchor(all.get(i));
//			analyzeThaiWordRatioFromUrl(all.get(i));
//			analyzeDomain(all.get(i));
//			analyzeCountry(all.get(i));
//			analyzeAnchorLang(all.get(i));
//			analyzeUrlLang(all.get(i));
			System.out.println("=====");
		}

	}


	public static void parseCurrentDirectoryLog(String path, boolean page){
		
		HashMap<Integer, Integer> rel = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> non = new HashMap<Integer, Integer>();
		
		for (int i = 1; i <= 10; i++) {
			rel.put(i, 0);
			non.put(i, 0);
		}
		
		int cTh = 0;
		int cNon = 0;
		
		int targetField = 4;
		boolean isPage = page;
		// 1 = currentDirectory, 2 = Host Directory, 3 = parent, 4 = child, 5 = sibling
		
		
		for(String s: FileUtils.readFile(path)){
			if(!s.contains("[http"))
				continue;
			
			s = s.substring(s.indexOf("INFO"));
			
			String[] tmp = s.split("\\[");
			String data = tmp[1].trim();

			if(data.indexOf("]") < 0)
				continue;
			
			//segment information
			data = data.substring(0, data.indexOf("]"));
			boolean isRelevant = Boolean.parseBoolean(data.split("\t")[2]);
			
			
			data = tmp[targetField].substring(tmp[targetField].indexOf("]")+1).trim();
			String[] t = data.split("\t");
			
			double sc;
			if(isPage)
				sc = Double.parseDouble(t[1].trim());
			else
				sc = Double.parseDouble(t[3].trim());
				
			if(sc < 0)
				continue;
			
			int score = (int) (sc * 10);
			if(score == 0)score = 1;
			
			if(isRelevant){
				cTh++;
				rel.put(score, rel.get(score) +1);
			}else{
				cNon++;
				non.put(score, non.get(score) +1);
			}
			
		}
		
		System.out.println("value\trel\tnon\trel%\tnon%\t");
		
		int totalRel = 0;
		int totalNon = 0;
		for(int i=1; i<=10; i++){
			
			double r = 1.0*rel.get(i)/cTh;
			double n = 1.0*non.get(i)/cNon;
			
			if(i!=10)
				System.out.printf("[%.1f-%.1f)\t%d\t%d\t%.3f\t%.3f\n", (i-1)/10.0, i/10.0, rel.get(i), non.get(i), (r+n)==0?0:r/(r+n)*100, (r+n)==0?0:n/(r+n)*100);
			else
				System.out.printf("[%.1f-%.1f]\t%d\t%d\t%.3f\t%.3f\n", (i-1)/10.0, i/10.0, rel.get(i), non.get(i), (r+n)==0?0:r/(r+n)*100, (r+n)==0?0:n/(r+n)*100);
				
		}
		
		
	}
	

	public static void parseCurrentDirectoryLogB(String path, boolean page){
		
		HashMap<Integer, Integer> rel = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> non = new HashMap<Integer, Integer>();
		
		for (int i = 1; i <= 2; i++) {
			rel.put(i, 0);
			non.put(i, 0);
		}
		
		int cTh = 0;
		int cNon = 0;
		
		int targetField = 4;
		boolean isPage = page;
		// 1 = currentDirectory, 2 = Host Directory, 3 = parent, 4 = child, 5 = sibling
		
		
		for(String s: FileUtils.readFile(path)){
			if(!s.contains("[http"))
				continue;
			
			s = s.substring(s.indexOf("INFO"));
			
			String[] tmp = s.split("\\[");
			String data = tmp[1].trim();

			if(data.indexOf("]") < 0)
				continue;
			
			//segment information
			data = data.substring(0, data.indexOf("]"));
			boolean isRelevant = Boolean.parseBoolean(data.split("\t")[2]);
			
			
			data = tmp[targetField].substring(tmp[targetField].indexOf("]")+1).trim();
			String[] t = data.split("\t");
			
			double sc;
			if(isPage)
				sc = Double.parseDouble(t[1].trim());
			else
				sc = Double.parseDouble(t[3].trim());
				
			if(sc < 0)
				continue;
			
			int score = (int) (sc * 10);
			
			if(score >=5)
				score = 2;
			else
				score = 1;
			
			
			if(isRelevant){
				cTh++;
				rel.put(score, rel.get(score) +1);
			}else{
				cNon++;
				non.put(score, non.get(score) +1);
			}
			
		}
		
		System.out.println("value\trel\tnon\trel%\tnon%\t");
		
		double r = 1.0*rel.get(1)/cTh;
		double n = 1.0*non.get(1)/cNon;
		
		System.out.printf("[%.1f-%.1f)\t%d\t%d\t%.3f\t%.3f\n", 0.0, 0.5, rel.get(1), non.get(1), (r+n)==0?0:r/(r+n)*100, (r+n)==0?0:n/(r+n)*100);
		
		r = 1.0*rel.get(2)/cTh;
		n = 1.0*non.get(2)/cNon;
		
		System.out.printf("[%.1f-%.1f]\t%d\t%d\t%.3f\t%.3f\n", 0.5, 1.0, rel.get(2), non.get(2), (r+n)==0?0:r/(r+n)*100, (r+n)==0?0:n/(r+n)*100);
		System.out.println();
		
		
		
	}
	
	
	public static void predict(String prefix, String trainingPath) {
		
		ArrayList<String[]> hop0 = loadFeaturesFromTestSet(prefix, 0);
		ArrayList<String[]> hop1 = loadFeaturesFromTestSet(prefix, 1);
		ArrayList<String[]> hop2 = loadFeaturesFromTestSet(prefix, 2);
		ArrayList<String[]> hop3 = loadFeaturesFromTestSet(prefix, 3);

		PredictorPool.trainPredictor(trainingPath);

		logger.warn("---------- hop 0 -----------");

		try {
			PredictorPool.predict(hop0);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.warn("---------- hop 1 -----------");

		try {
			PredictorPool.predict(hop1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.warn("---------- hop 2 -----------");

		try {
			PredictorPool.predict(hop2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.warn("---------- hop 3 -----------");

		try {
			PredictorPool.predict(hop3);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("fin");
		
		analyzeLinkPredictionFromLog("logs/ic.log");
		System.out.println("---");
		analyzeAnchorPredictionFromLog("logs/ic.log");
		System.out.println("---");
		analyzeURLPredictionFromLog("logs/ic.log");
		System.out.println("---");
		analyzePredictionFromLog("logs/ic.log");
		
	}

	public static void analyzeLinkPredictionFromLog(String path) {
		String[] lines = FileUtils.readFile(path);
		ConfusionMatrixObj[] cf = new ConfusionMatrixObj[4];
		for (int i = 0; i < 4; i++)
			cf[i] = new ConfusionMatrixObj();

		int i = 0;
		for (String s : lines) {
			if (s.contains("PredictorPool:68- finished"))
				i++;

			if (!s.contains("http://"))
				continue;

			s = s.substring(s.indexOf("http://")).trim();
			String[] result = s.split("\t");
			boolean isThai = false;
			if (result[1].equals("thai"))
				isThai = true;

			// 2 link, 3 anchor, 4 url, 5 total
			int target = 2;
			if (isThai) {
				if (result[target].equals("IRRELEVANT")) {
					// ทำนายไทยเป็น non
					cf[i].incFn();

				} else {
					// ทำนายไทยเป็น thai
					cf[i].incTp();
				}
			} else {
				if (result[target].equals("IRRELEVANT")) {
					// ทำนาย non เป็น non
					cf[i].incTn();

				} else {
					// ทำนาย non เป็น thai
					cf[i].incFp();
				}
			}
		}

		ConfusionMatrixObj fin = new ConfusionMatrixObj();
		fin.setFn(cf[0].getFn() + cf[1].getFn() + cf[2].getFn() + cf[3].getFn());
		fin.setFp(cf[0].getFp() + cf[1].getFp() + cf[2].getFp() + cf[3].getFp());
		fin.setTn(cf[0].getTn() + cf[1].getTn() + cf[2].getTn() + cf[3].getTn());
		fin.setTp(cf[0].getTp() + cf[1].getTp() + cf[2].getTp() + cf[3].getTp());

		System.out.println("hop0\t" + cf[0].getGmean());
		System.out.println("hop1\t" + cf[1].getGmean());
		System.out.println("hop2\t" + cf[2].getGmean());
		System.out.println("hop3\t" + cf[3].getGmean());
		System.out.println("total\t" + fin.getGmean());

	}
	
	public static void analyzeAnchorPredictionFromLog(String path) {
		String[] lines = FileUtils.readFile(path);
		ConfusionMatrixObj[] cf = new ConfusionMatrixObj[4];
		for (int i = 0; i < 4; i++)
			cf[i] = new ConfusionMatrixObj();

		int i = 0;
		for (String s : lines) {
			if (s.contains("PredictorPool:68- finished"))
				i++;

			if (!s.contains("http://"))
				continue;

			s = s.substring(s.indexOf("http://")).trim();
			String[] result = s.split("\t");
			boolean isThai = false;
			if (result[1].equals("thai"))
				isThai = true;

			// 2 link, 3 anchor, 4 url, 5 total
			int target = 3;
			if (isThai) {
				if (result[target].equals("IRRELEVANT")) {
					// ทำนายไทยเป็น non
					cf[i].incFn();

				} else {
					// ทำนายไทยเป็น thai
					cf[i].incTp();
				}
			} else {
				if (result[target].equals("IRRELEVANT")) {
					// ทำนาย non เป็น non
					cf[i].incTn();

				} else {
					// ทำนาย non เป็น thai
					cf[i].incFp();
				}
			}
		}

		ConfusionMatrixObj fin = new ConfusionMatrixObj();
		fin.setFn(cf[0].getFn() + cf[1].getFn() + cf[2].getFn() + cf[3].getFn());
		fin.setFp(cf[0].getFp() + cf[1].getFp() + cf[2].getFp() + cf[3].getFp());
		fin.setTn(cf[0].getTn() + cf[1].getTn() + cf[2].getTn() + cf[3].getTn());
		fin.setTp(cf[0].getTp() + cf[1].getTp() + cf[2].getTp() + cf[3].getTp());

		System.out.println("hop0\t" + cf[0].getGmean());
		System.out.println("hop1\t" + cf[1].getGmean());
		System.out.println("hop2\t" + cf[2].getGmean());
		System.out.println("hop3\t" + cf[3].getGmean());
		System.out.println("total\t" + fin.getGmean());

	}
	
	public static void analyzeURLPredictionFromLog(String path) {
		String[] lines = FileUtils.readFile(path);
		ConfusionMatrixObj[] cf = new ConfusionMatrixObj[4];
		for (int i = 0; i < 4; i++)
			cf[i] = new ConfusionMatrixObj();

		int i = 0;
		for (String s : lines) {
			if (s.contains("PredictorPool:68- finished"))
				i++;

			if (!s.contains("http://"))
				continue;

			s = s.substring(s.indexOf("http://")).trim();
			String[] result = s.split("\t");
			boolean isThai = false;
			if (result[1].equals("thai"))
				isThai = true;

			// 2 link, 3 anchor, 4 url, 5 total
			int target = 4;
			if (isThai) {
				if (result[target].equals("IRRELEVANT")) {
					// ทำนายไทยเป็น non
					cf[i].incFn();

				} else {
					// ทำนายไทยเป็น thai
					cf[i].incTp();
				}
			} else {
				if (result[target].equals("IRRELEVANT")) {
					// ทำนาย non เป็น non
					cf[i].incTn();

				} else {
					// ทำนาย non เป็น thai
					cf[i].incFp();
				}
			}
		}

		ConfusionMatrixObj fin = new ConfusionMatrixObj();
		fin.setFn(cf[0].getFn() + cf[1].getFn() + cf[2].getFn() + cf[3].getFn());
		fin.setFp(cf[0].getFp() + cf[1].getFp() + cf[2].getFp() + cf[3].getFp());
		fin.setTn(cf[0].getTn() + cf[1].getTn() + cf[2].getTn() + cf[3].getTn());
		fin.setTp(cf[0].getTp() + cf[1].getTp() + cf[2].getTp() + cf[3].getTp());

		System.out.println("hop0\t" + cf[0].getGmean());
		System.out.println("hop1\t" + cf[1].getGmean());
		System.out.println("hop2\t" + cf[2].getGmean());
		System.out.println("hop3\t" + cf[3].getGmean());
		System.out.println("total\t" + fin.getGmean());

	}
	
	public static void analyzePredictionFromLog(String path) {
		String[] lines = FileUtils.readFile(path);
		ConfusionMatrixObj[] cf = new ConfusionMatrixObj[4];
		for (int i = 0; i < 4; i++)
			cf[i] = new ConfusionMatrixObj();

		int i = 0;
		for (String s : lines) {
			if (s.contains("PredictorPool:68- finished"))
				i++;

			if (!s.contains("http://"))
				continue;

			s = s.substring(s.indexOf("http://")).trim();
			String[] result = s.split("\t");
			boolean isThai = false;
			if (result[1].equals("thai"))
				isThai = true;

			// 2 link, 3 anchor, 4 url, 5 total
			int target = 5;
			if (isThai) {
				if (result[target].equals("IRRELEVANT")) {
					// ทำนายไทยเป็น non
					cf[i].incFn();

				} else {
					// ทำนายไทยเป็น thai
					cf[i].incTp();
				}
			} else {
				if (result[target].equals("IRRELEVANT")) {
					// ทำนาย non เป็น non
					cf[i].incTn();

				} else {
					// ทำนาย non เป็น thai
					cf[i].incFp();
				}
			}
		}

		ConfusionMatrixObj fin = new ConfusionMatrixObj();
		fin.setFn(cf[0].getFn() + cf[1].getFn() + cf[2].getFn() + cf[3].getFn());
		fin.setFp(cf[0].getFp() + cf[1].getFp() + cf[2].getFp() + cf[3].getFp());
		fin.setTn(cf[0].getTn() + cf[1].getTn() + cf[2].getTn() + cf[3].getTn());
		fin.setTp(cf[0].getTp() + cf[1].getTp() + cf[2].getTp() + cf[3].getTp());

		System.out.println("hop0\t" + cf[0].getGmean());
		System.out.println("hop1\t" + cf[1].getGmean());
		System.out.println("hop2\t" + cf[2].getGmean());
		System.out.println("hop3\t" + cf[3].getGmean());
		System.out.println("total\t" + fin.getGmean());

	}

	public static ArrayList<String[]> loadFeaturesFromTestSet(String prefix, int hop) {
		ArrayList<String[]> data = FileUtils.readArffData(prefix + "-" + hop + "-non.arff");
		data.addAll(FileUtils.readArffData(prefix + "-" + hop + "-thai.arff"));
		data.addAll(FileUtils.readArffData(prefix + "-" + hop + "-internal.arff-intNon.arff"));
		data.addAll(FileUtils.readArffData(prefix + "-" + hop + "-internal.arff-intThai.arff"));

		return data;
	}

	public static void analyzeSrcRelDegree(ArrayList<String[]> data, boolean categorize) {
		if (categorize) {
			HashMap<Integer, Integer> thai = new HashMap<Integer, Integer>();
			for (int i = 0; i <= 10; i++) {
				thai.put(i, 0);
			}

			HashMap<Integer, Integer> non = new HashMap<Integer, Integer>(thai);
			double tc = 0, nc = 0;

			for (String[] s : data) {
				// index = 1;
				double sc = Double.parseDouble(s[1]);
				int score = (int) (sc * 10);

				if (s[s.length - 1].equals("non")) {
					non.put(score, non.get(score) + 1);
					nc++;
				} else {
					thai.put(score, thai.get(score) + 1);
					tc++;
				}
			}

			// calculate
			System.out.println("value\tthai%\tnon%");
			for (int i = 0; i <= 10; i++) {
				System.out.printf("%d\t%.2f\t%.2f\n", i, thai.get(i) + non.get(i) > 0 ? (1.0 * thai.get(i) / tc) / (thai.get(i) / tc + non.get(i) / nc) : 0,
						thai.get(i) + non.get(i) > 0 ? (1.0 * non.get(i) / nc) / (thai.get(i) / tc + non.get(i) / nc) : 0);
			}
		} else {

			int srcThai = 0, srcNon = 0;
			double destThaiFromThai = 0, destNonFromNon = 0, destThaiFromNon = 0, destNonFromThai = 0;
			boolean isSrcThai = false;
			for (String[] s : data) {
				isSrcThai = false;
				// index = 1;
				
				double score = Double.parseDouble(s[1]);
				if (Checker.getResultClass(score) == ResultClass.RELEVANT) {
					srcThai++;
					isSrcThai = true;
				} else {
					srcNon++;
				}

				if (s[s.length - 1].equals("non")) {
					if (isSrcThai) {
						destNonFromThai++;
					} else {
						destNonFromNon++;
					}
				} else {
					if (isSrcThai) {
						destThaiFromThai++;
					} else {
						destThaiFromNon++;
					}
				}
			}

			// calculate
			destNonFromNon /= srcNon;
			destNonFromThai /= srcThai;
			destThaiFromNon /= srcNon;
			destThaiFromThai /= srcThai;

			System.out.println("s\\d \t thai \t non \t totalLinks");
			System.out.printf("Thai:\t%.3f\t%.3f\t%d\n", destThaiFromThai, destNonFromThai, srcThai);
			System.out.printf("Non:\t%.3f\t%.3f\t%d\n", destThaiFromNon, destNonFromNon, srcNon);
		}
	}

	public static void analyzeSrcAvgRelScore(ArrayList<String[]> data) {
		HashMap<Integer, Integer> thai = new HashMap<Integer, Integer>();
		for (int i = 0; i <= 10; i++) {
			thai.put(i, 0);
		}

		HashMap<Integer, Integer> non = new HashMap<Integer, Integer>(thai);
		double tc = 0, nc = 0;

		for (String[] s : data) {
			// index = 1;
			double sc = Double.parseDouble(s[2]);
			int score = (int) (sc * 10);

			if (s[s.length - 1].equals("non")) {
				non.put(score, non.get(score) + 1);
				nc++;
			} else {
				thai.put(score, thai.get(score) + 1);
				tc++;
			}
		}

		// calculate
		System.out.println("value\tthai%\tnon%");
		for (int i = 0; i <= 10; i++) {
			System.out.printf("%d\t%.2f\t%.2f\n", i, thai.get(i) + non.get(i) > 0 ? (1.0 * thai.get(i) / tc) / (thai.get(i) / tc + non.get(i) / nc) : 0,
					thai.get(i) + non.get(i) > 0 ? (1.0 * non.get(i) / nc) / (thai.get(i) / tc + non.get(i) / nc) : 0);
		}

		// System.out.println("---- dest non ----");
		// for (int i = 0; i <= 10; i++) {
		// System.out.printf("%d\t%.2f\t%d\n", i, thai.get(i) + non.get(i) > 0 ?
		// (1.0 * non.get(i) / nc) / (thai.get(i) / tc + non.get(i) / nc) : 0,
		// non.get(i));
		// }
	}

	public static void analyzeThaiWordRatioFromAnchor(ArrayList<String[]> data) {
		HashMap<Integer, Integer> thai = new HashMap<Integer, Integer>();
		for (int i = 0; i <= 10; i++) {
			thai.put(i, 0);
		}

		HashMap<Integer, Integer> non = new HashMap<Integer, Integer>(thai);
		// HashMap<Integer, Integer> total = new HashMap<Integer,
		// Integer>(thai);

		double tc = 0, nc = 0;

		for (String[] s : data) {
			// index = 1;
			double sc = Double.parseDouble(s[5]);
			int score = (int) (sc * 10);

			if (s[s.length - 1].equals("non")) {
				non.put(score, non.get(score) + 1);
				nc++;
			} else {
				thai.put(score, thai.get(score) + 1);
				tc++;
			}
		}

		// calculate
		// System.out.println("---- dest thai ----");
		// for (int i = 0; i <= 10; i++) {
		// System.out.printf("%d\t%.2f\t%d\n", i, thai.get(i) + non.get(i) > 0 ?
		// (1.0 * thai.get(i) / tc) / (thai.get(i) / tc + non.get(i) / nc) : 0,
		// thai.get(i));
		// }
		//
		// System.out.println("---- dest non ----");
		// for (int i = 0; i <= 10; i++) {
		// System.out.printf("%d\t%.2f\t%d\n", i, thai.get(i) + non.get(i) > 0 ?
		// (1.0 * non.get(i) / nc) / (thai.get(i) / tc + non.get(i) / nc) : 0,
		// non.get(i));
		// }

		System.out.println("value\tthai%\tnon%");
		for (int i = 0; i <= 10; i++) {
			System.out.printf("%d\t%.2f\t%.2f\n", i, thai.get(i) + non.get(i) > 0 ? (1.0 * thai.get(i) / tc) / (thai.get(i) / tc + non.get(i) / nc) : 0,
					thai.get(i) + non.get(i) > 0 ? (1.0 * non.get(i) / nc) / (thai.get(i) / tc + non.get(i) / nc) : 0);
		}
	}

	public static void analyzeThaiWordRatioFromUrl(ArrayList<String[]> data) {
		HashMap<Integer, Integer> thai = new HashMap<Integer, Integer>();
		for (int i = 0; i <= 10; i++) {
			thai.put(i, 0);
		}
		double tc = 0, nc = 0;

		HashMap<Integer, Integer> non = new HashMap<Integer, Integer>(thai);

		for (String[] s : data) {
			// index = 1;
			double sc = Double.parseDouble(s[6]);
			int score = (int) (sc * 10);

			if (s[s.length - 1].equals("non")) {
				non.put(score, non.get(score) + 1);
				nc++;
			} else {
				thai.put(score, thai.get(score) + 1);
				tc++;
			}
		}

		// calculate
		// System.out.println("---- dest thai ----");
		// for (int i = 0; i <= 10; i++) {
		// System.out.printf("%d\t%.2f\t%d\n", i, thai.get(i) + non.get(i) > 0 ?
		// (1.0 * thai.get(i) / tc) / (thai.get(i) / tc + non.get(i) / nc) : 0,
		// thai.get(i));
		// }
		//
		// System.out.println("---- dest non ----");
		// for (int i = 0; i <= 10; i++) {
		// System.out.printf("%d\t%.2f\t%d\n", i, thai.get(i) + non.get(i) > 0 ?
		// (1.0 * non.get(i) / nc) / (thai.get(i) / tc + non.get(i) / nc) : 0,
		// non.get(i));
		// }
		System.out.println("value\tthai%\tnon%");
		for (int i = 0; i <= 10; i++) {
			System.out.printf("%d\t%.2f\t%.2f\n", i, thai.get(i) + non.get(i) > 0 ? (1.0 * thai.get(i) / tc) / (thai.get(i) / tc + non.get(i) / nc) : 0,
					thai.get(i) + non.get(i) > 0 ? (1.0 * non.get(i) / nc) / (thai.get(i) / tc + non.get(i) / nc) : 0);
		}
	}

	public static void analyzeDomain(ArrayList<String[]> data) {
		HashMap<String, Integer> destThai = new HashMap<String, Integer>();
		HashMap<String, Integer> destNon = new HashMap<String, Integer>();

		double ct = 0, cn = 0;
		for (String[] s : data) {
			String value = s[3];
			if (s[s.length - 1].equals("non")) {
				if (destNon.containsKey(value))
					destNon.put(value, destNon.get(value) + 1);
				else
					destNon.put(value, 1);

				cn++;
			} else {
				if (destThai.containsKey(value))
					destThai.put(value, destThai.get(value) + 1);
				else
					destThai.put(value, 1);

				ct++;
			}
		}

		// finding common subset
		HashMap<String, Integer> tt = new HashMap<String, Integer>();

		HashMap<String, Double> totalSet = new HashMap<String, Double>();
		for (String s : destThai.keySet()) {
			totalSet.put(s, destThai.get(s) / ct);
			tt.put(s, destThai.get(s));
		}

		for (String s : destNon.keySet()) {
			if (!totalSet.containsKey(s)) {
				totalSet.put(s, destNon.get(s) / cn);
				tt.put(s, destNon.get(s));
			} else {
				totalSet.put(s, totalSet.get(s) + (destNon.get(s) / cn));
				tt.put(s, tt.get(s) + destNon.get(s));
			}

		}

		ArrayList<String> key = new ArrayList<String>(destThai.keySet());
		Collections.sort(key, (a1, a2) -> (-1 * Double.compare(tt.get(a1), tt.get(a2))));

		System.out.println("key\tthai%\tnon%");
		for (int i = 0; i < Math.min(10, key.size()); i++) {
			String k = key.get(i);
			// System.out.println((destThai.containsKey(k)?destThai.get(k):0) +
			// "\t" + (destNon.containsKey(k)?(destNon.get(k) ):0));
			// System.out.println((destThai.containsKey(k)?(1.0 *
			// destThai.get(k) / ct):0) + "\t" + (destNon.containsKey(k)?(1.0 *
			// destNon.get(k) / cn):0));
			System.out.printf("%s\t%.2f\t%.2f\n", k, totalSet.get(k) > 0 && destThai.containsKey(k) ? (1.0 * destThai.get(k) / ct) / totalSet.get(k) : 0,
					totalSet.get(k) > 0 && destNon.containsKey(k) ? (1.0 * destNon.get(k) / cn) / totalSet.get(k) : 0);
		}

	}

	public static void analyzeCountry(ArrayList<String[]> data) {
		HashMap<String, Integer> destThai = new HashMap<String, Integer>();
		HashMap<String, Integer> destNon = new HashMap<String, Integer>();

		double ct = 0, cn = 0;
		for (String[] s : data) {
			String value = s[4];
			if (s[s.length - 1].equals("non")) {
				if (destNon.containsKey(value))
					destNon.put(value, destNon.get(value) + 1);
				else
					destNon.put(value, 1);
				cn++;
			} else {
				if (destThai.containsKey(value))
					destThai.put(value, destThai.get(value) + 1);
				else
					destThai.put(value, 1);

				ct++;
			}
		}

		// finding common subset
		HashMap<String, Integer> tt = new HashMap<String, Integer>();

		HashMap<String, Double> totalSet = new HashMap<String, Double>();
		for (String s : destThai.keySet()) {
			totalSet.put(s, destThai.get(s) / ct);
			tt.put(s, destThai.get(s));
		}

		for (String s : destNon.keySet()) {
			if (!totalSet.containsKey(s)) {
				totalSet.put(s, destNon.get(s) / cn);
				tt.put(s, destNon.get(s));
			} else {
				totalSet.put(s, totalSet.get(s) + (destNon.get(s) / cn));
				tt.put(s, tt.get(s) + destNon.get(s));
			}

		}

		ArrayList<String> key = new ArrayList<String>(destThai.keySet());
		Collections.sort(key, (a1, a2) -> (-1 * Double.compare(tt.get(a1), tt.get(a2))));

		System.out.println("key\tthai%\tnon%");
		for (int i = 0; i < Math.min(10, key.size()); i++) {
			String k = key.get(i);

			System.out.printf("%s\t%.2f\t%.2f\n", k, totalSet.get(k) > 0 && destThai.containsKey(k) ? (1.0 * destThai.get(k) / ct) / totalSet.get(k) : 0,
					totalSet.get(k) > 0 && destNon.containsKey(k) ? (1.0 * destNon.get(k) / cn) / totalSet.get(k) : 0);
		}

	}

	public static void analyzeAnchorLang(ArrayList<String[]> data) {
		HashSet<String> langSet = FeaturesExtraction.langSet;
		HashMap<String, Integer> thai = new HashMap<String, Integer>();
		for (String s : langSet) {
			thai.put(s, 0);
		}
		thai.put("other", 0);

		HashMap<String, Integer> non = new HashMap<String, Integer>(thai);

		double ct = 0, cn = 0;
		for (String[] s : data) {
			String value = s[7];
			if (s[s.length - 1].equals("non")) {
				non.put(value, non.get(value) + 1);
				cn++;
			} else {
				thai.put(value, thai.get(value) + 1);
				ct++;
			}
		}

		HashMap<String, Double> totalSet = new HashMap<String, Double>();

		HashMap<String, Integer> tt = new HashMap<String, Integer>();
		for (String s : langSet) {
			if (thai.containsKey(s)) {
				totalSet.put(s, thai.get(s) / ct);
				tt.put(s, thai.get(s));
			}
		}

		for (String s : langSet) {

			if (totalSet.containsKey(s)) {
				totalSet.put(s, totalSet.get(s) + (non.get(s) / cn));
				tt.put(s, tt.get(s) + non.get(s));
			} else {
				totalSet.put(s, non.get(s) / cn);
				tt.put(s, non.get(s));
			}

		}

		ArrayList<String> key = new ArrayList<String>(langSet);
		key.sort((a1, a2) -> (-1 * Double.compare(tt.get(a1), tt.get(a2))));

		System.out.println("key\tthai%\tnon%");
		for (int i = 0; i <= 10; i++) {
			String k = key.get(i);
			System.out.printf("%s\t%.2f\t%.2f\n", k, totalSet.get(k) > 0 && thai.containsKey(k) ? (1.0 * thai.get(k) / ct) / totalSet.get(k) : 0,
					totalSet.get(k) > 0 && non.containsKey(k) ? (1.0 * non.get(k) / cn) / totalSet.get(k) : 0);
		}

		// System.out.println("----- dest thai -------");
		//
		// for (int i = 0; i <= 10; i++) {
		// System.out.printf("%s\t%.2f\t%d\n", key.get(i),
		// totalSet.get(key.get(i)) > 0 ? (1.0 * thai.get(key.get(i))/ct) /
		// totalSet.get(key.get(i)) : 0, thai.get(key.get(i)));
		// }
		//
		// System.out.println("----- dest non -------");
		// key.sort((a, b) -> -1 * non.get(a).compareTo(non.get(b)));
		// for (int i = 0; i <= 10; i++) {
		// System.out.printf("%s\t%.2f\t%d\n", key.get(i),
		// totalSet.get(key.get(i)) > 0 ? (1.0 * non.get(key.get(i))/cn) /
		// totalSet.get(key.get(i)) : 0, non.get(key.get(i)));
		// }

	}

	public static void analyzeUrlLang(ArrayList<String[]> data) {
		HashSet<String> langSet = FeaturesExtraction.langSet;
		HashMap<String, Integer> thai = new HashMap<String, Integer>();
		for (String s : langSet) {
			thai.put(s, 0);
		}
		thai.put("other", 0);

		HashMap<String, Integer> non = new HashMap<String, Integer>(thai);

		double ct = 0, cn = 0;
		for (String[] s : data) {
			String value = s[8];
			if (s[s.length - 1].equals("non")) {
				non.put(value, non.get(value) + 1);
				cn++;
			} else {
				thai.put(value, thai.get(value) + 1);
				ct++;
			}
		}

		HashMap<String, Double> totalSet = new HashMap<String, Double>();
		for (String s : langSet) {
			if (thai.containsKey(s))
				totalSet.put(s, thai.get(s) / ct);
		}

		for (String s : langSet) {

			if (totalSet.containsKey(s)) {
				totalSet.put(s, totalSet.get(s) + (non.get(s) / cn));
			} else {
				totalSet.put(s, non.get(s) / cn);
			}

		}

		ArrayList<String> key = new ArrayList<String>(langSet);
		key.sort((a1, a2) -> (-1 * Double.compare(totalSet.get(a1), totalSet.get(a2))));

		System.out.println("key\tthai%\tnon%");
		for (int i = 0; i <= 10; i++) {
			String k = key.get(i);

			System.out.printf("%s\t%.2f\t%.2f\n", k, totalSet.get(k) > 0 && thai.containsKey(k) ? (1.0 * thai.get(k) / ct) / totalSet.get(k) : 0,
					totalSet.get(k) > 0 && non.containsKey(k) ? (1.0 * non.get(k) / cn) / totalSet.get(k) : 0);
		}

	}
}
