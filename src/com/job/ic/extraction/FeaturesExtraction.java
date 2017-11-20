/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.extraction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.daos.ResultDAO;
import com.job.ic.crawlers.daos.ResultDb;
import com.job.ic.crawlers.daos.SegmentGraphDAO;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.models.ResultModel;
import com.job.ic.crawlers.models.SegmentGraphModel;
import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.ml.classifiers.JapaneseTokenizer;
import com.job.ic.ml.classifiers.MyTextTokenizer;
import com.job.ic.ml.classifiers.TokenizedOutput;
import com.job.ic.nlp.services.LanguageIdentifier;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.Status;
import com.job.ic.utils.StringUtils;

import net.htmlparser.jericho.LoggerProvider;
import net.sf.javainetlocator.InetAddressLocator;

public class FeaturesExtraction extends Thread {

	private static Logger logger = Logger.getLogger(FeaturesExtraction.class);
	
	private static BufferedWriter bwThai;
	private static BufferedWriter bwNon;
	private static BufferedWriter bwInternal;

	private ResultModel sourceSeg;
	private SegmentGraphDAO sourceGraph;
	private HashMap<String, ResultModel> destResults;


	public static String[] domain = { "other", "ac", "ad", "ae", "aero", "af", "ag", "ai", "al", "am", "an", "ao", "aq", "ar", "arpa", "as", "asia", "at", "au", "aw", "ax", "az", "ba", "bb", "bd",
			"be", "bf", "bg", "bh", "bi", "biz", "bj", "bm", "bn", "bo", "br", "bs", "bt", "bv", "bw", "by", "bz", "ca", "cat", "cc", "cd", "cf", "cg", "ch", "ci", "ck", "cl", "cm", "cn", "co",
			"com", "coop", "cr", "cu", "cv", "cw", "cx", "cy", "cz", "de", "dj", "dk", "dm", "do", "dz", "ec", "edu", "ee", "eg", "er", "es", "et", "eu", "fi", "fj", "fk", "fm", "fo", "fr", "ga",
			"gb", "gd", "ge", "gf", "gg", "gh", "gi", "gl", "gm", "gn", "gov", "gp", "gq", "gr", "gs", "gt", "gu", "gw", "gy", "hk", "hm", "hn", "hr", "ht", "hu", "id", "ie", "il", "im", "in",
			"info", "int", "io", "iq", "ir", "is", "it", "je", "jm", "jo", "jobs", "jp", "ke", "kg", "kh", "ki", "km", "kn", "kp", "kr", "kw", "ky", "kz", "la", "lb", "lc", "li", "lk", "lr", "ls",
			"lt", "lu", "lv", "ly", "ma", "mc", "md", "me", "mg", "mh", "mil", "mk", "ml", "mm", "mn", "mo", "mobi", "mp", "mq", "mr", "ms", "mt", "mu", "museum", "mv", "mw", "mx", "my", "mz", "na",
			"name", "nc", "ne", "net", "nf", "ng", "ni", "nl", "no", "np", "nr", "nu", "nz", "om", "org", "pa", "pe", "pf", "pg", "ph", "pk", "pl", "pm", "pn", "post", "pr", "pro", "ps", "pt", "pw",
			"py", "qa", "re", "ro", "rs", "ru", "rw", "sa", "sb", "sc", "sd", "se", "sg", "sh", "si", "sj", "sk", "sl", "sm", "sn", "so", "sr", "st", "su", "sv", "sx", "sy", "sz", "tc", "td", "tel",
			"tf", "tg", "th", "tj", "tk", "tl", "tm", "tn", "to", "tp", "tr", "travel", "tt", "tv", "tw", "tz", "ua", "ug", "uk", "us", "uy", "uz", "va", "vc", "ve", "vg", "vi", "vn", "vu", "wf",
			"ws", "xxx", "ye", "yt", "za", "zm", "zw" };

