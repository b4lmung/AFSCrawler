package com.job.ic.ml.classifiers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class PageClassifierTask implements Runnable {


	private CountDownLatch cd;
	private LinkedBlockingQueue<WekaClassifier> pages;
	private String features;
	private LinkedBlockingQueue<PageClassifierTaskResult> results;
	
	public PageClassifierTask(LinkedBlockingQueue<WekaClassifier> pool, CountDownLatch cd, String features, LinkedBlockingQueue<PageClassifierTaskResult> results) {
		// TODO Auto-generated constructor stub
		this.pages = pool;
		this.cd = cd;
		this.features = features;
		this.results = results;
	}
	
	@Override
	public void run() {
		WekaClassifier cf = null;
		try{
			cf = this.pages.poll();
			String[] tmp = this.features.split(",");
			ClassifierOutput output = cf.predict(tmp[0], tmp);
			if (tmp[tmp.length - 1].equals("non")) 
				this.results.put(new PageClassifierTaskResult(tmp, output, ResultClass.IRRELEVANT));
			else
				this.results.put(new PageClassifierTaskResult(tmp, output, ResultClass.RELEVANT));
				
			
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			if(cd != null)
				try {
					this.pages.put(cf);
					
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			this.cd.countDown();
		}
	}

}
