package com.job.ic.experiments;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.job.ic.crawlers.daos.ResultDAO;
import com.job.ic.crawlers.daos.ResultDb;
import com.job.ic.crawlers.daos.SegmentGraphDAO;
import com.job.ic.crawlers.models.ResultModel;
import com.job.ic.crawlers.models.SegmentGraphModel;
import com.job.ic.crawlers.models.TestFrontier;
import com.job.ic.crawlers.models.TestFrontierModel;

public class DistanceThreshold {

	public static void main(String[] args){
		try {
			optimize(0.25);
			System.out.println("----");
			optimize(0.5);
			System.out.println("----");
			optimize(0.75);
			System.out.println("----");
			optimize(1.0);
			System.out.println("----");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void optimize(double threshold) throws IOException {
//		double threshold = 1.0;
		String dbPath = "db-test-estate/";

		HashMap<String, ResultModel>[] results = new HashMap[3];
		HashMap<String, SegGraph>[] graph = new HashMap[3];
		for (int i = 0; i < 3; i++) {
			graph[i] = new HashMap<>();
			results[i] = new HashMap<>();
		}

		ResultDb.createEnvironment(dbPath + "db-0");
		ResultDAO rd = ResultDb.getResultDAO();
		SegmentGraphDAO sd = ResultDb.getSegmentGraphDAO();
		
		System.out.println("db0 " + rd.getAll().length);

		int cThai=0;
		for (ResultModel m : rd.getAll()) {
			results[0].put(m.getSegmentName().toLowerCase(), m);
			cThai+=m.getRelevantPage();
		}

		for (SegmentGraphModel m : sd.getAll()) {
			if(m.getSourceSeg() == null || m.getDestSeg() == null)
				continue;
			
			try{
				if (!graph[0].containsKey(m.getSourceSeg()))
					graph[0].put(m.getSourceSeg().toLowerCase(), new SegGraph(m.getSourceSeg().toLowerCase(), m.getDestSeg().toLowerCase()));
				else
					graph[0].get(m.getSourceSeg().toLowerCase()).getDestSegs().add(m.getDestSeg().toLowerCase());
			}catch(Exception e){
				e.printStackTrace();
				System.out.println(m.getSourceSeg()==null);
				System.out.println(m.getDestSeg()==null);
			}
		}

		ResultDb.close();

		ResultDb.createEnvironment(dbPath + "db-1");
		rd = ResultDb.getResultDAO();
		sd = ResultDb.getSegmentGraphDAO();
		for (ResultModel m : rd.getAll()) {
			results[1].put(m.getSegmentName().toLowerCase(), m);
			cThai+=m.getRelevantPage();
		}
		System.out.println("db1 " + rd.getAll().length);

		// System.out.println("Contain>>>" + count + "\t" + results[1].size());

		for (SegmentGraphModel m : sd.getAll()) {
			if(m.getSourceSeg() == null || m.getDestSeg() == null)
				continue;
			
			if (!graph[1].containsKey(m.getSourceSeg()))
				graph[1].put(m.getSourceSeg().toLowerCase(), new SegGraph(m.getSourceSeg().toLowerCase(), m.getDestSeg().toLowerCase()));
			else
				graph[1].get(m.getSourceSeg().toLowerCase()).getDestSegs().add(m.getDestSeg().toLowerCase());
		}

		ResultDb.close();

		ResultDb.createEnvironment(dbPath + "db-2");
		rd = ResultDb.getResultDAO();
		
		for (ResultModel m : rd.getAll()){
			cThai+=m.getRelevantPage();
			results[2].put(m.getSegmentName().toLowerCase(), m);
		}
		System.out.println("db2 " + rd.getAll().length);

		ResultDb.close();

		// สร้าง hashset กันดาวน์โหลดซ้ำ
		HashSet<String>[] segDb = new HashSet[3];
		for (int i = 0; i < 3; i++) {
			segDb[i] = new HashSet<>();
		}

		int thai = 0, non = 0;

		TestFrontier frontier = new TestFrontier();
		TestFrontier f2 = new TestFrontier();
		
		//initial queue
		Set<String> hop0 = results[0].keySet();
		for (String s : hop0) {
			frontier.enqueue(new TestFrontierModel(s, 0, 10));
		}

		ResultModel m = null;
		SegGraph sgm = null;
		
		
		while(frontier.size() > 0){
			
			TestFrontierModel current = frontier.dequeue();
			int currentDepth = current.getDepth();
			String currSegName = current.getSegName().toLowerCase();
			
			if(currentDepth > 2)
				continue;
			
			
			//โหลดมาแล้ว
			if(segDb[currentDepth].contains(currSegName))
				continue;
			
			//โหลด
			m = results[currentDepth].get(currSegName);
			if(m == null)
				continue;
			
			thai += m.getRelevantPage();
			non += m.getIrrelevantPage();
			
			//add เข้า db
			segDb[currentDepth].add(currSegName);
			
			
			//แกะ link
			if(graph[currentDepth].containsKey(currSegName)){
				SegGraph sg = graph[currentDepth].get(currSegName);
				
				for(String s : sg.getDestSegs()){

					if(results[currentDepth+1].containsKey(s.toLowerCase())){
						
						double score = ResultDAO.calcPercentRel(results[currentDepth+1].get(s.toLowerCase()));
						if(score >= threshold)
							frontier.enqueue(new TestFrontierModel(s, currentDepth+1, score));
//						else
//							f2.enqueue(new TestFrontierModel(s, currentDepth+1, score));
					}
					
				}
			}
			
		}
		
		// SegmentQueueModel m = new SegmentQueueModel(segment, score, depth)

		System.out.println(1.0*thai/(thai+non) + "\t" + (1.0*thai/cThai) + "\t" + (2*(1.0*thai/(thai+non))*(1.0*thai/cThai))/((1.0*thai/(thai+non))+(1.0*thai/cThai)) );
//		thai= 0;
//		non = 0;
//		int r=0, ir=0;
//		while(f2.size() > 0){
//			TestFrontierModel current = f2.dequeue();
//			int currentDepth = current.getDepth();
//			String currSegName = current.getSegName().toLowerCase();
//			
//			if(currentDepth > 2)
//				continue;
//			
//			m = results[currentDepth].get(currSegName);
//			if(m == null)
//				continue;
//			
//			double score = FeaturesExtractionNew.calcPercentThai(m);
//			if(score >= threshold)
//			
//			
//			thai += m.getRelevantPage();
//			non += m.getIrrelevantPage();
//			
//		}
//		System.out.println(thai + "\t" + non + "\t" + count);
		
		// bw.close();
	}

}
