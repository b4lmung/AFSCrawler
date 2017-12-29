/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.utils;

import com.job.ic.crawlers.TrainingMultiHopSegmentCrawler;

public class Status {


	private static long SUCCESS;
	private static long TOTAL;
	private static long RELEVANT_PAGE;
	private static long CUM_REL_PAGE;
	
	private static long IRRELEVANT_PAGE;
	private static long lastDLTime;
	private static long TOTAL_PAGE;
	
	
	public static long getTotalPage(){
		return TOTAL_PAGE;
	}
	
	public static synchronized void addPage(boolean isRel) {
		if (isRel) {
			RELEVANT_PAGE++;
			CUM_REL_PAGE++;
		} else {
			IRRELEVANT_PAGE++;
		}
		TOTAL_PAGE++;
		
		
		setLastDLTime(System.currentTimeMillis());
	}

	public static synchronized String progressReport() {
		if (TOTAL == 0) {
			return "0.0";
		}
		return String.format("From [%d / %d] = %.2f\tHV:\t%s\tCumRelPage:%d", SUCCESS, TOTAL, SUCCESS * 100.0D / TOTAL, getHarvestRate(), getCumulativeRelevantPages());
	}

	public static synchronized String getHarvestRate() {
		if (RELEVANT_PAGE + IRRELEVANT_PAGE > 0) {
			return String.format("%d\t%.3f", RELEVANT_PAGE + IRRELEVANT_PAGE, RELEVANT_PAGE * 100.0 / (RELEVANT_PAGE + IRRELEVANT_PAGE));
		} else {
			return "";
		}
	}

	public static void clear() {
		
		SUCCESS = 0;
		TOTAL = 0;
		RELEVANT_PAGE = 0;
		IRRELEVANT_PAGE = 0;
		lastDLTime = 0;
		TOTAL_PAGE = 0;
	}

	public static synchronized long getLastDLTime() {
		return lastDLTime;
	}

	public static synchronized void setLastDLTime(long lastDLTime) {
		Status.lastDLTime = lastDLTime;
	}
	
	public static synchronized long getSUCCESS() {
		return SUCCESS;
	}

	public static void SUCCESS() {
		SUCCESS++;
	}

	public static synchronized long getTOTAL() {
		return TOTAL;
	}

	public static synchronized long getRelevantPages() {
		return RELEVANT_PAGE;
	}
	
	public static synchronized long getCumulativeRelevantPages() {
		return CUM_REL_PAGE;
	}
	
	
	public static void setTOTAL(long total) {
		TOTAL = total;
	}

}
