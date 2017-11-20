/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    StringToWordVector.java
 *    Copyright (C) 2002-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package com.job.ic.ml.classifiers;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.SparseInstance;
import weka.core.stopwords.StopwordsHandler;
import weka.core.stopwords.Null;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.stemmers.NullStemmer;
import weka.core.stemmers.Stemmer;
import weka.core.tokenizers.Tokenizer;
import weka.core.tokenizers.WordTokenizer;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;

/**
 * <!-- globalinfo-start --> Converts String attributes into a set of attributes
 * representing word occurrence (depending on the tokenizer) information from
 * the text contained in the strings. The set of words (attributes) is
 * determined by the first batch filtered (typically training data).
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 *  -C
 *  Output word counts rather than boolean word presence.
 * </pre>
 * 
 * <pre>
 *  -R &lt;index1,index2-index4,...&gt;
 *  Specify list of string attributes to convert to words (as weka Range).
 *  (default: select all string attributes)
 * </pre>
 * 
 * <pre>
 *  -V
 *  Invert matching sense of column indexes.
 * </pre>
 * 
 * <pre>
 *  -P &lt;attribute name prefix&gt;
 *  Specify a prefix for the created attribute names.
 *  (default: "")
 * </pre>
 * 
 * <pre>
 *  -W &lt;number of words to keep&gt;
 *  Specify approximate number of word fields to create.
 *  Surplus words will be discarded..
 *  (default: 1000)
 * </pre>
 * 
 * <pre>
 *  -prune-rate &lt;rate as a percentage of dataset&gt;
 *  Specify the rate (e.g., every 10% of the input dataset) at which to periodically prune the dictionary.
 *  -W prunes after creating a full dictionary. You may not have enough memory for this approach.
 *  (default: no periodic pruning)
 * </pre>
 * 
 * <pre>
 *  -T
 *  Transform the word frequencies into log(1+fij)
 *  where fij is the frequency of word i in jth document(instance).
 * </pre>
 * 
 * <pre>
 *  -I
 *  Transform each word frequency into:
 *  fij*log(num of Documents/num of documents containing word i)
 *    where fij if frequency of word i in jth document(instance)
 * </pre>
 * 
 * <pre>
 *  -N
 *  Whether to 0=not normalize/1=normalize all data/2=normalize test data only
 *  to average length of training documents (default 0=don't normalize).
 * </pre>
 * 
 * <pre>
 *  -L
 *  Convert all tokens to lowercase before adding to the dictionary.
 * </pre>
 * 
 * <pre>
 *  -stopwords-handler
 *  The stopwords handler to use (default Null).
 * </pre>
 * 
 * <pre>
 *  -stemmer &lt;spec&gt;
 *  The stemming algorithm (classname plus parameters) to use.
 * </pre>
 * 
 * <pre>
 *  -M &lt;int&gt;
 *  The minimum term frequency (default = 1).
 * </pre>
 * 
 * <pre>
 *  -O
 *  If this is set, the maximum number of words and the 
 *  minimum term frequency is not enforced on a per-class 
 *  basis but based on the documents in all the classes 
 *  (even if a class attribute is set).
 * </pre>
 * 
 * <pre>
 *  -tokenizer &lt;spec&gt;
 *  The tokenizing algorihtm (classname plus parameters) to use.
 *  (default: weka.core.tokenizers.WordTokenizer)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Len Trigg (len@reeltwo.com)
 * @author Stuart Inglis (stuart@reeltwo.com)
 * @author Gordon Paynter (gordon.paynter@ucr.edu)
 * @author Asrhaf M. Kibriya (amk14@cs.waikato.ac.nz)
 * @version $Revision: 10984 $
 */
public class MyStringToWordVector extends Filter implements UnsupervisedFilter, OptionHandler {

	/** for serialization. */
	static final long serialVersionUID = 8249106275278565424L;

	/** Range of columns to convert to word vectors. */
	protected Range m_SelectedRange = new Range("first-last");

	/** Contains a mapping of valid words to attribute indexes. */
	private TreeMap<String, Integer> m_Dictionary = new TreeMap<String, Integer>();

	/**
	 * True if output instances should contain word frequency rather than
	 * boolean 0 or 1.
	 */
	private boolean m_OutputCounts = false;

	/** A String prefix for the attribute names. */
	private String m_Prefix = "";

	/**
	 * Contains the number of documents (instances) a particular word appears
	 * in. The counts are stored with the same indexing as given by
	 * m_Dictionary.
	 */
	private int[] m_DocsCounts;

	/**
	 * Contains the number of documents (instances) in the input format from
	 * which the dictionary is created. It is used in IDF transform.
	 */
	private int m_NumInstances = -1;

	/**
	 * Contains the average length of documents (among the first batch of
	 * instances aka training data). This is used in length normalization of
	 * documents which will be normalized to average document length.
	 */
	private double m_AvgDocLength = -1;

	/**
	 * The default number of words (per class if there is a class attribute
	 * assigned) to attempt to keep.
	 */
	private int m_WordsToKeep = 1000;

	/**
	 * The percentage at which to periodically prune the dictionary.
	 */
	private double m_PeriodicPruningRate = -1;

	/**
	 * True if word frequencies should be transformed into log(1+fi) where fi is
	 * the frequency of word i.
	 */
	private boolean m_TFTransform;

	/** The normalization to apply. */
	protected int m_filterType = FILTER_NONE;

	/** normalization: No normalization. */
	public static final int FILTER_NONE = 0;
	/** normalization: Normalize all data. */
	public static final int FILTER_NORMALIZE_ALL = 1;
	/** normalization: Normalize test data only. */
	public static final int FILTER_NORMALIZE_TEST_ONLY = 2;

	/**
	 * Specifies whether document's (instance's) word frequencies are to be
	 * normalized. The are normalized to average length of documents specified
	 * as input format.
	 */
	public static final Tag[] TAGS_FILTER = { new Tag(FILTER_NONE, "No normalization"), new Tag(FILTER_NORMALIZE_ALL, "Normalize all data"),
			new Tag(FILTER_NORMALIZE_TEST_ONLY, "Normalize test data only"), };

	/**
	 * True if word frequencies should be transformed into
	 * fij*log(numOfDocs/numOfDocsWithWordi).
	 */
	private boolean m_IDFTransform;

	/** True if all tokens should be downcased. */
	private boolean m_lowerCaseTokens;

