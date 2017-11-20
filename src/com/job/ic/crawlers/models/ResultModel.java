/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.models;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class ResultModel {

	@PrimaryKey
	private String segmentName;
	private int relevantPage;
	private int irrelevantPage;
	private double avgRelScore;
	private String ipCountry;
	private String extras;

	@Override
	public String toString() {
		return String.format("%s\t Thai:%d \t NonThai: %d \t IP:%s", segmentName,
				relevantPage, irrelevantPage, avgRelScore, ipCountry);
	}

	public ResultModel() {
	}

	public ResultModel(String segmentName, int relevantPage, int irrelevantPage, double avgRelScore,
			String ipCountry, String extras) {
		this.relevantPage = relevantPage;
		this.irrelevantPage = irrelevantPage;
		this.avgRelScore = avgRelScore;
		this.ipCountry = ipCountry.toLowerCase();
		this.segmentName = segmentName.toLowerCase();
		this.extras = extras;
	}

	public int getRelevantPage() {
		return relevantPage;
	}

	public void setRelevantPage(int relevantPage) {
		this.relevantPage = relevantPage;
	}

	public int getIrrelevantPage() {
		return irrelevantPage;
	}

	public void setIrrelevantPage(int irrelevantPage) {
		this.irrelevantPage = irrelevantPage;
	}

	public String getIpCountry() {
		return ipCountry;
	}

	public void setIpCountry(String ipCountry) {
		this.ipCountry = ipCountry;
	}

	public void setSegmentName(String hostname) {
		this.segmentName = hostname.toLowerCase();
	}

	public String getSegmentName() {
		return segmentName.toLowerCase();
	}


	public double getAvgRelevantScore() {
		return avgRelScore;
	}

	public void setAvgRelevantScore(double avgRelevantScore) {
		this.avgRelScore = avgRelevantScore;
	}

	public String getExtras() {
		return extras;
	}

	public void setExtras(String extras) {
		this.extras = extras;
	}
}
