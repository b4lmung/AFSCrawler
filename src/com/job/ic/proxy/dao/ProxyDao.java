package com.job.ic.proxy.dao;

import java.util.ArrayList;
import java.util.Iterator;

import com.job.ic.proxy.model.ProxyModel;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

public class ProxyDao {

	// Index Accessor
	public PrimaryIndex<String, ProxyModel> index;

	// Open the indices
	public ProxyDao(EntityStore store) throws DatabaseException {
		// Primary key for Inventory classes
		index = store.getPrimaryIndex(String.class, ProxyModel.class);
	}

	public synchronized void add(ProxyModel model) throws DatabaseException {
		index.put(model);
	}
	
	public ProxyModel getByPrimaryKey(String key) throws DatabaseException {
		return index.get(key);
	}
	
	public long count(){
		return this.index.count();
	}
	
	public ProxyModel[] getAll(){
		
		EntityCursor<ProxyModel> e = this.index.entities();
		Iterator<ProxyModel> i = e.iterator();
		ArrayList<ProxyModel> output = new ArrayList<ProxyModel>();
		while(i.hasNext()){
			output.add(i.next());
		}
		
		e.close();
		if(output.size() > 0){
			ProxyModel[] tmp = new ProxyModel[output.size()];
			return output.toArray(tmp);
		}
		return null;
	}

}
