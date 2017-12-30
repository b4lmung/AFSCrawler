package com.job.ic.crawlers.site;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;

public class ParseLog {
	public static void main(String[] args) {

		int count = 0;

		HashMap<String, ArrayList<String>> dlhosts = new HashMap<>();

		for (String s : FileUtils.readFile("ic.log")) {

			if (s.contains("CumRelPage:10001"))
				break;

			if (!s.contains("DOWNLOADED"))
				continue;

			s = s.substring(s.indexOf("DOWNLOADED") + "DOWNLOADED".length());
			s = s.trim();
			String[] fields = s.split("\t");

			double score = Double.parseDouble(fields[0]);
			String url = fields[2];
			String host = HttpUtils.getHost(url);

			if (score > 0.5) {
				if (!dlhosts.containsKey(host))
					dlhosts.put(host, new ArrayList<>());

				dlhosts.get(host).add(url);
			}
			// 0,2

		}

		
		try {
			
			BufferedWriter bw = FileUtils.getBufferedFileWriter("output.txt");

			for (String host : dlhosts.keySet()) {

				ArrayList<String> urls = dlhosts.get(host);
				if (urls.size() > 300)
					continue;

				for (String s : urls)
					bw.write(s + "\t");
				bw.write("\n");

			}
			
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
