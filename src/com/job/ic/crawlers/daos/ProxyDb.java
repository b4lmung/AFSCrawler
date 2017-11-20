/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.daos;

import java.io.File;

import org.apache.log4j.Logger;

import com.sleepycat.je.CacheMode;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

public class ProxyDb {

	private static Logger logger = Logger.getLogger(ProxyDb.class);
	
	/**
	 * Berkeley Db's Environment instances
	 */
	private static Environment env;
	/**
	 * Berkeley Db's EntityStore instances, store the features of the downloaded
	 * websites
	 */
	private static EntityStore dbStore;

	public static void createEnvironment(String dbPath){
		createEnvironment(dbPath, false);
	}
	
	
	public static void createEnvironment(String dbPath, boolean transaction) {
		if (env == null && dbStore == null) {
			EnvironmentConfig config = new EnvironmentConfig();
			StoreConfig sc = new StoreConfig();
			sc.setTransactional(transaction);
			sc.setAllowCreate(true);
			config.setAllowCreate(true);
			config.setTransactional(transaction);
			config.setCacheMode(CacheMode.EVICT_LN);
		    config.setConfigParam(EnvironmentConfig.LOG_FILE_MAX, "100000000");
			env = new Environment(new File(dbPath), config);
			dbStore = new EntityStore(env, "proxy", sc);
//			env.cleanLog();
		}
	}



	/**
	 * Use for close bdb's environment and entity store
	 */
	public static void close() {
		try {
			
			dbStore.sync();
			env.sync();
			
			env.flushLog(true);
			
			logger.warn("SYNC Database Complete");
		
			dbStore.close();
			env.close();
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
	public static EntityStore getStore() {
		if (env != null && dbStore != null) {
			return dbStore;
		}
		return null;
	}

	public static Environment getEnvironment() {
		if (env != null) {
			return env;
		} else {
			return null;
		}
	}
	
}
