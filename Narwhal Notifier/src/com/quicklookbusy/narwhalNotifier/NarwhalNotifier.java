package com.quicklookbusy.narwhalNotifier;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.ads.AdRequest;
import com.google.ads.AdView;

public class NarwhalNotifier extends Activity {
	
	public static final String PREFS_NAME = "NarwhalNotifierPrefs";
	
	ToggleButton serviceButton;
	Spinner frequencySpinner;
	TextView serviceFeedbackLabel;
	
	String logTag = "NarwhalNotifier";
	
	Intent service;
	
	SharedPreferences settings;
	Editor settingsEditor;
	
	AlarmManager am;
	
	AlarmHelper ah;
	
	public class AccountEditListener implements OnClickListener {
		
		public void onClick(View v) {
			Intent accountActivity = new Intent(NarwhalNotifier.this, AccountEditor.class);
			startActivity(accountActivity);
		}
	}
	
	public class FrequencyListener implements OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> av, View v, int i, long l) {
			settingsEditor.putInt("frequency", Integer.parseInt(frequencySpinner.getSelectedItem().toString()));
			settingsEditor.putInt("frequencyIndex", frequencySpinner.getSelectedItemPosition());
			settingsEditor.commit();
			if(settings.getBoolean("serviceRunning", false)) {
				//If service is running, kill it and re-register it with the new frequency
				ah.unregisterService();
				ah.registerService();
			}
		}

		public void onNothingSelected(AdapterView<?> arg0) {
			//Do nothing
		}
	}
	
	public class ServiceListener implements OnClickListener {

		public void onClick(View v) {
				if(serviceButton.isChecked()) {					
					if(settings.getString("user", "").equals("")) {
						serviceFeedbackLabel.setText("Error: No user logged in");
						serviceFeedbackLabel.setTextColor(Color.RED);
						serviceButton.setChecked(false);
					}
					else {
						ah.registerService();
						
						serviceFeedbackLabel.setText("Service started");
						serviceFeedbackLabel.setTextColor(Color.GREEN);
					}
				}
				else {
					//Kill service
					if(settings.getBoolean("serviceRunning", false)) {
						ah.unregisterService();
						
						serviceFeedbackLabel.setText("Service stopped");
						serviceFeedbackLabel.setTextColor(Color.GREEN);
					}
					else {
						serviceFeedbackLabel.setText("Error: Service not running");
						serviceFeedbackLabel.setTextColor(Color.RED);
					}
				}
		}		
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        ah = new AlarmHelper(NarwhalNotifier.this);
        
        settings = getSharedPreferences(PREFS_NAME, 0);
        settingsEditor = settings.edit();
        
        LinearLayout accountEditTrigger = (LinearLayout) findViewById(R.id.accountEditTrigger);
        accountEditTrigger.setOnClickListener(new AccountEditListener());
        
        syncSubtext();
        
        frequencySpinner = (Spinner) findViewById(R.id.frequencySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.planets_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencySpinner.setAdapter(adapter);
        frequencySpinner.setSelection(settings.getInt("frequencyIndex", 2), true);
        frequencySpinner.setOnItemSelectedListener(new FrequencyListener());
        
        serviceButton = (ToggleButton) findViewById(R.id.serviceToggle);
        serviceButton.setChecked(settings.getBoolean("serviceRunning", false));
        serviceButton.setOnClickListener(new ServiceListener());
        
        serviceFeedbackLabel = (TextView) findViewById(R.id.serviceFeedbackLabel);
        serviceFeedbackLabel.setText("");
        
        AdView adView = (AdView)this.findViewById(R.id.adView);
        adView.loadAd(new AdRequest());
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	syncSubtext();
    	serviceFeedbackLabel.setText("");
    }
    
	private void syncSubtext() {
		TextView subText = (TextView) findViewById(R.id.accountSubtext);
		String user = settings.getString("user", "");
		if (user.equals("")) {
			subText.setText("Not currently logged in. Click to log in.");
		} else {
			subText.setText("Currently logged in as " + user + " - click to change");
		}
	}
}