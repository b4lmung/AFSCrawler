package com.job.ic.ml.classifiers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

public class PredictionTaskForFeatureAnalyzer extends Thread {

	private static Logger logger = Logger.getLogger(PredictionTaskForFeatureAnalyzer.class);

	private String[] features;
	private CountDownLatch cd;
	private LinkedBlockingQueue<Predictor> predictorPool;

	public PredictionTaskForFeatureAnalyzer(CountDownLatch cd, String[] input, LinkedBlockingQueue<Predictor> predictorPool) {
		super();
		this.features = input;
		this.cd = cd;
		this.predictorPool = predictorPool;
	}


	public void run() {

		Predictor predictor = null;
		WekaClassifier link = null;
		WekaClassifier anchor = null;
		WekaClassifier url = null;

		try {
			predictor = predictorPool.take();
			
			link = predictor.getLinkClassifier();
			anchor = predictor.getAnchorClassifier();
			url = predictor.getUrlClassifier();

			ClassifierOutput[] c = new ClassifierOutput[3];

			try {
				c[0] = link.predict(features[0], features);
			} catch (Exception e) {
				logger.error(features);
			}

			try {
				c[1] = anchor.predict(features[0], features);
			} catch (Exception e) {
				logger.error(features);
			}

			try {
				c[2] = url.predict(features[0], features);

			} catch (Exception e) {
				logger.error(features);
			}

			ClassifierOutput op = WekaClassifier.average(c[0], c[1], c[2]);

			logger.info(this.cd.getCount() + "\t" + this.features[0] + "\t" + this.features[this.features.length - 1] + "\t" + c[0].getResultClass() + "\t" + c[1].getResultClass() + "\t"
					+ c[2].getResultClass() + "\t" + op.getResultClass());

			
		} catch (Exception ex) {

		} finally {
			if(predictor != null)this.predictorPool.add(predictor);
			cd.countDown();
		}
	}

}
