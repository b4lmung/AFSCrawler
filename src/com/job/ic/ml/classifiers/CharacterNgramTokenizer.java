package com.job.ic.ml.classifiers;

import java.util.Enumeration;
import java.util.Vector;

import br.ufpb.ngrams.NGramCounter;
import br.ufpb.ngrams.NgramAnalyzer;



public class CharacterNgramTokenizer extends weka.core.tokenizers.NGramTokenizer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected transient NgramAnalyzer ng;
	protected transient Vector<String> tokenized;
	protected transient Enumeration<String> element;
	
	
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

	@Override
	public void tokenize(String arg0) {
		// TODO Auto-generated method stub
		NgramAnalyzer na = new NgramAnalyzer(arg0);
		NGramCounter[] counter;
		tokenized = new Vector<>();
		for(int i=1; i<=3; i++){
			counter = na.getNgramsOfLength(i);
			for(NGramCounter c: counter){
				for(int j=0; j<c.getCount(); j++){
					tokenized.add(c.getNGram());
				}
			}
		}
		
		element = tokenized.elements();
	}

}
