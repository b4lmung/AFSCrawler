/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.models.WebsiteSegment;

public class FileUtils {

	/**
	 * Retrieve all files in a specific directory
	 * 
	 * @param path
	 * @return Files object
	 */

	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(FileUtils.class);

	public static void rename(String org, String dest) {

		File o = new File(org);
		o.renameTo(new File(dest));
	}

	public static String readFileContent(String filepath) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filepath)))) {
			String tmp;
			StringBuilder sb = new StringBuilder();
			String[] tmpArr;
			while (br.ready()) {
				tmp = br.readLine();
				sb.append(tmp.trim() + " ");

			}

			return sb.toString();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	public static BufferedReader getBufferedReader(String filepath) throws IOException {
		return new BufferedReader(new FileReader(filepath));
	}

	public static BufferedWriter getBufferedFileWriter(String filepath) throws IOException {
		return new BufferedWriter(new FileWriter(filepath));
	}

	public static BufferedWriter getBufferedFileWriter(String filepath, boolean isAppend) throws IOException {
		return new BufferedWriter(new FileWriter(filepath, isAppend));
	}

	public static String[] readFile(String filepath) {

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filepath)))) {
			String tmp;
			LinkedList<String> ll = new LinkedList<String>();

			String[] tmpArr;
			while (br.ready()) {
				tmp = br.readLine();
				if (tmp == null)
					continue;
				ll.add(tmp.trim());
			}
			tmpArr = new String[ll.size()];
			ll.toArray(tmpArr);
			return tmpArr;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	public static void cleanArffData(String filepath, String header) {

		String[] d = FileUtils.readFile(filepath);
		ArrayList<String> data = new ArrayList<String>();
		data.add(header);

		for (String s : d) {
			if (s.startsWith("@"))
				continue;

			s = s.replace("null", "");

			if (s.trim().length() == 0)
				continue;

			if (s.contains("-----------------"))
				continue;

			if (s.contains("predicted"))
				s = s.substring(0, s.lastIndexOf("predicted"));
			// System.out.println(s.trim());

			if (s.trim().toLowerCase().endsWith("non") || s.trim().toLowerCase().endsWith("thai"))
				data.add(s);

		}

		FileUtils.writeTextFile(filepath, data, false);

	}

	public static ArrayList<String[]> readArffData(String filepath) {

		String[] d = FileUtils.readFile(filepath);
		ArrayList<String[]> data = new ArrayList<String[]>();

		boolean isData = false;
		for (String s : d) {
			if (s.contains("@data")) {
				isData = true;
				continue;
			}

			if (!isData)
				continue;

			if (s.contains("-----------------"))
				continue;

			s = s.replace("null", "");
			data.add(s.split(","));
		}

		return data;

	}

	public static ArrayList<String> readArffDataWithoutSplit(String filepath) {

		String[] d = FileUtils.readFile(filepath);
		ArrayList<String> data = new ArrayList<String>();

		boolean isData = false;
		for (String s : d) {
			if (s.contains("@data")) {
				isData = true;
				continue;
			}

			if (!isData)
				continue;

			if (s.contains("-----------------"))
				continue;

			s = s.replace("null", "");
			data.add(s);
		}

		return data;

	}

	public static byte[] readFileAsBytes(String filepath) {

		try (ByteArrayOutputStream br = new ByteArrayOutputStream()) {
			// LinkedList<String> ll = new LinkedList<String>();

			InputStream is = new FileInputStream(filepath);
			byte[] buffer = new byte[1024];
			int r;

			while ((r = is.read(buffer)) != -1) {
				br.write(buffer, 0, r);
			}

			is.close();
			br.close();

			return br.toByteArray();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	public static String[] readFile(String filepath, String encoding) {

		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filepath), encoding))) {
			String tmp;
			LinkedList<String> ll = new LinkedList<String>();

			String[] tmpArr;
			while (br.ready()) {
				tmp = br.readLine();
				ll.add(tmp.trim());
			}
			tmpArr = new String[ll.size()];
			ll.toArray(tmpArr);
			return tmpArr;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	public static String[] readStream(InputStream input) {

		try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
			String tmp;
			LinkedList<String> ll = new LinkedList<String>();

			String[] tmpArr;
			while (br.ready()) {
				tmp = br.readLine();
				ll.add(tmp.trim());
			}
			tmpArr = new String[ll.size()];
			ll.toArray(tmpArr);
			return tmpArr;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	public static String readStringFromStream(InputStream input) {

		try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
			String tmp;
			String output = "";
			while (br.ready()) {
				tmp = br.readLine();
				output += tmp.trim() + "\n";
			}

			return output;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	public static byte[] readBytesFromStream(InputStream input) {

		try (ByteArrayOutputStream br = new ByteArrayOutputStream()) {
			// LinkedList<String> ll = new LinkedList<String>();
			byte[] buffer = new byte[1024];
			int r;

			while ((r = input.read(buffer)) != -1) {
				br.write(buffer, 0, r);
			}

			br.close();

			return br.toByteArray();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	public static void mkdir(String filePath) {
		File f = new File(filePath);
		f.mkdir();
	}

	public static boolean exists(String filePath) {
		File f = new File(filePath);
		return f.exists();
	}

	public static void deleteFile(String filePath) {
		// System.out.println(exists(filePath));
		if (exists(filePath)) {
			File f = new File(filePath);
			f.delete();
			// System.out.println(f.delete());
		}
	}

	public static void deleteDir(String dirPath) {
		try {
			org.apache.commons.io.FileUtils.deleteDirectory(new File(dirPath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static File[] getAllFile(String path) {

		System.out.println(path);

		if (path != null) {
			File directory = new File(path);
			if (directory.isDirectory()) {
				return directory.listFiles();
			}
		}
		return null;
	}

	public static String[] getAllFileName(String path) {
		if (path != null) {
			File directory = new File(path);
			if (directory.isDirectory()) {
				return directory.list();
			}
		}
		return null;
	}

	public static void copyDirectory(String src, String dest) {
		try {
			org.apache.commons.io.FileUtils.copyDirectory(new File(src), new File(dest));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// if (src.charAt(src.length() - 1) != '/') {
		// src = src + "/";
		// }
		// if (dest.charAt(dest.length() - 1) != '/') {
		// dest = dest + "/";
		// }
		//
		// File f = new File(src);
		// File f2 = new File(dest);
		// if (!f.exists()) {
		// return;
		// }
		//
		// if (!f2.exists()) {
		// f2.mkdir();
		// }
		//
		// if (f.isDirectory() && f2.isDirectory()) {
		// String[] lists = f.list();
		// for (String file : lists) {
		// copyFile(src + file, dest + file);
		// }
		// } else {
		// return;
		// }
	}

	public static void copyFile(String srFile, String dtFile) {

		if (!exists(srFile))
			return;

		File f1 = new File(srFile);
		File f2 = new File(dtFile);

		try (InputStream in = new FileInputStream(f1); OutputStream out = new FileOutputStream(f2);) {
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	public static byte[] convertObjToByte(Object obj) {
		try (ByteArrayOutputStream bao = new ByteArrayOutputStream(); ObjectOutputStream os = new ObjectOutputStream(bao);) {

			os.writeObject(obj);
			os.flush();

			return bao.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		// return null;
	}

	public static Object convertByteToObj(byte[] input) {

		Object output = null;
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(input));) {
			output = ois.readObject();
		} catch (Exception e) {

		}
		return output;
	}

	public static Object deepClone(Object obj) {
		byte[] o = FileUtils.convertObjToByte(obj);
		return FileUtils.convertByteToObj(o);
	}

	public static boolean saveObjFile(Object obj, String filepath) {
		try {
			FileOutputStream fos = new FileOutputStream(filepath);
			ObjectOutputStream os = new ObjectOutputStream(fos);
			os.writeObject(obj);
			os.flush();
			os.close();
			fos.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static Object getResource(String filename) {
		try {
			ObjectInputStream ois = new ObjectInputStream(FileUtils.class.getResourceAsStream(filename));
			Object tmp = ois.readObject();
			ois.close();
			return tmp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String[] readResourceFile(String filename) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(FileUtils.class.getResourceAsStream(filename)))) {
			ArrayList<String> output = new ArrayList<>();
			String tmp;
			while ((tmp = br.readLine()) != null) {
				output.add(tmp);
			}

			String[] o = new String[output.size()];
			output.toArray(o);

			return o;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String readResourceFileAsString(String filename) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(FileUtils.class.getResourceAsStream(filename)))) {

			StringBuffer output = new StringBuffer();
			String tmp;

			while ((tmp = br.readLine()) != null) {
				output.append(tmp + "\n");
				// output.add(tmp);
			}

			// String[] o = new String[output.size()];
			// output.toArray(o);
			return output.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static InputStream getInputStreamFromResource(String filename) {
		return FileUtils.class.getResourceAsStream(filename);
	}

	public static ArrayList<String> readTextFileFromResource(String filename) {
		ArrayList<String> output = new ArrayList<>();
		
		try {

			InputStreamReader reader = new InputStreamReader(FileUtils.class.getResourceAsStream(filename));
			BufferedReader br = new BufferedReader(reader);

			String line;
			while((line = br.readLine())!=null) {
				output.add(line);
			}
			
			br.close();
			reader.close();
		} catch (Exception e) {

		}
		
		return output;
	}

	public static Object getObjFile(String filepath) {

		Object tmp;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath));
			tmp = ois.readObject();
			ois.close();
			return tmp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ArrayList<WebsiteSegment> readSegmentFile(String filePath) {
		ArrayList<WebsiteSegment> sms = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)))) {
			String tmp;
			String ip;
			while (br.ready()) {
				tmp = br.readLine();
				if (tmp.equals("=="))
					continue;

				String[] tt = tmp.split("\t");
				String base = HttpUtils.getBasePath(tt[0]);

				if (base == null)
					continue;

				ip = "other";

				ArrayList<Double> srcRelScores = new ArrayList<>();
				srcRelScores.add(10.0);

				ArrayList<LinksModel> inputs = new ArrayList<>();
				for (String t : tt) {
					inputs.add(new LinksModel(null, t, null, 10));
				}

				try {
					WebsiteSegment w = new WebsiteSegment(base, "", -1, inputs, -1, "", "", HttpUtils.getDomain(tt[0]), ip, "", "", srcRelScores, false);
					// sms.add(new WebsiteSegment(segmentName, sourceSeg, depth, urls,
					// distanceFromThai, linkp, anchorp, urlp, srcDomain, srcCountry, destDomain,
					// destCountry, urlString, anchorString, srcRelScores))
					sms.add(w);
				} catch (Exception e) {

				}

				inputs = null;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return sms;
	}

	// public static ArrayList<String[]> importQueueFromFile(String filepath) {
	// try {
	// BufferedReader br = new BufferedReader(new InputStreamReader(new
	// FileInputStream(filepath)));
	// String tmp;
	// ArrayList<String[]> queue = new ArrayList<String[]>();
	// ArrayList<String> ll = new ArrayList<String>();
	//
	// String[] tmpArr;
	// while (br.ready()) {
	// tmp = br.readLine();
	// if (!tmp.trim().equals("==")) {
	// // String[] urls = tmp.split("\t");
	// // for(String l: urls){
	// ll.add(tmp);
	// // }
	// } else {
	// if (ll.size() > 0) {
	// tmpArr = new String[ll.size()];
	// queue.add(ll.toArray(tmpArr));
	// }
	// ll = null;
	// ll = new ArrayList<String>();
	// }
	//
	// }
	//
	// return queue;
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// return null;
	// }


	public static LinkedList<String[]> importQueueFromFile(String filepath, HashSet<String> limit) {
		LinkedList<String[]> queue = new LinkedList<String[]>();
		LinkedList<String> ll = new LinkedList<String>();

		try {
			for (String line : FileUtils.readFile(filepath)) {
				if(line.equals("=="))
					continue;
				
				String[] urls = line.split("\t");
				String ss = HttpUtils.getBasePath(urls[0]);

				if (ss != null && limit != null && limit.contains(ss.toLowerCase()))
					continue;

				queue.add(urls);
			}
			
			return queue;
		} catch (Exception e) {

		}
		return null;
	}
	// public static void exportQueue(String filepath, ArrayList<String[]> t) {
	// try (BufferedWriter bw = FileUtils.getBufferedFileWriter(filepath)) {
	// for (String[] seg : t) {
	//
	// for (String s : seg) {
	// bw.write(s + "\n");
	// }
	//
	// bw.write("==\n");
	// }
	//
	// } catch (FileNotFoundException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e1) {
	// // TODO Auto-generated catch block
	// e1.printStackTrace();
	// }
	//
	// }

	public static void zipDirectory(String dirPath, String outputPath) throws IOException, IllegalArgumentException {
		// Check that the directory is a directory, and get its contents
		File d = new File(dirPath);
		if (!d.isDirectory())
			throw new IllegalArgumentException("Compress: not a directory:  " + dirPath);
		String[] entries = d.list();
		byte[] buffer = new byte[4096]; // Create a buffer for copying
		int bytes_read;

		// Create a stream to compress data and write it to the zipfile
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputPath));

		// Loop through all entries in the directory
		for (int i = 0; i < entries.length; i++) {
			File f = new File(d, entries[i]);
			if (f.isDirectory())
				continue; // Don't zip sub-directories
			FileInputStream in = new FileInputStream(f); // Stream to read file
			ZipEntry entry = new ZipEntry(f.getPath()); // Make a ZipEntry
			out.putNextEntry(entry); // Store entry
			while ((bytes_read = in.read(buffer)) != -1)
				// Copy bytes
				out.write(buffer, 0, bytes_read);
			in.close(); // Close input stream
		}
		// When we're done with the whole loop, close the output stream
		out.close();
	}

	public static void gzipFile(String filepath) {

		filepath = filepath.replace("http://", "");

		File f = new File(filepath);
		if (!f.exists()) {
			return;
		}

		try {
			FileInputStream fis = new FileInputStream(f);
			FileOutputStream fos = new FileOutputStream(filepath + ".gz");
			GZIPOutputStream gos = new GZIPOutputStream(fos);
			byte[] b = new byte[1024];

			int a;
			while ((a = fis.read(b)) != -1) {
				gos.write(b, 0, a);
			}

			f.delete();
			fis.close();
			gos.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String fixPath(String path) {

		if (path.length() < 0) {
			return path;
		}

		if (path.charAt(path.length() - 1) != '/') {
			path = path + "/";
		}
		return path;
	}

	/* public static FilePointerObj AppendStreamToSFFile(String filepath,
	 * InputStream fis){ synchronized(FileUtil.class){ long start=0,length=0; try {
	 * RandomAccessFile rnd = new RandomAccessFile(filepath,"rw");
	 * rnd.seek(rnd.length()); start = rnd.getFilePointer();
	 * 
	 * byte[] buffer = new byte[1024]; int n;
	 * 
	 * while((n = fis.read(buffer)) != -1 ) rnd.write(buffer, 0, n);
	 * 
	 * length = rnd.getFilePointer()-start; fis.close(); rnd.close(); fis =null; rnd
	 * = null;
	 * 
	 * return new FilePointerObj(start, length);
	 * 
	 * } catch (FileNotFoundException e) { e.printStackTrace(); } catch (IOException
	 * e) { e.printStackTrace(); }
	 * 
	 * return null; } } */

	public static boolean writeSegmentToFrontierFile(String outputPath, ArrayList<WebsiteSegment> data, boolean isAppend) {
		try {
			FileOutputStream writer = new FileOutputStream(outputPath, isAppend);
			for (WebsiteSegment s : data) {
				for (String url : s.getUrls())
					writer.write((url + "\n").getBytes());
				// writer.write("\n".getBytes());
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public static boolean writeTextFile(String outputPath, Collection<String> data, boolean isAppend) {
		try {
			FileOutputStream writer = new FileOutputStream(outputPath, isAppend);
			for (String s : data) {
				writer.write((s + "\n").getBytes());
			}
			writer.flush();
			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public static boolean writeTextFile(String outputPath, String[] data, boolean isAppend) {
		try {
			FileOutputStream writer = new FileOutputStream(outputPath, isAppend);
			for (String s : data)
				writer.write((s + "\n").getBytes());
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public static boolean writeTextFile(String outputPath, String data, boolean isAppend, String charset) {
		try {
			FileOutputStream w = new FileOutputStream(outputPath, isAppend);
			OutputStreamWriter wr = new OutputStreamWriter(w, charset);
			BufferedWriter writer = new BufferedWriter(wr);

			writer.write(data);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public static boolean writeTextFile(String outputPath, CharSequence data, boolean isAppend) {
		try {
			FileOutputStream writer = new FileOutputStream(outputPath, isAppend);
			writer.write(data.toString().getBytes());
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public static boolean writeBytes(String outputPath, byte[] data, boolean isAppend) {
		try {
			FileOutputStream writer = new FileOutputStream(outputPath, isAppend);
			writer.write(data);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public static Properties loadProperties(String fileName) throws Exception {
		Properties props = new Properties();
		File f = new File(fileName);

		if (f.exists()) {
			try {
				InputStream is = new BufferedInputStream(new FileInputStream(f));
				props.load(is);
				is.close();
			} catch (IOException e) {
			}
		} else {
			throw new RuntimeException("Cannot load properties files");
		}

		return props;
	}

	public static void separate(String inputPath, String outputPath, int numberOfLine) throws Exception {
		try {
			FileInputStream fis = new FileInputStream(inputPath);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			File f = new File(outputPath);
			if (!f.exists()) {
				f.mkdir();
			}

			int files = 0, count = 0;
			FileWriter fw = new FileWriter(f.getPath() + "/" + files + ".txt");
			String tmp;
			while ((tmp = br.readLine()) != null) {
				if (count % numberOfLine == 0) {
					fw.close();
					files++;
					fw = new FileWriter(f.getPath() + "/" + files + ".txt");
				}

				fw.write(tmp + "\n");
				count++;
			}
			br.close();
			fis.close();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void convert(String inputPath, String outputPath) {
		try {
			String[] lines = readFile(inputPath);
			Arrays.sort(lines, new Comparator<String>() {

				@Override
				public int compare(String o1, String o2) {
					// TODO Auto-generated method stub
					return o1.compareToIgnoreCase(o2);
				}

			});

			FileWriter fw = new FileWriter(outputPath);
			BufferedWriter bw = new BufferedWriter(fw);
			String host;
			String tmp = null;
			int count = 0;
			for (String s : lines) {
				host = HttpUtils.getHost(s);
				if (host == null)
					continue;

				if (!host.equals(tmp)) {
					count++;
					bw.write("==\n");
				}
				bw.write(s + "\n");
				tmp = host;
			}
			System.out.println(count);

			bw.close();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("FINISHED");
	}

	public static boolean isDirectory(String filename) {
		File f = new File(filename);

		return f.isDirectory();
	}
}