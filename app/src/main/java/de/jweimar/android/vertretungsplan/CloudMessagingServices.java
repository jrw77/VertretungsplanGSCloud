package de.jweimar.android.vertretungsplan;

import java.io.IOException;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class CloudMessagingServices {

    GoogleCloudMessaging gcm;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final String LOG_TAG = CloudMessagingServices.class.getCanonicalName();
	
    Activity activity;
    String SENDER_ID = "88908202967";
    PersistentData mydata;

    public CloudMessagingServices(Activity a){
    	activity = a;
    	mydata = PersistentData.getPersistentData(a, PersistentData.DUMMY_APPWIDGET_ID);
    }

    public void checkAndRegister(){
    	// Check device for Play Services APK. If check succeeds, proceed with GCM registration.
    	if (checkPlayServices()) {
    		gcm = GoogleCloudMessaging.getInstance(activity);

    		mydata = PersistentData.getPersistentData(activity, PersistentData.DUMMY_APPWIDGET_ID);

    		Log.i(LOG_TAG,"checkAndRegister(): registrationID="+mydata.registrationID);

    		// TODO: Check if App version changed, reregister if necessary.
    		// Reason: Google might have sent a GCM message during update, which will have resulted in 
    		// the invalidation of the registration ID (JRW)
            int registeredVersion = mydata.appVersion;
            int currentVersion = PersistentData.getAppVersion(activity);
            if (registeredVersion != currentVersion) {
                Log.i(LOG_TAG, "App version changed.");
                registerNewlyInBackground();
            }else if (mydata.registrationID == null) {
        		Log.i(LOG_TAG,"checkAndRegister(): must register!");
        		registerNewlyInBackground();
    		}else{
        		Log.i(LOG_TAG,"checkAndRegister(): was OK, registrationID="+mydata.registrationID);
        		if (mydata.code != null && !mydata.code.equals(mydata.registrationCode)){
        			sendRegisterInBackground();
        		}
        		// TODO:  reregister more often?
    		}

    	} else {
    		Log.w(LOG_TAG, "No valid Google Play Services APK found.");
            mydata.registrationLastFail = System.currentTimeMillis();
            mydata.registrationDeltaRetry = Math.min(1000L*60*60,mydata.registrationDeltaRetry*2); // min 1 hour
            mydata.saveRegistrationID(activity);
    	}
    }

	// Dies folgende ist für Google Cloud messaging
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     * Copied from GCM Demo Project
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
            	if (System.currentTimeMillis() < mydata.registrationLastFail+mydata.registrationDeltaRetry){
             		Log.w(LOG_TAG,"Not Displaying Error due to exponential backoff");
            	}
            	GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(LOG_TAG, "Google Cloud Messaging on this device is not supported.");
                // nicht so schlimm, wird einfach nicht so oft aktualisiert.
            }
            return false;
        }
        return true;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void sendRegisterInBackground() {
        new RegisterTask().execute(mydata);
    }

    static class RegisterTask extends AsyncTask<PersistentData, Void, String> {
        @Override
        protected String doInBackground(PersistentData... params) {
            PersistentData refToData = params[0];
            String msg = "";
            msg += sendRegistrationIdToBackend(refToData);
            params[0].registrationCode = params[0].code; // record the code we registered for
            return msg;
        }

        @Override
        protected void onPostExecute(String msg) {
            Log.i(LOG_TAG, msg);
            // Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
        }
    }
    private static String sendRegistrationIdToBackend(PersistentData data) {
        return UntisConnector.getAsString(
                "http://m.andapp.de/Vertretungsplan/register.php?"
                        +"secret=oskarsommer&"
                        +"code="+data.code+"&"
                        +"uniqueid="+data.uniqueID+"&"
                        +"gcmid="+data.registrationID
        );
    }



    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerNewlyInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                	if (System.currentTimeMillis() < mydata.registrationLastFail+mydata.registrationDeltaRetry){
                 		return "Not re-registering due to exponential backoff";
                	}
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(activity);
                    }
                    mydata.registrationID = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + mydata.registrationID+"\n";

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    msg += sendRegistrationIdToBackend(mydata);

                    // Persist the regID - no need to register again.
                    mydata.registrationLastFail = 0; //recently: success!
                    mydata.registrationDeltaRetry = 1000L*60*1; // 1 minutes default
                    mydata.saveRegistrationID(activity);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage()+" ";
                    msg += mydata.registrationID+"\n";
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                    mydata.registrationLastFail = System.currentTimeMillis();
                    mydata.registrationDeltaRetry = mydata.registrationDeltaRetry*2;
                    mydata.saveRegistrationID(activity);
                }
                return msg;
            }

 
			@Override
            protected void onPostExecute(String msg) {
                Log.i(LOG_TAG, msg);
                // Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
            }
        }.execute(null, null, null);
    };

    /**
     * TODO: does not work like this. The problem is the following: The application has registered 
     * twice with different uniqueIDs, but only knows the second uniqueID. The server does not know 
     * that two uniquIDs refer to the same device. Way around: use Device ID instead of newly generated unique ID.
     * 
     * TODO: also not yet implemented on the server.
     * @return
     */
    public String sendUnRegistrationIdToBackend() {
    	return UntisConnector.getAsString(
    			"http://m.andapp.de/Vertretungsplan/unregister.php?"
    			    +"secret=oskarsommer&"
        			+"code="+mydata.code+"&"
    				+"keepuniqueid="+mydata.uniqueID+"&"
    				+"gcmid="+mydata.registrationID
    			);	
	}



}
