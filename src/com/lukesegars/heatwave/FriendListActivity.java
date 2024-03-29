package com.lukesegars.heatwave;

import java.util.ArrayList;
import java.util.Comparator;

import com.lukesegars.heatwave.caches.ContactDataCache;

import android.net.Uri;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

//import com.bugsense.trace.BugSenseHandler;

//import net.hockeyapp.android.CrashManager;
//import net.hockeyapp.android.UpdateManager;

public class FriendListActivity extends ListActivity {
	private static final String TAG = "FriendListActivity";
	private Contact contextTarget = null;
	
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
//        BugSenseHandler.initAndStartSession(getApplicationContext(), "24fa824c");
        setContentView(R.layout.activity_friend_list);
        
        storeObjectContext();
        loadObjectCaches();

        ArrayList<Contact> contacts = new ArrayList<Contact>();
        ContactArrayAdapter listAdapter = new ContactArrayAdapter(this,
        	R.layout.display_contact_row,
        	contacts);
        
        setListAdapter(listAdapter);
        
		// Start call listener
		getApplicationContext().getContentResolver().registerContentObserver(
			android.provider.CallLog.Calls.CONTENT_URI, 
			true, 
			clm);

		// Add some click listeners to the images that show up on an empty
		// listview.
        ImageView awg = (ImageView)findViewById(R.id.add_waves_guide);
        awg.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
        		launchWaveManager();
			}
        });
        
        ImageView acg = (ImageView)findViewById(R.id.add_contacts_guide);
        acg.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
        		launchContactManager();
			}
        });
		
		registerForContextMenu(getListView());
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	// Update the list in case anything has changed.  This is now called
    	// for first load as well instead of calling in onStart() as well.
    	updateContactList();
//        checkForCrashes();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu cm, View v, ContextMenuInfo cmi) {
    	super.onCreateContextMenu(cm, v, cmi);
    	getMenuInflater().inflate(R.menu.context_friend_list, cm);
    	
    	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) cmi;
    	ContactArrayAdapter adapter = (ContactArrayAdapter)getListAdapter();
    	contextTarget = adapter.getItem(info.position);
    	
    	// Hide the "add to wave" menu item if there are no waves.
    	if (Wave.loadAll().size() == 0) cm.getItem(1).setEnabled(false);
    	
    	// Set the title to be the user's name.
    	cm.setHeaderTitle(contextTarget.getName());
    }
    
    @Override
	public boolean onContextItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.ctx_select_wave:
    			final ArrayList<Wave> waves = Wave.loadAll();
    			Log.i(TAG, "# of waves: " + waves.size());
    			final String[] names = new String[waves.size()];
    			for (int i = 0; i < waves.size(); i++) names[i] = waves.get(i).getName();
    			
    			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
    			dialog.setTitle("Select wave");
    			dialog.setItems(names, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						String name = names[which];
						Wave selected = null;
						for (int i = 0; i < waves.size(); i++) {
							if (waves.get(i).getName().equals(name)) selected = waves.get(i);
						}
						
						// If a wave was found with this name (which it always should be),
						// save it.
						if (selected != null) {
							Contact.Fields fields = contextTarget.new Fields();
							fields.setWave(selected);
							contextTarget.modify(fields);
							
							ContactArrayAdapter adapter = (ContactArrayAdapter)getListAdapter();
					    	adapter.sort(listSorter);
							// Notify the adapter that the data may have changed.
							adapter.notifyDataSetChanged();
						}
					}
				});
    			dialog.show();
    			break;
    		case R.id.ctx_snooze_contact:
    			// TODO: Check for null contextTarget.  If so, log it and abort add.
    			// Store the snooze event.
    			SnoozeMaster sm = SnoozeMaster.getInstance(getApplicationContext());
    			sm.addSnooze(contextTarget, Math.round(System.currentTimeMillis() / 1000));
    			
    			// Update the UI.
    			ContactArrayAdapter adapter = (ContactArrayAdapter)getListAdapter();
    			adapter.sort(listSorter);
    			adapter.notifyDataSetChanged();
    			
    			// Display a message to the user to let them know that the contact has been
    			// snoozed successfully.
    			Toast msg = Toast.makeText(getApplicationContext(), 
    				"Snoozed " + contextTarget.getName(), 
    				Toast.LENGTH_SHORT);
    			msg.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
    			msg.show();
    			contextTarget = null;
    			break;
    	}

    	return false;
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
//    private void checkForCrashes() {
//    	CrashManager.register(this, "800a9d6afe09212f2190b77eec8ea168");
//    }
    
//    private void checkForUpdates() {
//        // TODO: Remove this for store builds!
//    	UpdateManager.register(this, "800a9d6afe09212f2190b77eec8ea168");
//    }
    // end Hockey.app
    
    private void storeObjectContext() {
    	HeatwaveDatabase.setContext(getApplicationContext());
    	Wave.setContext(getApplicationContext());
    	Contact.setContext(getApplicationContext());
    }
    
    private void loadObjectCaches() {
    	Log.i(TAG, "Loading caches...");
    	long start = System.currentTimeMillis();

    	// Load the caches.
    	Contact.getAll();
    	Wave.loadAll();
    	
    	Log.i(TAG, "Finished in " + ((System.currentTimeMillis() - start) / 1000.0) + " seconds.");
    }
    
    @Override
    public void onListItemClick(ListView lv, View v, int position, long id) {
    	ContactArrayAdapter adapter = (ContactArrayAdapter)getListAdapter();
		Contact c = adapter.getItem(position);
		
		String phoneNum = c.getPhoneNum();

		Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNum));
		startActivity(i);
    }
    
    // TODO: Better to clear all and then re-sort?  Faster to diff then +/-?
    public void updateContactList() {
    	Log.i(TAG, "Updating contact list...");
    	long start = System.currentTimeMillis();
    	
    	Log.i(TAG, "  Contacts in cache: " + ContactDataCache.getInstance().numEntries());
    	ArrayList<Contact> contacts = Contact.getAll();
    	ContactArrayAdapter adapter = (ContactArrayAdapter) getListAdapter();
    	
    	// Remove all contacts and re-add them.
    	adapter.clear();
    	
    	// Read the latest timestamps for all contacts and add them to the list
    	// adapter.
    	for (Contact contact : contacts) {
    		contact.resetTimestamp();
    		adapter.add(contact);
    	}
    	
    	adapter.sort(listSorter);
    	adapter.notifyDataSetChanged();

    	Log.i(TAG, "Updating contacts took " + (System.currentTimeMillis() - start) / 1000.0 + " seconds.");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_friend_list, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.menu_contacts:
    			launchContactManager();
    			return true;
    		case R.id.menu_waves:
    			launchWaveManager();
    			return true;
    		default:
    			return super.onOptionsItemSelected(item);
    	}
    }
    
    private void launchContactManager() {
		startActivity(
				new Intent(this, SelectContactsActivity.class)
		);
    }
    
    private void launchWaveManager() {
		startActivity(
			new Intent(this, DisplayWaveActivity.class)
		);
    }
}
