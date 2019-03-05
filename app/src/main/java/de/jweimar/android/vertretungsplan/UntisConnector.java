package de.jweimar.android.vertretungsplan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Class that connects to the UNTIS class schedule planning system.
 * 
 * @author weimar
 *
 */
class UntisConnector {

	private PersistentData data;
	private Context context;
	private static final String TAG = "vertretungsplan.UntisCo";
	// String latest = "Vertretungen";
	private String page;
	String url;
	private static final boolean debug = false;
	private static HashMap<PersistentData, UntisConnector> cache = new HashMap<>();
	
	static UntisConnector getInstance(Context context, PersistentData data){
		UntisConnector temp = cache.get(data);
		if (temp == null){
			temp = new UntisConnector(context, data);
			cache.put(data, temp);
		}
		return temp;
	}
	
	private UntisConnector(Context context, PersistentData data){
		this.context = context;
		this.data = data;
		constructUrl();
	}

	/** construct the URL.
	 */
	private void constructUrl() {
		constructUrl(0);  // default: 0 weeks forward
	}
		
	/**
	 * @param weeksForward  number of weeks to look in the future. normally 0.
	 */
	void constructUrl(int weeksForward) {
		// construct the url.
		String klasseNummer = getKlasseNummer(data.klasse);
		 //NumberFormat formatTwoDigits = NumberFormat.getInstance();
		// if (formatTwoDigits instanceof DecimalFormat) {
		//     ((DecimalFormat)formatTwoDigits).setMinimumIntegerDigits(2);
		// }
		// String woche = formatTwoDigits.format(getWeekOfTheYear());
		// works, but simpler is:
		int week = getWeekOfTheYear()+weeksForward;
		String woche = (week < 10)? "0"+week : ""+week;
		// urlStart = "http://iserv.grosse-schule.versus-wf.de/idesk/v2plan/"
		if (data.urlStart != null){
			boolean urlHasW = data.urlStart.contains("/w/");
			url = data.urlStart+woche+(urlHasW?"":"/w")+"/w00"+klasseNummer+".htm";
		}else{
			url = null;
		}
	}
	
	private String getKlasseNummer(String klasse){
		if (klasse == null) return "00";
		
		// TODO: test the use mydata.klassenName
		if (data.klassenName != null && data.klassenName.length >1){
			for (int i=0; i<data.klassenName.length; i++){
				if (klasse.equals(data.klassenName[i])){
					return data.klassenNummer[i];
				}
			}		
		}
		// Fallback: old version
		Resources res = context.getResources();
		String[] klassen = res.getStringArray(R.array.klassen_array);
		String[] nummern = res.getStringArray(R.array.nummern_array);
		for (int i=0; i<klassen.length; i++){
			if (klasse.equals(klassen[i])){
				return nummern[i];
			}
		}
		return null;
	}
	
	/**
	 * get the current week, or the next week if it is after 13:00 on Friday.
	 * @return week of the year as used in UNTIS; change to next week on friday.
	 */
	private int getWeekOfTheYear(){
		Calendar cal = Calendar.getInstance();
		Calendar calFriMid = (Calendar)cal.clone();
		calFriMid.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
		calFriMid.set(Calendar.HOUR_OF_DAY, 13);
		calFriMid.set(Calendar.MINUTE, 0);
		
		int week = cal.get(Calendar.WEEK_OF_YEAR);
		if (cal.after(calFriMid)){
			week = week+1;
		}
		// correction for US setting in some phones makes weeks incorrect!
		if (Locale.getDefault().getCountry().equals("US") && cal.get(Calendar.YEAR) == 2016){
			week--;
		}

		return week;
	}
	
