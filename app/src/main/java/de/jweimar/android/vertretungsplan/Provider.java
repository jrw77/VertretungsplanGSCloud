package de.jweimar.android.vertretungsplan;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Debug;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.RemoteViews;

public class Provider extends AppWidgetProvider {

	/*
	 * Original Example code Copyright (C) 2008 The Android Open Source Project
	 * 
	 * Licensed under the Apache License, Version 2.0 (the "License"); you may
	 * not use this file except in compliance with the License. You may obtain a
	 * copy of the License at
	 * 
	 * http://www.apache.org/licenses/LICENSE-2.0
	 * 
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
	 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
	 * License for the specific language governing permissions and limitations
	 * under the License.
	 */

	/**
	 * A widget provider. We have a string that we pull from a preference in
	 * order to show the configuration settings and the current time when the
	 * widget was updated.
	 * 
	 */
	// log tag
	private static final String TAG = "vertretungsplan.Provid";
	static final boolean DEBUG = false;
	private static final int INTERVAL = 1800;

	private static SparseArray< String> aktuelleVertretung = new SparseArray<>();
	
	// private AppWidgetManager appWidgetManager;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		onUpdate(context, appWidgetIds, true);
	}
		
	public void onUpdate(Context context,
			int[] appWidgetIds, boolean separateTask) {
		if (DEBUG) {
			Log.d(TAG, "onUpdate");
		}
		if (DEBUG)
			Debug.waitForDebugger();

		// For each widget that needs an update, get the text that we should
		// display:
		// - Create a RemoteViews object for it
		// - Set the text in the RemoteViews object
		// - Tell the AppWidgetManager to show that views object for the widget.
		for (int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetId, separateTask);
		}
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		if (DEBUG)
			Log.d(TAG, "onDeleted");
		// When the user deletes the widget, delete the preference associated
		// with it.
		for (int appWidgetId: appWidgetIds) {
			PersistentData.deletePrefs(context, appWidgetId);
		}
	}

	@Override
	public void onEnabled(Context context) {
		if (DEBUG)
			Log.d(TAG, "onEnabled");

		// PackageManager pm = context.getPackageManager();
		// pm.setComponentEnabledSetting(new ComponentName(
		// "de.jweimar.android.vertretungsplan",
		// ".appwidget.BroadcastReceiver"),
		// PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
		// PackageManager.DONT_KILL_APP);
	}

	static void updateAppWidget(Context context, int appWidgetId, boolean separateTask) {
		Log.i(TAG, "updateAppWidget appWidgetId=" + appWidgetId);
		if (separateTask){
			// To prevent any ANR timeouts, we perform the update in an async task
			new UpdateTask().execute(new TaskParameters(context, appWidgetId));
		}else{
			UpdateTask uTask = new UpdateTask();
			RemoteViews rViews = uTask.doInBackground(new TaskParameters(context, appWidgetId));
			uTask.onPostExecute(rViews);
		}
	}

	private static class TaskParameters {
		Context context;
		int widgetId;

		TaskParameters(Context context, int widgetId) {
			this.context = context;
			this.widgetId = widgetId;
		}
	}

	private static class UpdateTask extends
			AsyncTask<TaskParameters, Void, RemoteViews> {

		TaskParameters tp;

		@Override
		protected RemoteViews doInBackground(TaskParameters... params) {
			PersistentData myData = PersistentData.getPersistentData(
					params[0].context, params[0].widgetId);
			tp = params[0];

			long now = System.currentTimeMillis() / 1000;
			if (myData.urlStart == null || myData.klasse == null) {
				// Noch nicht konfiguriert
				return null;
			}
			if (myData.paused) {
				// Build update with indication "paused".
				return buildUpdate(params[0].context, params[0].widgetId,
						myData, true);
			}
			if (!DEBUG && myData.lastUpdate + INTERVAL > now) {
				Log.d(TAG, " last update was only " + (now - myData.lastUpdate)
						/ (60.0) + "min ago.");
				return null;
			}

			// Build the widget update for today
			RemoteViews updateViews = buildUpdate(params[0].context,
					params[0].widgetId, myData, false);
			if (DEBUG)
				Log.d(TAG,  "widget "+params[0].widgetId+" update built klasse = " + myData.klasse);

			// Push update for this widget to the home screen
			
			if (DEBUG)
				Log.d(TAG, "widget "+params[0].widgetId+" updated");
			return updateViews;
		}

		// protected void onProgressUpdate(Integer... progress) {
		// }

		protected void onPostExecute(RemoteViews result) {
			if (result != null) {
				setWidget(result, tp);
			}
		}

	}

	/**
	 * Set the results.
	 */
	static void setWidget(RemoteViews result, TaskParameters tp) {
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(tp.context);
		appWidgetManager.updateAppWidget(tp.widgetId, result);
	}

	/**
	 * Erzeuge ein Widget Update, um den aktuellen Vertretungsplan im Widget
	 * anzuzeigen. Der Aufruf hier blockiert, sollte daher in einem separaten
	 * Thread ausgefürht werden.
	 */
	static public RemoteViews buildUpdate(Context context, int appWidgetId,
			PersistentData mydata, boolean paused) {
		// Let the UntisConnector get the most recent value.
		UntisConnector uc = UntisConnector.getInstance(context, mydata);

		String[] vertretungsTexte = uc.getCurrent();

		// save the preferences!
		mydata.savePrefs(context, appWidgetId);

		// Construct the RemoteViews object. It takes the package name (in
		// our case, it's our
		// package, but it needs this because on the other side it's the
		// widget host inflating
		// the layout from our package).
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.vertretungsplan_appwidget);

		Intent configureIntent = createConfigureIntent(context, appWidgetId);

		views.setOnClickPendingIntent(R.id.widgetText, PendingIntent
				.getActivity(context, 0, configureIntent,
						PendingIntent.FLAG_UPDATE_CURRENT));
		views.setOnClickPendingIntent(R.id.widgetTextAllgemein, PendingIntent
				.getActivity(context, 0, configureIntent,
						PendingIntent.FLAG_UPDATE_CURRENT));

		views.setOnClickPendingIntent(R.id.widgetFrame, PendingIntent
				.getActivity(context, 0, configureIntent,
						PendingIntent.FLAG_UPDATE_CURRENT));

		if (paused) {
			views.setTextViewText(R.id.widgetText, "Pausiert");

			views.setInt(R.id.widgetTextAllgemein, "setVisibility", View.GONE);
			views.setTextViewText(R.id.widgetTitle, mydata.klasse);
			aktuelleVertretung.put(appWidgetId, "Pausiert");

		} else {
			if (vertretungsTexte[0] == null) {
				Log.w(TAG, "text null");
				vertretungsTexte[0] = "Konfigurieren";
			}
			views.setTextViewText(R.id.widgetText, vertretungsTexte[0]);
			
			aktuelleVertretung.put(appWidgetId, vertretungsTexte[0]);

			if (vertretungsTexte[1] != null && !"".equals(vertretungsTexte[1])) {
				views.setTextViewText(R.id.widgetTextAllgemein,
						vertretungsTexte[1]);
				views.setInt(R.id.widgetTextAllgemein, "setVisibility",
						View.VISIBLE);
			} else {
				views.setInt(R.id.widgetTextAllgemein, "setVisibility",
						View.GONE);
			}

			views.setTextViewText(R.id.widgetTitle, mydata.klasse + " : "
					+ vertretungsTexte[2]);
		}
		return views;
	}
	/**
	 * Erzeuge ein Widget Update, um den aktuellen Vertretungsplan im Widget
	 * anzuzeigen. Der Aufruf hier blockiert, sollte daher in einem separaten
	 * Thread ausgefürht werden.
	 */
	static public RemoteViews initialViews(Context context, int appWidgetId) {

		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.vertretungsplan_appwidget);

		Intent configureIntent = createConfigureIntent(context, appWidgetId);

		views.setOnClickPendingIntent(R.id.widgetText, PendingIntent
				.getActivity(context, 0, configureIntent,
						PendingIntent.FLAG_UPDATE_CURRENT));
		views.setOnClickPendingIntent(R.id.widgetTextAllgemein, PendingIntent
				.getActivity(context, 0, configureIntent,
						PendingIntent.FLAG_UPDATE_CURRENT));

		views.setOnClickPendingIntent(R.id.widgetFrame, PendingIntent
				.getActivity(context, 0, configureIntent,
						PendingIntent.FLAG_UPDATE_CURRENT));

		return views;
	}

	/** Cretaes a configure intent for this app widget.
	 * @param context The current context of the app widget, is it the same as for the App?
	 * @param appWidgetId Which app widget ID is this for? Relevant for multiple Widgets.
	 * @return An Intent for starting the Configure Activity.
	 */
	public static Intent createConfigureIntent(Context context, int appWidgetId) {
		Intent configureIntent = new Intent(Intent.ACTION_EDIT, Uri.fromParts(
				"content", "de.jweimar.android.vertretungsplan/", ""
						+ appWidgetId), context, Configure.class);

		configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				appWidgetId);
		return configureIntent;
	}

	public static void initialWidget(Context context, int appWidgetId) {
		Provider.TaskParameters tp = new TaskParameters(context,appWidgetId);
		
		Provider.setWidget(Provider.initialViews(context, appWidgetId), tp);
		
	}
	
	public static String aktuellerWert(int appWidgetId){
		return Provider.aktuelleVertretung.get(appWidgetId);
	}
}
