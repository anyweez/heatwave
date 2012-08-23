package com.lukesegars.heatwave;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

public class Contact {
	private static final String TAG = "Contact";
	
	public class Fields {
		protected static final long DEFAULT_CID = -1;
		protected static final int DEFAULT_ADRID = -1;
		protected static final long DEFAULT_TIMESTAMP = 0;
		
		private long cid = DEFAULT_CID;
		private int adrId = DEFAULT_ADRID;
		private String name = null;
		private Wave wave = null;
		
		private long lastCallTimestamp = DEFAULT_TIMESTAMP;
		
		public Fields() {}
		
		// Getters and setters
		public long getCid() { return cid; }
		public void setCid(long c) { cid = c; };
		
		public int getAdrId() { return adrId; }
		public void setAdrId(int a) { adrId = a; }
		
		public String getName() { return name; }
		public void setName(String n) { name = n; }
		
		public Wave getWave() { return wave; }
		public void setWave(Wave w) { wave = w; }
		
		public long getLatestTimestamp() { return lastCallTimestamp; }
		public void setLatestTimestamp(long ts) { lastCallTimestamp = ts; } 
		
		protected void modify(Contact.Fields f) {
			if (f.getCid() != DEFAULT_CID) setCid(f.getCid());
			if (f.getAdrId() != DEFAULT_ADRID) setAdrId(f.getAdrId());
			if (f.getName() != null) setName(f.getName());
			if (f.getWave() != null) setWave(f.getWave());
			if (f.getLatestTimestamp() != DEFAULT_TIMESTAMP) 
				setLatestTimestamp(f.getLatestTimestamp());
		}
	}
	
	private Contact.Fields fields = new Contact.Fields();
	
	private static HeatwaveDatabase database = null;
	private static Context context = null;

	private static void initDb() {
		if (database == null) {
			database = HeatwaveDatabase.getInstance(context);
		}
	}
	
	public static void setContext(Context c) {
		context = c;
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
	public static Contact create(int adrId, Wave w) {
		initDb();
		
		if (database.contactExists(adrId)) {
			Log.i(TAG, "Loading preexisting contact (adr #" + adrId + ")");
			return Contact.loadByAdrId(adrId);
		}
		else {
			Log.i(TAG, "Creating new contact (adr #" + adrId + ")");
			// Create the Contact object.
			Contact c = Contact.skeleton();
		
			Contact.Fields cf = c.new Fields();
			cf.setAdrId(adrId);
			cf.setWave(w);

			// Update the object without updating the database record (which 
			// doesn't exist yet).
			c.modify(cf, false);
			
			// Return contact object.
			return database.addContact(c);
		}
	}
	
	public static void delete(int adrId) {
		database.removeContact(Contact.loadByAdrId(adrId));
	}
	
	/**
	 * Returns an empty contact with no fields filled in.  This method is
	 * used for instantiating new Contact.Field classes or other temporary
	 * uses that don't require database interactions.
	 * 
	 * @return an empty Contact
	 */
	public static Contact skeleton() {
		return new Contact();
	}
	
	public static Contact loadByAdrId(int adrId) {
		initDb();
		
		// Select record from database and return it.
		return database.fetchContact(adrId);
	}
	
	public void modify(Contact.Fields f, boolean updateDb) {
		initDb();
		fields.modify(f);
		
		// Update the database records if requested (default = true).
		if (updateDb) database.updateContact(this);
	}
	
	public void modify(Contact.Fields f) {
		modify(f, true);
	}
	
	////////////////////////////
	/// Private constructors ///
	////////////////////////////
	
	private Contact() {	}
	
	private Contact(Contact.Fields f) {
		fields = f;
	}
	
	///////////////////////////
	/// Getters and setters ///
	///////////////////////////
	
	public long getContactId() {
		return fields.getCid();
	}

	public int getAdrId() {
		return fields.getAdrId();
	}

	public String getName() {
		return fields.getName();
	}
	
	public Wave getWave() {
		return fields.getWave();
	}

	public ContentValues cv() {
		ContentValues cv = new ContentValues();

		cv.put("uid", fields.getAdrId());
		cv.put("wave", (fields.getWave() == null) ? null : fields.getWave().getId());
		cv.put("timestamp", fields.getLatestTimestamp());

		return cv;
	}

	@Override
	public String toString() {
		return fields.getName() + " [adr #" + 
			fields.getAdrId() + "]";
	}

	public String getSubtext() {
		if (fields.getLatestTimestamp() == Contact.Fields.DEFAULT_TIMESTAMP) {
			return "No contact.";
		}
		long lts = fields.getLatestTimestamp();
		
		SimpleDateFormat lastContact = new SimpleDateFormat("MM/dd/yyyy");
		String d = lastContact.format(new Date((long)lts * 1000));
		
		double numDays = Math.floor(
			((System.currentTimeMillis() / 1000.0) - lts) / 86400
			);
		
		return ((int) numDays == 0) ?
			"Last contacted on " + d + " (today)" :
			"Last contacted on " + d + " (" + (int)numDays + " days ago)";
	}
	
	public double getScore() {
		// If the timestamp hasn't been set, assume that no contact has been made.
		// We'll make this a top priority item.
		if (fields.getLatestTimestamp() == fields.DEFAULT_TIMESTAMP) return 10.0;

		if (fields.getWave() != null) {
			long currentTime = System.currentTimeMillis() / 1000;

			// FRACTION will always be >= 0
			double fraction = (currentTime - fields.getLatestTimestamp())
					/ (fields.getWave().getWaveLength() * 1.0);
			double score = Math.round(fraction * 100) / 10.0;

			return (score <= 10.0) ? score : 10.0;
		// If the user isn't part of a wave, their score will always be zero.
		} else {
			return 0.0;
		}
	}
}
