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
package info.debatty.java.stringsimilarity.examples;

import com.job.ic.utils.StringUtils;

import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.Jaccard;

/**
 *
 * @author Thibault Debatty
 */
public class Examples {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Jaccard index
        // =============
        Jaccard j2 = new Jaccard(2);
        Cosine c = new Cosine();
        // AB BC CD DE DF
        // 1  1  1  1  0
        // 1  1  1  0  1
        // => 3 / 5 = 0.6
        String s1 = "http://www.sawadee.co.th/hotel/638350/";
        String s2 = "http://www.sawadee.cn/hotel/638350/&lx=en&lg=cn&cur=CNY";
        
//        s1 = s1.substring(0, s1.indexOf("&"));
        s1 = StringUtils.getUniqueWordFromURL(s1);
        s2 = StringUtils.getUniqueWordFromURL(s2);
		
		System.out.println(s1);
		System.out.println(s2);
		
		System.out.println(c.similarity(s1,  s2));

    }

}
