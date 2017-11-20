package com.job.ic.ml.classifiers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.WebsiteSegment;

public class PredictionTask extends Thread {

	private static Logger logger = Logger.getLogger(PredictionTask.class);

	private WebsiteSegment input;
	private CountDownLatch cd;
	private LinkedBlockingQueue<Predictor> predictorPool;

	public PredictionTask(CountDownLatch cd, WebsiteSegment input, LinkedBlockingQueue<Predictor> predictorPool) {
		super();
		this.input = input;
		this.cd = cd;

		this.predictorPool = predictorPool;
	}

	public void run() {
		boolean usePredictor = false;
		if (CrawlerConfig.getConfig().isTrainingMode() || (CrawlerConfig.getConfig().getPredictorTrainingPath() != null && !CrawlerConfig.getConfig().getPredictorTrainingPath().trim().equals("")))
			usePredictor = true;

		Predictor predictor = null;

		try {
			if (usePredictor) {
				predictor = predictorPool.take();

				WekaClassifier link = predictor.getLinkClassifier();
				WekaClassifier anchor = predictor.getAnchorClassifier();
				WekaClassifier url = predictor.getUrlClassifier();

				String[] features = input.getLinkFeatureString().split(",");
				ClassifierOutput[] c = new ClassifierOutput[4];

				try {
					c[0] = link.predict(features[0], features);
				} catch (Exception e) {
					logger.error(features);
				}

				// anchor
				// if (!StringUtils.removeSpaces(features[10].replace("'",
				// "")).trim().equals("")) {
				try {
					c[1] = anchor.predict(features[0], features);
				} catch (Exception e) {
					logger.error(features);
				}
				// } else {
				// c[1] = null;
				// }

//				 try {
//					 c[2] = url.predict(features[0], features);
//				 } catch (Exception e) {
//					 logger.error(features);
//				 }

				 c[2] = null;

				this.input.setPrediction(c[0], c[1], c[2]);
			}

		} catch (Exception ex) {
			logger.error(ex.getCause() + "\t" + ex.getMessage());
		} finally {

			if (predictor != null)
				this.predictorPool.add(predictor);
			cd.countDown();
		}
	}

}
