package com.job.ic.nlp.services;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;

import net.htmlparser.jericho.Source;

public class LanguageIdentifier {
	public static String[] langs639_1 = { "lt", "lv", "mk", "ml", "mr", "ne", "no", "pa", "fa", "pl", "pt", "ro", "ru", "sk", "sl", "af", "sq", "ar", "bn", "bg", "zh", "hr", "cs", "da", "nl", "en",
			"et", "fi", "fr", "de", "el", "gu", "he", "hi", "hu", "id", "it", "ja", "kn", "ko", "so", "es", "sw", "sv", "ta", "te", "th", "tl", "tr", "uk", "ur", "vi" };

	private static List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();
	private static LanguageDetector langDetect = LanguageDetectorBuilder.create(NgramExtractors.standard()).withProfiles(languageProfiles).build();
	// public LanguageIdentifier(){
	// this.languageProfiles = new LanguageProfileReader().readAllBuiltIn();
	// this.langDetect =
	// LanguageDetectorBuilder.create(NgramExtractors.standard())
	// .withProfiles(this.languageProfiles)
	// .build();
	// }

	private LanguageIdentifier() {

	}

	public static synchronized String identifyLanguage(String input) {
		TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();

		TextObject textObject = textObjectFactory.forText(input);
		DetectedLanguage result = langDetect.detect(textObject);
		if (result == null)
			return "other";

		String lang = "other";
		try {
			lang = result.getLocale().getLanguage();
			if (lang.length() > 2)
				lang = lang.substring(0, 2);
		} catch (Exception e) {
			e.printStackTrace();
			return "other";
		}

		if (!Arrays.asList(langs639_1).contains(lang))
			return "other";

		return lang;
	}

	public static synchronized float identifyLanguage(String input, String target) {
		TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();

		TextObject textObject = textObjectFactory.forText(input);
		DetectedLanguage result = langDetect.detect(textObject);
		
		if (result == null)
			return 0;

		String lang = "other";
		try {
			lang = result.getLocale().getLanguage();
			if (lang.length() > 2)
				lang = lang.substring(0, 2);
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}

		if (!Arrays.asList(langs639_1).contains(lang))
			return 0;

		if (lang.equals(target))
			return (float) result.getProbability();

		return 0;
	}

	public static synchronized float identifyLanguage(byte[] input, String target) {
		try {
			ByteArrayInputStream b = new ByteArrayInputStream(input);
			Source sc = new Source(b);
			float output = identifyLanguage(sc.getTextExtractor().toString(), target);
			b.close();
			return output;
		} catch (Exception e) {
			return 0;
		}
	}
}
