package com.optimaize.langdetect;

import com.cybozu.labs.langdetect.util.Util;
import com.google.common.base.Optional;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractor;

import java.util.*;

/**
 *
 *
 * <p>This class is immutable and thus thread-safe.</p>
 *
 * @author Nakatani Shuyo
 * @author Fabian Kessler
 * @author Elmer Garduno
 */
public final class LanguageDetectorImpl implements LanguageDetector {

//    private static final Logger logger = LoggerFactory.getLogger(LanguageDetectorImpl.class);

    /**
     * TODO document what this is for, and why that value is chosen.
     */
    private static final double ALPHA_WIDTH = 0.05;

    /**
     * TODO document what this is for, and why that value is chosen.
     */
    private static final int ITERATION_LIMIT = 1000;

    /**
     * TODO document what this is for, and why that value is chosen.
     */
    private static final double CONV_THRESHOLD = 0.99999;

    /**
     * TODO document what this is for, and why that value is chosen.
     */
    private static final int BASE_FREQ = 10000;

    /**
     * TODO document what this is for, and why that value is chosen.
     */
    private static final int N_TRIAL = 7;

    /**
     * This is used when no custom seed was passed in.
     * By using the same seed for different calls, the results are consistent also.
     *
     * Changing this number means that users of the library might suddenly see other results after updating.
     * So don't change it hastily. I chose a prime number *clueless*.
     * See https://github.com/optimaize/language-detector/issues/14
     */
    private static final long DEFAULT_SEED = 41L;

    
    private final NgramFrequencyData ngramFrequencyData;

    /**
     * User-defined language priorities, in the same order as {@code langlist}.
     */
   
    private final double[] priorMap;

    private final double alpha;
    private final Optional<Long> seed;
    private final int shortTextAlgorithm;
    private final double prefixFactor;
    private final double suffixFactor;

    private final double probabilityThreshold;
    private final double minimalConfidence;

    private final NgramExtractor ngramExtractor;


    /**
     * Use the {@link LanguageDetectorBuilder}.
     */
    LanguageDetectorImpl(NgramFrequencyData ngramFrequencyData,
                         double alpha, Optional<Long> seed, int shortTextAlgorithm,
                         double prefixFactor, double suffixFactor,
                         double probabilityThreshold,
                         double minimalConfidence,
                         Map<LdLocale, Double> langWeightingMap,
                         NgramExtractor ngramExtractor) {
        if (alpha<0d || alpha >1d) throw new IllegalArgumentException("alpha must be between 0 and 1, but was: "+alpha);
        if (prefixFactor <0d || prefixFactor >10d) throw new IllegalArgumentException("prefixFactor must be between 0 and 10, but was: "+prefixFactor);
        if (suffixFactor <0d || suffixFactor >10d) throw new IllegalArgumentException("suffixFactor must be between 0 and 10, but was: "+suffixFactor);
        if (probabilityThreshold<0d || probabilityThreshold>1d) throw new IllegalArgumentException("probabilityThreshold must be between 0 and 1, but was: "+probabilityThreshold);
        if (minimalConfidence<0d || minimalConfidence>1d) throw new IllegalArgumentException("minimalConfidence must be between 0 and 1, but was: "+minimalConfidence);
        if (langWeightingMap!=null && langWeightingMap.isEmpty()) langWeightingMap = null;

        this.ngramFrequencyData = ngramFrequencyData;
        this.alpha = alpha;
        this.seed = seed;
        this.shortTextAlgorithm = shortTextAlgorithm;
        this.prefixFactor = prefixFactor;
        this.suffixFactor = suffixFactor;
        this.probabilityThreshold = probabilityThreshold;
        this.minimalConfidence = minimalConfidence;
        this.priorMap = (langWeightingMap==null) ? null : Util.makeInternalPrioMap(langWeightingMap, ngramFrequencyData.getLanguageList());
        this.ngramExtractor = ngramExtractor;
    }


    @Override
    public DetectedLanguage detect(CharSequence text) {
        List<DetectedLanguage> probabilities = getProbabilities(text);
        if (probabilities.isEmpty()) {
            return null;
        } else {
            DetectedLanguage best = probabilities.get(0);
            if (best.getProbability() >= minimalConfidence) {
                return best;
            } else {
                return null;
            }
        }
    }

    @Override
    public List<DetectedLanguage> getProbabilities(CharSequence text) {
        double[] langprob = detectBlock(text);
        if (langprob==null) {
            return Collections.emptyList();
        } else {
            return sortProbability(langprob);
        }
    }


    /**
     * @return null if there are no "features" in the text (just noise).
     */
    
