/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.models;

public class PageObject {

	private String url;
	private float pageScore;
	private String encoding;
	private int depth;
	private int distanceFromThai;
	private String contentType;
	private byte[] content;

	public PageObject(byte[] content, String contentType, String url,
			float pageScore, String encoding, int depth, int distanceFromThai) {
		this.content = content;
		this.url = url;
		this.pageScore = pageScore;
		this.encoding = encoding;
		this.depth = depth;
		this.distanceFromThai = distanceFromThai;
		this.contentType = contentType;
	}

	public String getContentType() {
		return this.contentType;
	}

	public float getPageScore() {
		return pageScore;
	}

	public void setPageScore(float score) {
		this.pageScore = score;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public PageObject(byte[] content, String url) {
		this.content = content;
		this.url = url;
		this.encoding = "iso-8859-1";
		this.depth = 0;
	}

	public PageObject() {
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public byte[] getContent() {
		return content;
	}

	@Override
	public String toString() {
		return getUrl() + " langscore: " + getPageScore() + " encoding:"
				+ getEncoding();
	}

	public void setDistanceFromThai(int distanceFromThai) {
		this.distanceFromThai = distanceFromThai;
	}

	public int getDistanceFromThai() {
		return distanceFromThai;
	}
}
