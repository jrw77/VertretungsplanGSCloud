package de.jweimar.android.vertretungsplan;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author weimar
 * 
 */
public class InfoScreen extends Activity {

	// private static final String TAG = "vertretungsplan.Info";

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		if (!this.getIntent().hasCategory("FROM_MENU")
				&& !PersistentData.firstTime(this)) {
			launchConfigure();
			finish();
		}

		// Set the view layout resource to use.
		setContentView(R.layout.infoscreen);

		// Add Version to Title
		try {
			this.setTitle(this.getTitle()
					+" Version "
					+getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		// put my ID in extra textview.
		// get the uniqueID from the extras 
		String uniqueID = this.getIntent().getStringExtra(CodeEntry.UNIQUE_ID);
		TextView myID = findViewById(R.id.myID);
		if (myID != null && uniqueID != null) {
			String text = " Meine ID: "+uniqueID;
			String gcmID = this.getIntent().getStringExtra(CodeEntry.GCM_ID);
			if (gcmID != null){
				text += "\n Google cloud messaging ID: "+gcmID;
			}
			myID.setText(text);
		}

		// The following somehow makes sure that the widget appears on the
		// widget list
		// in android >= 4.0
		sendBroadcast(new Intent(Intent.ACTION_MAIN)
				.addCategory(Intent.CATEGORY_HOME));

		Button ok = findViewById(R.id.OKInfoButton);
		if (ok != null) {
			ok.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					finish();
				}

			});
		}

		Button direct = findViewById(R.id.DirectButton);
		if (direct != null) {
			if (this.getIntent().hasCategory("FROM_MENU")) {
				// If this is already started from the Configure Activity: disable this button!
				direct.setVisibility(View.INVISIBLE);
			} else {
				direct.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						launchConfigure();
						finish();
					}

				});
			}
		}

	}

	/**
	 * launch the Configure Activity.
	 */
	public void launchConfigure() {
		Intent configure = new Intent();
		configure.setClass(InfoScreen.this.getBaseContext(), Configure.class);
		configure.setData(Uri.fromParts("dummy", "nowidget", ""
				+ PersistentData.DUMMY_APPWIDGET_ID));
		startActivity(configure);
	}
}