	/**
	 * get the current or next day, depending on the time of day.
	 * @return todays day number in the week.
	 */
	private static int getBestDay(){
		Calendar cal = Calendar.getInstance();
		// Log.i(TAG,"Timezone: "+cal.getTimeZone());
		Calendar calFriMid = (Calendar)cal.clone();
		calFriMid.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
		calFriMid.set(Calendar.HOUR_OF_DAY, 13);
		calFriMid.set(Calendar.MINUTE, 0);
		
		int day = cal.get(Calendar.DAY_OF_WEEK);
		if (cal.after(calFriMid)){
			return 0; // nächsten Montag
		}
		Calendar calMid = (Calendar)cal.clone();
		calMid.set(Calendar.HOUR_OF_DAY, 13);
		calMid.set(Calendar.MINUTE, 0);
		if (cal.after(calMid)){
			Log.i(TAG,"Nach Mittag, day = "+day);
			return day-Calendar.MONDAY+1;
		}else{
			Log.i(TAG,"Vor Mittag, day = "+day+ " MONDAY="+Calendar.MONDAY);
			return day-Calendar.MONDAY;			
		}
	}

	
	private void extractCurrentValues(String page) {
		
		// finde fünf mal  alles zwischen <table class="subst" >  bis </table>
		// TODO: find messages for all: example
		// <table border="3" rules="all" bgcolor="#F4F4F4" cellpadding="3" cellspacing="3">
		// <tr><th align="center" colspan="2">Nachrichten zum Tag</th></tr>
		// <tr><td colspan="2">Klasse 6b,  bitte Sportzeug mitbringen!!</td></tr>
		// <br></table>
		
		
		 Pattern p = Pattern.compile("(?sm)<a name=...>.*?<b>(.*?)</b>.*?<p>[\n\r]?(<table border=\"3\" rules=\"all\" .*?>.*?</table>)?.*?<table class=.subst.\\s?>[\r\n]*(.*?)[\r\n]*</table>");
		 Matcher m = p.matcher(page);

		 String[] res = new String[7];
		 String[] datum = new String[7]; 
		 String[] alle = new String[7];
		 
		 /*
		  *  <table class="subst" ><tr class='list'>\
		  *  <th class="list" align="center"><b>Klasse(n)</b></th>\
		  *  <th class="list" align="center">Stunde</th>\
		  *  <th class="list" align="center">(Fach)</th>\
		  *  <th class="list" align="center">(Lehrer)</th>\
		  *  <th class="list" align="center">(Verlegung  auf)</th>\
		  *  <th class="list" align="center">Fach</th>\
		  *  <th class="list" align="center">Vertreter</th>\
		  *  <th class="list" align="center">Art</th>\
		  *  <th class="list" align="center">Verlegung von</th>\
		  *  <th class="list" align="center">Raum</th>\
		  *  <th class="list" align="center">Bemerkungen</th>\
		  *  </tr>
		  *  <tr class='list odd'>
		  *  <td class="list" align="center"><b>5a</b></td>
		  *  <td class="list" align="center">6</td>
		  *  <td class="list" align="center">En</td>
		  *  <td class="list" align="center">J�</td>
		  *  <td class="list">&nbsp;</td>
		  *  <td class="list" align="center">Mu</td>
		  *  <td class="list" align="center">N�</td>
		  *  <td class="list" align="center">Vertretung</td>
		  *  <td class="list">&nbsp;</td>
		  *  <td class="list" align="center">Mu2</td>
		  *  <td class="list" align="center">&nbsp;</td>
		  *  </tr><tr class='list even'>
		  *  <td class="list" align="center"><b>5a, 5b, 5c, 5d, 5e, AG</b></td>
		  *  <td class="list" align="center">8</td>
		  *  <td class="list" align="center">F� En</td>
		  *  <td class="list" align="center">J�</td>
		  *  <td class="list" align="center">Entfall</td>
		  *  <td class="list" align="center">---</td>
		  *  <td class="list" align="center">---</td>
		  *  <td class="list" align="center">Entfall</td>
		  *  <td class="list">&nbsp;</td>
		  *  <td class="list" align="center">---</td>
		  *  <td class="list" align="center">&nbsp;</td>
		  *  </tr><tr class='list odd'>
		  *  <td class="list" align="center"><b>5b</b></td>
		  *  <td class="list" align="center">1</td>
		  *  <td class="list" align="center">Ge</td>
		  *  <td class="list" align="center">Sn</td>
		  *  <td class="list" align="center">Entfall</td>
		  *  <td class="list" align="center">---</td>
		  *  <td class="list" align="center">---</td>
		  *  <td class="list" align="center">Entfall</td>
		  *  <td class="list">&nbsp;</td>
		  *  <td class="list" align="center">---</td>
		  *  <td class="list" align="center">&nbsp;</td>....
		  */
		 Pattern p_tr = Pattern.compile("(?sm)<tr class=.*?>[\\r\\n]*(.*?)[\\r\\n]*</tr>");
		 Pattern p_td = Pattern.compile("(?sm)<td[^>]*>[\\r\\n]*(?:<b>)?(?:<span.*?>)?(.*?)(?:</span>)?(?:</b>)?</td>");
		 int i=0;

		 while (m.find()) {
             datum[i] = m.group(1);
             alle[i] = m.group(2);
             res[i] = m.group(3);

             if (debug) {
                 if (datum[i] != null) Log.i(TAG, "DATUM: " + datum[i]);
                 if (alle[i] != null) Log.i(TAG, "ALLE: " + alle[i]);
                 if (res[i] != null) Log.i(TAG, "RES: " + res[i]);
             }

             if (alle[i] != null) { // lese die Infos für alle ein.
                 String alle_hier = alle[i];
                 // Log.i(TAG,"ALL: "+alle_hier);
                 Matcher m_td = p_td.matcher(alle_hier);
                 alle[i] = "";
                 int countlines = 0;
                 while (m_td.find() && countlines < 2) {
                     if (!alle[i].isEmpty()) {
                         alle[i] += "\n";
                     }
                     alle[i] += substituteNBSP(m_td.group(1));
                     if (m_td.group(1).startsWith("Heute k")) {// debugging für Test
                         Log.i(TAG, "Nächstes Zeichen: " + ((int) alle[i].charAt(7)));
                     }

                     countlines++;
                 }
                 if (alle[i].length() > 40) {
                     alle[i] = alle[i].substring(0, 39) + "...";
                 }
                 Log.i(TAG, "ALL EXTRACTED: " + alle[i]);
             } else {
                 alle[i] = "";
             }
             if (res[i] != null && res[i].contains("Vertretungen sind nicht freigegeben")) {
                 res[i] = "Vertretungen gesperrt";
             } else if (res[i] != null && res[i].contains("Keine Vertretungen")) {
                 res[i] = "Keine Vertretungen";
             } else if (res[i] != null) {
                 // inside: tr identifizieren, darin erste zeile: header, alle danach Vertretungen.
                 Matcher m_tr = p_tr.matcher(res[i]);
                 // String oldres = res[i]; // for better debugging
                 res[i] = "";
                 //noinspection ResultOfMethodCallIgnored
                 m_tr.find(); // first row: headers, we ignore this.
                 // String headers = m_tr.group(1);
                 while (m_tr.find()) { // alle Treffer sind Vertretungen
                     if (res[i] != null && !res[i].isEmpty()) {
                         res[i] += "\n";
                     }
                     String row = m_tr.group(1);
                     Matcher m_td = p_td.matcher(row);
                     String[] rowValues = new String[12];
                     int columnIndex = 0;
                     while (m_td.find()) {
                         rowValues[columnIndex] = m_td.group(1);
                         columnIndex++;
                     }
                     if (!rowValues[0].contains(data.klasse)
                             && data.klasse != null && !data.klasse.contains("Alle")) {
                         vorbereitenKlassenNamenNeuLaden(rowValues[0], data.klasse);
                         Log.i(TAG, "Klassennamen: found " + rowValues[0] + " expected " + data.klasse);
                     }
                     String newrow = rowValues[1] + ". " + rowValues[2] + "(" + rowValues[3] + ") \u2192 "
                             + rowValues[5] + "(" + rowValues[6] + ")";
                     if (!"---".equals(rowValues[9])) {
                         newrow += ", " + rowValues[9];
                     }
                     if (rowValues[10] != null && !"&nbsp;".equals(rowValues[10])) {
                         newrow += rowValues[10];
                     }
                     newrow = substituteNBSP(newrow);
                     res[i] += newrow;
                 }
             }
			 
//			 if (res[i] != null){
//				 // Log.i(TAG,"i="+i+": "+res[i]);
//			 }
			 i++;
		 }
		if (i > 4){ // means five days were found!
			data.tag = res;
			data.datum = datum;
			data.allgemein = alle;
		}// otherwise: leave old values, maybe they are still OK.
	}

