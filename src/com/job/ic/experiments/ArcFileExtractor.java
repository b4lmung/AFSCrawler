package com.job.ic.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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

import org.apache.log4j.Logger;
import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecordBase;

import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.ml.classifiers.JapaneseTokenizer;
import com.job.ic.ml.classifiers.TokenizedOutput;
import com.job.ic.nlp.services.LanguageIdentifier;
import com.job.ic.nlp.services.Stemmer;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.LoggerProvider;
import net.htmlparser.jericho.Source;

public class ArcFileExtractor extends Thread {

	private String filePath;

	public ArcFileExtractor(String filePath) {
		this.filePath = filePath;
	}

	static int total;
	static int complete = 0;
	public static int count = 0;
	private static Logger logger = Logger.getLogger(ArcFileExtractor.class);
	private static BufferedWriter bw;
	
	public static int limit = 5;
	private static String classLabel = "non";
	private static String tgtlang = "ja"; //en

	private static String arffFile = "non-gaming.arff";
	private static String path = "page-gaming/irrelevant/general/";
	
	
	public static void main(String[] args)  {
		CrawlerConfig.getConfig().setMaxPagePerSite(limit);
		UrlDb.createEnvironment("C:/Users/Tanaphol/Desktop/tmpdb");
		net.htmlparser.jericho.Config.LoggerProvider = LoggerProvider.DISABLED;

		Locale.setDefault(Locale.US);
		System.setProperty("file.encoding", "UTF-8");

		try {
			bw = new BufferedWriter(new FileWriter(arffFile));

			bw.write("@relation 'pageClassifier'\n\n");
			bw.write("@attribute url string\n");
			bw.write("@attribute content string\n");
			bw.write("@attribute title string\n");
			bw.write("@attribute anchor string\n");
			bw.write("@attribute class {non,thai}\n\n");
			bw.write("@data\n");

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		int numThread = 4;
		
		ArrayList<File> f = new ArrayList<>(Arrays.asList(new File(path).listFiles()));
		Collections.shuffle(f);
		
		while(f.size() > 3000){
			f.remove(0);
		}
		
		total = f.size();
		
		
		ExecutorService sv = Executors.newFixedThreadPool(numThread);
		for (File fi : f) {
			sv.submit(new ArcFileExtractor(fi.getPath()));
		}

		sv.shutdown();

		while (!sv.isTerminated()) {
			try {
				Thread.sleep(10 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		try {
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		UrlDb.close();
		FileUtils.deleteDir("tmpdb");
	}

	public void run() {
		
		int count = 0;
		int totalCount = 0;int thai= 0, non=0;
		int d=0, j=0;
		try {
//			LanguageIdentifier lid = new LanguageIdentifier();
			
			
			HashMap<String, Integer> cnt = new HashMap<>();
			ArcReader reader;
			if(this.filePath.contains(".gz"))
				reader = ArcReaderFactory.getReaderCompressed(new FileInputStream(this.filePath));
			else
				reader = ArcReaderFactory.getReader(new FileInputStream(this.filePath));
				
			HashSet<String> md5 = new HashSet<>();

			// skip header
			
			ArcRecordBase r;
			String ttt = null;
			while ((r = reader.getNextRecord()) != null) {


				String u = r.getUrlStr();
				u = HttpUtils.getStaticUrl(u, true);

				if (u == null)
					continue;
				
				if(u.contains("filedesc:"))
					continue;
				
				
				if(u.contains("http://www.moskitodiving.com/Open/Water/Diver/PhiPhi/Phuket/Padi/Equipment/Specialist/"))
					continue;
				
				if(u.contains("/tag/"))
					continue;
				

				if(u.contains("/blog/"))
					continue;
				
				if(u.contains("/blogs/"))
					continue;
				
				if(u.contains("blog."))
					continue;
				
				if(u.contains("blogs."))
					continue;
				
				
				if(u.contains("/tags/"))
					continue;
				
				
				if ((ttt = HttpUtils.getHost(u)) != null) {
					if (ttt.contains("thaifoodandtravel.com")) {
						if (!u.contains("http://www.thaifoodandtravel.com/recipes/"))
							continue;
					}
				}

				
				
				if (ttt.contains("/search") || ttt.contains("video") || ttt.contains("reply"))
					continue;
				if (u.contains("?widgetType") || u.contains("http://nooskitchen.com/?p=942") || u.contains("http://nooskitchen.com/?p=869"))
					continue;
				if (u.contains("/?cat=12") || u.contains("order.cgi") || u.contains("shoptellafriend.asp"))
					continue;

				if (u.contains("banners/") || u.contains("component/mailto/") || u.contains("shopdisplayproducts.asp"))
					continue;

				if (u.contains("&print=1") || u.contains("video") || u.contains("reply"))
					continue;


				d++;
				
				if(u.contains("http://www.bangkoknavi.com"))
					continue;
				
				
				if(UrlDb.getUrlDAO().checkAndAddUrl(u, true))
					continue;

				
				
//				logger.info(u);
				try {
//					String tmp = FileUtils.readStringFromStream(r.getPayloadContent());
					Source sc = new Source(r.getPayloadContent());

					
//					System.err.println(sc.getEncoding());
					List<Element> headere = sc.getAllElements(HTMLElementName.TITLE);
					String data = "", title = "", anchor = "";
					if (headere.size() > 0)
						title += headere.get(0).getTextExtractor().toString();

					List<Element> anchore = sc.getAllElements(HTMLElementName.A);
					for (int k = 0; k < anchore.size(); k++)
						anchor += " " + anchore.get(k).getTextExtractor().toString();

					List<Element> mye = sc.getAllElements(HTMLElementName.BODY);
					if (mye == null || mye.size() == 0) {
						continue;
					}

					
					data += " " + mye.get(0).getTextExtractor().toString();

//					System.out.println(data);
					
					
					title = StringUtils.removeSymbols(title);
					title = StringUtils.removeSpaces(title);
					
					anchor = StringUtils.cleanAnchorText(anchor);
					
					data = StringUtils.removeSymbols(data);
					data = StringUtils.removeSpaces(data);

					double length = data.replaceAll(" ", "").length();

					
					if (length == 0)
						continue;

					String hash = StringUtils.md5(data);
					
					if(md5.contains(hash)){

						j++;
						continue;
					}
					
					md5.add(hash);
					
					
					String result = LanguageIdentifier.identifyLanguage(data);
					
					if(!result.equals(tgtlang))
						continue;
					
					
					if(tgtlang.equals("en")){
//						title = StringUtils.removeNonEngCharacter(title);
//						anchor = StringUtils.removeNonEngCharacter(anchor);
//						data = StringUtils.removeNonEngCharacter(data);

						
						data = StringUtils.removeSpaces(data);
						data = Stemmer.stem(data.trim());
						
						anchor = Stemmer.stem(anchor.trim());
						title = Stemmer.stem(title.trim());
						
						int wordCount = data.split(" ").length;
						if (wordCount < 100) {
							continue;
						}
					}else{
						

						data = StringUtils.removeSpaces(data);
						
						JapaneseTokenizer t = new JapaneseTokenizer();
						TokenizedOutput output = t.tokenizeString(data, true);
						
						if (output.getTokenized().size() < 100)
							continue;
					}
					
					double afterL = data.replaceAll(" ", "").length();
//					if (afterL / length >= 0.5) {
					
						if (classLabel.equals("thai")){
							thai++;
							write(u, data, title, anchor);
						}
						else if (!classLabel.equals("thai")){
							non++;
							write(u, data, title, anchor);
						}

						count++;
//					}

				} catch (Exception e) {
					System.out.println(u);
					System.out.println(count);
					e.printStackTrace();
					logger.error(e.toString() + "\t" + u);
				}

				totalCount++;
			}

			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		complete++;
		logger.info(complete + "/" + total + "\t"  + thai + "\t" + non + "\t" + d + "\t" + j + "\t" + this.filePath);
	}

	public synchronized void write(String url, String body, String title, String anchor) {
		try {
			bw.write("'" + url + "','" + body + "','" + title + "','" + anchor + "'," + classLabel + "\n");
		} catch (IOException e) {
		}
	}

	public static String getFileName(String url) {
		// return url;
		if (url.endsWith("/"))
			url = url.substring(0, url.length() - 1);
		return url.substring(url.lastIndexOf("/") + 1);
	}

}
