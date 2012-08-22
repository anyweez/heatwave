package com.lukesegars.heatwave;

import java.util.ArrayList;

import android.app.ListActivity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;


public class SelectContactsActivity extends ListActivity {
	private static final String TAG = "SelectContactsActivity";
	
	private HeatwaveDatabase database;

	private ArrayList<String> contactNames;
	private ArrayList<Integer> contactIds;
	
	@Override
	protected void onCreate(Bundle saved) {
		super.onCreate(saved);
		setContentView(R.layout.activity_select_contacts);
		
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		
		Button sc_btn = (Button)findViewById(R.id.save_contacts_btn);
		sc_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				SparseBooleanArray arr = getListView().getCheckedItemPositions();
				
				ArrayList<Integer> actives = new ArrayList<Integer>();

				for (int i = 0; i < arr.size(); i++) {
					int itemId = arr.keyAt(i);
					if (arr.valueAt(i)) actives.add(contactIds.get(itemId));
				}
				updateContacts(actives);
				finish();
			}
		});
		
		Button cc_btn = (Button)findViewById(R.id.cancel_contacts_btn);
		cc_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
		database = HeatwaveDatabase.getInstance(this);
		
		contactNames = new ArrayList<String>();
		contactIds = new ArrayList<Integer>();
		
		loadAdrContacts();
	}
	
	private void loadAdrContacts() {
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		String[] projection = new String[] {
			ContactsContract.Contacts._ID,
			ContactsContract.Contacts.DISPLAY_NAME
		};

		Cursor cursor = managedQuery(uri, 
			projection, 
			ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1", 
			null, 
			ContactsContract.Contacts.DISPLAY_NAME + " ASC");
		
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			contactIds.add(cursor.getInt(0));
			contactNames.add(cursor.getString(1));
			
			cursor.moveToNext();
		}

		ListView lv = getListView();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
			android.R.layout.simple_list_item_multiple_choice, 
			contactNames);
		lv.setAdapter(adapter);
		
		ArrayList<Integer> actives = database.getActiveContactAdrIds();
		
		// For each active ID, check to see if the contact ID
		for (int i = 0; i < contactIds.size(); i++) {
			if (actives.contains(contactIds.get(i))) lv.setItemChecked(i, true);
			else lv.setItemChecked(i, false);
		}
		
		adapter.notifyDataSetChanged();
	}

	private void updateContacts(ArrayList<Integer> actives) {
		ArrayList<Integer> currentActives = database.getActiveContactAdrIds();
		
		ArrayList<Integer> additions = new ArrayList<Integer>(actives);
		ArrayList<Integer> removals = new ArrayList<Integer>(currentActives);

		additions.removeAll(currentActives);
		removals.removeAll(actives);
		
		// 1. Create Contact objects and call db.addContacts()
//		ArrayList<Contact> addContacts = new ArrayList<Contact>();
//		for (Integer id : adds) {
//			addContacts.add(makeContact(id));
//		}
		ArrayList<Contact> addContacts = new ArrayList<Contact>();
		for (Integer id : additions) {
			addContacts.add(makeContact(id));
		}
		Log.i(TAG, "Adding:");
		for (Contact c : addContacts) {
			Log.i(TAG, c.toString());
		}
		database.addContacts(addContacts);
		
		// 2. Create Contact objects and call db.removeContacts()
		ArrayList<Contact> removeContacts = new ArrayList<Contact>();
		for (Integer id : removals) {
			removeContacts.add(makeContact(id));
		}
		Log.i(TAG, "Removing:");
		for (Contact c : removeContacts) {
			Log.i(TAG, c.toString());
		}
		database.removeContacts(removeContacts);
//		database.removeContacts(removeContacts);
		
		// FIXME: Not removing contacts that are unchecked.
	}
	
	/**
	 * Accepts a list of contact ID's that should define the list of active
	 * contacts.
	 * 
	 * @param activeIds
	 */
//	private void updateContacts(ArrayList<Integer> adds, ArrayList<Integer> drops) {
//		// 1. Create Contact objects and call db.addContacts()
//		ArrayList<Contact> addContacts = new ArrayList<Contact>();
//		for (Integer id : adds) {
//			addContacts.add(makeContact(id));
//		}
//		database.addContacts(addContacts);
//		
//		// 2. Create Contact objects and call db.removeContacts()
//		ArrayList<Contact> removeContacts = new ArrayList<Contact>();
//		for (Integer id : drops) {
//			removeContacts.add(makeContact(id));
//		}
//		database.removeContacts(removeContacts);
//	}
	
	private Contact makeContact(int contact_id) {
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		String[] projection = new String[] {
			ContactsContract.Contacts.DISPLAY_NAME
		};
		
		Cursor cursor = managedQuery(uri, 
			projection, 
			"_id = ?", 
			new String[] { String.valueOf(contact_id) }, 
			null);
		
		cursor.moveToFirst();
		return new Contact(cursor.getString(0), null, contact_id);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
}
