package com.lukesegars.heatwave;

import java.util.ArrayList;

import android.app.ListActivity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
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
		
		long waveId = getIntent().getExtras().getLong("waveId");
		database = HeatwaveDatabase.getInstance(this);
		wave = database.fetchWave(waveId);
		
		contactNames = new ArrayList<String>();
		contactIds = new ArrayList<Integer>();
				
		loadAdrContacts();
	}
	
	/**
	 * TODO: This should be moved into the HeatwaveDatabase.  It may also be 
	 * possible to roll it into one of the pre-existing functions.
	 */
	private void loadAdrContacts() {
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		String[] projection = new String[] {
			ContactsContract.Contacts._ID,
			ContactsContract.Contacts.DISPLAY_NAME
		};

		Cursor cursor = getContentResolver().query(uri, 
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
			int adrId = cursor.getInt(0);

			// Fetch the wave information for the current contact.
			Contact c = Contact.loadByAdrId(adrId);
			if (c != null) {
				Wave w = c.getWave();

				contactIds.add(adrId);
				contactNames.add(cursor.getString(1));
			
				if (w != null && w.getId() == wave.getId()) { 
					lv.setItemChecked(cNum, true);
				}
			}
			cursor.moveToNext();
		}
		cursor.close();
		adapter.notifyDataSetChanged();
	}
	
	public void updateWaveMembers(ArrayList<Integer> actives, ArrayList<Integer> inactives) {
		// For each user, update their contact record to indicate that
		// they are in the current wave.
		for (Integer cid : actives) {
			Contact c = Contact.loadByAdrId(cid);
			Contact.Fields cf = c.new Fields();

			cf.setWave(wave);
			c.modify(cf);
		}

		// Update the records of individuals who are no longer in the wave.
		for (Integer cid : inactives) {
			Contact c = Contact.loadByAdrId(cid);
			Contact.Fields cf = c.new Fields();

			cf.setWave(null);
			c.modify(cf);
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
