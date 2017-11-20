/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.daos;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.models.ResultModel;
import com.job.ic.crawlers.models.SegmentGraphModel;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.HttpUtils;

import org.apache.log4j.Logger;

import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

public class ResultDAO {
	private static Logger logger = Logger.getLogger(ResultDAO.class);

	public PrimaryIndex<String, ResultModel> pix;
	private EntityStore currentStore;
	
	public long getSize() {
		return this.pix.count();
	}

	/**
	 * Constructor
	 * 
	 * @param store
	 *            EntityStore of the bdb's environment
	 */
	public ResultDAO(EntityStore store) {
		this.currentStore = store;
		this.pix = currentStore.getPrimaryIndex(String.class, ResultModel.class);
//		System.out.println(this.currentStore.getEnvironment().getHome().getName());
	}

	/**
	 * method use for check what if the input hostname is contained in the entity store
	 * 
	 * @param hostname
	 *            hostname
	 * @return if duplicate return true, else return false
	 */
	public boolean isDuplicate(String hostname) {
		return this.pix.contains(hostname.toLowerCase());
	}

	/**
	 * method use for delete features data from the entity store
	 * 
	 * @param hostname
	 *            site's hostname
	 * @return if delete successful this method returns true, else it returns false
	 */
	public boolean deleteData(String hostname) {
		Transaction txn = this.currentStore.getEnvironment().beginTransaction(null, null);
		boolean out = this.pix.delete(txn, hostname.toLowerCase());

		txn.commit();
		txn = null;
		return out;
	}

	/**
	 * method use for insert features data
	 * 
	 * @param rs
	 *            features object
	 * @return true if insert data successful, else return false
	 */
	public boolean addData(ResultModel rs) {
		Transaction txn = this.currentStore.getEnvironment().beginTransaction(null, null);
		boolean out = this.pix.putNoOverwrite(txn, rs);
		txn.commit();
		txn = null;
		return out;
	}

//	public void AddUpdateData(ResultModel rs) {
//		String segName = rs.getSegmentName();
//		ResultModel rm;
//		if ((rm = getData(segName)) != null) {
//			rs.setIrrelevantPage(rs.getIrrelevantPage() + rm.getIrrelevantPage());
//			rs.setIrrelevantPage(rs.getRelevantPage() + rm.getRelevantPage());
//		}
//
//		Transaction txn = this.currentStore.getEnvironment().beginTransaction(null, null);
//		this.pix.putNoReturn(txn, rs);
//		txn.commit();
//		txn = null;
//	}

	public void updateData(ResultModel rs) {
		Transaction txn = this.currentStore.getEnvironment().beginTransaction(null, null);
		this.pix.putNoReturn(txn, rs);
		txn.commit();
		txn = null;
	}