	/** the stemming algorithm. */
	private Stemmer m_Stemmer = new NullStemmer();

	/** the minimum (per-class) word frequency. */
	private int m_minTermFreq = 1;

	/** whether to operate on a per-class basis. */
	private boolean m_doNotOperateOnPerClassBasis = false;

	/** Stopword handler to use. */
	private StopwordsHandler m_StopwordsHandler = new Null();

	/** the tokenizer algorithm to use. */
	private Tokenizer m_Tokenizer = new WordTokenizer();

	/**
	 * Default constructor. Targets 1000 words in the output.
	 */
	public MyStringToWordVector() {
	}

	/**
	 * Returns an enumeration describing the available options.
	 * 
	 * @return an enumeration of all the available options
	 */
	@Override
	public Enumeration<Option> listOptions() {

		Vector<Option> result = new Vector<Option>();

		result.addElement(new Option("\tOutput word counts rather than boolean word presence.\n", "C", 0, "-C"));

		result.addElement(
				new Option("\tSpecify list of string attributes to convert to words (as weka Range).\n" + "\t(default: select all string attributes)", "R", 1, "-R <index1,index2-index4,...>"));

		result.addElement(new Option("\tInvert matching sense of column indexes.", "V", 0, "-V"));

		result.addElement(new Option("\tSpecify a prefix for the created attribute names.\n" + "\t(default: \"\")", "P", 1, "-P <attribute name prefix>"));

		result.addElement(
				new Option("\tSpecify approximate number of word fields to create.\n" + "\tSurplus words will be discarded..\n" + "\t(default: 1000)", "W", 1, "-W <number of words to keep>"));

		result.addElement(new Option(
				"\tSpecify the rate (e.g., every 10% of the input dataset) at which to periodically prune the dictionary.\n"
						+ "\t-W prunes after creating a full dictionary. You may not have enough memory for this approach.\n" + "\t(default: no periodic pruning)",
				"prune-rate", 1, "-prune-rate <rate as a percentage of dataset>"));

		result.addElement(new Option("\tTransform the word frequencies into log(1+fij)\n" + "\twhere fij is the frequency of word i in jth document(instance).\n", "T", 0, "-T"));

		result.addElement(new Option(
				"\tTransform each word frequency into:\n" + "\tfij*log(num of Documents/num of documents containing word i)\n" + "\t  where fij if frequency of word i in jth document(instance)", "I",
				0, "-I"));

		result.addElement(new Option("\tWhether to 0=not normalize/1=normalize all data/2=normalize test data only\n" + "\tto average length of training documents " + "(default 0=don\'t normalize).",
				"N", 1, "-N"));

		result.addElement(new Option("\tConvert all tokens to lowercase before " + "adding to the dictionary.", "L", 0, "-L"));

		result.addElement(new Option("\tThe stopwords handler to use (default Null).", "-stopwords-handler", 1, "-stopwords-handler"));

		result.addElement(new Option("\tThe stemming algorithm (classname plus parameters) to use.", "stemmer", 1, "-stemmer <spec>"));

		result.addElement(new Option("\tThe minimum term frequency (default = 1).", "M", 1, "-M <int>"));

		result.addElement(new Option("\tIf this is set, the maximum number of words and the \n" + "\tminimum term frequency is not enforced on a per-class \n"
				+ "\tbasis but based on the documents in all the classes \n" + "\t(even if a class attribute is set).", "O", 0, "-O"));

		result.addElement(new Option("\tThe tokenizing algorihtm (classname plus parameters) to use.\n" + "\t(default: " + WordTokenizer.class.getName() + ")", "tokenizer", 1, "-tokenizer <spec>"));

		return result.elements();
	}

