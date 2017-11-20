/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.job.ic.ml.classifiers.ClassifierOutput;
import com.job.ic.ml.classifiers.HistoryPredictor;
import com.job.ic.ml.classifiers.NeighborhoodPredictor;
import com.job.ic.utils.FileUtils;
import com.sleepycat.je.cleaner.OffsetList.Segment;

public class SegmentFrontier implements Serializable {
	private static Logger logger = Logger.getLogger(SegmentFrontier.class);

	private static final long serialVersionUID = 3398838435657294526L;
//	private LinkedList<SegmentQueueModel> ll;
	
	private PriorityBlockingQueue<SegmentQueueModel> ll;
	private ConcurrentHashMap<String, SegmentQueueModel> elements;

	public SegmentFrontier() {
		initialize();
	}

	public void initialize() {
//		this.ll = new LinkedList<>();
		if (CrawlerConfig.getConfig().isTrainingMode()) {
			this.ll = new  PriorityBlockingQueue<>(100, (o1, o2) -> {
				return -1*Double.compare(o1.getScore(), o2.getScore());
			});
		} else if (CrawlerConfig.getConfig().getPredictorTrainingPath() == null || CrawlerConfig.getConfig().getPredictorTrainingPath().equals("")) {
			this.ll = new  PriorityBlockingQueue<>(100, (o1, o2) -> {
				return -1 * Double.compare(o1.getAvgRelScore(), o2.getAvgRelScore());
			});
		} else {
			this.ll = new  PriorityBlockingQueue<>(100, (o1, o2) -> {

//				boolean isO1Rel = o1.getPredictionScore() > 0.5;
//				boolean isO2Rel = o2.getPredictionScore() > 0.5;

//				boolean isO1Tun = (o1.getAvgRelDegree() <= CrawlerConfig.getConfig().getRelevanceDegreeThreshold());
//				boolean isO2Tun = (o2.getAvgRelDegree() <= CrawlerConfig.getConfig().getR  elevanceDegreeThreshold());

				int p1 = (int) (o1.getScore() * 100);
				int p2 = (int) (o2.getScore() * 100);

				int c = Integer.compare(p1, p2);
				
//				if(!isO1Rel && !isO2Rel && isO1Tun && isO2Tun)
//					return -1*Integer.compare((int)o1.getPredictionScore()*100, (int)o2.getPredictionScore()*100);

//				if(true)
//					return -1*Double.compare(o1.getPredictionScore(), o2.getPredictionScore());

				if (p1 != p2) {
					return -1 * c;
				}
				
				return -1*Double.compare(o1.getPredictionScore(), o2.getPredictionScore());

//				return -1*Double.compare(o1.getScore(), o2.getScore());

//				if(isO1Tun && isO2Tun){
//					
//					//sort ตามมค่าคะแนน ถ้า relevant ทั้งคู่
//					if(isO1Rel && isO2Rel){
//						if(c == 0)
//							return -1*Double.compare(o1.getPredictionScore(), o2.getPredictionScore());
//						else
//							return -1*c;
//					} // กรณีอื่น sort ตามค่า prob
//					else if(!isO1Rel && isO2Rel){
//						return 1;
//					}else if(isO1Rel && !isO2Rel){
//						return -1;
//					}else{	
//						return -1*Double.compare(o1.getPredictionScore(), o2.getPredictionScore());						
//					}
//				}
//				else if(!isO1Tun && isO2Tun){
//					return -1;
//				}else if(isO1Tun && !isO2Tun){
//					return 1;
//				}else{  // กรณีที่อื่น sort ตามคะแนนทั้งหมด
//					if(c == 0)
//						return -1*Double.compare(o1.getPredictionScore(), o2.getPredictionScore());						
//					else
//						return -1*c;
//				}
				
			});
		}
		elements = new ConcurrentHashMap<>();
	}
	
//	private void sort(){
//		if (CrawlerConfig.getConfig().isTrainingMode()) {
//			ll.sort((o1, o2) -> {
//				return Double.compare(o1.getDepth(), o2.getDepth());
//			});
//		} else if (CrawlerConfig.getConfig().getPredictorTrainingPath() == null || CrawlerConfig.getConfig().getPredictorTrainingPath().equals("")) {
//			ll.sort((o1, o2) -> {
//				return -1 * Double.compare(o1.getAvgRelScore(), o2.getAvgRelScore());
//			});
//		} else {
//			ll.sort((o1, o2) -> {
//
//				boolean isO1Rel = o1.getPredictionScore() > 0.5;
//				boolean isO2Rel = o2.getPredictionScore() > 0.5;
////
//				boolean isO1Tun = (o1.getAvgRelDegree() <= CrawlerConfig.getConfig().getRelevanceDegreeThreshold());
//				boolean isO2Tun = (o2.getAvgRelDegree() <= CrawlerConfig.getConfig().getRelevanceDegreeThreshold());
//
//				int p1 = (int) (o1.getScore() * 100);
//				int p2 = (int) (o2.getScore() * 100);
//
//				int c = Integer.compare(p1, p2);
//
//				if(isO1Tun && isO2Tun){
//					if(isO1Rel && isO2Rel){
//						if(c == 0)
//							return -1*Double.compare(o1.getPredictionScore(), o2.getPredictionScore());
//						else
//							return -1*c;
//					}else if(!isO1Rel && isO2Rel){
//						return 1;
//					}else if(isO1Rel && !isO2Rel){
//						return -1;
//					}else{
//						return -1*Double.compare(o1.getPredictionScore(), o2.getPredictionScore());						
//					}
//				}else if(!isO1Tun && isO2Tun){
//					return -1;
//				}else if(isO1Tun && !isO2Tun){
//					return 1;
//				}else{
//					if(c == 0)
//						return -1*Double.compare(o1.getPredictionScore(), o2.getPredictionScore());
//					else
//						return -1*c;
//				}
//				
//			});
//		}
//	}