	// private static final String UTF8oe = ""+(char)0xc3+""+(char)0xb6;
	private static final String ISOoe = ""+(char)0xfffd;
	// private static final String SELTSAMoe =""+(char)0xef + ""+(char)0xbf + ""+(char)0xbd;
	
	private String substituteNBSP(String s){
		
		return s.replace("&nbsp;", " ").replace("<br>", " ").replace(ISOoe,"-");
		// return s.replace("&nbsp;", " ").replace("<br>", " ").replace(SELTSAMoe,"oe1oe").replace(UTF8oe,"oe2oe").replace(ISOoe,"ö");
	}
	

	/**
	 * Hole den neuesten Vertretungsplan für die Klasse, und speichere die Daten in @data.
	 * Liefere die HTML-Seite als resultat zurück, dies wird nur in "Configure" benutzt, ansonsten ignoriert,
	 * @return result
	 */
	private String getLatest(){
		// Wenn kein gültiger Schlüssel eingegeben wurde, liefere Meldung!
		if (url == null){
			return "<html><body> Nicht Konfiguriert</body></html>";			
		}
		if (page != null) return page;
		// First test, if network and sufficient battery power is available.
		ConnectivityManager cm = (ConnectivityManager) 	
			context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni=null;
        if (cm != null){
            try {
                ni = cm.getActiveNetworkInfo();
            } catch(NullPointerException npe){
                Log.i(TAG,"NullPointerException: ");
            }
        }
        if (ni == null || !ni.isConnected()){
			Log.i(TAG,"No connection: "+((ni != null)? ni.toString(): "NetworkInfo null"));
			// TODO: store info in data?
			return "<html><body> Kein Netzwerk</body></html>";
		}

/*		// This is for some reason not allowed, as we are already in a subThread.
        //
   		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = context.registerReceiver(null, ifilter);
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		float batteryPct = level / (float)scale;
		
		if ( batteryPct < 0.4){
			return "<html><body> Batterie schwach, alte Daten werden verwendet.</body></html>";
		}
*/		
		// request latest data
		page = getAsString(url);
		
		if (page != null){
			extractCurrentValues(page); // Stores data in @data.
		}
		
		return page;
		
	}

	
	/**
	 * get the latest value by fetching the URL, then looking for the position of the requested data.
	 * @return complete page as string
	 */
	static String getAsString(String urlString) {
		try{
			URL url= new URL(urlString);
			Properties prop = System.getProperties();
			String oldencoding = prop.getProperty("file.encoding");
			prop.setProperty("file.encoding","iso-8859-1");
			
			InputStream is = (InputStream) url.getContent();
			String s = getAsString(is, url.toString());
			is.close();
			prop.setProperty("file.encoding",oldencoding); 

			return s;
			
		}catch(IOException e){
			Log.w(TAG,"IOException reading "+urlString);
			// maybe no Network!
		}catch(NullPointerException e){
			Log.w(TAG,"NullPointerException reading "+urlString);
			// maybe no Network!
		}
		return null;  		// not found
	}