    private double[] detectBlock(CharSequence text) {
        if (text.length() <= shortTextAlgorithm) {
            Map<String, Integer> ngrams = ngramExtractor.extractCountedGrams(text);
            if (ngrams.isEmpty()) return null;
            return detectBlockShortText(ngrams);
        } else {
            List<String> strings = ngramExtractor.extractGrams(text);
            if (strings.isEmpty()) return null;
            return detectBlockLongText(strings);
        }
    }

    /**
     */
    private double[] detectBlockShortText(Map<String, Integer> ngrams) {
        double[] prob = initProbability();
        double alpha = this.alpha; //TODO I don't understand what this does.
        for (Map.Entry<String, Integer> gramWithCount : ngrams.entrySet()) {
            updateLangProb(prob, gramWithCount.getKey(), gramWithCount.getValue(), alpha);
        }
        Util.normalizeProb(prob);
        return prob;
    }

    /**
     * This is the original algorithm used for all text length.
     * It is inappropriate for short text.
     */
    private double[] detectBlockLongText(List<String> ngrams) {
        assert !ngrams.isEmpty();
        double[] langprob = new double[ngramFrequencyData.getLanguageList().size()];
        Random rand = new Random(seed.or(DEFAULT_SEED));
        for (int t = 0; t < N_TRIAL; ++t) {
            double[] prob = initProbability();
            double alpha = this.alpha + (rand.nextGaussian() * ALPHA_WIDTH);

            for (int i=0; i<ITERATION_LIMIT; i++) {
                int r = rand.nextInt(ngrams.size());
                updateLangProb(prob, ngrams.get(r), 1, alpha);
                if (i % 5 == 0) {
                    if (Util.normalizeProb(prob) > CONV_THRESHOLD) break; //this looks like an optimization to return quickly when sure. TODO document what's the plan.
//                    if (logger.isTraceEnabled()) logger.trace("> " + sortProbability(prob));
                }
            }
            for(int j=0;j<langprob.length;++j) langprob[j] += prob[j] / N_TRIAL;
//            if (logger.isDebugEnabled()) logger.debug("==> " + sortProbability(prob));
        }
        return langprob;
    }

    /**
     * Initialize the map of language probabilities.
     * If there is the specified prior map, use it as initial map.
     * @return initialized map of language probabilities
     */
    private double[] initProbability() {
        double[] prob = new double[ngramFrequencyData.getLanguageList().size()];
        if (priorMap != null) {
            //TODO analyze and optimize this code, looks like double copy.
            System.arraycopy(priorMap, 0, prob, 0, prob.length);
            for(int i=0;i<prob.length;++i) prob[i] = priorMap[i];
        } else {
            for(int i=0;i<prob.length;++i) prob[i] = 1.0 / ngramFrequencyData.getLanguageList().size();
        }
        return prob;
    }


    /**
     * update language probabilities with N-gram string(N=1,2,3)
     * @param count 1-n: how often the gram occurred.
     */
    private boolean updateLangProb(double[] prob, String ngram, int count, double alpha) {
        double[] langProbMap = ngramFrequencyData.getProbabilities(ngram);
        if (langProbMap==null) {
            return false;
        }

//        if (logger.isTraceEnabled()) logger.trace(ngram + "(" + Util.unicodeEncode(ngram) + "):" + Util.wordProbToString(langProbMap, ngramFrequencyData.getLanguageList()));

        double weight = alpha / BASE_FREQ;
        if (ngram.length() >1) {
            if (prefixFactor !=1.0 && ngram.charAt(0)==' ') {
                weight *= prefixFactor;
            } else if (suffixFactor!=1.0 && ngram.charAt(ngram.length()-1)==' ') {
                weight *= suffixFactor;
            }
        }
        for (int i=0; i<prob.length; ++i) {
            for (int amount=0; amount<count; amount++) {
                prob[i] *= (weight + langProbMap[i]);
            }
        }
        return true;
    }


    /**
     * Returns the detected languages sorted by probabilities descending.
     * Languages with less probability than PROB_THRESHOLD are ignored.
     */
    
    private List<DetectedLanguage> sortProbability(double[] prob) {
        List<DetectedLanguage> list = new ArrayList<>();
        for (int j=0;j<prob.length;++j) {
            double p = prob[j];
            if (p >= probabilityThreshold) {
                for (int i=0; i<=list.size(); ++i) {
                    if (i == list.size() || list.get(i).getProbability() < p) {
                        list.add(i, new DetectedLanguage(ngramFrequencyData.getLanguage(j), p));
                        break;
                    }
                }
            }
        }
        return list;
    }

}
