/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.job.ic.extraction.FeaturesExtraction;
import com.job.ic.ml.classifiers.ClassifierOutput;
import com.job.ic.ml.classifiers.JapaneseTokenizer;
import com.job.ic.ml.classifiers.MyTextTokenizer;
import com.job.ic.ml.classifiers.TokenizedOutput;
import com.job.ic.ml.classifiers.WekaClassifier;
import com.job.ic.nlp.services.LanguageIdentifier;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;

@Entity
public class WebsiteSegment {

	@PrimaryKey(sequence = "ID")
	private long id;

	@SecondaryKey(relate = Relationship.MANY_TO_ONE)
	private String segmentName;

	private int depth;
	private ArrayList<LinksModel> urls;
	private int distanceFromRelevantSeg;

	private ClassifierOutput linkPrediction;
	private ClassifierOutput anchorPrediction;
	private ClassifierOutput urlPrediction;
	
	
	private String sourceSeg;
	private String srcDomain;
	private String srcCountry;
	private String domain;
	private String country;
	private String urlString;
	private String anchorString;
//	private String directoryFeatures;
	private double avgRelDegree;
	private double avgRelScore;

	private boolean isSameHostAsSource;

	private long timeStamp;

	private String featureString;
	// web page relevance score
	private ArrayList<Double> srcRelScores;

	public WebsiteSegment() {
		this.depth = 0;
	}

	public WebsiteSegment(String segmentName, String sourceSeg, int depth, ArrayList<LinksModel> urls, int distanceFromRel, String srcDomain, String srcCountry, String destDomain, String destCountry,
			String urlString, String anchorString, ArrayList<Double> srcRelScores, boolean isSameHostAsSource) {

		this.segmentName = segmentName.toLowerCase();
		this.sourceSeg = sourceSeg.toLowerCase();
		this.depth = depth;
		this.urls = urls;
		this.distanceFromRelevantSeg = distanceFromRel;
		this.srcDomain = srcDomain.toLowerCase();
		this.srcCountry = srcCountry.toLowerCase();
		this.domain = destDomain.toLowerCase();
		this.country = destCountry.toLowerCase();
		this.urlString = urlString.toLowerCase();
		this.anchorString = anchorString.toLowerCase();
		this.srcRelScores = srcRelScores;
		this.setSameHostAsSource(isSameHostAsSource);

		if (!Arrays.asList(FeaturesExtraction.domain).contains(this.domain.toLowerCase()))
			this.domain = "other";

		if (!Arrays.asList(FeaturesExtraction.domain).contains(this.srcDomain.toLowerCase()))
			this.srcDomain = "other";

		if (!Arrays.asList(FeaturesExtraction.country).contains(this.srcCountry.toLowerCase()))
			this.srcCountry = "other";

		if (!Arrays.asList(FeaturesExtraction.country).contains(this.country.toLowerCase()))
			this.country = "other";

		double thai = 0, non = 0, score = 0;

		if (this.srcRelScores != null) {
			for (double d : this.srcRelScores) {
				if (d >= 0.5)
					thai++;
				else
					non++;

				score += d;
			}
		} else {
			score = 0;
		}

		if (thai + non > 0) {
			this.avgRelDegree = thai / (thai + non);
			this.avgRelScore = score / (thai + non);
		} else {
			this.avgRelDegree = 0;
			this.avgRelScore = 0;
		}

		setTimeStamp(System.currentTimeMillis());
	}

	// */

	public String getLinkFeatureString() {

		if (this.featureString != null)
			return this.featureString;

		// relevance score
		double srcRelDegree = 0;
		double thai = 0, non = 0;
		double srcRelScore = 0;
		for (double d : this.srcRelScores) {
			if (d >= 0.5)
				thai++;
			else
				non++;

			srcRelScore += d;
		}

		// relevance degree
		if (thai + non > 0) {
			srcRelDegree = (1.0 * thai) / (thai + non);
			srcRelScore /= (thai + non);
		}

		if (!Arrays.asList(FeaturesExtraction.domain).contains(this.domain.toLowerCase()))
			this.domain = "other";

		if (!Arrays.asList(FeaturesExtraction.domain).contains(this.srcDomain.toLowerCase()))
			this.srcDomain = "other";

		String destSeg = StringUtils.cleanUrlDataForPrediction(this.segmentName);

		if (destSeg.contains("%"))
			destSeg = HttpUtils.decodeURL(destSeg);

		String output = destSeg + "," + srcRelDegree + "," + srcRelScore + "," + this.srcDomain + "-" + this.domain + "," + this.srcCountry + "-" + this.country + ",";

		MyTextTokenizer t;
		if (CrawlerConfig.getConfig().getTargetLang().equals("en"))
			t = new MyTextTokenizer();
		else
			t = new JapaneseTokenizer();

		TokenizedOutput out = t.tokenizeString(this.anchorString, true);

		if (out.getUnknown() + out.getKnown() != 0)
			output += (out.getKnown() * 1.0 / (out.getKnown() + out.getUnknown())) + ",";
		else
			output += "0,";

		out = t.tokenizeString(this.urlString, true);
		if (out.getUnknown() + out.getKnown() != 0)
			output += (out.getKnown() * 1.0 / (out.getKnown() + out.getUnknown())) + ",";
		else
			output += "0,";

		// langauge of anchor text feature
		String lang = "en";

		if (this.anchorString != null && this.anchorString.trim().length() > 10) {

			// LanguageIdentifier lid = new LanguageIdentifier();
			lang = LanguageIdentifier.identifyLanguage(this.anchorString);
		}

		if (!FeaturesExtraction.langSet.contains(lang))
			lang = "other";

		output += lang + ",";

		// language keyword (from url) feature
		HashMap<String, Integer> langMap = new HashMap<String, Integer>();
		String maxKey = "other";
		int maxKeyCount = 0;
		for (String s : out.getTokenized()) {
			s = s.trim().toLowerCase();
			s = StringUtils.normalizeWordFromUrl(s);

			if (FeaturesExtraction.langSet.contains(s)) {
				if (s.length() != 2) {
					int l = Integer.max(Arrays.asList(FeaturesExtraction.langs639_2b).indexOf(s), Arrays.asList(FeaturesExtraction.langs639_2t).indexOf(s));
					if (l >= 0)
						s = FeaturesExtraction.langs639_1[l];
					else
						s = "other";
				}

				if (langMap.containsKey(s)) {
					langMap.put(s, langMap.get(s) + 1);
				} else {
					langMap.put(s, 1);
				}
			}
		}

		for (String s : langMap.keySet()) {
			if (langMap.get(s) > maxKeyCount) {
				maxKeyCount = langMap.get(s);
				maxKey = s;
			}
		}

		output += maxKey + "," + this.distanceFromRelevantSeg + ",'" + this.anchorString.replace("\n", "") + "','" + this.urlString + "',non";
		this.featureString = output.toLowerCase();
		return this.featureString;
	}

