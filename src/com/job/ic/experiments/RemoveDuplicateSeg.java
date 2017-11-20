package com.job.ic.experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import com.job.ic.crawlers.models.ResultModel;
import com.job.ic.utils.SegmentExtractor;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;

@SuppressWarnings("unused")
public class RemoveDuplicateSeg {

	/**
	 * @param args
	 * @throws UnsupportedEncodingException
	 */

	public static String b(String url) {
		if (url.contains("?")) {
			return url.substring(0, url.indexOf("?"));
		}
		return url;
	}

	public static void main(String[] args) throws IOException{
		removeDupFrontier("d:/dataset1.txt", "d:/seeds-new-set.txt", "d:/newset-f.txt");
		SegmentExtractor.extractSegment("d:/newset-f.txt", "d:/newset-seed.txt");
		
	}
	
	public static void removeDupFrontier(String downoadedSeg, String frontierPath, String outputPath) throws IOException {
		String[] seg = FileUtils.readFile(downoadedSeg);
		HashSet<String> segDb = new HashSet<>();
		String base;
		
		for(String s: seg){
			base = HttpUtils.getBasePath(s).toLowerCase();
			segDb.add(base);
		}
		
		
		BufferedWriter bw = FileUtils.getBufferedFileWriter(outputPath);
		String[] s = FileUtils.readFile(frontierPath);
		for (String t : s) {
			base = HttpUtils.getBasePath(t).toLowerCase();

			if(segDb.contains(base)){
				FileUtils.writeTextFile("dup.txt", base + "\n", true);
				continue;
			}

			if (t.startsWith("file:"))
				continue;
			
			if (t.contains("twitter.com"))
				continue;

			if (t.contains("facebook.com"))
				continue;

			if (t.contains("google.com"))
				continue;

			if (t.contains("twitter.com"))
				continue;

			if (t.toLowerCase().contains(".jpg"))
				continue;

			if (t.toLowerCase().contains(".png"))
				continue;

			if (t.toLowerCase().contains(".gif"))
				continue;

			if (t.toLowerCase().contains(".pdf"))
				continue;

			if (t.toLowerCase().contains(".wmv"))
				continue;
			bw.write(t + "\n");
		}
		
		bw.close();
	}
//
//	public static void main(String[] args) {
//
//		// source
//		File[] f = new File(args[0]).listFiles();
//		String[] tmp;
//		HashSet<String> basedb = new HashSet<>();
//
//		for (File fi : f) {
//			tmp = FileUtils.readFile(fi.getAbsolutePath());
//			for (String s : tmp) {
//				s = HttpUtils.getBasePath(s);
//				if (s == null)
//					continue;
//
//				basedb.add(s);
//			}
//		}
//		tmp = FileUtils.readFile(args[1]);
//
//		HashSet<String> urldb = new HashSet<>();
//		ArrayList<String> output = new ArrayList<>();
//		String b;
//		for (String s : tmp) {
//			s = s.trim();
//			b = HttpUtils.getBasePath(s);
//			if (b == null)
//				continue;
//
//			if (basedb.contains(b))
//				continue;
//
//			if (urldb.contains(s))
//				continue;
//
//			output.add(b);
//			urldb.add(s);
//		}
//
//		SegmentExtractor.extractSegment(output, args[2]);
//	}
//
//	public static void oldextract(String[] args) {
//		String[] h0 = FileUtils.readFile("seeds/segment/back-hop1-s.txt");
//		String[] h1 = FileUtils.readFile("seeds/segment/hop0-seed-s.txt");
//		String[] h2 = FileUtils.readFile("seeds/segment/hop01-s.txt");
//		String[] h3 = FileUtils.readFile("seeds/segment/hop1-s.txt");
//		String[] h4 = FileUtils.readFile("seeds/segment/hop2-s.txt");
//
//		HashSet<String> db = new HashSet<String>();
//		HashSet<String> basedb = new HashSet<String>();
//		HashSet<String> all = new HashSet<String>();
//		for (String s : h0) {
//			s = b(s);
//			if (!db.contains(s) && !db.contains(HttpUtils.getBasePath(s)))
//				all.add(s);
//
//			db.add(s);
//			basedb.add(HttpUtils.getBasePath(s));
//		}
//		write(all, "d:/back-hop1.txt");
//		SegmentExtractor.extractSegment("d:/back-hop1.txt", "d:/back-hop1-s.txt");
//
//		for (String s : h1) {
//			s = b(s);
//			if (!db.contains(s) && !db.contains(HttpUtils.getBasePath(s)))
//				all.add(s);
//
//			db.add(s);
//			basedb.add(HttpUtils.getBasePath(s));
//		}
//		write(all, "d:/hop0.txt");
//		SegmentExtractor.extractSegment("d:/hop0.txt", "d:/hop0-s.txt");
//
//		for (String s : h2) {
//			s = b(s);
//			if (!db.contains(s) && !db.contains(HttpUtils.getBasePath(s)))
//				all.add(s);
//
//			db.add(s);
//			basedb.add(HttpUtils.getBasePath(s));
//		}
//
//		write(all, "d:/hop0-1.txt");
//		SegmentExtractor.extractSegment("d:/hop0-1.txt", "d:/hop0-1-s.txt");
//
//		for (String s : h3) {
//			s = b(s);
//			if (!db.contains(s) && !db.contains(HttpUtils.getBasePath(s)))
//				all.add(s);
//
//			db.add(s);
//			basedb.add(HttpUtils.getBasePath(s));
//		}
//
//		write(all, "d:/hop1.txt");
//		SegmentExtractor.extractSegment("d:/hop1.txt", "d:/hop1-s.txt");
//
//		for (String s : h4) {
//			s = b(s);
//			if (!db.contains(s) && !db.contains(HttpUtils.getBasePath(s))) {
//				// System.out.println(s);
//				all.add(s);
//			}
//
//			db.add(s);
//			basedb.add(HttpUtils.getBasePath(s));
//		}
//
//		write(all, "d:/hop2.txt");
//		SegmentExtractor.extractSegment("d:/hop2.txt", "d:/hop2-s.txt");
//
//	}

