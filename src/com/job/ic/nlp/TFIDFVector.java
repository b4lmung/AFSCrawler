package com.job.ic.nlp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.job.ic.ml.classifiers.MyTextTokenizer;
import com.job.ic.ml.classifiers.TokenizedOutput;
import com.job.ic.nlp.services.Stemmer;
import com.job.ic.utils.StringUtils;

public class TFIDFVector implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1759484202282965540L;
	private ConcurrentHashMap<String, Double> tf;
	private IDF idf;
	private int countFreq;

	private boolean isCentroidVector;

	private MyTextTokenizer tokenizer;
	private String url;
	private boolean isDownloaded;

	public int getCumulativeFreq() {
		return this.countFreq;
	}

	public TFIDFVector(List<TFIDFVector> leaves) {
		this.tf = new ConcurrentHashMap<>();
		this.isCentroidVector = true;
		this.isDownloaded = true;

		for (TFIDFVector w : leaves) {
			for (String s : w.getAllTerms()) {
				if (this.tf.containsKey(s)) {
					this.tf.put(s, this.tf.get(s) + w.getTFIDFValue(s));
				} else {
					this.tf.put(s, w.getTFIDFValue(s));
				}
			}

		}

		for (String s : this.tf.keySet()) {
			this.tf.put(s, this.tf.get(s) / leaves.size());
		}
	}

	public TFIDFVector(IDF idf, Document l, MyTextTokenizer tokenizer, boolean isTraining) {
		this.tf = new ConcurrentHashMap<>();
		this.countFreq = 0;
		this.idf = idf;
		this.tokenizer = tokenizer;
		this.isCentroidVector = false;
		this.url = l.getUrl();
		this.isDownloaded = false;

		String data = l.getData().toLowerCase();
		if (data != null)
			data = Stemmer.stem(data);

		TokenizedOutput to = this.tokenizer.tokenizeString(data, true);

		for (String token : to.getTokenized()) {
			if (!isTraining && !idf.contains(token))
				continue;

			addTerm(token, l, isTraining);
		}

		// process idf
		for (String token : to.getTokenized()) {
			if (isTraining){
				this.idf.addDocs(StringUtils.md5(l.getUrl()), token);
			}
		}

	}


	private void addTerm(String term, Document l, boolean incIDF) {

		term = cleanTerm(term);
		if (term == null)
			return;

		if (!this.tf.containsKey(term))
			this.tf.put(term, 0.0);

		this.tf.put(term, this.tf.get(term) + 1.0);

		this.countFreq++;
	}

	public void clear() {
		this.tf.clear();
		this.countFreq = 0;
	}

	public boolean containsTerm(String term) {
		term = cleanTerm(term);
		return this.tf.containsKey(term);
	}

	public double getTFIDFValue(String term){
		if(this.isCentroidVector)
			return this.tf.containsKey(term)?this.tf.get(term):0;
		
		if(this.tf.containsKey(term)){
			return (this.tf.get(term) / this.countFreq) * this.idf.getIDFValue(term);
		}
		return 0;
	}
	

	public double getTFValue(String term) {
		if(this.isCentroidVector)
			return this.tf.containsKey(term)?this.tf.get(term):0;
		
		if (this.tf.containsKey(term)) {
			return this.tf.get(term);
		} 	
		
		return 0;
		
	}

	public Set<String> getAllTerms() {
		return this.tf.keySet();
	}

	public IDF getIDF() {
		if (this.isCentroidVector)
			return null;

		return this.idf;
	}

	private static String cleanTerm(String term) {

		term = StringUtils.removeSymbols(term);
		term = StringUtils.removeSpaces(term);

		if (term == null || term.length() == 0)
			return null;

		return term.toLowerCase();
	}

	public String getURL() {
		return this.url;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (String s : getAllTerms()) {
			sb.append(s + ":" + getTFIDFValue(s) + "\t");
		}

		sb.append("}");

		return sb.toString();

	}

	public boolean isDownloaded() {
		return isDownloaded;
	}

	public void setDownloaded(boolean isDownloaded) {
		this.isDownloaded = isDownloaded;
	}

	public void removeTerm(String term) {
		this.tf.remove(term);
	}
}