	/**
	 * Parses a given list of options.
	 * <p/>
	 * 
	 * <!-- options-start --> Valid options are:
	 * <p/>
	 * 
	 * <pre>
	 *  -C
	 *  Output word counts rather than boolean word presence.
	 * </pre>
	 * 
	 * <pre>
	 *  -R &lt;index1,index2-index4,...&gt;
	 *  Specify list of string attributes to convert to words (as weka Range).
	 *  (default: select all string attributes)
	 * </pre>
	 * 
	 * <pre>
	 *  -V
	 *  Invert matching sense of column indexes.
	 * </pre>
	 * 
	 * <pre>
	 *  -P &lt;attribute name prefix&gt;
	 *  Specify a prefix for the created attribute names.
	 *  (default: "")
	 * </pre>
	 * 
	 * <pre>
	 *  -W &lt;number of words to keep&gt;
	 *  Specify approximate number of word fields to create.
	 *  Surplus words will be discarded..
	 *  (default: 1000)
	 * </pre>
	 * 
	 * <pre>
	 *  -prune-rate &lt;rate as a percentage of dataset&gt;
	 *  Specify the rate (e.g., every 10% of the input dataset) at which to periodically prune the dictionary.
	 *  -W prunes after creating a full dictionary. You may not have enough memory for this approach.
	 *  (default: no periodic pruning)
	 * </pre>
	 * 
	 * <pre>
	 *  -T
	 *  Transform the word frequencies into log(1+fij)
	 *  where fij is the frequency of word i in jth document(instance).
	 * </pre>
	 * 
	 * <pre>
	 *  -I
	 *  Transform each word frequency into:
	 *  fij*log(num of Documents/num of documents containing word i)
	 *    where fij if frequency of word i in jth document(instance)
	 * </pre>
	 * 
	 * <pre>
	 *  -N
	 *  Whether to 0=not normalize/1=normalize all data/2=normalize test data only
	 *  to average length of training documents (default 0=don't normalize).
	 * </pre>
	 * 
	 * <pre>
	 *  -L
	 *  Convert all tokens to lowercase before adding to the dictionary.
	 * </pre>
	 * 
	 * <pre>
	 *  -stopwords-handler
	 *  The stopwords handler to use (default Null).
	 * </pre>
	 * 
	 * <pre>
	 *  -stemmer &lt;spec&gt;
	 *  The stemming algorithm (classname plus parameters) to use.
	 * </pre>
	 * 
	 * <pre>
	 *  -M &lt;int&gt;
	 *  The minimum term frequency (default = 1).
	 * </pre>
	 * 
	 * <pre>
	 *  -O
	 *  If this is set, the maximum number of words and the 
	 *  minimum term frequency is not enforced on a per-class 
	 *  basis but based on the documents in all the classes 
	 *  (even if a class attribute is set).
	 * </pre>
	 * 
	 * <pre>
	 *  -tokenizer &lt;spec&gt;
	 *  The tokenizing algorihtm (classname plus parameters) to use.
	 *  (default: weka.core.tokenizers.WordTokenizer)
	 * </pre>
	 * 
	 * <!-- options-end -->
	 * 
	 * @param options
	 *            the list of options as an array of strings
	 * @throws Exception
	 *             if an option is not supported
	 */
	@Override
	public void setOptions(String[] options) throws Exception {

		String value = Utils.getOption('R', options);
		if (value.length() != 0) {
			setSelectedRange(value);
		} else {
			setSelectedRange("first-last");
		}

		setInvertSelection(Utils.getFlag('V', options));

		value = Utils.getOption('P', options);
		if (value.length() != 0) {
			setAttributeNamePrefix(value);
		} else {
			setAttributeNamePrefix("");
		}

		value = Utils.getOption('W', options);
		if (value.length() != 0) {
			setWordsToKeep(Integer.valueOf(value).intValue());
		} else {
			setWordsToKeep(1000);
		}

		value = Utils.getOption("prune-rate", options);
		if (value.length() > 0) {
			setPeriodicPruning(Double.parseDouble(value));
		} else {
			setPeriodicPruning(-1);
		}

		value = Utils.getOption('M', options);
		if (value.length() != 0) {
			setMinTermFreq(Integer.valueOf(value).intValue());
		} else {
			setMinTermFreq(1);
		}

		setOutputWordCounts(Utils.getFlag('C', options));

		setTFTransform(Utils.getFlag('T', options));

		setIDFTransform(Utils.getFlag('I', options));

		setDoNotOperateOnPerClassBasis(Utils.getFlag('O', options));

		String nString = Utils.getOption('N', options);
		if (nString.length() != 0) {
			setNormalizeDocLength(new SelectedTag(Integer.parseInt(nString), TAGS_FILTER));
		} else {
			setNormalizeDocLength(new SelectedTag(FILTER_NONE, TAGS_FILTER));
		}

		setLowerCaseTokens(Utils.getFlag('L', options));

		String stemmerString = Utils.getOption("stemmer", options);
		if (stemmerString.length() == 0) {
			setStemmer(null);
		} else {
			String[] stemmerSpec = Utils.splitOptions(stemmerString);
			if (stemmerSpec.length == 0) {
				throw new Exception("Invalid stemmer specification string");
			}
			String stemmerName = stemmerSpec[0];
			stemmerSpec[0] = "";
			Stemmer stemmer = (Stemmer) Class.forName(stemmerName).newInstance();
			if (stemmer instanceof OptionHandler) {
				((OptionHandler) stemmer).setOptions(stemmerSpec);
			}
			setStemmer(stemmer);
		}

		String stopwordsHandlerString = Utils.getOption("stopwords-handler", options);
		if (stopwordsHandlerString.length() == 0) {
			setStopwordsHandler(null);
		} else {
			String[] stopwordsHandlerSpec = Utils.splitOptions(stopwordsHandlerString);
			if (stopwordsHandlerSpec.length == 0) {
				throw new Exception("Invalid StopwordsHandler specification string");
			}
			String stopwordsHandlerName = stopwordsHandlerSpec[0];
			stopwordsHandlerSpec[0] = "";
			StopwordsHandler stopwordsHandler = (StopwordsHandler) Class.forName(stopwordsHandlerName).newInstance();
			if (stopwordsHandler instanceof OptionHandler) {
				((OptionHandler) stopwordsHandler).setOptions(stopwordsHandlerSpec);
			}
			setStopwordsHandler(stopwordsHandler);
		}

		String tokenizerString = Utils.getOption("tokenizer", options);
		if (tokenizerString.length() == 0) {
			setTokenizer(new WordTokenizer());
		} else {
			String[] tokenizerSpec = Utils.splitOptions(tokenizerString);
			if (tokenizerSpec.length == 0) {
				throw new Exception("Invalid tokenizer specification string");
			}
			String tokenizerName = tokenizerSpec[0];
			tokenizerSpec[0] = "";
			Tokenizer tokenizer = (Tokenizer) Class.forName(tokenizerName).newInstance();
			if (tokenizer instanceof OptionHandler) {
				((OptionHandler) tokenizer).setOptions(tokenizerSpec);
			}
			setTokenizer(tokenizer);
		}

		Utils.checkForRemainingOptions(options);
	}

	/**
	 * Gets the current settings of the filter.
	 * 
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override
	public String[] getOptions() {

		Vector<String> result = new Vector<String>();

		result.add("-R");
		result.add(getSelectedRange().getRanges());

		if (getInvertSelection()) {
			result.add("-V");
		}

		if (!"".equals(getAttributeNamePrefix())) {
			result.add("-P");
			result.add(getAttributeNamePrefix());
		}

		result.add("-W");
		result.add(String.valueOf(getWordsToKeep()));

		result.add("-prune-rate");
		result.add(String.valueOf(getPeriodicPruning()));

		if (getOutputWordCounts()) {
			result.add("-C");
		}

		if (getTFTransform()) {
			result.add("-T");
		}

		if (getIDFTransform()) {
			result.add("-I");
		}

		result.add("-N");
		result.add("" + m_filterType);

		if (getLowerCaseTokens()) {
			result.add("-L");
		}

		if (getStemmer() != null) {
			result.add("-stemmer");
			String spec = getStemmer().getClass().getName();
			if (getStemmer() instanceof OptionHandler) {
				spec += " " + Utils.joinOptions(((OptionHandler) getStemmer()).getOptions());
			}
			result.add(spec.trim());
		}

		if (getStopwordsHandler() != null) {
			result.add("-stopwords-handler");
			String spec = getStopwordsHandler().getClass().getName();
			if (getStopwordsHandler() instanceof OptionHandler) {
				spec += " " + Utils.joinOptions(((OptionHandler) getStopwordsHandler()).getOptions());
			}
			result.add(spec.trim());
		}

		result.add("-M");
		result.add(String.valueOf(getMinTermFreq()));

		if (getDoNotOperateOnPerClassBasis()) {
			result.add("-O");
		}

		result.add("-tokenizer");
		String spec = getTokenizer().getClass().getName();
		if (getTokenizer() instanceof OptionHandler) {
			spec += " " + Utils.joinOptions(((OptionHandler) getTokenizer()).getOptions());
		}
		result.add(spec.trim());

		return result.toArray(new String[result.size()]);
	}

	/**
	 * Constructor that allows specification of the target number of words in
	 * the output.
	 * 
	 * @param wordsToKeep
	 *            the number of words in the output vector (per class if
	 *            assigned).
	 */
	public MyStringToWordVector(int wordsToKeep) {
		m_WordsToKeep = wordsToKeep;
	}

