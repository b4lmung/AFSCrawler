/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.models;

import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Properties;

import org.apache.log4j.Logger;

public class CrawlerConfig implements Serializable {

	private static CrawlerConfig config;

	/**
	 * 
	 */
	private static final long serialVersionUID = -5074339577775449673L;
	private String downloadPath;
	private String dbPath;
	private String allowFileType;
	private long wait;
	private long timeout;
	private int maxPage;
	private int maxFileSize;
	private int maxDepth;
	private int distanceFromRelevantSeg;
	private int segmentThreshold;
	private boolean allowHttps;
	private boolean ignoreRobot;
	private boolean isOnlyStaticURL;
	private boolean isTrainingMode;
	private long soTimeout;
	private long connMgrTimeout;
	private String usrAgent;
	private String usrFrom;
	private boolean isGzip;
	private String checkerType;
	private int canonicalCount;
	private int windowSize;
	private int numThreads;
	private int updateInterval;
	private String targetLang;
	private String pageModel;
	private String proxyServer;
	private int proxyPort;
	private long limitDownloadedPages;
	private double relevanceDegreeThreshold;
	private String predictorTrainingPath;
	private String localProxyPath;
	private boolean useNeighborhoodPredictor;
	private boolean useHistoryPredictor;
	private boolean useDuplicateData;
	private boolean isPageMode;
	
	private static int count = 0;
	
	private static Logger logger = Logger.getLogger(CrawlerConfig.class);

//	private String supplyLog;
	
	private String relevantKeywordsPath;
	
	private String seedPath;
	private String linkClassifierAlgo;
	private String linkClassifierParams;
	private double[] classifierWeights;
	
