package de.jweimar.android.vertretungsplan;

import java.util.ArrayList;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.util.SparseArray;

public class PersistentData {

    private static final String PREF_KLASSE_KEY = "klasse_";
    private static final String PREF_TAG_KEY = "tag_";
    private static final String PREF_DATUM_KEY = "datum_";
    private static final String PREF_ALLE_KEY = "allgemein_";
    private static final String PREF_LASTUPDATE_KEY = "lastupdate_";
    private static final String PREF_URLSTART_KEY = "urlstart_";
    private static final String PREF_UNIQUEID_KEY = "uniqueID";
    private static final String PREF_PAUSED_KEY = "paused_";
    private static final String PREF_KLASSENARRAY_KEY = "klassenArray_";
    private static final String PREF_KLASSENNAMENBENOETIGENUPDATE_KEY = "klassenNamenBenoetigenUpdate_";
    private static final String PREF_LASTKLASSENNAMENUPDATE_KEY = "lastKlassenNamenUpdate_";
    private static final String PREF_CODE_KEY = "code_";
    private static final String PREF_REGISTRATIONID_KEY = "registration_id";
    private static final String PREF_REGISTRATION_LASTFAIL = "registration_lastfail";
    private static final String PREF_REGISTRATION_DELTARETRY = "registration_deltaretry";
    private static final String PREF_REGISTRATION_CODE = "registration_code";
    private static final String PREF_APP_VERSION = "appVersion";

    
    private static final String PREFS_NAME = "de.jweimar.android.vertretungsplan";
	private static final String TAG = "vertretungsplan.Persist";

    public static final int DUMMY_APPWIDGET_ID = 999999;

    
    protected String klasse;
    protected String[] tag;
    protected String[] datum;
    protected String[] allgemein;
    protected long lastUpdate;
    protected String urlStart;
    protected String uniqueID;
    protected boolean paused;
    protected String[] klassenName;
    protected String[] klassenNummer;
    protected boolean klassenNamenBenoetigenUpdate;
    protected long lastKlassenNamenUpdate;
    protected String code;
    protected String registrationID;

    protected long registrationLastFail;
    protected long registrationDeltaRetry;
    protected String registrationCode;

    protected int appVersion;

    // Context myContext; // Storing the context is dangerous!
    int widgetId;
    
    static SparseArray<PersistentData> cache = new  SparseArray<PersistentData>();
    
    static PersistentData getPersistentData(Context context, int widgetId){
    	PersistentData temp = cache.get(widgetId);
    	if (temp == null){
    		temp = new PersistentData(context,widgetId);
    	}
    	return temp;
    }
    
    /**
     * construct data element and fill with stored data if avaliable.
     */
    private PersistentData(Context context, int widgetId){
   	 	this.widgetId = widgetId; 
   	 	// myContext = context;
    	 loadPrefs(context, widgetId);
    	 cache.put(widgetId,this);
    }
    
