/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.ml.classifiers;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import org.apache.log4j.Logger;

import com.job.ic.utils.FileUtils;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.unsupervised.attribute.Discretize;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.Standardize;

public class WekaClassifier implements Serializable {

	private static final long serialVersionUID = 6526612804142573070L;
	private FilteredClassifier ml;
	private Instances orgTraining;
	private int[] removeAttributeIndexes;

	private boolean isDiscritize = false;
	private ArrayList<Filter> filters;
	private double accuracy10fold = -1.0;
	private String[] options;

	private String extra;
	private double weight;
	private int discretize = 5;

	// protected double accuracy = -1.0;

	private static Logger logger = Logger.getLogger(WekaClassifier.class);

	protected String packageName;

	public WekaClassifier(String packageName, String options, double weight) {
		this.packageName = packageName;

		try {
			if (options != null) {
				options = options.replace("-stoplist", "-stopwords-handler weka.core.stopwords.Rainbow");
				this.options = Utils.splitOptions(options);
			} else {
				this.options = null;
			}

		} catch (Exception e) {
			logger.error(e.getMessage() + "\t" + e.getCause());
			// e.printStackTrace();
		}

		this.filters = new ArrayList<>();
		this.weight = weight;
	}

	public WekaClassifier(String packageName, String options) {
		this.packageName = packageName;
		try {
			if (options != null) {
				options = options.replace("-stoplist", "-stopwords-handler weka.core.stopwords.Rainbow");
				this.options = Utils.splitOptions(options);
			} else {
				options = null;
			}
		} catch (Exception e) {
			logger.error(e.getMessage() + "\t" + e.getCause());
			logger.error(options);
			e.printStackTrace();
		}

		this.filters = new ArrayList<>();
		this.weight = 1.0;
	}

