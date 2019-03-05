package de.jweimar.android.vertretungsplan;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// import android.support.v7.app.ActionBarActivity;

public class Configure extends Activity {
	 // extends ActionBarActivity 
	// http://android-developers.blogspot.de/2013/08/actionbarcompat-and-io-2013-app-source.html
	// Solved!
	
	private static final long VIERZEHNTAGEINMILLISECONDS = 1000L * 3600 * 24 * 14;
    		
	static final String TAG = "vertretungsplan.Configu";
	private static final long THIRTY_MINUTES = 1000L * 60 * 30;

	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	WebView webView;
	Button pause;
	
	PersistentData mydata;

	ProgressBar pb;
	boolean klassenNamenHabenSichGeaendert = false;
	
	int weeksForward = 0;
	String angezeigteKlasse;
	long lastReload = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Provider.DEBUG)
			Debug.waitForDebugger();

		// Set the result to CANCELED. This will cause the widget host to cancel
		// out of the widget placement if they press the back button.
		setResult(RESULT_CANCELED);
 

		// Let's display the progress in the activity title bar, like the
		// browser app does.
		getWindow().requestFeature(Window.FEATURE_PROGRESS);

		// Set the view layout resource to use.
		setContentView(R.layout.main);

		pb = this.findViewById(R.id.progressBar2);

		// Initialize the WebView
		webView = findViewById(R.id.webview);
		WebSettings webSettings = webView.getSettings();
		if (webSettings != null){
			webSettings.setBuiltInZoomControls(true);
			webSettings.setBlockNetworkImage(true);
			webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
			webSettings.setJavaScriptEnabled(false);
		}else{
			Log.i("Configure","WebView: no WebSettings object!");
		}
		webView.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.w(TAG, "Webview möchte laden: " + url);
				return true; // Meaning: request has been handled.
			}
		});
		
		final Activity activity = this;

		webView.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
				// Activities and WebViews measure progress with different
				// scales.
				// The progress meter will automatically disappear when we reach
				// 100%
				activity.setProgress(progress * 1000);
				if (progress >= 100){
					pb.setVisibility(View.INVISIBLE);
				}
			}
		});

		Spinner klassenAuswahl = findViewById(R.id.KlasseSpinner);
		
		klassenAuswahl.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,
					int arg2, long arg3) {
				Log.i(TAG,"KlassenAuswahl pos"+arg2+" View "+view);
				if (klassenNamenHabenSichGeaendert){
					updateKlassenAuswahlSpinner();
					klassenNamenHabenSichGeaendert = false;
				}
				updateKlasse(view);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// do nothing
			}

		});
		
		// connect buttons for early versions of Android
		{
			pause = findViewById(R.id.buttonPause);
			pause.setOnClickListener(new OnClickListener(){
	
				@Override
				public void onClick(View view) {
					Configure.this.onVacation(view);
				}			
			});
		}

		{
			Button ok = findViewById(R.id.OKButton);
			ok.setOnClickListener(new OnClickListener(){
	
				@Override
				public void onClick(View view) {
					Configure.this.onOK(view);
				}			
			});
		}

		{
			Button next = findViewById(R.id.NextButton);
			next.setOnClickListener(new OnClickListener(){
	
				@Override
				public void onClick(View view) {
					Configure.this.onNext(view);
				}			
			});
		}
		weeksForward = 0;
		Intent intent = getIntent();
		String fragment = null;
		if (intent.getData() != null) { // for initial configuration, no data is
										// provided!
			fragment = intent.getData().getEncodedFragment();
		}
		if (fragment != null) { // when called from PendigIntent.
			mAppWidgetId = Integer.parseInt(fragment);
			Log.d(TAG, "onResume() called with fragment. Obj: " + this + " Id: " + mAppWidgetId);

		} else { // This is the case for initial configuration from widget host.
			Bundle extras = intent.getExtras();
			if (extras != null) {
				mAppWidgetId = extras.getInt(
						AppWidgetManager.EXTRA_APPWIDGET_ID,
						AppWidgetManager.INVALID_APPWIDGET_ID);
			} else{
				Log.d(TAG, "onResume() else else Obj: " + this + " Id: " + mAppWidgetId);
			}
		}

		// If they gave us an intent without the widget id, just bail.
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}else if (mAppWidgetId == PersistentData.DUMMY_APPWIDGET_ID){
			// not called from addpwidget.
			// disable "Pause Button"
			Button pause = findViewById(R.id.buttonPause);
			pause.setVisibility(View.INVISIBLE);
		}


		// Set a fallback View with a pending Intent in case anything goes wrong here.
		Provider.initialWidget(this,mAppWidgetId);
		
		mydata = PersistentData.getPersistentData(Configure.this, mAppWidgetId);
		
		Log.d(TAG, "onResume() Obj: " + this + " Id: " + mAppWidgetId
				+ " Klasse:" + mydata.klasse + " url: " + mydata.urlStart);

		setPauseDrawable();

		updateKlassenAuswahlSpinner();
		

	} // end onCreate();

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onStart() {
		super.onStart();

		// check update of Klassenliste
		if (mydata.klassenNamenBenoetigenUpdate && 
				System.currentTimeMillis() > mydata.lastKlassenNamenUpdate+VIERZEHNTAGEINMILLISECONDS  ){
			new KlassenNamenLadeTask().execute((Void[])null);
		}
		
		// check whether we have a startUrl, if not, request the secret code
		// and get the URL.
		if (mydata.urlStart == null) {
			startCodeEntry();
		}else{
			if (mydata.registrationID == null){
				// First time with cloud messaging, need to upgrade correctly.
				startCodeEntry();
			}
		}
		
		if (mydata.klasse != null && mydata.urlStart != null && ! mydata.klasse.equals(angezeigteKlasse)) {
			Log.d(TAG, "onStart()+ Angezeigt: " + angezeigteKlasse + " Gewünscht: "+mydata.klasse);			
			startLoadUrl();
		} else if (mydata.klasse != null && mydata.urlStart != null 
					&& System.currentTimeMillis() > lastReload + THIRTY_MINUTES) {
			Log.d(TAG, "onStart()+ Angezeigt: " + angezeigteKlasse + " Gewünscht: "+mydata.klasse);			
			startLoadUrl();
		} else {
			Log.d(TAG, "onStart()+ Kein Update nötig " + mydata.klasse);			
		}
		checkCloudMessaging();
	}
	


	/**
	 * Save Preferences when Activity is stopped.
	 */
	public void onStop(){
		super.onStop();
		mydata.savePrefs(getBaseContext(), mydata.widgetId);
	}
    
	/**
	 * return position in spinner for name.
	 * @param klasse Welcher Name der Klasse?
	 * @return position Position in der Liste
	 */
	private int getKlassePosition(String klasse) {
		if (klasse == null)
			return 1;
		// cannot assume sorted order, N < 150 is assumed.
		if (mydata.klassenName != null && mydata.klassenName.length > 1) {
			for (int i = 0; i < mydata.klassenName.length; i++) {
				if (klasse.equals(mydata.klassenName[i])) {
					Log.i(TAG,"getKlassePosition("+klasse+") = "+i);
					return i;
				}
			}
		}
		return 1;
	}

	/**
	 * Update webview when Klasse changes
	 * @param view The Spinner which initiated the change
	 */
	protected void updateKlasse(View view) {
		if (mydata == null){
			mydata = PersistentData.getPersistentData(Configure.this, mAppWidgetId);
		}
		CharSequence cs = null;
		if (view != null){
			cs = ((TextView) view).getText();
		}
		if (cs != null){
			mydata.klasse = cs.toString();
			if (!angezeigteKlasse.equals(mydata.klasse)){
				startLoadUrl();
			}
		}
	}


	/**
	 * Update the Spinner adapter for KlassenAuswahl.
	 */
	void updateKlassenAuswahlSpinner() {
		Spinner klassenAuswahl = findViewById(R.id.KlasseSpinner);

		if (mydata.klassenName != null ) {
			ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
					R.layout.simple_spinner_item, mydata.klassenName);
			klassenAuswahl.setAdapter(adapter);
		}
		klassenAuswahl.setSelection(getKlassePosition(mydata.klasse));
	}

	/**
	 * Lauch the code entry activity
	 */
	void startCodeEntry() {
		Intent codeRequest = new Intent();
		codeRequest.setClass(this.getBaseContext(), CodeEntry.class);
		codeRequest.putExtra(CodeEntry.UNIQUE_ID, mydata.uniqueID);
		codeRequest.putExtra(CodeEntry.OLD_CODE, mydata.code);
		mydata.klassenNamenBenoetigenUpdate = true;
		startActivityForResult(codeRequest, CodeEntry.DIALOG_CODE_ENTRY);
	}

	/**
	 * Overridden to allow "Back" button to go back in the WebView instead of
	 * quitting the Widget Configuration
	 * 
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
			webView.goBack();
			if (weeksForward > 0){
				weeksForward --;
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * get the start URL from m.andapp.de in the background.
	 *
	 */
	private void startLoadUrl(){
		// new UrlLadeTask().execute(new Integer[]{weeksForward}); not allowed in API level 11+!
		// must be on UI thread!
		loadURL(weeksForward);
		angezeigteKlasse = mydata.klasse;
		lastReload = System.currentTimeMillis();
	}
