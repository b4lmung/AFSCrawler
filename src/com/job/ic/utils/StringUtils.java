/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang.StringEscapeUtils;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.ml.classifiers.JapaneseTokenizer;
import com.job.ic.ml.classifiers.MyTextTokenizer;

import info.debatty.java.stringsimilarity.Cosine;
import net.htmlparser.jericho.Source;
import weka.core.tokenizers.WordTokenizer;

public class StringUtils {
	public static final String[] stopwords = { "my", "is", "am", "are", "'s", "was", "were", "'re", "'d", "would", "could", "a", "able", "about", "across", "after", "all", "almost", "also", "am",
			"among", "an", "and", "any", "are", "as", "at", "be", "because", "been", "but", "by", "can", "cannot", "could", "dear", "did", "do", "does", "either", "else", "ever", "every", "for",
			"from", "get", "got", "had", "has", "have", "he", "her", "hers", "him", "his", "how", "however", "i", "if", "in", "into", "is", "it", "its", "just", "least", "let", "like", "likely",
			"may", "me", "might", "most", "must", "my", "neither", "no", "nor", "not", "of", "off", "often", "on", "only", "or", "other", "our", "own", "rather", "said", "say", "says", "she",
			"should", "since", "so", "some", "than", "that", "the", "their", "them", "then", "there", "these", "they", "this", "tis", "to", "too", "twas", "us", "wants", "was", "we", "were", "what",
			"when", "where", "which", "while", "who", "whom", "why", "will", "with", "would", "yet", "you", "your" };
	private static HashSet<String> stop = new HashSet<String>(Arrays.asList(stopwords));

	public static WordTokenizer tokenizer;

	static {
		if (CrawlerConfig.getConfig().getTargetLang().equals("en"))
			tokenizer = new MyTextTokenizer();
		else if (CrawlerConfig.getConfig().getTargetLang().equals("ja"))
			tokenizer = new JapaneseTokenizer();
	}

	public static String removeSymbols(String text) {
		String planText = text.replaceAll("[?,.%$#@!*()<>&-[0-9]:+;{}/\\\\=\"'|_]+", " ").replaceAll("[\\s][\\s]+", " ").replace("•", "").replace("☎", "").replace("[", "").replace("]", "");
		return planText;
	}

	public static String removeNonEngCharacter(String text) {

		if (CrawlerConfig.getConfig() != null && CrawlerConfig.getConfig().getTargetLang().equals("en")) {
			String enText = text.replaceAll("[^a-zA-Z]", " ");
			return enText.replaceAll("( )+ |(\t)+", " ");
		} else {
			return text;
		}

	}

	// not thread safe
	public static String getUniqueWord(String input) {

		synchronized (tokenizer) {

			input = input.toLowerCase();

			HashSet<String> words = new HashSet<>();
			String output = "";

			tokenizer.tokenize(input);
			while (tokenizer.hasMoreElements()) {
				String s = tokenizer.nextElement();
				if (s.trim().equals(""))
					continue;

				if (words.contains(s))
					continue;

				output += s + " ";

				words.add(s.toLowerCase());
			}

			words.clear();

			return output;
		}

	}

	public static String getUniqueWordFromURL(String input) {

		if (input.contains("&"))
			input = input.substring(0, input.indexOf("&"));

		input = input.replace("http://", "").replace("https://", "").replace("www.", " ").trim();

		input = input.toLowerCase();

		String output = "";

		for (String s : input.replace("'", "").trim().split("\\.|/")) {
			if (s.trim().equals(""))
				continue;
			output += s + " ";
		}

		return output;
	}

	public static String cleanUrlDataForPrediction(String input) {

		if (input.contains("%"))
			input = HttpUtils.decodeURL(input);

		input =StringUtils.removeSpaces(input);
		return input.replace("%", "").replace(" ", "").replace(",", "").replace("\\", "").replace("\"", "").replace("'", "").replace("`", "").replace("{", "").replace("}", "");
	}

