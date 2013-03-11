package com.lukesegars.heatwave;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.lukesegars.heatwave.caches.ContactDataCache;
import com.lukesegars.heatwave.caches.WaveDataCache;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

public class Contact {
	private static final String TAG = "Contact";
	private static ContactDataCache ctxCache = ContactDataCache.getInstance();
	private static WaveDataCache waveCache = WaveDataCache.getInstance();
	
	/**
	 * The Fields subclass is instantiated once for each Contact object.  It
	 * contains all of the information that is relevant to a particular Contact.
	 * It (1) creates a layer of abstraction over the fact that the fields
	 * exist in different databases and (2) lets us run some of the more time-
	 * consuming queries just-in-time.
	 */
	public class Fields {
		protected static final long DEFAULT_CID = -1;
		protected static final long DEFAULT_ADRID = -1;
		public static final long DEFAULT_TIMESTAMP = 0;
		protected static final long DEFAULT_CALL_ID = -1;
		protected static final String DEFAULT_NAME = "";
		protected static final String DEFAULT_PHONE_NUM = "";
		protected final long DEFAULT_WAVEID = -1;
		public final static long NO_WAVE = -2;
		
		private long cid = DEFAULT_CID;
		private long adrId = DEFAULT_ADRID;
		private String name = DEFAULT_NAME;
		private long waveId = DEFAULT_WAVEID;
		private String phoneNum = DEFAULT_PHONE_NUM;
		
		private long lastCallTimestamp = DEFAULT_TIMESTAMP;
		private long lastCallId = DEFAULT_CALL_ID;
		
		private WaveDataCache wCache = WaveDataCache.getInstance();
		
		public Fields() {}
		
		// Getters and setters
		public boolean hasCid() { return cid != DEFAULT_CID; }
		public long getCid() { return cid; }
		public void setCid(long c) { cid = c; };
		
		public boolean hasAdrId() { return adrId != DEFAULT_ADRID; }
		public long getAdrId() { return adrId; }
		public void setAdrId(long a) { adrId = a; }
		
		public boolean hasName() { return name != DEFAULT_NAME; }
		public String getName() { return name; }
		public void setName(String n) { name = n; }
		
		/**
		 * In order for hasWave() to return true, wave must not be NULL
		 * and not be equal to the skeleton Wave that was initialized as
		 * DEFAULT_WAVE.
		 * 
		 * @return
		 */
		public boolean hasWave() { return waveCache.getEntry(getWaveId()) != null; }
		public long getWaveId() { return waveId; }
		public Wave getWave() { return wCache.getEntry(waveId); }
		public void setWave(Wave w) { waveId = (w != null) ? w.getId() : NO_WAVE; 	}
		public void setWaveId(long w) { waveId = w; }
		
		public boolean hasPhoneNum() { return phoneNum != DEFAULT_PHONE_NUM; }
		public String getPhoneNum() { 
			if (!hasPhoneNum()) {
				try { setPhoneNum(database.getPhoneForContact(this)); }
				catch (Exception e) {
					if (!hasAdrId()) {
						Log.w(TAG, "No adrId set for user when looking up phone num.");
					}
					else {
						Log.w(TAG, "No phone # found for Android user #" + getAdrId());
					}
				}
			}

			return phoneNum; 
		}
		public void setPhoneNum(String pn) { phoneNum = pn; }
		
		public void resetTimestamp() { lastCallTimestamp = DEFAULT_TIMESTAMP; }
		public boolean hasTimeStamp() { return lastCallTimestamp != DEFAULT_TIMESTAMP; }
		public long getLatestTimestamp() {
			if (!hasTimeStamp()) { lastCallTimestamp = database.updateTimestamp(this); }
			return lastCallTimestamp;
		}
		
		public boolean hasLastCallId() { return lastCallId != DEFAULT_CALL_ID; }
		public long getLastCallId() { return lastCallId; }
		public void setLastCallId(long lcid) { lastCallId = lcid; }
		
		protected void modify(Contact.Fields f) {
			if (f.hasCid()) setCid(f.getCid());
			if (f.hasAdrId()) setAdrId(f.getAdrId());
			if (f.hasName()) setName(f.getName());
			if (f.hasPhoneNum()) setPhoneNum(f.getPhoneNum());
			if (f.hasLastCallId()) setLastCallId(f.getLastCallId());
			
			// If the wave is NO_WAVE or an actual wave value, update it.
			if (f.getWaveId() != DEFAULT_WAVEID) setWaveId(f.getWaveId());
		}
	}
	
	private Contact.Fields fields = new Contact.Fields();
	
	private static HeatwaveDatabase database = null;
	private static Context context = null;

	public static void setContext(Context c) {
		context = c;
		database = HeatwaveDatabase.getInstance();
	}
	
	/**
	 * Creates a new Contact object and adds a new row in the database
	 * to make the object persistent.  If a Contact already exists for
	 * the adrId provided then that record is returned instead.
	 * 
	 * @param adrId
	 * @param w
	 * @return
	 */
	public static Contact create(long adrId, Wave w) {
		if (ctxCache.entryExists(adrId)) return ctxCache.getEntry(adrId);
		else {
			Log.i(TAG, "Loading contact from database.");
			// Create the Contact object.
			Contact c = Contact.skeleton();

			Contact.Fields cf = c.new Fields();
			cf.setAdrId(adrId);
			cf.setWave(w);

			// Update the object without updating the database record (which 
			// doesn't exist yet).
			c.modify(cf, false);
			
			// Return contact object.
			Contact contact = database.addContact(c);
			
			ctxCache.invalidateEntry(c.getAdrId(), false);
			ctxCache.addEntry(contact.getAdrId(), contact);
			
			return contact;
		}
	}
	
