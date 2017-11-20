package com.job.ic.experiments;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.ml.classifiers.TokenizedOutput;
import com.job.ic.nlp.services.LanguageIdentifier;
import com.job.ic.nlp.services.Stemmer;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;

import org.apache.log4j.Logger;
import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecordBase;

public class YahooDirExtractor extends Thread {

	private String filePath;
	private static LinkedBlockingQueue<String> buffer = new LinkedBlockingQueue<>();

	public YahooDirExtractor(String filePath) {
		this.filePath = filePath;
	}

	static int total;
	static int complete = 0;
	public static int count = 0;
	private static Logger logger = Logger.getLogger(YahooDirExtractor.class);
	private static BufferedWriter bw;

	public static void main(String[] args) {
		parse("dl/", "data.txt");
		// process("data.txt");
	}

	public static void extractData() {

		String target = "programming_languages";
		int limitNon = 1;
		HashSet<String> dat = new HashSet<>();
		HashSet<String> nondat = new HashSet<>();

		ArrayList<String> output = new ArrayList<>();

		HashSet<String> blogHosts = new HashSet<>();
		int r = 0;
		String topic = null;
		// String sss = "[http://www.kyo.or.jp/nouritsu/,
		// http://www.kyo.or.jp/nouritsu/]";

		boolean isTarget = false;

		for (String s : FileUtils.readFile("data_processed.txt")) {

			if (s.trim().length() == 0)
				continue;

			if (s.equals("==")) {
				r = 0;
				continue;
			}

			r++;

			if (r == 1) {
				s = s.substring(1, s.length() - 1);
				String[] tmp = s.split("\t");

				if (tmp.length == 2) {
					s = tmp[1];
					topic = s;

					if (s.contains(target)) {
						isTarget = true;
					}
				}
			}

			if (r == 2) {

				if (isTarget) {
					if (s.startsWith("[http")) {
						s = s.substring(1, s.length() - 1);
						String[] tmp = s.split(",");

						// output.add(topic + "\t" + tmp.length);
						for (String u : tmp) {
							u = u.trim();

							if (!u.contains("twitter") && !u.contains("facebook") && !u.contains("instagram")) {

								if (!HtmlParser.shouldFilter(u))
									dat.add(u + "\n==");
								else
									nondat.add(u + "\n==");
							}
						}
					}

					isTarget = false;

				} else if (!isTarget && !topic.contains("regional")) {
					if (s.startsWith("[http")) {
						s = s.substring(1, s.length() - 1);
						String[] tmp = s.split(",");
						int lim = limitNon;

						for (String u : tmp) {

							if (lim == 0)
								break;

							u = u.trim();

							if (!u.contains("twitter") && !u.contains("facebook") && !u.contains("instagram")) {

								if (!HtmlParser.shouldFilter(u)) {
									nondat.add(u + "\n==");
									lim--;
								}
							}

						}
					}
				}

			}
		}

		FileUtils.writeTextFile("programing-rel.txt", dat, false);
		FileUtils.writeTextFile("programing-non.txt", nondat, false);
		// FileUtils.writeTextFile("tmp.txt", output, false);

		// try (BufferedWriter bw =
		// FileUtils.getBufferedFileWriter("url-regional-data.txt")) {
		//
		// for (String s : dat) {
		// bw.write(s + "\n==\n");
		// }
		//
		// } catch (Exception e) {
		//
		// }
	}

