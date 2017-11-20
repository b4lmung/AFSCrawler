/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.daos;

import java.io.File;

import org.apache.log4j.Logger;

import com.job.ic.utils.FileUtils;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

public class WebsiteSegmentDb {

	private static Logger logger = Logger.getLogger(WebsiteSegmentDb.class);
	
	private static Environment env;

	private static EntityStore dbStore;

	private static WebsiteSegmentDAO segmentDao = null;
	
	
	public static void createEnvironment() {
		if(true)
		return;
		
		
		if (env == null && dbStore == null) {
			FileUtils.deleteDir("segDb");
			
			if(!FileUtils.exists("segDb"))
				FileUtils.mkdir("segDb");
			
			EnvironmentConfig config = new EnvironmentConfig();
			StoreConfig sc = new StoreConfig();
			sc.setTransactional(true);
			sc.setAllowCreate(true);
			config.setAllowCreate(true);
			config.setTransactional(true);
			config.setConfigParam(EnvironmentConfig.LOG_FILE_MAX, "100000000");
			env = new Environment(new File("segDb"), config);
			
			dbStore = new EntityStore(env, "segs", sc);
		}
	}


	public static WebsiteSegmentDAO getSegmentDAO(){
		if(dbStore == null){
			createEnvironment();
		}
		
		if(segmentDao == null){
			segmentDao = new WebsiteSegmentDAO(dbStore);
		}

		return segmentDao;
	}

	/**
	 * Use for close bdb's environment and entity store
	 */
	public static void close() {
		try {
			
			dbStore.sync();
			env.sync();
			env.flushLog(true);
			env.cleanLog();
//			logger.warn("SYNC Database Complete");
			dbStore.close();
			env.close();
			segmentDao = null;
			env = null;
			dbStore = null;
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
			logger.error("Error closing MyDbEnv: " + dbe.toString());
			System.exit(-1);
		}
		
		FileUtils.deleteDir("segDb");
	}

	/**
	 * use for get the bdb's EntityStore
	 */
//	public static EntityStore getStore() {
//		if (env != null && dbStore != null) {
//			return dbStore;
//		}
//		return null;
//	}
	
	public static Environment getEnvironment() {
		if (env != null) {
			return env;
		} else {
			return null;
		}
	}
	

}