	public static boolean isDuplicate(HashMap<String, ArrayList<String>> dup, String destSeg, String[] features, String anchor) {
		Cosine c = new Cosine();
		destSeg = destSeg.replace("'", "");
		String destHost = HttpUtils.getHost(destSeg);
		if (destHost != null)
			destHost = destHost.replace("https://", "").replace("http://", "").replace("www.", "");
		
		anchor = getUniqueWord(anchor);
		anchor = StringUtils.removeSymbols(anchor);
		
		features[10] = ""; // anchor
		features[11] = ""; // url
		
		for (String s : dup.keySet()) {
			for (String d : dup.get(s)) {
				if (s.equals(destSeg) && d.equals(anchor))
					return true;
				
				String dupHost = HttpUtils.getHost(s);
				
				if (dupHost != null)
					dupHost = dupHost.replace("https://", "").replace("http://", "").replace("www.", "");

				if (destHost != null && dupHost != null) {
					// if hostname are similar and the set of term is equal or similar
					if ((destHost.equals(dupHost) || c.similarity(s, destSeg) > 0.8) && (anchor.equals(d) || c.similarity(anchor, d) > 0.8)) {
						return true;
					}
				}
			}
		}
		
		
		if (!dup.containsKey(destSeg))
			dup.put(destSeg, new ArrayList<>());
		
		
		dup.get(destSeg).add(anchor);
		return false;
	
	}

	/*
	 * public static boolean isDuplicate(HashMap<String, String> dup, String
	 * destSeg, String[] features, String anchor) { // if(true) // return false;
	 * Cosine c = new Cosine(); String destHost = HttpUtils.getHost(destSeg); if
	 * (destHost != null) destHost = destHost.replace("https://",
	 * "").replace("http://", "").replace("www.", ""); anchor =
	 * getUniqueWord(anchor); anchor = StringUtils.removeSymbols(anchor);
	 * features[10] = ""; // anchor features[11] = ""; // url // if
	 * (anchor.trim().equals("")) { // for (String s : dupFeatures.keySet()) {
	 * // String[] d = dupFeatures.get(s); // if (Arrays.equals(features, d)) //
	 * return true; // } // dupFeatures.put(destSeg, features); // return false;
	 * // } else { for (String s : dup.keySet()) { String d = dup.get(s); // if
	 * dest and set of terms is equal if (s.equals(destSeg) && d.equals(anchor))
	 * return true; String dupHost = HttpUtils.getHost(s); if (dupHost != null)
	 * dupHost = dupHost.replace("https://", "").replace("http://",
	 * "").replace("www.", ""); if (destHost != null && dupHost != null) { // if
	 * hostname are similar and the set of term is equal if
	 * ((destHost.equals(dupHost) || c.similarity(s, destSeg) > 0.5) &&
	 * features.equals(d)) return true; } } dup.put(destSeg, anchor); return
	 * false; // } }
	 */

	/*
	 * public static String hashSegmentFeatures(String features){ String newText
	 * = ""; newText += tmp[0]; //destination segment newText +=
	 * (int)(Double.parseDouble(tmp[1])*10)+ ","; //srcRelDegree newText +=
	 * (int)(Double.parseDouble(tmp[2])*10)+ ","; //srcScore newText += tmp[3]+
	 * ","; //domain newText += tmp[4]+ ","; // domain newText +=
	 * (int)(Double.parseDouble(tmp[5])*10)+ ","; //relevant keyword from anchor
	 * newText += (int)(Double.parseDouble(tmp[6])*10)+ ","; // rel keyword from
	 * url newText += Integer.parseInt(tmp[9])+ ","; //depth //anchor String
	 * anchor = getUniqueWord(tmp[10]); //url not used since there might be the
	 * case that there are too many url cite to destination with the same anchor
	 * text features for(String s: tmp[11].replace("'", "").trim().split(" ")){
	 * words.add(s.toLowerCase()); } String url = ""; for(String s: words) url
	 * += s + " "; newText += anchor.trim()+ ","; return
	 * md5(newText.toLowerCase()); }
	 */

