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

import info.debatty.java.utils.SparseBooleanVector;

/**
 * Set representation of a string (list of occuring shingles/n-grams), without
 * cardinality.
 * @author Thibault Debatty
 */
public class StringSet {
    private final SparseBooleanVector vector;
    private final KShingling ks;
    
    public StringSet(SparseBooleanVector vector, KShingling ks) {
        this.vector = vector;
        this.ks = ks;
    }
    
    
    public double jaccardSimilarity(StringSet other) throws Exception {
        if (this.ks != other.ks) {
            throw new Exception("Profiles were not created using the same kshingling object!");
        }
        
        return this.vector.jaccard(other.vector);
    }
    
    public double sorensenDiceSimilarity(StringSet other) throws Exception {
        if (this.ks != other.ks) {
            throw new Exception("Profiles were not created using the same kshingling object!");
        }
        
        return 2 * this.vector.intersection(other.vector) / (this.vector.size() + other.vector.size());
    }
}
