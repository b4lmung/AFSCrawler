package com.job.ic.experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.daos.ResultDAO;
import com.job.ic.crawlers.daos.ResultDb;
import com.job.ic.crawlers.daos.SegmentGraphDAO;
import com.job.ic.crawlers.models.ResultModel;
import com.job.ic.crawlers.models.SegmentGraphModel;
import com.job.ic.crawlers.parser.HtmlParser;
public class OptimalThreshold {

	private static Logger logger = Logger.getLogger(OptimalThreshold.class);
	public static void main(String[] args) {

		String dbPath = "db-train-diving/";
		logger.info(dbPath);
		try {
			optimal(dbPath, 0.25);
			System.out.println("----");
			// System.exit(0);
			optimal(dbPath, 0.5);
			System.out.println("----");
			optimal(dbPath, 0.75);
			System.out.println("----");
			optimal(dbPath, 0.99);
			System.out.println("----");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void optimal(String dbPath, double threshold) throws IOException {
		// double threshold = 1.0;

		HashMap<String, ResultModel>[] results = new HashMap[3];
		HashMap<String, HashSet<String>>[] fwGraph = new HashMap[3];

		HashMap<String, HashSet<String>>[] reverseGraph = new HashMap[3];
		for (int i = 0; i < 3; i++) {
			fwGraph[i] = new HashMap<>();
			reverseGraph[i] = new HashMap<>();
			results[i] = new HashMap<>();
		}

		double totalRel = 0;

		// load all data first

		for (int i = 0; i <= 2; i++) {
			ResultDb.createEnvironment(dbPath + "db-" + i);
			ResultDAO rd = ResultDb.getResultDAO();
			SegmentGraphDAO sd = ResultDb.getSegmentGraphDAO();

			for (ResultModel m : rd.getAll()) {
				results[i].put(m.getSegmentName().toLowerCase(), m);
				totalRel += m.getRelevantPage();
			}
			
			if(i != 2)
			for (SegmentGraphModel sg : sd.getAll()) {

				if (!reverseGraph[i].containsKey(sg.getDestSeg())) {
					reverseGraph[i].put(sg.getDestSeg(), new HashSet<>());
				}
				
				if(!fwGraph[i].containsKey(sg.getSourceSeg()))
					fwGraph[i].put(sg.getSourceSeg(), new HashSet<>());
				
				fwGraph[i].get(sg.getSourceSeg()).add(sg.getDestSeg());
				
				reverseGraph[i].get(sg.getDestSeg()).add(sg.getSourceSeg());
			}

			ResultDb.close();

		}
		
		

		// list all relevant seg from H2
		HashSet<String> downloadListH2 = new HashSet<>();
		for (String s : results[2].keySet()) {
			ResultModel seg = results[2].get(s);
//			if(HtmlParser.shouldFilter(seg.getSegmentName()))
//				continue;
			
			if (ResultDAO.calcPercentRel(seg) > threshold) {
				downloadListH2.add(seg.getSegmentName());
			}
		}
		
		
		//find out the source seg in H1 that link to rel seg in H2
		HashSet<String> downloadListH1 = new HashSet<>();
		ArrayList<String> removeList = new ArrayList<>();
		for (String s : downloadListH2) {
			if (!reverseGraph[1].containsKey(s)) {
				removeList.add(s);
				continue;
			}

			HashSet<String> source = reverseGraph[1].get(s);

			ResultModel bestSrc = null;
			
			// select only best path
			for (String src : source) {
				if (bestSrc == null)
					bestSrc = results[1].get(src);
				else {
					if (ResultDAO.calcPercentRel(bestSrc) < ResultDAO.calcPercentRel(results[1].get(src)))
						bestSrc = results[1].get(src);
				}

			}
			downloadListH1.add(bestSrc.getSegmentName());
		}

		System.out.println(removeList.size() + "\t" + downloadListH2.size());
		downloadListH2.removeAll(removeList);
		removeList.clear();
		

		// get all relevant seg from H1 and insert to downloadListH1 
		// HashSet<String> relH1 = new HashSet<>();
		for (String s : results[1].keySet()) {
			ResultModel seg = results[1].get(s);
			
//			if(HtmlParser.shouldFilter(seg.getSegmentName())){
//				removeList.add(seg.getSegmentName());
//				
//				if(fwGraph[1].get(seg.getSegmentName())!= null){
//					removeList.addAll(fwGraph[1].get(seg.getSegmentName()));
//				}
//				continue;
//			}
			
			
			if (ResultDAO.calcPercentRel(seg) > threshold) {
				downloadListH1.add(seg.getSegmentName());
			}
		}
		
		
		downloadListH2.removeAll(removeList);


		// crawling
		// download h0

		double rel = 0, non = 0;
		for (String s : results[0].keySet()) {
			ResultModel m = results[0].get(s);
			rel += m.getRelevantPage();
			non += m.getIrrelevantPage();
		}

		// download h1;
		for (String s : downloadListH1) {
			ResultModel m = results[1].get(s);
			if(m == null)
				System.out.println(s);
			rel += m.getRelevantPage();
			non += m.getIrrelevantPage();
		}

		// download h2;
		for (String s : downloadListH2) {
			ResultModel m = results[2].get(s);
			rel += m.getRelevantPage();
			non += m.getIrrelevantPage();
		}

		double recall = rel / totalRel;
		double precision = rel / (rel + non);

		logger.info(String.format("Recall:\t%.3f\tHV:\t%.3f\tF1:\t%.3f\n", recall, precision, 2 * (recall * precision) / (recall + precision)));

		/*
		 * ArrayList<String> output = new ArrayList<>();
		 * 
		 * int cThai=0; for (ResultModel m : rd.getAll()) {
		 * if(m.getRelevantPage() > 0){
		 * output.add(ResultDAO.calcPercentRel(m) + "\t" +
		 * m.getRelevantPage() + "\t" + m.getIrrelevantPage()); }
		 * 
		 * 
		 * results[0].put(m.getSegmentName().toLowerCase(), m);
		 * cThai+=m.getRelevantPage(); }
		 * 
		 * for (SegmentGraphModel m : sd.getAll()) { if(m.getSourceSeg() == null
		 * || m.getDestSeg() == null) continue;
		 * 
		 * try{ if (!graph[0].containsKey(m.getSourceSeg()))
		 * graph[0].put(m.getSourceSeg().toLowerCase(), new
		 * SegGraph(m.getSourceSeg().toLowerCase(),
		 * m.getDestSeg().toLowerCase())); else
		 * graph[0].get(m.getSourceSeg().toLowerCase()).getDestSegs().add(m.
		 * getDestSeg().toLowerCase()); }catch(Exception e){
		 * e.printStackTrace(); System.out.println(m.getSourceSeg()==null);
		 * System.out.println(m.getDestSeg()==null); } }
		 * 
		 * ResultDb.close();
		 * 
		 * ResultDb.createEnvironment(dbPath + "db-1"); rd =
		 * ResultDb.getResultDAO(); sd = ResultDb.getSegmentGraphDAO(); for
		 * (ResultModel m : rd.getAll()) {
		 * 
		 * if(m.getRelevantPage() > 0){
		 * output.add(ResultDAO.calcPercentRel(m) + "\t" +
		 * m.getRelevantPage() + "\t" + m.getIrrelevantPage()); }
		 * 
		 * 
		 * results[1].put(m.getSegmentName().toLowerCase(), m);
		 * cThai+=m.getRelevantPage(); } System.out.println("db1 " +
		 * rd.getAll().length);
		 * 
		 * // System.out.println("Contain>>>" + count + "\t" +
		 * results[1].size());
		 * 
		 * for (SegmentGraphModel m : sd.getAll()) { if(m.getSourceSeg() == null
		 * || m.getDestSeg() == null) continue;
		 * 
		 * if (!graph[1].containsKey(m.getSourceSeg()))
		 * graph[1].put(m.getSourceSeg().toLowerCase(), new
		 * SegGraph(m.getSourceSeg().toLowerCase(),
		 * m.getDestSeg().toLowerCase())); else
		 * graph[1].get(m.getSourceSeg().toLowerCase()).getDestSegs().add(m.
		 * getDestSeg().toLowerCase()); }
		 * 
		 * ResultDb.close();
		 * 
		 * ResultDb.createEnvironment(dbPath + "db-2"); rd =
		 * ResultDb.getResultDAO();
		 * 
		 * for (ResultModel m : rd.getAll()){ if(m.getRelevantPage() > 0){
		 * output.add(ResultDAO.calcPercentRel(m) + "\t" +
		 * m.getRelevantPage() + "\t" + m.getIrrelevantPage()); }
		 * 
		 * cThai+=m.getRelevantPage();
		 * results[2].put(m.getSegmentName().toLowerCase(), m); }
		 * System.out.println("db2 " + rd.getAll().length);
		 * 
		 * FileUtils.writeTextFile("degree.txt", output, false);
		 * ResultDb.close();
		 * 
		 * // สร้าง hashset กันดาวน์โหลดซ้ำ HashSet<String>[] segDb = new
		 * HashSet[3]; for (int i = 0; i < 3; i++) { segDb[i] = new HashSet<>();
		 * }
		 * 
		 * int thai = 0, non = 0;
		 * 
		 * TestFrontier frontier = new TestFrontier(); TestFrontier f2 = new
		 * TestFrontier();
		 * 
		 * //initial queue Set<String> hop0 = results[0].keySet(); for (String s
		 * : hop0) { frontier.enqueue(new TestFrontierModel(s, 0, 10)); }
		 * 
		 * ResultModel m = null; SegGraph sgm = null;
		 * 
		 * 
		 * while(frontier.size() > 0){
		 * 
		 * TestFrontierModel current = frontier.dequeue(); int currentDepth =
		 * current.getDepth(); String currSegName =
		 * current.getSegName().toLowerCase();
		 * 
		 * if(currentDepth > 2) continue;
		 * 
		 * 
		 * //โหลดมาแล้ว if(segDb[currentDepth].contains(currSegName)) continue;
		 * 
		 * //โหลด m = results[currentDepth].get(currSegName); if(m == null)
		 * continue;
		 * 
		 * thai += m.getRelevantPage(); non += m.getIrrelevantPage();
		 * 
		 * //add เข้า db segDb[currentDepth].add(currSegName);
		 * 
		 * 
		 * //แกะ link if(graph[currentDepth].containsKey(currSegName)){ SegGraph
		 * sg = graph[currentDepth].get(currSegName);
		 * 
		 * for(String s : sg.getDestSegs()){
		 * 
		 * if(results[currentDepth+1].containsKey(s.toLowerCase())){
		 * 
		 * double score =
		 * ResultDAO.calcPercentRel(results[currentDepth+1].get(s.
		 * toLowerCase())); if(score >= threshold) frontier.enqueue(new
		 * TestFrontierModel(s, currentDepth+1, score)); // else //
		 * f2.enqueue(new TestFrontierModel(s, currentDepth+1, score)); }
		 * 
		 * } }
		 * 
		 * }
		 * 
		 * // SegmentQueueModel m = new SegmentQueueModel(segment, score, depth)
		 * 
		 * System.out.println(1.0*thai/(thai+non) + "\t" + (1.0*thai/cThai) +
		 * "\t" +
		 * (2*(1.0*thai/(thai+non))*(1.0*thai/cThai))/((1.0*thai/(thai+non))+(1.
		 * 0*thai/cThai)) ); // thai= 0; // non = 0; // int r=0, ir=0; //
		 * while(f2.size() > 0){ // TestFrontierModel current = f2.dequeue(); //
		 * int currentDepth = current.getDepth(); // String currSegName =
		 * current.getSegName().toLowerCase(); // // if(currentDepth > 2) //
		 * continue; // // m = results[currentDepth].get(currSegName); // if(m
		 * == null) // continue; // // double score =
		 * FeaturesExtractionNew.calcPercentThai(m); // if(score >= threshold)
		 * // // // thai += m.getRelevantPage(); // non +=
		 * m.getIrrelevantPage(); // // } // System.out.println(thai + "\t" +
		 * non + "\t" + count);
		 */
		// bw.close();
	}

}
