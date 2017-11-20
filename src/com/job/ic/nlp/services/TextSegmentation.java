package com.job.ic.nlp.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import com.job.ic.nlp.LexTo;
import com.job.ic.nlp.SegmentResult;




public class TextSegmentation{
	public static Set<String> lexitron;
	
	public static void createEnviroment(){
	
		try{
			lexitron = new HashSet<String>();
			InputStream is = TextSegmentation.class.getResourceAsStream("/thai.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
		
			while((line = br.readLine()) != null){
				if(line.length() > 3)
				{
					lexitron.add(line);
					lexitron.add(line.replace(" ",""));
				}
				
			};
			br.close();
			is.close();

			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) throws IOException{
		createEnviroment();
		LexTo l = new LexTo(lexitron);
		SegmentResult s;
		
		/*
		try(
			BufferedReader br = new BufferedReader(new FileReader("hop0-extended-verified.txt"));
			BufferedWriter bw = new BufferedWriter(new FileWriter("hop0-extended-verified-new.txt"));		
		){
			String tmp;
			String url;
			while((tmp = br.readLine()) != null){
				url = tmp.split("\t")[0];
				if(url.contains("?"))
					continue;
				
				url = url.replace("http://", "");
				url = url.replace(".", " ").replace("/", " ").replace("_", " ");
				url = StringUtils.removeSpaces(url);
				s = l.matchForSegment(url);
				
				if(s.getAmbiguous().size() + s.getKnown().size() > 0)
					bw.write(tmp + "\ttrue\n");
				else
					bw.write(tmp + "\tfalse\n");
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
//		*/
//		/*
		s= l.matchForSegment("thai");
		System.out.println(s.getAmbiguous().size());
		System.out.println(s.getKnown().size());
		for(String t: s.getKnown())
			System.out.println(t);
//		*/
	}
	
	public static SegmentResult check(String content, LexTo lexto){
		return lexto.matchForSegment(content);
	}

	
	
}
