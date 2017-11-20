package com.job.ic.experiments;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import com.sleepycat.je.log.PrintFileReader;
import com.job.ic.crawlers.daos.ResultDb;
import com.job.ic.crawlers.daos.ResultDAO;
import com.job.ic.crawlers.daos.SegmentGraphDAO;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.models.ResultModel;
import com.job.ic.crawlers.models.SegmentGraphModel;
import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.nlp.services.Checker;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;

@SuppressWarnings("unused")
public class SegmentThresholdAnalyzer {

	
	public static void main(String[] args){
		parse("segment-threshold/logs-estate/ic.log");
	}
	
	public static void parse(String path) {
		
//		System.out.println(extractMaxSegmentThreshold("110100"));
//		
//		System.exit(0);
		ArrayList<String> rels = new ArrayList<>();
		ArrayList<String> nons = new ArrayList<>();
		
		for(String s: FileUtils.readFile(path)){
			if(!s.contains("Finished downloading")){
				continue;
			}
			
			s = s.substring(s.indexOf("Finished downloading") + "Finished downloading".length());
			s = s.trim();
			String[] data = s.split("\t");
//			2  4  6  10
			
			int rel = Integer.parseInt(data[2]);
			int total = Integer.parseInt(data[6]);
			String order = data[10];
			
			double degree = 1.0*rel/total;
			if(degree > CrawlerConfig.getConfig().getRelevanceDegreeThreshold()){
				rels.add(order + "\t" + extractMaxSegmentThreshold(order));
			}else{
				nons.add(order + "\t" + extractMaxSegmentThreshold(order));
			}
		}
		
		FileUtils.writeTextFile("rel-segs.txt", rels, false);
		FileUtils.writeTextFile("non-segs.txt", nons, false);


		double[] rel = analyzeSegmentThreshold("rel-segs.txt");

//		System.out.println("==================");
		double[] non = analyzeSegmentThreshold("non-segs.txt");
		double[] total = new double[rel.length];
		
		double sumRel = Arrays.stream(rel).sum();
		double sumNon = Arrays.stream(rel).sum();
		
		for(int i=0; i< rel.length; i++){
//			rel[i] /= sumRel;
//			non[i] /= sumNon;
//			total[i] = rel[i] + non[i];
//			
//			if(total[i] > 0){
//				rel[i] /= total[i];
//				non[i] /= total[i];
//			}
		
			if(i != 10)
				System.out.printf("%d\t%.0f\t%.0f\n", i, rel[i], non[i]);
			else
				System.out.printf("≥%d\t%.0f\t%.0f\n", i, rel[i], non[i]);
				
		}
		
		
		//
//		FileUtils.deleteFile("non-seg.txt");
//		FileUtils.deleteFile("thai-seg.txt");
		
		
		
		
		/*for (int i = 0; i <= 2; i++) {
			String dbPath = "db-train-diving/db-" + i;
			ResultDb.createEnvironment(dbPath);
			ResultDAO r = ResultDb.getResultDAO();

			ResultModel[] ms = r.getAll();
			int thai = 0, non = 0;
			try {
				BufferedWriter bwThai = FileUtils.getBufferedFileWriter("thai-seg.txt", true);
				BufferedWriter bwNon = FileUtils.getBufferedFileWriter("non-seg.txt", true);

				for (ResultModel m : ms) {
					if (Checker.getResultClass(ResultDAO.calcPercentRel(m)) == ResultClass.RELEVANT) {
						bwThai.write(m.getExtras() + "\t" + extractMaxSegmentThreshold(m.getExtras()) + "\n");
						thai++;
					} else {
						bwNon.write(m.getExtras() + "\t" + extractMaxSegmentThreshold(m.getExtras()) + "\n");
						non++;
					}
				}

				bwThai.close();
				bwNon.close();
			} catch (Exception e) {

			}

			ResultDb.close();
		}
		


		
		analyzeSegmentThreshold("non-seg.txt");
		System.out.println("==================");
		analyzeSegmentThreshold("thai-seg.txt");

		FileUtils.deleteFile("non-seg.txt");
		FileUtils.deleteFile("thai-seg.txt");*/
		
	}

	public static int extractMaxSegmentThreshold(String input) {

		int t = -1;
		int max = 0;
		int end = input.lastIndexOf("1");

		// แสดงว่าไม่มีไทยเลย
		if (end < 0)
			return -1;
		
		if(input.length() == 1)
			return 0;
		

		for (int i = 0; i < input.length(); i++) {
			char s = input.charAt(i);
			if (s == '0'){
				
				t++;
				
//				System.out.println(i + "\t" + input + "\t" + s + "\t" + t);
			}

			if (s == '1') {
				max = Math.max(max, t);
				t = 0;
			}
		}

		max = Math.max(max, t);
		
		
//		System.out.println(max);
		// มีไทยแค่ page เดียว
		
		return max;
	}

	public static double[] analyzeSegmentThreshold(String input) {
		Hashtable<String, Integer> counter = new Hashtable<>();
		double[] buckets = new double[11];
		for (String s : FileUtils.readFile(input)) {

			String target = s.split("\t")[1];
			int n = Integer.parseInt(target);
			if(n==-1)
				buckets[10]++;
			else if(n<10)
				buckets[n]++;
			else
				buckets[10]++;
			
//			if (counter.containsKey(target)) {
//				counter.put(target, counter.get(target) + 1);
//			} else {
//				counter.put(target, 1);
//			}
			
		}
		
//		for(int i=0; i<11; i++){
//			System.out.printf("%d\t%d\n", i, buckets[i]);
//		}

		return buckets;
//		Enumeration<String> e = counter.keys();
//		while (e.hasMoreElements()) {
//			String target = e.nextElement();
//			System.out.println(target + "\t" + counter.get(target));
//		}
		
	}
}
