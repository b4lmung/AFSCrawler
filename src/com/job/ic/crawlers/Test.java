package com.job.ic.crawlers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;


import org.apache.http.protocol.HTTP;

import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.experiments.ParseLog;
import com.job.ic.extraction.FeaturesExtraction;
import com.job.ic.ml.classifiers.NeighborhoodPredictor;
import com.job.ic.ml.classifiers.PageClassifier;
import com.job.ic.ml.classifiers.PredictorPool;
import com.job.ic.ml.classifiers.PredictorPoolMulti;
import com.job.ic.proxy.ProxyService;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.SegmentExtractor;
import com.job.ic.utils.StringUtils;

public class Test {

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		Map<K, V> result = new LinkedHashMap<>();
		Stream<Entry<K, V>> st = map.entrySet().stream();

		st.sorted(Comparator.comparing(e -> e.getValue())).forEachOrdered(e -> result.put(e.getKey(), e.getValue()));

		return result;
	}

	public static void downloadSet(String stockName) {
		 try {
	            URL url = new URL("http://www.settrade.com/servlet/IntradayStockChartDataServlet?symbol=" + stockName);
	            URLConnection connection = url.openConnection();
//	            connection.setRequestProperty("Host", "www.settrade.com");
	            connection.setRequestProperty("Referer", "http://www.settrade.com/C13_FastQuoteChart.jsp?stockSymbol=" + stockName + "&symbolType=s");

	            InputStream stream = connection.getInputStream();
	            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
	            String line;
	            while((line = br.readLine()) != null) {
	            	System.out.println(line);
	            }
	            br.close();
	        }
	        catch (Exception e) {
	            e.printStackTrace();
	        }
		
	}
	
	public static void main(String[] args) throws IOException {
		
		System.out.println("hello");
		UrlDb.createEnvironment("urlDb");
		
		for(String s: FileUtils.readFile("site.txt")) {
			System.out.println(s + "\t" + UrlDb.getUrlDAO().checkAndAddUrl(s, false));
		}
		UrlDb.close();
		
		System.exit(0);;
		
		downloadSet("PTT");
		
		
		ArrayList<String> op = new ArrayList<>();
		
		boolean isData = false;
		for(String s: FileUtils.readFile("history.arff")){
			if(s.contains("@data")){
				isData = true;
				continue;
			}
			
			if(!isData)
				continue;
			
			if(!s.contains("["))
				continue;
			
			s = s.replace("[","").replace("]","");
			String[] features = s.split(",");
			
			String d = "'" +  StringUtils.cleanUrlDataForPrediction(features[0]) + "'";
			int count = 0;
			System.out.println(s);
			for(int i = 1; i <= features.length-1; i++){
				
//			for(int i = features.length-22; i <= features.length-1; i++){
				d += "," + features[i];
				count++;
			}
//			System.out.println(count);
			op.add(d);
			
		}
		
		FileUtils.writeTextFile("history_new.arff", op, false);
		System.exit(0);
//		FileUtils.cleanArffData("logs-proxy-gaming/all.arff", FeaturesExtraction.getHeader());
//		ParseLog.parsePredictorPoolLog("logs");

		ArrayList<Integer> test = new ArrayList<>();
		test.add(20);
		test.add(1);
		test.add(5);
		
		System.out.println(test);
		System.exit(0);
		UrlDb.createEnvironment("urlDb");
		for(String s: FileUtils.readFile("test-baseball-s.txt")){

			System.out.println(s + "\t" + UrlDb.getUrlDAO().checkAndAddUrl(s, false));
		}

		
		System.exit(0);
//		Collections.shuffle(rand);
//		ArrayList<String> test = new ArrayList<>();
//		for(int i=0; i<rand.size()/2; i++){
//			test.add(rand.remove(0));
//		}
		
//		FileUtils.writeTextFile("train.txt", rand, false);
//		FileUtils.writeTextFile("test.txt", test, false);
		
		
		SegmentExtractor.extractSegment("train.txt", "back-gaming-s-new.txt");
		System.exit(0);
		int r=0, n = 0;
		HashSet<String> isTourism = new HashSet<>();
		for(String s: FileUtils.readFile("logs/ic.log")){
			
			if(!s.contains("PageIndexer:103-"))
				continue;
			
			
			s = s.substring(s.indexOf("PageIndexer:103-") + "PageIndexer:103-".length());
			
			s = s.trim();
			String[] tmpm = s.split("\t");
			if(Double.parseDouble(tmpm[1]) > 0.5){
				r++;
				isTourism.add(tmpm[0]);
			}
			else{
				n++;
				isTourism.add(tmpm[0]);
			}
			
		}
		
		HashSet<String> isDiving = new HashSet<>();
		String previous = "";
		boolean containRel = false;
		ArrayList<ArrayList<String>> all = new ArrayList<>();
		ArrayList<String> seg = new ArrayList<>();
		for(String s: FileUtils.readFile("logs/logs-normal-diving.log")){
			if(!s.contains("HttpSegmentCrawler:285- DOWNLOADED"))
				continue;
			
			
			s = s.substring(s.indexOf("HttpSegmentCrawler:285- DOWNLOADED") + "HttpSegmentCrawler:285- DOWNLOADED".length()).trim();
				
			String[] data = s.split("\t");
			
			String url = data[3];
			String segment = HttpUtils.getBasePath(url);
			
			
			double score = Double.parseDouble(data[0]);
			
			if(score > 0.5)
				isDiving.add(url);
			
			System.out.println(segment);
			if(previous.equals(segment)){
				if(score > 0.5){
					containRel = true;
				}
				seg.add(url);
			}else{
			
				if(containRel == true){
					all.add(seg);
					containRel = false;
				}else{
					seg.clear();
				}

				if(score > 0.5)
					containRel = true;
				previous = segment;
				seg = null;
				seg = new ArrayList<>();
				seg.add(url);
			}
			
		}
		
		int ndt = 0, no=0;
		for(ArrayList<String> segment: all){
			for(String s: segment){
				if(!isDiving.contains(s) && isTourism.contains(s))
					ndt++;
				else if(!isDiving.contains(s) && !isTourism.contains(s))
					no++;
			}
		}
		
		System.out.println(ndt + "\t" + no);
		System.out.println(r + "\t" + n);
		
		
		System.exit(0);
		HashSet<String> sameHost = new HashSet<>();
		
		
		HashMap<String, HashSet<String>> site = new HashMap<>();
		for(String s: FileUtils.readFile("back-baseball-all.txt")){
			String[] tmp = s.split("\t");
			String url = tmp[1];
			
			
			if(url.equals(tmp[0]))
				continue;
			
			if(HtmlParser.shouldFilter(url))
				continue;
			
			if(url.contains("wikipedia"))
				continue;
			
			if(url.contains("clip"))
				continue;
			
			if(url.contains("archieve"))
				continue;
			

			if(url.contains("dailymail"))
				continue;
			if(url.contains("linkedin"))
				continue;
			
			
			String host = HttpUtils.getHost(tmp[0]);
			String hostback = HttpUtils.getHost(tmp[1]);
			
			if(hostback.equals(host)){
				sameHost.add(url);
			}
			
			if(!site.containsKey(host))
				site.put(host, new HashSet());
			
			site.get(host).add(url);
		
		}
		
		HashSet<String> fout = new HashSet<>();
		HashMap<String, Integer> dfg = new HashMap<>();
		
		HashSet<String> hostDb = new HashSet<>();
		for(String s: sameHost){
			String host = HttpUtils.getHost(s);
			//if(hostDb.contains(host))
			//	continue;
			
			if(dfg.containsKey(host)){
				if(dfg.get(host) >= 10)
					break;
				
				dfg.put(host,dfg.get(host)+1);
			}else{
				dfg.put(host, 1);

				fout.add(s);
			}
			
			hostDb.add(host);
		}
		
		System.out.println(fout.size());
		int limit = 80-fout.size();
		System.out.println("limit " + limit);
		for(String s: site.keySet()){
			if(hostDb.contains(s))
				continue;
			
			for(String k: site.get(s)){
				String host = HttpUtils.getHost(k);
				
				if(dfg.containsKey(host)){
					if(dfg.get(host) >= 1)
						break;
					
					dfg.put(host,dfg.get(host)+1);
				}else{
					dfg.put(host, 1);
				}
//				if(hostDb.contains(host))
//					break;
				
				
				fout.add(k);
				hostDb.add(host);
				
				limit--;
				if(limit ==0)
					break;
			}
			
			if(limit ==0)
				break;
		}
		
		System.out.println(fout.size());
		
		FileUtils.writeTextFile("back-baseball.txt", fout, false);
		SegmentExtractor.extractSegment("back-baseball.txt", "back-baseball-s.txt");
		System.exit(0);
		
		
		
		
		
		
		
		
		
		int cp = 0;
		HashMap<String, ArrayList<File>> picsdata = new HashMap<>();
		for(File f: FileUtils.getAllFile("a")){
			String name = f.getName();
			System.out.println(name);
			
			String chapter;
			if(name.indexOf("(") < 0)
				chapter = "0";
			else
				chapter = name.substring(name.indexOf("(")).replace("(", ")");
			
			if(!picsdata.containsKey(chapter)){
				picsdata.put(chapter, new ArrayList<>());
			}
			picsdata.get(chapter).add(f);
			
		}
		
		for(String s: picsdata.keySet()){
			ArrayList<File> pics = picsdata.get(s);
			for(File p: pics){
//				FileUtils.rename(p.getAbsolutePath(), "");
				p.renameTo(new File("pic" + cp + ".jpg"));
				cp++;
			}
		}
		
		
		System.exit(0);
		System.out.print("{");
		String[] lines = FileUtils.readFile("hosts_new.txt");
		for(int i=0; i<lines.length; i++){
			String s = lines[i];
			
			if(i != lines.length -1)
				System.out.print("\"" + s +"\",");
			else
				System.out.print("\"" + s + "\"}");
			
		}
		
		
		System.exit(0);
		HashSet<String> output = new HashSet<>();
		for(String s: FileUtils.readFile("hosts.txt")){
			s = s.replace("www.", "");
			int count = StringUtils.countWordInStr(s, ".");
			String[] tmp = s.split("\\.");
			String host = "";
			
			int start = 0;
			
			if(count == 2){
				start = 1;
			}else if(count == 3){
				start = 2;
			}else if(count == 4){
				start = 3;
			}
			
			for(int i=start; i<tmp.length; i++){
				host += tmp[i];
				if(i != tmp.length-1)
					host += ".";
			}
			output.add(host);
		}
		
		FileUtils.writeTextFile("hosts_new.txt", output, false);
		System.exit(0);
		int rel = 0;
		for(String s: FileUtils.readFile("ic.log")){
			if(!s.contains("BFSCrawler:187- DOWNLOADED"))
				continue;
			
			
			s = s.substring(s.indexOf("DOWNLOADED"));
			String[] tmp = s.split("\t");
			
			double score = Double.parseDouble(tmp[1]);
			System.out.println(score);
			if(score > 0.5)
				rel++;
		}
		System.out.println(rel);
		
		System.exit(0);
		UrlDb.createEnvironment("urlDb");
		
		for(String s: FileUtils.readFile("test-gaming-s.txt")){
			
			if(s.equals("=="))
				continue;
			
			if(!UrlDb.getUrlDAO().checkAndAddUrl(s, false))
				System.out.println(s + "\n==");
		}
		UrlDb.close();
		
		System.exit(0);
		int count = 0;
		HashSet<String> u = new HashSet<>();
		FileUtils.deleteDir("tmp");
		UrlDb.createEnvironment("urlDb2");
		for(String s: FileUtils.readFile("tmp.txt")){
//			u.add(s.toLowerCase());
			String host = HttpUtils.getHost(s);
			count++;
			if(UrlDb.getUrlDAO().checkAndAddUrl(s, true) == true)
				break;
		}
		
		System.out.println(u.size());
		System.out.println(count);
		UrlDb.close();
			
		
		
		System.exit(0);
		
		HashSet<String> ddd = new HashSet<>();
		BufferedWriter bww = FileUtils.getBufferedFileWriter("url3.txt");
		for(String s: FileUtils.readFile("logs/ic.log")){
			
			if(!s.contains("http://"))
				continue;
			
			s = s.substring(s.indexOf("http://"));
			System.out.println(s);
			String[] tmp = s.split("\t");
			
			if(tmp.length == 1)
				continue;
			
			String url = tmp[0].trim();
			double score = Double.parseDouble(tmp[1].trim());
			if(score > 0.5){
				ddd.add(HttpUtils.getHost(url));
			}
			
		}
		
		for(String s: ddd){
			bww.write("http://" + s + "\n");
		}
		
		bww.flush();
		
		bww.close();
		
		System.exit(0);
		ArrayList<String> data = new ArrayList<>();
		for(String s: FileUtils.readArffDataWithoutSplit("logs/features.arff")){
			String[] tmp = s.split(",");
//			if(tmp[tmp.length-1].equals("non")){
				if(tmp[0].contains("sansiri"))
					continue;
//			}
			
			data.add(s);
		}
		
		FileUtils.writeTextFile("logs/features-filtered.arff", data, false);
		
		System.exit(0);
		HashSet<String> urls = new HashSet<>();
		for(String s: FileUtils.readFile("urls.txt")){
			urls.add(s.toLowerCase());
		}
		
		
		HashSet<String> db = new HashSet<>();
		
		for(String s: FileUtils.readFile("logs/ic.log")){
			if(!s.contains("DOWNLOADED"))
				continue;
			
			s = s.substring(s.indexOf("http://"));
			s = s.substring(0, s.indexOf("\t"));
			
			db.add(s.toLowerCase());
		}
		
		
//		System.out.println(urls.size());
//		System.out.println(db.size());
		
		
		for(String s: db){
			urls.remove(s);
		}
		
		System.out.println(">>" + urls.size());
		
		
		FileUtils.writeTextFile("urls-no2.txt", new ArrayList<String>(urls), false);
		
		
		System.exit(0);
		for(String s: FileUtils.readFile("output.txt")){
			if(s.contains("dive")||s.contains("scuba"))
				FileUtils.writeTextFile("output1.txt", s +"\n", true);
			
//			if(s.contains("andaman"))
//				FileUtils.writeTextFile("output1.txt", s +"\n", true);
//			if(s.contains("thailand"))
//				FileUtils.writeTextFile("output1.txt", s +"\n", true);
//	
//			if(s.contains("pattaya"))
//				FileUtils.writeTextFile("output1.txt", s +"\n", true);
			
		}
		
		System.exit(0);
		UrlDb.createEnvironment("urlDb-estate");
		
		for(String s: FileUtils.readFile("test-estate-s.txt")){
			if(s.equals("=="))
				continue;
			
			System.out.println(s + "\t" + UrlDb.getUrlDAO().checkAndAddUrl(s, false));
		}
		
		System.exit(0);
		try {
			FileUtils.separate("dest-from-3.txt", "dl-tourism", 120000);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.exit(0);
		
		HashMap<String, ArrayList<String>> dd = new HashMap<>();
		dd.put("job", new ArrayList<>());
		
		dd.put("pass", new ArrayList<>());
		dd.put("som", new ArrayList<>());
		
		
		dd.get("job").add("1");
		dd.get("job").add("2");
		dd.get("job").add("3");
		

		dd.get("pass").add("1");
		dd.get("pass").add("2");
		

		dd.get("som").add("1");
		dd.get("som").add("2");
		dd.get("som").add("3");
		dd.get("som").add("4");
		
		
		Map<String, ArrayList<String>> result = new LinkedHashMap<>();
		Stream<Entry<String, ArrayList<String>>> st = dd.entrySet().stream();

		st.sorted(Comparator.comparing(e -> -1*e.getValue().size())).forEachOrdered(e -> result.put(e.getKey(), e.getValue()));

		
		for(String s: result.keySet()){
			System.out.println(s+ "\t" + result.get(s));
		}
		
		
		System.exit(0);
		UrlDb.createEnvironment("urlDb-tourism");
		for (WebsiteSegment ww : FileUtils.readSegmentFile("test-tourism-s.txt")) {
			System.out.println(ww.getSegmentName());
			for (String s : ww.getUrls())
				System.out.println(UrlDb.getUrlDAO().checkAndAddUrl(s, false));
		}

		UrlDb.close();
		System.exit(0);

		ProxyService.setupProxy("test-diving");
		System.out.println(ProxyService.contains("http://www.adangseadivers.com/links/"));
		ProxyService.terminateProxy();

		System.exit(0);
		HashMap<String, Integer> hosts = new HashMap<>();
		try (BufferedWriter bw = FileUtils.getBufferedFileWriter("non_new.arff")) {

			isData = false;

			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("non.arff")));
			String s;
			while ((s = br.readLine()) != null) {

				if (s.contains("@data")) {
					bw.write(s + "\n");
					isData = true;
					continue;
				}

				if (!isData) {
					bw.write(s + "\n");
					continue;
				}

				String[] split = s.split(",");
				if (split.length != 5)
					continue;
				String host = HttpUtils.getHost(split[0].replace("'", "").trim());

				if (split[0].contains("\\")) {
					s = s.substring(split[0].length());
					s = split[0].replace("\\", "-mikebackslashmike-") + s;
				}
				if (host == null)
					continue;

				if (hosts.containsKey(host)) {
					int tmp = hosts.get(host);

					if (tmp > 5)
						continue;

					hosts.put(host, tmp + 1);
				} else {
					hosts.put(host, 1);
				}
				bw.write(s + "\n");
			}
			
			br.close();
		} catch (Exception e) {

		}

		// UrlDb.createEnvironment("urlDb-train-diving");
		//
		// for(String s: FileUtils.readFile("test-diving-s.txt")){
		// System.out.println(s + "\t" + UrlDb.getUrlDAO().containUrl(s));
		// }
		System.exit(0);

		// String t1 =
		// "http://www.mairesorts.com,1.0,0.6299999952316284,com-com,us-us,0.2,0,en,other,0,'hotels
		// and resorts by mai resort group','', thai";
		// String t2 =
		// "http://www.mairesorts.com,1.0,0.7200000286102295,com-com,us-us,0.2,0,en,other,0,'hotels
		// and resorts by mai resort group','', thai";
		//
		// System.out.println(StringUtils.hash(t1));
		// System.out.println(StringUtils.hash(t2));

		// ProxyService.setupProxy("test");
		//
		// ProxyModel tmp =
		// ProxyService.retreiveContentByURL("http://paulgarrigan.com", null);
		//
		// for(LinksModel s: tmp.getLinks())
		// System.out.println(s.getLinkUrl());
		//
		// ProxyService.terminateProxy();

		// if(true)
		// System.exit(0);

		HashSet<String> url = new HashSet<>();

		for (String s : FileUtils.readFile("logs/ic.log")) {
			if (!s.contains("ProxyFocusedCrawler:180- "))
				continue;

			s = s.substring(s.indexOf("http://"));
			s = s.split("\t")[0];
			url.add(s.toLowerCase());
		}

		// ProxyService.setupProxy("test");
		boolean found = false;
		for (String s : FileUtils.readFile("tmp.txt")) {

			if (url.contains(s.toLowerCase())) {
				found = true;
			}

			if (s.lastIndexOf("/") == s.length() - 1 && url.contains(s.substring(0, s.lastIndexOf("/")).toLowerCase())) {
				found = true;
			}

			if (url.contains(s + "/"))
				found = true;

			if (!found)
				System.out.println(s);

			found = false;
		}
		// ProxyService.terminateProxy();
	}

	public static void compareLog() throws Exception {
		HashSet<String> urls = new HashSet<String>();
		for (String line : FileUtils.readFile("logs/ic.log")) {
			if (!line.contains("DOWNLOADED"))
				continue;

			line = line.substring(line.indexOf("DOWNLOADED") + "DOWNLOADED".length());

			urls.add(line.split("\t")[1].toLowerCase());
		}

		System.out.println("-----");

		for (String line : FileUtils.readFile("dl-train-tourism/logs/ic.log")) {
			if (!line.contains("DOWNLOADED"))
				continue;

			line = line.substring(line.indexOf("DOWNLOADED") + "DOWNLOADED".length());

			line = line.split("\t")[3].toLowerCase();
			if (!urls.contains(line))
				System.out.println(line);
		}
	}

}
