package com.lukesegars.heatwave.caches;

import java.util.HashMap;

public class DataCache<K, V> {
	protected HashMap<K, V> cache = new HashMap<K, V>();
	
	public V getEntry(K key) {
		return cache.get(key);
	}
	
	public void addEntry(K key, V value) {
		cache.put(key, value);
	}
	
	public boolean entryExists(K key) {
		return (cache.get(key) != null);
	}
	
	public void invalidateEntry(K key) {
		cache.remove(key);
	}
	
	public void invalidateAll() {
		cache.clear();
	}
	
	public int numEntries() {
		return cache.size();
	}
}
