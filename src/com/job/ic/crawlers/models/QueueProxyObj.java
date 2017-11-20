/**
 * @author Thanaphon Suebchua
 */

package com.job.ic.crawlers.models;
import java.io.Serializable;

public class QueueProxyObj implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5602258792551872391L;
	private String url;
	private String parentUrl;
	private double score;
	private int depth;
	private int distance;
	private Double[] previousCalculation;
	
	private String extra;
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getParentUrl() {
		return parentUrl;
	}
	public void setParentUrl(String parentUrl) {
		this.parentUrl = parentUrl;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public QueueProxyObj(String url, String parentUrl, double score, int depth, int distance) {
		super();
		this.url = url;
		this.parentUrl = parentUrl;
		this.score = score;
		this.depth = depth;
		this.distance = distance;
		this.extra = "";
		this.previousCalculation = null;
	}
	
	public int getDepth() {
		return depth;
	}
	public void setDepth(int depth) {
		this.depth = depth;
	}
	public int getDistanceFromRelevantPage() {
		return distance;
	}
	public void setDistanceFromFromRelevantPage(int distance) {
		this.distance = distance;
	}
	public String getExtra() {
		return extra;
	}
	public void setExtra(String extra) {
		this.extra = extra;
	}
	public Double[] getPreviousCalculation() {
		return previousCalculation;
	}
	public void setPreviousCalculation(Double[] previousCalculation) {
		this.previousCalculation = previousCalculation;
	}

}
