/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.nlp.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.nlp.LexTo;
import com.job.ic.utils.StringUtils;


public class LangChecker extends Checker {

	/**
	 * the list of Thai words in the Lexitron dictionary
	 */
	private static Set<String> lexitron;
	private LexTo lexto;

	/**
	 * Read the lexitron file and add the Thai words to the lexitron property
	 */
	public LangChecker() {
		try {
			createEnvironment();
			this.lexto = new LexTo(LangChecker.lexitron);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void createEnvironment() {
		if (LangChecker.lexitron != null)
			return;

		LangChecker.lexitron = new HashSet<String>();
		try {
			InputStream fis = LangChecker.class.getResourceAsStream("/resources/classifiers/lexitron.txt");// new
																						// FileInputStream("lexitronutf.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line;
			while ((line = br.readLine()) != null) {
				LangChecker.lexitron.add(line);
			}

			br.close();
			fis.close();
		} catch (IOException e) {
		}
	}

	

	/**
	 * Calculate the Thai language score
	 * 
	 * @param content
	 *            is the content of the web page without HTML tag and symbols
	 * @param lexto
	 *            is an analysis tool for calculate Thai language score from content
	 * @return the Thai language score
	 */
	public float checkStringContent(String content) {
		return lexto.match(content);
	}

	public float checkHtmlContent(byte[] b) {
		float lang = 0;
		List<Element> te;
		String content = "";
		try {
			Source sc = new Source(new String(b, "utf-8"));
			sc.setLogger(null);
			
			te = sc.getAllElements(HTMLElementName.HTML);
			
			for (int i = 0; i < te.size(); i++) {
				content += te.get(i).getTextExtractor().toString() + "\n";
			}
			
			content = StringUtils.removeSymbols(content);

			lang = checkStringContent(content);
			content = null;
			te.clear();
			te = null;
			
			if (!sc.getEncoding().toLowerCase().equals("utf-8")) {
				sc = new Source(new String(b, "tis-620"));
				sc.setLogger(null);
				
				te = sc.getAllElements(HTMLElementName.HTML);
				
				for (int i = 0; i < te.size(); i++) {
					content += te.get(i).getTextExtractor().toString() + "\n";
				}
				
				content = StringUtils.removeSymbols(content);

				lang = Math.max(lang, checkStringContent(content));
				te.clear();
				te = null;			
			}
			content = null;
		} catch (Exception e) {
			
		}
		return lang;
	}


	@Override
	public void finalize() {
		// TODO Auto-generated method stub
		this.lexto = null;
	}


}
