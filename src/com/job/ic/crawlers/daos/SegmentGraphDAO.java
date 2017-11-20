/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.daos;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.models.SegmentGraphModel;
import com.job.ic.crawlers.parser.HtmlParser;
import com.job.ic.utils.FileUtils;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;

public class SegmentGraphDAO {

	private static Logger logger = Logger.getLogger(SegmentGraphDAO.class);

	/**
	 * Primaryndex, reference to index field of SeggraphModel
	 */
	public PrimaryIndex<Long, SegmentGraphModel> pix;
	/**
	 * SecondaryIndex, reference to source field of SeggraphModel
	 */
	public SecondaryIndex<String, Long, SegmentGraphModel> source;
	/**
	 * SecondaryIndex, reference to destination field of SeggraphModel
	 */
	public SecondaryIndex<String, Long, SegmentGraphModel> dest;

	private EntityStore currentStore;

	/**
	 * Constructor
	 * 
	 * @param store
	 *            EntityStore of the bdb's environment
	 */
	public SegmentGraphDAO(EntityStore store) {
		this.currentStore = store;
		this.pix = this.currentStore.getPrimaryIndex(Long.class, SegmentGraphModel.class);
		this.source = this.currentStore.getSecondaryIndex(this.pix, String.class, "sourceSeg");
		this.dest = this.currentStore.getSecondaryIndex(this.pix, String.class, "destSeg");
	}

