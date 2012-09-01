/*
 * NarwhalNotifierReceiver.java
 * 
 * Defines the class that receives the alarm from the AlarmManager to check for new messages
 * 
 * Copyright (C) Shawn Busolits, 2012 All Rights Reserved
 */

package com.quicklookbusy.narwhalNotifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * An extension of BroadcastReceiver used to check for unread messages
 * @author Shawn Busolits
 * @version 1.0
 */
public class NarwhalNotifierReceiver extends BroadcastReceiver {
	
	/** ID used to send notifications */
	public static final int NOTIFICATION_ID = 86949977;
	
	/** Holds state about whether the service is running */
	volatile boolean runService;
	
	/** Used to send notifications to the user */
	NotificationManager notificationManager;
	
	/** Tag for log messages */
	String logTag = "NarwhalNotifierService";
	
	/** Used to store state about the app */
	SharedPreferences settings;
	/** Used to edit state about the app */
	Editor settingsEditor;
	
	/**
	 * Called by the AlarmManager. Checks if the user has any unread messages
	 * @param context Context of the app
	 * @param i Intent of the caller
	 */
	@Override
	public void onReceive(Context context, Intent i) {
		log("Alarm went off!");
        
        String ns = Context.NOTIFICATION_SERVICE;
        notificationManager = (NotificationManager) context.getSystemService(ns);
        
        settings = context.getSharedPreferences(NarwhalNotifier.PREFS_NAME, 0);
        settingsEditor = settings.edit();
        
        /*runService = true;
        RedditHitter hitter = new RedditHitter();
        hitter.start();*/
			
        update(context);		
	}
	
	/**
	 * Checks if the user has any unread messages
	 * @param context Context of the app
	 */
	private void update(Context context) {
		log("In loop");
		//Taken from http://stackoverflow.com/questions/693997/how-to-set-httpresponse-timeout-for-android-in-java
		HttpParams httpParams = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		// The default value is zero, that means the timeout is not used. 
		int timeoutConnection = 10000;
		HttpConnectionParams.setConnectionTimeout(httpParams, timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT) 
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = 10000;
		HttpConnectionParams.setSoTimeout(httpParams, timeoutSocket);
		
		//Taken from http://www.androidsnippets.com/executing-a-http-post-request-with-httpclient
		HttpClient httpclient = new DefaultHttpClient(httpParams);
		String url = "http://www.reddit.com/message/unread/.json";
		HttpGet httpget = new HttpGet(url);
		httpget.setHeader("Cookie", "reddit_session=" + settings.getString("cookie", ""));

		try {
			
			HttpResponse response = httpclient.execute(httpget);
			
			//Taken from http://stackoverflow.com/questions/2845599/how-do-i-parse-json-from-a-java-httpresponse
			BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			String jsonString = reader.readLine();
			log("First Response: " + jsonString.toString());
			JSONTokener tokener = new JSONTokener(jsonString);
			JSONObject jsonResult = new JSONObject(tokener);
			log("JSON Response: " + jsonResult.toString());
			
			JSONObject data = jsonResult.getJSONObject("data");
			settingsEditor.putString("modhash", data.getString("modhash"));
			settingsEditor.commit();
			
			JSONArray children = data.getJSONArray("children");
			
			
			if(children.length() == 0) {
				log("No new messages");
				notificationManager.cancel(NOTIFICATION_ID);
			}
			else {
				log("New messages!");
				
				JSONObject topMessageData = children.getJSONObject(0).getJSONObject("data");
				log("Created: " + topMessageData.getString("created"));
				String createdString = topMessageData.getString("created");
				NumberFormat nf = new DecimalFormat("###.##");				
				long topMessageTime = nf.parse(createdString).longValue();
				log("topMessageData: " + topMessageData.toString());				
				log("Top message time: " + settings.getLong("topMessageTime", 0));
				log("Current message time: " + topMessageTime);
				if(topMessageTime > settings.getLong("topMessageTime", 0)) {
					log("Notifying");
					//Only notify on a new top message
					settingsEditor.putLong("topMessageTime", topMessageTime);
					settingsEditor.commit();
					//Taken from http://developer.android.com/guide/topics/ui/notifiers/notifications.html
					int icon = R.drawable.notification;
					CharSequence tickerText = "New reddit message!";
					long when = System.currentTimeMillis();
					//Context context = getApplicationContext();
					CharSequence contentTitle = "New reddit message!";
					CharSequence contentText = "You have a new reddit message! Click here to view it!";
					
					Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.reddit.com/message/unread"));
					PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
					
					//DEPRECATED - Notification notification = new Notification(icon, tickerText, when);
					//DEPRECATED - notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
					NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
					builder.setContentTitle(contentTitle)
						.setTicker(tickerText)
						.setContentText(contentText)
						.setSmallIcon(icon)
						.setContentIntent(contentIntent)
						.setWhen(when);
					Notification notification = builder.build();
					notification.defaults |= Notification.DEFAULT_SOUND;
					notification.defaults |= Notification.DEFAULT_VIBRATE;
					notification.flags |= Notification.FLAG_AUTO_CANCEL;
				
					notificationManager.notify(NOTIFICATION_ID, notification);
				}
			}
			
		} catch (Exception e) {
			log("Error getting messages: " + e.toString());
		}
	}
	
	/**
	 * Log to logcat
	 * @param s String to write to logcat
	 */
	private void log(String s) {
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();
		Log.d(logTag, df.format(date) + ": " + s);
	}
}
