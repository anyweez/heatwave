package com.lukesegars.heatwave;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LoggerLaunchReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context c, Intent i) {
		// If the service isn't running yet then launch it.
		if (!CallLoggerService.isRunning()) {
			Intent service = new Intent(c, CallLoggerService.class);
			c.startService(service);
		}
	}

}
