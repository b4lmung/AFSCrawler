package com.job.ic.nlp.services;

import com.job.ic.utils.HttpUtils;
import com.job.ic.utils.StringUtils;

public class URLSegmentation {


	public static void main(String[] args){
		String input = "http://www.1stopchiangmai.com/what_to_see/trekking/doi_inthanon/";
		System.out.println(cleanUrl(input, true));
	}
	public static String cleanUrl(String input, boolean includeHostname) {
		// TODO Auto-generated method stub
		
		if(!includeHostname){
			String host = HttpUtils.getHost(input);
			input = input.replace(host, " ");
		}
		
		
		input = HttpUtils.getStaticUrl(input);
		if(input == null)
			return null;
		input = input.toLowerCase();
		String ext = HttpUtils.getExtension(input);
		if(ext != null)
			input = input.replace(ext, " ");
	
		if(!includeHostname){
			String domain = HttpUtils.getDomain(input);
			if(domain != null)
				input = input.replace(domain, " ");
		}
		
		input = input.replace("http://www", " ").replace("http://", " ");
		input = StringUtils.removeSymbols(input);
		
		String[] rep = {"index", "default", "main", "action"};
		for(String s: rep)
			input = input.replace(s, " ");
		
		input = StringUtils.removeSpaces(input).trim();
		
		return input;
	}

}
