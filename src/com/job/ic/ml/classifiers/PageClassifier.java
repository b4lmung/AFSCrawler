package com.job.ic.ml.classifiers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.IllegalCharsetNameException;
import java.util.List;

import org.apache.log4j.Logger;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.nlp.services.Checker;
import com.job.ic.nlp.services.LanguageIdentifier;
import com.job.ic.nlp.services.Stemmer;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.StringUtils;

import weka.core.Utils;

public class PageClassifier extends Checker {

	private WekaClassifier checker;
	// private LanguageIdentifier lid;
	private String targetLang;
	private boolean isDiving;
	private static Logger logger = Logger.getLogger(PageClassifier.class);

	public WekaClassifier getChecker() {
		return this.checker;
	}

	public PageClassifier(String modelPath, String targetLang) {

		if (modelPath.contains(".arff")) {
			trainClassifier(modelPath, targetLang);
		} else if (modelPath.contains(".model")) {
			checker = (WekaClassifier) FileUtils.getObjFile(modelPath);
		} else {
			logger.error("only .model / .arff");
			System.exit(0);
		}

		this.targetLang = targetLang;
		if (modelPath.contains("diving"))
			this.isDiving = true;

		logger.info(String.format("Loaded %s \tisDiving %b", modelPath, isDiving));
	}

	public PageClassifier() throws Exception {
		this.targetLang = "en";
		this.isDiving = false;

		// this.lid = new LanguageIdentifier();

		if (CrawlerConfig.getConfig() != null) {
			this.targetLang = CrawlerConfig.getConfig().getTargetLang().trim().toLowerCase();

			String model = CrawlerConfig.getConfig().getPageModel();

			logger.info("Model path: " + model);
			// System.out.println(model);
			if (model.contains(".arff")) {
				trainClassifier(model, targetLang);

				if (model.contains("diving"))
					this.isDiving = true;

				logger.info(String.format("Loaded %s \tisDiving %b", model, isDiving));

			} else if (model.contains(".model")) {
				if (model.contains("diving")) {
					this.isDiving = true;
				}

				if (model.startsWith("/resources/"))
					checker = (WekaClassifier) FileUtils.getResource(model);
				else
					checker = (WekaClassifier) FileUtils.getObjFile(model);

			} else {
				throw new Exception("not a model/training file");
			}

			logger.info(String.format("Loaded %s \tisDiving %b", model, isDiving));
		}

	}

