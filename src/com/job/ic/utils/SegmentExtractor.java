package com.job.ic.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.daos.ResultDb;
import com.job.ic.crawlers.daos.SegmentGraphDAO;
import com.job.ic.crawlers.daos.UrlDAO;
import com.job.ic.crawlers.daos.UrlDb;

public class SegmentExtractor {

	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(SegmentExtractor.class);

	
	
	/**
	 * @param args
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException {

		// String[] ll = FileUtils.readFile("test-s.txt");
		// Arrays.sort(ll);
		//
		// ArrayList<String> ln = new ArrayList<>();
		// BufferedWriter b = FileUtils.getBufferedFileWriter("list.txt");
		// for(int i=0; i<5000; i++){
		// ln.add(ll[i].split("\t")[2]);
		// b.write(ll[i] + "\n");
		// }
		// b.close();
		// extractSegment(ln, "non-estate-s.txt");
		// extractFrontier("db-1", "tt.txt", new HashSet<String>(), false);
		// extractSegment("back-thai-tourism-jp.txt",
		// "back-thai-tourism-jp-s.txt");
		// extractSegment("back-thai-tourism-jp.txt",
		// "back-thai-tourism-jp-s.txt");

		UrlDb.createEnvironment("C:/Users/b4lmung/Documents/Research/current-research/tourism-jp/urlDb");
		UrlDAO d = UrlDb.getUrlDAO();
		System.out.println(d.pix.count());
		// ArrayList<String> data = new ArrayList<>();
		// for(String s: FileUtils.readFile("dest-from-1.txt"))
		// d.delete(s);

		UrlDb.close();
		//
		// System.out.println(data.size());
		// extractSegment(data, "back-s.txt");

		if (true)
			return;

		// extractSegment("seeds/cusine/all-cusine.txt",
		// "seeds/cusine/all-cusine-exc-thai_cusine-s.txt");
		// extractSegment("seeds/cusine/all-thai.txt",
		// "seeds/cusine/all-thai-exc-exc-thai_cusine-s.txt");

		
		logger.info("extract segment");
		HashSet<String> dl = new HashSet<>();
		System.out.println("dbPath  previousHop");
		String target = "next.txt";
		String dbPath = args[0];
		String source = "next.txt";
		// rename file
		FileUtils.rename(source, args[1]);

		String[] ls = FileUtils.readFile(args[1]);

		BufferedWriter bw = FileUtils.getBufferedFileWriter("dl", true);

		for (String l : ls) {
			if (HttpUtils.getBasePath(l) != null)
				bw.write(HttpUtils.getBasePath(l).toLowerCase() + "\n");
		}

		bw.close();

		if (FileUtils.exists("dl")) {
			String[] l = FileUtils.readFile("dl");
			for (String s : l) {
				if (s.equals("=="))
					continue;

				if (HttpUtils.getBasePath(s) != null)
					dl.add(HttpUtils.getBasePath(s).toLowerCase());
				else
					dl.add(s);
			}
		}
		SegmentExtractor.extractFrontier(dbPath, "tmp-frontier.txt");
		FileUtils.convert("tmp-frontier.txt", target);
		FileUtils.deleteFile("tmp-frontier.txt");
	}

	public static void analyzeDuplicateSegmentExtractor(String[] args) throws IOException {

		int count = 0;
		String[] t;
		HashSet<String> set0 = new HashSet<>();
		HashSet<String> set1 = new HashSet<>();

		t = FileUtils.readFile("d:/set0.txt");
		for (String s : t) {
			s = s.toLowerCase().trim();
			set0.add(s);
		}

		String[] tmp;
		t = FileUtils.readFile("d:/dup.txt");
		for (String s : t) {
			set1.add(s.toLowerCase().trim());
		}

		BufferedWriter bw = FileUtils.getBufferedFileWriter("d:/couppling.txt");
		HashSet<String> db = new HashSet<>();
		HashSet<String> hostDb = new HashSet<>();
		String h;

		int c = 0;
		for (String s : set1) {
			s = s.toLowerCase().trim();
			if (db.contains(s))
				continue;

			h = HttpUtils.getHost(s);
			h = "http://" + (h + "/").replace("//", "/");
			if ((set0.contains(s)) || set0.contains(h)) {
				bw.write(s.concat("\n"));

				if (set0.contains(s)) {
					count++;
				}

				hostDb.add(h);
				if (set0.contains(h)) {
					if (!db.contains(s))
						c++;
				}
			}
			db.add(s);

		}

		bw.close();

		System.out.println(c);
		System.out.println(count);
		System.out.println(hostDb.size());

	}

	public static void extractFrontier(String dbPath, String output) {
		ResultDb.createEnvironment(dbPath);
		SegmentGraphDAO sd = ResultDb.getSegmentGraphDAO();
		sd.extractAllLinks(output);
		ResultDb.close();
	}

	public static String stat(String url) {
		if (url.contains("?")) {
			return url.substring(0, url.indexOf("?"));
		}
		return url;
	}

	public static void extractSegment(String source, String dest) {
		String[] lines = FileUtils.readFile(source);

		Arrays.sort(lines);
		HashSet<String> udb = new HashSet<String>();
		ArrayList<String> st = new ArrayList<>();
		for (String s : lines) {
			if(udb.contains(s))
				continue;
			
			if (HttpUtils.getBasePath(s) != null){
				udb.add(s);
				st.add(s);
			}
		}

		udb.clear();
		
		Collections.sort(st, (o1, o2) -> HttpUtils.getBasePath(o1.trim()).compareToIgnoreCase(HttpUtils.getBasePath(o2.trim())));

		
		HashMap<String, ArrayList<String>> smap = new HashMap<String, ArrayList<String>>();
		
		for(String s: st){
			String bp = HttpUtils.getBasePath(s);
			if(!smap.containsKey(bp)){
				ArrayList<String> data = new ArrayList<String>();
				data.add(s);
				smap.put(bp, data);
			}else{
				smap.get(bp).add(s);
			}
		}
		
		try (BufferedWriter bw = FileUtils.getBufferedFileWriter(dest)) {
			
			for(String b : smap.keySet()){
				for(String u : smap.get(b)){
					bw.write(u + "\t");
				}
				
				bw.write("\n==\n");		
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		// System.err.println(host.size());
	}

	// public static void extractSegment(ArrayList<String> st, String dest) {
	// Collections.sort(st, new Comparator<String>() {
	//
	// @Override
	// public int compare(String o1, String o2) {
	// return o1.compareToIgnoreCase(o2);
	// }
	// });
	//
	// String tmp, tmpHost, preHost = null;
	// HashSet<String> sDb = new HashSet<String>();
	// try (BufferedWriter bw = FileUtils.getBufferedFileWriter(dest)) {
	// for (String s : st) {
	// tmp = HttpUtils.getBasePath(s);
	// tmpHost = HttpUtils.getHost(s);
	//
	// if (tmpHost == null || tmp == null)
	// continue;
	//
	// tmpHost = tmpHost.toLowerCase();
	//
	// if (!sDb.contains(tmp.toLowerCase()) && tmp != null &&
	// tmp.trim().length() > 0) {
	// if (preHost != null && !preHost.equals(tmpHost)) {
	// bw.write("==\n" + s + "\n");
	// } else {
	// bw.write(s + "\n");
	// }
	// sDb.add(tmp.toLowerCase());
	// }
	//
	// preHost = tmpHost.toLowerCase();
	// }
	//
	// bw.write("==");
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
	//
	// public static ArrayList<String> extractSegment(ArrayList<String> st) {
	// ArrayList<String> output = new ArrayList<>();
	// Collections.sort(st, new Comparator<String>() {
	//
	// @Override
	// public int compare(String o1, String o2) {
	// return o1.compareToIgnoreCase(o2);
	// }
	// });
	//
	// String tmp, tmpHost, preHost = null;
	// HashSet<String> sDb = new HashSet<String>();
	// // try (BufferedWriter bw = FileUtils.getBufferedFileWriterdest()) {
	// for (String s : st) {
	// tmp = HttpUtils.getBasePath(s);
	// tmpHost = HttpUtils.getHost(s);
	//
	// if (tmpHost == null || tmp == null)
	// continue;
	//
	// tmpHost = tmpHost.toLowerCase();
	//
	// if (!sDb.contains(tmp.toLowerCase()) && tmp != null &&
	// tmp.trim().length() > 0) {
	// if (preHost != null && !preHost.equals(tmpHost)) {
	// output.add("==\n" + s + "\n");
	// } else {
	// output.add(s + "\n");
	// }
	// sDb.add(tmp.toLowerCase());
	// }
	//
	// preHost = tmpHost.toLowerCase();
	// }
	//
	// output.add("==");
	// // } catch (Exception e) {
	// // e.printStackTrace();
	// // }
	//
	// return output;
	// }
	// public static HashSet<String> extractSegment(String sourcePath) {
	// String[] st = FileUtils.readFile(sourcePath);
	// Arrays.sort(st);
	//
	// String tmp, tmpHost, preHost = null;
	// HashSet<String> sDb = new HashSet<String>();
	// // try (BufferedWriter bw = FileUtils.getBufferedFileWriter(dest)) {
	// for (String s : st) {
	// tmp = HttpUtils.getBasePath(s);
	// tmpHost = HttpUtils.getHost(s);
	// if (!sDb.contains(tmp) && tmp != null && tmp.trim().length() > 0) {
	// // if (preHost != null && !preHost.equals(tmpHost)) {
	// // bw.write("==\n" + s + "\n");
	// // }else{
	// // bw.write(s + "\n");
	// // }
	// sDb.add(tmp);
	// }
	//
	// preHost = tmpHost;
	// }
	// return sDb;
	// // bw.write("==");
	// // } catch (Exception e) {
	// // e.printStackTrace();
	// // }
	// }

	// public static void extractSegment(String[] st, String dest) {
	// Arrays.sort(st);
	//
	// String tmp, tmpHost, preHost = null;
	// HashSet<String> sDb = new HashSet<String>();
	// try (BufferedWriter bw = FileUtils.getBufferedFileWriter(dest)) {
	// for (String s : st) {
	// tmp = HttpUtils.getBasePath(s);
	// tmpHost = HttpUtils.getHost(s);
	// if (!sDb.contains(tmp) && tmp != null && tmp.trim().length() > 0) {
	// if (preHost != null && !preHost.equals(tmpHost)) {
	// bw.write("==\n" + s + "\n");
	// }else{
	// bw.write(s + "\n");
	// }
	// sDb.add(tmp);
	// }
	//
	// preHost = tmpHost;
	// }
	//
	// bw.write("==");
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
}
