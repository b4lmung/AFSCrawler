package com.job.ic.experiments;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import com.job.ic.utils.FileUtils;

public class Distributor {

	public static void main(String[] args) throws IOException {

	}

	

	public static void analyzeFeatures() throws IOException {
		String[] lines = FileUtils.readFile("thai-n.arff");
		// System.out.println("===");

		Hashtable<String, Integer> counter = new Hashtable<>();
		// 3=domain, 4=country
		int targetId = 2;

		String target;
		String[] tmp;

		boolean checkSrc = false;

		HashSet<String> set = new HashSet<>();
		// BufferedWriter bw = FileUtils.getBufferedFileWriter("tmp.txt");
		boolean isData = false;
		for (String s : lines) {
			tmp = s.split(",");
			if (s.contains("@data")) {
				isData = true;
				continue;
			}

			// check dup

			if (set.contains(tmp[0].trim().toLowerCase()))
				continue;

			if (checkSrc)
				set.add(tmp[0].trim().toLowerCase());

			if (!isData)
				continue;
			//
			target = tmp[targetId];

			if (!target.contains("-inf-0.1")) {
				// bw.write(s + "\n");
			}

			if (counter.containsKey(target)) {
				counter.put(target, counter.get(target) + 1);
			} else {
				counter.put(target, 1);
			}

		}
		// bw.close();
		Enumeration<String> e = counter.keys();
		// System.out.println("===");
		while (e.hasMoreElements()) {
			target = e.nextElement();
			System.out.println(target + "\t" + counter.get(target));
		}
	}
}
