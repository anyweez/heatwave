package com.lukesegars.heatwave;

import java.util.ArrayList;

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
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

public class HeatwaveDatabase {
	private static final String TAG = "HeatwaveDatabase";

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
		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "heatwave";
		private static final String WAVE_TABLE_NAME = "waves";
		private static final String CONTACTS_TABLE_NAME = "contacts";
		private static final String EVENTLOG_TABLE_NAME = "eventlog";

		private static final String WAVE_TABLE_CREATE = "CREATE TABLE "
				+ WAVE_TABLE_NAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ // primary key
				"name TEXT, " + // name of the wave
				"wavelength INTEGER)"; // the amount of time between contact (in
										// seconds)

		private static final String CONTACTS_TABLE_CREATE = "CREATE TABLE "
				+ CONTACTS_TABLE_NAME
				+ " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " + // primary key
				"uid INTEGER, " + // system-level user ID (Android contact ID)
				"wave INTEGER)"; // foreign key to the wave table. Contacts can
									// only
									// be in one wave at a time.

		private static final String EVENTLOG_TABLE_CREATE = "CREATE TABLE "
				+ EVENTLOG_TABLE_NAME
				+ " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "event_type TEXT, " + "uid INTEGER, " + "timestamp DATETIME)";

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
			db.execSQL(EVENTLOG_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + WAVE_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + CONTACTS_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + EVENTLOG_TABLE_NAME);

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

	// public void close() {
	// dbHelper.close();
	// }

	// ///////////////////////////////////
	// //// Contact-related methods //////
	// ///////////////////////////////////

	public boolean addContact(Contact newContact) {
		return database.insert(HeatwaveOpenHelper.CONTACTS_TABLE_NAME, null,
				newContact.cv()) != -1;
	}

	public boolean addContacts(ArrayList<Contact> contacts) {
		boolean errors = false;
		for (Contact c : contacts) {
			if (!addContact(c))
				errors = true;
		}

		return errors;
	}

	public void updateContact(Contact c) {
		String[] selectionArgs = new String[] { String.valueOf(c.getContactId()) };
		database.update(HeatwaveOpenHelper.CONTACTS_TABLE_NAME, 
			c.cv(), 
			"_id = ?", 
			selectionArgs);
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
	
	public Contact fetchContact(int contactId) {
		// Get the list of contacts from the Heatwave database.
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		qBuilder.setTables(HeatwaveOpenHelper.CONTACTS_TABLE_NAME);

		String[] local_projection = { "_id", "uid", "wave" };

		// TODO: merge fetchContact and fetchContacts to share a lot of this code.
		String selection = (contactId >= 0) ? "uid = ?" : null;
		String[] selectionArgs = (contactId >= 0) ? new String[] { String.valueOf(contactId) } : null;
		Cursor cursor = qBuilder.query(database, 
				local_projection, 
				selection, 
				selectionArgs,
				null, null, null);

		// Some parameters for the system-wide contact lookups.
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		String[] adr_projection = new String[] { 
			ContactsContract.Contacts._ID,
			ContactsContract.Contacts.DISPLAY_NAME 
		};

		// All future queries will be made to the event log table.
		qBuilder.setTables(HeatwaveOpenHelper.EVENTLOG_TABLE_NAME);
		cursor.moveToFirst();

		// Return null if the contact doesn't exist.
		if (cursor.isAfterLast()) return null;
		
//		while (!cursor.isAfterLast()) {
		int user_id = cursor.getInt(0);
		int adr_user_id = cursor.getInt(1);
		int wave_id = cursor.getInt(2);

		// Query to find the contact's name
		Cursor adrCursor = context.getContentResolver().query(uri,
			adr_projection, "_id = ?", 
			new String[] { String.valueOf(adr_user_id) }, 
			null);
			
		adrCursor.moveToFirst();
		String name = adrCursor.getString(1);

		// Get the timestamp of the most recent call with the
		// contact.
		String[] call_projection = new String[] {
			"timestamp"
		};
			
//		Cursor callCursor = qBuilder.query(database, 
//			call_projection, 
//			"uid = ?", 
//			new String[] { String.valueOf(adr_user_id) }, 
//			null, 
//			null, 
//			"timestamp DESC", 
//			"1");
			
		Contact contact = new Contact(name, fetchWave(wave_id), adr_user_id, user_id);
		contact.setLastCallTimestamp(getLastCallTimestamp(contact));
			
		// Clean up.
		adrCursor.close();
			
		return contact;
	}
	
	public int getLastCallTimestamp(Contact c) {
		Cursor callCursor = context.getContentResolver().query(
			android.provider.CallLog.Calls.CONTENT_URI, 
			new String[] {
				CallLog.Calls.NUMBER,
				CallLog.Calls.DURATION,
				CallLog.Calls.DATE,
				CallLog.Calls.TYPE 
			}, 
			null, 
			null, android.provider.CallLog.Calls.DATE + " DESC");

		callCursor.moveToFirst();
		while (!callCursor.isAfterLast()) {
			String num = callCursor.getString(0);
			String dur = callCursor.getString(1);
			Log.i(TAG, "Call from " + num + " that lasted " + dur + " seconds.");
			callCursor.moveToNext();
		}

		callCursor.close();
		return 0;
	}
	public ArrayList<Contact> fetchContacts() {
		// Get the list of contacts from the Heatwave database.
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		qBuilder.setTables(HeatwaveOpenHelper.CONTACTS_TABLE_NAME);

		String[] local_projection = { "_id", "uid", "wave" };

		Cursor cursor = qBuilder.query(database, local_projection, null, null,
				null, null, null);
		ArrayList<Contact> contacts = new ArrayList<Contact>();

		// Some parameters for the system-wide contact lookups.
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		String[] adr_projection = new String[] { 
			ContactsContract.Contacts._ID,
			ContactsContract.Contacts.DISPLAY_NAME 
		};

		// All future queries will be made to the event log table.
		qBuilder.setTables(HeatwaveOpenHelper.EVENTLOG_TABLE_NAME);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			int user_id = cursor.getInt(0);
			int adr_user_id = cursor.getInt(1);
			int wave_id = cursor.getInt(2);

			// Query to find the contact's name
			Cursor adrCursor = context.getContentResolver().query(uri,
					adr_projection, "_id = ?", 
					new String[] { String.valueOf(adr_user_id) }, 
					null);
			
			adrCursor.moveToFirst();
			String name = adrCursor.getString(1);

			// Get the timestamp of the most recent call with the
			// contact.
			String[] call_projection = new String[] {
				"timestamp"
			};
			
			Cursor callCursor = qBuilder.query(database, 
				call_projection, 
				"uid = ?", 
				new String[] { String.valueOf(adr_user_id) }, 
				null, 
				null, 
				"timestamp DESC", 
				"1");
			
			Contact contact = new Contact(name, fetchWave(wave_id), adr_user_id);
			
			if (callCursor.getCount() > 0) {
				callCursor.moveToFirst();
				int timestamp = Integer.parseInt(callCursor.getString(0));
				contact.setLastCallTimestamp(timestamp);
			}
			
			// Clean up.
			adrCursor.close();
			callCursor.close();
			
			contacts.add(contact);
			cursor.moveToNext();
		}
		return contacts;
	}

	// ////////////////////////////////
	// //// Wave-related methods //////
	// ////////////////////////////////

	public boolean addWave(Wave newWave) {
		return database.insert(HeatwaveOpenHelper.WAVE_TABLE_NAME, null,
				newWave.cv()) != -1;
	}

	public Wave fetchWave(int wave_id) {
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		qBuilder.setTables(HeatwaveOpenHelper.WAVE_TABLE_NAME);

		String[] projection = { "_id", "name", "wavelength" };

		Cursor cursor = qBuilder.query(database, projection,
				"_id = " + wave_id, null, null, null, null);

		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			return new Wave(cursor.getString(1), cursor.getInt(2),
					cursor.getInt(0));
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
		String[] selectionArgs = new String[] { String.valueOf(w.getId()) };
		database.update(HeatwaveOpenHelper.WAVE_TABLE_NAME, 
			w.cv(), 
			"_id = ?", 
			selectionArgs);
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

	public ArrayList<Wave> fetchWaves() {
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		qBuilder.setTables(HeatwaveOpenHelper.WAVE_TABLE_NAME);

		String[] projection = { "_id", "name", "wavelength" };

		Cursor cursor = qBuilder.query(database, projection, null, null, null,
				null, null);

		ArrayList<Wave> waves = new ArrayList<Wave>();

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			waves.add(new Wave(cursor.getString(1), cursor.getInt(2), cursor
					.getInt(0)));
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
	public void logCall(String phoneNum) {
		Uri uri = Uri.withAppendedPath(
			ContactsContract.PhoneLookup.CONTENT_FILTER_URI, 
			Uri.encode(phoneNum));
		
		String[] projection = new String[] {
			ContactsContract.PhoneLookup._ID,
			ContactsContract.PhoneLookup.DISPLAY_NAME
		};
		
		Cursor cur = context.getContentResolver().query(uri,
			projection,
			null,
			null,
			null);
		
		if (cur.getCount() == 0) {
			Log.w(TAG, "Could not identify user with phone number " + phoneNum);
		}
		else {
			cur.moveToFirst();
			ContentValues cv = new ContentValues();
			cv.put("event_type", "call");
			cv.put("uid", cur.getInt(0));
			cv.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
		
			database.insert(HeatwaveOpenHelper.EVENTLOG_TABLE_NAME, null, cv);
			cur.close();
		}
	}
}