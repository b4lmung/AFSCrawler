package com.job.ic.ml.classifiers;

import java.util.ArrayList;

public class TokenizedOutput {

	private ArrayList<String> tokenized;
	private int known;
	private int unknown;
	private int ambiguous;
	
	public TokenizedOutput(){
		known = 0;
		unknown = 0;
		ambiguous = 0;
	}
	

	public ArrayList<String> getTokenized() {
		return tokenized;
	}
	public void setTokenized(ArrayList<String> tokenized) {
		this.tokenized = tokenized;
	}
	public int getKnown() {
		return known;
	}
	public void incKnown() {
		this.known++;
	}
	public int getUnknown() {
		return unknown;
	}
	public void incUnknown() {
		this.unknown++;
	}
	public int getAmbiguous() {
		return ambiguous;
	}
	public void incAmbiguous() {
		this.ambiguous++;
	}

}
