package com.quicklookbusy.narwhalNotifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

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
	
	public class AccountEditListener implements OnClickListener {
		
		public void onClick(View v) {
			Intent accountActivity = new Intent(NarwhalNotifier.this, AccountEditor.class);
			startActivity(accountActivity);
		}
	}
	
	public class FrequencyListener implements OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> av, View v, int i, long l) {
			settingsEditor.putInt("frequency", Integer.parseInt(frequencySpinner.getSelectedItem().toString()));
			settingsEditor.commit();
			if(settings.getBoolean("serviceRunning", false)) {
				//If service is running, kill it and re-register it with the new frequency
				unregisterService();
				registerService();
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
						registerService();
						
						serviceFeedbackLabel.setText("Service started");
						serviceFeedbackLabel.setTextColor(Color.GREEN);
						settingsEditor.putBoolean("serviceRunning", true);
						settingsEditor.commit();
					}
				}
				else {
					//Kill service
					if(settings.getBoolean("serviceRunning", false)) {
						unregisterService();
						
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
        
        settings = getSharedPreferences(PREFS_NAME, 0);
        settingsEditor = settings.edit();
        
        LinearLayout accountEditTrigger = (LinearLayout) findViewById(R.id.accountEditTrigger);
        accountEditTrigger.setOnClickListener(new AccountEditListener());
        
        TextView subText = (TextView) findViewById(R.id.accountSubtext);
        syncSubtext();
        
        frequencySpinner = (Spinner) findViewById(R.id.frequencySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.planets_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencySpinner.setAdapter(adapter);
        frequencySpinner.setOnItemSelectedListener(new FrequencyListener());
        
        serviceButton = (ToggleButton) findViewById(R.id.serviceToggle);
        serviceButton.setChecked(settings.getBoolean("serviceRunning", false));
        serviceButton.setOnClickListener(new ServiceListener());
        
        serviceFeedbackLabel = (TextView) findViewById(R.id.serviceErrorLabel);
        serviceFeedbackLabel.setText("");
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	syncSubtext();
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
	
	public void registerService() {
		//Taken from http://stackoverflow.com/questions/1082437/android-alarmmanager
		am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(NarwhalNotifier.this, NarwhalNotifierService.class);
		PendingIntent pi = PendingIntent.getBroadcast(NarwhalNotifier.this, 0, i, 0);
		Calendar time = Calendar.getInstance();
		time.setTimeInMillis(System.currentTimeMillis());
		time.add(Calendar.SECOND, 5);
		long interval = settings.getInt("frequency", 5) * 60 * 1000;
		am.setRepeating(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), interval, pi);
	}
	
	public void unregisterService() {
		am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(NarwhalNotifier.this, NarwhalNotifierService.class);
		PendingIntent pi = PendingIntent.getBroadcast(NarwhalNotifier.this, 0, i, 0);
		am.cancel(pi);
	}
    
    /*private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.quicklookbusy.narwhalNotifier.NarwhalNotifierService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }*/
}