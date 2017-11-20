package com.job.ic.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.LoggerProvider;
import net.htmlparser.jericho.Source;

public class BackLinkUtils {

//	private static Logger logger = Logger.getLogger(BackLinkUtils.class);
	
	public static void main(String[] args) throws UnsupportedEncodingException{
		
		net.htmlparser.jericho.Config.LoggerProvider = LoggerProvider.DISABLED;

//		String tmp = "gconsole.com";
//		ArrayList<String> links = retrieve("link:" + tmp.trim());
//		String x;
//		for(String t: links){
//			if(!t.startsWith("/url?q="))
//				continue;
//			
//			if(t.contains("webcache.googleusercontent.com"))
//				continue;
//			
//			
//			String target = "/url?q=";
//			t = t.substring(t.indexOf(target) + target.length());
//			target = "&";
//			t = t.substring(0, t.indexOf(target));
//			t = URLDecoder.decode(t, "UTF-8");
//			
//			System.out.println(t);
//		}
//		
		
//		ArrayList<WebsiteSegment> tmp = FileUtils.readSegmentFile("back-tourism-s.txt");
//		for(WebsiteSegment s: tmp){
//			FileUtils.writeTextFile("back-t.txt", s.getUrls(), true);
//		}
		getBackLinksFromFile("h.txt", "back-gaming-new.txt");
//		for(String s: getBackLinks("http://www.crystalclearthailand.com/")){
//			System.out.println(s);
//		}
		
	}
	
	public static void getBackLinksFromFile(String source, String output) {
		// TODO Auto-generated method stub
		try(
				BufferedWriter bw = new BufferedWriter(new FileWriter(output));
		){
			ArrayList<String> file = new ArrayList<String>(Arrays.asList(FileUtils.readFile(source)));
			
			
			int total = file.size();
			int count = 0;
			for(String tmp: file){
				tmp = tmp.trim();
				if(tmp.equals("=="))
					continue;
							
				
				ArrayList<String> urls = getBackLinks(tmp);
				if(urls.size() > 0){
					for(String url: urls){
						bw.write(tmp + "\t" + url + "\n");
					}
				}
				
				System.out.printf("%d/%d\t%s\tsize:%d", count++, total, tmp, urls.size());
//				logger.info(String.format("%d/%d\t%s\tsize:%d", count++, total, tmp, urls.size()));

				//sleep for safety  หยุดรอไม่เกิน 3 นาที
				Thread.sleep(1*1000*35);
			
			}
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static ArrayList<String> getBackLinks(String url){
		ArrayList<String> links = retrieve("link:" + url.trim());
		ArrayList<String> output = new ArrayList<String>();
		for(String t : links){
			if(!t.startsWith("/url?q="))
				continue;
			
			if(t.contains("webcache.googleusercontent.com"))
				continue;
			
			if(t.contains("/settings/ads/preferences"))
				continue;
			
//			System.out.println(t);
			
			String target = "/url?q=";
			t = t.substring(t.indexOf(target) + target.length());
			
			target = "&";
			t = t.substring(0, t.indexOf(target));
//			System.out.println(t);
			try {
				t = URLDecoder.decode(t, "UTF-8");
			} catch (UnsupportedEncodingException e) {
			}
			
			
			output.add(t);
//			System.err.println(t);
//			System.out.println(t);
			
			
		}
		
		return output;
	}
	
	public static ArrayList<String> retrieve(String entry) {
		ArrayList<String> links = new ArrayList<String>();
		try {
			// string new entry is making the keywords into query that can be known by google
			String newEntry = (java.net.URLEncoder.encode(entry, "UTF-8").replace("+", "%20"));

			// inputing the keywords to google search engine
			URL url = new URL("http://www.google.co.th/search?hl=th&site=&q=" + newEntry);
			// makking connection to the internet
			URLConnection urlConn = url.openConnection();
			urlConn.setUseCaches(false);
			urlConn.setRequestProperty("User-Agent", "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 ( .NET CLR 3.5.30729)");

			// getting the input stream of page html into bufferedreader
			BufferedReader buffReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			String line;
			StringBuffer buffer = new StringBuffer();

			// getting the input stream of html into stringbuffer
			while ((line = buffReader.readLine()) != null) {
				buffer.append(line + "\n");
				
			}
			
			FileUtils.writeTextFile("test.html", buffer, false);
			
			
//			FileUtils.writeTextFile("d:/testtt.txt", buffer.toString(), false);
			Source sc = new Source(buffer.toString());
			List<Element> s = sc.getAllElements(HTMLElementName.A);
			links = addAnchorLink(s);
			
			
			//System.out.println(buffer.toString());
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		return links;
	}

	
	
	private static ArrayList<String> addAnchorLink(List<Element> s) {
		try {
			ArrayList<String> l = new ArrayList<String>();
			String tmp;
			for (int i = 0; i < s.size(); i++) {
				tmp = s.get(i).toString();
				if (tmp.equals("") || tmp == null) {
					continue;
				}

				if (tmp.indexOf("href=") >= 0) {
					tmp = tmp.substring(tmp.indexOf("href="), tmp.indexOf(">")).replace("href=", "");
				}

				if (tmp.indexOf("HREF=") >= 0) {
					tmp = tmp.substring(tmp.indexOf("HREF="), tmp.indexOf(">")).replace("HREF=", "");
				}

				if (tmp.charAt(0) == '\"') {
					tmp = tmp.substring(1);
					tmp = tmp.substring(0, tmp.indexOf("\""));
				} else if (tmp.charAt(0) == '\'') {
					tmp = tmp.substring(1);
					tmp = tmp.substring(0, tmp.indexOf("\'"));
				}

				tmp = tmp.replace("\"", "").replace("'", "");
				l.add(tmp);
			}

			return l;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
