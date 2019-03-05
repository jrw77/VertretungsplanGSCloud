package de.jweimar.android.vertretungsplan;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class WidgetUpdateService extends IntentService {

	private static final String LOG_TAG = WidgetUpdateService.class.getCanonicalName();
	public static final int NOTIFICATION_ID = 1;
    public static final int REQUEST_CODE = 123;

	public WidgetUpdateService(){
		super("WidgetUpdateService");
	}
	@Override
	protected void onHandleIntent(Intent intent) {
		Context context = this.getBaseContext();
	    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

	    ComponentName thisWidget = new ComponentName(context,
	    		Provider.class);
	    
	    int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

	    Log.w(LOG_TAG, "Direct count of widgets:" + String.valueOf(allWidgetIds.length));
	    if (allWidgetIds.length == 0){
		    thisWidget = new ComponentName(context,
		    		Provider2.class);
		    
		    allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

		    Log.w(LOG_TAG, "Direct count of widgets (2):" + String.valueOf(allWidgetIds.length));
	    	
	    }
	    // TODO: move this to WidgetUpdateService, since Network may not be done on this thread!
	    boolean relevant = false;
	    for (int widgetId : allWidgetIds) {
		    Provider.updateAppWidget(context,widgetId, false);
		    String aktuell = Provider.aktuellerWert(widgetId);
	        Log.i(LOG_TAG, "GCM Broadcast receiver Widget "+widgetId+" Wert "+aktuell);

		    if (!("".equals(aktuell) || "Pausiert".equals(aktuell) || "Keine Vertretungen".equals(aktuell))){
		    	relevant = true;
		    }
	        Log.i(LOG_TAG, "relevant  "+relevant);
	    }
        
	    sendNotification(context, relevant?"Vertretungen wurden aktualisiert!":"Vertretungen aktualisiert, keine Vertretungen", relevant);

	}

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(Context context, String msg, boolean relevant) {
		NotificationManager mNotificationManager = (NotificationManager)
				context.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent
				.getActivity(context, 0, Provider.createConfigureIntent(context, PersistentData.DUMMY_APPWIDGET_ID),
						PendingIntent.FLAG_UPDATE_CURRENT);
   
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
    		.setSmallIcon(R.drawable.notification1)
        	.setContentTitle("Vertretungsplan")
        	.setStyle(new NotificationCompat.BigTextStyle().bigText(msg)) // requires API level 16 at least. 
        	.setAutoCancel(true)
        	.setContentText(msg)
        	;
        
        if (relevant){
        	long t1 = 200L;      
        	long[] vibratePattern = {0L, t1, t1, t1 };
        	mBuilder.setVibrate(vibratePattern);	
        }
		if (mNotificationManager == null){
		    return;
        }
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        // schedule removal of the notification after one hour.
        scheduleNotificationRemoval(context, relevant);
    }

    void scheduleNotificationRemoval(Context context, boolean relevant){
        int alarmType = AlarmManager.ELAPSED_REALTIME;
        final int HOUR_MILLIS = 60*60*1000;

        // First create an intent for the alarm to activate.
        // This code simply starts an Activity, or brings it to the front if it has already
        // been created.
        Intent intent = new Intent(context, NotificationCancelReceiver.class);
        intent.setAction(Intent.ACTION_DEFAULT);
  
        // Because the intent must be fired by a system service from outside the application,
        // it's necessary to wrap it in a PendingIntent.  Providing a different process with
        // a PendingIntent gives that other process permission to fire the intent that this
        // application has created.
        // Also, this code creates a PendingIntent to start an Activity.  To create a
        // BroadcastIntent instead, simply call getBroadcast instead of getIntent.
        @NonNull
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, 0);

        // The AlarmManager, like most system services, isn't created by application code, but
        // requested from the system.
        AlarmManager alarmManager = (AlarmManager)
               context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null){
            return;
        }
        
        // Cancel any previous alarms set like this.
        alarmManager.cancel(pendingIntent);
        
        // set the alarm.
        if (relevant){
        	alarmManager.set(alarmType, SystemClock.elapsedRealtime() + HOUR_MILLIS,
                    pendingIntent);
        }else{
         	alarmManager.set(alarmType, SystemClock.elapsedRealtime() + HOUR_MILLIS/4,
                    pendingIntent);
    	}
    }

}
