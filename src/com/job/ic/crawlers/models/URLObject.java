package com.job.ic.crawlers.models;

public class URLObject {
	private String url;
	private Double parentScore;
	
	public URLObject(String url, Double parentScore) {
		super();
		this.url = url;
		this.parentScore = parentScore;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public Double getParentScore() {
		return parentScore;
	}
	public void setParentScore(Double parentScore) {
		this.parentScore = parentScore;
	}
	
}