	public static void parse(String path, String outputPath) {

		Locale.setDefault(Locale.US);
		System.setProperty("file.encoding", "UTF-8");

		String filePath = path;

		int numThread = 8;

		// String filePath = args[0];
		// int numThread = Integer.parseInt(args[1]);

		ArrayList<File> f = new ArrayList<>(Arrays.asList(new File(filePath).listFiles()));
		Collections.shuffle(f);

		while (f.size() > 3000) {
			f.remove(0);
		}

		total = f.size();

		ExecutorService sv = Executors.newFixedThreadPool(numThread);
		for (File fi : f) {
			sv.submit(new YahooDirExtractor(fi.getPath()));
		}

		sv.shutdown();

		while (!sv.isTerminated()) {
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		ArrayList<String> output = new ArrayList<>();
		buffer.drainTo(output);
		FileUtils.writeTextFile(outputPath, output, false);
		System.exit(0);

	}

	public static void process(String path) {

		HashMap<String, ArrayList<String>> hdata = new HashMap<>();
		HashMap<String, String> name = new HashMap<>();
		ArrayList<String> output = new ArrayList<>(Arrays.asList(FileUtils.readFile(path)));
		// String[] output = ;

		// first part (deal with normal urls)
		for (String s : output) {
			s = s.toLowerCase().trim();
			if (s.length() == 0)
				continue;

			try {
				String[] data = s.split("\t");
				if (data[2].startsWith("http")) {
					if (hdata.containsKey(data[0])) {
						hdata.get(data[0]).add(data[2]);
					} else {
						ArrayList<String> tmp = new ArrayList<>();
						tmp.add(data[2]);
						hdata.put(data[0].trim(), tmp);
					}
				}

				if (!name.containsKey(data[0]))
					name.put(data[0], data[1]);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(">>>" + s);
			}
		}

		System.out.println("-----");
		int count = 0;

		// second part (deal with sub & link dir)
		HashSet<String> miss = new HashSet<>();
		for (String s : output) {

			s = s.toLowerCase().trim();
			if (s.length() == 0)
				continue;

			try {
				String[] data = s.split("\t");
				if (!data[2].startsWith("http")) {
					try {
						ArrayList<String> tmp = access(hdata, data[2]);
						if (tmp != null) {
							if (hdata.containsKey(data[0])) {
								hdata.get(data[0]).addAll(tmp);
							} else {
								hdata.put(data[0], tmp);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						count++;
						miss.add(s);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(">>>" + s);
			}

		}

		System.out.println(count + "\t" + miss.size());
		FileUtils.writeTextFile("missing.txt", new ArrayList<>(miss), false);
		System.out.println("-----");

		try (BufferedWriter bw = FileUtils.getBufferedFileWriter("data_processed.txt")) {
			for (String cat : hdata.keySet()) {
				// String catName = name.get(cat);
				String line = cat + "\t";
				for (String u : hdata.get(cat)) {
					line += u + "\t";
				}

				line = line.trim() + "\n";
				bw.write(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("--Fin--");
	}

	public static ArrayList<String> access(HashMap<String, ArrayList<String>> hdata, String cat) {
		if (!hdata.containsKey(cat))
			return null;

		ArrayList<String> output = new ArrayList<>();
		ArrayList<String> urls = hdata.get(cat);
		for (int i = 0; i < urls.size(); i++) {
			if (urls.get(i).startsWith("http")) {
				output.add(urls.get(i));
			} else {
				ArrayList<String> tmp = access(hdata, urls.get(i));
				if (tmp != null)
					output.addAll(tmp);
			}
		}

		return output;
	}

	public void run() {
		int thai = 0, non = 0;
		String u = null;
		// int d=0, j=0;
		try {
			// LanguageIdentifier lid = new LanguageIdentifier();

			ArcReader reader;
			if (this.filePath.contains(".gz"))
				reader = ArcReaderFactory.getReaderCompressed(new FileInputStream(this.filePath));
			else
				reader = ArcReaderFactory.getReader(new FileInputStream(this.filePath));

			HashSet<String> data = new HashSet<>();

			// skip header

			ArcRecordBase r;
			while ((r = reader.getNextRecord()) != null) {
				u = r.getUrlStr();

				if (u == null)
					continue;

				if (u.contains("filedesc:"))
					continue;

				if (u.contains("computers_and_internet/hardware/smartphone/site/yahoo/")) {
					System.err.println("FOUND " + this.filePath);
				}

				String category = u.replace("http://dir.yahoo.co.jp/", "");

				if (u.contains("WhitePages")) {
					category = category.substring(0, category.lastIndexOf("WhitePages"));
				}

				Source sc = new Source(r.getPayloadContent());
				String jpHeader = sc.getElementById("breadcrumb").getTextExtractor().toString();

				List<Element> links = new ArrayList<>();
				Element rgsite = sc.getElementById("rgsite");
				Element ofsite = sc.getElementById("ofclsite");
				List<Element> urls;
				if (rgsite != null) {
					urls = rgsite.getAllElements(HTMLElementName.A);
					if (urls != null)
						links.addAll(urls);
				}

				if (ofsite != null) {
					urls = ofsite.getAllElements(HTMLElementName.A);
					if (urls != null)
						links.addAll(urls);
				}

				for (Element l : links) {
					String url = l.getAttributeValue("href");
					url = url.toLowerCase();

					if (url.startsWith("http://dir.yahoo")) {

						url = url.replace("http://dir.yahoo.co.jp/", "");
						if (url.startsWith("search"))
							continue;

						if (url.contains("?"))
							url = url.substring(0, url.indexOf("?"));

						data.add(category + "\t" + jpHeader + "\t" + url);
					} else {
						data.add(category + "\t" + jpHeader + "\t" + url);
					}

				}

			}

			buffer.addAll(data);
			reader.close();
		} catch (Exception e) {

			e.printStackTrace();
		}

		complete++;
		logger.info(complete + "/" + total + "\t" + thai + "\t" + non + this.filePath);
	}

	public static String getFileName(String url) {
		// return url;
		if (url.endsWith("/"))
			url = url.substring(0, url.length() - 1);
		return url.substring(url.lastIndexOf("/") + 1);
	}

}
