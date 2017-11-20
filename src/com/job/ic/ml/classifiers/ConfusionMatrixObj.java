package com.job.ic.ml.classifiers;

public class ConfusionMatrixObj {
	private int fp;
	private int tn;
	private int tp;
	private int fn;
	
	public ConfusionMatrixObj(int tp, int fn, int tn, int fp){
		this.fp = fp;
		this.tn = tn;
		this.tp = tp;
		this.fn = fn;
	}
	
	public ConfusionMatrixObj(){
		this.fp = 0;
		this.tn = 0;
		this.tp = 0;
		this.fn = 0;
	}
	
	public int getFp() {
		return fp;
	}
	public void setFp(int fp) {
		this.fp = fp;
	}
	public int getTn() {
		return tn;
	}
	public void setTn(int tn) {
		this.tn = tn;
	}
	public int getTp() {
		return tp;
	}
	public void setTp(int tp) {
		this.tp = tp;
	}
	public int getFn() {
		return fn;
	}
	public void setFn(int fn) {
		this.fn = fn;
	}
	
	public void incFp() {
		this.fp++;
	}
	
	public void incTn() {
		this.tn++;
	}
	
	public void incTp() {
		this.tp++;
	}
	
	public void incFn() {
		this.fn++;
	}
	
	
	
	public double getGmean(){
		double d = tp / (1.0*(tp + fn));
		
		d *= tn /(1.0* (tn + fp));
		d = Math.sqrt(d);
		return d;
	}
	
	public double getAccuracy(){
		double d = (tp + tn) / (1.0*(tp + fn + tn + fp));
		return d;
	}
	
	
	public double getRelevantPrecision(){
		double output = (1.0*tp)/(fp+tp);
		return Double.isNaN(output)?0:output;
	}
	
	public double getIrrelevantPrecision(){
		double output =  (1.0*tn)/(fn+tn);
		return Double.isNaN(output)?0:output;
	}
	
	@Override
	public String toString() {
		return String.format("TP:\t%d\tTN:\t%d\tFP:\t%d\tFN:\t%d\tGmean:\t%.3f\tAcc:\t%.3f\tPre:\t%.3f\t%.3f", tp, tn, fp, fn, getGmean(), getAccuracy(), getRelevantPrecision(), getIrrelevantPrecision());
	}
}
