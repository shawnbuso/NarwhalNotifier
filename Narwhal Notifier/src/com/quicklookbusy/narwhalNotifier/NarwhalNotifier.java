/*
 * NarwhalNotifier.java
 * 
 * Defines the main class which controls the main view when the app is launched
 * 
 * Copyright (C) Shawn Busolits, 2012 All Rights Reserved
 */

package com.quicklookbusy.narwhalNotifier;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.ads.AdRequest;
import com.google.ads.AdView;

/**
 * An extension of Activity which presents and controls the main app screen
 * @author Shawn Busolits
 * @version 1.0
 */
public class NarwhalNotifier extends Activity {
	
	/** Used to obtain the SharedPreferences for the app */
	public static final String PREFS_NAME = "NarwhalNotifierPrefs";
	
	/** Button used to turn the service off and on */
	ToggleButton serviceButton;
	/** Label used to give feedback about starting and stopping the service */
	TextView serviceFeedbackLabel;
	
	/** Tag for log statements */
	String logTag = "NarwhalNotifier";
	
	/** Used to store state */
	SharedPreferences settings;
	
	/** Used to register or unregister our service */
	AlarmHelper ah;
	
	/**
	 * Listens for clicks on the "Edit Account" view
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	public class AccountEditListener implements OnClickListener {
		
		/**
		 * Called when the user clicks the "Edit Accout" view, and launches the
		 * Activity for editing the user account
		 * @param v View clicked to call this method
		 */
		public void onClick(View v) {
			Intent accountActivity = new Intent(NarwhalNotifier.this, AccountEditor.class);
			startActivity(accountActivity);
		}
	}
	
	/**
	 * Listens for clicks on the "Options" view
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	public class OptionsListener implements OnClickListener {
		
		/**
		 * Called when the user clicks the "Options" view, and launches the
		 * Activity for editing options
		 * @param v View clicked to call this method
		 */
		public void onClick(View v) {
			Intent optionsActivity = new Intent(NarwhalNotifier.this, Options.class);
			startActivity(optionsActivity);
		}
	}
	
	/**
	 * Listens for clicks on the service toggle button
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	public class ServiceListener implements OnClickListener {

		/**
		 * Called when the button is clicked
		 * @param v The view that was clicked
		 */
		public void onClick(View v) {
				if(serviceButton.isChecked()) {					
					if(settings.getString("user", "").equals("")) {
						serviceFeedbackLabel.setText("Error: No user logged in");
						serviceFeedbackLabel.setTextColor(Color.YELLOW);
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
						serviceFeedbackLabel.setTextColor(Color.RED);
					}
					else {
						serviceFeedbackLabel.setText("Error: Service not running");
						serviceFeedbackLabel.setTextColor(Color.YELLOW);
					}
				}
		}		
	}
	
	/**
	 * {@inheritDoc}
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        ah = new AlarmHelper(NarwhalNotifier.this);
        
        settings = getSharedPreferences(PREFS_NAME, 0);
        Editor editor = settings.edit();
        
        LinearLayout accountEditTrigger = (LinearLayout) findViewById(R.id.accountEditTrigger);
        accountEditTrigger.setOnClickListener(new AccountEditListener());
        
        syncSubtext();
        
        TextView optionsTrigger = (TextView) findViewById(R.id.optionsTrigger);
        optionsTrigger.setOnClickListener(new OptionsListener());
        
        
        serviceButton = (ToggleButton) findViewById(R.id.serviceToggle);
        serviceButton.setChecked(settings.getBoolean("serviceRunning", false));
        serviceButton.setOnClickListener(new ServiceListener());
        
        serviceFeedbackLabel = (TextView) findViewById(R.id.serviceFeedbackLabel);
        serviceFeedbackLabel.setText("");
        
        if(!settings.contains("frequency")) {
        	editor.putInt("frequency", 60);
			editor.putInt("frequencyIndex", 7);
        }
        
        if(!settings.contains("checkMessages")) {
        	editor.putBoolean("checkMessages", true);
        }
        
        if(!settings.contains("checkModmail")) {
        	editor.putBoolean("checkModmail", true);
        }
        editor.commit();
        
        AdView adView = (AdView)this.findViewById(R.id.adView);
        adView.loadAd(new AdRequest());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
    	super.onResume();
    	syncSubtext();
        
        if(settings.getBoolean("serviceRunning", false)) {
        	serviceFeedbackLabel.setText("Service is running");
			serviceFeedbackLabel.setTextColor(Color.GREEN);
        } else {
        	serviceFeedbackLabel.setText("Service is NOT running");
			serviceFeedbackLabel.setTextColor(Color.RED);
        }
    }
    
    /**
     * Sets the subtext for the Edit Account view to show the username of the currently
     * logged in user, or show that no user is logged in.
     */
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