package de.jweimar.android.vertretungsplan;

import java.util.Calendar;
import java.util.Date;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;

public class Entschuldigung extends Activity {

	private static final String NAME2 = "name";
	private static final String KLASSE = "klasse";
	private static final String KLASSENLEHRER = "klassenlehrer";
	private static final String GRUND = "grund";
	private static final String ERZIEHER = "erzieher";
	protected static final String GESCHLECHT = "Geschlecht";
	
	private Spinner spinnergeschlecht;
	private EditText nameField;
	private EditText klasseField;
	private EditText klassenlehrerField;
	private EditText grundField;
	private RadioGroup dateRadioGroup;
	private EditText erzieherField;
	private Button sendeButton;
	private SharedPreferences prefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entschuldigung_main);
		this.setTitle(R.string.entschuldigung_name);
		
		spinnergeschlecht = (Spinner) findViewById(R.id.spinnergeschlecht);
		nameField = (EditText) findViewById(R.id.nameField);
		klasseField = (EditText) findViewById(R.id.klasseField);
		klassenlehrerField = (EditText) findViewById(R.id.klassenlehrerField);
		grundField = (EditText) findViewById(R.id.grundField);
		dateRadioGroup = (RadioGroup) findViewById(R.id.datumRadioGroup);
		erzieherField  = (EditText) findViewById(R.id.erzieherField);
		sendeButton = (Button) findViewById(R.id.sendeButton);
		
		prefs = this.getPreferences(0);
		if (prefs.contains(NAME2)){
			String name = prefs.getString(NAME2, null);
			nameField.setText(name);
		}
		if (prefs.contains(KLASSE)){
			klasseField.setText(prefs.getString(KLASSE, null));
		}
		klassenlehrerField.setText(prefs.getString(KLASSENLEHRER, null));
		grundField.setText(prefs.getString(GRUND, null));
		erzieherField.setText(prefs.getString(ERZIEHER, null));
		spinnergeschlecht.setSelection(prefs.getInt(GESCHLECHT, 0));
		sendeButton.setOnClickListener(new OnClickListener(){
			
			@Override
			public void onClick(View v) {
				StringBuilder mailTextSB = new StringBuilder();
				mailTextSB.append(Entschuldigung.this.getString(R.string.text0));
				mailTextSB.append(Entschuldigung.this.getString(R.string.text1));
				switch(Entschuldigung.this.spinnergeschlecht.getSelectedItemPosition()){
				case 0: mailTextSB.append(" mein Kind "); break;
				case 1: mailTextSB.append(" meine Tochter "); break;
				case 2: mailTextSB.append(" mein Sohn "); break;
				case 3: mailTextSB.append(" ich "); break;
				}
				mailTextSB.append(nameField.getText());
				mailTextSB.append(", \nKlasse ");
				mailTextSB.append(klasseField.getText());
				mailTextSB.append(", Klassenlehrer ");
				mailTextSB.append(klassenlehrerField.getText());
				mailTextSB.append("\nwegen ");
				mailTextSB.append(grundField.getText());

				java.text.DateFormat df = DateFormat.getDateFormat(Entschuldigung.this);
				Calendar cal = Calendar.getInstance();
				switch(Entschuldigung.this.dateRadioGroup.getCheckedRadioButtonId()){
				case R.id.heute: mailTextSB.append(" heute, am "); 
					mailTextSB.append(df.format(cal.getTime()));
					break;
				case R.id.morgen: mailTextSB.append(" morgen, am "); 
					cal.add(Calendar.DAY_OF_YEAR, 1);
					mailTextSB.append(df.format(cal.getTime()));
					break;
				case R.id.vonbis: 
					// TODO
					mailTextSB.append(" von ... bis ..."); 
					break;
				}
				mailTextSB.append(" ");
				mailTextSB.append(Entschuldigung.this.getString(R.string.text2));
				mailTextSB.append("\n\n");
				mailTextSB.append("Mit freundlichen Grüßen, \n");
				mailTextSB.append(erzieherField.getText());
				mailTextSB.append("\n");
		
				
				
				// Nun Werte abspeichern für´s nächste Mal.
				Editor edit = prefs.edit();
				edit.putString(NAME2, nameField.getText().toString());
				edit.putString(KLASSE, klasseField.getText().toString());
				edit.putString(KLASSENLEHRER, klassenlehrerField.getText().toString());
				edit.putString(GRUND, grundField.getText().toString());
				edit.putString(ERZIEHER, erzieherField.getText().toString());
				edit.putInt(GESCHLECHT, Entschuldigung.this.spinnergeschlecht.getSelectedItemPosition());
				edit.commit();
				
				
				// Was machen wir damit? Email senden!
				Intent emailIntent = new Intent(Intent.ACTION_SEND);
				emailIntent.setType("text/plain");
				Uri data = Uri.fromParts("mailto", "sekretariat@grosse-schule.de", null);
				// emailIntent.setData(data);
				emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"sekretariat@grosse-schule.de"}); // recipients
				emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Entschuldigung "+nameField.getText()+" "+df.format(cal.getTime()));
				emailIntent.putExtra(Intent.EXTRA_TEXT, mailTextSB.toString());
				
				// Intent.createChooser(emailIntent, "Entschuldigungs-Mail")
				startActivity(emailIntent);
				// finish();
			}	
		});
		
	}
}
