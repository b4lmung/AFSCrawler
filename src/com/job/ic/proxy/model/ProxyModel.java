package com.job.ic.proxy.model;

import java.util.ArrayList;

import com.job.ic.crawlers.models.LinksModel;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class ProxyModel {
	@PrimaryKey
	private String url;
	
	private float score;
	
	private ArrayList<LinksModel> links;
//	private long startByte;

	public ProxyModel(){
		
	}
	
	public ProxyModel(String url, float score, ArrayList<LinksModel> links){
		this.url = url.toLowerCase();
		this.score = score;
		this.links = links;
	}

	public String getUrl(){
		return this.url;
	}
	
	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public ArrayList<LinksModel> getLinks() {
		return links;
	}

	public void setLinks(ArrayList<LinksModel> links) {
		this.links = links;
	}
	
	
}
