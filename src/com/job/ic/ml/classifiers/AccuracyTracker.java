package com.job.ic.ml.classifiers;

public class AccuracyTracker {
	private static ConfusionMatrixObj linkRel = new ConfusionMatrixObj();
	private static ConfusionMatrixObj anchorRel = new ConfusionMatrixObj();
	private static ConfusionMatrixObj urlRel = new ConfusionMatrixObj();

	private static ConfusionMatrixObj linkNon = new ConfusionMatrixObj();
	private static ConfusionMatrixObj anchorNon = new ConfusionMatrixObj();
	private static ConfusionMatrixObj urlNon = new ConfusionMatrixObj();

	private static ConfusionMatrixObj history = new ConfusionMatrixObj();
	private static ConfusionMatrixObj neighbor = new ConfusionMatrixObj();

	private static ConfusionMatrixObj allRel = new ConfusionMatrixObj();
	private static ConfusionMatrixObj allNon = new ConfusionMatrixObj();
	
	public static void clear(){
		 linkRel = new ConfusionMatrixObj();
		 anchorRel = new ConfusionMatrixObj();
		 urlRel = new ConfusionMatrixObj();

		 linkNon = new ConfusionMatrixObj();
		 anchorNon = new ConfusionMatrixObj();
		 urlNon = new ConfusionMatrixObj();

		 history = new ConfusionMatrixObj();
		 neighbor = new ConfusionMatrixObj();

		 allRel = new ConfusionMatrixObj();
		 allNon = new ConfusionMatrixObj();
	}
	
	public static double calcWeight(double acc){
//		return acc;
//		return Math.log10(acc/(1-acc));
		
		double output = 1;
//		output = acc;
		
		
//		output = Math.log10(acc/(1-acc));
		
		if(output == Double.NaN)
			return 1;
		
		return output;
	}
	
	public static ConfusionMatrixObj getLinkRelConfusionMatrix() {
		return linkRel;
	}
	public static ConfusionMatrixObj getAnchorRelConfusionMatrix() {
		return anchorRel;
	}
	public static ConfusionMatrixObj getUrlRelConfusionMatrix() {
		return urlRel;
	}
	public static ConfusionMatrixObj getLinkNonConfusionMatrix() {
		return linkNon;
	}
	public static ConfusionMatrixObj getAnchorNonConfusionMatrix() {
		return anchorNon;
	}
	public static ConfusionMatrixObj getUrlNonConfusionMatrix() {
		return urlNon;
	}
	public static ConfusionMatrixObj getHistoryConfusionMatrix() {
		return history;
	}
	public static ConfusionMatrixObj getRelSegmentPredictorConfusionMatrix() {
		return allRel;
	}
	public static ConfusionMatrixObj getNonSegmentPredictorConfusionMatrix() {
		return allNon;
	}

	public static ConfusionMatrixObj getNeighborConfusionMatrix() {
		return neighbor;
	}

	public static void setNeighborConfusionMatrix(ConfusionMatrixObj neighbor) {
		AccuracyTracker.neighbor = neighbor;
	}
}
