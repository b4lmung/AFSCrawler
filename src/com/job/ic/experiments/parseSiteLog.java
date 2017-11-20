package com.job.ic.experiments;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.job.ic.utils.FileUtils;

public class parseSiteLog {
	public static void main(String[] args) {
//		prepareLog("tourism");
//		prepareLog("tourism-jp");
//		prepareLog("estate");
//		prepareLog("diving");

//		parseLog("tourism");
//		parseLog("diving");
//		parseLog("estate");
//		parseLog("diving");
		
	}
	
	public static void parseLog(String topic){
		int thai = 0;
		int non = 0;
		int total = 0;
		int t = 0;
		int threshold = 400;
		for (String s : FileUtils.readFile("results-site-" + topic + ".txt")) {
			String[] data = s.split("\t");
			String str = data[1].trim();
			
			for(int i=0; i<str.length(); i++){
				if(total > 10000)
					break;
				if(total % 10 == 0){
					System.out.printf("%d\t%.3f\n", total, 1.0*thai/total);
				}
				if(str.charAt(i) == '1'){
					t = 0;
					thai++;
					total++;
				}else{
					t++;
					non++;
					total++;
					if(t > threshold)
						break;
				}
				
			}
			
		}
			
	}

	public static void prepareLog(String topic) {
		// Crawler:152
		String path = topic;
		int thai = 0;
		int non = 0;
		String target = "Crawler:187- FINISHED :";

		HashMap<String, String> db = new HashMap<>();
		ArrayList<String> siteList = new ArrayList<>();
		for (String s : FileUtils.readFile("site-" + path + ".txt")) {
			if (s.equals("=="))
				continue;

			siteList.add(s);
		}

		int length = 0;
		
		siteList.sort((String o1, String o2) -> o1.compareTo(o2));
		
		
		for (String s : FileUtils.readFile("logs-site-" + path + "/ic.log")) {
			if (!s.contains(target)) {
				continue;
			}

			s = s.substring(s.indexOf(target) + target.length());
			String[] tmp = s.split("\t");
			if (tmp.length != 5)
				continue;

			db.put(tmp[0].toLowerCase().trim(), tmp[4]);
			length += tmp[4].trim().length();

		}

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(
				"results-site-" + path + ".txt"))) {
			for (String s : siteList) {
				s = s.replace("http://", "");
				if (db.containsKey(s.toLowerCase())) {
					bw.write(s + "\t" + db.get(s.toLowerCase()) + "\n");
				}
			}
		} catch (Exception e) {

		}

		System.out.println(length);
	}
}
