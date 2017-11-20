package com.job.ic.ml.classifiers;

public class KFoldTaskResult {
	
	private String[] features;
	private ClassifierOutput linkOutput;
	private ClassifierOutput anchorOutput;
	private ClassifierOutput urlOutput;
	private ClassifierOutput avgOutput;
	
	
	private ResultClass realOutput;
	
	public KFoldTaskResult(String[] features, ClassifierOutput linkOutput, ClassifierOutput anchorOutput,
			ClassifierOutput urlOutput, ClassifierOutput avgOutput, ResultClass realOutput) {
		super();
		this.setFeatures(features);
		this.linkOutput = linkOutput;
		this.anchorOutput = anchorOutput;
		this.urlOutput = urlOutput;
		this.avgOutput = avgOutput;
		this.setRealOutput(realOutput);
	}
	
	public KFoldTaskResult(){
		
	}
	
	
	public ClassifierOutput getLinkOutput() {
		return linkOutput;
	}
	public void setLinkOutput(ClassifierOutput linkOutput) {
		this.linkOutput = linkOutput;
	}
	public ClassifierOutput getAnchorOutput() {
		return anchorOutput;
	}
	public void setAnchorOutput(ClassifierOutput anchorOutput) {
		this.anchorOutput = anchorOutput;
	}
	public ClassifierOutput getUrlOutput() {
		return urlOutput;
	}
	public void setUrlOutput(ClassifierOutput urlOutput) {
		this.urlOutput = urlOutput;
	}

	public ResultClass getRealOutput() {
		return realOutput;
	}

	public void setRealOutput(ResultClass realOutput) {
		this.realOutput = realOutput;
	}

	public ClassifierOutput getAvgOutput() {
		return avgOutput;
	}

	public void setAvgOutput(ClassifierOutput avgOutput) {
		this.avgOutput = avgOutput;
	}

	public String[] getFeatures() {
		return features;
	}

	public void setFeatures(String[] features) {
		this.features = features;
	}
	
}
