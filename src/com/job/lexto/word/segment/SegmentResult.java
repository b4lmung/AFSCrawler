package com.job.lexto.word.segment;

import java.util.ArrayList;

public class SegmentResult {
	private ArrayList<String> unknown;
	private ArrayList<String> known;
	private ArrayList<String> ambiguous;
	private int special;
	
	public SegmentResult(){
		unknown = new ArrayList<>();
		known = new ArrayList<>();
		ambiguous = new ArrayList<>();
		special = 0;
	}
	
	public ArrayList<String> getUnknown() {
		return unknown;
	}
	public void setUnknown(ArrayList<String> unknown) {
		this.unknown = unknown;
	}
	public ArrayList<String> getKnown() {
		return known;
	}
	public void setKnown(ArrayList<String> known) {
		this.known = known;
	}
	public ArrayList<String> getAmbiguous() {
		return ambiguous;
	}
	public void setAmbiguous(ArrayList<String> ambiguous) {
		this.ambiguous = ambiguous;
	}
	public int getSpecial() {
		return special;
	}
	public void setSpecial(int special) {
		this.special = special;
	}
	
	
	
}
