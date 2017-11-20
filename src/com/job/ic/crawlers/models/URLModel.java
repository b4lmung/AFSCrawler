package com.job.ic.crawlers.models;

import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;

@Entity
public class URLModel {
	@PrimaryKey
	private String url;

	@SecondaryKey(relate = Relationship.MANY_TO_ONE)
	private String site;
	
//	@SecondaryKey(relate = Relationship.MANY_TO_ONE)
//	private Boolean isDl;

	public URLModel() {

	}

	public URLModel(String url) {
		this.url = StringUtils.md5(url);
		this.site = HttpUtils.getHost(url);
//		this.setIsDl(false);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getSite(){
		return this.site;
	}
//	public Boolean getIsDl() {
//		return isDl;
//	}
//
//	public void setIsDl(Boolean isDl) {
//		this.isDl = isDl;
//	}
}
