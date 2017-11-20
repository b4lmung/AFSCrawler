package com.job.ic.ml.classifiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.utils.FileUtils;

import weka.core.tokenizers.WordTokenizer;

public class MyTextTokenizer extends weka.core.tokenizers.WordTokenizer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3177872657091919679L;
	private WordTokenizer analyzer;
	
	protected HashSet<String> lex;
	
	protected transient Enumeration<String> element;
	private HashSet<Object> soundex;
	private static String[] stopwords = {"a", "an", "and", "are", "as", "at", "be", "but", "by",
	"for", "if", "in", "into", "is", "it",
	"no", "not", "of", "on", "or", "such",
	"that", "the", "their", "then", "there", "these",
	"they", "this", "to", "was", "will", "with", "ll", "nt"};

	public static HashSet<String> stopList = new HashSet<>(Arrays.asList(stopwords));
	public static void main(String[] args) {
		MyTextTokenizer m = new MyTextTokenizer();

		String input = "Bangkok Bangkok Bangkok schedules the New Year festival 2013 events to preserve the cultural arts, beautiful traditions of Thailand. Therefore, Bangkok has determined that to set up New Year 2013";
		
		// System.out.println(input.length());
		m.tokenize(input);
		while(m.hasMoreElements()){
			System.out.println(">>" + m.nextElement());
		}
		
//		input = "phuket i'll go to KlongChan";
//
//		m.tokenize(input);
//		while(m.hasMoreElements()){
//			System.out.println("--" + m.nextElement());
//		}
		
		
	}

	public MyTextTokenizer() {
		
		analyzer = new WordTokenizer();
		
		lex = new HashSet<>();
		soundex = new HashSet<>();
		String[] t;
		
		//เอา Dict lexto เข้าใน lex เพื่อใช้เทียบเสียงว่าเป็นคำไทยรึเปล่า
		for (String s : FileUtils.readStream(MyTextTokenizer.class.getResourceAsStream(CrawlerConfig.getConfig().getRelevantKeywordsPath()))) {
			t = s.split("\t");

			if (t.length > 1 && t[1].equals("1"))
				continue;
			lex.add(t[0]);
			lex.add(encode(t[0]));
		}
	}
	

	@Override
	public String getRevision() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String globalInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasMoreElements() {
		// TODO Auto-generated method stub
		return element.hasMoreElements();
	}

	@Override
	public String nextElement() {
		// TODO Auto-generated method stub
		return element.nextElement();
	}

	public ArrayList<String> tokenizePartOne(String input) {
		input = input.toLowerCase();
		ArrayList<String> results = new ArrayList<>();
		
		this.analyzer.tokenize(input);
		String curr;
		while(this.analyzer.hasMoreElements()){
			curr = this.analyzer.nextElement().toString();
			if(stopList.contains(curr))
				continue;
			
			results.add(curr);
		}
		
//		TokenStream st = this.analyzer.tokenStream(null, new StringReader(input));
//		try {
//			while (st.incrementToken()) {
//				results.add(st.getAttribute(CharTermAttribute.class).toString());
//			}
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		return results;
	}

	@Override
	public void tokenize(String arg0) {
		TokenizedOutput output = tokenizeString(arg0, true);
		element = Collections.enumeration(output.getTokenized());
	}

	public TokenizedOutput tokenizeString(String input, boolean incNonWord) {

		TokenizedOutput tout = new TokenizedOutput();

		ArrayList<String> tokenized = tokenizePartOne(input);
		ArrayList<String> output = new ArrayList<>();
	
		String tmp = "", lastKnown = "";

		boolean found = false;
		boolean[] limit = new boolean[tokenized.size()];
		for (int i = 0; i < tokenized.size(); i++) {

			if (limit[i] == true)
				continue;
			

			// ตั้งต้น
			found = false;
			tmp = tokenized.get(i);
			lastKnown = "";

			if(soundex.contains(encode(tmp))){
				output.add(tmp);				
				continue;
			}

			
			// กรณีที่หาแล้วเจอคำแรกเลย ให้ทดเก็บไว้ก่อน
			if (lex.contains(tmp)) {
				lastKnown = tmp;
				found = true;
			}

			
			for (int j = i + 1; j < tokenized.size() && j < i + 10; j++) {

				if (limit[j] == true)
					continue;

				tmp = tmp + " " + tokenized.get(j);
				if (lex.contains(tmp) || soundex.contains(encode(tmp))) {
					lastKnown = tmp;
					found = true;
					limit[j] = true;
					for(int k=j; k>=i; k--)
						limit[k] = true;
				}
			}

			if (found) {
				output.add(lastKnown);
			} else {
//				output.add(Stemmer.stem(tokenized.get(i)));
				output.add(tokenized.get(i));
			}
		}

		
		ArrayList<String> realOutput = new ArrayList<>();
		for (int i = 0; i < output.size(); i++) {

			// เช็คว่า token เป็นตัวเลขรึอักขระพิเศษรึเปล่า
			if (!incNonWord && isNumeric(output.get(i))) {
				continue;
			}

			if (lex.contains(output.get(i))) {
				tout.incKnown();

			} else {
				tout.incUnknown();

//				if (!incNonWord) {
//					continue;
//				}
			}
			
			realOutput.add(output.get(i));

		}
		output.clear();
		tout.setTokenized(realOutput);
		return tout;

	}
	
	public static String encode(String s) { 
        char[] x = s.toUpperCase().toCharArray();
        char firstLetter = x[0];

        // convert letters to numeric code
        for (int i = 0; i < x.length; i++) {
            switch (x[i]) {
                case 'B':
                case 'F':
                case 'P':
                case 'V': { x[i] = '1'; break; }

                case 'C':
                case 'G':
                case 'J':
                case 'K':
                case 'Q':
                case 'S':
                case 'X':
                case 'Z': { x[i] = '2'; break; }

                case 'D':
                case 'T': { x[i] = '3'; break; }

                case 'L': { x[i] = '4'; break; }

                case 'M':
                case 'N': { x[i] = '5'; break; }

                case 'R': { x[i] = '6'; break; }

                default:  { x[i] = '0'; break; }
            }
        }

        // remove duplicates
        String output = "" + firstLetter;
        for (int i = 1; i < x.length; i++)
            if (x[i] != x[i-1] && x[i] != '0')
                output += x[i];

        // pad with 0's or truncate
        output = output + "0000";
        return output.substring(0, 4);
    }
 

	public double calculatePercentThai(String input) {
		TokenizedOutput output = tokenizeString(input, false);

		return output.getKnown() * 1.0 / (output.getKnown() + output.getUnknown());
	}

	public static boolean isNumeric(String str) {
		return str.matches("-?\\d+(\\.\\d+)?"); // match a number with optional
												// '-' and decimal.
	}
}
