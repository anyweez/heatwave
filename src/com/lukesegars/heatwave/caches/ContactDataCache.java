package com.lukesegars.heatwave.caches;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import android.util.Log;

import com.lukesegars.heatwave.Contact;
import com.lukesegars.heatwave.HeatwaveDatabase;
import com.lukesegars.heatwave.Wave;

public class ContactDataCache extends DataCache<Long, Contact> {
	private static final String TAG = "ContactDataCache";
	private static ContactDataCache instance = null;
	
	private HeatwaveDatabase db = HeatwaveDatabase.getInstance();
	
	public static ContactDataCache getInstance() {
		if (instance == null) instance = new ContactDataCache();
		return instance;
	}
	
	private ContactDataCache() { super(); }
	
	public ArrayList<Contact> getAllEntries() {
		ArrayList<Contact> contacts = new ArrayList<Contact>();
		Iterator<Entry<Long, Contact>> it = cache.entrySet().iterator();
		
		while (it.hasNext()) {
			contacts.add(it.next().getValue());
		}
		return contacts;
	}
	
	public void invalidateEntry(long key) {
		invalidateEntry(key, true);
	}
	
	public void invalidateEntry(long key, boolean reload) {
		super.invalidateEntry(key);
		
		// Store the new version.
		if (reload) addEntry(key, db.fetchContact(key));
	}
	
	public void invalidateAll() { invalidateAll(true); }
	
	public void invalidateAll(boolean reload) {
		super.invalidateAll();
		
		// Reload and store new stuff.
		if (reload) {
			ArrayList<Contact> contacts = db.fetchContacts();
			for (Contact c : contacts) {
				addEntry(c.getAdrId(), c);
			}
		}
	}
	
	public void invalidateByWave(Wave wave) { invalidateByWave(wave, true); }
	
	public void invalidateByWave(Wave wave, boolean reload) {
		Iterator<Entry<Long, Contact>> it = cache.entrySet().iterator();
		
		ArrayList<Long> removed = new ArrayList<Long>();
		// Invalidate those that are in the provided wave.
		while (it.hasNext()) {
			Entry<Long, Contact> entry = it.next();

			if (entry.getValue().getWave() == wave) {
				Log.i(TAG, "Invalidated user: " + entry.getValue().getName());
				invalidateEntry(entry.getKey());
				removed.add(entry.getValue().getAdrId());
			}
		}
		
		if (reload) {
			// Re-add those that are supposed to be in the wave.
			for (Long adrId : removed) {
				Contact c = db.fetchContact(adrId);
				if (c != null) {
					Log.i(TAG, "Adding user: " + c.getName());
					addEntry(c.getAdrId(), c);
				}
			}
		}
	}
}
