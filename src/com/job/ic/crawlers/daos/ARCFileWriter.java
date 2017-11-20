package com.job.ic.crawlers.daos;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.PageObject;
import com.job.ic.utils.FileUtils;

import org.archive.io.arc.ARCConstants;
import org.archive.io.arc.ARCWriter;

public class ARCFileWriter {

	private ARCWriter writer;
	private static final String DEFAULT_IP = "127.0.0.1";

	public ARCFileWriter() throws Exception {
		
		writer = null;
		if(CrawlerConfig.getConfig() == null)
			throw new Exception("load config first");
		
		if (!CrawlerConfig.getConfig().getDownloadPath().equals("") && CrawlerConfig.getConfig().getDownloadPath() != null) {
			FileUtils.mkdir(FileUtils.fixPath(CrawlerConfig.getConfig().getDownloadPath()));
			File[] dirs = new File[] { new File(FileUtils.fixPath(CrawlerConfig.getConfig().getDownloadPath())) };
			writer = new ARCWriter(new AtomicInteger(), Arrays.asList(dirs), "dl",CrawlerConfig.getConfig().isGzip(), ARCConstants.DEFAULT_MAX_ARC_FILE_SIZE);
		}
	}


	public void close() {
		try {
			if (writer != null) {
				writer.close();
			}
		} catch (IOException e) {
		}
	}

	public synchronized void writeRecord(PageObject fp) {
		if (writer == null) {
			return;
		}

		try (ByteArrayInputStream bis = new ByteArrayInputStream(fp.getContent())) {
			writer.write(fp.getUrl(), fp.getContentType(), DEFAULT_IP, getTime(), fp.getContent().length, bis);
			bis.close();
			
		} catch (IOException e) {

		}

	}

	public static void main(String[] args) throws Exception {

		
		Locale.setDefault(Locale.US);
		System.setProperty("file.encoding", "UTF-8");

		
		File destination = new File("crawler");
		String siteurl = "passmaid";
		File[] dirs = new File[] { destination };
		ARCWriter w = new ARCWriter(new AtomicInteger(), Arrays.asList(dirs), "crawl-" + siteurl, false, -1);
		
		//w.checkSize();
		byte[] b1 = "hello world test".getBytes();
		ByteArrayInputStream bis = new ByteArrayInputStream(b1);
		
		w.write("http://joblovepass.com/index.html", "text/html", "153.12.3.2", getTime(), b1.length, bis);
		
		w.close();
	}
	
	
	public static long getTime(){
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.UK);
		return c.getTime().getTime();
	}
}
