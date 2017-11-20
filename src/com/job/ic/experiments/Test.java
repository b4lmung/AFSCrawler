package com.job.ic.experiments;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.job.ic.utils.FileUtils;

public class Test {

	private static String[] ts = { "タ", "ティ", "トゥ", "テ", "" };
	private static String[] ps = { "パ", "ピ", "プ", "ペ", "ポ" };
	private static String[] ks = { "カ", "キ", "ク", "ケ", "コ" };
	
	
	private static String[] t = { "タ" };
	@SuppressWarnings("unused")
	private static String[] p = { "パ", "プ" };
	@SuppressWarnings("unused")
	private static String[] k = { "カ", "ク" };
	private static String[] rl = { "ラ", "リ", "ル", "レ", "ロ" };
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ArrayList<String> rp = new ArrayList<>();
		HashMap<String, String> rpm = new HashMap<>();
//		タムボン
		
		for(String s: t){
			for (int j = 0; j < 5; j++) {
				rp.add(s+rl[j]);
				rpm.put(s+rl[j], ts[j]);
			}
		}
		
		
		for(String s: ps){
			for (int j = 0; j < 5; j++) {
				rp.add(s+rl[j]);
				rpm.put(s+rl[j], ps[j]);
			}
		}
		
		for(String s: ks){
			for (int j = 0; j < 5; j++) {
				rp.add(s+rl[j]);
				rpm.put(s+rl[j], ks[j]);
			}
		}
		
		k  =null;
		p = null;
//		
//		for (int i = 0; i < 5; i++) {
//			// t sound
//			for (int j = 0; j < 5; j++) {
//				rp.add(t[i]+rl[j]);
//				rpm.put(t[i]+rl[j], t[j]);
//			}
//
//			// p sound
//			for (int j = 0; j < 5; j++) {
//				rp.add(p[i]+rl[j]);
//				rpm.put(p[i]+rl[j], p[j]);
//			}
//
//			//k sound
//			for (int j = 0; j < 5; j++) {
//				rp.add(k[i]+rl[j]);
//				rpm.put(k[i]+rl[j], k[j]);
//			}			
//
//		}

		String[] lines = FileUtils.readFile("thaiwords-ja.txt");

		ArrayList<String> tmp = new ArrayList<>();
		int c = 0;
		for (String l : lines) {
			for (String m : rp) {
				if (l.contains(m)) {
					String before = l;
					String base = rpm.get(m);
					
					if(base.equals(""))
						continue;
					
					l = l.replace(m, base);
					tmp.add(before + "\t" + l);
					c++;
				}
			}
		}
		
		System.out.println(c);
		try(BufferedWriter bw = FileUtils.getBufferedFileWriter("thaiwords-ja-d.txt")){
			for(String s: tmp){
				bw.write(s + "\n");
			}
		}catch(Exception e){
			
		}
	}

}
