package com.job.ic.proxy;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

import net.sf.javainetlocator.InetAddressLocator;

public class IPThread implements Runnable{
	private String inputHost;

	private static Logger logger = Logger.getLogger(IPThread.class);
	private CountDownLatch cd;
	public IPThread(String inputHost, CountDownLatch cd){
		this.inputHost = inputHost;
		this.cd = cd;
	}
	
	public void run(){
		try {
			String s = InetAddressLocator.getCountry(this.inputHost, true);
			logger.info(this.cd.getCount() + "\t" + this.inputHost + "\t" + s);
			Random r = new Random(1000);
			Thread.sleep(r.nextInt(1000));
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			cd.countDown();
		}
	}
}
