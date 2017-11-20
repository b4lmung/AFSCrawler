/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.daos;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.ResultModel;
import com.job.ic.crawlers.models.SegmentGraphModel;
import com.job.ic.utils.FileUtils;

import org.apache.log4j.Logger;

import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

public class ResultDb {

	private static Logger logger = Logger.getLogger(ResultDb.class);

	/**
	 * Berkeley Db's Environment instances
	 */
	private static Environment env;
	
	/**
	 * Berkeley Db's EntityStore instances, store the features of the downloaded websites
	 */
	private static EntityStore resultStore;

	
	/**
	 * Use for create the bdb's environment, entity store
	 * 
	 * @param conf
	 *            Crawler's config instance
	 */
	public static void createEnvironment() {
		if (env == null && resultStore == null) {
			EnvironmentConfig config = new EnvironmentConfig();
			StoreConfig sc = new StoreConfig();
			sc.setTransactional(true);
			sc.setAllowCreate(true);
			config.setAllowCreate(true);
			config.setTransactional(true);
			config.setCachePercentVoid(30);
			
			CheckpointConfig cfg = new CheckpointConfig();
			cfg.setForce(true);
			cfg.setMinutes(10);
			
			
			
			env = new Environment(new File(CrawlerConfig.getConfig().getDbPath()), config);
			
			env.checkpoint(cfg);
			resultStore = new EntityStore(env, "result", sc);
		}else{
			logger.info("use old result store");
		}
	}
	
	
//	public static ResultDAO getResultDAO(){
//		if(rd == null)
//			rd = new ResultDAO(getResultStore());
//		
//		return rd;
//	}

	public static void createEnvironment(String dbPath) {
		if (env == null && resultStore == null) {
			EnvironmentConfig config = new EnvironmentConfig();
			StoreConfig sc = new StoreConfig();
			sc.setTransactional(true);
			sc.setAllowCreate(true);

			config.setAllowCreate(true);
			config.setTransactional(true);

			env = new Environment(new File(dbPath), config);
			resultStore = new EntityStore(env, "result", sc);
		}
	}

	public static void removeData(String dbPath, String segmentPath) {
		String[] rem = FileUtils.readFile(segmentPath);
		createEnvironment(dbPath);
		SegmentGraphDAO hs = new SegmentGraphDAO(resultStore);
		ResultDAO rs = new ResultDAO(resultStore);
		long size = rem.length;
		long count = 0;

		for (String s : rem) {
			hs.deleteBySource(s.trim());
			rs.deleteData(s.trim());
			printProgress(count++, size);
		}

		printProgress(size, size);

	}

	public static boolean isThaiHost(ResultModel m, double percentThai) {
		int thai = m.getRelevantPage();
		int non = m.getIrrelevantPage();
		int total = thai + non;

		if (total != 0 && ((1.0 * thai) / total) >= percentThai) {
			return true;
		}

		return false;
	}

	public static double calcPercentThai(ResultModel m) {
		int thai = m.getRelevantPage();
		int non = m.getIrrelevantPage();
		int total = thai + non;

		return (1.0 * thai) / total;
	}



//	public static void createEnvironment(CrawlerConfig conf) {
//		if (env == null && resultStore == null) {
//			EnvironmentConfig config = new EnvironmentConfig();
//			StoreConfig sc = new StoreConfig();
//			sc.setTransactional(true);
//			sc.setAllowCreate(true);
//			config.setAllowCreate(true);
//			config.setTransactional(true);
//
//			env = new Environment(new File(conf.getDbPath()), config);
//			resultStore = new EntityStore(env, "result", sc);
//		}
//	}

	private static void printProgress(long current, long total) {
		double percent = 1.0 * current / total;
		int num = (int) (50 * percent);
		String output = "";
		output += ("\r[");
		int i;
		for (i = 0; i < num; i++)
			output += ("#");

		for (; i < 50; i++)
			output += ("-");

		output += String.format("] %.2f - (%d/%d)", percent * 100, current, total);
		logger.warn(output);
	}

	public static void mergeDb(String source, String dest) {
		EnvironmentConfig config = new EnvironmentConfig();
		StoreConfig sc = new StoreConfig();
		sc.setTransactional(true);
		sc.setAllowCreate(true);
		config.setAllowCreate(true);
		config.setTransactional(true);

		// source
		Environment env1 = new Environment(new File(source), config);
		EntityStore resultStore1 = new EntityStore(env1, "result", sc);
		SegmentGraphDAO hs = new SegmentGraphDAO(resultStore1);
		ResultDAO rs = new ResultDAO(resultStore1);

		// dest
		Environment env2 = new Environment(new File(dest), config);
		EntityStore resultStore2 = new EntityStore(env2, "result", sc);
		SegmentGraphDAO hd = new SegmentGraphDAO(resultStore2);
		ResultDAO rd = new ResultDAO(resultStore2);

		// merge
		ArrayList<SegmentGraphModel> graphs;
		ArrayList<ResultModel> results;
		HashSet<String> dup = new HashSet<String>();

		long graphSize = hs.getSize();
		long resultSize = rs.getSize();
		long count = 0;
		int limit = 1000;

		logger.info("\nMerge result:");
		// merge result
		count = 0;
		while (count < rs.getSize()) {
			results = rs.getRange(count, count + limit);
			for (ResultModel r : results) {
//				if (rd.isDuplicate(r.getSegmentName()))
//					dup.add(r.getSegmentName());
//				else
					rd.addData(r);
			}
			results.clear();
			results = null;
			printProgress(count, resultSize);
			count += limit;
		}
		printProgress(resultSize, resultSize);

		logger.info("Merge segment graph:");
		// merge graph
		while (count < hs.getSize()) {
			graphs = hs.getAll();

			for (SegmentGraphModel g : graphs) {
				if (dup.contains(g.getSourceSeg()))
					continue;

				hd.pix.put(g);
			}

			graphs.clear();
			graphs = null;
			printProgress(count, graphSize);
			count += limit;
		}
		printProgress(graphSize, graphSize);

		// close
		resultStore1.close();
		resultStore2.close();
		env1.close();
		env2.close();

	}

	/**
	 * Use for close bdb's environment and entity store
	 */
	public static void close() {
		try {
			env.cleanLog();
			
			resultStore.sync();
			env.sync();
			env.flushLog(true);

			logger.warn("SYNC Database Complete");

			resultStore.close();
			env.close();
			env = null;
			resultStore = null;
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
			logger.error("Error closing MyDbEnv: " + dbe.toString());
		}
	}

	/**
	 * use for get the bdb's EntityStore
	 * @return 
	 */
	public static EntityStore getStore() {
		if (env != null && resultStore != null) {
			return resultStore;
		}
		return null;
	}

	public static ResultDAO getResultDAO(){
		if (env != null && resultStore != null) {
			return new ResultDAO(resultStore);
		}
		
		return null;
	}
	
	public static SegmentGraphDAO getSegmentGraphDAO(){
		if (env != null && resultStore != null) {
			return new SegmentGraphDAO(resultStore);
		}
		
		return null;
	}


//	public static SegmentGraphDAO getSegmentGraphDAO() {
//		if(sd == null)
//			sd = new SegmentGraphDAO(getResultStore());
//		
//		return sd;
//	}

}