	public static String[] country = { "other", "eu", "af", "ax", "al", "dz", "as", "ad", "ao", "ai", "aq", "ag", "ar", "am", "aw", "au", "at", "az", "bs", "bh", "bd", "bb", "by", "be", "bz", "bj",
			"bm", "bt", "bo", "bq", "ba", "bw", "bv", "br", "io", "bn", "bg", "bf", "bi", "kh", "cm", "ca", "cv", "ky", "cf", "td", "cl", "cn", "cx", "cc", "co", "km", "cg", "cd", "ck", "cr", "ci",
			"hr", "cu", "cw", "cy", "cz", "dk", "dj", "dm", "do", "ec", "eg", "sv", "gq", "er", "ee", "et", "fk", "fo", "fj", "fi", "fr", "gf", "pf", "tf", "ga", "gm", "ge", "de", "gh", "gi", "gr",
			"gl", "gd", "gp", "gu", "gt", "gg", "gn", "gw", "gy", "ht", "hm", "va", "hn", "hk", "hu", "is", "in", "id", "ir", "iq", "ie", "im", "il", "it", "jm", "jp", "je", "jo", "kz", "ke", "ki",
			"kp", "kr", "kw", "kg", "la", "lv", "lb", "ls", "lr", "ly", "li", "lt", "lu", "mo", "mk", "mg", "mw", "my", "mv", "ml", "mt", "mh", "mq", "mr", "mu", "yt", "mx", "fm", "md", "mc", "mn",
			"me", "ms", "ma", "mz", "mm", "na", "nr", "np", "nl", "nc", "nz", "ni", "ne", "ng", "nu", "nf", "mp", "no", "om", "pk", "pw", "ps", "pa", "pg", "py", "pe", "ph", "pn", "pl", "pt", "pr",
			"qa", "re", "ro", "ru", "rw", "bl", "sh", "kn", "lc", "mf", "pm", "vc", "ws", "sm", "st", "sa", "sn", "rs", "sc", "sl", "sg", "sx", "sk", "si", "sb", "so", "za", "gs", "ss", "es", "lk",
			"sd", "sr", "sj", "sz", "se", "ch", "sy", "tw", "tj", "tz", "th", "tl", "tg", "tk", "to", "tt", "tn", "tr", "tm", "tc", "tv", "ug", "ua", "ae", "gb", "us", "um", "uy", "uz", "vu", "ve",
			"vn", "vg", "vi", "wf", "eh", "ye", "zm", "zw", "internal" };

	public static String[] langs639_1 = { "lt", "lv", "mk", "ml", "mr", "ne", "no", "pa", "fa", "pl", "pt", "ro", "ru", "sk", "sl", "af", "sq", "ar", "bn", "bg", "zh", "hr", "cs", "da", "nl", "en",
			"et", "fi", "fr", "de", "el", "gu", "he", "hi", "hu", "id", "it", "ja", "kn", "ko", "so", "es", "sw", "sv", "ta", "te", "th", "tl", "tr", "uk", "ur", "vi" };
	public static String[] langs639_2t = { "lit", "lav", "mkd", "mal", "mar", "nep", "nor", "pan", "fas", "pol", "por", "ron", "rus", "slk", "slv", "afr", "sqi", "ara", "ben", "bul", "zho", "hrv",
			"ces", "dan", "nld", "eng", "est", "fin", "fra", "deu", "ell", "guj", "heb", "hin", "hun", "ind", "ita", "jpn", "kan", "kor", "som", "spa", "swa", "swe", "tam", "tel", "tha", "tgl",
			"tur", "ukr", "urd", "vie" };
	public static String[] langs639_2b = { "lit", "lav", "mac", "mal", "mar", "nep", "nor", "pan", "per", "pol", "por", "rum", "rus", "slo", "slv", "afr", "alb", "ara", "ben", "bul", "chi", "hrv",
			"cze", "dan", "dut", "eng", "est", "fin", "fre", "ger", "gre", "guj", "heb", "hin", "hun", "ind", "ita", "jpn", "kan", "kor", "som", "spa", "swa", "swe", "tam", "tel", "tha", "tgl",
			"tur", "ukr", "urd", "vie" };

