package com.job.ic.crawlers.models;

import java.util.ArrayList;

import com.job.ic.crawlers.HttpSegmentCrawler;
import com.job.ic.ml.classifiers.ClassifierOutput;
import com.job.ic.ml.classifiers.ResultClass;
import com.job.ic.ml.classifiers.WekaClassifier;

public class DirectoryTreeNode {
	private DirectoryTreeNode parentNode;
	private DirectoryTree directoryTree;
	private ArrayList<DirectoryTreeNode> childNodes;
	private int relSegs;
	private int totalSegs;
	private int relPages;
	private int nonPages;
	private String pathName;
	
//	private ClassifierOutput dirPrediction;
//	private ArrayList<ClassifierOutput> dirPredictions;
	
	private ArrayList<Integer> historyRelPages;
	private ArrayList<Integer> historyNonPages;
	private ArrayList<Integer> historyRelSegs;
	private ArrayList<Integer> historyNonSegs;
	
	public DirectoryTreeNode(String pathName, DirectoryTreeNode parentNode, DirectoryTree tree){
		this.pathName = pathName;
		this.parentNode = parentNode;
		this.childNodes = new ArrayList<>();
		this.relPages = 0;
		this.nonPages = 0;
		this.relSegs = 0;
		this.totalSegs = 0;
//		this.dirPrediction = null;
		this.historyRelPages = new ArrayList<>();
		this.historyNonPages = new ArrayList<>();
		

		this.historyRelSegs = new ArrayList<>();
		this.historyNonSegs = new ArrayList<>();
		
		this.setDirectoryTree(tree);
	}
	
	public ArrayList<DirectoryTreeNode> getSiblings(){
		if(this.parentNode != null)
			return this.parentNode.getSiblings();
		
		return null;
	}

	public DirectoryTreeNode getParentNode() {
		return parentNode;
	}

	public void setParentNode(DirectoryTreeNode parentNode) {
		this.parentNode = parentNode;
	}

	public ArrayList<DirectoryTreeNode> getChildNodes() {
		return childNodes;
	}
	
	public void addChildNode(DirectoryTreeNode ch){
		this.childNodes.add(ch);
	}

	public void addSegment(int rel, int non) {
		double degree = HttpSegmentCrawler.calcRelevanceDegree(rel, non);
		if(degree > CrawlerConfig.getConfig().getRelevanceDegreeThreshold())
			this.relSegs++;
		
		this.relPages += rel;
		this.nonPages += non;
		
		this.totalSegs++;
		
		historyRelPages.add(this.relPages);
		historyNonPages.add(this.nonPages);
		
		historyRelSegs.add(this.relSegs);
		historyNonSegs.add(this.totalSegs-this.relSegs);
	}
	
	public double getRelSegsRatio(){
		if(this.totalSegs ==0)
			return -1;
		
		double output = 0;
		if(this.totalSegs > 0)
			output = relSegs*1.0/totalSegs;
		
		if(Double.isNaN(output))
			return -1;
		
		return output;
	}
	
	public double getRelPagesRatio(){
		if(this.totalSegs == 0)
			return -1;
		
		double output = 1.0*this.relPages/(this.relPages + this.nonPages);
		if(Double.isNaN(output))
			return -1;
		
		return output;
	}

	public boolean isCrawled() {
		return this.totalSegs>0?true:false;
	}

	public String getPathName() {
		return pathName;
	}


	public void setPathName(String pathName) {
		this.pathName = pathName;
	}
	
	public void destroyNode(){
		this.childNodes.clear();
		this.childNodes = null;
		this.parentNode = null;
	}

	public int getRelPages() {
		return relPages;
	}

	public void setRelPages(int relPages) {
		this.relPages = relPages;
	}

	public int getNonPages() {
		return nonPages;
	}

	public void setNonPages(int nonPages) {
		this.nonPages = nonPages;
	}

	
	public DirectoryTree getDirectoryTree() {
		return directoryTree;
	}

	public void setDirectoryTree(DirectoryTree directoryTree) {
		this.directoryTree = directoryTree;
	}

//	public void addDirPrediction(ClassifierOutput input) {
//		if(input == null)
//			return;
//		this.dirPredictions.add(input);
//	}
	
//	public ClassifierOutput getDirPrediction(){
//		if(this.dirPredictions.size() == 0)
//			return null;
//		
//		System.err.println("get dir prediction");
//		return WekaClassifier.average(dirPredictions.toArray(new ClassifierOutput[dirPredictions.size()]));
//	}
	
	public ResultClass getResultClass(){
		if(this.relPages*1.0/(this.relPages + this.nonPages) >= 0.5)
			return ResultClass.RELEVANT;
		
		return ResultClass.IRRELEVANT;
	}

//	public ClassifierOutput getDirPrediction() {
//		return dirPrediction;
//	}
//
//	public void setDirPrediction(ClassifierOutput dirPrediction) {
//		this.dirPrediction = dirPrediction;
//	}

	public String getCumulativeNumbers(){
		return String.format("%s\t%s\t%s\t%s", this.historyRelPages, this.historyNonPages, this.historyRelSegs, this.historyNonSegs);
	}

	
}
