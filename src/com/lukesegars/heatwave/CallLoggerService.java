package com.lukesegars.heatwave;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallLoggerService extends Service {
	private static final String TAG = "EndCallWatcherService";
	private static boolean running = false;

	public static boolean isRunning() {
		return running;
	}
	
	/**
	 * Detects incoming calls and records them if they last long enough.
	 */
	protected class EndCallListener extends PhoneStateListener {
		private boolean inCall = false;
		
		// The minimum duration of a call before its logged.
		// TODO: Convert this into a public setting?
		private double callThreshold = 60.0;
		private double callStart = 0;
		
		private String contactNumber = null;

		private HeatwaveDatabase database = null;
		
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			super.onCallStateChanged(state, incomingNumber);
			if (database == null) {
				database = HeatwaveDatabase.getInstance(getApplicationContext());
			}
			
			
			switch (state) {
				case TelephonyManager.CALL_STATE_RINGING:
					Log.i("EndCallListener", "Receiving call...");
					Log.i("EndCallListener", "Starting a phone call with " + incomingNumber + ".");
					// Store the incoming phone number.
					contactNumber = incomingNumber;
					break;
				
				case TelephonyManager.CALL_STATE_OFFHOOK:
					Log.i("EndCallListener", "Dialing...");
					Log.i("EndCallListener", "Starting a phone call with " + incomingNumber + ".");
					inCall = true;
					callStart = System.currentTimeMillis();
					break;

				case TelephonyManager.CALL_STATE_IDLE:
					if (inCall) {
						Log.i("EndCallListener", "Ended a phone call from " + contactNumber + ".");
						
						// Log the call if its been more than *callThreshold* seconds.
						if ((System.currentTimeMillis() - callStart) / 1000 > callThreshold) {
//							database.logCall(contactNumber);
						}
					}
					inCall = false;
					incomingNumber = null;
					break;
			}
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
    public void onCreate() {
		super.onCreate();
		
		Log.i(TAG, "Creating and binding call listener...");
		EndCallListener listener = new EndCallListener();
		TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
		Log.i(TAG, "Done.");
		
		running = true;
	}
	
	@Override
	public void onDestroy() {
		running = false;
	}
}
