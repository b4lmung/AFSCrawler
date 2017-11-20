package com.job.ic.crawlers.models;

import java.util.Comparator;
import java.util.PriorityQueue;

public class TestFrontier {
	private PriorityQueue<TestFrontierModel> queue;
	
	public TestFrontier(){
		queue = new PriorityQueue<>(1, new Comparator<TestFrontierModel>() {

			

			@Override
			public int compare(TestFrontierModel o1, TestFrontierModel o2) {
				
				
				return -1*Double.compare(o1.getScore(), o2.getScore());
				
			}
		});
	}
	
	public void enqueue(TestFrontierModel input){
		
		queue.add(input);
	}
	
	public TestFrontierModel dequeue(){
		
		return queue.poll();
	}
	
	public long size(){
		return queue.size();
	}
	
	public static void main(String[] args){
		TestFrontier s = new TestFrontier();
		s.enqueue(new TestFrontierModel("a", 0, 10.0));
		s.enqueue(new TestFrontierModel("a", 0, 3.0));
		s.enqueue(new TestFrontierModel("a", 0, 11.0));
		
		
		System.out.println(s.dequeue());
		System.out.println(s.dequeue());
		System.out.println(s.dequeue());
	}

}
