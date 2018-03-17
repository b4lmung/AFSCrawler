package com.job.ic.crawlers.models;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.daos.WebsiteSegmentDb;
import com.job.ic.ml.classifiers.ClassifierOutput;
import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.ml.classifiers.WekaClassifier;
//import com.job.ic.crawlers.daos.WebsiteSegmentDAO;
//import com.job.ic.crawlers.daos.WebsiteSegmentDb;
import com.job.ic.proxy.ProxyService;

public class SegmentQueueModel {

	private String segmentName;
	private int depth;

//	private int size = 0;
	

	private ArrayList<ClassifierOutput> neighborhoodPredictions;
	private ArrayList<ClassifierOutput> historyPredictions;
	
	private ArrayList<WebsiteSegment> segmentData;
	private double predictedScore = 0;
	private double weightScore = 0;
	private double avgRelDegree = 0;
	private double avgRelScore = 0;
	private long timestamp;
	private double maxRelScore = -1;
	
	private static boolean useDb = false;
	

	public SegmentQueueModel(String segment, int depth, WebsiteSegment data, ClassifierOutput neighborhoodPrediction, ClassifierOutput historyPrediction) {
	
		super();
		this.segmentName = segment.toLowerCase();
		this.depth = depth;
		this.neighborhoodPredictions = new ArrayList<>();
		this.historyPredictions = new ArrayList<>();
		if(!useDb) {
			this.segmentData = new ArrayList<WebsiteSegment>();
		}
		
		addSegmentData(data, neighborhoodPrediction, historyPrediction);
		
		this.timestamp = System.currentTimeMillis();
	}
	
	public ArrayList<ClassifierOutput> getNeighborhoodPredictions(){
		return this.neighborhoodPredictions;
	}

	public String getSegmentName() {
		return segmentName;
	}

	public void setScore(double score){
		this.weightScore = score;
	}
	
	public void setSegmentName(String segment) {
		this.segmentName = segment.toLowerCase();
	}

	public long getSourceSegSize() {
		return this.segmentData.size();
	}

	public double getPredictionScore() {
		return this.predictedScore / this.segmentData.size();
	}

	public double getScore() {
//		if(this.dirPrediction != null && this.dirPrediction.getResultClass() == ResultClass.RELEVANT)
//			return this.weightScore / this.segmentData.size() + this.dirPrediction.getRelevantScore();
		if (this.depth < 0) {
			return this.weightScore;
		}
		return this.weightScore / this.segmentData.size();
	}

	public double getAvgRelDegree() {
		return this.avgRelDegree / this.segmentData.size();
	}

	public double getAvgRelScore() {
		return avgRelScore / this.segmentData.size();
	}
	
	public double getMaxRelScore() {
		return this.maxRelScore;
	}

//	public void addSegmentData(ArrayList<WebsiteSegment> allSegments, ClassifierOutput dirPrediction) {
//		for (WebsiteSegment s : allSegments)
//			addSegmentData(s, dirPrediction);
//	}

