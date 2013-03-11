package com.lukesegars.heatwave.caches;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import com.lukesegars.heatwave.Contact;

public class ContactDataCache extends DataCache<Long, Contact> {
	private static final String TAG = "ContactDataCache";
	private static ContactDataCache instance = null;
	
	public static ContactDataCache getInstance() {
		if (instance == null) instance = new ContactDataCache();
		return instance;
	}
	
	private ContactDataCache() { super(); }
	
	public ArrayList<Contact> getAllEntries() {
		ArrayList<Contact> contacts = new ArrayList<Contact>();
		Iterator<Entry<Long, Contact>> it = cache.entrySet().iterator();

		while (it.hasNext()) {
			Contact c = it.next().getValue();
			contacts.add(c);
		}
		return contacts;
	}
	
	public void invalidateEntry(long key) {
		invalidateEntry(key, true);
	}
	
	public void invalidateEntry(long key, boolean reload) {
		super.invalidateEntry(key);
		
		// Store the new version.
		if (reload) addEntry(key, Contact.loadByAdrId(key));
	}
	
	public void invalidateAll() { invalidateAll(true); }
	
	public void invalidateAll(boolean reload) {
		super.invalidateAll();
		
		// Reload and store new stuff.
		if (reload) {
			ArrayList<Contact> contacts = Contact.getAll();
			for (Contact c : contacts) addEntry(c.getAdrId(), c);
		}
	}
}
