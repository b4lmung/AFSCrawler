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
	private int inLink;
	private ClassifierOutput dirPrediction;

	public QueueObj(String url, String parentUrl,
			int depth, int distanceFromThai, double score) {
		this.url = url;
		this.isRelevantParent = false;
		this.depth = depth;
		this.score = new ArrayList<>();
		this.score.add(score);
		this.setTimeMillis(System.currentTimeMillis());
		setDistanceFromThai(distanceFromThai);
		this.dirPrediction = null;
		this.inLink = 0;
	}
	
	public QueueObj(String url, String parentUrl,
			int depth, int distanceFromThai, double score, ClassifierOutput dir) {
		this.url = url;
		this.isRelevantParent = false;
		this.depth = depth;
		this.score = new ArrayList<>();
		this.score.add(score);
		this.setTimeMillis(System.currentTimeMillis());
		setDistanceFromThai(distanceFromThai);
		this.dirPrediction = dir;
		this.inLink = 0;
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

	public ArrayList<Double> getInLinkScores() {
		return this.score;
	}
	
	public Double getScore() {
		if(avgMode){
			double output = 0;
			for(double i: score){
				output+=i;
			}
			
			output/=score.size();
			return output;
		}else{
			double max = 0;
			for(double i: score){
				if(max < Math.max(max, i))
					max=i;
			}
			
			return max;
		}
	}

	public void setScore(double input) {
		this.score.add(input);
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

	public ClassifierOutput getDirPrediction() {
		return dirPrediction;
	}

	public void setDirPrediction(ClassifierOutput dirPrediction) {
		this.dirPrediction = dirPrediction;
	}
}