	public String getAnchorString() {
		return anchorString;
	}

	public String getCountry() {
		return country;
	}

	public int getDepth() {
		return depth;
	}

	public int getDistanceFromRelevantSeg() {
		return distanceFromRelevantSeg;
	}

	public String getDomain() {
		return domain.toLowerCase();
	}

	public String getSegmentName() {
		return segmentName.toLowerCase();
	}

	public String getSrcCountry() {
		return srcCountry.toLowerCase();
	}

	public String getSrcDomain() {
		return srcDomain.toLowerCase();
	}

	public ArrayList<String> getUrls() {
		
		List<String> output = urls.stream().sorted((a, b)->-1*Float.compare(a.getParentScore(), b.getParentScore())).map(a -> a.getLinkUrl()).collect(Collectors.toList());
		return new ArrayList(output);
//		urls.sort((a, b)->-1*Float.compare(a.getParentScore(), b.getParentScore()));
//		List<String> output = urls.stream().map(a -> a.getLinkUrl()).collect(Collectors.toList());
//		return output;
	}
	
	public List<LinksModel> getLinkModels(){
		urls.sort((a, b)->-1*Float.compare(a.getParentScore(), b.getParentScore()));
		return urls;
	}

	public String getUrlString() {
		return urlString.toLowerCase();
	}

	public void setAnchorString(String anchorString) {
		this.anchorString = anchorString.toLowerCase();
	}

	public void setCountry(String country) {
		this.country = country.toLowerCase();
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setDistanceFromRelevantSeg(int distance) {
		this.distanceFromRelevantSeg = distance;
	}

	public void setDomain(String destDomain) {
		this.domain = destDomain.toLowerCase();
	}

	public void setSegmentName(String segmentName) {
		this.segmentName = segmentName.toLowerCase();
	}

	public void setSrcCountry(String srcCountry) {
		this.srcCountry = srcCountry.toLowerCase();
	}

	public void setSrcDomain(String srcDomain) {
		this.srcDomain = srcDomain.toLowerCase();
	}

	public double getSrcRelDegree() {
		return this.avgRelDegree;
	}

	public double getAvgSrcRelScore() {
		return this.avgRelScore;
	}

//	public void setUrls(ArrayList<String> urls) {
//		this.urls = urls;
//	}

	public void setUrlString(String urlString) {
		this.urlString = urlString.toLowerCase();
	}

	public String getSourceSeg() {
		return sourceSeg.toLowerCase();
	}

	public void setSourceSeg(String sourceSeg) {
		this.sourceSeg = sourceSeg.toLowerCase();
	}
	
	public ClassifierOutput getLinkPrediction() {
		return linkPrediction;
	}

	public ClassifierOutput getAnchorPrediction() {
		return anchorPrediction;
	}

	public ClassifierOutput getUrlPrediction() {
		return urlPrediction;
	}
	

	public void setPrediction(ClassifierOutput linkPrediction, ClassifierOutput anchorPrediction, ClassifierOutput urlPrediction) {
		this.linkPrediction = linkPrediction;
		this.anchorPrediction = anchorPrediction;
		this.urlPrediction = urlPrediction;		
	}

	

	public void setLinkFeatureString(String input) {
		this.featureString = input;
	}

	public boolean isSameHostAsSource() {
		return isSameHostAsSource;
	}

	public void setSameHostAsSource(boolean isSameHostAsSource) {
		this.isSameHostAsSource = isSameHostAsSource;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public long getID(){
		return this.id;
	}

}
