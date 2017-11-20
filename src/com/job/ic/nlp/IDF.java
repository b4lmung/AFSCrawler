package com.job.ic.nlp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.job.ic.crawlers.models.LinksModel;

public class IDF implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4479912844498092485L;

	private ConcurrentHashMap<String, HashSet<String>> idf;
	
	private Set<String> totalDocs;
	
	public IDF(){
		this.idf = new ConcurrentHashMap<>();
		this.totalDocs = new ConcurrentHashMap().newKeySet();
	}
	
	public void addDocs(String md5url, String term){
		if(!this.idf.containsKey(term)){
			this.idf.put(term, new HashSet<>());
		}
		
		this.idf.get(term).add(md5url);
		
		this.totalDocs.add(md5url);
	}
	
	
	public boolean contains(String term){
		return this.idf.containsKey(term);
	}
	
	public double getIDFValue(String term){
		if(this.idf.containsKey(term)){
			return Math.log10((this.totalDocs.size()*1.0) / this.idf.get(term).size());
		}	
		return 0;
	}
	
	
	public Set<String> getAllTerms(){
		return this.idf.keySet();
	}
}
