package com.job.ic.crawlers;

import java.util.ArrayList;

import com.job.ic.crawlers.models.WebsiteSegment;
import com.job.ic.utils.FileUtils;
import com.job.ic.utils.SegmentExtractor;

public class Separate {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		ArrayList<WebsiteSegment> tmp = FileUtils.readSegmentFile("dest-from-3.txt");
		
		int target = 4;
		
		int size = tmp.size();
		int limit = size/target + 1;
		System.out.println(limit);
		
		ArrayList<WebsiteSegment> t = new ArrayList<WebsiteSegment>();
//		for(WebsiteSegment s: tmp){
//			for(String url : s.getUrls()){
//				if(ud.containUrl(url))
//					continue;
//				
//				t.add(url);
//			}
//		}
//		FileUtils.writeTextFile("tmp.txt", t, false);
//		SegmentExtractor.extractSegment("tmp.txt", "dest-from-3-n.txt");
		int count = 0;
		int loop = 0;
		while(true){
			WebsiteSegment s = tmp.remove(0);
			t.add(s);
			
			count++;
			if(count > limit)
			{
				System.out.println(">>>" + t.size());
				FileUtils.writeSegmentToFrontierFile("tmp.txt", t, false);
				SegmentExtractor.extractSegment("tmp.txt", "dest-from-3-" + (++loop) + ".txt");
				t.clear();
				count = 0;
			}
			
			if(tmp.size() == 0){
				if(count < limit && t.size() > 0){
					System.out.println(">>>" + t.size());
					FileUtils.writeSegmentToFrontierFile("tmp.txt", t, false);
					SegmentExtractor.extractSegment("tmp.txt", "dest-from-4-" + (++loop) + ".txt");
					t.clear();
				}
				break;
			}
			
		}
		
		
		System.out.println("fin");

		
	}

}