	/**
	 * add Seggraph between source's Seg and destination's Seg
	 * 
	 * @param sourceSeg
	 *            source's Segname
	 * @param destSeg
	 *            destination's Segnames
	 * @return true if successful
	 */
	public boolean addSegLinks(String sourceSeg, String destSeg, ArrayList<LinksModel> linksToDestSeg) {

		try {
			Transaction txn;
			txn = this.currentStore.getEnvironment().beginTransaction(null, null);

			this.pix.putNoOverwrite(txn, new SegmentGraphModel(sourceSeg.toLowerCase(), destSeg, linksToDestSeg));

			txn.commit();
			txn = null;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	public long getSize() {
		return this.pix.count();
	}

	public void deleteBySource(String source) {
		Transaction txn = this.currentStore.getEnvironment().beginTransaction(null, null);
		EntityCursor<SegmentGraphModel> m = this.source.subIndex(source).entities(txn, null);
		SegmentGraphModel tmp;
		for (tmp = m.first(); tmp != null; tmp = m.next()) {
			m.delete();
		}

		m.close();
		txn.commit();

	}

	/**
	 * method use for get all of the destinations's Segname from input a
	 * source's Segname
	 * 
	 * @param sourceSeg
	 *            source's Segname
	 * @return all destinations site
	 */
	public ArrayList<SegmentGraphModel> getDests(String sourceSeg) {
		EntityCursor<SegmentGraphModel> e = this.source.subIndex(sourceSeg).entities();
		Iterator<SegmentGraphModel> i = e.iterator();
		ArrayList<SegmentGraphModel> output = new ArrayList<>();
		while (i.hasNext()) {
			output.add(i.next());
		}

		e.close();
		if (output.size() > 0) {
			return output;
		}

		return null;

	}

	/**
	 * method use for get source's Segname from the input destination's Segname
	 * 
	 * @param destSeg
	 *            destnation's Segname
	 * @return all sources site
	 */
	public ArrayList<SegmentGraphModel> getSources(String destSeg) {
		EntityCursor<SegmentGraphModel> e = this.dest.subIndex(destSeg).entities();
		Iterator<SegmentGraphModel> i = e.iterator();
		ArrayList<SegmentGraphModel> output = new ArrayList<>();
		while (i.hasNext()) {
			output.add(i.next());
		}

		e.close();
		if (output.size() > 0) {

			return output;
		}

		return null;

	}

	// public ArrayList<String> getAllDestNoDup() {
	//
	// EntityCursor<SegmentGraphModel> e = this.pix.entities();
	// SegmentGraphModel m;
	// ArrayList<String> dests = new ArrayList<String>();
	// for(m = e.first(); m != null; e.nextNoDup()){
	// dests.add(m.getDestSeg());
	// }
	// e.close();
	//
	// return dests;
	// }
	//
	// public ArrayList<String> getAllDestNoDupWithLim(HashSet<String>
	// limitSourceSeg) {
	// EntityCursor<SegmentGraphModel> e;
	// SegmentGraphModel m;
	// ArrayList<String> dests = new ArrayList<String>();
	//
	// for(String s: limitSourceSeg){
	// e = this.source.subIndex(s).entities();
	// for(m = e.first(); m != null; e.nextNoDup()){
	// dests.add(m.getDestSeg());
	// }
	//
	// e.close();
	// }
	//
	// return dests;
	// }

	/**
	 * method use for get all of the Seggraph record from the entity store
	 * 
	 * @return Seggraphs
	 */
	public ArrayList<SegmentGraphModel> getAll() {

		EntityCursor<SegmentGraphModel> e = this.pix.entities();
		Iterator<SegmentGraphModel> i = e.iterator();
		ArrayList<SegmentGraphModel> output = new ArrayList<>();
		
		long curr = 0;
		while (i.hasNext()) {
			output.add(i.next());
			if(curr%10000 == 0)
				logger.info(curr + "/" + this.pix.count());
			curr++;
		}

		e.close();
		if (output.size() > 0) {

			return output;
		}

		return null;
	}

	public void extractAllLinks(String destPath) {

		UrlDb.createEnvironment("urlDb");
		UrlDAO rd = UrlDb.getUrlDAO();
		
		EntityCursor<SegmentGraphModel> e = this.pix.entities();
		Iterator<SegmentGraphModel> i = e.iterator();
		
		
		try (BufferedWriter bw = FileUtils.getBufferedFileWriter(destPath)) {
			long total = this.pix.count();
			long curr = 0;

			HashSet<String> results = new HashSet<String>();
			
			while (i.hasNext()) {
				
				SegmentGraphModel sm = i.next();
				List<String> urls = sm.getLinks().stream().map(m -> m.getLinkUrl()).filter(m -> !HtmlParser.shouldFilter(m)).collect(Collectors.toList());
				results.addAll(urls);

				if(curr%10000 == 0)
					logger.info(curr + "/" + total);
				
				curr++;
			}

			for(String s: results){
				bw.write(s + "\n");
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (e != null)
					e.close();
			} catch (Exception ex) {
			}
		}

		UrlDb.close();

		// return null;
	}

//	public ArrayList<SegmentGraphModel> getRange(long start, long end) {
//
//		EntityCursor<SegmentGraphModel> e = this.pix.entities();
//		Iterator<SegmentGraphModel> i = e.iterator();
//		ArrayList<SegmentGraphModel> output = new ArrayList<>();
//		int index = 0;
//		
//		while (i.hasNext()) {
//			if (index < start) {
//				index++;
//				i.next(); // index++;
//				continue;
//			}
//
//			output.add(i.next());
//
//			index++;
//
//			if (index > end)
//				break;
//		}
//
//		e.close();
//		if (output.size() > 0) {
//			return output;
//		}
//
//		return null;
//	}

	public HashSet<String> getDestSegs() {

		HashSet<String> output = new HashSet<>();
		EntityCursor<SegmentGraphModel> e = this.pix.entities();
		Iterator<SegmentGraphModel> i = e.iterator();
		long index = 0;
		long count = this.pix.count();
		while (i.hasNext()) {
			if (index % 1000 == 0)
				logger.info(index + "/" + count);
			output.add(i.next().getDestSeg());
			index++;
		}

		e.close();
		if (output.size() > 0) {
			return output;
		}

		return null;
	}
}
