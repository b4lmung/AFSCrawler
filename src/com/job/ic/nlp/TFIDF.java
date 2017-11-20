package com.job.ic.nlp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.job.ic.ml.classifiers.MyTextTokenizer;
import com.job.ic.nlp.services.WordNet;

public class TFIDF implements Serializable {
	private static Logger logger = Logger.getLogger(TFIDF.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 2153930340789962902L;
	private HashMap<String, Integer> overallFreq;
	private ArrayList<TFIDFVector> TFIDFVectors;
	private IDF idf;
	private MyTextTokenizer tokenizer;
	private TFIDFVector modelCentroid;
	
	
	public static void main(String[] args) {

		// TFIDF t = (TFIDF) FileUtils.getObjFile("tourism.bin");

		// LinkedBlockingQueue<Document> docs = (LinkedBlockingQueue<Document>)
		// FileUtils.getObjFile("output.bin");

		logger.info("loaded");

		String s1 = "sony canon nikon nikon sample";
		String s2 = "sony canon pana pana example example example";
		String s3 = "this is another sample";
		s3 = "Comprehensive another example up-to-date news coverage, aggregated from sources all over the world by Google News.";
		// s3 = s2;

		Document l1 = new Document("http://b1", s1);
		Document l2 = new Document("http://b2", s2);
		Document l3 = new Document("http://b3", s3);

		ArrayList<Document> corpus = new ArrayList<>();
		corpus.add(l1);
		corpus.add(l2);

		TFIDF t = new TFIDF(corpus, new MyTextTokenizer());

		TFIDFVector v1 = t.extractTFIDFFeatureTFIDFVector(l1, false);
		TFIDFVector v2 = t.extractTFIDFFeatureTFIDFVector(l2, false);
		TFIDFVector v3 = t.extractTFIDFFeatureTFIDFVector(l3, false);

		long start = System.currentTimeMillis();
		System.out.println("Cosine\t" + cosineSim(v1, v2));
		System.out.println("Time " + (System.currentTimeMillis() - start));
		
		
		
		
		//add docs to tfidf
		
		//extract centroid for each cluster from tfidf;
		
		//save centroids
		
		

	}

	public TFIDF(ArrayList<Document> corpus, MyTextTokenizer tokenizer) {
		this.tokenizer = tokenizer;
		this.idf = new IDF();
		this.overallFreq = new HashMap<>();
		
		this.TFIDFVectors = new ArrayList<>();
		assert (this.tokenizer != null);

		int count = 0;
		for (Document s : corpus) {
//			if (s.getData() == null || s.getData().length() == 0)
//				continue;

			TFIDFVector n = new TFIDFVector(this.idf, s, tokenizer, true);
			this.TFIDFVectors.add(n);
			
			count++;
			if (count % 100 == 0) {
				logger.info(count + "/" + corpus.size());
			}
		}

		this.modelCentroid = new TFIDFVector(this.TFIDFVectors);

		// optimize
		for (String s : this.modelCentroid.getAllTerms()) {
			if (this.idf.getIDFValue(s) == 0 || this.modelCentroid.getTFIDFValue(s) == 0) {
				System.out.println(this.modelCentroid.getTFIDFValue(s) + " Remove term " + s);
				this.modelCentroid.removeTerm(s);
			}
		}
	}

	public TFIDFVector extractTFIDFFeatureTFIDFVector(Document s, boolean incIDF) {
		if (s == null || s.getData() == null || s.getData().trim().length() == 0)
			return null;

		return new TFIDFVector(this.idf, s, tokenizer, incIDF);
	}

	public static double euclideanSim(TFIDFVector v1, TFIDFVector v2) {
		double distance = 0;

		HashSet<String> terms = new HashSet<>();
		terms.addAll(v1.getAllTerms());
		terms.addAll(v2.getAllTerms());

		for (String s : terms) {
			// System.out.println(s + "\t" + v1.getValue(s) + "\t" +
			// v2.getValue(s));

			if (v1.containsTerm(s) && v2.containsTerm(s)) {
				distance += Math.pow(v1.getTFIDFValue(s) - v2.getTFIDFValue(s), 2);
			} else if (v1.containsTerm(s) && !v2.containsTerm(s)) {
				distance += Math.pow(v1.getTFIDFValue(s), 2);
			} else if (!v1.containsTerm(s) && v2.containsTerm(s)) {
				distance += Math.pow(v2.getTFIDFValue(s), 2);
			}
		}

		// System.out.println(distance);
		distance = Math.sqrt(distance / 2);
		return 1 - distance;
	}

	public static double cosineSim(TFIDFVector v1, TFIDFVector v2) {

		HashSet<String> terms = new HashSet<>();
		terms.addAll(v1.getAllTerms());
		terms.retainAll(v2.getAllTerms());

		double score = 0;
		double frac1 = 0, frac2 = 0;

		for (String s : terms) {
			double vv1 = v1.getTFIDFValue(s);
			if(vv1 <= 0)
				continue;
			
			double vv2 = v2.getTFIDFValue(s);
			if(vv2 <= 0)
				continue;
			
			score += vv1*vv2;
			frac1 += Math.pow(vv1, 2);
			frac2 += Math.pow(vv2, 2);
		}

		double frac = Math.sqrt(frac1) * Math.sqrt(frac2);
		if (frac == 0)
			return 0;

		return score / frac;
	}

	public static double semanticSim(TFIDFVector v1, TFIDFVector v2) {
		double sim = 0;
		double frac1 = 0;
		double frac2 = 0;
		double semf1 = 0;
		double wn = 0;

		for (String i : v1.getAllTerms()) {
			for (String j : v2.getAllTerms()) {

				if (i.equals(j)) {
					if (v1.getTFIDFValue(i) > 0 && v2.getTFIDFValue(j) > 0)
						sim += v1.getTFIDFValue(i) * v2.getTFIDFValue(j);

					semf1 += 1;

					if (v2.getTFIDFValue(j) > 0)
						frac2 += Math.pow(v2.getTFIDFValue(j), 2);
				} else {

					wn = WordNet.compute(i, j);

					if (v1.getTFIDFValue(i) > 0 && v2.getTFIDFValue(j) > 0 && wn > 0) {
						sim += v1.getTFIDFValue(i) * v2.getTFIDFValue(j) * wn;
					}

					if (wn > 0)
						semf1 += wn;

					if (wn > 0 && v2.getTFIDFValue(j) > 0)
						frac2 += Math.pow(v2.getTFIDFValue(j), 2) * wn;
				}

			}

			if (semf1 > 0 && v1.getTFIDFValue(i) > 0)
				frac1 += Math.pow(v1.getTFIDFValue(i), 2) * semf1;

			semf1 = 0;
		}

		double fraction = Math.sqrt(frac1) * Math.sqrt(frac2);

		logger.info(sim + "\t" + Math.sqrt(frac1) + "\t" + Math.sqrt(frac2));
		if (fraction == 0)
			return 0;

		double result = sim / fraction;
		return result > 1 ? 1 : result;
	}

	public static void printTFIDFVector(TFIDFVector input) {

		System.out.println("\n-- TFIDFVector --");
		for (String s : input.getAllTerms()) {
			System.out.print(s + "\t" + input.getTFIDFValue(s) + ", ");
		}

		System.out.println("\n-----");
	}

	public static void printIDF(IDF d) {
		System.out.println("----------Inverted Document Frequency ----------");
		for (String s : d.getAllTerms()) {
			System.out.println(s + "\t" + d.getIDFValue(s));
		}
	}

	public IDF getIDF() {
		return this.idf;
	}


	public MyTextTokenizer getTokenizer() {
		// TODO Auto-generated method stub
		return this.tokenizer;
	}
	
	public ArrayList<TFIDFVector> getTFIDFVector(){
		return this.TFIDFVectors;
	}
}
