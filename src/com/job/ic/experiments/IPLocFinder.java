package com.job.ic.experiments;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import net.sf.javainetlocator.InetAddressLocator;
import net.sf.javainetlocator.InetAddressLocatorException;

public class IPLocFinder extends Thread{

	public static Hashtable<String, Integer> ip = new Hashtable<String, Integer>();
	public static Hashtable<String, Integer> domain = new Hashtable<String, Integer>();
	
	public static int count = 0;
	private String host;
	
	
	public IPLocFinder(String host){
		this.host = host;
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		ExecutorService exe = Executors.newFixedThreadPool(10);
		String[] list = FileUtils.readFile("all.txt");
		String host, country, d;
		int tmp;
		HashSet<String> set = new HashSet<>();
		for(String s: list){
			host = HttpUtils.getHost(s);
			if(set.contains(host))
				continue;
			
			set.add(host);
			d = HttpUtils.getDomain(s);
			if(domain.containsKey(d)){
				tmp = domain.get(d)+1;
				domain.put(d, tmp);
			}else{
				tmp = 1;
				domain.put(d, tmp);
			}
			
		
			exe.execute(new IPLocFinder(host));
			
		}
		
		exe.shutdown();
		
		while(!exe.isTerminated()){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		System.out.println("====");
		BufferedWriter bw = FileUtils.getBufferedFileWriter("ip.txt");
		Enumeration<String> e = ip.keys();
		while(e.hasMoreElements()){
			country = e.nextElement();
			bw.write((country + "\t" + ip.get(country)) + "\n");
		}
		bw.close();
		
//		System.out.println("====");
//		bw = FileUtils.getBufferedFileWriter("domain.txt");
//		e = domain.keys();
//		while(e.hasMoreElements()){
//			d = e.nextElement();
//			bw.write((d + "\t" + domain.get(d)) + "\n");
//		}
//		bw.close();
		
	}

	public void run(){
		try {
			String tmp = InetAddressLocator.getLocale(host).getCountry().toUpperCase();
			if(ip.containsKey(tmp)){
				ip.put(tmp, ip.get(tmp)+1);
			}else{
				ip.put(tmp, 1);
			}
		} catch (InetAddressLocatorException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
		count++;
		if(count%100==0)
			System.out.println(count);
	}
}
