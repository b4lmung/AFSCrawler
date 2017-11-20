package com.job.ic.nlp.services;
 
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.HirstStOnge;
import edu.cmu.lti.ws4j.impl.JiangConrath;
import edu.cmu.lti.ws4j.impl.LeacockChodorow;
import edu.cmu.lti.ws4j.impl.Lesk;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.Resnik;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
 
public class WordNet {
 
	private static ILexicalDatabase db;
	
	static{
		WS4JConfiguration.getInstance().setMFS(true);
		WS4JConfiguration.getInstance().setCache(true);
		db = new NictWordNet();
	}
	/*
	//available options of metrics
	private static RelatednessCalculator[] rcs = { new HirstStOnge(db),
			new LeacockChodorow(db), new Lesk(db), new WuPalmer(db),
			new Resnik(db), new JiangConrath(db), new Lin(db), new Path(db) };
	*/
	public static double compute(String word1, String word2) {
		if(word1.equals(word2))
			return 1;
		
		double s = new WuPalmer(db).calcRelatednessOfWords(word1, word2);
		return s*s;
	}
 
	public static void main(String[] args) {
		String[] words = {"add", "get", "filter", "remove", "check", "find", "collect", "create"};
 
		for(int i=0; i<words.length-1; i++){
			for(int j=i+1; j<words.length; j++){
				double distance = compute(words[i], words[j]);
				System.out.println(words[i] +" -  " +  words[j] + " = " + distance);
			}
		}
	}
}