	public void train(String trainFilePath, int[] removeAttributeIndexes, boolean isDiscritize, Filter[] additional, boolean useFilter, boolean isKFold) {

		logger.info("Train file:\t" + trainFilePath);
		logger.info(this.packageName);
		if (options != null) {
			logger.info("options:\t" + Arrays.deepToString(options));
		}

		try {
			DataSource trainingSource = new DataSource(trainFilePath);

			this.orgTraining = trainingSource.getDataSet();
			logger.info("Training size:" + this.orgTraining.size());
			this.orgTraining.setClassIndex(this.orgTraining.numAttributes() - 1);

			// additional filter
			if (additional != null) {
				for (Filter f : additional) {
					f.setInputFormat(this.orgTraining);
					filters.add(f);
				}
			}

			// remove filter
			if (removeAttributeIndexes != null) {
				this.removeAttributeIndexes = removeAttributeIndexes;
				String rm = "";
				for (int i = 0; i < removeAttributeIndexes.length; i++) {
					if (i != removeAttributeIndexes.length - 1) {
						rm += (removeAttributeIndexes[i]) + ",";
					} else {
						rm += (removeAttributeIndexes[i]);
					}
				}

				try {
					Remove remove = new Remove();
					remove.setAttributeIndices(rm);
					remove.setInputFormat(this.orgTraining);

					filters.add(remove);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// discritize filter
			if (isDiscritize) {
				this.setDiscritize(true);

				Discretize disc = new Discretize();
				disc.setAttributeIndices("first-last");
				disc.setBins(this.discretize);
				disc.setInputFormat(this.orgTraining);
				filters.add(disc);
			}

			Standardize std = new Standardize();
			std.setInputFormat(this.orgTraining);
			filters.add(std);

			MultiFilter mf = new MultiFilter();
			Filter[] fil = new Filter[filters.size()];
			fil = filters.toArray(fil);
			mf.setFilters(fil);

			Class<?> cls = Class.forName(packageName);
			AbstractClassifier c = (AbstractClassifier) cls.newInstance();
			c.setOptions(options);
			this.ml = new FilteredClassifier();
			this.ml.setClassifier(c);

			mf.setInputFormat(trainingSource.getDataSet());
			
			if (useFilter)
				this.ml.setFilter(mf);
			
			// System.out.println(((NaiveBayesMultinomialText)ml.getClassifier()).getStopwords().toString());
			this.ml.buildClassifier(this.orgTraining);
			logger.info("finished building classifier");
			if (isKFold) {
				Evaluation kFoldEval = new Evaluation(this.orgTraining);
				kFoldEval.crossValidateModel(this.ml, this.orgTraining, 10, new Random(1));

				this.accuracy10fold = kFoldEval.correct() / (kFoldEval.correct() + kFoldEval.incorrect());
				logger.info("10-fold accuracy : " + accuracy10fold);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void updateClassifier(Instances instances) throws Exception {

		this.orgTraining.setClassIndex(this.orgTraining.numAttributes() - 1);

		// apply filter;

		Instances newData = null;

		for (Filter f : filters) {
			newData = Filter.useFilter(instances, f);
			instances = newData;
			newData = null;
		}

		for (Instance in : instances) {
			try {
				((UpdateableClassifier) this.ml.getClassifier()).updateClassifier(in);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(in.toString() + "\t" + e.getCause());
			}
		}

	}

	public double getAccuracyFromTestSet(String testFilePath) {

		try {
			DataSource testingSource = new DataSource(testFilePath);

			Instances testSet = testingSource.getDataSet();

			if (testSet.classIndex() == -1) {
				testSet.setClassIndex(this.orgTraining.numAttributes() - 1);
			}

			Evaluation evalTest = new Evaluation(this.orgTraining);
			evalTest.evaluateModel(this.ml, testSet);

			double accuracy = evalTest.correct() / (evalTest.correct() + evalTest.incorrect());
			logger.info("Test file:\t" + testFilePath);
			logger.info("\n" + evalTest.toMatrixString());
			// logger.info(evalTest.toClassDetailsString());
			logger.info("Accuracy\t" + accuracy);

			// double tp = evalTest.numTruePositives(0);
			// double fn = evalTest.numFalseNegatives(0);
			// double tn = evalTest.numTrueNegatives(0);
			// double fp = evalTest.numFalsePositives(0);
			//
			// double d = tp / (tp + fn);
			// d *= tn / (tn + fp);
			// d = Math.sqrt(d);
			// logger.info("G-mean\t" + d);
			// FileUtils.writeTextFile("d:/train-norm.arff",
			// training.toString(), false);
			// FileUtils.writeTextFile("d:/test-norm.arff", testing.toString(),
			// false);
			return accuracy;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1;
	}

	public double getGMeanFromTestSet(String testFilePath) {

		try {
			DataSource testingSource = new DataSource(testFilePath);

			Instances testSet = testingSource.getDataSet();

			if (testSet.classIndex() == -1) {
				testSet.setClassIndex(this.orgTraining.numAttributes() - 1);
			}

			Evaluation evalTest = new Evaluation(this.orgTraining);
			evalTest.evaluateModel(this.ml, testSet);
			// logger.info(evalTest.toMatrixString());
			logger.info("Test file:\t" + testFilePath);

			double tp = evalTest.numTruePositives(0);
			double fn = evalTest.numFalseNegatives(0);
			double tn = evalTest.numTrueNegatives(0);
			double fp = evalTest.numFalsePositives(0);

			double d = tp / (tp + fn);
			logger.info(String.format("TP %.0f\tFN %.0f\tTN %.0f\tFP %.0f", tp, fn, tn, fp));
			d *= tn / (tn + fp);
			d = Math.sqrt(d);
			// logger.info("G-mean\t" + d);
			// FileUtils.writeTextFile("d:/train-norm.arff",
			// training.toString(), false);
			// FileUtils.writeTextFile("d:/test-norm.arff", testing.toString(),
			// false);
			return d;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1;
	}

	public ClassifierOutput predict(String instanceName, String[] features) {
		try {
			HashSet<Integer> rm = new HashSet<Integer>();
			if (this.removeAttributeIndexes != null)
				for (int i : this.removeAttributeIndexes)
					rm.add(i);

			Instance test = (Instance) this.orgTraining.instance(0).copy();
			test.setClassValue(Utils.missingValue());

			for (int i = 0; i < features.length; i++) {

				if (!rm.contains(i + 1)) {
					if (features[i].trim().equals("?")) {
						test.setValue(orgTraining.attribute(i), Utils.missingValue());
					} else {
						if (!isNumeric(features[i])) {
							test.setValue(orgTraining.attribute(i), features[i].trim());
						} else {
							test.setValue(orgTraining.attribute(i), Double.parseDouble(features[i].trim()));
						}
					}
				} else {
					test.setValue(orgTraining.attribute(i), Utils.missingValue());
				}

			}

			Instances testing = new Instances(this.orgTraining, 1);
			testing.add(test);

			testing.setClassIndex(testing.numAttributes() - 1);
			double[] d = ml.distributionForInstance(testing.instance(0));

			// System.out.println( test.classAttribute().value(0) + "\t" +
			// d[0]);
			// System.out.println( test.classAttribute().value(1)+ "\t" + d[1]);
			// System.out.println(ml.classifyInstance(testing.instance(0)));

//			System.exit(0);
			if (test.classAttribute().value(0).toLowerCase().equals("thai") || test.classAttribute().value(0).toLowerCase().equals("rel") || test.classAttribute().value(0).toLowerCase().equals("true"))
				return new ClassifierOutput(d[0], d[1], this.weight);
			else
				return new ClassifierOutput(d[1], d[0], this.weight);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(instanceName + "\t" + Arrays.deepToString(features));
			e.printStackTrace();
		}

		return null;
	}

	/*
	 * public Instances convertToInstances(ArrayList<String> instances) {
	 * Instances tmp = new Instances(this.orgTraining, instances.size());
	 * 
	 * for (String ins : instances) { String[] features = ins.split(",");
	 * Instance test = (Instance) this.orgTraining.instance(0).copy(); //
	 * test.setClassValue(Utils.missingValue());
	 * 
	 * for (int i = 0; i < features.length; i++) { if (!isNumeric(features[i]))
	 * { test.setValue(orgTraining.attribute(i),
	 * features[i].toLowerCase().trim()); } else {
	 * test.setValue(orgTraining.attribute(i),
	 * Double.parseDouble(features[i].trim())); } }
	 * 
	 * tmp.add(test);
	 * 
	 * 
	 * // output.add(test); }
	 * 
	 * 
	 * // if (this.removeAttributeIndexes != null) { // String rm = ""; // for
	 * (int i = 0; i < removeAttributeIndexes.length; i++) { // if (i !=
	 * removeAttributeIndexes.length - 1) { // rm += (removeAttributeIndexes[i])
	 * + ","; // } else { // rm += (removeAttributeIndexes[i]); // } // } // //
	 * try { // Remove remove = new Remove(); // remove.setAttributeIndices(rm);
	 * // remove.setInputFormat(this.orgTraining); // // Instances output =
	 * Filter.useFilter(tmp, remove); // // return output; // // } catch
	 * (Exception e) { // e.printStackTrace(); // } // }
	 * 
	 * 
	 * return tmp; }
	 * 
	 */

	public static boolean isNumeric(String input) {
		try {
			Double.parseDouble(input);
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public double getAccuracy10fold() {
		return accuracy10fold;
	}

	public void setAccuracy10fold(double accuracy10fold) {
		this.accuracy10fold = accuracy10fold;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String[] getOptions() {
		return options;
	}

	public void setOptions(String[] options) {
		this.options = options;
	}

	public static ClassifierOutput min(ClassifierOutput[] cf) {
		double minNon = cf[0].getIrrelevantScore(), minThai = cf[0].getRelevantScore();

		for (int i = 0; i < cf.length; i++) {
			if (cf[i] == null)
				continue;

			minNon = Math.min(minNon, cf[i].getIrrelevantScore());
			minThai = Math.min(minThai, cf[i].getRelevantScore());
		}

		return new ClassifierOutput(minThai, minNon, 1.0);
	}

	public static ClassifierOutput max(ClassifierOutput[] cf) {
		double maxNon = cf[0].getIrrelevantScore(), maxThai = cf[0].getRelevantScore();

		for (int i = 0; i < cf.length; i++) {

			if (cf[i] == null)
				continue;

			maxNon = Math.max(maxNon, cf[i].getIrrelevantScore());
			maxThai = Math.max(maxThai, cf[i].getRelevantScore());
		}

		return new ClassifierOutput(maxThai, maxNon, 1.0);
	}
	
	
	public static ClassifierOutput average(ClassifierOutput... cf) {

		double totalWeight = 0;
		for (ClassifierOutput c : cf) {
			if (c == null)
				continue;

			if (c.getWeight() > 0)
				totalWeight += c.getWeight();
		}

		
		double thai = 0, non = 0;

		for (ClassifierOutput c : cf) {
			if (c == null)
				continue;

			if (c.getWeight() <= 0)
				continue;

			non += (c.getWeight() / totalWeight) * c.getIrrelevantScore();
			thai += (c.getWeight() / totalWeight) * c.getRelevantScore();
		}

		// thai /= cf.size();
		// non /= cf.size();
		return new ClassifierOutput(thai, non, 1.0);
	}

	public static ClassifierOutput product(ClassifierOutput... cf) {

		double thai = 1, non = 1;

		for (int i = 0; i < cf.length; i++) {
			if (cf[i] == null)
				continue;

			non *= cf[i].getIrrelevantScore();
			thai *= cf[i].getRelevantScore();
		}
		return new ClassifierOutput(thai, non, 1.0);
	}

	public static ClassifierOutput majority(ClassifierOutput... cf) {

		double thai = 0, non = 0;

		for (int i = 0; i < cf.length; i++) {
			if (cf[i] == null)
				continue;

			if (cf[i].getResultClass() == ResultClass.RELEVANT)
				thai++;
			else
				non++;
		}

		return new ClassifierOutput(thai / cf.length, non / cf.length, 1.0);

	}
	
	public static ClassifierOutput weightMajority(ClassifierOutput... cf) {

		double thai = 0, non = 0;
		
		for (int i = 0; i < cf.length; i++) {
			if (cf[i] == null)
				continue;

			
			if (cf[i].getResultClass() == ResultClass.RELEVANT)
				thai+= cf[i].getWeight();
			else
				non+=cf[i].getWeight();
		}

		double total = thai+non;
		thai = thai/total;
		non = non/total;
		
		return new ClassifierOutput(thai / cf.length, non / cf.length, 1.0);

	}

	public boolean isDiscritize() {
		return isDiscritize;
	}

	public void setDiscritize(boolean isDiscritize) {
		this.isDiscritize = isDiscritize;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public FilteredClassifier getClassifier() {
		return this.ml;
	}

	public Instances getDataset() {
		return this.orgTraining;
	}

	public int getDiscretize() {
		return discretize;
	}

	public void setDiscretize(int discretize) {
		this.discretize = discretize;
	}
}
