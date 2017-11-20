/**
 * @author Mahoro Andou
 */
package net.mikelab.ic.crawlers;

import java.util.Hashtable;

public class HashDb {

	/**
	 * Hash database variable each hash store the downloaded url's string and depth (distance from
	 * source thai webpages)
	 */
	private Hashtable<String, Integer> db;

	/**
	 * Constructor of class, create Hashtable<String, Integer> db instances
	 */
	public HashDb() {
		db = new Hashtable<>();
	}

	/**
	 * 
	 * @param url
	 *            url's string
	 * @param depth
	 *            distance from source thai webpages
	 * @return true if successful
	 */
	public boolean addUrl(String url, int depth) {
		try {
			db.put(url, depth);
		} catch (Exception e) {
		}
		return true;
	}

	/**
	 * Use for check if the input url is contained in the hash database
	 * 
	 * @param url
	 *            Url's string
	 * @return true if found in database, else return false
	 */
	public boolean containUrl(String url) {
		try {
			return db.containsKey(url);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return true;
	}

	/**
	 * Use for get the size of the hash database
	 * 
	 * @return number of elements in hash database
	 */
	public int getSize() {
		return db.size();
	}

	/**
	 * Use for get the hash database object
	 * 
	 * @return the db object
	 */
	public Hashtable<String, Integer> getObject() {
		return this.db;
	}

	public void clear() {
		this.db.clear();
	}
}
