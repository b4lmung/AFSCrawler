package com.job.ic.proxy.model;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Test {
	public static void main(String[] args) throws Exception{
		System.setProperty("http.proxyHost", "localhost");
		System.setProperty("http.proxyPort", "5555");
		
		URL u = new URL("http://1000fights.com/how-to-beat-the-post-trip-blues/");
		HttpURLConnection uc = (HttpURLConnection) u.openConnection();
		uc.connect();
		
		InputStream is = uc.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String tmp;
		
		while((tmp = br.readLine()) != null){
			System.out.println(tmp);
		}
		
		is.close();
		
	}
	
	public static void crawl() throws Exception{
		
		
	}
	
}