	public static CrawlerConfig getConfig(){
		if(CrawlerConfig.config == null){
			try {
				loadConfig("crawler.conf");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return CrawlerConfig.config;
	}
	

	public static void loadConfig() {
		if(CrawlerConfig.config == null)
			try {
				loadConfig("crawler.conf");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	private static void loadConfig(String filepath) throws Exception {

		if(count++ == 1)
			throw new Exception("loaded multiple time");
		
		CrawlerConfig.config = new CrawlerConfig();
		Properties ps;
		try {
			FileInputStream fis = new FileInputStream(filepath);
			ps = new Properties();
			ps.load(fis);

			String max_depth = ps.getProperty("maxDepth");
			String file_type = ps.getProperty("fileType");
			String timeout = ps.getProperty("timeout");
			String wait = ps.getProperty("wait");
			String max_page = ps.getProperty("maxPagePerSite");
			String max_filesize = ps.getProperty("maxFileSize");
			String download_path = ps.getProperty("downloadPath");
			String db_path = ps.getProperty("dbPath");
			String segmentThreshold = ps.getProperty("segmentThreshold");
			String distance = ps.getProperty("distanceFromRelevantSeg");
			String ignoreRobot = ps.getProperty("ignoreRobot");
			String isOnlyStaticURL = ps.getProperty("isOnlyStaticURL");
			String soTimeout = ps.getProperty("soTimeout");
			String usrAgent = ps.getProperty("usrAgent");
			String usrFrom = ps.getProperty("usrFrom");
			String connMgrTimeout = ps.getProperty("connMgrTimeout");
			String checkerType = ps.getProperty("checkerType");
			String canonicalC = ps.getProperty("canonicalCount");
			String numThreads = ps.getProperty("threads");
			String targetLang = ps.getProperty("targetLang");
			String model = ps.getProperty("pageClassifierModel");
			String proxyServer = ps.getProperty("proxyServer");
			String port = ps.getProperty("proxyPort");
			String pages = ps.getProperty("limitDownloadedPages");
			String threshold  = ps.getProperty("relevanceDegree");
			String predictor = ps.getProperty("predictorTrainingPath");
			String localPath = ps.getProperty("localProxyPath");
			String relKeywordsPath = ps.getProperty("relevantKeywordsPath");
			
			String useNeighborhood = ps.getProperty("useNeighborhood");
			String useHistory = ps.getProperty("useHistory");
			
			String useDup = ps.getProperty("useDup");
			
			
			
			String seed = ps.getProperty("seedPath");
			String updateInterval = ps.getProperty("updateInterval");
			String https = ps.getProperty("allowHttps");
			String[] weights = ps.getProperty("weightClassifiers").split(",");
			String linkAlgo = ps.getProperty("linkClassifierAlgo");
			String linkParams = ps.getProperty("linkClassifierParams");
			
			config.setTrainingMode(Boolean.parseBoolean(ps.getProperty("isTrainingMode")));
			
			double[] w = new double[weights.length];
			for(int i=0; i<weights.length; i++){
				w[i] = Double.parseDouble(weights[i]);
			}
			
			config.setClassifierWeights(w);
			config.setLinkClassifierAlgo(linkAlgo);
			config.setLinkClassifierParams(linkParams);
			
			
			config.windowSize = (Integer.parseInt(ps.getProperty("windowSize")));
			config.setGzip(false);
			if (ps.getProperty("isGzip").equals("true"))
				config.setGzip(true);

			boolean ignore = false;
			if (ignoreRobot.equals("true")) {
				ignore = true;
			}
			boolean isStatic = false;
			if (isOnlyStaticURL.equals("true")) {
				isStatic = true;
			}

			long conn_timeout = Long.parseLong(connMgrTimeout);
			fis.close();
			

//			config.setSupplyLog(supplyLog);
			config.setRelevantKeywordsPath(relKeywordsPath);
			config.setMaxDepth(Integer.parseInt(max_depth));
			config.setSeedPath(seed);
			config.setLocalProxyPath(localPath);
			config.setLimitDownloadedPages(Long.parseLong(pages));
			config.setRelevanceDegreeThreshold(Double.parseDouble(threshold));
			config.setPredictorTrainingPath(predictor);
			config.setPageModel(model);
			config.setProxyServer(proxyServer);
			if(port.trim().length() > 0)
				config.setProxyPort(Integer.parseInt(port));
			
			config.setUseHistoryPredictor(Boolean.parseBoolean(useHistory));
			config.setUseNeighborhoodPredictor(Boolean.parseBoolean(useNeighborhood));
			
			config.setUseDuplicateData(Boolean.parseBoolean(useDup));
			config.setTargetLang(targetLang);
			
//			System.err.println(config.getTargetLang());
			
			config.setUsrAgent(usrAgent);
			config.setUsrFrom(usrFrom);
			config.setConnMgrTimeout(Long.parseLong(connMgrTimeout));
			config.setAllowFileType(file_type);
			config.setWaitTime(Long.parseLong(wait));
			config.setTimeout(Long.parseLong(timeout));
			
			if(config.getLocalProxyPath().equals(""))
				config.setMaxPagePerSite(Integer.parseInt(max_page));
			else
				config.setMaxPagePerSite(-1);
				
			config.setMaxFileSize(Integer.parseInt(max_filesize));
			config.setDownloadPath(download_path);
			config.setDbPath(db_path);
			config.setDistanceFromRelevantSeg(Integer.parseInt(distance));
			config.setIgnoreRobot(ignore);
			config.setOnlyStaticURL(isStatic);
			config.setSoTimeout(Long.parseLong(soTimeout));
			config.setConnMgrTimeout(conn_timeout);
			config.setCheckerType(checkerType);
			config.setCanonicalCount(Integer.parseInt(canonicalC));
			config.setSegmentThreshold(Integer.parseInt(segmentThreshold));
			config.setNumThreads(Integer.parseInt(numThreads));
			config.setUpdateInterval(Integer.parseInt(updateInterval));
			config.setAllowHttps(Boolean.parseBoolean(https));
			
			try{
				String pageMode = ps.getProperty("pageMode");
				config.setPageMode(Boolean.parseBoolean(pageMode));
			}catch(Exception e){
				config.setPageMode(false);
				System.err.println("segment mode");
			}
			
//			if(config.isPageMode){
//				config.setPredictorTrainingPath(config.getPredictorTrainingPath().replace(".arff", "-page.arff"));
//			}
			
			if(config.isTrainingMode){
				config.setLimitDownloadedPages(-1);
				config.setSegmentThreshold(-1);
				config.setDistanceFromRelevantSeg(-1);
				config.setPredictorTrainingPath(null);
//				config.setUpdateInterval(1000);
			}
			
			
			
//			logger.info("==============Overview Configuration=================");
//			logger.info("Use History:\t" + useHistory);
//			logger.info("Use Dup:\t" + useDup);
//			logger.info("segmentThreshold:\t" + segmentThreshold);
//			logger.info("distanceThreshold:\t" + distance);
//			logger.info("maxPagePerSite:\t" + pages);
//			logger.info("Predictor Training File:\t" + predictor);
//			logger.info("Page Classifier path:\t" + model);
//			
//			logger.info("local proxy path:\t" + localPath);
//			logger.info("===============================");
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public CrawlerConfig() {
	}

	public long getWaitTime() {
		return wait;
	}

	public void setWaitTime(long wait2) {
		this.wait = wait2;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout2) {
		this.timeout = timeout2;
	}

	public String getAllowFileType() {
		return allowFileType;
	}

	public void setAllowFileType(String fileType) {
		this.allowFileType = fileType;
	}


	public void setMaxPagePerSite(int max_page) {
		this.maxPage = max_page;
	}

	public int getMaxPagePerSite() {
		return this.maxPage;
	}

	public void setMaxFileSize(int max_filesize) {
		this.maxFileSize = max_filesize;
	}

	public int getMaxFileSize() {
		return maxFileSize;
	}

	public String getDownloadPath() {
		return this.downloadPath;
	}

	public void setDownloadPath(String downloadPath) {
		this.downloadPath = downloadPath;
	}

	public void setDbPath(String dbPath) {
		this.dbPath = dbPath;
	}

	public String getDbPath() {
		return dbPath;
	}

	public int getDistanceFromRelevantSeg() {
		return distanceFromRelevantSeg;
	}

	public void setDistanceFromRelevantSeg(int distance) {
		this.distanceFromRelevantSeg = distance;
	}

	public int getSegmentThreshold() {
		return segmentThreshold;
	}

	public void setSegmentThreshold(int serverThreshold) {
		this.segmentThreshold = serverThreshold;
	}

	public void setIgnoreRobot(boolean ignoreRobot) {
		this.ignoreRobot = ignoreRobot;
	}

	public boolean isIgnoreRobot() {
		return ignoreRobot;
	}

	public void setOnlyStaticURL(boolean isOnlyStaticURL) {
		this.isOnlyStaticURL = isOnlyStaticURL;
	}

	public boolean isOnlyStaticURL() {
		return isOnlyStaticURL;
	}

	public void setSoTimeout(long sotimeout) {
		this.soTimeout = sotimeout;
	}

	public long getSoTimeout() {
		return soTimeout;
	}

	public void setConnMgrTimeout(long connMgrTimeout) {
		this.connMgrTimeout = connMgrTimeout;
	}

	public long getConnMgrTimeout() {
		return connMgrTimeout;
	}

	public void setUsrAgent(String usrAgent) {
		this.usrAgent = usrAgent;
	}

	public String getUsrAgent() {
		return usrAgent;
	}

	public void setUsrFrom(String usrFrom) {
		this.usrFrom = usrFrom;
	}

	public String getUsrFrom() {
		return usrFrom;
	}

	public boolean isGzip() {
		return isGzip;
	}

	public void setGzip(boolean isGzip) {
		this.isGzip = isGzip;
	}

	public int getCanonicalCount() {
		return canonicalCount;
	}

	public void setCanonicalCount(int canonicalCount) {
		this.canonicalCount = canonicalCount;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	public String getCheckerType() {
		return checkerType;
	}

	public void setCheckerType(String checkerType) {
		this.checkerType = checkerType;
	}

	public int getNumThreads() {
		return numThreads;
	}

	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}

	public String getTargetLang() {
		return targetLang;
	}

	public void setTargetLang(String targetLang) {
		this.targetLang = targetLang;
	}

	public String getPageModel() {
		return pageModel;
	}

	public void setPageModel(String pageModel) {
		this.pageModel = pageModel;
	}

	public String getProxyServer() {
		return proxyServer;
	}

	public void setProxyServer(String proxyServer) {
		this.proxyServer = proxyServer;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public long getLimitDownloadedPages() {
		return limitDownloadedPages;
	}

	public void setLimitDownloadedPages(long limitDownloadPages) {
		this.limitDownloadedPages = limitDownloadPages;
	}


	public String getPredictorTrainingPath() {
		return predictorTrainingPath;
	}

	public void setPredictorTrainingPath(String predictorTrainingPath) {
		this.predictorTrainingPath = predictorTrainingPath;
	}

	public double getRelevanceDegreeThreshold() {
		return relevanceDegreeThreshold;
	}

	public void setRelevanceDegreeThreshold(double relevanceDegreeThreshold) {
		this.relevanceDegreeThreshold = relevanceDegreeThreshold;
	}

	public String getLocalProxyPath() {
		return localProxyPath;
	}

	public void setLocalProxyPath(String localProxyPath) {
		this.localProxyPath = localProxyPath;
	}

	public String getSeedPath() {
		return seedPath;
	}

	public void setSeedPath(String seedPath) {
		this.seedPath = seedPath;
	}

	public int getUpdateInterval() {
		return updateInterval;
	}

	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
	}


	public int getMaxDepth() {
		return maxDepth;
	}


	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}


	public boolean isAllowHttps() {
		return allowHttps;
	}


	public void setAllowHttps(boolean allowHttps) {
		this.allowHttps = allowHttps;
	}


	public String getLinkClassifierAlgo() {
		return linkClassifierAlgo;
	}


	public void setLinkClassifierAlgo(String linkClassifierAlgo) {
		this.linkClassifierAlgo = linkClassifierAlgo;
	}


	public String getLinkClassifierParams() {
		return linkClassifierParams;
	}


	public void setLinkClassifierParams(String linkClassifierParams) {
		this.linkClassifierParams = linkClassifierParams;
	}


	public double[] getClassifierWeights() {
		return classifierWeights;
	}


	public void setClassifierWeights(double[] classifierWeights) {
		this.classifierWeights = classifierWeights;
	}


	public boolean isTrainingMode() {
		return isTrainingMode;
	}


	public void setTrainingMode(boolean isTrainingMode) {
		this.isTrainingMode = isTrainingMode;
	}


	public String getRelevantKeywordsPath() {
		return relevantKeywordsPath;
	}


	public void setRelevantKeywordsPath(String relevantKeywordsPath) {
		this.relevantKeywordsPath = relevantKeywordsPath;
	}


	public boolean useNeighborhoodPredictor() {
		return useNeighborhoodPredictor;
	}
	
	public boolean useHistoryPredictor() {
		return useHistoryPredictor;
	}


	public void setUseNeighborhoodPredictor(boolean useHistoryPredictor) {
		this.useNeighborhoodPredictor = useHistoryPredictor;
	}

	public void setUseHistoryPredictor(boolean useHistoryPredictor) {
		this.useHistoryPredictor = useHistoryPredictor;
	}

	public boolean useDuplicateData() {
		return useDuplicateData;
	}


	public void setUseDuplicateData(boolean useDuplicateData) {
		this.useDuplicateData = useDuplicateData;
	}


	public boolean isPageMode() {
		return isPageMode;
	}


	public void setPageMode(boolean isPageMode) {
		this.isPageMode = isPageMode;
	}


//	public String getSupplyLog() {
//		return supplyLog;
//	}
//
//
//	public void setSupplyLog(String supplyLog) {
//		this.supplyLog = supplyLog;
//	}


}
