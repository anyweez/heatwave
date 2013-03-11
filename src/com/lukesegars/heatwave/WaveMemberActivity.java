package com.lukesegars.heatwave;

import java.util.ArrayList;
import java.util.Collections;

import com.lukesegars.heatwave.caches.ContactDataCache;

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
	private ArrayList<Long> contactIds;
	
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
				
				ArrayList<Long> actives = new ArrayList<Long>();
				ArrayList<Long> inactives = new ArrayList<Long>();
				
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
		database = HeatwaveDatabase.getInstance();
		wave = database.fetchWave(waveId);
		
		loadAdrContacts();
	}
	
	/**
	 * TODO: This should be moved into the HeatwaveDatabase.  It may also be 
	 * possible to roll it into one of the pre-existing functions.
	 */
	private void loadAdrContacts() {
		ArrayList<Contact> contacts = Contact.getAll();
		ArrayList<String> contactNames = new ArrayList<String>();
		contactIds = new ArrayList<Long>();
		
		Collections.sort(contacts, new ContactComparator());
		for (Contact c : contacts) {
			contactNames.add(c.getName());
			contactIds.add(c.getAdrId());
		}

		ListView lv = getListView();
		// Configure the adapter for the ListView.
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
				android.R.layout.simple_list_item_multiple_choice, 
				contactNames);
		lv.setAdapter(adapter);
		
		for (int i = 0; i < contacts.size(); i++) {
			Contact c = contacts.get(i);
			if (c.getWave() != null && c.getWave().getId() == wave.getId()) { 
				lv.setItemChecked(i, true);
			}			
		}
		
		
		
		adapter.notifyDataSetChanged();
	}
	
	public void updateWaveMembers(ArrayList<Long> actives, ArrayList<Long> inactives) {
		ContactDataCache ctxCache = ContactDataCache.getInstance();
		
		// For each user, update their contact record to indicate that
		// they are in the current wave.
		for (Long cid : actives) {
			Contact c = Contact.loadByAdrId(cid);
			Contact.Fields cf = c.new Fields();

			cf.setWave(wave);
			c.modify(cf);
			
			ctxCache.invalidateEntry(c.getAdrId());
		}

		// Update the records of individuals who are no longer in the wave.
		for (Long cid : inactives) {
			Contact c = Contact.loadByAdrId(cid);
			Contact.Fields cf = c.new Fields();

			cf.setWave(null);
			c.modify(cf);

			ctxCache.invalidateEntry(c.getAdrId());
//			ctxCache.addEntry(c.getAdrId(), c);
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