	/**
	 * get the latest value by fetching the URL
	 * @return whole page as one string
	 */
	private static String getAsString(InputStream is, String url) {
		String line;
		StringBuilder sb = new StringBuilder(5000);
		
		try{
			InputStreamReader isr = new InputStreamReader(is, Charset.forName("iso-8859-1"));
			
			BufferedReader br = new BufferedReader(isr, 1024);
			while ( (line = br.readLine()) != null){
				sb.append(line);
			}

			return sb.toString();  
		}catch(IOException e){
			Log.w(TAG,"IOException reading "+url);
			// maybe no Network!
		}catch(NullPointerException e){
			Log.w(TAG,"NullPointerException reading "+url);
			// maybe no Network!
		}
		return null;  		// not found
	}

	/**
	 * Hole Daten vom Server, ignoriere dabei die HTML-Seite. dann liefere die Texte für den aktuellen Tag.
	 * @return array of texts
	 */
	String[] getCurrent(){
		getLatest();
		String[] ergebnis = new String[3];
		final int bestDay = getBestDay();
		ergebnis[0] = data.tag[bestDay];
		if (data.allgemein != null && data.allgemein.length > 4){
			ergebnis[1] = data.allgemein[bestDay];
		}
		if (data.datum != null && data.datum.length > 4){
			ergebnis[2] = data.datum[bestDay];
		}else{
			ergebnis[2] =  "XXX";
		}
		return ergebnis;
	}
	
