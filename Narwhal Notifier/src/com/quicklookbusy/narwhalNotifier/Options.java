/*
 * Options.java
 * 
 * Defines the class which controls the Activity used to modify app options
 * 
 * Copyright (C) Shawn Busolits, 2012 All Rights Reserved
 */

package com.quicklookbusy.narwhalNotifier;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

public class Options extends Activity {

	/** Tag for log messages */
	String logTag = "NarwhalNotifierOptions";

	/**
	 * Used to change the frequency with which the app will check for unread
	 * messages
	 */
	Spinner frequencySpinner;

	/** Used to change preferences on messages */
	CheckBox messages;

	/** Used to change preferences on modmail */
	CheckBox modmail;

	/** Used to change preferences on mod queue */
	CheckBox modqueue;

	/** Used to take input on subreddits the user moderates */
	EditText subField;

	/** Used to store state */
	SharedPreferences settings;
	/** Used to edit stored app state */
	Editor settingsEditor;

	/** Used to register or unregister our service */
	AlarmHelper ah;

	/**
	 * Listens for changes to the frequency spinner
	 * 
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	public class FrequencyListener implements OnItemSelectedListener {

		/**
		 * Called when the user selects a frequency from the frequency spinner
		 * 
		 * @param av
		 *            AdapterView where the selection happened
		 * @param v
		 *            View selected to call this method
		 * @param i
		 *            Position of the selected item (unused)
		 * @param l
		 *            Row id of the item selected (unused)
		 */
		public void onItemSelected(AdapterView<?> av, View v, int i, long l) {
			settingsEditor.putInt("frequency", Integer
					.parseInt(frequencySpinner.getSelectedItem().toString()));
			settingsEditor.putInt("frequencyIndex",
					frequencySpinner.getSelectedItemPosition());
			settingsEditor.commit();
			if (settings.getBoolean("serviceRunning", false)) {
				// If service is running, kill it and re-register it with the
				// new frequency
				ah.unregisterService();
				ah.registerService();
			}
		}

		/**
		 * Unused Called when the selection disappears from this view. The
		 * selection can disappear for instance when touch is activated or when
		 * the adapter becomes empty.
		 * 
		 * @param av
		 *            The AdapterView that now contains no selected item
		 */
		public void onNothingSelected(AdapterView<?> av) {
			// Do nothing
		}
	}

	/**
	 * Updates settings when messages checkbox is changed
	 * 
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	private class MessagesCheckListener implements
			CompoundButton.OnCheckedChangeListener {
		/**
		 * Updates settings when messages checkbox is changed
		 * 
		 * @param buttonView
		 *            The button clicked
		 * @param isChecked
		 *            True if the button is checked, false otherwise
		 */
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			log("Setting checkMessages to " + isChecked);
			settingsEditor.putBoolean("checkMessages", isChecked);
			settingsEditor.commit();
		}
	}

	/**
	 * Updates settings when modmail checkbox is changed
	 * 
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	private class ModmailCheckListener implements
			CompoundButton.OnCheckedChangeListener {
		/**
		 * Updates settings when modmail checkbox is changed
		 * 
		 * @param buttonView
		 *            The button clicked
		 * @param isChecked
		 *            True if the button is checked, false otherwise
		 */
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			log("Setting checkModmail to " + isChecked);
			settingsEditor.putBoolean("checkModmail", isChecked);
			settingsEditor.commit();
		}
	}

	/**
	 * Updates settings when modqueue checkbox is changed
	 * 
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	private class ModqueueCheckListener implements
			CompoundButton.OnCheckedChangeListener {
		/**
		 * Updates settings when modqueue checkbox is changed
		 * 
		 * @param buttonView
		 *            The button clicked
		 * @param isChecked
		 *            True if the button is checked, false otherwise
		 */
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			log("Setting checkModqueue to " + isChecked);
			settingsEditor.putBoolean("checkModqueue", isChecked);
			settingsEditor.commit();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.options);

		ah = new AlarmHelper(Options.this);

		settings = getSharedPreferences(NarwhalNotifier.PREFS_NAME, 0);
		settingsEditor = settings.edit();

		frequencySpinner = (Spinner) findViewById(R.id.frequencySpinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.frequency_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		frequencySpinner.setAdapter(adapter);
		frequencySpinner.setSelection(settings.getInt("frequencyIndex", 7),
				true);
		frequencySpinner.setOnItemSelectedListener(new FrequencyListener());

		messages = (CheckBox) findViewById(R.id.messagesCheck);
		messages.setOnCheckedChangeListener(new MessagesCheckListener());
		messages.setChecked(settings.getBoolean("checkMessages", false));

		modmail = (CheckBox) findViewById(R.id.modmailCheck);
		modmail.setOnCheckedChangeListener(new ModmailCheckListener());
		modmail.setChecked(settings.getBoolean("checkModmail", false));

		modqueue = (CheckBox) findViewById(R.id.modqueueCheck);
		modqueue.setOnCheckedChangeListener(new ModqueueCheckListener());
		modqueue.setChecked(settings.getBoolean("checkModqueue", false));
	}

	/**
	 * Log to logcat
	 * 
	 * @param s
	 *            String to write to logcat
	 */
	private void log(String s) {
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();
		Log.d(logTag, df.format(date) + ": " + s);
	}

}
