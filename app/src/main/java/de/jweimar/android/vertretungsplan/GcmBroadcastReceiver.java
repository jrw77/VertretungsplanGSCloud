/**
 * Copyright J. Weimar 2014
 */
package de.jweimar.android.vertretungsplan;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;


/**
 * Called when the ping message from Google Cloud messaging arrives.
 */

public class GcmBroadcastReceiver extends BroadcastReceiver {

	private static final String LOG_TAG = GcmBroadcastReceiver.class.getCanonicalName();
	private static long lastReceived = 0L;
    
    
    @Override
    public void onReceive(Context context, Intent intent) {
    	synchronized(GcmBroadcastReceiver.class){ // make sure that only one instance runs at a a time.
        Log.i(LOG_TAG, "GCM Broadcast received for Update");

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        String messageType = gcm.getMessageType(intent);
        Bundle extras = intent.getExtras();
        
        if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)){
            if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
                Log.i(LOG_TAG, "Received: " + extras.toString());
                // TODO: check for special messages (e.g. reload)
            }
            if (System.currentTimeMillis() < lastReceived +1000L*10 ) {// last received within 10 seconds
            	// ignore, as unregistering one of the IDs is not supported.
                Log.i(LOG_TAG, "Ignore, multiple messages ");
                return;
            }
            lastReceived = System.currentTimeMillis();

            Intent widgetUpdateIntent = new Intent(context, WidgetUpdateService.class);
            
            context.startService(widgetUpdateIntent);
            
            setResultCode(Activity.RESULT_OK);
      	
        }
        
    }
    
    }
}
