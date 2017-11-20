package com.cybozu.labs.langdetect.util;

import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractor;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.ngram.OldNgramExtractor;

import java.util.Formatter;
import java.util.List;
import java.util.Map;

/**
 * A place for sharing code.
 *
 * @author Fabian Kessler
 */
public class Util {

    private static final NgramExtractor ngramExtractor = NgramExtractors.standard();

    public static void addCharSequence(LangProfile langProfile, CharSequence text) {
        //TODO replace with new code.

//        List<String> old = OldNgramExtractor.extractNGrams(text, null);
//        List<String> nuu = ngramExtractor.extractGrams(text);
//
//        Set<String> oldSet = new HashSet<>(old);
//        Set<String> nuuSet = new HashSet<>(nuu);
//
//        ArrayList<String> justNuu = new ArrayList<>(nuu);
//        justNuu.removeAll(old);
//
//        ArrayList<String> justOld = new ArrayList<>(old);
//        justOld.removeAll(nuu);
//
//        System.out.println(text);

//        for (String s : ngramExtractor.extractGrams(text)) {
//            langProfile.add(s);
//        }
        for (String s : OldNgramExtractor.extractNGrams(text, null)) {
            langProfile.add(s);
        }
    }



    /**
     * unicode encoding (for verbose mode)
     */
    public static String unicodeEncode(String s) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            if (ch >= '\u0080') {
                String st = Integer.toHexString(0x10000 + (int) ch);
                while (st.length() < 4) st = "0" + st;
                buf.append("\\u").append(st.subSequence(1, 5));
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }


    /**
     * normalize probabilities and check convergence by the maximum probability
     * @return maximum of probabilities
     */
    public static double normalizeProb(double[] prob) {
        double maxp = 0, sump = 0;
        for(int i=0;i<prob.length;++i) sump += prob[i];
        for(int i=0;i<prob.length;++i) {
            double p = prob[i] / sump;
            if (maxp < p) maxp = p;
            prob[i] = p;
        }
        return maxp;
    }


    public static String wordProbToString(double[] prob, List<LdLocale> langlist) {
        Formatter formatter = new Formatter();
        for(int j=0;j<prob.length;++j) {
            double p = prob[j];
            if (p>=0.00001) {
                formatter.format(" %s:%.5f", langlist.get(j), p);
            }
        }
        String output = formatter.toString();
        formatter.close();
        return output;
    }


    /**
     */
    public static double[] makeInternalPrioMap( Map<LdLocale, Double> langWeightingMap,
                                                List<LdLocale> langlist) {
        assert !langWeightingMap.isEmpty();
        double[] priorMap = new double[langlist.size()];
        double sump = 0;
        for (int i=0;i<priorMap.length;++i) {
            LdLocale lang = langlist.get(i);
            if (langWeightingMap.containsKey(lang)) {
                double p = langWeightingMap.get(lang);
                assert p>=0 : "Prior probability must be non-negative!";
                priorMap[i] = p;
                sump += p;
            }
        }
        assert sump > 0 : "Sum must be greater than zero!";
        for (int i=0;i<priorMap.length;++i) priorMap[i] /= sump;
        return priorMap;
    }

}
