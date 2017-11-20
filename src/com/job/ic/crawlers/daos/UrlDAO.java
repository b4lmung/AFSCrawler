/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.daos;

import java.util.ArrayList;
import java.util.Iterator;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.URLModel;
import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;

public class UrlDAO {

	public PrimaryIndex<String, URLModel> pix;
	public SecondaryIndex<String, String, URLModel> six;
	// public SecondaryIndex<Boolean, String, URLModel> dlx;

	public long getSize() {
		return this.pix.count();
	}

	public UrlDAO(EntityStore store) {
		this.pix = store.getPrimaryIndex(String.class, URLModel.class);
		this.six = store.getSecondaryIndex(this.pix, String.class, "site");
		// this.dlx = store.getSecondaryIndex(this.pix, Boolean.class, "isDl");
	}

	private long countSite(String url) {
		String site = HttpUtils.getHost(url);
		if (site == null)
			return 0;
		
		return this.six.subIndex(site).count()/2;
	}

	public URLModel[] getAll() {
		try {
			EntityCursor<URLModel> e = this.pix.entities();
			Iterator<URLModel> i = e.iterator();
			ArrayList<URLModel> output = new ArrayList<>();

			while (i.hasNext()) {
//				System.out.println(i.next().getUrl());
				output.add(i.next());
			}

			e.close();
			System.out.println(output.size());
			if (output.size() > 0) {
				URLModel[] tmp = new URLModel[output.size()];
				return output.toArray(tmp);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private void addUrl(String url) {
		url = HttpUtils.getStaticUrl(url);

		if (url == null)
			return;

		url = url.toLowerCase().trim();
		this.pix.put(new URLModel(url));
		this.pix.put(new URLModel(url + "/"));
		if (url.charAt(url.length() - 1) == '/') {
			this.pix.put(new URLModel(url.substring(0, url.length() - 1)));
		}
	}

	static int i = 0;
	public synchronized boolean checkAndAddUrl(String url, boolean addUrl) {

		if (CrawlerConfig.getConfig() != null && CrawlerConfig.getConfig().getMaxPagePerSite() >= 0 && countSite(url) >= CrawlerConfig.getConfig().getMaxPagePerSite()) {
			return true;
		}

		boolean isDownloaded = containUrlWithLock(url);
		if (!isDownloaded && addUrl)
			addUrl(url);

		
		return isDownloaded;
	}

	private boolean containUrlWithLock(String url) {
		url = HttpUtils.getStaticUrl(url);

		if (url == null)
			return false;

		url = url.toLowerCase().trim();

		url = url.toLowerCase().trim();
		if (url.charAt(url.length() - 1) == '/') {
			String url3 = StringUtils.md5(url.substring(0, url.length() - 1));
			url3 = StringUtils.md5(url + "/");
			if (this.pix.contains(url3))
				return true;
		}

		String url1 = StringUtils.md5(url);
		String url2 = StringUtils.md5(url + "/");
		return this.pix.contains(url1) || this.pix.contains(url2);
	}

	private void delete(String url) {
		url = HttpUtils.getStaticUrl(url);
		if (url == null)
			return;
		url = url.toLowerCase().trim();
		url = StringUtils.md5(url);
		if (this.pix.contains(url))
			;
		this.pix.delete(url);

	}

}
