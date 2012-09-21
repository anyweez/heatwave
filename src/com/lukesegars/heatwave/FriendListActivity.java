package com.lukesegars.heatwave;

import java.util.ArrayList;
import java.util.Comparator;

import android.net.Uri;
import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

public class FriendListActivity extends ListActivity {
	private static final String TAG = "FriendListActivity";

	// Added before
	private CallLogMonitor clm = new CallLogMonitor(null);
	
	private Comparator<Contact> listSorter = new Comparator<Contact>() {
		public int compare(Contact first, Contact second) {
			if (first.getScore() < second.getScore()) return 1;
			else if (first.getScore() > second.getScore()) return -1;
			else return 0;
		}
    };
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storeObjectContext();
		registerForContextMenu(getListView());
        
        setContentView(R.layout.activity_friend_list);
        long startTime = System.currentTimeMillis();

        ArrayList<Contact> contacts = Contact.getAll();
        
        Log.i(TAG, "Contacts fetched after " +  (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
        ArrayList<String> names = new ArrayList<String>();
        for (Contact c : contacts) {
        	names.add(c.getName());
        }

        ContactArrayAdapter listAdapter = new ContactArrayAdapter(this,
        		R.layout.display_contact_row,
        		contacts);
        
        listAdapter.sort(listSorter);
        
        setListAdapter(listAdapter);
        listAdapter.notifyDataSetChanged();
        
        Log.i(TAG, "Loaded display in " + (System.currentTimeMillis() - startTime) / 1000.0 + " seconds");
        
		// Start call listener
		clm.returnTo(this);
		getApplicationContext().getContentResolver().registerContentObserver(
			android.provider.CallLog.Calls.CONTENT_URI, 
			true, 
			clm);
		
		// Hockey.net
	    checkForUpdates();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	// Update the list in case anything has changed.
    	updateContactList();

        checkForCrashes();
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	// Unregister the CallLogMonitor in order to prevent leaks.
    	getApplicationContext().getContentResolver().unregisterContentObserver(clm);
    }
    
    // Hockey.app
    private void checkForCrashes() {
    	CrashManager.register(this, "800a9d6afe09212f2190b77eec8ea168");
    }
    
    private void checkForUpdates() {
        // TODO: Remove this for store builds!
    	UpdateManager.register(this, "800a9d6afe09212f2190b77eec8ea168");
    }
    // end Hockey.app
    
    private void storeObjectContext() {
    	Wave.setContext(getApplicationContext());
    	Contact.setContext(getApplicationContext());
    }
    
    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
    	ContactArrayAdapter adapter = (ContactArrayAdapter)getListAdapter();
		Contact c = adapter.getItem(position);
		
		String phoneNum = c.getPhoneNum();

		Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNum));
		startActivity(i);
    }
    
    // TODO: Necessary to clear all and then re-sort?  Would it be faster 
    public void updateContactList() {
    	ArrayList<Contact> contacts = Contact.getAll();
    	ContactArrayAdapter adapter = (ContactArrayAdapter) getListAdapter();
    	
    	// Remove all contacts and re-add them.
    	adapter.clear();
    	
    	for (Contact contact : contacts) {
    		adapter.add(contact);
    	}
    	
    	adapter.sort(listSorter);
    	adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_friend_list, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	// TODO: Add handlers for each of the menu options.
    	switch (item.getItemId()) {
    		case R.id.menu_contacts:
    			startActivity(
    				new Intent(this, SelectContactsActivity.class)
    			);
    			return true;
    		case R.id.menu_waves:
    			startActivity(
    				new Intent(this, DisplayWaveActivity.class)
    			);
    			return true;
    		default:
    			return super.onOptionsItemSelected(item);
    	}
    }
}
