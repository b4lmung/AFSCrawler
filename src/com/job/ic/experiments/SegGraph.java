package com.job.ic.experiments;

import java.util.ArrayList;

public class SegGraph {

	private String sourceSeg;
	private ArrayList<String> destSegs;
	public String getSourceSeg() {
		return sourceSeg;
	}
	public void setSourceSeg(String sourceSeg) {
		this.sourceSeg = sourceSeg;
	}
	public ArrayList<String> getDestSegs() {
		return destSegs;
	}
	public void setDestSegs(ArrayList<String> destSeg) {
		this.destSegs = destSeg;
	}
	
	public SegGraph(String sourceSeg, String destSeg) {
		super();
		this.sourceSeg = sourceSeg;
		this.destSegs = new ArrayList<>();
		this.destSegs.add(destSeg);
	}
	
	public SegGraph(String sourceSeg, ArrayList<String> destSegs) {
		super();
		this.sourceSeg = sourceSeg;
		this.destSegs = destSegs;
	}
	
	
}
