/*
 * AlarmHelper.java
 * 
 * Defines the class which is used to register with the AlarmManager and 
 * catches the boot broadcast to register the service at boot.
 * 
 * Copyright 2012 Shawn Busolits
 * Licensed under the Apache License, Version 2.0 (the "License"); you may 
 * not use this file except in compliance with the License. You may obtain a 
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 */

package com.quicklookbusy.narwhalNotifier;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * An extension of BroadcastReceiver which is used to register with the
 * AlarmManager and catches the boot broadcast to register the service at boot.
 * 
 * @author Shawn Busolits
 * @version 1.0
 */
public class AlarmHelper extends BroadcastReceiver {

	/** Context of the ap */
	Context context;
	/** Used to store app state */
	SharedPreferences settings;
	/** Used to edit stored app state */
	Editor settingsEditor;

	/** Used to register the service */
	AlarmManager am;

	/** Log tag */
	String logTag = "AlarmHelper";

	/** Null constructor */
	public AlarmHelper() {
	}

	/**
	 * Initializes the AlarmHelper with the settings and settings editor
	 * 
	 * @param c
	 *            Context of the app
	 */
	public AlarmHelper(Context c) {
		context = c;
		settings = context.getSharedPreferences(NarwhalNotifier.PREFS_NAME, 0);
		settingsEditor = settings.edit();
	}

	/**
	 * Catches the boot broadcast and registers the service IF it was registered
	 * when the phone was last shut down
	 * 
	 * @param c
	 *            Cotnext of the ap
	 * @param i
	 *            Intent calling this method
	 */
	@Override
	public void onReceive(Context c, Intent i) {
		Log.d(logTag, "Caught boot");
		context = c;
		settings = context.getSharedPreferences(NarwhalNotifier.PREFS_NAME, 0);
		settingsEditor = settings.edit();
		if (settings.getBoolean("serviceRunning", false) == true) {
			registerService();
		}
	}

	/**
	 * Registers the service with the AlarmManager
	 */
	public void registerService() {
		// Taken from
		// http://stackoverflow.com/questions/1082437/android-alarmmanager
		Log.d(logTag, "Registering service");
		am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, NarwhalNotifierReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		Calendar time = Calendar.getInstance();
		time.setTimeInMillis(System.currentTimeMillis());
		time.add(Calendar.SECOND, 1);
		long interval = settings.getInt("frequency", 5) * 60 * 1000;
		am.setRepeating(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(),
				interval, pi);
		settingsEditor.putBoolean("serviceRunning", true);
		settingsEditor.commit();
		Log.d(logTag, "Registered service");
	}

	/**
	 * Removes the service from registration with the AlarmManager
	 */
	public void unregisterService() {
		am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, NarwhalNotifierReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		am.cancel(pi);
		settingsEditor.putBoolean("serviceRunning", false);
		settingsEditor.commit();
	}
}