	public static void main(String[] args) throws IOException {

		PageClassifier cf;
		try {
			cf = new PageClassifier("train.arff", "ja");

			String tmp = FileUtils.readFileContent("test1.htm");
			// System.out.println(cf.checkHtmlContent(tmp.getBytes()));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void trainClassifier(String path, String targetLang) {
		int[] rm = { 1 };// {1,4,5,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24};

		String algo = "weka.classifiers.bayes.NaiveBayesMultinomialText";

		String option = null;
		try {

			if (targetLang.equals("en"))
				option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -lowercase -stopwords-handler weka.core.stopwords.Rainbow -tokenizer \"com.job.ic.ml.classifiers.MyTextTokenizer\" -stemmer weka.core.stemmers.IteratedLovinsStemmer";
			else if (targetLang.equals("ja"))
				option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -lowercase -stopwords-handler weka.core.stopwords.Rainbow -tokenizer \"com.job.ic.ml.classifiers.JapaneseTokenizer\" -stemmer weka.core.stemmers.NullStemmer";
			else
				throw new Exception("unknown language " + targetLang);

		} catch (Exception e) {
			e.printStackTrace();
		}

		this.checker = new WekaClassifier(algo, option);
		this.checker.train(path, rm, false, null, true, false);

	}

	public static void trainClassifier(String path, String targetLang, String outputPath) {
		int[] rm = { 1 };// {1,4,5,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24};

		String algo = "weka.classifiers.bayes.NaiveBayesMultinomialText";

		String option = null;
		try {

			if (targetLang.equals("en"))
				option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -lowercase -stopwords-handler weka.core.stopwords.Rainbow -tokenizer \"com.job.ic.ml.classifiers.MyTextTokenizer\" -stemmer weka.core.stemmers.IteratedLovinsStemmer";
			else if (targetLang.equals("ja"))
				option = "-W -P 0 -M 3.0 -normalize -norm 1.0 -lnorm 2.0 -lowercase -stopwords-handler weka.core.stopwords.Rainbow -tokenizer \"com.job.ic.ml.classifiers.JapaneseTokenizer\" -stemmer weka.core.stemmers.NullStemmer";

		} catch (Exception e) {
			e.printStackTrace();
		}

		WekaClassifier c = new WekaClassifier(algo, option);
		c.train(path, rm, false, null, true, false);
		FileUtils.saveObjFile(c, outputPath);

	}

	@Override
	public void finalize() {
		// TODO Auto-generated method stub
		// this.checker.getAlphabet().clearMap();
		this.checker = null;
	}

	@Override
	public synchronized float checkHtmlContent(byte[] content) {
		Source sc;

		String cc;

		if (content == null)
			return 0;

		try {
			sc = new Source(new ByteArrayInputStream(content));
			//
			List<Element> headere = sc.getAllElements(HTMLElementName.TITLE);
			String data = "", title = "", anchor = "";
			if (headere.size() > 0)
				title += headere.get(0).getTextExtractor().toString();

			List<Element> anchore = sc.getAllElements(HTMLElementName.A);
			for (int k = 0; k < anchore.size(); k++)
				anchor += " " + anchore.get(k).getContent().toString();

			OutputDocument doc = new OutputDocument(sc);
			for (Element e : sc.getAllElements()) {
				if (e.getName().equals(HTMLElementName.FORM)) {
					doc.remove(e.getChildElements());
					doc.remove(e);
				}
			}

			sc = new Source(doc.toString());
			List<Element> mye = sc.getAllElements(HTMLElementName.BODY);
			if (mye == null || mye.size() == 0)
				return 0;

			cc = mye.get(0).getContent().getTextExtractor().toString();
			cc = cc.toLowerCase();
			doc = null;

			data = cc;
			data = StringUtils.removeSpaces(data);

			double length = data.replaceAll(" ", "").length();

			data = StringUtils.removeSymbols(data);
			data = StringUtils.removeSpaces(data);
			data = StringUtils.removeSpaces(data);

			title = StringUtils.removeSymbols(title);
			title = StringUtils.removeSpaces(title);

			anchor = StringUtils.removeSymbols(anchor);
			anchor = StringUtils.removeSpaces(anchor);

			String lang = "en";
			lang = LanguageIdentifier.identifyLanguage(data);

			// System.err.println(targetLang.equals("ja") + "\t" +
			// this.targetLang + "\t" + lang);
			if (!lang.toLowerCase().equals(this.targetLang))
				return 0;

			if (targetLang.equals("en")) {
				anchor = StringUtils.removeNonEngCharacter(anchor);
				data = StringUtils.removeNonEngCharacter(data);
				title = StringUtils.removeNonEngCharacter(title);

				anchor = Stemmer.stem(anchor.trim());
				title = Stemmer.stem(title.trim());
				data = Stemmer.stem(data.trim());

				int wordCount = data.split(" ").length;
				if (wordCount < 100) {
					return 0;
				}

				double afterL = data.replaceAll(" ", "").length();
				if (length == 0)
					return 0;

				if (afterL / length >= 0.5) {
					ClassifierOutput op = checker.predict("test", new String[] { "test", data, title, anchor });
					
					if (isDiving) {
						MyTextTokenizer t = new MyTextTokenizer();
						TokenizedOutput to = t.tokenizeString(data, true);
						boolean found = false;
						for (String token : to.getTokenized()) {
							token = token.toLowerCase().trim();

							if (token.equals("scuba") || token.equals("diver") || token.equals("divers") || token.equals("dives") || token.equals("dive") || token.equals("snorkel")
									|| token.equals("snorkeling")) {
								found = true;
							}
						}
						if (!found)
							return -1*(float) ((int) (op.getRelevantScore() * 100)) / 100;
					}

					return (float) ((int) (op.getRelevantScore() * 100)) / 100;
				} else {
					// System.out.println("PASS4");
					return 0;
				}
			} else if (targetLang.equals("ja")) {

				JapaneseTokenizer t = new JapaneseTokenizer();
				if(t.tokenizeString(data).size() < 50)
					return 0;

				ClassifierOutput op = checker.predict("test", new String[] { "test", data, title, anchor });
				return (float) ((int) (op.getRelevantScore() * 100)) / 100;

			} else {
				throw new Exception("unknown language " + targetLang);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println("PASS5");
		return 0;
	}

}