    /**
     * return true if this is the fist time overal that this application runs.
     * @param context
     * @return
     */
    static boolean firstTime(Context context){
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_UNIQUEID_KEY, null) == null ;
    }
 
    /**
     * return true if this is the first time that the application runs with google cloud messaging code.
     * @param context
     * @return
     */
    static boolean firstTimeCloud(Context context){
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getString(PREF_REGISTRATIONID_KEY, null) == null ;
    }
   
    /**
     *  Write the data to the SharedPreferences object for this widget
     * @param context
     * @param appWidgetId
     */
    void savePrefs(Context context, int appWidgetId) {
        final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor prefsEd = prefs.edit();
        
        if (klasse != null) prefsEd.putString(PREF_KLASSE_KEY + appWidgetId, klasse);
        if (tag != null ){
         	for( int i=0; i<tag.length; i++){
        		prefsEd.putString(PREF_TAG_KEY + appWidgetId+"_"+i, tag[i]);
        	}      		
        }
        if (datum != null ){
         	for( int i=0; i<datum.length; i++){
        		prefsEd.putString(PREF_DATUM_KEY + appWidgetId+"_"+i, datum[i]);
        	}      		
        }
        if (allgemein != null ){
         	for( int i=0; i<allgemein.length; i++){
        		prefsEd.putString(PREF_ALLE_KEY + appWidgetId+"_"+i, allgemein[i]);
        	}      		
        }
      
        prefsEd.putLong(PREF_LASTUPDATE_KEY + appWidgetId, lastUpdate); 
 
        if (urlStart != null ){
        		prefsEd.putString(PREF_URLSTART_KEY + appWidgetId , urlStart);
        }


        prefsEd.putBoolean(PREF_PAUSED_KEY + appWidgetId, paused); 

        if (urlStart != null ){
    		prefsEd.putString(PREF_URLSTART_KEY + appWidgetId , urlStart);
        }

        prefsEd.putBoolean(PREF_KLASSENNAMENBENOETIGENUPDATE_KEY + appWidgetId, 
        		klassenNamenBenoetigenUpdate); 
        
        prefsEd.putLong(PersistentData.PREF_LASTKLASSENNAMENUPDATE_KEY + appWidgetId, 
        			this.lastKlassenNamenUpdate); 

        if (code != null ){
    		prefsEd.putString(PREF_CODE_KEY + appWidgetId , code);
            String mainCode = prefs.getString(PREF_CODE_KEY+DUMMY_APPWIDGET_ID,null);
            if (mainCode == null || mainCode.equals("")){
            	prefsEd.putString(PREF_CODE_KEY + DUMMY_APPWIDGET_ID , code);
            }
        }

        prefsEd.commit();
    }

    /**
     * load the data for this widget from the shared preferences
     * @param context
     * @param appWidgetId
     */
    public void loadPrefs(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        
        klasse = prefs.getString(PREF_KLASSE_KEY + appWidgetId, null);

        urlStart = prefs.getString(PREF_URLSTART_KEY + appWidgetId, null);
        if (urlStart == null){
        	urlStart = prefs.getString(PREF_URLSTART_KEY+DUMMY_APPWIDGET_ID,null);
        }
        
        uniqueID = prefs.getString(PREF_UNIQUEID_KEY, null);
        if (uniqueID == null){
        	// First time we start, therefore create the unique ID and save it immediately,
        	uniqueID = UUID.randomUUID().toString();
            SharedPreferences.Editor prefsEdit = prefs.edit();
            prefsEdit.putString(PREF_UNIQUEID_KEY , uniqueID);
            prefsEdit.commit();
        }

        registrationID = prefs.getString(PREF_REGISTRATIONID_KEY, null);

        String temp;
        ArrayList<String> al = new ArrayList<String>();
        int i=0;
        while ((temp= prefs.getString(PREF_TAG_KEY + appWidgetId+"_"+i, null)) != null){
        	al.add(temp);
        	i++;
        }
        if (al.size() > 0){
        	tag = new String[al.size()];
            tag = al.toArray(tag);
        }else{
    	   tag = new String[5];
        }
        al = new ArrayList<String>();
        i=0;
        while ((temp= prefs.getString(PREF_DATUM_KEY + appWidgetId+"_"+i, null)) != null){
        	al.add(temp);
        	i++;
        }
        if (al.size() > 0){
        	datum = new String[al.size()];
        	datum = al.toArray(datum);
        }else{
     	   datum = new String[5];
         }

        al = new ArrayList<String>();
        i=0;
        while ((temp= prefs.getString(PREF_ALLE_KEY + appWidgetId+"_"+i, null)) != null){
        	al.add(temp);
        	i++;
        }
        if (al.size() > 0){
        	allgemein = new String[al.size()];
        	allgemein = al.toArray(allgemein);
        }else{
      	    allgemein = new String[5];
        }

        
        lastUpdate = prefs.getLong(PREF_LASTUPDATE_KEY + appWidgetId, -1L); 
        paused = prefs.getBoolean(PREF_PAUSED_KEY + appWidgetId, false); 
        
        klassenNamenBenoetigenUpdate = prefs.getBoolean(PREF_KLASSENNAMENBENOETIGENUPDATE_KEY + appWidgetId, 
        		false); 
        
        lastKlassenNamenUpdate = prefs.getLong(PersistentData.PREF_LASTKLASSENNAMENUPDATE_KEY + appWidgetId, 
        		 -1L); 

        code = prefs.getString(PREF_CODE_KEY + appWidgetId, null);
        if (code == null || code.equals("")){
        	code = prefs.getString(PREF_CODE_KEY+DUMMY_APPWIDGET_ID,null);
        }
        registrationID =  prefs.getString(PREF_REGISTRATIONID_KEY, null);
 
        registrationLastFail = prefs.getLong(PREF_REGISTRATION_LASTFAIL, -1L); 
        registrationDeltaRetry = prefs.getLong(PREF_REGISTRATION_DELTARETRY, 1000L*60*3);
        registrationCode =  prefs.getString(PREF_REGISTRATION_CODE, null);
        appVersion =  prefs.getInt(PREF_APP_VERSION, -1);

        readKlassenArray(context, appWidgetId);
   }

    protected void saveRegistrationID(Context context){
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor prefsEdit = prefs.edit();
        prefsEdit.putString(PREF_REGISTRATIONID_KEY , registrationID);
        prefsEdit.putLong(PREF_REGISTRATION_LASTFAIL , registrationLastFail);
        prefsEdit.putLong(PREF_REGISTRATION_DELTARETRY , registrationDeltaRetry);
        prefsEdit.putString(PREF_REGISTRATION_CODE , registrationCode);
        appVersion = getAppVersion(context);
        prefsEdit.putInt(PREF_APP_VERSION , appVersion);
       prefsEdit.commit();
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * delete the data for this widget from the shared preferencs.
     * @param context
     * @param appWidgetId
     */
	public static void deletePrefs(Context context, int appWidgetId) {
	       SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
	       prefs.remove(PREF_KLASSE_KEY + appWidgetId);
        	for( int i=0; i<5; i++){
        		prefs.remove(PREF_TAG_KEY + appWidgetId+"_"+i);
        	}      		
           	for( int i=0; i<5; i++){
        		prefs.remove(PREF_DATUM_KEY + appWidgetId+"_"+i);
        	}      		
           	for( int i=0; i<5; i++){
        		prefs.remove(PREF_ALLE_KEY + appWidgetId+"_"+i);
        	}      		
	       prefs.remove(PREF_LASTUPDATE_KEY + appWidgetId);
	       prefs.remove(PREF_PAUSED_KEY + appWidgetId);
	       
	       prefs.commit();
	       
	}
	
	/**
	 * simply fill the data from the history string. 
	 */
	void readKlassenArray(Context context, int appWidgetId) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		
        String klassenDaten = prefs.getString(PREF_KLASSENARRAY_KEY + appWidgetId, null);
		if (klassenDaten == null){
			Log.d(TAG," klassenDaten = null ");
			return;
		}		
		
		JSONArray ja;
		try {
			ja = new JSONArray(klassenDaten);
		} catch (JSONException e) {
			Log.w(TAG,"JSON Array error:"+klassenDaten);
			return;
		}

		try {
			JSONArray jaNamen = ja.getJSONArray(0);
			JSONArray jaNummern = ja.getJSONArray(1);
		
			// Log.d(TAG,"Jason Namen array "+klasenDaten+" has length "+jaNamen.length());
			if (jaNamen.length() > 0){
				klassenName = new String[jaNamen.length()];
				klassenNummer = new String[jaNamen.length()];
				for (int i=0; i<jaNamen.length(); i++){
					klassenName[i] = jaNamen.optString(i);
					klassenNummer[i] = jaNummern.optString(i);
					// Log.d(TAG,"Extracting "+klassenName[i]+" from jason array");
				}
			}else{
				klassenName = new String[1];
				klassenNummer = new String[1];
				klassenName[0] = "undefiniert";
				klassenNummer[0] = "00";
			}
		} catch (JSONException e) {
			 Log.w(TAG,"JSONException in readKlassenArray ");
			 klassenNamenBenoetigenUpdate = true;		 
		}
	}

	/**
	 * save klassenArray data
	 */
	void writeKlassenArray(Context context, int appWidgetId, String jasonString) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_KLASSENARRAY_KEY+appWidgetId , jasonString);
        prefs.commit();
	}

	/**
	 * generate klassenArray data
	 */
	private String generateKlassenArray(){

        JSONArray ja = new JSONArray();
		JSONArray jaNamen = new JSONArray();
		JSONArray jaNummern = new JSONArray();
		for (String s : klassenName){
			jaNamen.put(s);
		}
		for (String s : klassenNummer){
			jaNummern.put(s);
		}
		ja.put(jaNamen);
		ja.put(jaNummern);
		// Log.d(TAG,"Length of array:: "+ja.length());
		return ja.toString();
		// Log.d(TAG,"History now "+data.history);
	}
}
