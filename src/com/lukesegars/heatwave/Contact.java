package com.lukesegars.heatwave;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.util.Log;

public class Contact {
	private int contact_id;
	private int adr_user_id;
	private String name;
	private Wave wave;

	private int lastCallTimestamp;

	public Contact(String n, Wave w, int id) {
		name = n;
		wave = w;
		adr_user_id = id;

		lastCallTimestamp = -1;
	}
	
	public Contact(String n, Wave w, int aid, int hwid) {
		name = n;
		wave = w;
		adr_user_id = aid;
		contact_id = hwid;
		
		lastCallTimestamp = -1;
	}
	
	public int getContactId() {
		return contact_id;
	}

	public int getAdrId() {
		return adr_user_id;
	}

	public String getName() {
		return name;
	}
	
	public Wave getWave() {
		return wave;
	}

	public void setLastCallTimestamp(int ts) {
		lastCallTimestamp = ts;
	}
	
	public void setWave(Wave w) {
		wave = w;
	}

	public ContentValues cv() {
		ContentValues cv = new ContentValues();

		cv.put("uid", adr_user_id);
		cv.put("wave", (wave == null) ? null : wave.getId());

		return cv;
	}

	@Override
	public String toString() {
		return name + " [adr #" + adr_user_id + "]";
	}

	public String getSubtext() {
		if (lastCallTimestamp == -1) {
			return "No contact.";
		}
		
		SimpleDateFormat lastContact = new SimpleDateFormat("MM/dd/yyyy");
		String d = lastContact.format(new Date((long)lastCallTimestamp * 1000));
		
		double numDays = Math.floor(
			((System.currentTimeMillis() / 1000.0) - lastCallTimestamp) / 86400
			);
		if ((int)numDays == 0) {
			return "Last contacted on " + d + " (today)";			
		}
		else {
			return "Last contacted on " + d + " (" + (int)numDays + " days ago)";
		}
	}
	
	public double getScore() {
		if (lastCallTimestamp == -1) {
			Log.i("Contact", "Timestamp not set.");
			return 0.0;
		}

		if (wave != null) {
			long currentTime = System.currentTimeMillis() / 1000;

			// FRACTION will always be >= 0
			double fraction = (currentTime - lastCallTimestamp)
					/ (wave.getWaveLength() * 1.0);
			double score = Math.round(fraction * 100) / 10.0;

			return (score <= 10.0) ? score : 10.0;
		} else {
			return 0.0;
		}
	}
}
