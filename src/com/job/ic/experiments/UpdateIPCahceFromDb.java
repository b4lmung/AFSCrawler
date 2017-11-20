package com.job.ic.experiments;

import java.util.concurrent.ConcurrentHashMap;

import net.sf.javainetlocator.InetAddressLocator;

import com.job.ic.crawlers.daos.ResultDAO;
import com.job.ic.crawlers.daos.ResultDb;
import com.job.ic.crawlers.models.ResultModel;
import com.job.ic.utils.HttpUtils;

public class UpdateIPCahceFromDb {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String dbPrefix = "db-test-diving/";
		ConcurrentHashMap<String, String> c = new ConcurrentHashMap<String, String>();
		for(int i=0; i<=4; i++){
			ResultDb.createEnvironment(dbPrefix + "db-" + i);
			ResultDAO rd = ResultDb.getResultDAO();
			
			ResultModel[] rds = rd.getAll();
			for(int j=0; j<rds.length; j++){
				String host = HttpUtils.getHost(rds[j].getSegmentName());
				String country = "other";
				
				
				if(rds[j].getIpCountry().equals("other")){
					country = InetAddressLocator.getCountry(host, true);
				}else{
					country = rds[j].getIpCountry();
				}
				
				c.put(host, country);
				
				if(j%100 == 0)
					System.out.println(j + "/" + rds.length);
			}
			

			ResultDb.close();
		}
		
		InetAddressLocator.updateAndExportCacheTable(c, "ip_cache");
	}

}
