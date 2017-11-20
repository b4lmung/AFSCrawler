/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.models;

import java.io.Serializable;

import com.job.ic.ml.classifiers.ResultClass;
import com.sleepycat.persist.model.Persistent;

@Persistent 
public class ClassifierOutput implements Serializable {

	
	public ClassifierOutput(){
		this.resultClass = ResultClass.RELEVANT;
		this.IrrelevantScore = 0.5;
		this.RelevantScore = 0.5;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8933449631713747665L;
	/**
	 * 
	 */
	/**
	 * Predicted hostname
	 */
	/**
	 * Predicted class
	 */
	private ResultClass resultClass;
	/**
	 * Predicted Thai Probability
	 */
	private double RelevantScore;
	/**
	 * Predicted NonThai Probability
	 */
	private double IrrelevantScore;

	private double weight;

	private String extra;
	/**
	 * getter method of resultClass field
	 * 
	 * @return predicted class
	 */
	public ResultClass getResultClass() {
		return resultClass;
	}

	/**
	 * getter method of thaiScore field
	 * 
	 * @return predicted Thai probability
	 */
	public double getRelevantScore() {
		return RelevantScore;
	}

	/**
	 * getter method of nonThaiScore field
	 * 
	 * @return predicted NonThai probability
	 */
	public double getIrrelevantScore() {
		return IrrelevantScore;
	}

	/**
	 * Constructor
	 * 
	 * @param hostname
	 *            predicted site's hostname
	 * @param thaiScore
	 *            predicted thai probability
	 * @param nonThaiScore
	 *            predicted nonthai probability
	 */
	public ClassifierOutput(double relevantScore, double irrelevantScore, double weight) {
		this.RelevantScore = relevantScore;
		this.IrrelevantScore = irrelevantScore;

		this.weight = weight;
		if (this.RelevantScore >= this.IrrelevantScore) {
			this.resultClass = ResultClass.RELEVANT;
		} else {
			this.resultClass = ResultClass.IRRELEVANT;
		}

	}
	
	public ClassifierOutput(double relevantScore, double irrelevantScore, double weight, String extra) {
		this.RelevantScore = relevantScore;
		this.IrrelevantScore = irrelevantScore;
		this.extra = extra;

		this.weight = weight;
		if (this.RelevantScore >= this.IrrelevantScore) {
			this.resultClass = ResultClass.RELEVANT;
		} else {
			this.resultClass = ResultClass.IRRELEVANT;
		}

	}

	@Override
	public String toString() {

		return String.format("Result = %s, ThaiScore = %f, NonThaiScore = %f", resultClass, this.RelevantScore, this.IrrelevantScore);
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}
}
