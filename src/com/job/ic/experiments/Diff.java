package com.job.ic.experiments;

import java.util.ArrayList;

import com.job.ic.proxy.ProxyService;
import com.job.ic.proxy.model.ProxyModel;
import com.job.ic.utils.FileUtils;

public class Diff {
	public static void main(String[] args) {

//		String target = "logs-wavg-update-500-tourism";
//		String ref = "logs-best-tourism-soft";
//
//		HashSet<String> db = new HashSet<>();
//
//		File[] fis;
//
//		fis = FileUtils.getAllFile(target);
//		Arrays.sort(fis, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
//
//		for (File fi : fis) {
//
//			System.out.println(fi.getName());
//			for (String s : FileUtils.readFile(fi.getPath())) {
//				if (s.contains("DOWNLOADED")) {
//					s = s.substring(s.indexOf("DOWNLOADED") + "DOWNLOADED".length());
//					s = s.trim();
//					s = s.split("\t")[3];
//					db.add(s.toLowerCase());
//				}
//			}
//		}
//
//		// -==========
//
//		fis = FileUtils.getAllFile(ref);
//
//		ArrayList<String> results = new ArrayList<>();
//		for (File fi : fis) {
//			for (String s : FileUtils.readFile(fi.getPath())) {
//
//				if (s.contains("ProxyFocusedCrawler:174-")) {
//					s = s.substring(s.indexOf("ProxyFocusedCrawler:174-") + "ProxyFocusedCrawler:174-".length()).trim();
//					s = s.split("\t")[0].toLowerCase();
//					
//					
//					if(!db.contains(s)){
//						results.add(s);
//					}
//				}
//
//			}
//		}
//		
//		
//		FileUtils.writeTextFile("not-found.txt", results, false);
		
		ProxyService.setupProxy("e:\\proxy-tourism");
		ArrayList<String> r= new ArrayList<>();
		double avg = 0;
		int count = 0;
		for(String s: FileUtils.readFile("not-found.txt")){
			ProxyModel m = ProxyService.retreiveContentByURL(s, null);
			
			if(m == null)
				continue;
			
			if(m.getScore() > 0.5){
				r.add(s + "\t" + m.getScore());
				avg += m.getScore();
				count++;
			}
		}
		
		System.out.println(avg/count);
		FileUtils.writeTextFile("not-found-thai.txt", r, false);
		ProxyService.terminateProxy();

	}
}