	/**
	 * Extract the class anmes from the navigation bar.
	 * @param urlStart start url will be appended by /frames/navbar.htm
	 */
	private static String getKlassenNamenNummern(String urlStart) {
		String navPage=null;
		String namen = null;
		StringBuilder nummern = new StringBuilder();
        if (urlStart != null){
			String urlNav = urlStart+"/frames/navbar.htm";
			navPage = getAsString(urlNav);
		}
		if (navPage != null){
			 Pattern p = Pattern.compile("(?sm)var classes = (\\[.*?\\]);.*?var flcl = (.);");
			 Matcher m = p.matcher(navPage);
			 if (m.find()){
				 namen = m.group(1);
                 int flcl = Integer.parseInt(m.group(2));
                 if ((flcl & 0x1) != 0 ){
					 namen = "[ \"-Alle-\", "+namen.substring(1);
					 nummern.append("[\"000\", ");
				 }else{
					 nummern.append( "[");
				 }
				 String [] array = namen.split(",");
				 if (array.length > 0){
					 nummern.append("\"001\"");
				 }
				 for (int i=1; i<array.length; i++){
					 nummern.append(",\"");
					 if (i+1<100){
						 nummern.append("0");
					 }
					 if (i+1<10){
						 nummern.append("0");
					 }
					 nummern.append(i+1);
					 nummern.append("\"");
					 
				 }
				 nummern.append("]");
			 }
			 return "["+namen+", "+nummern.toString()+"]";
		}
		return null;
	}

	/**
	 * loads the class names again.
	 */
	void ladeKlassenNamen(Context context) {
		// determine Klassen Namen und Nummern
		String klassenNamen = getKlassenNamenNummern(data.urlStart);
		if (klassenNamen != null) {
			data.writeKlassenArray(context, data.widgetId, klassenNamen);
			data.readKlassenArray(context, data.widgetId);
			data.lastKlassenNamenUpdate = System.currentTimeMillis();
			data.klassenNamenBenoetigenUpdate = false;
		}
	}

	
	private void vorbereitenKlassenNamenNeuLaden(String ist, String soll){
		data.klassenNamenBenoetigenUpdate = true;
		Log.w(TAG,"Klassennamen müssen neu geladen werden: soll "+soll+" aber ist: "+ist);
	}
}
