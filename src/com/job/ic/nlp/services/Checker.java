package com.job.ic.nlp.services;

import com.job.ic.ml.classifiers.ResultClass;


public abstract class Checker{
	public abstract float checkHtmlContent(byte[] content);
	
	public static ResultClass getResultClass(double checkScore) {
		if (checkScore > 0.5)
			return ResultClass.RELEVANT;

		return ResultClass.IRRELEVANT;
	}
	
	//public abstract void finalize();
	
}
