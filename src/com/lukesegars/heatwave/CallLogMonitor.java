package com.lukesegars.heatwave;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
	private Activity target = null;
	
    public CallLogMonitor(Handler h) {
        super(h);
        
        Log.i("CallLogMonitor", "CLM running...");
    }
    
    public void returnTo(Activity a) {
    	target = a;
    }

    @Override
    public boolean deliverSelfNotifications() {
        return true;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        
        // If a target to return to has been provided, relaunch that intent.
        // TODO: This currently isn't returning to TARGET but to the app base (ok).
        if (target != null) {
        	// Unregister self.  We'll rebind as needed.
//            target.getApplicationContext().getContentResolver().unregisterContentObserver(this);

            Context base = target.getBaseContext();
        	
        	Intent i = base.getPackageManager().getLaunchIntentForPackage(
        		base.getPackageName());
        	
        	// CLEAR_TOP means that all activities between now and any previously
        	// occuring instances of the new activity should be closed (meaning the
        	// call log activity).
        	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	
        	// Launch!
        	base.startActivity(i);
        }
    }
}
