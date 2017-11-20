/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.ml.classifiers;

import java.io.Serializable;
import java.util.ArrayList;

import com.sleepycat.persist.model.Persistent;

@Persistent
public class ClassifierOutput implements Serializable {


	private static final long serialVersionUID = -8933449631713747665L;
	private ResultClass resultClass;
	private double relevantScore;
	private double irrelevantScore;
	private double weight;
	private String features;
	private String extra;
	
	public ClassifierOutput() {
		this.resultClass = ResultClass.RELEVANT;
		this.irrelevantScore = 0.5;
		this.relevantScore = 0.5;
	}


	public ResultClass getResultClass() {
		return resultClass;
	}

	public double getRelevantScore() {
		return relevantScore;
	}

	public double getIrrelevantScore() {
		return irrelevantScore;
	}

	public ClassifierOutput(double relevantScore, double irrelevantScore, double weight) {
		this.relevantScore = relevantScore;
		this.irrelevantScore = irrelevantScore;

		this.weight = weight;
		if (this.relevantScore >= this.irrelevantScore) {
			this.resultClass = ResultClass.RELEVANT;
		} else {
			this.resultClass = ResultClass.IRRELEVANT;
		}

	}

	public ClassifierOutput(double relevantScore, double irrelevantScore, double weight, String extra) {
		this.relevantScore = relevantScore;
		this.irrelevantScore = irrelevantScore;
		this.features = extra;

		this.weight = weight;
		if (this.relevantScore >= this.irrelevantScore) {
			this.resultClass = ResultClass.RELEVANT;
		} else {
			this.resultClass = ResultClass.IRRELEVANT;
		}

	}

	public ClassifierOutput(String input) {
		this.resultClass = input.substring(input.indexOf("Result=") + "Result=".length(), input.indexOf(",")).trim().toLowerCase().equals("relevant") ? ResultClass.RELEVANT : ResultClass.IRRELEVANT;
		this.relevantScore = Double.parseDouble(input.substring(input.indexOf("Rel=") + "Rel=".length(), input.indexOf(", Non")));
		this.irrelevantScore = Double.parseDouble(input.substring(input.indexOf("Non=") + "Non=".length(), input.indexOf("]")));
	}

	@Override
	public String toString() {
		return String.format("[Result=%s, Rel=%f, Non=%f]", resultClass, this.relevantScore, this.irrelevantScore);
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public String getFeatures() {
		return this.features;
	}

	public void setFeatures(String features) {
		this.features = features;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}
}
