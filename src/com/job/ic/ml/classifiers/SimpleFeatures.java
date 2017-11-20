package com.job.ic.ml.classifiers;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import com.job.ic.utils.FileUtils;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.tokenizers.Tokenizer;

public class SimpleFeatures {

	public static void main(String[] args) throws Exception {

		buildDict("training/training-estate.arff");
		convert("test.arff", "dest.arff");

	}

	@SuppressWarnings("unchecked")
	public static void convert(String source, String dest) {

		HashMap<String, Integer> mapAnchor = (HashMap<String, Integer>) FileUtils
				.getObjFile("dict.bin");

		Tokenizer a = new JapaneseTokenizer();
		try {

			boolean start = false;

			String tmp, key;
			int[] anchor;

			String[] in;
			BufferedWriter bw = FileUtils.getBufferedFileWriter(dest);

			// header
			ArrayList<String> r = new ArrayList<String>(Arrays.asList(FileUtils
					.readResourceFile("/header-cf.txt")));
			for (int i = 0; i <= 6; i++) {
				bw.write(r.get(i) + "\n");
			}

			for (int i = 0; i < mapAnchor.size(); i++) {
				bw.write("@attribute anchor" + i + " numeric\n");
			}

			bw.write(r.get(9) + "\n@data\n");

			// 7,9
			String[] ins = FileUtils.readFile(source);

			for (int i = 0; i < ins.length; i++) {

				if (ins[i].contains("@data")) {
					start = true;
					continue;
				}

				if (!start)
					continue;

				anchor = null;

				anchor = new int[mapAnchor.size()];

				in = ins[i].split(",");
				for (int j = 0; j < 6; j++) {
					bw.write(in[j] + ",");
				}

				// ////////// anchor ///////////
				tmp = in[6];

				// anchor
				a.tokenize(tmp);

				while (a.hasMoreElements()) {
					key = a.nextElement().toString();
					if (mapAnchor.containsKey(key)) {
						if(mapAnchor.size() > mapAnchor.get(key))
							anchor[mapAnchor.get(key)]++;
					}
				}

				for (int j = 0; j < anchor.length; j++) {
				
//					bw.write(anchor[j] + ",");
					
					if(anchor[j] >=1)
						bw.write("1,");
					else
						bw.write("0,");
						
				}

				// //////// class ////////
				bw.write(in[8] + "\n");
			}

			bw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void buildDict(String path) {
		HashMap<String, Integer> map = new HashMap<>();
		HashSet<String> keys = new HashSet<String>();
		String s;

		DataSource ds;
		try {
			ds = new DataSource(path);

			MyTextTokenizer t = new MyTextTokenizer();
			Instances ins = ds.getDataSet();
			for (int i = 0; i < ins.size(); i++) {
				s = ins.get(i).stringValue(6);
				t.tokenize(s);
				while (t.hasMoreElements()) {
					String word = t.nextElement().toString();
					if (map.containsKey(word)) {
						int freq = map.get(word).intValue() + 1;
						map.put(word, freq);
					} else {
						map.put(word, 1);
					}
					keys.add(word);
				}

			}

			for (String key : keys) {
				if (map.get(key) < 3) {
					map.remove(key);
				}
			}
			System.out.println(map.size());
			FileUtils.saveObjFile(map, "dict.bin");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}