	// public static String[] allLangs = {
	// "ab","aa","af","ak","sq","am","ar","an","hy","as","av","ae","ay","az","bm","ba","eu","be","bn","bh","bi","bs","br","bg","my","ca","ch","ce","ny","zh","cv","kw","co","cr","hr","cs","da","dv","nl","dz","en","eo","et","ee","fo","fj","fi","fr","ff","gl","ka","de","el","gn","gu","ht","ha","he","hz","hi","ho","hu","ia","id","ie","ga","ig","ik","io","is","it","iu","ja","jv","kl","kn","kr","ks","kk","km","ki","rw","ky","kv","kg","ko","ku","kj","la","","lb","lg","li","ln","lo","lt","lu","lv","gv","mk","mg","ms","ml","mt","mi","mr","mh","mn","na","nv","nd","ne","ng","nb","nn","no","ii","nr","oc","oj","cu","om","or","os","pa","pi","fa","pl","ps","pt","qu","rm","rn","ro","ru","sa","sc","sd","se","sm","sg","sr","gd","sn","si","sk","sl","so","st","es","su","sw","ss","sv","ta","te","tg","th","ti","bo","tk","tl","tn","to","tr","ts","tt","tw","ty","ug","uk","ur","uz","ve","vi","vo","wa","cy","wo","fy","xh","yi","yo","za","zu","abk","aar","afr","aka","sqi","amh","ara","arg","hye","asm","ava","ave","aym","aze","bam","bak","eus","bel","ben","bih","bis","bos","bre","bul","mya","cat","cha","che","nya","zho","chv","cor","cos","cre","hrv","ces","dan","div","nld","dzo","eng","epo","est","ewe","fao","fij","fin","fra","ful","glg","kat","deu","ell","grn","guj","hat","hau","heb","her","hin","hmo","hun","ina","ind","ile","gle","ibo","ipk","ido","isl","ita","iku","jpn","jav","kal","kan","kau","kas","kaz","khm","kik","kin","kir","kom","kon","kor","kur","kua","lat","","ltz","lug","lim","lin","lao","lit","lub","lav","glv","mkd","mlg","msa","mal","mlt","mri","mar","mah","mon","nau","nav","nde","nep","ndo","nob","nno","nor","iii","nbl","oci","oji","chu","orm","ori","oss","pan","pli","fas","pol","pus","por","que","roh","run","ron","rus","san","srd","snd","sme","smo","sag","srp","gla","sna","sin","slk","slv","som","sot","spa","sun","swa","ssw","swe","tam","tel","tgk","tha","tir","bod","tuk","tgl","tsn","ton","tur","tso","tat","twi","tah","uig","ukr","urd","uzb","ven","vie","vol","wln","cym","wol","fry","xho","yid","yor","zha","zul","abk","aar","afr","aka","alb","amh","ara","arg","arm","asm","ava","ave","aym","aze","bam","bak","baq","bel","ben","bih","bis","bos","bre","bul","bur","cat","cha","che","nya","chi","chv","cor","cos","cre","hrv","cze","dan","div","dut","dzo","eng","epo","est","ewe","fao","fij","fin","fre","ful","glg","geo","ger","gre","grn","guj","hat","hau","heb","her","hin","hmo","hun","ina","ind","ile","gle","ibo","ipk","ido","ice","ita","iku","jpn","jav","kal","kan","kau","kas","kaz","khm","kik","kin","kir","kom","kon","kor","kur","kua","lat","","ltz","lug","lim","lin","lao","lit","lub","lav","glv","mac","mlg","may","mal","mlt","mao","mar","mah","mon","nau","nav","nde","nep","ndo","nob","nno","nor","iii","nbl","oci","oji","chu","orm","ori","oss","pan","pli","per","pol","pus","por","que","roh","run","rum","rus","san","srd","snd","sme","smo","sag","srp","gla","sna","sin","slo","slv","som","sot","spa","sun","swa","ssw","swe","tam","tel","tgk","tha","tir","tib","tuk","tgl","tsn","ton","tur","tso","tat","twi","tah","uig","ukr","urd","uzb","ven","vie","vol","wln","wel","wol","fry","xho","yid","yor","zha","zul"};