	public static void delete(long adrId) {
		ctxCache.invalidateEntry(adrId, false);
		database.removeContact(adrId);
	}
	
	/**
	 * Returns an empty contact with no fields filled in.  This method is
	 * used for instantiating new Contact.Field classes or other temporary
	 * uses that don't require database interactions.
	 * 
	 * @return an empty Contact
	 */
	public static Contact skeleton() { return new Contact(); }
	
	// TODO: Add caching.
	public static Contact loadByAdrId(long adrId) {
		// Select record from database and return it.
		if (ctxCache.entryExists(adrId)) return ctxCache.getEntry(adrId);
		else {
			Contact c = database.fetchContact(adrId);
			if (c != null) ctxCache.addEntry(adrId, c);
			return c;
		}
	}
	
	public static ArrayList<Contact> getAll() {
		if (ctxCache.numEntries() == 0) {
			Log.i(TAG, "Loading all contacts from database.");
			ArrayList<Contact> contacts = database.fetchContacts();
//			for (Contact c : contacts) {
//			}
		}
		
		return ctxCache.getAllEntries();
	}
	
	public void modify(Contact.Fields f, boolean updateDb) {
		fields.modify(f);
		
		// Update the database records if requested (default = true).
		if (updateDb) {
			database.updateContact(this);
			ctxCache.invalidateEntry(getAdrId());
		}
		else {
			// Is this necessary?  Depending on whether this and the object in the table
			// are the same object.
			ctxCache.invalidateEntry(getAdrId(), false);
			ctxCache.addEntry(getAdrId(), this);
		}
	}
	
	public void modify(Contact.Fields f) { modify(f, true); }
	
	public void resetTimestamp() {
		fields.resetTimestamp();
		fields.getLatestTimestamp();
	}
	
	////////////////////////////
	/// Private constructors ///
	////////////////////////////
	
	private Contact() {	 
		fields = new Contact.Fields(); 
		
		if (ctxCache == null) {
			ctxCache = ContactDataCache.getInstance();
			ctxCache.invalidateAll();
		}
	}
	private Contact(Contact.Fields f) { fields = f; }
	
	///////////////////////////
	/// Getters and setters ///
	///////////////////////////
	
	public long 		getContactId() { return fields.getCid(); }
	public long 		getAdrId() { return fields.getAdrId(); }
	public String 		getName() { return fields.getName(); }
	public Wave 		getWave() { return fields.getWave(); }
	public String 		getPhoneNum() { return fields.getPhoneNum(); }
	public boolean 	hasName() { return fields.hasName(); }
	public boolean		hasWave() { return fields.hasWave(); }

	public ContentValues cv() {
		ContentValues cv = new ContentValues();

		cv.put("uid", getAdrId());
		cv.put("wave", (hasWave()) ? getWave().getId() : Fields.NO_WAVE);
		cv.put("lastCallId", fields.getLastCallId());

		return cv;
	}

	@Override
	public String toString() {
		return fields.getName() + " [adr #" + fields.getAdrId() + ", wave '" + fields.getWave() + "', wid=" + fields.getWaveId() + "]";
	}
	
	public long getLatestTimestamp() { 
		return fields.getLatestTimestamp(); 
	}

	public String getSubtext() {
		// TODO: Streamline fetching of latest timestamp.  Currently being done
		// in too many places.
		if (!fields.hasTimeStamp()) return "No contact.";

		long lts = fields.getLatestTimestamp();
		SimpleDateFormat lastContact = new SimpleDateFormat("MM/dd/yyyy");
		String d = lastContact.format(new Date((long)lts * 1000));
		
		// TODO: Create secondsPerPeriod constant to replace hard-coded 86400.
		double numDays = Math.floor(
			((System.currentTimeMillis() / 1000.0) - lts) / 86400
		);
		
		return ((int) numDays == 0) ?
			"Last contacted on " + d + " (< 1 day ago)" :
			"Last contacted on " + d + " (" + (int)numDays + " days ago)";
	}
	
	/**
	 * Computes the score [0.0 - 10.0] measuring how close a particular contact
	 * is to the desired contact window.  If a negative number is returned then
	 * the user does not have membership in a wave and the true score can't be
	 * computed.
	 * 
	 * @return
	 */
	public double getScore() {
		// If the user isn't part of a wave, don't show a score.
		if (!fields.hasWave()) return -1.0;
		
		// If the timestamp hasn't been set, assume that no contact has been made.
		// We'll make this a top priority item.
		SnoozeMaster sm = SnoozeMaster.getInstance(context);
		long snooze = sm.latestSnooze(this);
		long call = fields.getLatestTimestamp();

		// Return max score if there are no snoozes and no calls.
		if (snooze == call && call == Contact.Fields.DEFAULT_TIMESTAMP) return 10.0;
	
		long mostRecent = Math.max(snooze, call);
		long currentTime = System.currentTimeMillis() / 1000;
		
		double fraction = (currentTime - mostRecent)
			/ (getWave().getWaveLength() * 1.0);
		
		// FRACTION will always be >= 0
		if (fraction < 0.0) fraction = 0.0;
		
		double score = Math.round(fraction * 100) / 10.0;
		return Math.min(score, 10.0);
	}
}
