/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.daos;

import java.io.File;

import com.job.ic.utils.FileUtils;

import org.apache.log4j.Logger;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

public class UrlDb {

	private static Logger logger = Logger.getLogger(UrlDb.class);
	
	/**
	 * Berkeley Db's Environment instances
	 */
	private static Environment env;
	/**
	 * Berkeley Db's EntityStore instances, store the features of the downloaded
	 * websites
	 */
	private static EntityStore dbStore;

	private static UrlDAO urlDao = null;
	
	
	public static void createEnvironment(String dbPath) {
		if(!FileUtils.exists(dbPath))
			FileUtils.mkdir(dbPath);
		
		if (env == null && dbStore == null) {
			EnvironmentConfig config = new EnvironmentConfig();
			StoreConfig sc = new StoreConfig();
			sc.setTransactional(true);
			sc.setAllowCreate(true);
			config.setAllowCreate(true);
			config.setTransactional(true);

			env = new Environment(new File(dbPath), config);
			dbStore = new EntityStore(env, "url", sc);
		}
	}


	public static UrlDAO getUrlDAO(){
		if(urlDao == null)
			urlDao = new UrlDAO(dbStore);
		
		return urlDao;
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
			logger.warn("SYNC Database Complete");
			dbStore.close();
			env.close();
			urlDao = null;
			env = null;
			dbStore = null;
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
			logger.error("Error closing MyDbEnv: " + dbe.toString());
			System.exit(-1);
		}
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
