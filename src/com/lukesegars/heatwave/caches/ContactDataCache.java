package com.lukesegars.heatwave.caches;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import com.lukesegars.heatwave.Contact;

public class ContactDataCache extends DataCache<Long, Contact> {
	private static ContactDataCache instance = null;
	
	public static ContactDataCache getInstance() {
		if (instance == null) instance = new ContactDataCache();
		return instance;
	}
	
	private ContactDataCache() {
		super();
	}
	
	public ArrayList<Contact> getAllEntries() {
		ArrayList<Contact> contacts = new ArrayList<Contact>();
		Iterator<Entry<Long, Contact>> it = cache.entrySet().iterator();
		
		while (it.hasNext()) {
			contacts.add(it.next().getValue());
		}
		return contacts;
	}
}
