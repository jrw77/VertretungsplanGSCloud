package de.jweimar.android.vertretungsplan;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author weimar
 *
 */
public class NotificationCancelReceiver extends BroadcastReceiver {

	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null){
        	notificationManager.cancel(WidgetUpdateService.NOTIFICATION_ID);
		}
	}

}
