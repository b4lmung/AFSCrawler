/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.daos;

import java.util.ArrayList;
import java.util.Iterator;

import com.job.ic.crawlers.models.WebsiteSegment;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityIndex;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;

public class WebsiteSegmentDAO {

	public PrimaryIndex<Long, WebsiteSegment> pix;
	public SecondaryIndex<String, Long, WebsiteSegment> six;
	// public SecondaryIndex<Boolean, String, WebsiteSegment> dlx;

	public long getSize() {
		return this.pix.count();
	}

	public WebsiteSegmentDAO(EntityStore store) {
		this.pix = store.getPrimaryIndex(Long.class, WebsiteSegment.class);
		this.six = store.getSecondaryIndex(this.pix, String.class, "segmentName");
		// this.dlx = store.getSecondaryIndex(this.pix, Boolean.class, "isDl");
	}
	
	
	public WebsiteSegment[] getAll() {
		try {
			EntityCursor<WebsiteSegment> e = this.pix.entities();
			Iterator<WebsiteSegment> i = e.iterator();
			ArrayList<WebsiteSegment> output = new ArrayList<>();

			while (i.hasNext()) {
				output.add(i.next());
			}

			e.close();
			System.out.println(output.size());
			if (output.size() > 0) {
				WebsiteSegment[] tmp = new WebsiteSegment[output.size()];
				return output.toArray(tmp);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public void addWebsiteSegment(WebsiteSegment seg) {
		this.pix.put(seg);
	}


	public ArrayList<WebsiteSegment> retrieve(String segname) {
		EntityIndex<Long, WebsiteSegment> tmp = this.six.subIndex(segname.toLowerCase());
		EntityCursor<WebsiteSegment> curr = tmp.entities();
		
		Iterator<WebsiteSegment> i = curr.iterator();
		ArrayList<WebsiteSegment> output = new ArrayList<>();
		while (i.hasNext()) {
			output.add(i.next());
		}
		
//		System.out.println(output.size());
		curr.close();
		
		for(WebsiteSegment w : output){
			tmp.delete(w.getID());
		}
//		System.out.println(segname + ">>>" + output.size());
		return output;
	}

	

}
