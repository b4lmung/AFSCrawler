/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.utils;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.log4j.Logger;

import com.job.ic.crawlers.models.CrawlerConfig;

public class HttpUtils {

	public static final int URL_MAX_LENGTH = 2043;

	private static Logger logger = Logger.getLogger(HttpUtils.class);

	private static HashMap<String, HashSet<String>> ip = new HashMap<>();
	public static void main(String args[]) throws MalformedURLException {
		// Core.config = new CrawlerConfig("conf/crawler.conf");
		String s = fixRelativePath("http://www.crateandbarrel.com/kitchen-and-food/top-rated-kitchen-and-food/1", "%7B%7BUrl%7D%7D");
		System.out.println(HttpUtils.getStaticUrl(s));
		// CrawlerConfig.loadConfig("conf/crawler.conf");
		// System.out.println(CrawlerConfig.getConfig().isOnlyStaticURL());
		// String url = "http://www.cpe.ku.ac.th/test/view?gep=alalaa";
		// // String target = "content/41/212/de/teamspeak_viewer.html";
		// // System.out.println("<<" + fixRelativePath(url, target));
		// System.out.println(getStaticUrl(url));
		//
		// System.out.println(isIp("http://192.168.1.33/test.htm"));
		// System.out.println(getStaticUrl("http://www.gconsole.com/test"));
		// System.out.println(getBasePath("http://www.1stopphuket.com/travel_news/tag/cash"));
		// System.out.println(getBasePath("http://www.1stopphuket.com/travel_news/tag/cash/"));
		// System.out.println(HttpUtils.getBasePath("http://www.doitinasia.com/lebanon"));
		// System.out.println(HttpUtils.getBasePath("http://www.doitinasia.com/lebanon/"));

	}

	public static boolean isCommonIp(String input1, String input2){
		HashSet<String> s1 = checkIp(input1);
		HashSet<String> s2 = checkIp(input2);

		if(s1 == null || s2 == null)
			return false;
		
		if(s1.size() > 0){
			s1.clear();
			s2.clear();
			return true;
		}else{
			s2.clear();
			return false;
		}
	}
	
	public static HashSet<String> checkIp(String input) {
		if (input.contains("http"))
			input = HttpUtils.getHost(input);
		
		if(ip.containsKey(input))
			return ip.get(input);
		
		InetAddress[] all;
		
		try {
			all = InetAddress.getAllByName(input);
			HashSet<String> output = new HashSet<>();
			for (InetAddress a : all) {
				output.add(a.getHostAddress());
			}
			ip.put(input, output);
			return output;
		} catch (UnknownHostException e) {
			// e.printStackTrace();
			return null;
		}
	}

	public static String getBasePath(String url) {
		
		if(CrawlerConfig.getConfig().isPageMode())
			return url;
		
		try {
			// url = normalizeURL(url);
			// if(url.endsWith("/"))
			// url = url.substring(0, url.length()-1);

			URL u = new URL(url);
			String path = u.getFile();

			if (path.trim().equals(""))
				return url + "/";

			boolean isFile = false;

			if (path.contains(".") || path.contains("?"))
				isFile = true;

			if (isFile) {
				path = url.substring(0, url.lastIndexOf("/") + 1);
				return path;
			}

			if (path.lastIndexOf("/") >= 0) {
				return url.substring(0, url.lastIndexOf("/") + 1);
			}
			// if (!path.endsWith("/"))
			// url = url.trim() + "/";

			return url.trim().toLowerCase();
		} catch (Exception e) {
			return null;
		}
	}

	public static int countSlash(String url) {
		Pattern p = Pattern.compile("/");
		Matcher m = p.matcher(url);
		int count = 0;
		while (m.find()) {
			count++;
		}

		return count;
	}

	public static int countWord(String word, String url) {
		return StringUtils.countMatches(url, word);
	}

	public static String replaceSlash(String url) {

		if (url.contains("http://")) {
			url = url.replace("http://", "");

			return "http://" + url.replaceAll("//*", "/").trim();
		}

		if (url.contains("https://")) {
			url = url.replace("https://", "");

			return "https://" + url.replaceAll("//*", "/").trim();
		}

		return null;

	}