	public static String[] blh = { "about.com", "lasvegashomesandcondos.com","europerealestatedirectory.com", "uscondex.com","dubailandhomes.co.uk", "panamabeachrealty.com", "airfrance.com", "cpanama.com", "condosutah.com", "turkish-property-world.com", "property2000.com,",
			"ukrealestatedirectory.com", "dubaipropertylistings.com", "condo.com", "x-rates.com", "internationalrealestatedirectory.com", "australiarealestatecentral.com", "condosutah.com",
			"florida-disney-villa-rentals.com", "villamartin-holiday-villas.co.uk", "ultra-properties.com", "dubailandhomes.co.uk", "ownerdirect.com", "holt-realty.com",
			"startpagina.nl", "penang.ws", "srilanka-hotels.ws", "visit-mekong.com", "phuket.com", "phiphi.phuket.com", "china-hotels.ws", "philippines-hotels.ws", "go-seychelles.com",
			"indonesia-holidays.com", "japan-hotels.ws", "asiawebdirect.com", "taiwan-hotels.net", "india-hotel.net", "borneo-hotels.com", "korea-hotels.net", "krabi-hotels.com", "langkawi-info.com",
			"bangkok.com", "khaolak-hotels.com", "singapore-guide.com", "huahin.bangkok.com", "hong-kong-hotels.ws", "kuala-lumpur.ws", "kosamui.com", "china-macau.com", "malacca.ws",
			"thailand-guide.com", "maldives-resorts.net", "hotels.com", "visit-malaysia.com", "chiangmai.bangkok.com", "bali-indonesia.com", "omniture.com", "koh-chang.bangkok.com" };

	public static HashSet<String> langSet = new HashSet<>(Arrays.asList(langs639_1));
	public static HashSet<String> blackListHost = new HashSet<>(Arrays.asList(blh));

