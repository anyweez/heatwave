package com.lukesegars.heatwave.caches;

public class QueryDataCache extends DataCache<Long, String> {
	private static QueryDataCache instance = null;
	
	public static QueryDataCache getInstance() {
		if (instance == null) instance = new QueryDataCache();
		return instance;
	}
	
	private QueryDataCache() {
		super();
	}
}
