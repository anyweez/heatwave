package com.lukesegars.heatwave;

import java.util.ArrayList;
import java.util.Map.Entry;

import com.lukesegars.heatwave.caches.ContactDataCache;

import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

/**
 * The CallLogMonitor is used to return the user back to the Heatwave UI
 * after a call is made from Heatwave.  It listens for changes to the call
 * log, which occur after the call ends, and then relaunches the FriendsList
 * activity.
 * 
 * This monitor is currently being bound before making a call and un
 */
public class CallLogMonitor extends ContentObserver {
    public CallLogMonitor(Handler h) {
        super(h);
        
        Log.i("CallLogMonitor", "CLM running...");
    }
    
    @Override
    public boolean deliverSelfNotifications() {
        return true;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        
    	Log.i("CallLogMonitor", "Invalidating cache due to call activity.");
    	// Invalidate and reload all contacts to get new timestamp data.
    	ArrayList<Contact> contacts = ContactDataCache.getInstance().getAllEntries();
    	for (Contact c : contacts) {
    		c.resetTimestamp();
    	}
    }
}
