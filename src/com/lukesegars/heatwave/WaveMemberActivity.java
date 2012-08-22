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


public class WaveMemberActivity extends ListActivity {
	private static final String TAG = "WaveMemberActivity";
	
	private HeatwaveDatabase database;

	private ArrayList<String> contactNames;
	private ArrayList<Integer> contactIds;
	
	private Wave wave;
	
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
				ArrayList<Integer> inactives = new ArrayList<Integer>();
				
				for (int i = 0; i < arr.size(); i++) {
					int itemId = arr.keyAt(i);

					Log.i(TAG, "ITEM #" + itemId + ": " + arr.valueAt(i));
					if (arr.valueAt(i)) actives.add(contactIds.get(itemId));
					else inactives.add(contactIds.get(itemId));
				}
				updateWaveMembers(actives, inactives);
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
		
		int waveId = getIntent().getExtras().getInt("waveId");
		wave = database.fetchWave(waveId);
		
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
		
		ListView lv = getListView();
		// Configure the adapter for the ListView.
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
				android.R.layout.simple_list_item_multiple_choice, 
				contactNames);
		lv.setAdapter(adapter);
		
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			int cNum = contactIds.size();
			int cid = cursor.getInt(0);

			// Fetch the wave information for the current contact.
			Contact c = database.fetchContact(cid);
			if (c != null) {
				Wave w = c.getWave();

				contactIds.add(cid);
				contactNames.add(cursor.getString(1));
			
				if (w != null && w.getId() == wave.getId()) {
					lv.setItemChecked(cNum, true);
				}
			}
			cursor.moveToNext();
		}

		adapter.notifyDataSetChanged();
	}
	
	public void updateWaveMembers(ArrayList<Integer> actives, ArrayList<Integer> inactives) {
		// For each user, update their contact record to indicate that
		// they are in the current wave.
		for (Integer cid : actives) {
			Contact c = database.fetchContact(cid);
			c.setWave(wave);

			// Save the changes.
			database.updateContact(c);
		}

		// Update the records of individuals who are no longer in the wave.
		for (Integer cid : inactives) {
			Contact c = database.fetchContact(cid);
			c.setWave(null);
			
			database.updateContact(c);
		}
		
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
