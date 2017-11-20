package com.job.ic.crawlers.models;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.HttpSegmentCrawler;
import com.job.ic.ml.classifiers.NeighborhoodPredictor;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;

public class DirectoryTree {
	
	private static Logger logger = Logger.getLogger(DirectoryTree.class);

	private DirectoryTreeNode rootNode;
	private String hostname;

	private int overallRelPages;
	private int overallNonPages;
	private int overallRelSegs;
	private int overallNonSegs;
	

	private ArrayList<Integer> cumulativeRelPages;
	private ArrayList<Integer> cumulativeNonPages;
	
	private ArrayList<Integer> cumulativeRelSegs;
	private ArrayList<Integer> cumulativeNonSegs;
	

	public static void main(String[] args) {
		DirectoryTree d = new DirectoryTree("www.gconsole.com");
		d.addCrawledNode("http://www.gconsole.com/thailand/bangkok/index.html", 5, 3);
		d.addCrawledNode("http://www.gconsole.com/thailand/bangkok/index.html", 10, 0);
		d.addCrawledNode("http://www.gconsole.com/thailand/bangkok/index.html", 10, 0);
		d.addCrawledNode("http://www.gconsole.com/thailand/bangkok/index.html", 10, 0);

		d.addCrawledNode("http://www.gconsole.com/thailand/phuket/index.html", 10, 0);
		d.addCrawledNode("http://www.gconsole.com/thailand/phuket/index2.html", 10, 0);

		// d.clear();

		System.out.println("---------------------------");
		DirectoryTreeNode tmp = d.findNode("http://www.gconsole.com/thailand/tmp", null);
		DirectoryTreeNode tmp2 = d.findNode("http://www.gconsole.com/thailand/bangkok/tmp", null);
		DirectoryTreeNode tmp3 = d.findNode("http://www.gconsole.com/thailand/phuket/tmp", null);

		System.out.println(tmp.getRelPages() + "\t" + tmp.getNonPages() + "\t" + DirectoryTree.getAvgRelPagesRatioChildNodes(tmp) + "\t" + DirectoryTree.getAvgRelSegsRatioChildNodes(tmp) + "\t"
				+ DirectoryTree.getAvgRelSegsRatioSiblingNodes(tmp) + "\t" + DirectoryTree.getAvgRelSegsRatioSiblingNodes(tmp));
		System.out.println(tmp2 == null);
		System.out.println(tmp3 == null);

	}

	public DirectoryTree(String hostname) {
		this.rootNode = new DirectoryTreeNode("/", null, this);
		this.hostname = hostname;
		
		this.cumulativeRelPages = new ArrayList<>();
		this.cumulativeNonPages = new ArrayList<>();
		this.cumulativeRelSegs = new ArrayList<>();
		this.cumulativeNonSegs = new ArrayList<>();
		
	}

	public DirectoryTreeNode addEmptyNode(String urlPath) {
		urlPath = cleanUrlPath(urlPath);
		String[] tmp = urlPath.split("/");

		String path = "";
		DirectoryTreeNode node = null;
		DirectoryTreeNode previous = this.rootNode;

		for (int i = 0; i < tmp.length; i++) {
			path += tmp[i] + "/";

			node = findNode(path, previous);

			if (node != null) {
				previous = node;
			} else {
				previous = addEmptyNodes(path, previous);
			}
		}

		return previous;
	}

	public DirectoryTreeNode addCrawledNode(String urlPath, int rel, int non) {

		this.overallRelPages += rel;
		this.overallNonPages += non;
		if (HttpSegmentCrawler.calcRelevanceDegree(rel, non) > CrawlerConfig.getConfig().getRelevanceDegreeThreshold()) {
			this.overallRelSegs++;
		} else {
			this.overallNonSegs++;
		}

		//history
		this.cumulativeRelPages.add(this.overallRelPages);
		this.cumulativeNonPages.add(this.overallNonPages);
		
		this.cumulativeRelSegs.add(this.overallRelSegs);
		this.cumulativeNonSegs.add(this.overallNonSegs);
		
		DirectoryTreeNode node = addEmptyNode(urlPath);
		// add segments
		node.addSegment(rel, non);


//		logger.info(urlPath + "\t" + overallRelPages + "\t" + overallNonPages);
		
		return node;

	}
	
	public String getCumulativeNumbers(){
		return String.format("%s\t%s\t%s\t%s", this.cumulativeRelPages, this.cumulativeNonPages, this.cumulativeRelSegs, this.cumulativeNonSegs);
	}

	private DirectoryTreeNode addEmptyNodes(String urlPath, DirectoryTreeNode parentNode) {
		urlPath = cleanUrlPath(urlPath);

		DirectoryTreeNode child = new DirectoryTreeNode(urlPath, parentNode, this);
		parentNode.addChildNode(child);

		return child;
	}