	public static void write(HashSet<String> all, String path) {
		try (BufferedWriter bw = FileUtils.getBufferedFileWriter(path)) {
			for (String s : all) {

				if (s.contains("twitter.com"))
					continue;

				if (s.contains("facebook.com"))
					continue;

				if (s.contains("google.com"))
					continue;

				if (s.contains("twitter.com"))
					continue;

				if (s.toLowerCase().contains(".jpg"))
					continue;

				if (s.toLowerCase().contains(".png"))
					continue;

				if (s.toLowerCase().contains(".gif"))
					continue;

				if (s.toLowerCase().contains(".pdf"))
					continue;

				if (s.toLowerCase().contains(".wmv"))
					continue;

				if (s.toLowerCase().contains("&"))
					continue;

				if (s.toLowerCase().contains(";"))
					continue;

				if (StringUtils.countWordInStr(s, "http") >= 2)
					continue;

				bw.write(s.concat("\n"));
			}
		} catch (Exception e) {

		}
		all.clear();
	}

	public static void hv(String[] args) throws IOException {
		// sortResult();
		double level = 0;
		while (level <= 75) {

			BufferedWriter bw = FileUtils.getBufferedFileWriter("d:\\hv-cv-" + level + ".txt");
			String[] results = FileUtils.readFile("d:/Result/" + level + ".txt");
			int total = 0;
			int t = -1;
			String[] tmp;

			for (int i = 0; i < results.length; i++) {
				tmp = results[i].split("\t");
				total = Integer.parseInt(tmp[3]);
				if (t < total / 20000) {
					bw.write(String.format("%d\t%s\t%s\n", total, tmp[4], tmp[5]));
					t = total / 20000;
					System.out.println(t + "\t" + total);
				}

				if (t > 142)
					break;
				// bw.write(String.format("%d\t%s\t%s\n", total, tmp[4], tmp[5]));

			}

			level += 12.5;
			bw.close();
			// break;
		}
	}

	public static void sortResult() throws IOException {

		HashSet<String> set = new HashSet<String>();
		ArrayList<ResultModel> rs = new ArrayList<ResultModel>();
		BufferedWriter bw;
		String[] tmp;
		String filepath;

		double level = 0;
		int atLeast = 5;
		long thai = 0;
		long non = 0;
		long total = 0;
		double hv = 0;

		while (level <= 0) {
			set.clear();
			thai = 0;
			non = 0;
			total = 0;
			filepath = "result/" + level + ".txt";
			bw = FileUtils.getBufferedFileWriter("d:/" + filepath);

			String[] l = FileUtils.readFile("Result/" + 0.0 + "-" + 0 + ".txt");
			for (String s : l) {
				tmp = s.split("\t");
				if (!set.contains(tmp[0])) {
					rs.add(new ResultModel(tmp[0], Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2]), 0, null, null));
				}

				set.add(tmp[0]);
			}

			for (ResultModel m : rs) {
				if (m.getRelevantPage() + m.getIrrelevantPage() >= atLeast) {
					thai += m.getRelevantPage();
					non += m.getIrrelevantPage();
					total += m.getIrrelevantPage() + m.getRelevantPage();
					bw.write(String.format("%s\t%d\t%d\t%d\t%.2f\t%.2f\n", m.getSegmentName(), m.getRelevantPage(), m.getIrrelevantPage(), total, 1.0 * thai / total, thai / 1629303.0));
				}
			}
			rs.clear();

			for (int i = 1; i <= 2; i++) {

				String[] lines = FileUtils.readFile("Result/" + level + "-" + i + ".txt");
				for (String s : lines) {
					tmp = s.split("\t");
					if (!set.contains(tmp[0])) {
						rs.add(new ResultModel(tmp[0], Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2]), 0, null, null));
					}

					set.add(tmp[0]);
				}

				Collections.sort(rs, new Comparator<ResultModel>() {
					@Override
					public int compare(ResultModel o1, ResultModel o2) {
						double t1, t2;
						t1 = (1.0 * o1.getRelevantPage()) / (o1.getRelevantPage() + o1.getIrrelevantPage());
						t2 = (1.0 * o1.getRelevantPage()) / (o1.getRelevantPage() + o1.getIrrelevantPage());

						if (t1 > t2)
							return -1;
						else if (t1 < t2)
							return +1;

						return 0;
					}
				});

				for (ResultModel m : rs) {
					if (m.getRelevantPage() + m.getIrrelevantPage() >= atLeast) {
						thai += m.getRelevantPage();
						non += m.getIrrelevantPage();
						total += m.getIrrelevantPage() + m.getRelevantPage();
						bw.write(String.format("%s\t%d\t%d\t%d\t%.2f\t%.2f\n", m.getSegmentName(), m.getRelevantPage(), m.getIrrelevantPage(), total, 1.0 * thai / total, thai / 1629303.0));
					}
				}
				rs.clear();
			}
			level += 12.5;
			bw.close();

		}
	}
}
