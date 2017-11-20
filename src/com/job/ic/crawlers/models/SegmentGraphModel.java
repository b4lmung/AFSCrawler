/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.models;

import java.util.ArrayList;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;

@Entity
public class SegmentGraphModel {

	public SegmentGraphModel() {
	}

	@PrimaryKey(sequence = "ID")
	private long id;
	@SecondaryKey(relate = Relationship.MANY_TO_ONE)
	private String sourceSeg;
	@SecondaryKey(relate = Relationship.MANY_TO_ONE)
	private String destSeg;

	private ArrayList<LinksModel> links;
	

	public SegmentGraphModel(String sourceSeg, String destSeg, ArrayList<LinksModel> links) {
		super();
		this.sourceSeg = sourceSeg.toLowerCase();
		this.destSeg = destSeg.toLowerCase();
		this.links = links;
	}

	public String getSourceSeg() {
		return sourceSeg;
	}

	public void setSourceSeg(String sourceHost) {
		this.sourceSeg = sourceHost;
	}

	public String getDestSeg() {
		return destSeg;
	}

	public void setDestSeg(String destHost) {
		this.destSeg = destHost;
	}

	public long getId() {
		return id;
	}

	public ArrayList<LinksModel> getLinks() {
		return links;
	}

	public void setLinks(ArrayList<LinksModel> links) {
		this.links = links;
	}
	
	@Override
	public String toString() {
		return String.format("Soruce:%s \tDest:%s", this.sourceSeg, this.destSeg);
	}

	
}
