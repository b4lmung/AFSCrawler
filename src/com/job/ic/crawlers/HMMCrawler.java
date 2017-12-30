package com.job.ic.crawlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.jwat.arc.ArcReader;
import org.jwat.arc.ArcReaderFactory;
import org.jwat.arc.ArcRecordBase;

import com.job.ic.crawlers.daos.ARCFileWriter;
import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.models.PageObject;
import com.job.ic.crawlers.models.QueueProxyObj;
import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.ml.classifiers.KMeansForHMMCrawler;
import com.job.ic.ml.classifiers.MyTextTokenizer;
import com.job.ic.ml.classifiers.PageClassifier;
import com.job.ic.ml.classifiers.WekaClassifier;
import com.job.ic.nlp.services.Checker;
import com.job.ic.nlp.services.LexToChecker;
import com.job.ic.nlp.Document;
import com.job.ic.nlp.TFIDF;
import com.job.ic.nlp.TFIDFVector;
import com.job.ic.proxy.ProxyService;
import com.job.ic.proxy.model.ProxyModel;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.LoggerProvider;
import net.htmlparser.jericho.Source;
import weka.clusterers.SimpleKMeans;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.stemmers.IteratedLovinsStemmer;
import weka.core.stopwords.Rainbow;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class HMMCrawler {

	public static String path = "gaming";
	public static final int numStates = 3; // fixed states
	public static int numClusters = 3;
	public static TFIDF model;
	public static TFIDFVector[] centroids;
	private static Logger logger = Logger.getLogger(HMMCrawler.class);

	private static Double[] init = null;
	private static Double[][] a = null;
	private static Double[][] b = null;

	public static void main(String[] args) throws Exception {
		 
		System.out.println("Args: [topic {estate,diving,tourism,gaming,baseball}]");
		path = args[0];
		
		// train();
		// estate
		
//		String seq = "000001000011000001";
//		Double[] p = findNextProb(seq, init, a, b, null);

		// */

		// diving
		 crawl(null);

	}

	public static void train() {
		net.htmlparser.jericho.Config.LoggerProvider = LoggerProvider.DISABLED;

		findingLStates();
		extractData(path, new HashSet<>(Arrays.asList(FileUtils.readFile("l0-" + path + ".txt"))));
		clustering();
		mergeClusteringResults();
		findABMatrix();
	}

	public static void initialize(String path) {
		
		

		if (CrawlerConfig.getConfig().getPageModel().contains("estate"))
			path = "estate";

		if (CrawlerConfig.getConfig().getPageModel().contains("diving"))
			path = "diving";

		if (CrawlerConfig.getConfig().getPageModel().contains("tourism"))
			path = "tourism";

		model = (TFIDF) FileUtils.getObjFile(path + "-tfidf.model");
		centroids = (TFIDFVector[]) FileUtils.getObjFile(path + "-tfidf.centroids");

		System.out.println("loaded");
		System.out.println(centroids.length);
		
//		init = parseArray("[0.21833601584746812, 0.07787544880524948, 0.7037885353472824]");
//		a = parseMatrix("[[0.9151328255867005, 0.044844662166437434, 0.0400225122468621], [0.15376443167651363, 0.7301642365439861, 0.11607133177950024], [0.0, 0.007905562751243213, 0.9920944372487568]]");
//		b = parseMatrix("[[0.9998201115308508, 0.0, 1.7988846914912754E-4], [0.0022945446031374384, 0.10442519316319362, 0.893280262233669], [0.0, 0.0, 1.0], [0.0, 0.0, 1.0]]");
//		
//		if(true)
//			return;

		if (path.contains("estate")) {
			init = parseArray("[0.05629061301182181, 0.101464025679167, 0.8422453613090112]");
			a = parseMatrix(
					"[[0.5454545454545454, 0.26634897360703813, 0.18819648093841643], [0.06257707509881423, 0.738866930171278, 0.1985559947299078], [0.0, 0.0127057742618136, 0.9872942257381864]]");
			b = parseMatrix("[[0.988, 0.008, 0.004], [0.0017152658662092624, 0.11217838765008577, 0.886106346483705], [0.0, 0.0, 1.0]]");
		} else if (path.contains("diving")) {
			// diving
			init = parseArray("[0.1347603121516165, 0.17449275362318842, 0.6907469342251951]");
			a = parseMatrix(
					"[[0.7636685399777855, 0.17640622668722336, 0.05992523333499113], [0.13789358020637307, 0.7687038744416518, 0.0934025453519751], [0.0, 0.009913638646406006, 0.990086361353594]]");
			b = parseMatrix("[[1.0, 0.0, 0.0], [0.0, 0.30930930930930933, 0.6906906906906907], [0.0, 0.23255813953488372, 0.7674418604651163]]");
		} else if (path.contains("tourism")) {
			// tourism
			init = parseArray("[0.21833601584746812, 0.13232016837934876, 0.6493438157731831]");
			a = parseMatrix(
					"[[0.8683393583640551, 0.0828328382070463, 0.04882780342889867], [0.1619858042024785, 0.7016923440145072, 0.13632185178301426], [0.0, 0.009878947308485077, 0.990121052691515]]");
			b = parseMatrix("[[1.0, 0.0, 0.0], [6.808896958692692E-4, 0.1082614616432138, 0.891057648660917], [0.0027114341756086305, 0.18610822660666898, 0.8111803392177224]]");

		} else if (path.contains("gaming")) {
			// tourism
			init = parseArray("[0.14789980040815412, 0.1061873472225337, 0.7459128523693122]");
			a = parseMatrix(
					"[[0.7722530101650483, 0.1412878652850025, 0.08645912454994914], [0.17654758324143077, 0.6335761788590716, 0.1898762378994976], [0.0, 0.005431894461471114, 0.9945681055385289]]");
			b = parseMatrix("[[1.0, 0.0, 0.0], [0.0, 0.029949436017113962, 0.970050563982886], [0.012475171620726906, 0.14963236575251768, 0.8378924626267554]]");

		} else if (path.contains("baseball")) {
			// tourism
			init = parseArray("[0.1405792651724491, 0.11134991300788047, 0.7480708218196704]");
			a = parseMatrix(
					"[[0.6754089846793041, 0.21965893068192022, 0.10493208463877571], [0.18777131293558233, 0.6434516416633598, 0.16877704540105784], [0.0, 0.014241307222166917, 0.9857586927778331]]");
			b = parseMatrix("[[1.0, 0.0, 0.0], [0.0, 0.0, 1.0], [1.904813736428202E-4, 0.1419902582383194, 0.8578192603880378]]");

		}

	}

	public static String findCurrentSeq(Double[] simResults, String previousSeq) {
		// emission output (for hmm)
		int hmmCluster = closestCluster(simResults);

		// current sequence
		return previousSeq + hmmCluster;
	}

	public static int closestCluster(Double[] simResults) {
		int index = 0;
		double closest = simResults[0];

		for (int i = 1; i < numClusters; i++) {
			if (closest < simResults[i]) {
				index = i;
				closest = simResults[i];
			}
		}

		return index;
	}

	public static HashMap<LinksModel, Double> parallelSimLinks(ArrayList<LinksModel> links) {

		ConcurrentHashMap<LinksModel, Double> data = new ConcurrentHashMap<>();

		ExecutorService sv = Executors.newFixedThreadPool(16);

		for (LinksModel l : links) {
			sv.submit(() -> {
				TFIDFVector input = model.extractTFIDFFeatureTFIDFVector(new Document("tmp", l.getAnchorText()), false);
				double cos = TFIDF.cosineSim(input, centroids[0]);
				data.put(l, cos);
			});
		}

		sv.shutdown();

		try {
			sv.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		HashMap<LinksModel, Double> output = new HashMap<>();
		output.putAll(data);
		data.clear();

		return output;
	}

	public static Double[] simCluster(String srcPage) {
		TFIDFVector input = model.extractTFIDFFeatureTFIDFVector(new Document("tmp", srcPage), false);
		Double[] results = new Double[numClusters];
		if (centroids != null) {
			for (int i = 0; i < centroids.length; i++) {
				double cos = TFIDF.cosineSim(input, centroids[i]);
				results[i] = cos;
			}

			return results;
		}

		return null;
	}

	public static Double findCurrentProb(int lState, String seq, Double[] init, Double[][] aMatrix, Double[][] emission) {
		int currEmission = Integer.parseInt(seq.substring(seq.length() - 1));

		if (seq.length() == 1) {
			return init[lState] * emission[currEmission][lState];
		} else {
			String newSeq = seq.substring(0, seq.length() - 1);
			double result = 0;
			for (int i = 0; i < numStates; i++) {
				result += findCurrentProb(i, newSeq, init, aMatrix, emission) * aMatrix[i][lState];
			}

			return result * emission[currEmission][lState];
		}

	}

	public static Double[] findNextProb(String seq, Double[] init, Double[][] aMatrix, Double[][] emission, Double[] previousCalculation) {
		double result = 0;
		Double[] probs = new Double[numStates];
		Double[] currentProbs = new Double[numStates];

		if (previousCalculation == null) {
			for (int i = 0; i < numStates; i++) {
				currentProbs[i] = findCurrentProb(i, seq, init, aMatrix, emission);
			}
		} else {
			int crawledResult = Integer.parseInt(seq.substring(seq.length() - 1));
			for (int i = 0; i < numStates; i++) {
				currentProbs[i] = previousCalculation[i] * emission[crawledResult][i];
			}
		}

		for (int j = 0; j < numStates; j++) {
			result = 0;
			for (int i = 0; i < numStates; i++) {
				// double currProb = findCurrentProb(i, seq, init, aMatrix,
				// emission);
				result += currentProbs[i] * aMatrix[i][j];
			}
			probs[j] = result;
		}

		return probs;
	}

	public static Double[] parseArray(String data) {
		// [0.03350720036285293, 0.03543485655970065, 0.006463317836489398,
		// 0.924594625240957]

		data = data.substring(1, data.length() - 1);
		String[] tmp = data.split(",");
		Double[] output = new Double[tmp.length];
		for (int i = 0; i < tmp.length; i++) {
			output[i] = Double.parseDouble(tmp[i].trim());
		}

		return output;
	}

	public static Double[][] parseMatrix(String data) {
		data = data.substring(1, data.length() - 1);

		data = data.replace("], [", "\n");
		data = data.replace("[", "");
		data = data.replace("]", "");

		// each lines
		String[] row = data.split("\n");
		int numRow = row.length;
		int numCol = row[0].split(",").length;

		Double[][] output = new Double[numRow][numCol];

		for (int i = 0; i < row.length; i++) {
			String[] col = row[i].split(",");
			for (int j = 0; j < col.length; j++) {
				output[i][j] = Double.parseDouble(col[j]);
			}
		}

		return output;
	}

	public static void findingLStates() {
		ProxyService.setupProxy("proxy-train-" + path);
		ProxyService.exportUrls("proxy-" + path + ".txt");

		HashSet<String> rel = new HashSet<>();
		HashSet<String> remains = new HashSet<>();

		// finding l0
		for (String s : FileUtils.readFile("proxy-" + path + ".txt")) {
			String[] tmp = s.split("\t");
			double score = Double.parseDouble(tmp[1]);
			if (score > 0.5)
				rel.add(tmp[0].trim());
			else
				remains.add(tmp[0].trim());
		}

		System.out.println("Remains size " + remains.size());

		// rel is l0, a page that cite to rel is l1
		HashSet<String> l1 = new HashSet<>();

		ArrayList<String> allUrls = ProxyService.getAll();
		for (String s : allUrls) {

			if (rel.contains(s)) {
				continue;
			}

			ProxyModel m = ProxyService.retreiveContentByURL(s, null);

			if (m != null) {
				for (LinksModel l : m.getLinks()) {
					if (rel.contains(l.getLinkUrl())) {
						l1.add(s);
						break;
					}
				}
			}
		}

		ProxyService.terminateProxy();

		System.out.println("L0\t" + rel.size());
		System.out.println("L1\t" + l1.size());

		remains.removeAll(rel);
		remains.removeAll(l1);
		System.out.println("L2\t" + remains.size());

		FileUtils.writeTextFile("l0-" + path + ".txt", rel, false);
		FileUtils.writeTextFile("l1-" + path + ".txt", l1, false);
		FileUtils.writeTextFile("l2-" + path + ".txt", remains, false);

		// clustering && label
		// 1.) extract data 2.) K-Means 3.) export the results

		// extractData(path, rel);

		// */
		// kMeans

		// calculate probabilities
		// run the crawler ??

		// hop 0 -> hop 1

	}

	public static void clustering() {
		try {
			DataSource ds = new DataSource("kmeans-" + path + "-non.arff");
			Instances org = ds.getDataSet();
			Remove rm = new Remove();
			rm.setAttributeIndices("1,3");
			rm.setInputFormat(ds.getDataSet());

			System.out.println("removing");
			// remove
			Instances removed = Filter.useFilter(ds.getDataSet(), rm);

			System.out.println("vectorized");
			StringToWordVector sv = new StringToWordVector(30000);
			sv.setAttributeIndices("first-last");
			sv.setMinTermFreq(3);
			sv.setIDFTransform(true);
			sv.setTFTransform(true);
			sv.setOutputWordCounts(true);
			sv.setLowerCaseTokens(true);
			sv.setStemmer(new IteratedLovinsStemmer());
			sv.setTokenizer(new MyTextTokenizer());
			sv.setStopwordsHandler(new Rainbow());
			sv.setInputFormat(removed);
			Instances vectorized = Filter.useFilter(removed, sv);

			System.out.println("clustering");
			SimpleKMeans km = new SimpleKMeans();
			km.setNumClusters(numClusters - 1);
			km.setPreserveInstancesOrder(true);

			km.buildClusterer(vectorized);

			int[] assignments = km.getAssignments();

			try (BufferedWriter bw = FileUtils.getBufferedFileWriter("kmeans-" + path + "-non-results.txt")) {

				// write data
				for (int i = 0; i < org.numInstances(); i++) {
					Instance ins = org.get(i);
					bw.write(assignments[i] + "\n");
				}

			} catch (Exception e) {

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void mergeClusteringResults() {

		try {

			HashMap<Integer, ArrayList<Document>> corpus = new HashMap<>();

			for (int i = 0; i < numClusters; i++) {
				corpus.put(i, new ArrayList<>());
			}

			ArrayList<String> output = new ArrayList<>();
			DataSource ds = new DataSource("kmeans-" + path + "-non.arff");
			String[] results = FileUtils.readFile("kmeans-" + path + "-non-results.txt");

			// DataSource results = new DataSource("kmeans-" + path +
			// "-non-results.arff");

			Instances sc = ds.getDataSet();

			for (int i = 0; i < sc.numInstances(); i++) {
				String url = sc.get(i).stringValue(sc.attribute("url"));
				String clusterName = results[i];
				clusterName = clusterName.replace("cluster", "");
				clusterName = String.valueOf(Integer.parseInt(clusterName) + 1);
				String data = sc.get(i).stringValue(sc.attribute("content"));
				output.add(url + "\t" + clusterName);

				int cluster = Integer.parseInt(clusterName);
				corpus.get(cluster).add(new Document(url, data));
			}

			ds = new DataSource("kmeans-" + path + "-rel.arff");

			sc = ds.getDataSet();
			for (int i = 0; i < sc.numInstances(); i++) {
				String url = sc.get(i).stringValue(sc.attribute("url"));
				output.add(url + "\t0");
				String data = sc.get(i).stringValue(sc.attribute("content"));
				corpus.get(0).add(new Document(url, data));
			}

			FileUtils.writeTextFile("kmeans-" + path + "-results.txt", output, false);
			int[] start = new int[numClusters];
			int[] end = new int[numClusters];

			ArrayList<Document> all = new ArrayList<>();
			for (int i = 0; i < numClusters; i++) {
				start[i] = all.size();
				all.addAll(corpus.get(i));
				end[i] = all.size() - 1;
				System.out.println(start[i] + "\t" + end[i]);
			}

			TFIDF model = new TFIDF(all, new MyTextTokenizer());
			ArrayList<TFIDFVector> allVectors = model.getTFIDFVector();

			TFIDFVector[] centroids = new TFIDFVector[numClusters];

			System.out.println(allVectors.size());
			for (int i = 0; i < numClusters; i++) {
				List<TFIDFVector> sub;

				System.out.println(start[i] + "\t" + end[i]);
				sub = allVectors.subList(start[i], end[i]);

				centroids[i] = new TFIDFVector(sub);
			}

			System.out.println("saving model & centroids");

			FileUtils.saveObjFile(model, path + "-tfidf.model");
			FileUtils.saveObjFile(centroids, path + "-tfidf.centroids");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void findABMatrix() {
		// load l_states data
		HashMap<Integer, HashSet<String>> lStates = new HashMap<>();

		Double[] pi = new Double[numStates];
		double all = 0;
		for (int i = 0; i < numStates; i++) {
			String[] data = FileUtils.readFile("l" + i + "-" + path + ".txt");
			lStates.put(i, new HashSet<>());

			for (String s : data) {
				lStates.get(i).add(s);
			}
			pi[i] = (double) lStates.get(i).size();
			all += pi[i];
		}

		for (int i = 0; i < numStates; i++)
			pi[i] /= all;

		logger.info("-------------- Initial ---------------");
		logger.info(Arrays.deepToString(pi));

		ProxyService.setupProxy("proxy-train-" + path);

		Double[][] aMatrix = new Double[numStates][numStates];

		for (int i = 0; i < numStates; i++) {
			double total = 0;
			for (int j = 0; j < numStates; j++) {

				aMatrix[i][j] = 0.0;
				// find a_ij
				// every web pages from s
				HashSet<String> setI = lStates.get(i);
				HashSet<String> setJ = lStates.get(j);

				double target = 0;
				for (String s : setI) {
					ProxyModel m = ProxyService.retreiveContentByURL(s, null);
					if (m != null) {
						for (LinksModel l : m.getLinks()) {
							String destUrl = l.getLinkUrl();

							if (setJ.contains(destUrl)) {
								// increment Aij
								aMatrix[i][j]++;
							}

						}

					}
				}

				// Aij = target/total;
				total += aMatrix[i][j];
			}

			for (int k = 0; k < numStates; k++) {
				aMatrix[i][k] /= total;
			}
		}

		logger.info("-------------- Transition ---------------");
		logger.info(Arrays.deepToString(aMatrix));

		// load cluster data
		HashMap<String, Integer> cStates = new HashMap<>();
		for (int i = 0; i < numClusters; i++) {
			String[] data = FileUtils.readFile("kmeans-" + path + "-results.txt");
			for (String s : data) {
				String[] tmp = s.split("\t");
				cStates.put(tmp[0], Integer.parseInt(tmp[1]));
			}
		}

		logger.info("-------------- Emission ---------------");

		Double[][] emission = new Double[numClusters][numStates];
		int[] count = new int[numClusters];

		// init
		for (int i = 0; i < numClusters; i++) {
			count[i] = 0;
			for (int j = 0; j < numStates; j++)
				emission[i][j] = 0.0;
		}

		for (int i = 0; i < numStates; i++) {
			HashSet<String> setI = lStates.get(i);
			for (String s : setI) {
				String url = StringUtils.cleanUrlDataForPrediction(s);
				// cStates.containsKey(key)
				if (cStates.containsKey(url)) {
					int results = cStates.get(url);
					emission[results][i]++;
					count[results]++;
				}

			}

		}

		// normalize
		for (int i = 0; i < numClusters; i++) {
			for (int j = 0; j < numStates; j++)
				emission[i][j] /= count[i];
		}

		logger.info(Arrays.deepToString(emission));

		ProxyService.terminateProxy();

	}

	public static void extractData(String path, HashSet<String> rel) {
		ArrayList<File> files = new ArrayList<>(Arrays.asList(new File("dl-train-" + path).listFiles()));

		ArrayList<String> relData = new ArrayList<>();
		ArrayList<String> nonData = new ArrayList<>();

		for (File f : files) {

			try {
				ArcReader reader;
				if (f.getName().contains(".gz"))
					reader = ArcReaderFactory.getReaderCompressed(new FileInputStream(f));
				else
					reader = ArcReaderFactory.getReader(new FileInputStream(f));

				ArcRecordBase r;
				while ((r = reader.getNextRecord()) != null) {
					try {
						String url = r.getUrlStr();
						url = StringUtils.cleanUrlDataForPrediction(url);
						url = url.replace(",", "");

						Source sc = new Source(r.getPayloadContent());

						List<Element> mye = sc.getAllElements(HTMLElementName.BODY);
						if (mye == null || mye.size() == 0) {
							continue;
						}

						String data = " " + mye.get(0).getTextExtractor().toString();
						data = StringUtils.removeSymbols(data);
						data = StringUtils.removeSpaces(data);

						if (rel.contains(url))
							relData.add(String.format("'%s','%s',%s\n", url, data, "thai"));
						else
							nonData.add(String.format("'%s','%s',%s\n", url, data, "non"));
					} catch (Exception ex) {

					}
				}
			} catch (IOException ee) {

			}

		}

		try (BufferedWriter bw = FileUtils.getBufferedFileWriter("kmeans-" + path + "-rel.arff")) {
			bw.write("@relation 'cluster'\n\n");
			bw.write("@attribute url string\n");
			bw.write("@attribute content string\n");
			bw.write("@attribute class {non,thai}\n\n");
			bw.write("@data\n");

			// write data

			for (String s : relData) {
				bw.write(s);
			}
		} catch (Exception e) {

		}

		try (BufferedWriter bw = FileUtils.getBufferedFileWriter("kmeans-" + path + "-non.arff")) {
			bw.write("@relation 'cluster'\n\n");
			bw.write("@attribute url string\n");
			bw.write("@attribute content string\n");
			bw.write("@attribute class {non,thai}\n\n");
			bw.write("@data\n");

			// write data

			for (String s : nonData) {
				bw.write(s);
			}
		} catch (Exception e) {

		}
	}

	public static int countPage = 0;

	private static double rel = 0;
	private static double non = 0;

	public static String seedPath = CrawlerConfig.getConfig().getSeedPath();

	public static boolean isBFS = false;
	// public static UrlDAO ud;
	private static long limitPage = CrawlerConfig.getConfig().getLimitDownloadedPages();
	private static Checker page;

	public static PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager();
	public static CloseableHttpClient client;

	public static synchronized void addPage(final double score, final String url) {

		if (score > 0.5) {
			rel++;
		} else {
			non++;
		}

		if (HMMCrawler.rel + HMMCrawler.non > HMMCrawler.limitPage) {
			System.exit(0);
		}

	}

	public static String progress() {
		double hv = rel / (rel + non);
		// double total = rel + non;
		hv *= 100;

		return String.format("%d\t%.3f", (int) (rel + non), hv);
	}

	public static void crawl(final String[] args) throws Exception {

		net.htmlparser.jericho.Config.LoggerProvider = LoggerProvider.DISABLED;

		RequestConfig config;

		if (CrawlerConfig.getConfig().getProxyServer().trim().length() != 0) {
			// set proxy
			HttpHost proxy = new HttpHost(CrawlerConfig.getConfig().getProxyServer(), CrawlerConfig.getConfig().getProxyPort());
			config = RequestConfig.custom().setStaleConnectionCheckEnabled(false).setProxy(proxy).setSocketTimeout((int) CrawlerConfig.getConfig().getSoTimeout())
					.setConnectTimeout((int) CrawlerConfig.getConfig().getTimeout()).build();
		} else {
			config = RequestConfig.custom().setStaleConnectionCheckEnabled(false).setSocketTimeout((int) CrawlerConfig.getConfig().getSoTimeout())
					.setConnectTimeout((int) CrawlerConfig.getConfig().getTimeout()).build();
		}

		// handle ssl connection
		if (CrawlerConfig.getConfig().isAllowHttps()) {
			SSLContextBuilder builder = SSLContexts.custom();
			builder.loadTrustMaterial(null, (chain, authType) -> true);
			SSLContext sslContext = builder.build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new X509HostnameVerifier() {

				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}

				@Override
				public void verify(String arg0, String[] arg1, String[] arg2) throws SSLException {
				}

				@Override
				public void verify(String arg0, X509Certificate arg1) throws SSLException {
				}

				@Override
				public void verify(String arg0, SSLSocket arg1) throws IOException {
				}
			});

			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().register("https", sslsf).register("http", new PlainConnectionSocketFactory())
					.build();
			mgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

		}

		mgr.setMaxTotal(300);
		mgr.setDefaultMaxPerRoute(10);
		client = HttpClients.custom().setUserAgent(CrawlerConfig.getConfig().getUsrAgent()).setConnectionManager(mgr).setDefaultRequestConfig(config).build();

		page = null;
		if (CrawlerConfig.getConfig().getCheckerType() != null && CrawlerConfig.getConfig().getCheckerType().trim().length() > 0) {

			if (CrawlerConfig.getConfig().getCheckerType().equals("weka")) {
				try {
					page = new PageClassifier();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			} else {
				page = new LexToChecker();
			}

		}

		softFocusedThread();

		System.out.println(String.valueOf(HMMCrawler.rel) + "\t" + HMMCrawler.non);

	}

	public static void softFocusedThread() {

		initialize(path);

		UrlDb.createEnvironment("urlDb");
		ARCFileWriter writer = null;
		try {
			writer = new ARCFileWriter();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Locale.setDefault(Locale.US);
		System.setProperty("file.encoding", "UTF-8");

		PriorityQueue<QueueProxyObj> queue = new PriorityQueue<>(1, (o1, o2) -> {
			// int p1 = (int) (o1.getScore() * 100);
			// int p2 = (int) (o2.getScore() * 100);

			// return -1 * Integer.compare(p1, p2);

			return -1 * Double.compare(o1.getScore(), o2.getScore());
		});

		ArrayList<WebsiteSegment> segments = FileUtils.readSegmentFile(seedPath);
		System.out.println(segments.size());
		for (int i = 0; i < segments.size(); i++) {
			WebsiteSegment s = segments.remove(0);
			for (String url : s.getUrls()) {
				queue.add(new QueueProxyObj(url, null, (segments.size() - i) * 100.0, -1, -1));
			}
		}

		logger.info("start");

		String host = null;
		HtmlParser ps = new HtmlParser();

		while (queue.size() > 0) {
			QueueProxyObj qObj = queue.remove();
			if (qObj == null)
				continue;

			String srcSeq = qObj.getExtra();
			Double[] previousCalculation = qObj.getPreviousCalculation();

			String url = qObj.getUrl();

			if (url == null)
				continue;

			double score = 0.0;

			ArrayList<LinksModel> links = null;

			byte[] p = download(url);

			if (p == null)
				continue;

			if (writer != null)
				writer.writeRecord(new PageObject(p, url));

			links = ps.parse(p, url);
			score = page.checkHtmlContent(p);

			if (url.toLowerCase().contains("moscow"))
				score = 0;

			if (url.toLowerCase().contains("canada"))
				score = 0;

			if (url.toLowerCase().contains("florida"))
				score = 0;

			if (url.toLowerCase().contains("byowner."))
				score = 0;

			if (url.toLowerCase().contains("kiev"))
				score = 0;

			if (url.toLowerCase().contains("whistlerforsale"))
				score = 0;

			if (url.toLowerCase().contains("hellenic-realty"))
				score = 0;

			if (url.toLowerCase().contains("kiev"))
				score = 0;

			if (url.toLowerCase().contains("whistlerforsale"))
				score = 0;

			if (url.toLowerCase().contains("whistler"))
				score = 0;

			if (url.toLowerCase().contains("landntours"))
				score = 0;

			if (url.toLowerCase().contains("intlistings.com"))
				score = 0;

			if (url.toLowerCase().contains("united-states"))
				score = 0;

			if (url.toLowerCase().contains("texas"))
				score = 0;

			// if (url.toLowerCase().contains("anantara."))
			// score = 0;

			if (url.toLowerCase().contains("justrealestate"))
				score = 0;

			if (links == null)
				continue;

			// predict hmm prob
			// find emission
			Double[] clustering = simCluster(new String(p));
			String currSeq = findCurrentSeq(clustering, srcSeq);

			HashMap<LinksModel, Double> map = parallelSimLinks(links);
			// System.err.println(map.size() + "\t" + queue.size());
			Double[] hmmScore = findNextProb(currSeq, init, a, b, previousCalculation);

			addPage(score, url);
			// HMMCrawler.logger
			// .info(String.format("%s\t%.2f\t%s\tParent:\t%.2f\tOutlinks:\t",
			// url, score, progress(), qObj.getScore(),
			// links!=null?links.size():0));

			logger.info(String.format("DOWNLOADED\t%.2f\tDepth:%d\tDistance:%d\t%s\t%d\tHV:\t%s\tqScore:\t%.3f\tpScore:%.3f\tsrcScore:%.3f\tqSize:\t%d", score, qObj.getDepth(), 0, url,
					(int) (rel + non), progress(), qObj.getScore(), 0.0, 0.0, queue.size()));

			for (final LinksModel l : links) {

				double destScore = clustering[0];
				destScore = (score + hmmScore[0]) * 0.5;
				// destScore = ((clustering[0] +
				// (map.containsKey(l)==false?0.0:map.get(l)))/2.0 +
				// hmmScore[0])/2.0;
				// destScore = ((score +
				// (map.containsKey(l)==false?0.0:map.get(l)))/2.0 +
				// hmmScore[0])/2.0;

				// System.out.println("check " + l.getLinkUrl());
				if (UrlDb.getUrlDAO().checkAndAddUrl(l.getLinkUrl(), false))
					continue;

				// System.out.println("pass");

				QueueProxyObj newUrl;
				if (score > 0.5)
					newUrl = new QueueProxyObj(l.getLinkUrl(), url, destScore, qObj.getDepth() + 1, 0);
				else
					newUrl = new QueueProxyObj(l.getLinkUrl(), url, destScore, qObj.getDepth() + 1, qObj.getDistanceFromRelevantPage() + 1);

				// setCurrentSeq to all extracted urls;
				newUrl.setExtra(currSeq);
				newUrl.setPreviousCalculation(hmmScore);
				// System.out.println("enqueue");
				queue.add(newUrl);
			}

		}

		if (writer != null)
			writer.close();

		HMMCrawler.logger.info("finished");

	}

	public static byte[] download(String url) {

		InputStream is = null;

		if (url.indexOf("http://") < 0) {
			url = "http://" + url;
		}

		if (UrlDb.getUrlDAO().checkAndAddUrl(url, true)) {
			return null;
		}

		HttpGet request = null;

		try {
			request = new HttpGet(url);
			HttpResponse response = client.execute(request);
			int status = response.getStatusLine().getStatusCode();

			if (status != 200 && status != 302) {
				request.abort();
				return null;
			}

			HttpEntity entity = response.getEntity();
			String contentType = entity.getContentType().getValue();

			if (contentType != null) {
				contentType = contentType.toLowerCase();
			}

			if (!HttpUtils.isDownloadFileType(HttpUtils.getContentType(contentType), CrawlerConfig.getConfig().getAllowFileType())) {
				request.abort();
				return null;
			}

			is = entity.getContent();

			String header = response.getStatusLine() + "\n" + response.getFirstHeader("Date") + "\n" + response.getFirstHeader("Server") + "\n" + response.getFirstHeader("Content-type")
					+ " Last-modified: " + response.getFirstHeader("Last-modified") + "\n" + "Content-length: " + entity.getContentLength() + "\n\n";

			byte[] b = writeStreamToByteArray(header, is, CrawlerConfig.getConfig().getMaxFileSize());

			if (b == null) {
				request.abort();
				return null;
			}

			is.close();
			EntityUtils.consume(entity);

			return b;

		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			if (request != null) {
				request.abort();
				request = null;
			}

			final byte[] b = null;
			is = null;
		}

		return null;
	}

	private static byte[] writeStreamToByteArray(String header, InputStream fis, long limit) {
		try (ByteArrayOutputStream tmp = new ByteArrayOutputStream()) {

			tmp.write(header.getBytes());
			byte[] buffer = new byte[1024];
			int n;

			while ((n = fis.read(buffer)) != -1) {

				tmp.write(buffer, 0, n);
				if (tmp.size() > limit) {
					return null;
				}

				if (Thread.currentThread().isInterrupted())
					break;
			}

			buffer = null;
			buffer = tmp.toByteArray();
			tmp.close();
			return buffer;
		} catch (IOException e) {
			// e.printStackTrace();
			logger.error(e.getMessage());
		}

		return null;
	}
}
