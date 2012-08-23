package com.lukesegars.heatwave;

import java.util.ArrayList;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.Contacts.People;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

// TODO: Make sure nothing crashes if a Contact is deleted from the Android contact list.
public class HeatwaveDatabase {
	private static final String TAG = "HeatwaveDatabase";
	private static final int DURATION_THRESHOLD = 4;

	// SQLite database helper (local class)
	private HeatwaveOpenHelper dbHelper;
	private SQLiteDatabase database;
	private Context context;

	private static HeatwaveDatabase instance;

	/**
	 * This class is responsible for configuring and updating the tables used by
	 * Heatwave. It also initializes database instances to be used by the parent
	 * class.
	 */
	private static class HeatwaveOpenHelper extends SQLiteOpenHelper {
		// TODO: Clean up these constants. Many may not be needed anymore.
		private static final int DATABASE_VERSION = 2;
		private static final String DATABASE_NAME = "heatwave";
		private static final String WAVE_TABLE_NAME = "waves";
		private static final String CONTACTS_TABLE_NAME = "contacts";
//		private static final String EVENTLOG_TABLE_NAME = "eventlog";

		private static final String WAVE_TABLE_CREATE = "CREATE TABLE "
				+ WAVE_TABLE_NAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ // primary key
				"name TEXT, " + // name of the wave
				"wavelength INTEGER, " + // the amount of time between contact (in
										// seconds)
				"lastContact TEXT)";

		private static final String CONTACTS_TABLE_CREATE = "CREATE TABLE "
				+ CONTACTS_TABLE_NAME
				+ " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " + // primary key
				"uid INTEGER, " +   // system-level user ID (Android contact ID)
				"wave INTEGER," +   // foreign key to the wave table. Contacts can
									// only
									// be in one wave at a time.
				"timestamp TEXT)";

//		private static final String EVENTLOG_TABLE_CREATE = "CREATE TABLE "
//				+ EVENTLOG_TABLE_NAME
//				+ " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
//				+ "event_type TEXT, " + "uid INTEGER, " + "timestamp DATETIME)";

		public HeatwaveOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		/**
		 * Creates all of the required tables in the "waves" database. 
		 */
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(WAVE_TABLE_CREATE);
			db.execSQL(CONTACTS_TABLE_CREATE);
//			db.execSQL(EVENTLOG_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + WAVE_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + CONTACTS_TABLE_NAME);
//			db.execSQL("DROP TABLE IF EXISTS " + EVENTLOG_TABLE_NAME);

