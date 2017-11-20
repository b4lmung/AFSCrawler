package com.job.ic.ml.classifiers;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.utils.StringUtils;

public class kFoldTask extends Thread {

	private static Logger logger = Logger.getLogger(kFoldTask.class);

	private String input;
	private CountDownLatch cd;
	private LinkedBlockingQueue<Predictor> predictorPool;

	private LinkedBlockingQueue<KFoldTaskResult> kFoldResults;
	
	public kFoldTask(CountDownLatch cd, String input, LinkedBlockingQueue<Predictor> predictorPool, LinkedBlockingQueue<KFoldTaskResult> results) {
		super();
		this.input = input;
		this.cd = cd;

		this.predictorPool = predictorPool;
		this.kFoldResults = results;
	}

	public void run() {
		boolean usePredictor = false;
		if (CrawlerConfig.getConfig().getPredictorTrainingPath() != null && !CrawlerConfig.getConfig().getPredictorTrainingPath().trim().equals(""))
			usePredictor = true;

		Predictor p = null;
		try {

			p = this.predictorPool.take();

			WekaClassifier link = p.getLinkClassifier();
			WekaClassifier anchor = p.getAnchorClassifier();
			WekaClassifier url = p.getUrlClassifier();

			String[] features = input.split(",");
			ClassifierOutput[] c = new ClassifierOutput[3];

			try {
				c[0] = link.predict(features[0], features);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(Arrays.deepToString(features));
				
				System.exit(0);
			}

			if (!StringUtils.removeSpaces(features[10].replace("'", "")).trim().equals("")) {
				try {
					c[1] = anchor.predict(features[0], features);
				} catch (Exception e) {
					e.printStackTrace();
					logger.error(features);
				}
			} else {
				c[1] = null;
			}

//			if (!StringUtils.removeSpaces(features[11].replace("'", "")).trim().equals("")) {
//
//				try {
//					c[2] = url.predict(features[0], features);
//				} catch (Exception e) {
//					logger.error(features);
//				}
//
//			} else {
				c[2] = null;
//			}

			KFoldTaskResult k = null;

			if (features[features.length - 1].trim().equals("non")) {
				k = new KFoldTaskResult(features, c[0], c[1], c[2], WekaClassifier.average(c[0], c[1], c[2]), ResultClass.IRRELEVANT);
			} else {
				k = new KFoldTaskResult(features, c[0], c[1], c[2], WekaClassifier.average(c[0], c[1], c[2]), ResultClass.RELEVANT);
			}

			if (k != null)
				this.kFoldResults.add(k);

		} catch (Exception ex) {

		} finally {
			if(p != null)this.predictorPool.add(p);
			
			cd.countDown();
		}
	}

}
