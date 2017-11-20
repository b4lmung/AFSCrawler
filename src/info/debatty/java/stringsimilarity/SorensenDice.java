/*
 * The MIT License
 *
 * Copyright 2015 Thibault Debatty.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package info.debatty.java.stringsimilarity;

import info.debatty.java.stringsimilarity.interfaces.NormalizedStringSimilarity;
import info.debatty.java.stringsimilarity.interfaces.NormalizedStringDistance;

/**
 * Similar to Jaccard index, but this time the similarity is computed as 2 * |V1
 * inter V2| / (|V1| + |V2|). Distance is computed as 1 - cosine similarity.
 *
 * @author Thibault Debatty
 */
public class SorensenDice extends ShingleBased implements
        NormalizedStringDistance, NormalizedStringSimilarity {

    /**
	 * 
	 */
	private static final long serialVersionUID = 213031780648374309L;

	/**
     * Sorensen-Dice coefficient, aka Sørensen index, Dice's coefficient or
     * Czekanowski's binary (non-quantitative) index.
     *
     * The strings are first converted to boolean sets of k-shingles (sequences
     * of k characters), then the similarity is computed as 2 * |A inter B| /
     * (|A| + |B|). Attention: Sorensen-Dice distance (and similarity) does not
     * satisfy triangle inequality.
     *
     * @param k
     */
    public SorensenDice(int k) {
        super(k);
    }

    public SorensenDice() {
        super(3);
    }

    public double similarity(String s1, String s2) {
        KShingling ks = new KShingling(k);
        int[] profile1 = ks.getArrayProfile(s1);
        int[] profile2 = ks.getArrayProfile(s2);

        int length = Math.max(profile1.length, profile2.length);
        profile1 = java.util.Arrays.copyOf(profile1, length);
        profile2 = java.util.Arrays.copyOf(profile2, length);

        int inter = 0;
        int sum = 0;
        for (int i = 0; i < length; i++) {
            if (profile1[i] > 0 && profile2[i] > 0) {
                inter++;
            }

            if (profile1[i] > 0) {
                sum++;
            }

            if (profile2[i] > 0) {
                sum++;
            }
        }

        return 2.0 * inter / sum;
    }

    public double distance(String s1, String s2) {
        return 1 - similarity(s1, s2);
    }
}
