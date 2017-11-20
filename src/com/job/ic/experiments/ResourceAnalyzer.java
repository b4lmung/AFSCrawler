package com.job.ic.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;

import org.apache.log4j.Logger;

import com.job.ic.crawlers.daos.ResultDAO;
import com.job.ic.crawlers.daos.SegmentGraphDAO;
import com.job.ic.crawlers.models.ResultModel;
import com.job.ic.crawlers.models.SegmentGraphModel;
import com.job.ic.extraction.FeaturesExtraction;
import com.job.ic.utils.FileUtils;
import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

public class ResourceAnalyzer {

	public static void main(String[] args) throws Exception {
		// mergeResult(null);
		// testTestSet(null, Double.parseDouble(args[0]));

		// plotHvCv(null);
		// mergeHvCvResult(null);

		/*
		 * String[] seg = { "db-data1-back-hop2", "db-data1-back-hop1", "db-data1-hop0",
		 * "db-data1-hop1", "db-data1-hop2", }; // String[] seg = { "db-back-hop2", "db-back-hop1",
		 * "db-hop0", "db-hop1", "db-hop2" };
		 * 
		 * int start = Integer.parseInt(args[0]);
		 * 
		 * for(double percentThai = 0; percentThai <= 0.5; percentThai+=0.125){
		 * System.out.println(percentThai); ArrayList<String> queue = new ArrayList<>(); for(int
		 * i=start; i<seg.length; i++){ plotHvCv(seg[i], queue, percentThai, percentThai + "-" + i +
		 * ".txt"); } }
		 */
	}


	public static void plotHvCv(String sourceDbPath, ArrayList<String> queue, double percentThai, String outputPath) throws Exception {
		Locale.setDefault(Locale.US);
		System.setProperty("file.encoding", "UTF-8");

		EnvironmentConfig config = new EnvironmentConfig();
		StoreConfig sc = new StoreConfig();
		sc.setTransactional(true);
		sc.setAllowCreate(true);
		config.setAllowCreate(true);
		config.setTransactional(true);

		CheckpointConfig cfg = new CheckpointConfig();
		cfg.setForce(true);
		cfg.setMinutes(1);

		Environment env = new Environment(new File(sourceDbPath), config);
		EntityStore sourceStore = new EntityStore(env, "result", sc);

		ResultDAO rd = new ResultDAO(sourceStore);
		SegmentGraphDAO sd = new SegmentGraphDAO(sourceStore);
		String tmp;
		ResultModel rm;
		ArrayList<SegmentGraphModel> sm;
		ArrayList<ResultModel> arm;

		HashSet<String> dest = new HashSet<>();

		BufferedWriter bw = FileUtils.getBufferedFileWriter(outputPath);

		int count = 0;
		int size = 500;
		if (queue.size() == 0) {
			while (count <= rd.getSize()) {
				arm = rd.getRange(count, count + size);
				for (ResultModel s : arm) {
					if (ResultDAO.calcPercentRel(s) >= percentThai) {
						sm = sd.getDests(s.getSegmentName());

						if (sm == null) {
							sm = sd.getDests(s.getSegmentName().toLowerCase());
							if (sm == null)
								continue;
						}

						while (sm.size() > 0) {
							dest.add(sm.remove(0).getDestSeg());
						}

					}
					bw.write(String.format("%s\t%d\t%d\t%d\t%.2f\n", s.getSegmentName(), s.getRelevantPage(), s.getIrrelevantPage(), s.getRelevantPage() + s.getIrrelevantPage(),
							ResultDAO.calcPercentRel(s)));
				}
				count += size;
			}

		} else {

			while (queue.size() > 0) {
				tmp = queue.remove(0);
				if ((rm = rd.getData(tmp)) != null && ResultDAO.calcPercentRel(rm) >= percentThai) {
					sm = sd.getDests(rm.getSegmentName());

					if (sm == null) {
						sm = sd.getDests(rm.getSegmentName().toLowerCase());
						if (sm == null)
							continue;
					}
					while (sm.size() > 0) {
						dest.add(sm.remove(0).getDestSeg());
					}
					bw.write(String.format("%s\t%d\t%d\t%d\t%.2f\n", rm.getSegmentName(), rm.getRelevantPage(), rm.getIrrelevantPage(), rm.getRelevantPage() + rm.getIrrelevantPage(),
							ResultDAO.calcPercentRel(rm)));
				}
			}
		}

		bw.close();

		queue.addAll(dest);

		sourceStore.close();
		env.close();

	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void mergeHvCvResult(String[] args) throws IOException {
		HashSet<String> db = new HashSet<String>();
		String base = "D:/current-research/dataset1/hv/375/";

		String output = base.concat("all.txt");
		PrintWriter pw = new PrintWriter(output);
		String[] f1;
		ArrayList<ResultModel> rms;
		String[] tmp;

		long thai = 0;
		long non = 0;
		long totalThai = 403971;
		double hv;
		int threshold = 5;
		int count = 0;
		for (File f : new File(base).listFiles()) {
			if (f.getName().equals("all.txt") || f.getName().equals("result.txt"))
				continue;

			f1 = FileUtils.readFile(f.getAbsolutePath());
			rms = new ArrayList<ResultModel>();

			for (String s : f1) {
				tmp = s.toLowerCase().split("\t");
				db.add(tmp[0]);

				if (Integer.parseInt(tmp[1]) + Integer.parseInt(tmp[2]) < threshold)
					continue;

				// if (tmp.length != 3)
				rms.add(new ResultModel(tmp[0], Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2]), 0, null, null));

			}

			Collections.sort(rms, new Comparator<ResultModel>() {
				@Override
				public int compare(ResultModel o1, ResultModel o2) {
					double r1 = ResultDAO.calcPercentRel(o1);
					double r2 = ResultDAO.calcPercentRel(o2);
					if (r1 > r2)
						return -1;
					else if (r2 > r1)
						return 1;

					// TODO Auto-generated method stub
					return 0;
				}

			});
			for (ResultModel m : rms) {
				thai += m.getRelevantPage();
				non += m.getIrrelevantPage();
				hv = 1.0 * thai / (thai + non);
				pw.write(String.format("%s\t%.3f\t%.3f\t%d\t%d\t%d\n", m.getSegmentName(), hv, 1.0 * thai / totalThai, thai, non, thai + non));
			}

			rms.clear();
		}

		pw.flush();
		pw.close();

		String[] lines = FileUtils.readFile(base + "all.txt");
		String[] test;
		long total;
		long latest = -1;
		BufferedWriter bw = FileUtils.getBufferedFileWriter(base + "result.txt");
		for (String s : lines) {
			test = s.split("\t");
			total = Long.parseLong(test[test.length - 1]);
			if (latest < total / 10000) {
				latest = total / 10000;
				bw.write(s + "\n");
			}
		}
		bw.close();

		System.out.println(thai);
	}

}