	public synchronized void enQueue(ArrayList<WebsiteSegment> segments) {
		for (WebsiteSegment dest : segments) {
			enQueue(dest);
		}
		
//		sort();
	}

	public void enqueueSeeds(String filePath) {
		
		ArrayList<WebsiteSegment> segs = FileUtils.readSegmentFile(CrawlerConfig.getConfig().getSeedPath());

		int i = 0;
		for (WebsiteSegment q : segs) {
			q.setPrediction(new ClassifierOutput(100, 0, 1), null, null);
			SegmentQueueModel newEntry = new SegmentQueueModel(q.getSegmentName(), q.getDepth(), q, null, null);
			newEntry.setScore((segs.size() - i) * 1000);
			this.ll.add(newEntry);
			elements.put(newEntry.getSegmentName(), newEntry);

			i++;
		}
		
//		sort();
	}

	public synchronized void enQueue(WebsiteSegment q) {
//		String seg = HttpUtils.getBasePath(q.getUrls().get(0));
		
		ClassifierOutput neighborhood = NeighborhoodPredictor.predict(q.getSegmentName());
		ClassifierOutput history = HistoryPredictor.predict(q.getSegmentName());
		
		if (contain(q.getSegmentName())) {
			SegmentQueueModel tmp = elements.get(q.getSegmentName());
			remove(tmp);
			tmp.addSegmentData(q, neighborhood, history);
			this.ll.add(tmp);
			
		} else {
			SegmentQueueModel newEntry = new SegmentQueueModel(q.getSegmentName(), q.getDepth(), q, neighborhood, history);
			this.ll.add(newEntry);
			elements.put(newEntry.getSegmentName(), newEntry);
		}
		
		
//		sort();

	}
	
	private synchronized void remove(SegmentQueueModel input){
		
		Iterator<SegmentQueueModel> it = this.ll.iterator();
		while(it.hasNext()){
			SegmentQueueModel tmp = it.next();
			if(tmp.getSegmentName().equals(input.getSegmentName())){
				it.remove();
			}
		}
		elements.remove(input.getSegmentName());
	}
	
	public synchronized void print(){
		System.out.println("-----------------");
		for(SegmentQueueModel s : this.ll){
			System.out.println(">>>>>\t" + s.getSegmentName() + "\t" + s.getScore() + "\t" + s.getPredictionScore() + "\t" + s);
		}
	}

	public synchronized SegmentQueueModel deQueue() {
		if(this.ll.size() == 0)
			return null;
	
//		SegmentQueueModel tmp = this.ll.remove(0);
		SegmentQueueModel tmp;
		try {
			tmp = this.ll.poll(3, TimeUnit.MINUTES);
			this.elements.remove(tmp.getSegmentName());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		return tmp;
	}

	public synchronized boolean contain(String segment) {

		return elements.containsKey(segment);
	}

	public int size() {
		return ll.size();
	}

	public void clear() {
		ll.clear();
	}
	
	
	public static void main(String[] args){
		SegmentFrontier f = new SegmentFrontier();
		f.enqueueSeeds("test-diving-s.txt");
		
		ArrayList<String> urls = new ArrayList<>();
		urls.add("index.html");
		
		ArrayList<Double> score = new ArrayList<>();
		score.add(0.9);
		
//		WebsiteSegment s1 = new WebsiteSegment("http://www.sunrise-divers.com/", "world", 0, urls, 0, "other", "other", "other", "other", "", "", score, true);
//		s1.setPredictionScore(0.98);
		
//		WebsiteSegment s2 = new WebsiteSegment("http://thailand.greatestdivesites.com/divesites/", "world", 0, urls, 0, "other", "other", "other", "other", "", "", score, true);
		
		System.out.println("after enqueue seed");
		
		System.out.println("after dequeue");
		
		SegmentQueueModel tmp = f.deQueue();
		System.out.println(tmp.getSegmentName() + "\t" + tmp.getScore());
		
		tmp = f.deQueue();
		System.out.println(tmp.getSegmentName() + "\t" + tmp.getScore());
		tmp = f.deQueue();
		System.out.println(tmp.getSegmentName() + "\t" + tmp.getScore());
		tmp = f.deQueue();
		System.out.println(tmp.getSegmentName() + "\t" + tmp.getScore());
		tmp = f.deQueue();
		System.out.println(tmp.getSegmentName() + "\t" + tmp.getScore());
		tmp = f.deQueue();
		System.out.println(tmp.getSegmentName() + "\t" + tmp.getScore());
		tmp = f.deQueue();
		System.out.println(tmp.getSegmentName() + "\t" + tmp.getScore());
		tmp = f.deQueue();
		System.out.println(tmp.getSegmentName() + "\t" + tmp.getScore());
		tmp = f.deQueue();
		System.out.println(tmp.getSegmentName() + "\t" + tmp.getScore());
		
				
		

		System.exit(0);
		System.out.println("after dequeue");
		
		
//		f.enQueue(s1);

		System.out.println("after enqueue");
		f.print();
		
		System.out.println("----------------");
		
//		f.enQueue(s2);
//		f.print();
//		
		
		
	}
}
