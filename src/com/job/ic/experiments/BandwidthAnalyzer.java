/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.experiments;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.nlp.services.Checker;
import com.job.ic.nlp.services.LangChecker;

public class BandwidthAnalyzer {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		try (FileInputStream fis = new FileInputStream("logs_files/FullLog.txt"); BufferedReader br = new BufferedReader(new InputStreamReader(fis));) {
			Checker checker = new LangChecker();
			String tmp;
			String[] t;
			double thai = 0, non = 0, score, num_byte;
			while ((tmp = br.readLine()) != null) {
				t = tmp.split("\t");
				if (t[2].split("/").length > 14) {
					continue;
				}

				score = Double.parseDouble(t[0]);
				num_byte = Double.parseDouble(t[1]);
				if (Checker.getResultClass(score) == ResultClass.RELEVANT) {
					thai += num_byte / 1024.0;
				} else {
					non += num_byte / 1024.0;
				}

			}
			System.out.println("THAI:\t" + thai / 1024.0 + "\tMb");
			System.out.println("NON:\t" + non / 1024.0 + "\tMb");
		} catch (Exception e) {

		}

	}
}