	public static String getPathForRobots(String url) {
		url = url.replace("http://", "");
		String tmp;
		if (url.contains("/")) {
			url = url.substring(url.indexOf("/"));
			tmp = url.substring(1);
			if (!tmp.contains("/") && tmp.contains("?"))
				return "/";
			else
				return url;
		}
		return "/";
	}

	public static String fixRelativePath(String baseuri, String url) {

		String nurl;
		String nbaseuri;
		if (baseuri == null || url == null) {
			return null;
		}
		baseuri = baseuri.trim();
		if ((url = url.trim()).contains("http://") || (url = url.trim()).contains("https://")) {
			if (url.length() > 4096) {
				return null;
			}
			try {
				return URI.create(url).normalize().toString();
			} catch (Exception e) {
				return null;
			}
		}

		if (url.equals("")) {
			return null;
		}

		// if (url.charAt(url.length() - 1) == '/') {
		// url = url.substring(0, url.lastIndexOf("/"));
		// }
		//
		// if (HttpUtils.countWord(url.replace("/", ""), baseuri.replace("/",
		// "")) > CrawlerConfig.getConfig().getCanonicalCount()) {
		// return null;
		// }

		if (HttpUtils.countWord("&amp;", baseuri) > CrawlerConfig.getConfig().getCanonicalCount()) {
			return null;
		}

		// String tmp = url;
		// if(tmp.contains("?")){
		// tmp = url.substring(0, url.indexOf("?"));
		//
		// if(CrawlerConfig.getConfig().isOnlyStaticURL())
		// url = url.substring(0, url.indexOf("?"));
		//
		// if(tmp.contains("/"))
		// tmp = tmp.substring(0, tmp.lastIndexOf("/"));
		// }
		// int index = 0;
		// index = url.lastIndexOf("/");
		// if (index > 0) {
		// tmp = url.substring(0, index);
		// }
		// if(tmp.contains("../"))
		// tmp = tmp.replace("../", "/");
		// if (tmp != null && HttpUtils.countWord(tmp, baseuri) >
		// CrawlerConfig.getConfig().getCanonicalCount()) {
		// return null;
		// }

		try {
			url = URIEncodeUtils.encodeQuery(url, "UTF-8");
			baseuri = URIEncodeUtils.encodeQuery(baseuri, "UTF-8");

			URI n = URIUtils.resolve(new URI(baseuri), new URI(url));

			String output = n.normalize().toString();

			// check first
			if (output.contains("/")) {
				String[] check = output.split("/");
				for (int i = 1; i < check.length; i++) {
					if (countWord("/" + check[i] + "/", output) > CrawlerConfig.getConfig().getCanonicalCount())
						return null;
				}
			}

			if (output.length() < 4096) {
				return output;
			}
		} catch (Exception u) {
			logger.info("fixRelativePath error : " + url + "\t" + baseuri);
			// u.printStackTrace();
			// empty catch block
		}

		return null;
	}

	public static String encodeURL(String url) {
		HttpGet get;

		try {
			url = url.replaceAll(" ", "%20").replaceAll("<", "%3C").replaceAll(">", "%3E").replaceAll("\\[", "%5B").replaceAll("]", "%5D");
			get = new HttpGet(url);
			return get.getURI().toASCIIString();
		} catch (Exception e) {
			// e.printStackTrace();
			logger.info("encode url error\t" + url);
			return url;
			// return null;
		}
	}

	public static String decodeURL(String url) {
		try {
			return new String(URLCodec.decodeUrl(url.getBytes()));
		} catch (DecoderException e) {
			return url;
		}
	}

	public static boolean isDownloadFileType(String targetFileType, String allowFileTypes) {
		if (allowFileTypes.indexOf(targetFileType) >= 0 || targetFileType == null || "".equals(targetFileType)) {
			return true;
		}

		return false;
	}