	public void addSegmentData(WebsiteSegment segment, ClassifierOutput neighborhoodPrediction, ClassifierOutput historyPrediction) {
		
		if(useDb){
			WebsiteSegmentDb.getSegmentDAO().addWebsiteSegment(segment);
		}else{
			this.segmentData.add(segment);
		}
		
		if(this.segmentData.size() == 0) {
			this.maxRelScore = segment.getAvgSrcRelScore();
		}else {
			this.maxRelScore = Math.max(this.maxRelScore, segment.getAvgSrcRelScore());
		}
		

		this.depth = Math.min(this.depth, segment.getDepth());
		
		double predictionScore = Math.max(0.1, WekaClassifier.average(segment.getLinkPrediction(), segment.getAnchorPrediction(), segment.getUrlPrediction(), neighborhoodPrediction, historyPrediction).getRelevantScore());		
		this.depth = Math.min(this.depth, segment.getDepth());
		if (this.depth == 0) {
			this.weightScore += predictionScore;
			this.predictedScore += predictionScore;
			this.avgRelDegree += segment.getSrcRelDegree();
			this.avgRelScore += segment.getAvgSrcRelScore();
		} else {
			this.weightScore += Math.max(0.1, predictionScore) * Math.max(0.1, segment.getAvgSrcRelScore());
			this.predictedScore += predictionScore;
			this.avgRelDegree += segment.getSrcRelDegree();
			this.avgRelScore += segment.getAvgSrcRelScore();
		}
		
		if(neighborhoodPrediction != null)
			this.neighborhoodPredictions.add(neighborhoodPrediction);
		
		if(historyPrediction != null)
			this.historyPredictions.add(historyPrediction);

		/*double score = 0;
		double predict = 0;
		double avgDegree = 0;
		double avgRelScore = 0;

		for (WebsiteSegment q : segmentData) {
			double predictionScore = Math.max(0.1, WekaClassifier.average(segment.getLinkPrediction(), segment.getAnchorPrediction(), segment.getUrlPrediction(), dirPrediction).getRelevantScore());		

			if(this.depth == 0){
				score += Math.max(0.1, predictionScore);	
			}else if(this.depth > 0){
				score += Math.max(0.1, predictionScore) * Math.max(0.1, q.getAvgSrcRelScore());
			}else{
				score = 1000;
			}
		
			predict += predictionScore;
			avgDegree += q.getSrcRelDegree();
			avgRelScore += q.getAvgSrcRelScore();
		}

		this.avgRelScore = avgRelScore;
		this.avgRelDegree = avgDegree;
		this.predictedScore = predict;
		this.weightScore = score;*/
	
	}

	public ArrayList<WebsiteSegment> getAllSegments() {
		if(this.segmentData != null)
			return this.segmentData;
			
		this.segmentData = WebsiteSegmentDb.getSegmentDAO().retrieve(this.segmentName);
		return this.segmentData;
	}

	public WebsiteSegment getWebsiteSegment() {
		// ArrayList<WebsiteSegment> allSegs = this.segmentData;
		ArrayList<WebsiteSegment> allSegs;
		if (this.segmentData != null)
			allSegs = this.segmentData;
		else
			allSegs = getAllSegments();
		
		if (allSegs.size() == 0)
			return null;

		WebsiteSegment oldOne = allSegs.get(0);
		int depth = oldOne.getDepth();
		int reldis = oldOne.getDistanceFromRelevantSeg();

		ArrayList<LinksModel> urlList = new ArrayList<>();

		String destDomain = oldOne.getDomain();
		String destCountry = oldOne.getCountry();

		for (WebsiteSegment q : allSegs) {

			for (LinksModel s : q.getLinkModels()) {
				
				// logger.info("check >>>> " + s + "\t" +
				// urlList.contains(s.toLowerCase()));
				if (urlList.contains(s.getLinkUrl().toLowerCase())) {
					continue;
				}

				// filter
				if (UrlDb.getUrlDAO().checkAndAddUrl(s.getLinkUrl(), false)) {
					continue;
				}

				if (!CrawlerConfig.getConfig().getLocalProxyPath().equals("") && !ProxyService.contains(s.getLinkUrl())) {
					continue;
				}

				urlList.add(s);
			}

			if (q.getDepth() < depth)
				depth = q.getDepth();

			if (q.getDistanceFromRelevantSeg() < reldis)
				reldis = q.getDistanceFromRelevantSeg();

		}
		
		urlList.sort((a,b) -> -1*Float.compare(a.getParentScore(), b.getParentScore()));

		ArrayList<Double> srcRelScore = new ArrayList<>();
		WebsiteSegment s = new WebsiteSegment(oldOne.getSegmentName(), "null", depth, urlList, reldis, "other", "other", destDomain, destCountry, "", "", srcRelScore, false);

		allSegs = null;
		return s;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public long getTimestamp() {
		return this.timestamp;
	}

	public ArrayList<ClassifierOutput> getHistoryPredictions() {
		return historyPredictions;
	}

	public void setHistoryPredictions(ArrayList<ClassifierOutput> historyPredictions) {
		this.historyPredictions = historyPredictions;
	}


	//
	// public void clear() {
	// this.segmentData.clear();
	// }

}