	public static String removeHtmlTag(String text) {
		Source sc = new Source(text);
		return sc.getTextExtractor().toString();
		// return text.replaceAll("\\<.*?>", "");
	}

	public static String md5(String str) {
		String md5Result = null;
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.reset();
			md5.update(str.getBytes());
			md5Result = new BigInteger(1, md5.digest()).toString(16);
		} catch (NoSuchAlgorithmException localNoSuchAlgorithmException) {
		}
		return md5Result;
	}

	public static ArrayList<String> wordBreaking(String test) {
		test = test.replace("'s", " ");
		test = test.replace("'d", " ");
		test = test.replace("' s", " ");
		test = test.replace("' d", " ");

		ArrayList<String> output = new ArrayList<String>();
		test = removeSymbols(test);
		String tmp;
		StringTokenizer s = new StringTokenizer(test);
		while (s.hasMoreTokens()) {
			tmp = s.nextToken();
			if (!stop.contains(tmp))
				output.add(tmp);
		}

		return output;
	}

	public static String removeSpaces(String text) {
		if (text == null)
			return null;
		return text.replaceAll("\\s+", " ").replaceAll("\\n+", " ").replaceAll("\\t+", " ").trim();
	}

	public static String findCommonBasePath(String[] sortedBasePaths, String target) {
		for (int i = sortedBasePaths.length; i > 0; i--) {

			if (sortedBasePaths[i].length() > target.length())
				continue;

			if (target.startsWith(sortedBasePaths[i])) {
				return sortedBasePaths[i];
			}
		}

		return null;
	}

	public static String removeNoise(String str) {
		if (str == null || str.trim().length() == 0)
			return null;
		str = removeSpaces(str);
		String[] tmp = str.split(" ");
		String output = "";
		for (int i = 0; i < tmp.length; i++) {
			if (i == 1 || i == tmp.length - 1) {
				if (tmp[i].length() > 2)
					output += tmp[i] + " ";
			} else {
				output += tmp[i] + " ";
			}
		}

		return output;
	}

	public static int countWordInStr(String inputString, String targetWrd) {
		return org.apache.commons.lang.StringUtils.countMatches(inputString, targetWrd);
	}

	public static String cleanAnchorText(String anchor) {
		anchor = removeHtmlTag(anchor);
		anchor = anchor.replace("nbsp", "").replace("href", "");
		anchor = anchor.replace("-mikelinkstartmike", " ");
		anchor = anchor.replace("-mikelinkendmike", " ");
		anchor = StringEscapeUtils.unescapeHtml(anchor);
		anchor = removeSymbols(removeSpaces(anchor));

		if (anchor == null || anchor.equals("null"))
			anchor = "";

		return anchor;
	}

	public static String cleanTextFromUrl(String url) {

		url = removeSymbols(removeSpaces(url));

		if (url == null || url.equals("null"))
			url = "";

		return url;
	}

	public static String normalizeWordFromUrl(String input) {
		if (input.equals("zh-tw"))
			input = "cn";

		if (input.equals("zh-cn"))
			input = "cn";

		if (input.equals("zh"))
			input = "cn";

		if (input.equals("zho"))
			input = "cn";

		if (input.equals("afr"))
			input = "af";

		if (input.equals("afrikaans"))
			input = "af";

		if (input.equals("ara"))
			input = "ar";

		if (input.equals("arabic"))
			input = "ar";

		if (input.equals("bengali"))
			input = "bn";

		if (input.equals("ben"))
			input = "bn";

		if (input.equals("czech"))
			input = "cs";

		if (input.equals("ces"))
			input = "cs";

		if (input.equals("danish"))
			input = "da";

		if (input.equals("dan"))
			input = "da";

		if (input.equals("german"))
			input = "de";

		if (input.equals("deu"))
			input = "de";

		if (input.equals("ell"))
			input = "el";

		if (input.equals("greek"))
			input = "el";

		// new lang//
		if (input.equals("english"))
			input = "en";

		if (input.equals("eng"))
			input = "en";

		// new lang//
		if (input.equals("spanish"))
			input = "es";

		if (input.equals("spa"))
			input = "es";

		// new lang//
		if (input.equals("estonian"))
			input = "et";

		if (input.equals("est"))
			input = "et";

		// new lang//
		if (input.equals("persian"))
			input = "fa";

		if (input.equals("fas"))
			input = "fa";

		// new lang//
		if (input.equals("finish"))
			input = "fi";

		if (input.equals("fin"))
			input = "fi";

		// new lang//
		if (input.equals("french"))
			input = "fr";

		if (input.equals("french"))
			input = "fra";

		// new lang//
		if (input.equals("gujarati"))
			input = "gu";

		if (input.equals("gujarati"))
			input = "guj";

		// new lang//
		if (input.equals("hebrew"))
			input = "he";

		if (input.equals("hebrew"))
			input = "heb";

		// new lang//
		if (input.equals("hindi"))
			input = "hi";

		if (input.equals("hin"))
			input = "hi";

		// new lang//
		if (input.equals("croatian"))
			input = "hr";

		if (input.equals("hrv"))
			input = "hr";

		// new lang//
		if (input.equals("hungarian"))
			input = "hu";

		if (input.equals("hun"))
			input = "hu";

		// new lang//
		if (input.equals("indonesian"))
			input = "id";

		if (input.equals("ind"))
			input = "id";

		// new lang//
		if (input.equals("italian"))
			input = "it";

		if (input.equals("ita"))
			input = "it";

		// new lang//
		if (input.equals("japanese"))
			input = "ja";

		if (input.equals("jpn"))
			input = "ja";

		// new lang//
		if (input.equals("kannada"))
			input = "kn";

		if (input.equals("kan"))
			input = "kn";

		// new lang//
		if (input.equals("korean"))
			input = "ko";

		if (input.equals("kor"))
			input = "ko";

		// new lang//
		if (input.equals("lithuanian"))
			input = "lt";

		if (input.equals("lit"))
			input = "lt";

		// new lang//
		if (input.equals("latvian"))
			input = "lv";

		if (input.equals("lav"))
			input = "lv";

		// new lang//
		if (input.equals("macedonian"))
			input = "mk";

		if (input.equals("mkd"))
			input = "mk";

		// new lang//
		if (input.equals("malayalam"))
			input = "ml";

		if (input.equals("malayalam"))
			input = "mal";

		// new lang//
		if (input.equals("marathi"))
			input = "mr";

		if (input.equals("marathi"))
			input = "mar";

		// new lang//
		if (input.equals("nepali"))
			input = "ne";

		if (input.equals("nep"))
			input = "ne";

		// new lang//
		if (input.equals("dutch"))
			input = "nl";

		if (input.equals("nld"))
			input = "nl";

		// new lang//
		if (input.equals("norwegian"))
			input = "nn";

		if (input.equals("nno"))
			input = "nn";

		if (input.equals("nb"))
			input = "nn";

		if (input.equals("nob"))
			input = "nn";

		// new lang//
		if (input.equals("polish"))
			input = "pl";

		if (input.equals("pol"))
			input = "pl";

		// new lang//
		if (input.equals("portuguese"))
			input = "pt";

		if (input.equals("por"))
			input = "pt";

		// new lang//
		if (input.equals("romanian"))
			input = "ro";

		if (input.equals("ron"))
			input = "ro";

		// new lang//
		if (input.equals("russian"))
			input = "ru";

		if (input.equals("rus"))
			input = "ru";

		// new lang//
		if (input.equals("slovak"))
			input = "sk";

		if (input.equals("slk"))
			input = "sk";

		// new lang//
		if (input.equals("slovenian"))
			input = "sl";

		if (input.equals("slovene"))
			input = "sl";

		// new lang//
		if (input.equals("somali"))
			input = "so";

		if (input.equals("som"))
			input = "so";

		// new lang//
		if (input.equals("albanian"))
			input = "sq";

		if (input.equals("sqi"))
			input = "sq";

		// new lang//
		if (input.equals("swedish"))
			input = "sv";

		if (input.equals("swe"))
			input = "sv";

		// new lang//
		if (input.equals("tamil"))
			input = "ta";

		if (input.equals("tam"))
			input = "ta";

		// new lang//
		if (input.equals("telugu"))
			input = "te";

		if (input.equals("tel"))
			input = "te";

		// new lang//
		if (input.equals("thai"))
			input = "th";

		if (input.equals("tha"))
			input = "th";

		//
		// new lang//
		if (input.equals("tagalog"))
			input = "tl";

		if (input.equals("tgl"))
			input = "tl";

		// new lang//
		if (input.equals("turkish"))
			input = "tr";

		if (input.equals("tur"))
			input = "tr";

		// new lang//
		if (input.equals("ukrainian"))
			input = "uk";

		if (input.equals("ukr"))
			input = "uk";

		// new lang//
		if (input.equals("urdu"))
			input = "ur";

		if (input.equals("urd"))
			input = "ur";

		// new lang//
		if (input.equals("vietnamese"))
			input = "vi";

		if (input.equals("vie"))
			input = "vi";

		return input;
	}

	public static String extractWordFromURL(String inputUrl, String excludeString) {
		if (inputUrl == null)
			return "";

		inputUrl = inputUrl.toLowerCase();

		if (inputUrl.contains("-mikequotemike-"))
			inputUrl = inputUrl.replace("-mikequotemike-", "'");

		if (excludeString != null && excludeString.contains("-mikequotemike-"))
			excludeString = excludeString.replace("-mikequotemike-", "'");

		if (inputUrl.contains("-mikespacemike-"))
			inputUrl = inputUrl.replace("-mikespacemike-", " ");

		if (excludeString != null && excludeString.contains("-mikespacemike-"))
			excludeString = excludeString.replace("-mikespacemike-", " ");

		URLCodec codec = new URLCodec("UTF-8");
		try {
			inputUrl = codec.decode(inputUrl);
		} catch (DecoderException e1) {
		}

		if (excludeString != null)
			try {
				excludeString = codec.decode(excludeString);
				excludeString = excludeString.toLowerCase();
			} catch (DecoderException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		inputUrl = inputUrl.toLowerCase();

		String ext = HttpUtils.getExtension(inputUrl);
		if (excludeString != null) {
			try {
				inputUrl = inputUrl.substring(inputUrl.indexOf(excludeString) + excludeString.length());
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(inputUrl + "\t" + excludeString);
			}
		} else {
			String hostname = HttpUtils.getHost(inputUrl);
			if (hostname != null)
				inputUrl = inputUrl.replace(hostname, "");
		}

		inputUrl = StringEscapeUtils.unescapeHtml(inputUrl);

		if (inputUrl == null || inputUrl.trim().length() == 0)
			return "";

		if (ext != null)
			inputUrl = inputUrl.replace("." + ext, " ");

		inputUrl = inputUrl.replace("http://www", " ").replace("http://", " ").replace("https://", " ");
		inputUrl = removeSymbols(inputUrl);

		String[] rep = { "index", "default", "main", "action" };
		for (String s : rep)
			inputUrl = inputUrl.replace(s, " ");

		inputUrl = removeSpaces(inputUrl).trim();

		return cleanTextFromUrl(inputUrl);
	}
}