	public DirectoryTreeNode findNode(String urlPath, DirectoryTreeNode node) {
		urlPath = cleanUrlPath(urlPath);

		if (node == null) {
			node = this.rootNode;
		}

		if (StringUtils.countWordInStr("/", urlPath) > StringUtils.countWordInStr("/", node.getPathName())) {
			return null;
		}

		if (node.getPathName().equals(urlPath)) {
			return node;
		}

		if (node.getChildNodes() == null || node.getChildNodes().size() == 0) {
			return null;
		}

		for (DirectoryTreeNode ch : node.getChildNodes()) {
			DirectoryTreeNode t = findNode(urlPath, ch);
			if (t != null)
				return t;
		}

		return null;
	}

	public void clear() {
		clear(this.rootNode);
	}

	public void clear(DirectoryTreeNode node) {

		if (node.getChildNodes().size() == 0) {
			System.out.println("clear\t" + node.getPathName() + "\t");
			node.destroyNode();
			return;
		} else {
			for (DirectoryTreeNode ch : node.getChildNodes()) {
				clear(ch);
			}

			node.destroyNode();
		}
	}

	private String cleanUrlPath(String urlPath) {
		try {
			if (urlPath.contains("http://")) {
				String host = HttpUtils.getHost(urlPath);
				urlPath = urlPath.substring(urlPath.indexOf(host) + host.length());
			}

			if (urlPath.lastIndexOf("/") > 0)
				urlPath = urlPath.substring(0, urlPath.lastIndexOf("/") + 1);

			return urlPath;
		} catch (Exception e) {
			return null;
		}
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public static double getAvgRelSegsRatioSiblingNodes(DirectoryTreeNode node) {
		return getAvgRelSegsRatioChildNodes(node.getParentNode());
	}

	public static double getAvgRelPagesRatioSiblingNodes(DirectoryTreeNode node) {
		return getAvgRelPagesRatioChildNodes(node.getParentNode());
	}

	public static double getAvgRelSegsRatioChildNodes(DirectoryTreeNode node) {
		double c = 0;

		if (node == null || node.getChildNodes() == null || node.getChildNodes().size() == 0)
			return -1;

		int count = 0;
		for (DirectoryTreeNode n : node.getChildNodes()) {
			if (n.getRelSegsRatio() < 0)
				continue;

			c += n.getRelSegsRatio();
			count++;
		}

		if (Double.isNaN(c / count))
			return -1.0;

		return c / count;
	}

	public static double getAvgRelPagesRatioChildNodes(DirectoryTreeNode node) {

		if (node == null || node.getChildNodes() == null || node.getChildNodes().size() == 0)
			return -1;

		double c = 0;
		int count = 0;
		for (DirectoryTreeNode n : node.getChildNodes()) {
			if (n.getRelPagesRatio() < 0)
				continue;

			c += n.getRelPagesRatio();
			count++;
		}

		if (Double.isNaN(c / count))
			return -1.0;

		return c / count;
	}

	public int getOverallRelPages() {
		return this.overallRelPages;
	}

	public void setOverallRelPages(int overallRelPages) {
		this.overallRelPages = overallRelPages;
	}

	public int getOverallNonPages() {
		return this.overallNonPages;
	}

	public void setOverallNonPages(int overallNonPages) {
		this.overallNonPages = overallNonPages;
	}

	public int getOverallSegs() {
		return this.overallNonSegs + this.overallRelSegs;
	}

	public int getOverallRelSegs() {
		return this.overallRelSegs;
	}

	public void setOverallRelSegs(int overallRelSegs) {
		this.overallRelSegs = overallRelSegs;
	}

	public int getOverallNonSegs() {
		return this.overallNonSegs;
	}

	public void setOverallNonSegs(int overallNonSegs) {
		this.overallNonSegs = overallNonSegs;
	}

	public double getOverallRelSegsRatio() {
		double cal = 1.0 * this.overallRelSegs / (this.overallRelSegs + this.overallNonSegs);

		if (Double.isNaN(cal))
			return -1;

		return cal;
	}



	public double getOverallRelPagesRatio() {
		double cal = 1.0 * this.overallRelPages / (this.overallRelPages + this.overallNonPages);

		if (Double.isNaN(cal))
			return -1;

		return cal;
	}

	public ArrayList<Integer> getCumulativeRelPages() {
		return cumulativeRelPages;
	}

	public void setCumulativeRelPages(ArrayList<Integer> cumulativeRelPages) {
		this.cumulativeRelPages = cumulativeRelPages;
	}

	public ArrayList<Integer> getCumulativeNonPages() {
		return cumulativeNonPages;
	}

	public void setCumulativeNonPages(ArrayList<Integer> cumulativeNonPages) {
		this.cumulativeNonPages = cumulativeNonPages;
	}

	public ArrayList<Integer> getCumulativeRelSegs() {
		return cumulativeRelSegs;
	}

	public void setCumulativeRelSegs(ArrayList<Integer> cumulativeRelSegs) {
		this.cumulativeRelSegs = cumulativeRelSegs;
	}

	public ArrayList<Integer> getCumulativeNonSegs() {
		return cumulativeNonSegs;
	}

	public void setCumulativeNonSegs(ArrayList<Integer> cumulativeNonSegs) {
		this.cumulativeNonSegs = cumulativeNonSegs;
	}
}
