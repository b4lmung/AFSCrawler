/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.PriorityQueue;


public class Queue implements Serializable {

	/**
     * 
     */
	private static final long serialVersionUID = 3398838435657294526L;
	private PriorityQueue<QueueObj> ll;
	private HashMap<String, QueueObj> db;

	public Queue() {
		ll = new PriorityQueue<>(1, (o1, o2) -> {

//			if (isBFS) {
//				if (o1.getDepth() <= 0 && o2.getDepth() <= 0)
//					return -1;
//				else if (o1.getDepth() <= 0 && o2.getDepth() > 0)
//					return -1;
//				else if (o1.getDepth() > 0 && o2.getDepth() <= 0)
//					return 1;
//			}

			// int p1 = (int) (o1.getScore() * 100);
			// int p2 = (int) (o2.getScore() * 100);

			return -1 * Double.compare(o1.getScore(), o2.getScore());
			// return -1 * Integer.compare(p1, p2);

		});
		
		db = new HashMap<>();
	}

//	public ArrayList<QueueObj> getQueue() {
//		return ll;
//	}

	public synchronized void enQueue(QueueObj q) {
		
		if(!db.containsKey(q.getUrl())){
			db.put(q.getUrl(), q);
			ll.add(q);
		}else{
			QueueObj qo = db.get(q.getUrl());
			ll.remove(qo);
			qo.addSrcScore(q.getSrcScores());
			qo.updatePredictions(null, q.getHistoryPrediction());
			ll.add(qo);
		}
	}

	public synchronized QueueObj deQueue() {
		if (ll.size() > 0) {
			QueueObj qo = ll.poll();
			db.remove(qo);
			return qo;
		} else {
			return null;
		}
		
	}

	public int size() {
		return ll.size();
	}

	
	public void clear(){
		ll.clear();
	}
}
