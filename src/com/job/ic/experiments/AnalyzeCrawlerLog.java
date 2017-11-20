package com.job.ic.experiments;

import java.io.File;

import com.job.ic.utils.FileUtils;

public class AnalyzeCrawlerLog {
	public static void main(String[] args) {
		String input = "logs-multi-diving-new-1";

		int relSegs = 0;
		int nonSegs = 0;
		int countRel = 0;
		int countNon = 0;
		int pagePerSeg = 0;
		
		for (File f : FileUtils.getAllFile(input)) {
			if (!f.getName().contains(".log"))
				continue;

			for (String s : FileUtils.readFile(f.getAbsolutePath())) {

				if (!s.contains("DOWNLOADED"))
					continue;

				s = s.substring(s.indexOf("DOWNLOADED") + "DOWNLOADED".length()).trim();

				String[] tmp = s.split("\t");

				if (tmp[4].equals("1")) {
					
					if(1.0*countRel/countRel + countNon > 0.5){
						relSegs++;
						pagePerSeg += countRel + countNon;
					}else{
						nonSegs++;
					}
					
					countRel = 0;
					countNon = 0;
				}

				if (Double.parseDouble(tmp[0]) > 0.5) {
					countRel++;
				}else{
					countNon++;
				}
					

			}

		}
	}
}