	/**
	 * method use for get all of the hostgraph record from the entity store
	 * 
	 * @return all features which contain in entity store
	 */
	public ResultModel[] getAll() {
		try {
			EntityCursor<ResultModel> e = this.pix.entities();
			Iterator<ResultModel> i = e.iterator();
			ArrayList<ResultModel> output = new ArrayList<>();

			while (i.hasNext()) {
				output.add(i.next());
			}

			e.close();
			if (output.size() > 0) {
				ResultModel[] tmp = new ResultModel[output.size()];
				return output.toArray(tmp);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public ArrayList<ResultModel> getRange(long start, long end) {
		try {
			EntityCursor<ResultModel> e = this.pix.entities();
			Iterator<ResultModel> i = e.iterator();
			ArrayList<ResultModel> output = new ArrayList<>();
			int index = 0;
			ResultModel rm;
			// String tmp;
			while (i.hasNext()) {
				if (index < start) {
					index++;
					i.next(); // index++;
					continue;
				}

				rm = i.next();
				
				output.add(rm);
				index++;

				if (index > end)
					break;
			}

			e.close();
			if (output.size() > 0) {
				return output;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * use for get the input host's features
	 * 
	 * @param hostname
	 *            host name
	 * @return features object
	 */
	public ResultModel getData(String segName) {
		try {
			ResultModel r = this.pix.get(segName.toLowerCase());
			return r;
		} catch (Exception e) {
			// e.printStackTrace();
			return null;
		}
	}

//	public ResultModel getData(String segName, double percentThai) {
//		try {
//			ResultModel r = this.pix.get(segName.toLowerCase());
//			ResultModel tmp;
//
//			boolean isThai = FeaturesExtractionNew.isThaiHost(r, percentThai);
//			boolean isChildThai = false;
////			if (incChild) {
////				for (String s : r.getChildSegments()) {
////					if ((tmp = this.pix.get(s)) != null) {
////						isChildThai = FeaturesExtraction.isThaiHost(tmp, percentThai);
////						if (isThai == isChildThai) {
////							r.setRelevantPage(r.getRelevantPage() + tmp.getRelevantPage());
////							r.setIrrelevantPage(r.getIrrelevantPage() + tmp.getIrrelevantPage());
////							childSegs.add(s);
////						}
////					}
////				}
////			}
//
//			return r;
//		} catch (Exception e) {
//			// e.printStackTrace();
//			return null;
//		}
//	}
	
	public static synchronized double calcPercentRel(ResultModel m) {
		int thai = m.getRelevantPage();
		int non = m.getIrrelevantPage();
		int total = thai + non;

		return (1.0 * thai) / total;
	}

	public static void extractSourceSegLinks(HashSet<String> segDb, String sourceDbPath, String destDbPath, double thaiPercent, int id) throws IOException {

		String tmp;
		Hashtable<String, Double> results = new Hashtable<String, Double>();

		ResultDb.createEnvironment(destDbPath);
		ResultDAO rd = ResultDb.getResultDAO();

		logger.info("Loading destination result :" + destDbPath);

		long count = 0;
		ArrayList<ResultModel> arr;
		long total = rd.getSize();

		while (count < total) {
			arr = rd.getRange(count, count + 1000);
			for (ResultModel r : arr) {
				if (segDb.contains(r.getSegmentName().trim()))
					continue;
				
				if (r.getRelevantPage() + r.getIrrelevantPage() < 5)
					continue;
				
			    if(calcPercentRel(r) >= thaiPercent){
			    	if(r.getRelevantPage() >= 5){
			    		results.put(r.getSegmentName().trim(), calcPercentRel(r));
			    	}
			    }
				segDb.add(r.getSegmentName().trim());
			}

			count += 1000;
		}

		logger.info("Total dest seg :" + results.size());
		/*
		 * for (String s : FileUtils.readFile(destSeg)) { rm = rd.getData(s); if (rm == null)
		 * continue; results.put(rm.getSegmentName().trim(),
		 * FeaturesExtraction.calcPercentThai(rm)); }
		 */
		ResultDb.close();

		logger.info("Loading source segment graph :" + sourceDbPath);
		ResultDb.createEnvironment(sourceDbPath);
		rd = ResultDb.getResultDAO();
		SegmentGraphDAO sd = ResultDb.getSegmentGraphDAO();
		Enumeration<String> keys = results.keys();
		ArrayList<SegmentGraphModel> ss;
//		ArrayList<LinksModel> links;

		count = 0;
		total = results.size();
//
//		String url;
//		String anchor;
//		String tmpUrl;
		HashSet<String> urls = new HashSet<String>();

		HashSet<String> sourceThSeg = new HashSet<String>();
		HashSet<String> dest = new HashSet<String>();

		BufferedWriter bw = FileUtils.getBufferedFileWriter("result-graph.txt");
	
		ResultModel rm;
		while (keys.hasMoreElements()) {
//			anchor = null;
//			url = null;
//			anchor = "";
			urls.clear();

			tmp = keys.nextElement();

			ss = sd.getSources(tmp);
			if (ss == null)
				continue;

//			url = tmp;
			
			for (SegmentGraphModel t : ss) {
				if (t.getSourceSeg().equals(tmp))
					continue;

				if(sourceThSeg.contains(t.getSourceSeg()))
					continue;
				
				sourceThSeg.add(t.getSourceSeg());

			}

			logger.info(String.format("%d/%d\t%s", count++, total, tmp));
		}
		
		for(String s: sourceThSeg){
			if((rm = rd.getData(s)) == null)
				continue;
			
			bw.write(String.format("%s\t%s\t%s\n", rm.getSegmentName(), HttpUtils.getDomain(rm.getSegmentName()), rm.getIpCountry()));
			
			dest.add(rm.getSegmentName());
		}
		bw.close();
		ResultDb.close();
	}

	public static void getDestSegFromSourceSeg(HashSet<String> segDb, String sourceDbPath, String destDbPath, ArrayList<String> queue, String sourceOutput, String destOutput, double thaiPercent,
			boolean isSeed) {

		try {
			ResultDb.createEnvironment(sourceDbPath);
			SegmentGraphDAO sd = ResultDb.getSegmentGraphDAO();
			ResultDAO rd = ResultDb.getResultDAO();
			PrintWriter result = new PrintWriter(sourceOutput);
			HashSet<String> dest = new HashSet<>();

			long count = 0;
			long size = queue.size();

			ResultModel rm;
			ArrayList<SegmentGraphModel> tmp;
			String seg;
			ArrayList<LinksModel> links = null;

			boolean isThai = false;
			// วน source seg

			for (String f : queue) {
				printProgress(count++, size);

				seg = HttpUtils.getBasePath(f);

				rm = rd.getData(seg);

				if (rm == null) {
					if ((rm = rd.getData(seg.toLowerCase())) == null)
						continue;
				}

				if (segDb.contains(rm.getSegmentName()))
					continue;

				isThai = calcPercentRel(rm) >= thaiPercent;
				result.write(String.format("%s\t%s\t%s\n", rm.getSegmentName(), rm.getRelevantPage(), rm.getIrrelevantPage()));

				tmp = sd.getDests(seg);

				if (tmp == null)
					continue;

				if (isThai || isSeed) {
					for (SegmentGraphModel t : tmp) {
						links = t.getLinks();

						for (LinksModel l : links) {
							dest.add(HttpUtils.getBasePath(l.getLinkUrl()));

						}
					}
				}

				segDb.add(rm.getSegmentName());

			}

			logger.info("Finished reading source segment");
			// printProgress(count, size);

			System.out.println("+++++++" + dest.size());
			// System.out.println("!!!!!!!!" + queue.size());

			// ใส่ destination อันใหม่เข้าไป
			queue.clear();
			for (String s : dest) {
				queue.add(s);
			}

			result.close();
			ResultDb.close();

			if (destOutput != null && destDbPath != null) {
				ResultDb.createEnvironment(sourceDbPath);
				rd = ResultDb.getResultDAO();
				BufferedWriter bw = FileUtils.getBufferedFileWriter(destOutput);
				for (String s : dest) {
					if ((rm = rd.getData(s)) != null) {
						bw.write(String.format("%s\t%d\t%d\n", rm.getSegmentName(), rm.getRelevantPage(), rm.getIrrelevantPage())); // else
					}
				}
				bw.close();
				ResultDb.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void getDestSegs(String dbPath, String sourceSegs, String output) {
		ResultDb.createEnvironment(dbPath);
		SegmentGraphDAO s = ResultDb.getSegmentGraphDAO();
		String[] files = FileUtils.readFile(sourceSegs);
		HashSet<String> dest = new HashSet<String>();
		ArrayList<SegmentGraphModel> tmp;

		long count = 0;
		long size = files.length;

		for (String f : files) {
			tmp = s.getDests(f);
			if (tmp == null)
				continue;

			for (SegmentGraphModel t : tmp) {
				dest.add(HttpUtils.getBasePath(t.getDestSeg()));
			}

			printProgress(count, size);
			count++;
		}

		printProgress(count, size);
		logger.info("\nwritting to " + output);

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(output))) {
			Iterator<String> i = dest.iterator();
			String t;
			while (i.hasNext()) {
				t = i.next();
				if (t != null)
					bw.write(t.concat("\n"));
			}
		} catch (Exception e) {
			logger.error(e.getMessage() + e.getMessage());
		}
	}

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

	

	public static void extractSourceSegLinks2( String sourceDbPath, String destDbPath, double thaiPercent) throws IOException {

		BufferedWriter source = FileUtils.getBufferedFileWriter("source.txt", true);
		BufferedWriter dest = FileUtils.getBufferedFileWriter("dest.txt", true);
		String tmp;
		Hashtable<String, Double> results = new Hashtable<String, Double>();

		ResultDb.createEnvironment(destDbPath);
		ResultDAO rd = ResultDb.getResultDAO();

		logger.info("Loading destination result :" + destDbPath);

		long count = 0;
		ArrayList<ResultModel> arr;
		long total = rd.getSize();

		while (count < total) {
			arr = rd.getRange(count, count + 1000);
			for (ResultModel r : arr) {
			
				if(calcPercentRel(r) >= thaiPercent){

					String domain = HttpUtils.getDomain(r.getSegmentName());
					if(domain == null)
						continue;
					
					results.put(r.getSegmentName().trim(), calcPercentRel(r));
					dest.write(String.format("%s\t%s\t%s\n", r.getSegmentName(), r.getIpCountry(), domain));  
				}
			}

			count += 1000;
		}

		logger.info("Total dest seg :" + results.size());
		
		ResultDb.close();

		logger.info("Loading source segment graph :" + sourceDbPath);
		ResultDb.createEnvironment(sourceDbPath);
		SegmentGraphDAO sd = ResultDb.getSegmentGraphDAO();
		rd = ResultDb.getResultDAO();
		Enumeration<String> keys = results.keys();
		ArrayList<SegmentGraphModel> ss;
		

		count = 0;
		total = results.size();

	
		HashSet<String> sourceThSeg = new HashSet<String>();
		while (keys.hasMoreElements()) {
			tmp = keys.nextElement();

			ss = sd.getSources(tmp);
			if (ss == null)
				continue;

			
			for (SegmentGraphModel t : ss) {
				String srcSeg = t.getSourceSeg();
				
				if (srcSeg.equals(tmp))
					continue;

				if(sourceThSeg.contains(srcSeg.toLowerCase()))
					continue;
				
				
				sourceThSeg.add(srcSeg.toLowerCase());
				ResultModel m = rd.getData(srcSeg);
				if(m == null)
					continue;
				
				
				String domain = HttpUtils.getDomain(m.getSegmentName());
				if(domain == null)
					continue;
				
				source.write(String.format("%s\t%s\t%s\n", m.getSegmentName(), m.getIpCountry(), domain));  
			}
			

			

			logger.info(String.format("%d/%d\t%s", count++, total, tmp));
		}

		source.close();
		dest.close();

		ResultDb.close();
	}

}
