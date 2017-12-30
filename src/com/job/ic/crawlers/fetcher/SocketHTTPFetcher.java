/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.fetcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import com.job.ic.crawlers.daos.UrlDAO;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.PageObject;
import com.job.ic.utils.HttpUtils;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

public class SocketHTTPFetcher {
	private Random r;
	private CloseableHttpClient client;

	public SocketHTTPFetcher(CloseableHttpClient client) {
		this.client = client;
	}

	private static Logger logger = Logger.getLogger(SocketHTTPFetcher.class);

	public PageObject download(String url, UrlDAO urlDao) {

		PageObject fp = null;
		InputStream is = null;
		byte[] b;

		if (url.indexOf("http://") < 0) {
			url = "http://" + url;
		}

		if (urlDao != null && urlDao.checkAndAddUrl(url, true)) {
			return null;
		}

		HttpGet request = null;
		HttpEntity entity = null;
		CloseableHttpResponse response = null;
		String encoding = "null";
		// Prepare request message
		request = new HttpGet(url);

		try {
			// Get response
			response = client.execute(request);

			int status = response.getStatusLine().getStatusCode();
			// System.out.println(status);
			if (status != 200 && status != 302) {
				HttpClientUtils.closeQuietly(response);
				EntityUtils.consume(entity);
				return null;
			}

			entity = response.getEntity();

			String contentType = entity.getContentType().getValue();
			if (contentType != null) {
				contentType = contentType.toLowerCase();
				int t = contentType.indexOf("charset=");
				if (t >= 0) {
					encoding = contentType.substring(t + 8);
					contentType = contentType.substring(0, t);
				} else {
					encoding = "null";
				}
			}

			if (!HttpUtils.isDownloadFileType(HttpUtils.getContentType(contentType), CrawlerConfig.getConfig().getAllowFileType())) {
				HttpClientUtils.closeQuietly(response);
				EntityUtils.consume(entity);
				return null;
			}

			is = entity.getContent();

			String header = response.getStatusLine() + "\n" + response.getFirstHeader("Date") + "\n" + response.getFirstHeader("Server") + "\n" + response.getFirstHeader("Content-type")
					+ " Last-modified: " + response.getFirstHeader("Last-modified") + "\n" + "Content-length: " + entity.getContentLength() + "\n\n";

			b = writeStreamToByteArray(header, is, CrawlerConfig.getConfig().getMaxFileSize());

			if(b == null) {
				HttpClientUtils.closeQuietly(response);
				EntityUtils.consume(entity);
				return null;
			}
			
			is.close();
			fp = new PageObject(b, contentType, url, -1, encoding, 0, 0);
			
			
		} catch (Exception e) {
			logger.info(e + "\t" + e.getMessage() + "\t" + e.getCause());
		} finally {
			if (entity != null)
				try {
					EntityUtils.consume(entity);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			HttpClientUtils.closeQuietly(response);

		}

		try {
			if (CrawlerConfig.getConfig().getWaitTime() != 0) {
				Thread.sleep(r.nextInt(((int) CrawlerConfig.getConfig().getWaitTime()) / 1000) * 1000);
			}
		} catch (InterruptedException e1) {
			logger.error(e1.getMessage());
		}

		return fp;
	}

	public byte[] downloadAsBytes(String url) {

		InputStream is = null;
		byte[] b;

		if (url.indexOf("http://") < 0) {
			url = "http://" + url;
		}

		HttpGet request = null;
		HttpEntity entity = null;
		CloseableHttpResponse response = null;
		try {
			// Prepare request message
			request = new HttpGet(url);

			// Get response
			response = client.execute(request);

			int status = response.getStatusLine().getStatusCode();
			// System.out.println(status);
			if (status != 200 && status != 302) {
				return null;
			}

			entity = response.getEntity();

			String contentType = entity.getContentType().getValue();
			if (contentType != null) {
				contentType = contentType.toLowerCase();
			}

			is = entity.getContent();

			String header = response.getStatusLine() + "\n" + response.getFirstHeader("Date") + "\n" + response.getFirstHeader("Server") + "\n" + response.getFirstHeader("Content-type")
					+ " Last-modified: " + response.getFirstHeader("Last-modified") + "\n" + "Content-length: " + entity.getContentLength() + "\n\n";

			b = writeStreamToByteArray(header, is, CrawlerConfig.getConfig().getMaxFileSize());

			if (b == null) {
				return null;
			}

			is.close();
			request.releaseConnection();

			return b;
		} catch (Exception e) {
			// e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			if (response != null)
				try {
					response.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			if (request != null) {
				request.releaseConnection();
				request.abort();
				request = null;
			}

			if (entity != null)
				try {
					EntityUtils.consume(entity);
				} catch (IOException e) {
					e.printStackTrace();
				}

			b = null;
			is = null;
		}

		return null;
	}

	private byte[] writeStreamToByteArray(String header, InputStream fis, long limit) {
		try (ByteArrayOutputStream tmp = new ByteArrayOutputStream()) {

			tmp.write(header.getBytes());
			byte[] buffer = new byte[1024];
			int n;

			while ((n = fis.read(buffer)) != -1) {

				tmp.write(buffer, 0, n);
				if (tmp.size() > limit) {
					return null;
				}

				if (Thread.currentThread().isInterrupted())
					break;
			}

			buffer = null;
			buffer = tmp.toByteArray();
			tmp.close();
			return buffer;
		} catch (IOException e) {
			// e.printStackTrace();
			logger.error(e.getMessage());
		}

		return null;
	}

}
