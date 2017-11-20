package com.job.ic.ml.classifiers;

import java.io.Serializable;

public class Predictor implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8185696816658662183L;
	private WekaClassifier link;
	private WekaClassifier anchor;
	private WekaClassifier url;
	
	public Predictor(WekaClassifier link, WekaClassifier anchor, WekaClassifier url) {
		super();
		this.link = link;
		this.anchor = anchor;
		this.url = url;
//		this.setDir(dir);
	}
	public WekaClassifier getLinkClassifier() {
		return link;
	}
	public void setLinkClassifier(WekaClassifier link) {
		this.link = link;
	}
	public WekaClassifier getAnchorClassifier() {
		return anchor;
	}
	public void setAnchorClassifier(WekaClassifier anchor) {
		this.anchor = anchor;
	}
	public WekaClassifier getUrlClassifier() {
		return url;
	}
	public void setUrlClassifier(WekaClassifier url) {
		this.url = url;
	}

}
