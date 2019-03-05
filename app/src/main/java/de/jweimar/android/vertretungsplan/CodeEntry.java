package de.jweimar.android.vertretungsplan;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

/**
 * This activity displays a dialog to enter a code, which is then used to retrieve the URL for the local
 * untis system.
 * @author weimar
 *
 */
public class CodeEntry extends Activity {
	static final String CODE_RESULT = "de.jweimar.android.vertretungsplan.CODE_RESULT";

	static final int DIALOG_CODE_ENTRY = 0x12;

	public static final String UNIQUE_ID = "uniqueId";
	public static final String OLD_CODE = "oldCode";
	public static final String GCM_ID = "gcmId";

	static final String TAG = "vertretungsplan.CodeEnt";

	protected static final int CODE_LENGTH = 10;


	String uniqueId;			
	String oldCode;			
	EditText codeField;		// text field for code entry
	ProgressBar pb;			// rotating Wait-symbol

	private String codeString;
	
    // Whether there is a Wi-Fi connection.
    private static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    private static boolean mobileConnected = false;



	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Provider.DEBUG) Debug.waitForDebugger();

		// get the uniqueID from the extras
		uniqueId = this.getIntent().getStringExtra(UNIQUE_ID);

		// get the uniqueID from the extras
		oldCode = this.getIntent().getStringExtra(OLD_CODE);

		// Set the default result to CANCELED.
		setResult(RESULT_CANCELED);

		// Set the view layout resource to use.
		setContentView(R.layout.coderequest);

		// prepare all content, also crates all the listeners
		prepareCodeField();
		prepareOkButton();
		prepareCancelButton();

		pb = this.findViewById(R.id.progressBar1);

	}

	/**
	 * Prepare the codeField and create the listener for text edit actions (assume editing complete).
	 */
	private void prepareCodeField() {
		codeField = findViewById(R.id.codeInputField);
		// set text to previous code
		if (oldCode != null && !oldCode.isEmpty()){
			codeField.setText(oldCode);
		}
		codeField.setOnEditorActionListener(new OnEditorActionListener(){
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				checkCode();
				return true;
			}
		});
	}

	/**
	 * Prepare the okButton and create the listener.
	 */
	private void prepareOkButton() {
		Button ok_button = findViewById(R.id.code_entry_ok_button);

		ok_button.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				checkCode();
			}
		});
	}

	/**
	 * Prepare the  cancelButton and create the listener, which returns "Cancel"-intent to the calling activity
	 */
	private void prepareCancelButton() {
		Button cancel_button = findViewById(R.id.code_entry_cancel_button);
		cancel_button.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View view) {
				/* User clicked cancel so do some stuff */
				// in this case: finish the whole activity, which does not add
				// the widget,
				
				Intent resultIntent = new Intent(CODE_RESULT);
				setResult(Activity.RESULT_CANCELED, resultIntent);
				CodeEntry.this.finish();
			}
		});
	}
	
	/**
	 * 
	 */
	private void checkCode() {
		/* User clicked OK so do some stuff */
		// first check whether network is connected.
		updateConnectedFlags();
		if (!(wifiConnected || mobileConnected)){
			Toast.makeText(CodeEntry.this,
					"Keine Internetverbindung verfügbar!",
					Toast.LENGTH_LONG).show();
			return;
		}
		if (codeField == null) {
			Log.e(TAG, "codeField == null");
			return;
		}
		Editable code = codeField.getText();
		if (code == null || code.length() != CODE_LENGTH) {
			Log.e(TAG, "codeField.getText() == null");
			Toast.makeText(CodeEntry.this,
					"Der Code muss " + CODE_LENGTH + " Zeichen haben",
					Toast.LENGTH_SHORT).show();
			// showDialog(DIALOG_CODE_ENTRY);
			return;
		}
		codeString = code.toString();
		new NetworkTask().execute(code.toString());
		
	}

    // Checks the network connection and sets the wifiConnected and mobileConnected
    // variables accordingly.
    private void updateConnectedFlags() {
	    // set defaults
        wifiConnected = false;
        mobileConnected = false;
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr == null){
            return;
        }
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        } else {
            wifiConnected = false;
            mobileConnected = false;
        }
    }


	/**
	 * get the start URL from m.andapp.de in the background.
	 *
	 */
	@SuppressLint("StaticFieldLeak")
	private class NetworkTask extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... code) {
			return  UntisConnector.getAsString(
					"http://m.andapp.de/Vertretungsplan/codeToUrl.php?code="
					+ code[0] + "&id=" + uniqueId);
	     }
		/** set the progress bar to visible
		 */
		protected void onPreExecute() {
			pb.setVisibility(View.VISIBLE);
	     }
		
		protected void onPostExecute(String result) {
			pb.setVisibility(View.INVISIBLE);

			if (result == null) {
				Toast.makeText(CodeEntry.this, "Der Code ist ungültig",
						Toast.LENGTH_SHORT).show();
				Log.e(TAG," Result from codeToUrl.php null");
			}else if (!result.startsWith("http")) {
				Toast.makeText(CodeEntry.this, result, Toast.LENGTH_LONG).show();
				Log.e(TAG," Result from codeToUrl.php: "+result);
			}else{
				Intent resultIntent = new Intent(CODE_RESULT);
				resultIntent.putExtra("url", result);
				resultIntent.putExtra("code", codeString);
				setResult(Activity.RESULT_OK, resultIntent);
				finish();
			}
	     }
	}
	

}