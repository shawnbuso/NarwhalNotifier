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

public class AlarmHelper extends BroadcastReceiver {
	
	Context context;
	SharedPreferences settings;
	Editor settingsEditor;
	
	AlarmManager am;
	
	String logTag = "AlarmHelper";
	
	public AlarmHelper() {}
	
	public AlarmHelper(Context c)
	{
		context = c;
		settings = context.getSharedPreferences(NarwhalNotifier.PREFS_NAME, 0);
        settingsEditor = settings.edit();
	}

	@Override
	public void onReceive(Context c, Intent i) {
		Log.d(logTag, "Caught boot");
		context = c;
		settings = context.getSharedPreferences(NarwhalNotifier.PREFS_NAME, 0);
        settingsEditor = settings.edit();
		if(settings.getBoolean("serviceRunning", false) == true) {
			registerService();
		}
	}
	
	public void registerService() {
		//Taken from http://stackoverflow.com/questions/1082437/android-alarmmanager
		Log.d(logTag, "Registering service");
		am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, NarwhalNotifierService.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		Calendar time = Calendar.getInstance();
		time.setTimeInMillis(System.currentTimeMillis());
		time.add(Calendar.SECOND, 1);
		long interval = settings.getInt("frequency", 5) * 60 * 1000;
		am.setRepeating(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), interval, pi);
		settingsEditor.putBoolean("serviceRunning", true);
		settingsEditor.commit();
		Log.d(logTag, "Registered service");
	}
	
	public void unregisterService() {
		am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, NarwhalNotifierService.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		am.cancel(pi);
		settingsEditor.putBoolean("serviceRunning", false);
		settingsEditor.commit();
	}
}
