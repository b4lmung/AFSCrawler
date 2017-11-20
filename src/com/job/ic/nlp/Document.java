package com.job.ic.nlp;

import java.io.Serializable;

public class Document implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3540161978683129330L;
	private String url;
	private String data;
	
	public Document(String url, String data){
		setUrl(url);
		setData(data);
	}
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}

	
	
}