	/**
	 * Used to store word counts for dictionary selection based on a threshold.
	 */
	private class Count implements Serializable, RevisionHandler {

		/** for serialization. */
		static final long serialVersionUID = 2157223818584474321L;

		/** the counts. */
		public int count, docCount;

		/**
		 * the constructor.
		 * 
		 * @param c
		 *            the count
		 */
		public Count(int c) {
			count = c;
		}

		/**
		 * Returns the revision string.
		 * 
		 * @return the revision
		 */
		@Override
		public String getRevision() {
			return RevisionUtils.extract("$Revision: 10984 $");
		}
	}

	/**
	 * Returns the Capabilities of this filter.
	 * 
	 * @return the capabilities of this object
	 * @see Capabilities
	 */
	@Override
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();

		// attributes
		result.enableAllAttributes();
		result.enable(Capability.MISSING_VALUES);

		// class
		result.enableAllClasses();
		result.enable(Capability.MISSING_CLASS_VALUES);
		result.enable(Capability.NO_CLASS);

		return result;
	}

	/**
	 * Sets the format of the input instances.
	 * 
	 * @param instanceInfo
	 *            an Instances object containing the input instance structure
	 *            (any instances contained in the object are ignored - only the
	 *            structure is required).
	 * @return true if the outputFormat may be collected immediately
	 * @throws Exception
	 *             if the input format can't be set successfully
	 */
	@Override
	public boolean setInputFormat(Instances instanceInfo) throws Exception {

		super.setInputFormat(instanceInfo);
		m_SelectedRange.setUpper(instanceInfo.numAttributes() - 1);
		m_AvgDocLength = -1;
		m_NumInstances = -1;
		return false;
	}

	/**
	 * Input an instance for filtering. Filter requires all training instances
	 * be read before producing output.
	 * 
	 * @param instance
	 *            the input instance.
	 * @return true if the filtered instance may now be collected with output().
	 * @throws IllegalStateException
	 *             if no input structure has been defined.
	 */
	@Override
	public boolean input(Instance instance) throws Exception {

		if (getInputFormat() == null) {
			throw new IllegalStateException("No input instance format defined");
		}
		if (m_NewBatch) {
			resetQueue();
			m_NewBatch = false;
		}
		if (isFirstBatchDone()) {
			ArrayList<Instance> fv = new ArrayList<Instance>();
			int firstCopy = convertInstancewoDocNorm(instance, fv);
			Instance inst = fv.get(0);
			if (m_filterType != FILTER_NONE) {
				normalizeInstance(inst, firstCopy);
			}
			push(inst);
			return true;
		} else {
			bufferInput(instance);
			return false;
		}
	}

	/**
	 * Signify that this batch of input to the filter is finished. If the filter
	 * requires all instances prior to filtering, output() may now be called to
	 * retrieve the filtered instances.
	 * 
	 * @return true if there are instances pending output.
	 * @throws IllegalStateException
	 *             if no input structure has been defined.
	 */
	@Override
	public boolean batchFinished() throws Exception {

		if (getInputFormat() == null) {
			throw new IllegalStateException("No input instance format defined");
		}

		// We only need to do something in this method
		// if the first batch hasn't been processed. Otherwise
		// input() has already done all the work.
		if (!isFirstBatchDone()) {

			// turn of per-class mode if the class is not nominal (or is all
			// missing)!
			if (getInputFormat().classIndex() >= 0) {
				if (!getInputFormat().classAttribute().isNominal() || getInputFormat().attributeStats(getInputFormat().classIndex()).missingCount == getInputFormat().numInstances()) {
					m_doNotOperateOnPerClassBasis = true;
				}
			}

			// Determine the dictionary from the first batch (training data)
			determineDictionary();

			// Convert all instances w/o normalization
			ArrayList<Instance> fv = new ArrayList<Instance>();
			int firstCopy = 0;
			for (int i = 0; i < m_NumInstances; i++) {
				firstCopy = convertInstancewoDocNorm(getInputFormat().instance(i), fv);
			}

			// Need to compute average document length if necessary
			if (m_filterType != FILTER_NONE) {
				m_AvgDocLength = 0;
				for (int i = 0; i < fv.size(); i++) {
					Instance inst = fv.get(i);
					double docLength = 0;
					for (int j = 0; j < inst.numValues(); j++) {
						if (inst.index(j) >= firstCopy) {
							docLength += inst.valueSparse(j) * inst.valueSparse(j);
						}
					}
					m_AvgDocLength += Math.sqrt(docLength);
				}
				m_AvgDocLength /= m_NumInstances;
			}

			// Perform normalization if necessary.
			if (m_filterType == FILTER_NORMALIZE_ALL) {
				for (int i = 0; i < fv.size(); i++) {
					normalizeInstance(fv.get(i), firstCopy);
				}
			}

			// Push all instances into the output queue
			for (int i = 0; i < fv.size(); i++) {
				push(fv.get(i));
			}
		}

		// Flush the input
		flushInput();

		m_NewBatch = true;
		m_FirstBatchDone = true;
		return (numPendingOutput() != 0);
	}

	/**
	 * Returns a string describing this filter.
	 * 
	 * @return a description of the filter suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String globalInfo() {
		return "Converts String attributes into a set of attributes representing " + "word occurrence (depending on the tokenizer) information from the "
				+ "text contained in the strings. The set of words (attributes) is " + "determined by the first batch filtered (typically training data).";
	}

	/**
	 * Gets whether output instances contain 0 or 1 indicating word presence, or
	 * word counts.
	 * 
	 * @return true if word counts should be output.
	 */
	public boolean getOutputWordCounts() {
		return m_OutputCounts;
	}

	/**
	 * Sets whether output instances contain 0 or 1 indicating word presence, or
	 * word counts.
	 * 
	 * @param outputWordCounts
	 *            true if word counts should be output.
	 */
	public void setOutputWordCounts(boolean outputWordCounts) {
		m_OutputCounts = outputWordCounts;
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String outputWordCountsTipText() {
		return "Output word counts rather than boolean 0 or 1" + "(indicating presence or absence of a word).";
	}

	/**
	 * Get the value of m_SelectedRange.
	 * 
	 * @return Value of m_SelectedRange.
	 */
	public Range getSelectedRange() {
		return m_SelectedRange;
	}

	/**
	 * Set the value of m_SelectedRange.
	 * 
	 * @param newSelectedRange
	 *            Value to assign to m_SelectedRange.
	 */
	public void setSelectedRange(String newSelectedRange) {
		m_SelectedRange = new Range(newSelectedRange);
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String attributeIndicesTipText() {
		return "Specify range of attributes to act on." + " This is a comma separated list of attribute indices, with" + " \"first\" and \"last\" valid values. Specify an inclusive"
				+ " range with \"-\". E.g: \"first-3,5,6-10,last\".";
	}

	/**
	 * Gets the current range selection.
	 * 
	 * @return a string containing a comma separated list of ranges
	 */
	public String getAttributeIndices() {
		return m_SelectedRange.getRanges();
	}

	/**
	 * Sets which attributes are to be worked on.
	 * 
	 * @param rangeList
	 *            a string representing the list of attributes. Since the string
	 *            will typically come from a user, attributes are indexed from
	 *            1. <br>
	 *            eg: first-3,5,6-last
	 * @throws IllegalArgumentException
	 *             if an invalid range list is supplied
	 */
	public void setAttributeIndices(String rangeList) {
		m_SelectedRange.setRanges(rangeList);
	}

	/**
	 * Sets which attributes are to be processed.
	 * 
	 * @param attributes
	 *            an array containing indexes of attributes to process. Since
	 *            the array will typically come from a program, attributes are
	 *            indexed from 0.
	 * @throws IllegalArgumentException
	 *             if an invalid set of ranges is supplied
	 */
	public void setAttributeIndicesArray(int[] attributes) {
		setAttributeIndices(Range.indicesToRangeList(attributes));
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String invertSelectionTipText() {
		return "Set attribute selection mode. If false, only selected" + " attributes in the range will be worked on; if" + " true, only non-selected attributes will be processed.";
	}

	/**
	 * Gets whether the supplied columns are to be processed or skipped.
	 * 
	 * @return true if the supplied columns will be kept
	 */
	public boolean getInvertSelection() {
		return m_SelectedRange.getInvert();
	}

	/**
	 * Sets whether selected columns should be processed or skipped.
	 * 
	 * @param invert
	 *            the new invert setting
	 */
	public void setInvertSelection(boolean invert) {
		m_SelectedRange.setInvert(invert);
	}

	/**
	 * Get the attribute name prefix.
	 * 
	 * @return The current attribute name prefix.
	 */
	public String getAttributeNamePrefix() {
		return m_Prefix;
	}

	/**
	 * Set the attribute name prefix.
	 * 
	 * @param newPrefix
	 *            String to use as the attribute name prefix.
	 */
	public void setAttributeNamePrefix(String newPrefix) {
		m_Prefix = newPrefix;
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String attributeNamePrefixTipText() {
		return "Prefix for the created attribute names. " + "(default: \"\")";
	}

	/**
	 * Gets the number of words (per class if there is a class attribute
	 * assigned) to attempt to keep.
	 * 
	 * @return the target number of words in the output vector (per class if
	 *         assigned).
	 */
	public int getWordsToKeep() {
		return m_WordsToKeep;
	}

	/**
	 * Sets the number of words (per class if there is a class attribute
	 * assigned) to attempt to keep.
	 * 
	 * @param newWordsToKeep
	 *            the target number of words in the output vector (per class if
	 *            assigned).
	 */
	public void setWordsToKeep(int newWordsToKeep) {
		m_WordsToKeep = newWordsToKeep;
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String wordsToKeepTipText() {
		return "The number of words (per class if there is a class attribute " + "assigned) to attempt to keep.";
	}

	/**
	 * Gets the rate at which the dictionary is periodically pruned, as a
	 * percentage of the dataset size.
	 * 
	 * @return the rate at which the dictionary is periodically pruned
	 */
	public double getPeriodicPruning() {
		return m_PeriodicPruningRate;
	}

	/**
	 * Sets the rate at which the dictionary is periodically pruned, as a
	 * percentage of the dataset size.
	 * 
	 * @param newPeriodicPruning
	 *            the rate at which the dictionary is periodically pruned
	 */
	public void setPeriodicPruning(double newPeriodicPruning) {
		m_PeriodicPruningRate = newPeriodicPruning;
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String periodicPruningTipText() {
		return "Specify the rate (x% of the input dataset) at which to periodically prune the dictionary. " + "wordsToKeep prunes after creating a full dictionary. You may not have enough "
				+ "memory for this approach.";
	}

	/**
	 * Gets whether if the word frequencies should be transformed into
	 * log(1+fij) where fij is the frequency of word i in document(instance) j.
	 * 
	 * @return true if word frequencies are to be transformed.
	 */
	public boolean getTFTransform() {
		return this.m_TFTransform;
	}

	/**
	 * Sets whether if the word frequencies should be transformed into
	 * log(1+fij) where fij is the frequency of word i in document(instance) j.
	 * 
	 * @param TFTransform
	 *            true if word frequencies are to be transformed.
	 */
	public void setTFTransform(boolean TFTransform) {
		this.m_TFTransform = TFTransform;
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String TFTransformTipText() {
		return "Sets whether if the word frequencies should be transformed into:\n " + "   log(1+fij) \n" + "       where fij is the frequency of word i in document (instance) j.";
	}

	/**
	 * Sets whether if the word frequencies in a document should be transformed
	 * into: <br>
	 * fij*log(num of Docs/num of Docs with word i) <br>
	 * where fij is the frequency of word i in document(instance) j.
	 * 
	 * @return true if the word frequencies are to be transformed.
	 */
	public boolean getIDFTransform() {
		return this.m_IDFTransform;
	}

	/**
	 * Sets whether if the word frequencies in a document should be transformed
	 * into: <br>
	 * fij*log(num of Docs/num of Docs with word i) <br>
	 * where fij is the frequency of word i in document(instance) j.
	 * 
	 * @param IDFTransform
	 *            true if the word frequecies are to be transformed
	 */
	public void setIDFTransform(boolean IDFTransform) {
		this.m_IDFTransform = IDFTransform;
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String IDFTransformTipText() {
		return "Sets whether if the word frequencies in a document should be " + "transformed into: \n" + "   fij*log(num of Docs/num of Docs with word i) \n"
				+ "      where fij is the frequency of word i in document (instance) j.";
	}

	/**
	 * Gets whether if the word frequencies for a document (instance) should be
	 * normalized or not.
	 * 
	 * @return true if word frequencies are to be normalized.
	 */
	public SelectedTag getNormalizeDocLength() {

		return new SelectedTag(m_filterType, TAGS_FILTER);
	}

	/**
	 * Sets whether if the word frequencies for a document (instance) should be
	 * normalized or not.
	 * 
	 * @param newType
	 *            the new type.
	 */
	public void setNormalizeDocLength(SelectedTag newType) {

		if (newType.getTags() == TAGS_FILTER) {
			m_filterType = newType.getSelectedTag().getID();
		}
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String normalizeDocLengthTipText() {
		return "Sets whether if the word frequencies for a document (instance) " + "should be normalized or not.";
	}

	/**
	 * Gets whether if the tokens are to be downcased or not.
	 * 
	 * @return true if the tokens are to be downcased.
	 */
	public boolean getLowerCaseTokens() {
		return this.m_lowerCaseTokens;
	}

	/**
	 * Sets whether if the tokens are to be downcased or not. (Doesn't affect
	 * non-alphabetic characters in tokens).
	 * 
	 * @param downCaseTokens
	 *            should be true if only lower case tokens are to be formed.
	 */
	public void setLowerCaseTokens(boolean downCaseTokens) {
		this.m_lowerCaseTokens = downCaseTokens;
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String doNotOperateOnPerClassBasisTipText() {
		return "If this is set, the maximum number of words and the " + "minimum term frequency is not enforced on a per-class " + "basis but based on the documents in all the classes "
				+ "(even if a class attribute is set).";
	}

	/**
	 * Get the DoNotOperateOnPerClassBasis value.
	 * 
	 * @return the DoNotOperateOnPerClassBasis value.
	 */
	public boolean getDoNotOperateOnPerClassBasis() {
		return m_doNotOperateOnPerClassBasis;
	}

	/**
	 * Set the DoNotOperateOnPerClassBasis value.
	 * 
	 * @param newDoNotOperateOnPerClassBasis
	 *            The new DoNotOperateOnPerClassBasis value.
	 */
	public void setDoNotOperateOnPerClassBasis(boolean newDoNotOperateOnPerClassBasis) {
		this.m_doNotOperateOnPerClassBasis = newDoNotOperateOnPerClassBasis;
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String minTermFreqTipText() {
		return "Sets the minimum term frequency. This is enforced " + "on a per-class basis.";
	}

	/**
	 * Get the MinTermFreq value.
	 * 
	 * @return the MinTermFreq value.
	 */
	public int getMinTermFreq() {
		return m_minTermFreq;
	}

	/**
	 * Set the MinTermFreq value.
	 * 
	 * @param newMinTermFreq
	 *            The new MinTermFreq value.
	 */
	public void setMinTermFreq(int newMinTermFreq) {
		this.m_minTermFreq = newMinTermFreq;
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String lowerCaseTokensTipText() {
		return "If set then all the word tokens are converted to lower case " + "before being added to the dictionary.";
	}

	/**
	 * the stemming algorithm to use, null means no stemming at all (i.e., the
	 * NullStemmer is used).
	 * 
	 * @param value
	 *            the configured stemming algorithm, or null
	 * @see NullStemmer
	 */
	public void setStemmer(Stemmer value) {
		if (value != null) {
			m_Stemmer = value;
		} else {
			m_Stemmer = new NullStemmer();
		}
	}

	/**
	 * Returns the current stemming algorithm, null if none is used.
	 * 
	 * @return the current stemming algorithm, null if none set
	 */
	public Stemmer getStemmer() {
		return m_Stemmer;
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String stemmerTipText() {
		return "The stemming algorithm to use on the words.";
	}

	/**
	 * Sets the stopwords handler to use.
	 * 
	 * @param value
	 *            the stopwords handler, if null, Null is used
	 */
	public void setStopwordsHandler(StopwordsHandler value) {
		if (value != null) {
			m_StopwordsHandler = value;
		} else {
			m_StopwordsHandler = new Null();
		}
	}

	/**
	 * Gets the stopwords handler.
	 * 
	 * @return the stopwords handler
	 */
	public StopwordsHandler getStopwordsHandler() {
		return m_StopwordsHandler;
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String stopwordsHandlerTipText() {
		return "The stopwords handler to use (Null means no stopwords are used).";
	}

	/**
	 * the tokenizer algorithm to use.
	 * 
	 * @param value
	 *            the configured tokenizing algorithm
	 */
	public void setTokenizer(Tokenizer value) {
		m_Tokenizer = value;
	}

	/**
	 * Returns the current tokenizer algorithm.
	 * 
	 * @return the current tokenizer algorithm
	 */
	public Tokenizer getTokenizer() {
		return m_Tokenizer;
	}

	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String tokenizerTipText() {
		return "The tokenizing algorithm to use on the strings.";
	}

	/**
	 * sorts an array.
	 * 
	 * @param array
	 *            the array to sort
	 */
	private static void sortArray(int[] array) {

		int i, j, h, N = array.length - 1;

		for (h = 1; h <= N / 9; h = 3 * h + 1) {
			;
		}

		for (; h > 0; h /= 3) {
			for (i = h + 1; i <= N; i++) {
				int v = array[i];
				j = i;
				while (j > h && array[j - h] > v) {
					array[j] = array[j - h];
					j -= h;
				}
				array[j] = v;
			}
		}
	}

	/**
	 * determines the selected range.
	 */
	private void determineSelectedRange() {

		Instances inputFormat = getInputFormat();

		// Calculate the default set of fields to convert
		if (m_SelectedRange == null) {
			StringBuffer fields = new StringBuffer();
			for (int j = 0; j < inputFormat.numAttributes(); j++) {
				if (inputFormat.attribute(j).type() == Attribute.STRING) {
					fields.append((j + 1) + ",");
				}
			}
			m_SelectedRange = new Range(fields.toString());
		}
		m_SelectedRange.setUpper(inputFormat.numAttributes() - 1);

		// Prevent the user from converting non-string fields
		StringBuffer fields = new StringBuffer();
		for (int j = 0; j < inputFormat.numAttributes(); j++) {
			if (m_SelectedRange.isInRange(j) && inputFormat.attribute(j).type() == Attribute.STRING) {
				fields.append((j + 1) + ",");
			}
		}
		m_SelectedRange.setRanges(fields.toString());
		m_SelectedRange.setUpper(inputFormat.numAttributes() - 1);

		// System.err.println("Selected Range: " +
		// getSelectedRange().getRanges());
	}

	/**
	 * determines the dictionary.
	 */
	private void determineDictionary() {

		// Operate on a per-class basis if class attribute is set
		int classInd = getInputFormat().classIndex();
		int values = 1;
		if (!m_doNotOperateOnPerClassBasis && (classInd != -1)) {
			values = getInputFormat().attribute(classInd).numValues();
		}

		// TreeMap dictionaryArr [] = new TreeMap[values];
		@SuppressWarnings("unchecked")
		TreeMap<String, Count>[] dictionaryArr = new TreeMap[values];
		for (int i = 0; i < values; i++) {
			dictionaryArr[i] = new TreeMap<String, Count>();
		}

		// Make sure we know which fields to convert
		determineSelectedRange();

		// Tokenize all training text into an orderedMap of "words".
		long pruneRate = Math.round((m_PeriodicPruningRate / 100.0) * getInputFormat().numInstances());
		for (int i = 0; i < getInputFormat().numInstances(); i++) {
			Instance instance = getInputFormat().instance(i);
			int vInd = 0;
			if (!m_doNotOperateOnPerClassBasis && (classInd != -1)) {
				vInd = (int) instance.classValue();
			}

			// Iterate through all relevant string attributes of the current
			// instance
			Hashtable<String, Integer> h = new Hashtable<String, Integer>();
			for (int j = 0; j < instance.numAttributes(); j++) {
				if (m_SelectedRange.isInRange(j) && (instance.isMissing(j) == false)) {

					// Get tokenizer
					m_Tokenizer.tokenize(instance.stringValue(j));

					// Iterate through tokens, perform stemming, and remove
					// stopwords
					// (if required)
					while (m_Tokenizer.hasMoreElements()) {
						String word = m_Tokenizer.nextElement().intern();

						if (this.m_lowerCaseTokens == true) {
							word = word.toLowerCase();
						}

						word = m_Stemmer.stem(word);

						if (m_StopwordsHandler.isStopword(word)) {
							continue;
						}

						if (!(h.containsKey(word))) {
							h.put(word, new Integer(0));
						}

						Count count = dictionaryArr[vInd].get(word);
						if (count == null) {
							dictionaryArr[vInd].put(word, new Count(1));
						} else {
							count.count++;
						}
					}
				}
			}

			// updating the docCount for the words that have occurred in this
			// instance(document).
			Enumeration<String> e = h.keys();
			while (e.hasMoreElements()) {
				String word = e.nextElement();
				Count c = dictionaryArr[vInd].get(word);
				if (c != null) {
					c.docCount++;
				} else {
					System.err.println("Warning: A word should definitely be in the " + "dictionary.Please check the code");
				}
			}

			if (pruneRate > 0) {
				if (i % pruneRate == 0 && i > 0) {
					for (int z = 0; z < values; z++) {
						ArrayList<String> d = new ArrayList<String>(1000);
						Iterator<String> it = dictionaryArr[z].keySet().iterator();
						while (it.hasNext()) {
							String word = it.next();
							Count count = dictionaryArr[z].get(word);
							if (count.count <= 1) {
								d.add(word);
							}
						}
						Iterator<String> iter = d.iterator();
						while (iter.hasNext()) {
							String word = iter.next();
							dictionaryArr[z].remove(word);
						}
					}
				}
			}
		}

		// Figure out the minimum required word frequency
		int totalsize = 0;
		int prune[] = new int[values];
		for (int z = 0; z < values; z++) {
			totalsize += dictionaryArr[z].size();

			int array[] = new int[dictionaryArr[z].size()];
			int pos = 0;
			Iterator<String> it = dictionaryArr[z].keySet().iterator();
			while (it.hasNext()) {
				String word = it.next();
				Count count = dictionaryArr[z].get(word);
				array[pos] = count.count;
				pos++;
			}

			// sort the array
			sortArray(array);
			if (array.length < m_WordsToKeep) {
				// if there aren't enough words, set the threshold to
				// minFreq
				prune[z] = m_minTermFreq;
			} else {
				// otherwise set it to be at least minFreq
				prune[z] = Math.max(m_minTermFreq, array[array.length - m_WordsToKeep]);
			}
		}

		// Convert the dictionary into an attribute index
		// and create one attribute per word
		ArrayList<Attribute> attributes = new ArrayList<Attribute>(totalsize + getInputFormat().numAttributes());

		// Add the non-converted attributes
		int classIndex = -1;
		for (int i = 0; i < getInputFormat().numAttributes(); i++) {
			if (!m_SelectedRange.isInRange(i)) {
				if (getInputFormat().classIndex() == i) {
					classIndex = attributes.size();
				}
				attributes.add((Attribute) getInputFormat().attribute(i).copy());
			}
		}

		// Add the word vector attributes (eliminating duplicates
		// that occur in multiple classes)
		TreeMap<String, Integer> newDictionary = new TreeMap<String, Integer>();
		int index = attributes.size();
		for (int z = 0; z < values; z++) {
			Iterator<String> it = dictionaryArr[z].keySet().iterator();
			while (it.hasNext()) {
				String word = it.next();
				Count count = dictionaryArr[z].get(word);
				if (count.count >= prune[z]) {
					if (newDictionary.get(word) == null) {
						newDictionary.put(word, new Integer(index++));
						attributes.add(new Attribute(m_Prefix + word));
					}
				}
			}
		}

		// Compute document frequencies
		m_DocsCounts = new int[attributes.size()];
		Iterator<String> it = newDictionary.keySet().iterator();
		while (it.hasNext()) {
			String word = it.next();
			int idx = newDictionary.get(word).intValue();
			int docsCount = 0;
			for (int j = 0; j < values; j++) {
				Count c = dictionaryArr[j].get(word);
				if (c != null) {
					docsCount += c.docCount;
				}
			}
			m_DocsCounts[idx] = docsCount;
		}

		// Trim vector and set instance variables
		attributes.trimToSize();
		m_Dictionary = newDictionary;
		m_NumInstances = getInputFormat().numInstances();

		// Set the filter's output format
		Instances outputFormat = new Instances(getInputFormat().relationName(), attributes, 0);
		outputFormat.setClassIndex(classIndex);
		setOutputFormat(outputFormat);
	}

	/**
	 * Converts the instance w/o normalization.
	 * 
	 * @oaram instance the instance to convert
	 * @param v
	 * @return the conerted instance
	 */
	private int convertInstancewoDocNorm(Instance instance, ArrayList<Instance> v) {

		// Convert the instance into a sorted set of indexes
		TreeMap<Integer, Double> contained = new TreeMap<Integer, Double>();

		// Copy all non-converted attributes from input to output
		int firstCopy = 0;
		for (int i = 0; i < getInputFormat().numAttributes(); i++) {
			if (!m_SelectedRange.isInRange(i)) {
				if (getInputFormat().attribute(i).type() != Attribute.STRING && getInputFormat().attribute(i).type() != Attribute.RELATIONAL) {
					// Add simple nominal and numeric attributes directly
					if (instance.value(i) != 0.0) {
						contained.put(new Integer(firstCopy), new Double(instance.value(i)));
					}
				} else {
					if (instance.isMissing(i)) {
						contained.put(new Integer(firstCopy), new Double(Utils.missingValue()));
					} else if (getInputFormat().attribute(i).type() == Attribute.STRING) {

						// If this is a string attribute, we have to first add
						// this value to the range of possible values, then add
						// its new internal index.
						if (outputFormatPeek().attribute(firstCopy).numValues() == 0) {
							// Note that the first string value in a
							// SparseInstance doesn't get printed.
							outputFormatPeek().attribute(firstCopy).addStringValue("Hack to defeat SparseInstance bug");
						}
						int newIndex = outputFormatPeek().attribute(firstCopy).addStringValue(instance.stringValue(i));
						contained.put(new Integer(firstCopy), new Double(newIndex));
					} else {
						// relational
						if (outputFormatPeek().attribute(firstCopy).numValues() == 0) {
							Instances relationalHeader = outputFormatPeek().attribute(firstCopy).relation();

							// hack to defeat sparse instances bug
							outputFormatPeek().attribute(firstCopy).addRelation(relationalHeader);
						}
						int newIndex = outputFormatPeek().attribute(firstCopy).addRelation(instance.relationalValue(i));
						contained.put(new Integer(firstCopy), new Double(newIndex));
					}
				}
				firstCopy++;
			}
		}

		double max = -1;
		for (int j = 0; j < instance.numAttributes(); j++) {
			// if ((getInputFormat().attribute(j).type() == Attribute.STRING)
			if (m_SelectedRange.isInRange(j) && (instance.isMissing(j) == false)) {

				m_Tokenizer.tokenize(instance.stringValue(j));

				while (m_Tokenizer.hasMoreElements()) {
					String word = m_Tokenizer.nextElement();
					if (this.m_lowerCaseTokens == true) {
						word = word.toLowerCase();
					}
					word = m_Stemmer.stem(word);
					Integer index = m_Dictionary.get(word);
					if (index != null) {
						if (m_OutputCounts) { // Separate if here rather than
												// two lines down
												// to avoid hashtable lookup
							Double count = contained.get(index);

							if (count != null) {
								max = Math.max(count, max);

								contained.put(index, new Double(count.doubleValue() + 1.0));
							} else {
								max = Math.max(1, max);

								contained.put(index, new Double(1));
							}
						} else {
							contained.put(index, new Double(1));
						}
					}
				}
			}
		}

		// Doing TFTransform
		if (m_TFTransform == true) {
			Iterator<Integer> it = contained.keySet().iterator();
			for (; it.hasNext();) {
				Integer index = it.next();
				if (index.intValue() >= firstCopy) {
					double val = contained.get(index).doubleValue();
					// val = Math.log(val + 1);
					val = Math.log(0.5 + (0.5*val / max));
					contained.put(index, new Double(val));
				}
			}
		}

		// Doing IDFTransform
		if (m_IDFTransform == true) {
			Iterator<Integer> it = contained.keySet().iterator();
			for (; it.hasNext();) {
				Integer index = it.next();
				if (index.intValue() >= firstCopy) {
					double val = contained.get(index).doubleValue();
					val = val * Math.log(m_NumInstances / (double) m_DocsCounts[index.intValue()]);
					contained.put(index, new Double(val));
				}
			}
		}

		// Convert the set to structures needed to create a sparse instance.
		double[] values = new double[contained.size()];
		int[] indices = new int[contained.size()];
		Iterator<Integer> it = contained.keySet().iterator();
		for (int i = 0; it.hasNext(); i++) {
			Integer index = it.next();
			Double value = contained.get(index);
			values[i] = value.doubleValue();
			indices[i] = index.intValue();
		}

		Instance inst = new SparseInstance(instance.weight(), values, indices, outputFormatPeek().numAttributes());
		inst.setDataset(outputFormatPeek());

		v.add(inst);

		return firstCopy;
	}

	/**
	 * Normalizes given instance to average doc length (only the newly
	 * constructed attributes).
	 * 
	 * @param inst
	 *            the instance to normalize
	 * @param firstCopy
	 * @throws Exception
	 *             if avg. doc length not set
	 */
	private void normalizeInstance(Instance inst, int firstCopy) throws Exception {

		double docLength = 0;

		if (m_AvgDocLength < 0) {
			throw new Exception("Average document length not set.");
		}

		// Compute length of document vector
		for (int j = 0; j < inst.numValues(); j++) {
			if (inst.index(j) >= firstCopy) {
				docLength += inst.valueSparse(j) * inst.valueSparse(j);
			}
		}
		docLength = Math.sqrt(docLength);

		// Normalize document vector
		for (int j = 0; j < inst.numValues(); j++) {
			if (inst.index(j) >= firstCopy) {
				double val = inst.valueSparse(j) * m_AvgDocLength / docLength;
				inst.setValueSparse(j, val);
				if (val == 0) {
					System.err.println("setting value " + inst.index(j) + " to zero.");
					j--;
				}
			}
		}
	}

	/**
	 * Returns the revision string.
	 * 
	 * @return the revision
	 */
	@Override
	public String getRevision() {
		return RevisionUtils.extract("$Revision: 10984 $");
	}

	/**
	 * Main method for testing this class.
	 * 
	 * @param argv
	 *            should contain arguments to the filter: use -h for help
	 */
	public static void main(String[] argv) {
		runFilter(new MyStringToWordVector(), argv);
	}
}
