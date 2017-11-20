package com.optimaize.langdetect.ngram;

/**
 * Filters those that were not generated by the old n-gram generator.
 *
 * @author Fabian Kessler
 */
public class BackwardsCompatibleNgramFilter implements NgramFilter {

    private static final BackwardsCompatibleNgramFilter INSTANCE = new BackwardsCompatibleNgramFilter();

    public static NgramFilter getInstance() {
        return INSTANCE;
    }

    private BackwardsCompatibleNgramFilter() {
    }


    @Override
    public boolean use(String ngram) {
        switch (ngram.length()) {
            case 1:
                if (ngram.charAt(0)==' ') {
                    return false;
                }
                return true;
            case 2:
                if (Character.isUpperCase(ngram.charAt(0)) && Character.isUpperCase(ngram.charAt(1))) {
                    //all upper case
                    return false;
                }
                return true;
            case 3:
                if (Character.isUpperCase(ngram.charAt(0)) && Character.isUpperCase(ngram.charAt(1)) && Character.isUpperCase(ngram.charAt(2))) {
                    //all upper case
                    return false;
                }
                if (ngram.charAt(1)==' ') {
                    //middle char is a space
                    return false;
                }
                return true;
            default:
                throw new UnsupportedOperationException("Unsupported n-gram length: "+ngram.length());
        }
    }

}
