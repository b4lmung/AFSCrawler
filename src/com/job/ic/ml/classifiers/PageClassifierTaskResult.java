package com.job.ic.ml.classifiers;

public class PageClassifierTaskResult {
	
	private String[] features;
	private ClassifierOutput prediction;
	
	
	private ResultClass realOutput;
	
	public PageClassifierTaskResult(String[] features, ClassifierOutput prediction, ResultClass realOutput) {
		super();
		this.setFeatures(features);
		this.prediction = prediction;
		this.setRealOutput(realOutput);
	}
	
	public PageClassifierTaskResult(){
		
	}
	
	
	public ClassifierOutput getPrediction() {
		return this.prediction;
	}

	public ResultClass getRealOutput() {
		return realOutput;
	}

	public void setRealOutput(ResultClass realOutput) {
		this.realOutput = realOutput;
	}

	public String[] getFeatures() {
		return features;
	}

	public void setFeatures(String[] features) {
		this.features = features;
	}
	
}
