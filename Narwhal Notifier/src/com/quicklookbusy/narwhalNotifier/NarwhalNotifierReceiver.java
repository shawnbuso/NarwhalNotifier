/*
 * NarwhalNotifierReceiver.java
 * 
 * Defines the class that receives the alarm from the AlarmManager to check for new messages
 * 
 * Copyright (C) Shawn Busolits, 2012 All Rights Reserved
 */

package com.quicklookbusy.narwhalNotifier;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import busoLibs.asyncRequest.AsyncRequest;
import busoLibs.asyncRequest.RequestCallback;

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
 * 
 * @author Shawn Busolits
 * @version 1.0
 */
public class NarwhalNotifierReceiver extends BroadcastReceiver {

	/** ID used to send message notifications */
	public static final int NOTIFICATION_ID = 86949977;
	
	/** ID used to send modmail notifications */
	public static final int MODMAIL_NOTIFICATION_ID = 86949978;

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
	 * 
	 * @param context
	 *            Context of the app
	 * @param i
	 *            Intent of the caller
	 */
	@Override
	public void onReceive(Context context, Intent i) {
		log("Alarm went off!");

		String ns = Context.NOTIFICATION_SERVICE;
		notificationManager = (NotificationManager) context
				.getSystemService(ns);

		settings = context.getSharedPreferences(NarwhalNotifier.PREFS_NAME, 0);
		settingsEditor = settings.edit();

		/*
		 * runService = true; RedditHitter hitter = new RedditHitter();
		 * hitter.start();
		 */

		update(context, false);
		update(context, true);
	}

	/**
	 * Checks if the user has any unread messages
	 * 
	 * @param context
	 *            Context of the app
	 */
	private void update(Context context, boolean modmail) {
		String url = "";
		if(modmail) {
			url = "http://www.reddit.com/message/moderator/unread/.json";
		} else {
			url = "http://www.reddit.com/message/unread/.json";
		}
		NameValuePair header = new BasicNameValuePair("Cookie",
				"reddit_session=" + settings.getString("cookie", ""));

		AsyncRequest req = new AsyncRequest(url, null, Arrays.asList(header),
				new UpdateCallback(context, modmail), AsyncRequest.REQUEST_TYPE.GET);
		req.start();

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

	public class UpdateCallback implements RequestCallback {
		/** Used for notifications */
		Context context;
		
		/** Used to keep state */
		boolean modmail;
		
		/**
		 * Initializes the callback with the context
		 * @param context Context of the app
		 */
		public UpdateCallback(Context context, boolean modmail) {
			this.context = context;
			this.modmail = modmail;
		}
		
		/**
		 * Reads the results and determines if a notification is required 
		 * @param o Object returned from AsyncRequest
		 */
		public void doOnResult(Object o) {
			String topTimeString = "";
			String contentSuffix = "";
			String intentURL = "";
			String contentTitle = "";
			String tickerText = "";
			int notificationID = 0;
			int icon = 0;
			if(modmail) {
				topTimeString = "topModmailMessageTime";
				contentSuffix = " new modmail messages. Click here to view them!";
				intentURL = "http://www.reddit.com/message/moderator";
				contentTitle = "New modmail message!";
				notificationID = MODMAIL_NOTIFICATION_ID;
				icon = R.drawable.mod_notification;
			} else {
				topTimeString = "topMessageTime";
				contentSuffix = " new reddit messages. Click here to view them!";
				intentURL = "http://www.reddit.com/message/unread";
				contentTitle = "New reddit message!";
				notificationID = NOTIFICATION_ID;
				icon = R.drawable.notification;
			}
			tickerText = contentTitle;
			
			try {
				String jsonString = (String) o;
				JSONTokener tokener = new JSONTokener(jsonString);
				JSONObject jsonResult = new JSONObject(tokener);

				JSONObject data = jsonResult.getJSONObject("data");
				settingsEditor.putString("modhash", data.getString("modhash"));
				settingsEditor.commit();

				JSONArray children = data.getJSONArray("children");

				int numMessages;
				if (children.length() == 0) {
					log("No new messages");
					notificationManager.cancel(NOTIFICATION_ID);
					numMessages = 0;
				} else {
					log("New messages!");
					numMessages = children.length();

					JSONObject topMessageData = children.getJSONObject(0)
							.getJSONObject("data");
					String createdString = topMessageData.getString("created");
					NumberFormat nf = new DecimalFormat("###.##");
					long topMessageTime = nf.parse(createdString).longValue();
					if (topMessageTime > settings.getLong(topTimeString, 0)) {
						log("Notifying");
						// Only notify on a new top message
						settingsEditor
								.putLong(topTimeString, topMessageTime);
						settingsEditor.commit();
						// Taken from
						// http://developer.android.com/guide/topics/ui/notifiers/notifications.html
						long when = System.currentTimeMillis();
						//CharSequence contentTitle = "New reddit message!";
						CharSequence contentText = "";
						if(numMessages > 1) {
							contentText = "You have " + numMessages + /*" new reddit messages. Click here to view them!"*/ contentSuffix;
						} else {
							String author = topMessageData.getString("author");
							String body = topMessageData.getString("body");
							String subreddit = topMessageData.getString("subreddit");
							contentText = author + " via " + subreddit + ": " + body;
						}

						Intent notificationIntent = new Intent(
								Intent.ACTION_VIEW,
								Uri.parse(/*"http://www.reddit.com/message/unread"*/intentURL));
						PendingIntent contentIntent = PendingIntent
								.getActivity(context, 0, notificationIntent, 0);

						// DEPRECATED - Notification notification = new
						// Notification(icon, tickerText, when);
						// DEPRECATED - notification.setLatestEventInfo(context,
						// contentTitle, contentText, contentIntent);
						NotificationCompat.Builder builder = new NotificationCompat.Builder(
								context);
						builder.setContentTitle(contentTitle)
								.setTicker(tickerText)
								.setContentText(contentText).setSmallIcon(icon)
								.setContentIntent(contentIntent).setWhen(when);
						Notification notification = builder.build();
						notification.defaults |= Notification.DEFAULT_SOUND;
						notification.defaults |= Notification.DEFAULT_VIBRATE;
						notification.flags |= Notification.FLAG_AUTO_CANCEL;

						notificationManager.notify(notificationID,
								notification);
					}
				}

			} catch (Exception e) {
				log("Error getting messages: " + e.toString());
			}

		}
	}
}
