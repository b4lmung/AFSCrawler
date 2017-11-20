package com.job.ic.crawlers.models;

public class TestFrontierModel {
	private String segName;
	private int depth;
	private double score;
	public String getSegName() {
		return segName;
	}
	public void setSegName(String segName) {
		this.segName = segName.toLowerCase();
	}
	public int getDepth() {
		return depth;
	}
	public void setDepth(int depth) {
		this.depth = depth;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public TestFrontierModel(String segName, int depth, double score) {
		super();
		this.segName = segName.toLowerCase();
		this.depth = depth;
		this.score = score;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return String.format("%s\t%d\t%.2f", segName, depth, score);
	}

}
