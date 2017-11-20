package com.job.ic.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.log4j.Logger;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.ml.classifiers.ClassifierOutput;
import com.job.ic.ml.classifiers.ConfusionMatrixObj;
import com.job.ic.ml.classifiers.NeighborhoodPredictor;
import com.job.ic.ml.classifiers.PredictorPool;
import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.ml.classifiers.WekaClassifier;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;

public class ParseLog {

	/**
	 * @param args
	 * @throws IOException
	 */

	private static Logger logger = Logger.getLogger(ParseLog.class);
	public static HashMap<String, Integer> limitSite = new HashMap<>();
	public static ArrayList<String> siteOrder = new ArrayList<>();

	public static void main(String[] args) throws Exception {


		String p = "C:/Users/b4lmung/Desktop/slope/logs-real-diving";

		ArrayList<Double> real = parseSegmentCrawlerHV(p, 20000, 1000);
		ArrayList<Double> ratio = parseSegmentCrawlerHV(p.replace("real", "ratio"), 20000, 1000);
		ArrayList<Double> multi = parseSegmentCrawlerHV(p.replace("real", "multi"), 20000, 1000);
		ArrayList<Double> best = parseSegmentCrawlerHV(p.replace("real", "best"), 20000, 1000);

		printHV(1000, true, real, ratio, multi, best);

		// System.out.println("-----------------");

		// printHV(100, false, parseSegmentCrawlerHV(p.replace("single",
		// "dup").replace("normal", "page"), 20000, 100));

		System.exit(0);

		String path = "2017/logs-real-diving";
		String path2 = "2017/logs-site-diving/ic.log";

		// parseClassifierCorreation("estate.txt");
		// parseClassifierCorreation("diving.txt");

		System.out.println("->>> Segment");
		parseSegmentCrawlerHV(path, 20000, 100);

		System.out.println("->>> Site");
		parseSiteCrawler(path2);

		HashSet<String> site = new HashSet<>(Arrays.asList(FileUtils.readFile("site.txt")));
		HashSet<String> seg = new HashSet<>(Arrays.asList(FileUtils.readFile("site-segs.txt")));

		for (String s : seg) {
			if (!site.contains(s)) {
				System.out.println("http://" + s + "\n==");
			}
		}
		System.exit(0);

		// softlog("C:/Users/b4lmung/Google
		// Drive/Results/Diving-new/diving-results-new/logs-multi-diving-new",
		// "FocusedCrawler:216", -1, 100 );
		// ArrayList<Double> d1 = mylog("C:/Users/b4lmung/Google
		// Drive/Results/Internet
		// Results/results-d1/new-diving/logs-multi-diving-1", 10000, 100);
		// ArrayList<Double> d2 = mylog("logs-multi-diving-3", 10000, 100);

		// printMyLog(100, d1, d2);

		String base = "C:/Users/Tanaphol/Documents/Research drive/Results/Internet Results/results-d1/new-diving/";
		base = "C:/Users/b4lmung/Desktop/new-test/";
		base = "diving/";
		// C:\Users\b4lmung\Google Drive\Results\Internet Results\diving\20k
		printHV(1000, parseSegmentCrawlerHV(base + "logs-real-estate", 20000, 1000));
		// printHV(1000, softlog(base + "logs-best-diving",
		// "FocusedCrawler:236", 30000, 1000));

	}

	public static void parseSiteCrawler(String path) {
		HashMap<String, Integer> countSite = new HashMap<>();
		HashMap<String, Integer> relSite = new HashMap<>();
		HashMap<String, Integer> nonSite = new HashMap<>();

		int rel = 0;
		for (String s : FileUtils.readFile(path)) {
			if (!s.contains("DOWNLOADED"))
				continue;

			s = s.substring(s.indexOf("DOWNLOADED") + "DOWNLOADED".length());
			s = s.trim();
			String[] tmp = s.split("\t");

			String site = HttpUtils.getHost(tmp[1]);
			// System.err.println(limitSite.containsKey("mobi.thaiproperty.com"));
			if (!limitSite.containsKey(site))
				continue;

			if (site == null)
				continue;

			double score = Double.parseDouble(tmp[0]);

			if (!countSite.containsKey(site)) {
				countSite.put(site, 0);
			}

			// TODO: limit number of downloaded
			// System.out.println(site + "\t" + limitSite.containsKey(site));
			if (countSite.get(site) > limitSite.get(site))
				continue;

			countSite.put(site, countSite.get(site) + 1);

			if (!relSite.containsKey(site)) {
				relSite.put(site, 0);
				nonSite.put(site, 0);
			}

			if (score > 0.5) {
				if (countSite.get(site) < limitSite.get(site))
					rel++;

				relSite.put(site, relSite.get(site) + 1);
			} else {
				nonSite.put(site, nonSite.get(site) + 1);
			}
		}

		System.out.println("----------------In case that the number of visiting each site is equal ---------------------------");

		System.out.println(countSite.size());
		FileUtils.writeTextFile("site.txt", countSite.keySet(), false);
		System.out.println("relevant pages: " + rel);
		System.out.println("relevant pages per site: " + ((1.0 * rel) / countSite.keySet().size()));

		System.out.println("----------------In case that the number of downloaded is equal ---------------------------");

		int r = 0, n = 0, total = 0;
		for (String s : siteOrder) {
			if (relSite.containsKey(s)) {
				r += relSite.get(s);
				n += nonSite.get(s);
				total += relSite.get(s) + nonSite.get(s);
			}

			if (total > 20000)
				break;
		}
		System.out.println("relevant pages per site: " + ((1.0 * r) / siteOrder.size()));

		System.out.printf("rel:\t%d\tnon:\t%d\thv:\t%.3f\t", r, n, (1.0 * r) / (r + n));
	}