//	private class UrlLadeTask extends AsyncTask<Integer, Void, Void> {
//		protected Void doInBackground(Integer... url) {
//			loadURL(weeksForward);
//			return null;
//	     }
//		/** set the progress bar to visible
//		 */
//		protected void onPreExecute() {
//			pb.setVisibility(View.VISIBLE);
//	     }
//
//		protected void onPostExecute(Void none) {
//			pb.setVisibility(View.INVISIBLE);
//	     }
//	}

	
	/**
	 * load the URL and display the result in the WebView, possibly for next
	 * week.
	 */
	protected void loadURL(int weeksForward) {
		
		pb.setVisibility(View.VISIBLE);
		
		UntisConnector uc = UntisConnector.getInstance(this, mydata);
		uc.constructUrl(weeksForward);

		webView.loadUrl(uc.url);
	}

	/**
	 * load class names from the Untis frame header in the background.
	 *
	 */
	@SuppressLint("StaticFieldLeak")
	private class KlassenNamenLadeTask extends AsyncTask<Void, Void, Void> {
		protected Void doInBackground(Void... url) {
			UntisConnector uc = UntisConnector.getInstance(Configure.this, mydata);
			Log.d(TAG,"Klassennamen werden neu geladen");
			uc.ladeKlassenNamen(Configure.this);
			klassenNamenHabenSichGeaendert = true;
			return null;
	     }
		/** set the progress bar to visible
		 */
		protected void onPreExecute() {
			pb.setVisibility(View.VISIBLE);
	     }
		
		protected void onPostExecute(Void none) {
			pb.setVisibility(View.INVISIBLE);
			updateKlassenAuswahlSpinner(); // Update spinner based on the result.

	     }
	}

	/**
	 * When the OK button is clicked, check whether find was successful, then
	 * get the title and store everything persistently, then schedule the
	 * updates.
	 * 
	 * @param view View form which event originated
	 */
	public void onOK(View view) {
		final Context context = Configure.this;

		// Toast.makeText(this, "On OK", Toast.LENGTH_SHORT).show();

		if (klassenNamenHabenSichGeaendert){
				updateKlassenAuswahlSpinner();
				klassenNamenHabenSichGeaendert = false;
				Toast.makeText(context, "Klassennamen wurden aktualisiert", Toast.LENGTH_SHORT).show();
				return;
		}
		
		mydata.paused = false;

		mydata.savePrefs(context, mAppWidgetId);

		// Log.d(TAG,"Data saved");

		// only save if called as "Configure"
		if (mAppWidgetId != PersistentData.DUMMY_APPWIDGET_ID){
			// Push widget update to surface with newly set data
			// Later, this will be called regularly from the AppWidget Host, but now
			// we have to do it directly.
			Provider.updateAppWidget(context, mAppWidgetId, true);

			// TODO where do we set the update interval?
		}
		
		// Make sure we pass back the original appWidgetId
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);
		finish();

	}

	/**
	 * When the Next button is clicked, load the data for next week.
	 * 
	 * @param view Vie wfrom which event originated
	 */
	public void onNext(View view) {
		weeksForward += 1;
		startLoadUrl();
	}
	
	/**
	 * set the drawable for the pause button to the appropriate state.
	 */
	private void setPauseDrawable(){
		final Context context = Configure.this;

		if (mydata.paused){  
			pause.setBackground(
					context.getResources().getDrawable(android.R.drawable.ic_media_play));
		}else{  
			pause.setBackground(
					context.getResources().getDrawable(android.R.drawable.ic_media_pause));
		}
	
	}
	/**
	 * When the Pause button is clicked, toggle the paused state and save the data.
	 * 
	 * @param view View from which the event comes
	 */
	public void onVacation(View view) {
		final Context context = Configure.this;

		mydata.paused = !mydata.paused;
		
		mydata.savePrefs(context, mAppWidgetId);
		// Log.d(TAG,"Data saved");

		if (mAppWidgetId != PersistentData.DUMMY_APPWIDGET_ID){
			// Push widget update to surface with newly set data
			// Later, this will be called regularly from the AppWidget Host, but now
			// we have to do it directly.
			Provider.updateAppWidget(context, mAppWidgetId, true);

		}
		// Make sure we pass back the original appWidgetId
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);
		finish();
	}

	/**
	 * Process result of the Code request activity
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (Provider.DEBUG)
			Debug.waitForDebugger();

		// Check which request we're responding to
		if (requestCode == CodeEntry.DIALOG_CODE_ENTRY) {
			// Make sure the request was successful
			if (resultCode == Activity.RESULT_OK) {
				mydata.urlStart = data.getStringExtra("url");
				mydata.code = data.getStringExtra("code");
				mydata.savePrefs(getBaseContext(), mydata.widgetId);
				
				new KlassenNamenLadeTask().execute((Void[])null);
				checkCloudMessaging();
				updateKlassenAuswahlSpinner();
				startLoadUrl();  
			} else if (resultCode == Activity.RESULT_CANCELED) {
				// Display some information about the app instead
				Intent infoRequest = new Intent();
				infoRequest.setClass(this.getBaseContext(), InfoScreen.class);
				infoRequest.addCategory("FROM_MENU"); // To avoid endless loop!
				startActivity(infoRequest);
				finish();
			}
		}
	}

	/**
	 * create Menu from XML.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.options_menu, menu);
	    return true;
	}

	/**
	 * What to do when a menu item is selected
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.ABOUT_ID:
			// Launch the Info activity through this intent  (no result)
			Intent launchAboutIntent = new Intent(Intent.ACTION_VIEW).setClass(
					this, InfoScreen.class).addCategory("FROM_MENU");
			launchAboutIntent.putExtra(CodeEntry.UNIQUE_ID, mydata.uniqueID);
			launchAboutIntent.putExtra(CodeEntry.GCM_ID, mydata.registrationID);

			startActivity(launchAboutIntent);
			return true;
		case R.id.SCHULWECHSEL_ID:
			// launch code entry activity
			startCodeEntry();
			return true;
			
		case R.id.ENTSCHULDIGUNG_ID:
			// When the button is clicked, launch an activity through this
			// intent
			Intent launchEntschuldigungIntent = new Intent(Intent.ACTION_EDIT).setClass(
					this, Entschuldigung.class);
			startActivity(launchEntschuldigungIntent);
			return true;

		case R.id.playstore_menu:
		{// share the playstore ID
			Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=de.jweimar.android.vertretungsplan");
			sendIntent.setType("text/plain");
			startActivity(sendIntent);
		}
		return true;
		case R.id.passwortweiter_menu:
		{	// share the playstore ID
			if (mydata != null && mydata.code != null){
				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_TEXT, mydata.code);
				sendIntent.setType("text/plain");
				startActivity(sendIntent);
			}else{
				Toast.makeText(this.getBaseContext(), "Code nicht gespeichert", Toast.LENGTH_LONG).show();
			}
		}
		return true;
		case R.id.INETmenu_ID:
		{	// share open INet direct access in browser
				Uri webpage = Uri.parse("http://inet"+(mydata.widgetId%10)+".andapp.de/inet.html");
				Intent webIntent = new Intent(Intent.ACTION_VIEW, webpage);
				startActivity(webIntent);
				finish();
		}
		return true;

		default: return super.onOptionsItemSelected(item);
		
		}
		
	}

	private void checkCloudMessaging() {
		Log.i(TAG,"Now init Cloud Messaging");
		CloudMessagingServices cms = new CloudMessagingServices(this);
		cms.checkAndRegister();		
	}

}