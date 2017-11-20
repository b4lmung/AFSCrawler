package com.job.ic.nlp.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.utils.FileUtils;

public class Stemmer {
	private static HashSet<String> stopword = new HashSet<>();

	static {
		InputStream is = Stemmer.class.getResourceAsStream("/resources/classifiers/stopword.txt");
		String[] lines = FileUtils.readStream(is);

		for (String s : lines) {
			stopword.add(s);
		}
	}

	public static String stem(String text) {
		
		if (!CrawlerConfig.getConfig().getTargetLang().equals("en"))
			return text;

		text = text.toLowerCase();
		StringBuffer result = new StringBuffer();
		if (text != null && text.trim().length() > 0) {
			StringReader tReader = new StringReader(text);
			Analyzer analyzer;

			analyzer = new StandardAnalyzer(Version.LUCENE_35, stopword);

			TokenStream tStream = analyzer.tokenStream("contents", tReader);
			CharTermAttribute term = tStream.addAttribute(CharTermAttribute.class);

			try {
				while (tStream.incrementToken()) {
					result.append(term.toString());
					result.append(" ");
				}
			} catch (IOException ioe) {
				System.out.println("Error: " + ioe.getMessage());
			}
		}

		// If, for some reason, the stemming did not happen, return the original
		// text
		if (result.length() == 0)
			result.append(text);
		return result.toString().trim();
	}

	public static void main(String[] args) {
		String[] s = new String[5];
		s[0] = "Banking on banks to raise the interest rate";
		s[1] = "Jogging along the river bank to look at the sailboats";
		s[2] = "Jogging to the bank to look at the interest rate";
		s[3] = "BuzzerBeating shot banked in!";
		s[4] = "Scenic outlooks on the bank of the PotomacRiver";

		HashSet<String> set = new HashSet<>();
		String[] tmp;
		for (String str : s) {
			System.out.println(Stemmer.stem(str));
			tmp = Stemmer.stem(str).split(" ");
			set.addAll(Arrays.asList(tmp));
		}

		ArrayList<String> output = new ArrayList<>();
		for (String str : set)
			output.add(str);

		Collections.sort(output, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return o1.compareToIgnoreCase(o2);
			}
		});

		for (String str : output)
			System.out.println(str);

		// String target = "D:/current-research/page-classifier/sampling/";
		// non
		// String[] tmp;
		// BufferedWriter bw;
		// for (File i : new File(target + "non").listFiles()) {
		//
		// tmp = FileUtils.readFile(i.getAbsolutePath());
		// try {
		// bw = FileUtils.getBufferedFileWriter(i.getAbsolutePath());
		//
		// for (String s : tmp) {
		// bw.write(s.concat("\n"));
		// }
		// bw.close();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }

		// String s = Stemmer.stem("walking");
		// System.out.println(s);
	}
}