	public static ArrayList<Double> parseSegmentCrawlerHV(String input, int limit, int unit) throws IOException {
		// TODO Auto-generated method stub

		int thai = 0;
		int non = 0;
		double score;
		String[] tmp;

		HashMap<String, Integer> irrelevantPerSite = new HashMap<>();
		HashMap<String, Integer> tPerSite = new HashMap<>();

		HashSet<String> urldb = new HashSet<>();

		HashMap<String, Integer> relSiteCount = new HashMap<>();

		File[] f = new File(input).listFiles();

		List<File> tmpList = Stream.of(f).filter(i -> i.getName().contains("ic.log")).collect(Collectors.toList());

		f = tmpList.toArray(new File[tmpList.size()]);

		Arrays.sort(f, logFilesComparator);

		ArrayList<Double> hvout = new ArrayList<>();

		int bb = 0;

		ArrayList<Double> segs = new ArrayList<>();

		String start = null, end = null;
		for (int i = 0; i < f.length; i++) {

			if (!f[i].getName().contains(".log"))
				continue;

			String[] lines = FileUtils.readFile(f[i].getPath());
			String iden = "DOWNLOADED";
			double r = 0, n = 0;
			for (String s : lines) {

				if (start == null) {
					start = s.substring(0, s.indexOf("["));
				}

				if (thai + non >= limit) {
					end = s;
					System.err.println(s);
					end = end.substring(0, end.indexOf("["));
					break;
				}

				if (s.toLowerCase().contains("HttpSegmentCrawler:296- Finished downloading")) {
					segs.add(r / n);
					r = 0;
					n = 0;
				}

				if (!s.contains(iden)) {
					continue;
				}

				s = s.substring(s.indexOf(iden) + iden.length());
				s = s.trim();
				tmp = s.split("\t");

				if (urldb.contains(HttpUtils.getStaticUrl(tmp[3], true).toLowerCase())) {
					System.err.println(">>" + tmp[3]);
					continue;
				}

				score = Double.parseDouble(tmp[0]);

				String site = HttpUtils.getHost(tmp[3]);

				if (tmp[3].indexOf("bohol") > 0) {
					score = 0;
				}

				if (urldb.contains(HttpUtils.getStaticUrl(tmp[1]).toLowerCase()))
					continue;

				urldb.add(HttpUtils.getStaticUrl(tmp[3], true).toLowerCase());

				if (score > 0.5) {
					r++;
					thai++;

					if (!limitSite.containsKey(site)) {
						siteOrder.add(site);
						limitSite.put(site, 1);
					} else {
						limitSite.put(site, limitSite.get(site) + 1);
					}

					if (!relSiteCount.containsKey(site)) {
						relSiteCount.put(site, 1);
					} else {
						relSiteCount.put(site, relSiteCount.get(site) + 1);
					}

				} else {
					n++;
					non++;

					if (!irrelevantPerSite.containsKey(site)) {
						irrelevantPerSite.put(site, 1);
					} else {
						irrelevantPerSite.put(site, irrelevantPerSite.get(site) + 1);
					}
				}

				if (score < 0 && Math.abs(score) > 0.5) {
					bb++;

					if (!tPerSite.containsKey(site)) {
						tPerSite.put(site, 1);
					} else {
						tPerSite.put(site, tPerSite.get(site) + 1);
					}
				}

				double hv = (thai * 1.0) / (thai + non);

				if ((thai + non) % unit == 0) {
					hvout.add(hv);

				}

			}

		}

		System.out.println("Relevant/Irrelevant pages:" + thai + "/" + non);
		System.out.println("-------------");
		System.out.println("Sites count:" + relSiteCount.size());
		double page = 0;
		for (String s : relSiteCount.keySet()) {
			page += relSiteCount.get(s);
		}
		System.out.println("Average rel page/site: " + (page / relSiteCount.size()));

		System.out.println("-----Irrelevant pages in the website segment that serve at least one relevant ---------------");

		int irr = 0;
		for (String s : relSiteCount.keySet()) {
			if (irrelevantPerSite.containsKey(s)) {
				irr += irrelevantPerSite.get(s);
			}
		}

		System.out.println(irr + "\t" + (1.0 * irr / non) + "\t" + (1.0 * irr / limit));

		int tir = 0;
		for (String s : relSiteCount.keySet()) {
			if (tPerSite.containsKey(s)) {
				tir += tPerSite.get(s);
			}
		}
		System.out.println(tir + "\t" + (1.0 * tir / non) + "\t" + (1.0 * tir / limit));

		// try (BufferedWriter bw =
		// FileUtils.getBufferedFileWriter("site-segs.txt")) {
		// for (String s : relSiteCount.keySet()) {
		// // bw.write(s + "\n");
		// bw.write("http://" + s + "\n==\n");
		// }
		// } catch (Exception e) {
		// }

		if (end != null && start != null) {
			System.out.println("---- Time Usage----");
			System.out.println(start);
			System.out.println(end);
			// String input = "2017-05-03 00:56:54,218";
			// String input2 = "2017-05-03 01:56:54,218";
			SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

			try {
				Date date = parser.parse(start);
				Date date2 = parser.parse(end);
				long diff = date2.getTime() - date.getTime();
				System.out.println(TimeUnit.MILLISECONDS.toMinutes(diff) + " mins");
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
		}

		return hvout;
	}

	public static void parseClassifierCorreation(String path) {
		// int ff = 0, tf = 0, ft = 0, fff = 0, fft = 0, ftf = 0, tff = 0, ftt =
		// 0, tft = 0, ttf;
		HashMap<String, Integer> map = new HashMap<>();
		HashMap<String, Integer> map1 = new HashMap<>();// p1, p3
		HashMap<String, Integer> map2 = new HashMap<>();// p2, p3
		HashMap<String, Integer> map3 = new HashMap<>();// p1, p2

		HashSet<String> url = new HashSet<>();

		HashMap<String, Integer> map4 = new HashMap<>();// (p1, p2) + p3

		// initialize
		map.put("fff", 0);
		map.put("fft", 0);
		map.put("ftf", 0);
		map.put("tff", 0);
		map.put("ftt", 0);
		map.put("tft", 0);
		map.put("ttf", 0);
		map.put("ttt", 0);

		map1.put("ff", 0);
		map1.put("tf", 0);
		map1.put("ft", 0);
		map1.put("tt", 0);

		map2.put("ff", 0);
		map2.put("tf", 0);
		map2.put("ft", 0);
		map2.put("tt", 0);

		map3.put("ff", 0);
		map3.put("tf", 0);
		map3.put("ft", 0);
		map3.put("tt", 0);

		map4.put("ff", 0);
		map4.put("tf", 0);
		map4.put("ft", 0);
		map4.put("tt", 0);

		ConfusionMatrixObj p1 = new ConfusionMatrixObj();
		ConfusionMatrixObj p2 = new ConfusionMatrixObj();
		ConfusionMatrixObj p3 = new ConfusionMatrixObj();
		ConfusionMatrixObj all = new ConfusionMatrixObj();
		ConfusionMatrixObj two = new ConfusionMatrixObj();

		ClassifierOutput[] output;

		int totalSamples = 0;
		int numClassifier = 3;
		double entropy = 0;

		double dis = 0;

		for (String s : FileUtils.readFile(path)) {
			String[] tmp = s.split("\t");

			if (tmp[1].equals("[null]"))
				continue;

			boolean p3null = tmp[3].trim().equals("[null]");

			// if(p3null)
			// continue;

			int correct = 0;
			totalSamples++;

			boolean isRel = Boolean.parseBoolean(tmp[4].trim());

			output = new ClassifierOutput[3];

			boolean p1Rel = tmp[1].substring(tmp[1].indexOf("=") + 1, tmp[1].indexOf(",")).trim().equals("RELEVANT");
			boolean p2Rel = tmp[2].substring(tmp[2].indexOf("=") + 1, tmp[2].indexOf(",")).trim().equals("RELEVANT");
			boolean p3Rel = false;

			output[0] = new ClassifierOutput(tmp[1]);
			output[1] = new ClassifierOutput(tmp[2]);

			if (!p3null) {
				p3Rel = tmp[4].substring(tmp[4].indexOf("=") + 1, tmp[4].indexOf(",")).trim().equals("RELEVANT");
				output[2] = new ClassifierOutput(tmp[4]);
			} else {
				output[2] = null;
				// output[2] = new ClassifierOutput(0.5, 0.5, 1.0);
			}

			String h = "";
			String h_two = "";
			if (p1Rel == isRel) {
				h += "t";
				if (p1Rel) {
					p1.incTp();
				} else {
					p1.incTn();
				}
				correct++;

			} else {
				h += "f";
				if (p1Rel) {
					p1.incFp();
				} else {
					p1.incFn();
				}
			}

			if (p2Rel == isRel) {
				h += "t";
				if (p2Rel) {
					p2.incTp();
				} else {
					p2.incTn();
				}
				correct++;

			} else {
				h += "f";
				if (p2Rel) {
					p2.incFp();
				} else {
					p2.incFn();
				}
			}

			if (!p3null) {
				if (p3Rel == isRel) {
					h += "t";
					h_two += "t";
					if (p3Rel) {
						p3.incTp();
					} else {
						p3.incTn();
					}
					correct++;
				} else {
					h += "f";
					h_two += "f";

					url.add(tmp[0].trim());

					if (p3Rel) {

						p3.incFp();
					} else {
						p3.incFn();
					}
				}
			}

			output[0].setWeight(1);
			output[1].setWeight(1);
			if (output[2] != null)
				output[2].setWeight(1);

			// ensemble output
			ClassifierOutput ens = WekaClassifier.average(output[0], output[1], output[2]);
			ClassifierOutput en2 = WekaClassifier.average(output[0], output[1]);
			// System.out.println(ens + "\t" + output[0] + "\t" + output[1] +
			// "\t" + output[2]);

			if (isRel) {
				if (ens.getResultClass() == ResultClass.RELEVANT) {
					all.incTp();
					h_two += "t";
				} else {
					all.incFn();
					h_two += "f";
				}
			} else {
				if (ens.getResultClass() == ResultClass.RELEVANT) {
					all.incFp();
					h_two += "f";
				} else {
					all.incTn();
					h_two += "t";
				}
			}

			if (isRel) {
				if (en2.getResultClass() == ResultClass.RELEVANT) {
					two.incTp();
				} else {
					two.incFn();
				}
			} else {
				if (en2.getResultClass() == ResultClass.RELEVANT) {
					two.incFp();
				} else {
					two.incTn();
				}
			}

			if (map.containsKey(h)) {
				map.put(h, map.get(h) + 1);
			} else {
				map.put(h, 1);
			}
			//
			// String h1 = String.valueOf(h.charAt(0)) +
			// String.valueOf(h.charAt(2));
			// String h2 = String.valueOf(h.charAt(1)) +
			// String.valueOf(h.charAt(2));
			//
			// if (map1.containsKey(h1)) {
			// map1.put(h1, map1.get(h1) + 1);
			// } else {
			// map1.put(h1, 1);
			// }
			//
			// if (map2.containsKey(h2)) {
			// map2.put(h2, map2.get(h2) + 1);
			// } else {
			// map2.put(h2, 1);
			// }
			//
			// if (map4.containsKey(h_two)) {
			// map4.put(h_two, map4.get(h_two) + 1);
			// } else {
			// map4.put(h_two, 1);
			// }
			//
			// String h3 = String.valueOf(h.charAt(0)) +
			// String.valueOf(h.charAt(1));
			// if (map3.containsKey(h3)) {
			// map3.put(h3, map3.get(h3) + 1);
			// } else {
			// map3.put(h3, 1);
			// }
			// System.out.prindtln(h);

			entropy += (1.0 / (numClassifier - Math.ceil(numClassifier / 2.0))) * Math.min(correct, numClassifier - correct);
			dis += correct * (numClassifier - correct);
		}
		System.out.println("=============== Accuracy ==================");
		System.out.println("p1:\t" + p1);
		System.out.println("p2:\t" + p2);
		System.out.println("p3:\t" + p3);
		System.out.println("all:\t" + all);
		System.out.println("two:\t" + two);

		System.out.println("=============== Correlation ==================");
		System.out.println("Entropy : " + (entropy / totalSamples));
		System.out.printf("KW : %f\n", dis / (totalSamples * numClassifier * numClassifier));

		System.out.println(url.size());
		FileUtils.writeTextFile("test.txt", new ArrayList<String>(url), false);
		// System.out.println("Co : " +
		// calcClassifierCorrelation(map4.get("tt"), map4.get("ff"),
		// map4.get("tf"),map4.get("ft")));
		// System.out.println("p1-p2-p3:\t" +
		// calcClassifierCorrelation(map.get("fff"), map.get("fft"),
		// map.get("ftf"), map.get("tff"), map.get("ftt"), map.get("tft"),
		// map.get("ttf")));
		// System.out.println("p1-p2:\t" +
		// calcClassifierCorrelation(map3.get("ff"), map3.get("tf"),
		// map3.get("ft")));
		// System.out.println("p1-p3:\t" +
		// calcClassifierCorrelation(map1.get("ff"), map1.get("tf"),
		// map1.get("ft")));
		// System.out.println("p2-p3:\t" +
		// calcClassifierCorrelation(map2.get("ff"), map2.get("tf"),
		// map2.get("ft")));
		//
	}

	public static double calcClassifierCorrelation(int tt, int ff, int tf, int ft) {
		// double output = (tt*ff)-(ft*tf);
		// output /= Math.sqrt((tt+tf)*(ft+ff)*(tt+ft)*(tf+ff));
		double output = (2.0 * ff) / (tf + ft + 2 * ff);
		return Double.isNaN(output) ? 0 : output;
	}

	public static double calcClassifierCorrelation(int fff, int fft, int ftf, int tff, int ftt, int tft, int ttf) {
		double output = (3.0 * fff) / (fff + fft + ftf + tff + ftt + tft + 3 * fff);
		return Double.isNaN(output) ? 0 : output;
	}

	public static Comparator<File> logFilesComparator = (o1, o2) -> {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
		Date d1 = null, d2 = null;

		if (o1.getName().contains("ic.log") && o2.getName().contains("ic.log")) {
			if (o1.getName().equals("ic.log")) {
				d1 = new Date();
			} else {
				try {
					d1 = sd.parse(o1.getName().replace("ic.log.", ""));
				} catch (Exception e) {
				}
			}

			if (o2.getName().equals("ic.log")) {
				d2 = new Date();
			} else {
				try {
					d2 = sd.parse(o2.getName().replace("ic.log.", ""));
				} catch (Exception e) {
				}
			}

			if (d1 != null && d2 != null) {
				return d1.compareTo(d2);
			}
		}

		return 0;
	};

	public static void analyzeSegmentCrawlerPerformanceFromAllLog(String path, int unit) {

		int countSeg = 0;
		ConfusionMatrixObj cf = new ConfusionMatrixObj();
		boolean isData = false;

		boolean sep = false;
		for (String s : FileUtils.readFile(path)) {
			if (s.contains("@data")) {
				isData = true;
				continue;
			}

			if (!isData)
				continue;

			if (s.startsWith("-------")) {

				if (sep)
					continue;

				sep = true;

				countSeg++;

				if (countSeg % unit == 0) {
					System.out.println(cf.toString());
				}
				continue;
			} else {
				sep = false;
			}

			// if(Double.parseDouble(s.split(",")[1]) > 0.5){
			// continue;
			// }

			s = s.substring(s.lastIndexOf(",") + 1).trim();
			String[] tmp = s.split("\t");

			if (s.trim().length() == 0)
				continue;

			String predicted = tmp[2].trim();
			String result = tmp[0].trim();

			if (result.equals("thai")) {
				if (predicted.equals("rel"))
					cf.incTp();
				else
					cf.incFn();

			} else {
				if (predicted.equals("rel"))
					cf.incFp();
				else
					cf.incTn();

			}

		}

		System.out.println(countSeg);
	}

	public static void parseSegmentCrawlerPerformance(String path) {
		int dl = 0;
		File[] f = FileUtils.getAllFile(path);
		List<File> tmpList = Stream.of(f).filter(i -> i.getName().contains("ic.log")).collect(Collectors.toList());

		f = tmpList.toArray(new File[tmpList.size()]);

		Arrays.sort(f, logFilesComparator);

		int tp = 0, tn = 0, fp = 0, fn = 0;

		boolean isMulti = false;

		for (File ff : f) {
			String[] lines = FileUtils.readFile(ff.getPath());

			String dd = "";
			for (String s : lines) {

				if (s.contains("HttpSegmentCrawlerMulti"))
					isMulti = true;

				if (s.contains("DOWNLOADED"))
					dd = s.substring(s.indexOf("DOWNLOADED")).split("\t")[7];

				if (s.contains("segment predictor precision:")) {
					tp = 0;
					tn = 0;
					fp = 0;
					fn = 0;

					if (isMulti) {
						String relSrc = s.substring(s.indexOf("TP"), s.indexOf("|")).trim();
						String nonSrc = s.substring(s.indexOf("|") + 1).trim();

						// relSrc
						String[] tmp;
						tmp = relSrc.split("\t");

						// System.out.println(relSrc);
						// System.out.println(nonSrc);

						if (tmp[0].equals("TP:")) {
							tp = Integer.parseInt(tmp[1].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							tn = Integer.parseInt(tmp[3].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							fp = Integer.parseInt(tmp[5].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							fn = Integer.parseInt(tmp[7].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));

							// nonSrc
							tmp = nonSrc.split("\t");
							tp += Integer.parseInt(tmp[1].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							tn += Integer.parseInt(tmp[3].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							fp += Integer.parseInt(tmp[5].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							fn += Integer.parseInt(tmp[7].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));

						} else {
							tp = Integer.parseInt(tmp[0].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							tn = Integer.parseInt(tmp[1].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							fp = Integer.parseInt(tmp[2].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							fn = Integer.parseInt(tmp[3].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));

							// nonSrc
							tmp = nonSrc.split("\t");
							tp += Integer.parseInt(tmp[0].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							tn += Integer.parseInt(tmp[1].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							fp += Integer.parseInt(tmp[2].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							fn += Integer.parseInt(tmp[3].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));

						}
					} else {
						String relSrc = s.substring(s.indexOf("TP")).trim();
						String[] tmp = relSrc.split("\t");

						if (tmp[0].equals("TP:")) {
							tp = Integer.parseInt(tmp[1].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							tn = Integer.parseInt(tmp[3].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							fp = Integer.parseInt(tmp[5].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							fn = Integer.parseInt(tmp[7].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
						} else {
							tp = Integer.parseInt(tmp[0].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							tn = Integer.parseInt(tmp[1].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							fp = Integer.parseInt(tmp[2].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));
							fn = Integer.parseInt(tmp[3].replace("TP:", "").replace("FP:", "").replace("TN:", "").replace("FN:", ""));

						}

					}

					// l = Double.parseDouble();

					System.out.println(new ConfusionMatrixObj(tp, fn, tn, fp));
				}

			}
		}

	}

	public static ArrayList<Double> parseUpdateSegmentPredictor2(String input, int unit) {
		File[] f = new File(input).listFiles();

		List<File> tmpList = Stream.of(f).filter(i -> i.getName().contains("ic.log")).collect(Collectors.toList());

		f = tmpList.toArray(new File[tmpList.size()]);

		Arrays.sort(f, logFilesComparator);

		ArrayList<Double> results = new ArrayList<>();
		double avg = 0;
		int countSeg = 0, countPage = 0;

		System.out.println("DownloadedPage\tDownloadedSegment\tQueueScore");
		for (int i = 0; i < f.length; i++) {

			if (!f[i].getName().contains(".log"))
				continue;

			String[] lines = FileUtils.readFile(f[i].getPath());
			String sep = "Finished downloaded";
			String iden = "DOWNLOADED";
			for (String s : lines) {

				if (s.contains(iden)) {
					countPage++;
				}

				if (s.contains(sep)) {

					countSeg++;
					String[] tmp = s.substring(s.indexOf(sep) + sep.length()).trim().split("\t");
					double degree = Double.parseDouble(tmp[2]) / Double.parseDouble(tmp[6]);
					boolean isRel = degree > 0.5 ? true : false;

					boolean predictionAsRel = Double.parseDouble(tmp[8]) > 0.5 ? true : false;

					if (isRel && predictionAsRel) {
						avg += 1;
					}

					if (countSeg % 100 == 0) {
						System.out.printf("%d\t%d\t%.3f\n", countPage, countSeg, avg / countSeg);
					}

				}

			}

		}

		return results;
	}

	public static ArrayList<Double> parseSegmentCrawlerQScore(String input, int unit) {
		File[] f = new File(input).listFiles();

		List<File> tmpList = Stream.of(f).filter(i -> i.getName().contains("ic.log")).collect(Collectors.toList());

		f = tmpList.toArray(new File[tmpList.size()]);

		Arrays.sort(f, logFilesComparator);

		ArrayList<Double> results = new ArrayList<>();
		double avg = 0;
		int countSeg = 0, countPage = 0;
		System.out.println("DownloadedPage\tDownloadedSegment\tQueueScore");
		for (int i = 0; i < f.length; i++) {

			if (!f[i].getName().contains(".log"))
				continue;

			String[] lines = FileUtils.readFile(f[i].getPath());
			String sep = "Finished downloaded";
			String iden = "DOWNLOADED";
			double qscore = -1;
			double score = 0;

			int c = 0;
			for (String s : lines) {

				if (s.contains("DOWNLOADED")) {
					s = s.substring(s.indexOf("qScore:\t") + "qScore:\t".length()).trim();
					s = s.substring(0, s.indexOf("\t"));

					// latest qscore
					qscore = Double.parseDouble(s);
					countPage++;
				}

				// System.out.println(s);
				if (s.contains(sep)) {
					s = s.substring(s.indexOf(sep) + sep.length()).trim();
					String[] tmp = s.split("\t");

					double th = Double.parseDouble(tmp[2]);
					double non = Double.parseDouble(tmp[4]);
					double relDegree = 1.0 * th / (th + non);

					// 8 = prediction
					// System.out.println(s);
					score = Double.parseDouble(tmp[8]);
					score = qscore;

					// if (relDegree <= 0.5 && score > 0.5) {
					avg += score;
					results.add(score);
					c++;
					// }

					countSeg++;

					if (countSeg % 100 == 0) {
						System.out.printf("%d\t%d\t%.3f\n", countPage, countSeg, avg / 100);
						avg = 0;
						c = 0;
					}

				}

			}

		}

		return results;
	}

	public static void printHV(int unit, ArrayList<Double>... inputs) {
		printHV(unit, true, inputs);
	}

	public static void printHV(int unit, boolean incPage, ArrayList<Double>... inputs) {
		int count = 0;

		int min = Integer.MAX_VALUE;
		for (int i = 0; i < inputs.length; i++) {
			inputs[i].add(0, 1.0);
			min = Math.min(min, inputs[i].size());
		}

		// min++;

		for (int i = 0; i < min; i++) {

			if (incPage)
				if (i == 0)
					System.out.print("1\t");
				else
					System.out.print(((i) * unit) + "\t");

			for (int j = 0; j < inputs.length; j++) {
				System.out.printf("%.3f", inputs[j].get(i));

				if (j != inputs.length - 1)
					System.out.print("\t");
			}

			System.out.println();
			// double hv = 0;
			// for (int j = 0; j < inputs.length; j++) {
			// hv += inputs[j].get(i);
			// }
			//
			// hv /= inputs.length;
			// count += unit;
			// if (incPage)
			// System.out.printf("%d\t%.3f\t", count, hv);
			// else
			// System.out.printf("%.3f\t", hv);
		}

		System.out.println();
	}

	public static ArrayList<Double> softlog(String input, int limitPage, int unit) {

		int thai = 0;
		int non = 0;
		int c = 0;

		double score;

		ArrayList<Double> output = new ArrayList<>();

		HashSet<String> dl = new HashSet<>();
		String url;

		File[] files = new File(input).listFiles();
		List<File> tmpList = Stream.of(files).filter(o -> o.getName().contains("ic.log")).collect(Collectors.toList());
		files = tmpList.toArray(new File[tmpList.size()]);

		Arrays.sort(files, logFilesComparator);

		for (int i = 0; i < files.length; i++) {
			String[] tmp;
			if (!files[i].getName().contains(".log"))
				continue;

			String[] lines = FileUtils.readFile(files[i].getPath());

			for (String s : lines) {
				if (!s.contains("http"))
					continue;

				if (s.contains("fixRelativePath error"))
					continue;

				if (s.contains("HttpUtils"))
					continue;

				if (input.toLowerCase().contains("best")) {
					if (s.contains("ERROR BestFirst"))
						continue;

					if (!s.contains("BestFirst"))
						continue;
				}

				if (input.toLowerCase().contains("hmm")) {
					if (s.contains("ERROR HMMCrawler"))
						continue;

					if (!s.contains("HMMCrawler"))
						continue;
				}

				s = s.substring(s.indexOf("http"));
				s = s.trim();
				tmp = s.split("\t");

				c++;
				if (tmp[0].contains("filedesc:")) {
					// System.out.println("FILE");
					continue;
				}

				url = tmp[0].toLowerCase().trim();

				url = HttpUtils.getStaticUrl(url, true);

				url = url.replace("http://-%20", "http://");

				if (dl.contains(url)) {
					System.out.println("DUP " + url);
					continue;
				}

				dl.add(url);
				score = Double.parseDouble(tmp[1]);

				// System.out.println(url);
				if (HtmlParser.shouldFilter(url)) {
					// System.out.println("filter");
					score = 0;
				}

				if (score > 0.5) {
					thai++;
				} else {
					non++;
				}

				if (thai + non > limitPage)
					break;

				if ((thai + non) % unit == 0) {
					double hv = (thai * 1.0) / (thai + non);

					output.add(hv);
				}
			}
		}

		return output;

	}

	public static void parseDbLog() {
		String[] lines = FileUtils.readFile("bh1.txt");
		String[] l;

		int[] ci = { 0, 0, 0, 0, 0, 0 };
		int[] cr = { 0, 0, 0, 0, 0, 0 };

		for (String s : lines) {
			l = s.split("\t");
			String domain = HttpUtils.getDomain(l[0]);
			if (domain == null) {
				cr[5] += Integer.parseInt(l[1]);
				ci[5] += Integer.parseInt(l[2]);

			} else {
				// System.out.println(s);
				domain = domain.toLowerCase();
				switch (domain) {
				case "com":
					cr[0] += Integer.parseInt(l[1]);
					ci[0] += Integer.parseInt(l[2]);
					break;
				case "net":
					cr[1] += Integer.parseInt(l[1]);
					ci[1] += Integer.parseInt(l[2]);
					break;
				case "org":
					cr[2] += Integer.parseInt(l[1]);
					ci[2] += Integer.parseInt(l[2]);
					break;
				case "th":
					cr[3] += Integer.parseInt(l[1]);
					ci[3] += Integer.parseInt(l[2]);
					break;
				case "info":
					cr[4] += Integer.parseInt(l[1]);
					ci[4] += Integer.parseInt(l[2]);
					break;

				default:
					cr[5] += Integer.parseInt(l[1]);
					ci[5] += Integer.parseInt(l[2]);
					break;
				}
			}

		}

		for (int i = 0; i <= 5; i++) {
			System.out.printf("R\t%d\tI\t%d\n", cr[i], ci[i]);
		}

	}

	public static void parseCorrelation(String... dirs) {
		ArrayList<HashSet<String>> dbs = new ArrayList<>();
		for (int i = 0; i < dirs.length; i++) {
			HashSet<String> db = new HashSet<>();

			File[] f = new File(dirs[i]).listFiles();
			System.out.println(f.length);
			for (int j = 0; j < f.length; j++) {
				if (!f[j].getName().contains("ic.log"))
					continue;
				// String[] lines = FileUtils.readFile(base + i + ".log");
				String[] lines = FileUtils.readFile(f[j].getPath());
				String iden = "DOWNLOADED";
				for (String s : lines) {
					if (!s.contains(iden)) {
						continue;
					}
					s = s.substring(s.indexOf(iden) + iden.length());
					s = s.trim();
					String[] tmp = s.split("\t");
					db.add(HttpUtils.getStaticUrl(tmp[3], true).toLowerCase());
				}
			}
			dbs.add(db);
		}
		// d1 - common subset
		HashSet<String> all = new HashSet<>();
		for (int k = 0; k < dirs.length; k++) {
			all.addAll(dbs.get(k));
		}
		System.out.println("Total urls " + all.size());
		// common subset
		if (dirs.length > 1) {
			for (int k = 0; k < dirs.length; k++) {
				all.retainAll(dbs.get(k));
			}
		}
		System.out.println("Common urls " + all.size());
	}
	// String[] lines = FileUtils.readFile(base + i + ".log");

	public static void parseCrawlerProxyLog(String path) {

		int countSegs = 0;
		int relSegs = 0;
		int nonSegs = 0;
		double avgRelPagesPerRelSeg = 0;
		double avgPagesPerSeg = 0;

		for (String s : FileUtils.readFile(path)) {

			if (s.contains("Finished downloaded")) {

				s = s.substring(s.indexOf("Finished downloaded")) + "Finished downloaded".length();
				s = s.trim();
				String[] data = s.split("\t");

				int thai = Integer.parseInt(data[2]);
				int non = Integer.parseInt(data[4]);
				int total = Integer.parseInt(data[6]);

				double relDegree = 1.0 * thai / (thai + non);
				avgRelPagesPerRelSeg += total;

				if (relDegree > CrawlerConfig.getConfig().getRelevanceDegreeThreshold()) {
					avgRelPagesPerRelSeg += thai;
					relSegs++;
				} else {
					nonSegs++;
				}
				avgPagesPerSeg += total;
				countSegs++;
			}

			System.out.println("Total segs:\t" + countSegs);
			System.out.println("Relevant segs:\t" + relSegs);
			System.out.println("Non segs:\t" + nonSegs);
			System.out.println("avg relevant page per seg:\t" + avgRelPagesPerRelSeg / relSegs);
			System.out.println("avg page per seg:\t" + avgPagesPerSeg / countSegs);

		}
	}

	public static void parsePredictorPoolLog(String path) {
		File[] files = FileUtils.getAllFile(path);
		Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
		String iden = "Average Gmean :";

		double avg = 0;
		int count = 0, c = 0;
		for (File f : files) {
			System.out.println(f.getName());
			String[] lines = FileUtils.readFile(f.getPath());

			for (String s : lines) {
				if (s.contains(iden)) {
					s = s.substring(s.indexOf(iden) + iden.length()).trim();
					logger.info(s);

					avg += Double.parseDouble(s.split("\t")[0]);
				}
			}

			logger.info("Overall G-mean : " + avg / 10);

		}

	}

	public static void parseGmean(String path) {
		String[] lines = FileUtils.readFile(path);

		double avg = 0;
		double avg_t = 0;
		int count = 0;
		for (String s : lines) {
			if (!s.contains("g-mean"))
				continue;

			s = s.replace("-", "");
			s = s.replace(":", "");

			s = s.split("\t")[1];
			avg_t += Double.parseDouble(s);
			avg += Double.parseDouble(s);

			count++;
			if (count % 10 == 0) {
				System.out.println("------------------");
				System.out.println(avg_t / 10);
				avg_t = 0;
			}
		}

		System.out.println("Final :\t" + avg / count);
		System.out.println(count);

	}

	public static void selectSamples(String path, int limit, boolean isThai) {
		File[] files = FileUtils.getAllFile(path);
		ArrayList<String> data = new ArrayList<>();

		String[] tmp;
		for (File f : files) {
			if (f.isDirectory() || !f.getName().contains(".log"))
				continue;
			String[] lines = FileUtils.readFile(f.getPath());
			String iden = "DOWNLOADED";
			for (String s : lines) {
				if (!s.contains(iden)) {
					continue;
				}

				s = s.substring(s.indexOf(iden) + iden.length());
				s = s.trim();
				tmp = s.split("\t");

				double score = Double.parseDouble(tmp[0]);

				if (isThai && score > 0.5)
					data.add(tmp[2].toLowerCase());
				else if (!isThai && (score <= 0.5 && score > 0))
					data.add(tmp[2].toLowerCase());

			}

		}

		Collections.shuffle(data);
		ArrayList<String> ndata = new ArrayList<>();
		for (int i = 0; i < limit && data.size() > 0; i++) {
			ndata.add(data.remove(0));
		}

		if (isThai)
			FileUtils.writeTextFile("thai.txt", ndata, false);
		else
			FileUtils.writeTextFile("non.txt", ndata, false);

	}

	public static void parsePageClassifier(String path) {
		String[] lines = FileUtils.readFile(path);

		double avg = 0;
		double avg_t = 0;
		int count = 0;

		String iden = "WekaClassifier:281-";
		for (String s : lines) {
			if (!s.contains(iden))
				continue;

			s = s.substring(s.indexOf(iden) + iden.length());
			s = s.replace("TP", "").replace("FP", "").replace("TN", "").replace("FN", "");

			String[] tmp = s.split("\t");
			ConfusionMatrixObj cf = new ConfusionMatrixObj(Integer.parseInt(tmp[0].trim()), Integer.parseInt(tmp[1].trim()), Integer.parseInt(tmp[2].trim()), Integer.parseInt(tmp[3].trim()));

			avg_t += cf.getGmean();
			avg += cf.getGmean();

			count++;
			if (count % 10 == 0) {
				System.out.println(count / 10 + "\t" + avg_t / 10);
				avg_t = 0;
			}
		}

		System.out.println("Final :\t" + avg / count);
		System.out.println(count);

	}

	public static void parsePredictorLog(String path, int k) {

		double anchor = 0;
		double link = 0;
		double url = 0;
		double best = 0;
		double min = 0;
		double max = 0;
		double product = 0;
		double average = 0;
		double majority = 0;
		int count = 0;

		double[] minG = new double[8];
		for (int i = 0; i < minG.length; i++)
			minG[i] = Double.MAX_VALUE;

		double[] maxG = new double[8];

		// String filename = path.substring(path.indexOf("/")+1);
		System.out.println(">>" + path);
		String[] lines = FileUtils.readFile(path);

		double tmp = 0;
		double a = 0;
		int c = 0;
		int d = 0;
		double b = 0;
		for (String s : lines) {

			String target = "link G-mean:";
			if (s.contains(target)) {
				s = s.substring(s.indexOf(target) + target.length());
				link += Double.parseDouble(s);
				count++;

				tmp = Double.parseDouble(s);
				minG[0] = Math.min(minG[0], tmp);
				maxG[0] = Math.max(maxG[0], tmp);
				// FileUtils.writeTextFile("link-gmean.log",
				// Double.parseDouble(s) + "\n", true);
				// System.out.println("link >" + s.trim());
				b += tmp;
				d++;
				if (d % k == 0) {
					System.out.println("Link >" + b / k);
					b = 0;
				}

			}

			target = "anchor G-mean:";
			if (s.contains(target)) {
				s = s.substring(s.indexOf(target) + target.length());
				anchor += Double.parseDouble(s);

				tmp = Double.parseDouble(s);
				minG[1] = Math.min(minG[1], tmp);
				maxG[1] = Math.max(maxG[1], tmp);
				// FileUtils.writeTextFile("anchor-gmean-" + filename,
				// Double.parseDouble(s) + "\n", true);
			}

			target = "url G-mean:";
			if (s.contains(target)) {
				s = s.substring(s.indexOf(target) + target.length());
				url += Double.parseDouble(s);

				tmp = Double.parseDouble(s);
				minG[2] = Math.min(minG[2], tmp);
				maxG[2] = Math.max(maxG[2], tmp);
				// FileUtils.writeTextFile("url-gmean-" + filename,
				// Double.parseDouble(s) + "\n", true);
			}

			target = "final min g-mean:";
			if (s.contains(target)) {
				s = s.substring(s.indexOf(target) + target.length());
				min += Double.parseDouble(s);

				tmp = Double.parseDouble(s);
				minG[3] = Math.min(minG[3], tmp);
				maxG[3] = Math.max(maxG[3], tmp);
				// FileUtils.writeTextFile("min-gmean-" + filename,
				// Double.parseDouble(s) + "\n", true);
			}

			target = "final max g-mean:";
			if (s.contains(target)) {
				s = s.substring(s.indexOf(target) + target.length());
				max += Double.parseDouble(s);

				tmp = Double.parseDouble(s);
				minG[4] = Math.min(minG[4], tmp);
				maxG[4] = Math.max(maxG[4], tmp);
				// FileUtils.writeTextFile("max-gmean-" + filename,
				// Double.parseDouble(s) + "\n", true);
			}

			target = "final average g-mean:";
			if (s.contains(target)) {
				s = s.substring(s.indexOf(target) + target.length());
				average += Double.parseDouble(s);

				tmp = Double.parseDouble(s);
				minG[5] = Math.min(minG[5], tmp);
				maxG[5] = Math.max(maxG[5], tmp);

				a += Double.parseDouble(s);
				// System.out.println(s);
				c++;
				if (c % k == 0) {
					System.out.println((c / k - 1) + "\t" + (a / k));
					a = 0;
				}

				// FileUtils.writeTextFile("average-gmean.log",
				// Double.parseDouble(s) + "\n", true);

			}

			target = "final product g-mean:";
			if (s.contains(target)) {
				s = s.substring(s.indexOf(target) + target.length());
				product += Double.parseDouble(s);
				tmp = Double.parseDouble(s);
				minG[6] = Math.min(minG[6], tmp);
				maxG[6] = Math.max(maxG[6], tmp);
				// FileUtils.writeTextFile("product-gmean-" + filename,
				// Double.parseDouble(s) + "\n", true);
			}

			target = "final majority g-mean:";
			if (s.contains(target)) {
				s = s.substring(s.indexOf(target) + target.length());
				majority += Double.parseDouble(s);
				tmp = Double.parseDouble(s);
				minG[7] = Math.min(minG[7], tmp);
				maxG[7] = Math.max(maxG[7], tmp);
				// FileUtils.writeTextFile("majority-gmean-" + filename,
				// Double.parseDouble(s) + "\n", true);
			}

			target = "final best-fusion g-mean";
			if (s.contains(target)) {
				s = s.substring(s.indexOf(target) + target.length());
				// System.out.println(s);
				best += Double.parseDouble(s);
				// FileUtils.writeTextFile(filename + "-best",
				// Double.parseDouble(s) + "\n", true);
			}

			// if(s.contains(":243") && count > 0){
			// System.out.println("hello world");
			// anchor/=10;
			// link/=10;
			// url/=10;
			//
			// min/=10;
			// max/=10;
			// average/=10;
			// product/=10;
			// majority/=10;
			//
			// anchor = link = url = min = max = average = product =
			// majority = 0;
			//
			// }
		}

		anchor /= k * 10;
		link /= k * 10;
		url /= k * 10;
		best /= k * 10;

		min /= k * 10;
		max /= k * 10;
		average /= k * 10;
		product /= k * 10;
		majority /= k * 10;
		System.out.printf("%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", link, anchor, url, min, max, average, product, majority, best);

		// System.out.println(link);
		// System.out.println(anchor);
		// System.out.println(url);
		// System.out.println(min);
		// System.out.println(max);
		// System.out.println(average);
		// System.out.println(product);
		// System.out.println(majority);

	}

	public static void parseSimpleMLLogv2(String path) {
		// String filename = path.substring(path.indexOf("/") + 1);

		double anchor = 0;
		double link = 0;
		double url = 0;
		double single = 0;
		int count = 0;

		File[] files = new File(path).listFiles();

		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
			}
		});

		for (File f : files) {

			String[] lines = FileUtils.readFile(f.getPath());

			for (String s : lines) {

				String target = "link G-mean:";
				if (s.contains(target)) {
					s = s.substring(s.indexOf(target) + target.length());
					link += Double.parseDouble(s);
					count++;
					// FileUtils.writeTextFile("link-simple-gmean-" + path,
					// Double.parseDouble(s) + "\n", true);

				}

				target = "anchor G-mean:";
				if (s.contains(target)) {
					s = s.substring(s.indexOf(target) + target.length());
					anchor += Double.parseDouble(s);
					// FileUtils.writeTextFile("anchor-simple-gmean-" + path,
					// Double.parseDouble(s) + "\n", true);

				}

				target = "url G-mean:";
				if (s.contains(target)) {
					s = s.substring(s.indexOf(target) + target.length());
					url += Double.parseDouble(s);
					// FileUtils.writeTextFile("url-simple-gmean-" + path,
					// Double.parseDouble(s) + "\n", true);

				}

				target = "single G-mean:";
				if (s.contains(target)) {
					s = s.substring(s.indexOf(target) + target.length());
					single += Double.parseDouble(s);
					FileUtils.writeTextFile("single-simple-gmean.log", Double.parseDouble(s) + "\n", true);
				}

			}
		}

		anchor /= 100;
		link /= 100;
		url /= 100;

		System.out.printf("%.3f\t%.3f\t%.3f\t%.3f\n", link, anchor, url, single);

		// System.out.println(link);
		// System.out.println(anchor);
		// System.out.println(url);
		// System.out.println(min);
		// System.out.println(max);
		// System.out.println(average);
		// System.out.println(product);
		// System.out.println(majority);

	}

	public static void parseSimpleGmean(String path) {
		String[] lines = FileUtils.readFile(path + "/ic.log");

		double avg = 0;
		double avg_t = 0;
		int count = 0;
		for (String s : lines) {
			if (!s.contains("simple g-mean:"))
				continue;

			s = s.split("\t")[1];
			avg_t += Double.parseDouble(s);
			avg += Double.parseDouble(s);

			count++;
			if (count % 10 == 0) {
				System.out.println("------------------");
				System.out.println(avg_t / 10);
				avg_t = 0;
			}
		}

		System.out.println("Final :\t" + avg / count);
		System.out.println(count);

	}

	public static void test() throws Exception {
		HashSet<String> limit = new HashSet<>(Arrays.asList(FileUtils.readFile("limit.txt")));
		HashSet<String> d0 = new HashSet<>();
		for (String s : FileUtils.readFile("f/d0.txt")) {
			d0.add(s.split("\t")[0]);
		}

		int com = 0;
		int net = 0;
		int th = 0;
		int org = 0;
		int info = 0;
		int other = 0;

		int icom = 0;
		int inet = 0;
		int ith = 0;
		int iorg = 0;
		int iinfo = 0;
		int iother = 0;

		String[] tmp;
		String seg;
		int thai = 0;
		int non = 0;
		for (int i = 0; i <= 2; i++) {
			String[] s = FileUtils.readFile("d1/hop" + i + "-1.txt");
			for (String t : s) {
				tmp = t.split("\t");
				seg = tmp[0].toLowerCase();
				if (d0.contains(seg) && !limit.contains(seg)) {
					continue;
				}

				System.out.println(tmp[0]);
				String domain = HttpUtils.getDomain(tmp[0]);
				if (domain == null) {
					continue;
				}

				domain = domain.toLowerCase();

				switch (domain) {
				case "com":
					com += Integer.parseInt(tmp[1]);
					icom += Integer.parseInt(tmp[2]);
					break;
				case "net":
					net += Integer.parseInt(tmp[1]);
					inet += Integer.parseInt(tmp[2]);
					break;
				case "th":
					th += Integer.parseInt(tmp[1]);
					ith += Integer.parseInt(tmp[2]);
					break;
				case "org":
					org += Integer.parseInt(tmp[1]);
					iorg += Integer.parseInt(tmp[2]);
					break;
				case "info":
					info += Integer.parseInt(tmp[1]);
					iinfo += Integer.parseInt(tmp[2]);
					break;
				default:
					other += Integer.parseInt(tmp[1]);
					iother += Integer.parseInt(tmp[2]);
					break;
				}

				thai += Integer.parseInt(tmp[1]);
				non += Integer.parseInt(tmp[2]);

			}

		}

		System.out.println(thai);
		System.out.println(non);
		System.out.println("==========");
		System.out.println(com);
		System.out.println(net);
		System.out.println(th);
		System.out.println(org);
		System.out.println(info);
		System.out.println(other);
		System.out.println("==========");
		System.out.println(icom);
		System.out.println(inet);
		System.out.println(ith);
		System.out.println(iorg);
		System.out.println(iinfo);
		System.out.println(iother);
	}

	public static void parseProxyLog(String[] args) throws IOException {
		HashSet<String> urldb = new HashSet<>();

		HashSet<String> hostDb = new HashSet<>();

		Hashtable<String, Integer> ddb = new Hashtable<>();
		HashSet<String> hdb = new HashSet<>();
		String[] lines = FileUtils.readFile("d:/current-research/logs-proxy/ic.log");
		String[] tmp;
		String host, domain;
		int thai = 0;
		int non = 0;
		double score;
		String base;
		for (String l : lines) {
			if (!l.contains("PageIndexer:71-"))
				continue;

			l = l.substring(l.indexOf("PageIndexer:71-") + "PageIndexer:71-".length()).trim();
			tmp = l.split("\t");

			if (tmp[0].contains("filedesc:"))
				continue;

			if (urldb.contains(HttpUtils.getStaticUrl(tmp[0].trim().toLowerCase())))
				continue;

			host = HttpUtils.getHost(tmp[0]);
			base = HttpUtils.getBasePath(tmp[0]);
			if (!hdb.contains(host)) {

				domain = HttpUtils.getDomain(tmp[0]).toLowerCase();
				if (!ddb.containsKey(domain)) {
					ddb.put(domain, 1);
				} else {
					int c = ddb.get(domain);
					c++;

					ddb.put(domain, c);
				}
			}
			hdb.add(host);

			urldb.add(HttpUtils.getStaticUrl(tmp[0].trim().toLowerCase()));

			hostDb.add(host);

			if (Double.parseDouble(tmp[1]) >= 0.5)
				thai++;
			else
				non++;

		}

		Enumeration<String> e = ddb.keys();
		String key;
		System.out.println(thai);
		System.out.println(non);
		System.out.println(thai + non);
		System.out.println("============");
		System.out.println(hdb.size());
		System.out.println("============");
		while (e.hasMoreElements()) {
			key = e.nextElement();
			System.out.printf("%s\t%d\n", key, ddb.get(key));
		}

	}

	public static void parseMainCfLog(String path, String out) {
		// TODO Auto-generated method stub
		// String[] s =
		// FileUtils.readFile("D:/current-research/f/result/3/logs/ic.log");
		String[] s = FileUtils.readFile(path);

		String trainFile = null;
		String algo = null;
		// int tp = 0, fp = 0, tn = 0, fn = 0;
		String[] tmp;
		String output;
		double acc = 0;
		double g = 0;
		try (BufferedWriter bw = FileUtils.getBufferedFileWriter(out)) {
			String oldFile = "";
			for (String l : s) {
				// System.out.println(l);

				String iden = "WekaClassifier:66-";
				if (l.contains(iden)) {
					trainFile = l.substring(l.indexOf(iden) + iden.length());

					if (!oldFile.equals(trainFile))
						bw.write("===========\n");
					oldFile = trainFile;
				}

				iden = "WekaClassifier:151-";
				if (l.contains(iden)) {
					algo = l.substring(l.indexOf(iden) + iden.length());

				}

				iden = "WekaClassifier:155- Accuracy";
				if (l.contains(iden)) {
					acc = Double.parseDouble(l.substring(l.indexOf(iden) + iden.length()));

				}

				iden = "WekaClassifier:165- G-mean";
				if (l.contains(iden)) {
					g = Double.parseDouble(l.substring(l.indexOf(iden) + iden.length()).toLowerCase());
					output = String.format("%s\t%s\t%.3f\t%.3f\n", algo, trainFile, g, acc);
					bw.write(output);
					System.out.println(output);

				}
				// if(l.contains("WekaClassifier:152- ")){
				// }
				// System.out.printf("%s\t%s\n", trainFile, algo);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
