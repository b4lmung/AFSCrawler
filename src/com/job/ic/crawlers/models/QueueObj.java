/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.models;

import java.io.Serializable;
import java.util.ArrayList;

import com.job.ic.ml.classifiers.ClassifierOutput;

public class QueueObj implements Serializable {

	private static boolean avgMode = false;

	private static final long serialVersionUID = 2294872674909725252L;
	private String url;
	private String parentUrl;
	private boolean isRelevantParent;
	private ArrayList<Double> score;
	private int depth;
	private int distanceFromThai;
	private long timeMillis;
	// private ArrayList<ClassifierOutput> neighborPredictions;
	private ClassifierOutput historyPrediction;

	private double maxSrcScore;

	public QueueObj(String url, String parentUrl, int depth, int distanceFromThai, double srcScore) {
		this.url = url;
		this.isRelevantParent = false;
		this.depth = depth;
		this.score = new ArrayList<>();
		this.score.add(srcScore);
		this.maxSrcScore = srcScore;
		this.setTimeMillis(System.currentTimeMillis());
		setDistanceFromThai(distanceFromThai);
		// this.neighborPredictions = new ArrayList<>();
		// this.historyPredictions = new ArrayList<>();
	}

	public QueueObj(String url, String parentUrl, int depth, int distanceFromThai, double srcScore, ClassifierOutput neighborPrediction, ClassifierOutput historyPrediction) {
		this.url = url;
		this.isRelevantParent = false;
		this.depth = depth;
		this.score = new ArrayList<>();
		this.score.add(srcScore);
		this.maxSrcScore = srcScore;
		this.setTimeMillis(System.currentTimeMillis());
		setDistanceFromThai(distanceFromThai);
		// this.neighborPredictions = new ArrayList<>();
		// this.historyPredictions = new ArrayList<>();

		// if (neighborPrediction != null)
		// this.neighborPredictions.add(neighborPrediction);
		if (historyPrediction != null)
			this.historyPrediction = historyPrediction;
	}

	@Override
	public String toString() {
		return url;
	}

	public boolean getIsRelevantParent() {
		return isRelevantParent;
	}

	public void setIsThaiParent(boolean parent) {
		this.isRelevantParent = parent;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public ArrayList<Double> getSrcScores() {
		return this.score;
	}

	public Double getScore() {
		if (avgMode) {
			double output = 0;
			for (double i : score) {
				output += i;
			}

			output /= score.size();
			return output;
		} else {
			double max = 0;
			for (double i : score) {
				if (max < Math.max(max, i))
					max = i;
			}

			if (CrawlerConfig.getConfig().useHistoryPredictor()) {
				// if (this.historyPredictions.size() == 0)
				// return (max + 0.5) / 2;
				// else
				// return (max + this.historyPredictions.get(this.historyPredictions.size() -
				// 1).getRelevantScore()) / 2;

				if (this.historyPrediction != null)
					return (max + this.historyPrediction.getRelevantScore()) / 2;
				else
					return max;

			} else {
				return max;
			}
		}
	}

	public void addSrcScore(double input) {
		this.maxSrcScore = Math.max(maxSrcScore, input);
		this.score.add(input);
	}

	public void addSrcScore(ArrayList<Double> input) {
		for (Double d : input) {
			this.maxSrcScore = Math.max(maxSrcScore, d);
			this.score.add(d);
		}
	}

	public void updatePredictions(ClassifierOutput nPrediction, ClassifierOutput hPrediction) {
		// this.neighborPredictions.addAll(nPrediction);
		// this.historyPredictions.addAll(hPrediction);
		this.historyPrediction = hPrediction;
	}

	public void setDistanceFromThai(int distanceFromThai) {
		this.distanceFromThai = distanceFromThai;
	}

	public int getDistanceFromThai() {
		return distanceFromThai;
	}

	public void setParentUrl(String parentUrl) {
		this.parentUrl = parentUrl;
	}

	public String getParentUrl() {
		return parentUrl;
	}

	public long getTimeMillis() {
		return timeMillis;
	}

	public void setTimeMillis(long timeMillis) {
		this.timeMillis = timeMillis;
	}

	public double getHistoryScore() {
		if (this.historyPrediction != null)
			return this.historyPrediction.getRelevantScore();
		return -0.5;
	}

	public ClassifierOutput getHistoryPrediction() {
		return this.historyPrediction;
	}

	public double getMaxSrcScore() {
		// TODO Auto-generated method stub
		return maxSrcScore;
	}

	// public ArrayList<ClassifierOutput> getNeighborhoodPrediction() {
	// return this.neighborPredictions;
	// }
	//
	// public void addNeighborhoodPrediction(ClassifierOutput prediction) {
	// this.neighborPredictions.add(prediction);
	// }
	//
	// public ArrayList<ClassifierOutput> getHistoryPrediction() {
	// return historyPredictions;
	// }
	//
	// public void setHistoryPrediction(ClassifierOutput historyPrediction) {
	// this.historyPredictions.add(historyPrediction);
	// }
}
