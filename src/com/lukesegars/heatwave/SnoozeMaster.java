package com.lukesegars.heatwave;

import java.util.HashMap;

import android.content.Context;
import android.util.Log;

public class SnoozeMaster {
	// Singleton instance
	private static SnoozeMaster instance = null;
	private static HeatwaveDatabase database = null;
	
	private HashMap<Long, Long> snoozeLog = null;
	
	/**
	 * Load the snooze log.
	 */
	private SnoozeMaster(Context c) {
		database = HeatwaveDatabase.getInstance();
		refresh();
	}
	
	public static SnoozeMaster getInstance(Context c) {
		if (instance == null) instance = new SnoozeMaster(c);
		return instance;
	}
	
	public void addSnooze(Contact c) {
		addSnooze(c, 0);
	}
	
	public void addSnooze(Contact c, long timestamp) {
		database.addSnoozeRecord(c, timestamp);
		
		// Update the local record as well so we don't need to query
		// the database again.
		snoozeLog.put(c.getAdrId(), timestamp);
		c.resetTimestamp();
	}
	
	public long latestSnooze(Contact c) {
		// If a record exists, return it.
		if (snoozeLog.containsKey(c.getAdrId())) {
			return snoozeLog.get(c.getAdrId());
		}
		// If it doesn't, return DEFAULT_TIMESTAMPE so the call log results won't be disrupted.
		else return Contact.Fields.DEFAULT_TIMESTAMP;
	}
	
	private void refresh() {
		snoozeLog = database.getSnoozeLog();
	}
}