	public static void main(String[] args) {

		net.htmlparser.jericho.Config.LoggerProvider = LoggerProvider.DISABLED;
		try {

//			CrawlerConfig.loadConfig("crawler.conf");
			String prefix = "diving";
			String base = "e:/db-train-" + prefix + "/";

			int end = 2;
			for (int i = 0; i < end; i++) {
				System.out.println("Extract " + i  + "\t" + (i+1) );
				extractFeatures(base + "db-" + i + "/", base + "db-" + (i + 1) + "/", prefix + "-" + i);
				cleanData(prefix + "-" + i + "-internal.arff", true);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	

	public static void cleanData(String filePath, boolean checkDup) {

		int cthai = 0;
		int cnon = 0;
		try (BufferedWriter bwT = FileUtils.getBufferedFileWriter(filePath + "-intThai.arff"); BufferedWriter bwN = FileUtils.getBufferedFileWriter(filePath + "-intNon.arff");) {
			int count = 0;
			boolean isData = false;
			String[] header = FileUtils.readResourceFile("/resources/classifiers/header-cf.txt");
			HashSet<String> data = new HashSet<>();
			for (String s : FileUtils.readFile(filePath)) {
				if (s.contains("@data")) {
					isData = true;
					for (String k : header) {
						bwT.write(k + "\n");
						bwN.write(k + "\n");
					}
					continue;
				}

				if (isData) {
					if (data.contains(s)) {
						if (s.trim().endsWith("non"))
							cthai++;
						else
							cnon++;

						count++;
						continue;
					}

					if (checkDup)
						data.add(s);

					if (s.trim().endsWith("non"))
						bwN.write(s + "\n");
					else
						bwT.write(s + "\n");

				}

			}

			System.out.println(count);
			System.out.println(cthai);
			System.out.println(cnon);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// FileUtils.deleteFile(filePath);
		FileUtils.rename(filePath + ".tmp", filePath);

	}

	public FeaturesExtraction(ResultModel sourceSeg, SegmentGraphDAO sourceGraph, HashMap<String, ResultModel> destResults) {
		this.sourceSeg = sourceSeg;
		this.sourceGraph = sourceGraph;
		this.destResults = destResults;
	}

	public void run() {
		try {
			extract();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void extractFeatures(String sourceDbPath, String destDbPath, String outputPath) throws IOException {
		Status.clear();

		
		bwThai = FileUtils.getBufferedFileWriter(outputPath + "-thai.arff");
		bwNon = FileUtils.getBufferedFileWriter(outputPath + "-non.arff");
		bwInternal = FileUtils.getBufferedFileWriter(outputPath + "-internal.arff");

		String header = getHeader();
		bwThai.write(header);
		bwNon.write(header);
		bwInternal.write(header);

		logger.info("loading destination results");
		ResultDb.createEnvironment(destDbPath);
		ResultDAO destDao = ResultDb.getResultDAO();

		long total = destDao.getSize();
		long curr = 0;
		HashMap<String, ResultModel> destResults = new HashMap<String, ResultModel>();
		HashMap<String, ResultModel> srcResults = new HashMap<String, ResultModel>();

		while (curr <= total) {
			ArrayList<ResultModel> arr = destDao.getRange(curr, total + 10000);

			for (ResultModel r : arr) {
				if (r != null)
					destResults.put(r.getSegmentName(), r);
			}
			logger.info(curr + "/" + total);
			curr += 10000;
		}
		logger.info(curr + "/" + total);
		logger.info("#dest seg :" + destResults.size());

		ResultDb.close();

		logger.info("loading source results");
		ResultDb.createEnvironment(sourceDbPath);

		ResultDAO srcDao = ResultDb.getResultDAO();

		total = srcDao.getSize();
		curr = 0;
		while (curr <= total) {
			ArrayList<ResultModel> arr = srcDao.getRange(curr, total + 10000);

			for (ResultModel r : arr) {
				if (r != null)
					srcResults.put(r.getSegmentName(), r);
			}
			logger.info(curr + "/" + total);
			curr += 10000;
		}
		logger.info(curr + "/" + total);
		logger.info("#source seg :" + srcResults.size());

		ExecutorService exe = Executors.newFixedThreadPool(8);
		Status.setTOTAL(srcResults.size());

		SegmentGraphDAO sdo = ResultDb.getSegmentGraphDAO();

		for (String s : srcResults.keySet()) {
			exe.submit(new FeaturesExtraction(srcResults.get(s), sdo, destResults));
		}

		exe.shutdown();

		while (!exe.isTerminated()) {
			try {
				Thread.sleep(15 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		bwInternal.close();
		bwThai.close();
		bwNon.close();
		ResultDb.close();
	}

	public void extractFeaturesFromSegment(WebsiteSegment source, WebsiteSegment dest) {

	}

	public void extract() {
		String output;
		int window = CrawlerConfig.getConfig().getWindowSize();

		// LanguageIdentifier lid = new LanguageIdentifier();

		String srcHost = HttpUtils.getHost(this.sourceSeg.getSegmentName());
		if (srcHost == null)
			return;

		ArrayList<SegmentGraphModel> dests = this.sourceGraph.getDests(this.sourceSeg.getSegmentName());

		if (dests == null)
			return;

		// src domain feature
		String srcDomain = HttpUtils.getDomain(this.sourceSeg.getSegmentName());
		if (srcDomain == null)
			srcDomain = "other";

		if (!Arrays.asList(domain).contains(srcDomain.toLowerCase()))
			srcDomain = "other";

		// srcCountry
		String srcCountry = null;
		srcCountry = this.sourceSeg.getIpCountry();

		if (srcCountry.equalsIgnoreCase("other")) {
			try {
				// System.out.println("check ip");
				srcCountry = InetAddressLocator.getLocale(srcHost).getCountry();
				// System.out.println(tmpIP);
			} catch (Exception e) {
				srcCountry = null;
			}
		}

		if (srcCountry == null || srcCountry.equals(""))
			srcCountry = "other";

		if (!Arrays.asList(country).contains(srcCountry.toLowerCase()))
			srcCountry = "other";

		for (SegmentGraphModel dest : dests) {

			output = "";
			String destSeg = dest.getDestSeg();
			String destHost = HttpUtils.getHost(destSeg);

			if (destSeg == null)
				continue;

			if (destHost == null)
				continue;

			if (!this.destResults.containsKey(destSeg))
				continue;

			ResultModel destResult = destResults.get(destSeg);

			// destSeg
			output += StringUtils.cleanUrlDataForPrediction(destSeg) + ",";

			// src degree feature
			output += ResultDAO.calcPercentRel(this.sourceSeg) + ",";

			// src avgRelScore feature
			output += this.sourceSeg.getAvgRelevantScore() + ",";

			output += srcDomain.toLowerCase() + "-";

			// dest domain feature
			String destDomain = HttpUtils.getDomain(destSeg);
			if (destDomain == null)
				destDomain = "other";

			if (!Arrays.asList(domain).contains(destDomain.toLowerCase()))
				destDomain = "other";

			output += destDomain.toLowerCase() + ",";

			// src geo feature
			output += srcCountry.toLowerCase() + "-";

			// dest geo feature
			String destCountry = destResult.getIpCountry();
			if (destCountry.equalsIgnoreCase("other")) {
				try {
					destCountry = InetAddressLocator.getLocale(destHost).getCountry();
				} catch (Exception e) {
					destCountry = null;
				}
			}
			if (destCountry == null || destCountry.equals(""))
				destCountry = "other";

			if (!Arrays.asList(country).contains(destCountry.toLowerCase()))
				destCountry = "other";

			output += destCountry.toLowerCase() + ",";

			// extract anchor and url
			ArrayList<LinksModel> ms = dest.getLinks();
			if (ms == null)
				continue;

			String anchor = "", url = "";
			int k;

			
			url = StringUtils.extractWordFromURL(destSeg, null) + " ";
			
			for (LinksModel m : ms) {
				if (m.getLinkUrl() == null)
					continue;

				String anc = m.getAnchorText();
				
				
				if (anc != null && anc.contains("-mikelinkstartmike-") && anc.contains("-mikelinkendmike-")) {
					k = anc.indexOf("-mikelinkstartmike-");
					if (k > window)
						anc = anc.substring(k - window);

					k = anc.lastIndexOf("-mikelinkendmike-");

					if (anc.length() - (k + "-mikelinkendmike-".length()) > window)
						anc = anc.substring(0, (k + "-mikelinkendmike-".length()) + window);

					anc = StringUtils.cleanAnchorText(anc);
					m.setAnchorText(anc);
				}
				
				url += StringUtils.extractWordFromURL(m.getLinkUrl(), destSeg) + " ";

				if (m.getAnchorText() != null)
					anchor += StringUtils.cleanAnchorText(m.getAnchorText()) + " ";
				
			}
			url = StringUtils.cleanTextFromUrl(url);
			anchor = StringUtils.cleanAnchorText(anchor);

			url = url.trim().toLowerCase();
			anchor = anchor.trim().toLowerCase();

			MyTextTokenizer t;
			if (CrawlerConfig.getConfig().getTargetLang().equals("en")) {
				t = new MyTextTokenizer();
			} else {
				t = new JapaneseTokenizer();
			}

			// thai word ratio features
			TokenizedOutput out = t.tokenizeString(anchor, true);
			if (out.getUnknown() + out.getKnown() != 0 && out != null)
				output += (out.getKnown() * 1.0 / (out.getKnown() + out.getUnknown())) + ",";
			else
				output += "0,";

			out = t.tokenizeString(url, true);
			if (out.getUnknown() + out.getKnown() != 0 && out != null)
				output += (out.getKnown() * 1.0 / (out.getKnown() + out.getUnknown())) + ",";
			else
				output += "0,";

			// langauge of anchor text feature
			String lang = "en";

			if (anchor != null && anchor.trim().length() > 10) {
				lang = LanguageIdentifier.identifyLanguage(anchor);
			}

			if (!langSet.contains(lang))
				lang = "other";

			output += lang + ",";

			// language keyword (from destSeg url) feature
			HashMap<String, Integer> langMap = new HashMap<String, Integer>();
			String maxKey = "other";

			int maxKeyCount = 0;
			for (String s : out.getTokenized()) {
				s = s.trim().toLowerCase();
				s = StringUtils.normalizeWordFromUrl(s);

				if (langSet.contains(s)) {

					// change lang to 639_1
					if (s.length() != 2) {
						int l = Integer.max(Arrays.asList(langs639_2b).indexOf(s), Arrays.asList(langs639_2t).indexOf(s));
						if (l >= 0)
							s = langs639_1[l];
						else
							s = "other";
					}

					if (langMap.containsKey(s)) {
						langMap.put(s, langMap.get(s) + 1);
					} else {
						langMap.put(s, 1);
					}
				}
			}

			for (String s : langMap.keySet()) {
				if (langMap.get(s) > maxKeyCount) {
					maxKeyCount = langMap.get(s);
					maxKey = s;
				}
			}

			output += maxKey + ",";

			// anchor and url feature

			output += "'" + anchor + "','" + url + "',";
			output = output.replaceAll("\\r\\n|\\r|\\n", "");

			boolean isThai = false;
			boolean sameHost = false;

			if (destHost.equals(srcHost))
				sameHost = true;

			if (ResultDAO.calcPercentRel(destResult) > CrawlerConfig.getConfig().getRelevanceDegreeThreshold())
				isThai = true;

			if (blackListHost.contains(destHost.replace("www.", "")))
				isThai = false;

			if (isThai) {
				output += "THAI";

				if (sameHost) {
					FeaturesExtraction.writeInternal(output);
				} else {
					FeaturesExtraction.writeThai(output);
				}

			} else {
				output += "NON";
				if (sameHost) {
					FeaturesExtraction.writeInternal(output);
				} else {
					FeaturesExtraction.writeNon(output);
				}

			}

		}
		Status.SUCCESS();
		logger.info(Status.progressReport());
	}

	public static synchronized void writeInternal(String input) {
		try {
			bwInternal.write(input.toLowerCase() + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static synchronized void writeNon(String input) {
		try {
			bwNon.write(input.toLowerCase() + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static synchronized void writeThai(String input) {
		try {
			bwThai.write(input.toLowerCase() + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	public static synchronized double calcPercentThai(ResultModel m) {
//		int thai = m.getRelevantPage();
//		int non = m.getIrrelevantPage();
//		int total = thai + non;
//
//		return (1.0 * thai) / total;
//	}

	public static String getHeader() {
		String output = "";

		for (String s : FileUtils.readResourceFile("/resources/classifiers/header-cf.txt")) {
			output += s + "\n";
		}

		return output;
	}

	public static String buildHeaderString() {

		String header = "";
		// build header

		header = "@relation 'crawler'\n" + "@attribute segname string\n" + "@attribute src-degreetTh numeric\n" + "@attribute src-RelScore numeric\n" + "@attribute domain";

		System.out.println("domain");
		for (int i = 0; i < domain.length; i++) {
			if (i == 0)
				header = header + "{";

			for (int j = 0; j < domain.length; j++) {
				header += domain[i] + "-" + domain[j];

				if (!(i == j && i == domain.length - 1))
					header += ",";
				else
					header += "}\n";

			}
		}

		System.out.println("ccc");
		header += "@attribute country";

		for (int i = 0; i < country.length; i++) {
			if (i == 0)
				header = header + "{";

			for (int j = 0; j < country.length; j++) {
				header += country[i] + "-" + country[j];

				if (!(i == j && i == country.length - 1))
					header += ",";
				else
					header += "}\n";

			}

		}

		System.out.println("lll");
		header += "@attribute anchor-thai-words-ratio numeric\n@attribute url-thai-words-ratio numeric\n@attribute anchorLang{";

		// language from anchor & url
		String la = "";
		for (int i = 0; i < langs639_1.length; i++) {
			if (i != langs639_1.length - 1)
				la += langs639_1[i] + ",";
			else
				la += langs639_1[i] + ",other";
		}

		header += la + "}\n";
		header += "@attribute urlLang{" + la + "}\n";

		header += "@attribute anchor string\n" + "@attribute url string\n" + "@attribute class {thai,non}\n\n@data\n";

		return header;
	}

	
	
}