	public static String getContentType(String fullContentType) {
		fullContentType += ";";
		return fullContentType.substring(0, fullContentType.indexOf(";"));
	}

	public static String getFileTypeFromURL(String url) {
		if (url.lastIndexOf(".") >= 0) {
			url = url.substring(url.lastIndexOf(".") + 1);
			if (url.indexOf("?") >= 0) {
				url = url.substring(0, url.indexOf("?"));
			}
		}
		return url;
	}

	public static String getStaticUrl(String url, boolean isStatic) {

		boolean foundfile = false;

		url = url.replace("http://", "").replace("https://", "");

		if (url.indexOf("/") > 0) {
			String tmp = url.substring(url.lastIndexOf("/"));
			if (tmp.length() > 1) {
				tmp = tmp.substring(1);
			}

			if (tmp.contains(".") || tmp.contains("?")) {
				foundfile = true;
			}
		}

		if (!foundfile) {
			if (url.charAt(url.length() - 1) == '/')
				url = url.substring(0, url.length() - 1);
		}

		url = "http://" + url;
		if (isStatic) {
			if (url.contains("?")) {
				url = url.substring(0, url.indexOf("?"));
			}
		}

		return (encodeURL(replaceSlash(url).trim()));
	}

	public static String getStaticUrl(String url) {
		try {

			if (url == null)
				return url;

			boolean foundfile = false;

			String b = url;
			url = url.replace("http://", "").replace("https://", "");

			if (url.indexOf("/") > 0) {
				String tmp = url.substring(url.lastIndexOf("/"));
				if (tmp.length() > 1) {
					tmp = tmp.substring(1);
				}

				if (tmp.contains(".") || tmp.contains("?")) {
					foundfile = true;
				}
			}

			// if (!foundfile) {
			// if(url != null && url.length() > 0 && url.charAt(url.length()-1)
			// == '/')
			// url = url.substring(0, url.length()-1);
			// }

			if (url == null || url.length() == 0)
				return null;

			url = "http://" + url;
			if (CrawlerConfig.getConfig() == null || CrawlerConfig.getConfig().isOnlyStaticURL()) {
				if (url.contains("?")) {
					url = url.substring(0, url.indexOf("?"));
				}
			}

			url = encodeURL(replaceSlash(url).trim());
			URL u = new URL(url);

			if (u.getPath().equals(""))
				url = url + "/";
		} catch (Exception e) {
			logger.info("Cannot get static url :" + url);
			return null;
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		// System.out.println("before:" + url + "\tafter :" +
		// encodeURL(replaceSlash(url).trim()) );
		return url;
	}

	public static boolean isIgnoreURL(String[] badpath, String url) {
		for (int i = 0; i < badpath.length; i++) {
			if (badpath[i].equals(url) || url.indexOf(badpath[i]) >= 0) {
				return false;
			}
		}
		return true;
	}

	public static String getHost(String url) {
		
		if(!url.contains("http://"))
			url = "http://" + url;
		
		
		try {
			URL u = new URL(url);
			if (u.getHost() != null) {
				return u.getHost().toLowerCase();
			}
		} catch (Exception e) {
			return null;
		}

		return null;
	}

	public static String getDomain(String url) {
		String hostname = getHost(url);
		if (hostname == null)
			return null;

		if (hostname.contains("."))
			return hostname.substring(hostname.lastIndexOf(".") + 1).toUpperCase();
		return null;
	}

	public static String getExtension(String url) {
		try {
			URL u = new URL(url);
			String tmp = u.getFile();
			if (tmp.contains(".")) {
				tmp = tmp.substring(tmp.indexOf(".") + 1, tmp.length());
				return tmp;

			}

		} catch (MalformedURLException e) {
			return null;
		}
		return null;
	}

	public static boolean isAvailable(String url) {
		try {
			URL u = new URL(url);
			InetAddress.getAllByName(u.getHost());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isIp(String text) {
		String tmp = HttpUtils.getHost(text);

		if (tmp == null)
			return false;

		Pattern p = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
		Matcher m = p.matcher(tmp);
		return m.find();
	}

}