			onCreate(db);
		}
	} // end the helper class

	private HeatwaveDatabase(Context c) {
		context = c;
	}

	public static HeatwaveDatabase getInstance(Context c) {
		if (instance != null)
			return instance;
		else {
			instance = new HeatwaveDatabase(c);
			instance.open();
			return instance;
		}
	}

	/**
	 * Opens a R/W instance of a database that is stored as a private member of
	 * the class. The open() method returns the calling object so that the
	 * method can be chained with other more interesting methods.
	 * 
	 * This method MUST be called before any queries can be made.
	 * 
	 * @throws SQLException
	 */
	private HeatwaveDatabase open() throws SQLException {
		dbHelper = new HeatwaveOpenHelper(context);
		database = dbHelper.getWritableDatabase();

		return this;
	}

	///////////////////////////////////
	//// Contact-related methods //////
	///////////////////////////////////

	public Contact addContact(Contact newContact) {
		database.insert(HeatwaveOpenHelper.CONTACTS_TABLE_NAME, 
			null, newContact.cv());
		
		return fetchContact(newContact.getAdrId());
	}

	public void addContacts(ArrayList<Contact> contacts) {
		for (Contact c : contacts) {
			addContact(c);
		}
	}
	
	public boolean contactExists(int adrId) {
		Cursor c = database.query(HeatwaveOpenHelper.CONTACTS_TABLE_NAME, 
			new String[] { "_id" }, 
			"uid = ?", 
			new String[] { String.valueOf(adrId) }, 
			null, null, null);
		
		boolean exists = c.getCount() > 0;
		c.close();
		
		return exists;
	}

	public void updateContact(Contact c) {
		database.update(HeatwaveOpenHelper.CONTACTS_TABLE_NAME, 
			c.cv(), 
			"_id = ?", 
			new String[] { String.valueOf(c.getContactId()) });
	}
	
	public ArrayList<Integer> getActiveContactIds() {
		String[] projection = new String[] { "_id" };

		Cursor c = database.query(HeatwaveOpenHelper.CONTACTS_TABLE_NAME,
				projection, null, null, null, null, null);

		ArrayList<Integer> activeContacts = new ArrayList<Integer>();
		c.moveToFirst();

		while (!c.isAfterLast()) {
			activeContacts.add(c.getInt(0));
			c.moveToNext();
		}

		return activeContacts;
	}
	
	public ArrayList<Integer> getActiveContactAdrIds() {
		String[] projection = new String[] { "uid" };

		Cursor c = database.query(HeatwaveOpenHelper.CONTACTS_TABLE_NAME,
				projection, null, null, null, null, null);

		ArrayList<Integer> activeContacts = new ArrayList<Integer>();
		c.moveToFirst();

		while (!c.isAfterLast()) {
			activeContacts.add(c.getInt(0));
			c.moveToNext();
		}

		return activeContacts;		
	}

	public boolean removeContact(Contact target) {
		return (database.delete(HeatwaveOpenHelper.CONTACTS_TABLE_NAME,
				"uid = ?",
				new String[] { String.valueOf(target.getAdrId()) })) > 0;
	}

	public boolean removeContacts(ArrayList<Contact> contacts) {
		boolean errors = false;
		for (Contact c : contacts) {
			if (!removeContact(c))
				errors = true;
		}

		return errors;
	}
	
	// TODO: merge fetchContact and fetchContacts to share a lot of this code.
	public Contact fetchContact(int adrId) {
		// Get the list of contacts from the Heatwave database.
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		qBuilder.setTables(HeatwaveOpenHelper.CONTACTS_TABLE_NAME);

		Contact c = Contact.skeleton();
		Contact.Fields cf = c.new Fields();
		
		// Update the timestamp of the most recent call for this user before
		// requesting that information from the database.
		
		Cursor cursor = qBuilder.query(database, 
			new String[] { "_id", "uid", "wave" }, 
			"uid = ?", 
			new String[] { String.valueOf(adrId) },
			null, null, null);

		cursor.moveToFirst();

		// Return null if the contact doesn't exist.
		if (cursor.isAfterLast()) return null;
		
		cf.setCid(cursor.getInt(0));
		cf.setAdrId(cursor.getInt(1));
		cf.setWave(fetchWave(cursor.getInt(2)));
		cf.setLatestTimestamp(updateTimestamp(adrId));
//		cf.setLatestTimestamp(Integer.parseInt(cursor.getString(3)));
		
		cursor.close();
		///////////////////////////////////////////////////////////////////
		// Get some contact information from the Android contact tables. //
		///////////////////////////////////////////////////////////////////
		
		// Some parameters for the system-wide contact lookups.
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		String[] adr_projection = new String[] { 
			ContactsContract.Contacts._ID,
			ContactsContract.Contacts.DISPLAY_NAME 
		};

		// Query to find the contact's name from the Android contact registry.
		Cursor adrCursor = context.getContentResolver().query(uri,
			adr_projection, "_id = ?", 
			new String[] { String.valueOf(cf.getAdrId()) }, 
			null);
			
		adrCursor.moveToFirst();
		cf.setName(adrCursor.getString(1));
		
		// Clean up.
		adrCursor.close();
		c.modify(cf, false);
		
		return c;
	}
	
	public long updateTimestamp(int adrId) {
		// Get all of the phone numbers for the contact.
		StringBuilder contactQuery = new StringBuilder();
		ArrayList<Long> rawIds = getRawContacts(adrId);
		String[] rawStr = new String[rawIds.size()];
		
		contactQuery.append("(");
		for (int i = 0; i < rawIds.size(); i++) {
			contactQuery.append(Data.RAW_CONTACT_ID + " = ?");
			rawStr[i] = String.valueOf(rawIds.get(i));
			
			if (i != rawIds.size() - 1) contactQuery.append(" OR ");
		}
		contactQuery.append(")");
		
		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, 
			new String[] {
				Phone.NUMBER,
			},
			contactQuery.toString() + " AND " + Data.MIMETYPE + 
			" = '" + Phone.CONTENT_ITEM_TYPE + "'",
			rawStr,
			null
		);
		
		// Build a query string with all of the phone numbers.
		StringBuilder phoneNumQry = new StringBuilder();
		
		// The call must meet the minimum duration requirement.
		phoneNumQry.append(CallLog.Calls.DURATION + " >= " + DURATION_THRESHOLD + " AND ");
		// The call must not be missed (either incoming or outgoing and received)
		phoneNumQry.append(CallLog.Calls.TYPE + " != " + CallLog.Calls.MISSED_TYPE + " AND ");
		
		// There will be at least one phone number for the user because Heatwave
		// doesn't let you add users that you don't have phone numbers for (see
		// SelectContactsActivity.loadAdrContacts().
		c.moveToFirst();
		
		// If no phone numbers exist then return zero (it is impossible that
		// they are a contact that we've contacted on this device).
		if (c.isAfterLast()) return 0;
		
		while (!c.isAfterLast()) {
			phoneNumQry.append(CallLog.Calls.NUMBER + " = " + c.getString(0).replace("-", ""));
			c.moveToNext();
			
			if (!c.isAfterLast()) phoneNumQry.append(" OR ");
		}
		Log.i(TAG, phoneNumQry.toString());
		
		// Scan through the call log for recent calls.
		Cursor callCursor = context.getContentResolver().query(
			android.provider.CallLog.Calls.CONTENT_URI, 
			new String[] {
				CallLog.Calls.DATE,
			}, 
			phoneNumQry.toString(), 
			null,
			android.provider.CallLog.Calls.DATE + " DESC LIMIT 1");

		callCursor.moveToFirst();

		// If there are no call records then either the user has never called 
		// them or its been so long that it fell off the end of the logs.
		// Either way we're stuck with assuming they've never called.
		if (callCursor.isAfterLast()) return 0;

		long lastCall = (Long.parseLong(callCursor.getString(0)) / 1000);

		// Save the new timestamp as the most recent one.
		ContentValues cv = new ContentValues();
		cv.put("timestamp", lastCall);
		database.update(HeatwaveOpenHelper.CONTACTS_TABLE_NAME, 
			cv, 
			"uid = ?", 
			new String[] { String.valueOf(adrId) });
		
		callCursor.close();
		return lastCall;
	}
	
	public ArrayList<Long> getRawContacts(int adrId) {
		Cursor c = context.getContentResolver().query(RawContacts.CONTENT_URI, 
			new String[] { RawContacts._ID }, 
			RawContacts.CONTACT_ID + " = ?", 
			new String[] { String.valueOf(adrId) }, null);
		
		ArrayList<Long> raws = new ArrayList<Long>();
		
		c.moveToFirst();
		while (!c.isAfterLast()) {
			raws.add(c.getLong(0));
			c.moveToNext();
		}
		
		return raws;
	}
	
	public ArrayList<Contact> fetchContacts() {
		// Get the list of contacts from the Heatwave database.
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		qBuilder.setTables(HeatwaveOpenHelper.CONTACTS_TABLE_NAME);

		String[] local_projection = { "_id", "uid", "wave", "timestamp" };

		Cursor cursor = qBuilder.query(database, local_projection, null, null,
				null, null, null);
		ArrayList<Contact> contacts = new ArrayList<Contact>();

		// Some parameters for the system-wide contact lookups.
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		String[] adr_projection = new String[] { 
			ContactsContract.Contacts._ID,
			ContactsContract.Contacts.DISPLAY_NAME 
		};

		cursor.moveToFirst();
		
		while (!cursor.isAfterLast()) {
			Contact c = Contact.skeleton();
			Contact.Fields cf = c.new Fields();
			
			cf.setCid(cursor.getInt(0));
			cf.setAdrId(cursor.getInt(1));
			cf.setWave(fetchWave(cursor.getInt(2)));
			// Update the timestamp for each user.
			cf.setLatestTimestamp(updateTimestamp(cf.getAdrId()));

			// Query to find the contact's name
			Cursor adrCursor = context.getContentResolver().query(uri,
					adr_projection, "_id = ?", 
					new String[] { String.valueOf(cf.getAdrId()) }, 
					null);
			
			adrCursor.moveToFirst();
			cf.setName(adrCursor.getString(1));

			// Clean up.
			adrCursor.close();
			
			c.modify(cf, false);
			contacts.add(c);

			cursor.moveToNext();
		}

		cursor.close();
		return contacts;
	}

	// ////////////////////////////////
	// //// Wave-related methods //////
	// ////////////////////////////////

	public boolean addWave(Wave newWave) {
		return database.insert(HeatwaveOpenHelper.WAVE_TABLE_NAME, null,
				newWave.cv()) != -1;
	}
	
	public Wave fetchWave(int waveId) {
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		qBuilder.setTables(HeatwaveOpenHelper.WAVE_TABLE_NAME);

		String[] projection = { "_id", "name", "wavelength" };

		Cursor cursor = qBuilder.query(database, projection,
				"_id = " + waveId, null, null, null, null);

		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			Wave w = Wave.skeleton();
			Wave.Fields wf = w.new Fields();
			
			wf.setId(cursor.getInt(0));
			wf.setName(cursor.getString(1));
			wf.setWavelength(cursor.getInt(2));
			
			w.modify(wf, false);
			
			return w;
		} else
			return null;
	}

	public boolean removeWave(Wave target) {
		// Delete the wave.
		boolean wave = (database.delete(HeatwaveOpenHelper.WAVE_TABLE_NAME,
			"_id = ?",
			new String[] { String.valueOf(target.getId()) })) > 0;
				
		ContentValues cv = new ContentValues();
		cv.put("wave", (Integer)null);
		
		// Clear the foreign key pointing to the wave on all relevant contacts.
		boolean contacts = (database.update(HeatwaveOpenHelper.CONTACTS_TABLE_NAME,
			cv,
			"wave = ?",
			new String[] { String.valueOf(target.getId()) })) > 0;
			
		return wave && contacts;
	}
	
	public void updateWave(Wave w) {
		database.update(HeatwaveOpenHelper.WAVE_TABLE_NAME, 
			w.cv(), 
			"_id = ?", 
			new String[] { String.valueOf(w.getId()) });
	}

	public int getWaveMemberCount(Wave w) {
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		qBuilder.setTables(HeatwaveOpenHelper.CONTACTS_TABLE_NAME);

		String[] projection = { "_id" };
		String[] selectionArgs = { String.valueOf(w.getId()) };
		
		Cursor c = qBuilder.query(database, 
			projection, 
			"wave = ?", 
			selectionArgs,
			null, null, null);
		
		int count = 0;
		c.moveToFirst();
		while (!c.isAfterLast()) {
			count += 1;
			c.moveToNext();
		}
		c.close();
		
		return count;
	}
	
	public Wave loadWaveByName(String name) {
		Cursor c = database.query(HeatwaveOpenHelper.WAVE_TABLE_NAME,
				new String[] { "_id" }, 
				"name = ?", 
				new String[] { String.valueOf(name) }, 
				null, null, null);
		
		c.moveToFirst();
		if (c.isAfterLast()) return null;
		else return fetchWave(c.getInt(0));
	}

	public ArrayList<Wave> fetchWaves() {
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		qBuilder.setTables(HeatwaveOpenHelper.WAVE_TABLE_NAME);

		String[] projection = { "_id", "name", "wavelength" };

		Cursor cursor = qBuilder.query(database, projection, null, null, null,
				null, null);

		ArrayList<Wave> waves = new ArrayList<Wave>();

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Wave w = Wave.skeleton();
			Wave.Fields wf = w.new Fields();
			
			wf.setName(cursor.getString(1));
			wf.setWavelength(cursor.getInt(2));
			wf.setId(cursor.getInt(0));
			
			w.modify(wf, false);
			
			waves.add(w);
			cursor.moveToNext();
		}
		return waves;
	}
	
	//////////////////////
	/// Misc. Lookups ////
	//////////////////////
	
	public String getPhoneForContact(Contact c) throws Exception {
		String[] projection = new String[] {
			Phone.NUMBER
		};
		
		Cursor cur = context.getContentResolver().query(Phone.CONTENT_URI, 
			projection, 
			Phone.CONTACT_ID + " = ?", 
			new String[] { String.valueOf(c.getAdrId()) }, 
			null);
		
		if (cur.getCount() == 0) {
			throw new Exception("No phone number available for " + c.toString());
		}
		else {
			cur.moveToFirst();
			return cur.getString(0);
		}
	}
	
	/**
	 * Records a log entry for user identified by the provided phone number.
	 */
//	public void logCall(String phoneNum) {
//		Uri uri = Uri.withAppendedPath(
//			ContactsContract.PhoneLookup.CONTENT_FILTER_URI, 
//			Uri.encode(phoneNum));
//		
//		String[] projection = new String[] {
//			ContactsContract.PhoneLookup._ID,
//			ContactsContract.PhoneLookup.DISPLAY_NAME
//		};
//		
//		Cursor cur = context.getContentResolver().query(uri,
//			projection,
//			null,
//			null,
//			null);
//		
//		if (cur.getCount() == 0) {
//			Log.w(TAG, "Could not identify user with phone number " + phoneNum);
//		}
//		else {
//			cur.moveToFirst();
//			ContentValues cv = new ContentValues();
//			cv.put("event_type", "call");
//			cv.put("uid", cur.getInt(0));
//			cv.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
//		
//			database.insert(HeatwaveOpenHelper.EVENTLOG_TABLE_NAME, null, cv);
//			cur.close();
//		}
//	}
}