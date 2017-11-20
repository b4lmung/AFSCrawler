package com.job.ic.crawlers.models;

import java.io.Serializable;

import com.sleepycat.persist.model.Persistent;

@Persistent
public class LinksModel implements Serializable{
	
	private static final long serialVersionUID = -7068854969755602910L;
	private String linkUrl;
	private String sourceUrl;
	private String anchorText;
	private Float parentScore;

	public LinksModel(){
		
	}
	
	public LinksModel(String sourceUrl, String linkUrl, String anchorText, float f) {
		super();
		this.sourceUrl = sourceUrl;
		this.linkUrl = linkUrl;
		this.anchorText = anchorText;
		this.parentScore = f;
	}

	public String getLinkUrl() {
		return linkUrl;
	}

	public void setLinkUrl(String linkUrl) {
		this.linkUrl = linkUrl;
	}

	public String getAnchorText() {
		return anchorText;
	}

	public void setAnchorText(String anchorText) {
		this.anchorText = anchorText;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public Float getParentScore() {
		return parentScore;
	}

	public void setParentScore(Float parentScore) {
		this.parentScore = parentScore;
	}

}
