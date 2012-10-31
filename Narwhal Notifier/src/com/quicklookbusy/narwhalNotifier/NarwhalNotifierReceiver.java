/*
 * NarwhalNotifierReceiver.java
 * 
 * Defines the class that receives the alarm from the AlarmManager to check for new messages
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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
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
import busoLibs.asyncRequest.AsyncRequest;
import busoLibs.asyncRequest.RequestCallback;

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

	/** ID used to send modqueue notifications */
	public static final int MODQUEUE_NOTIFICATION_ID = 86949979;

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

		log("checkMessages is " + settings.getBoolean("checkMessages", false));
		if (settings.getBoolean("checkMessages", false)) {
			checkMessages(context, false);
		}
		log("checkModmail is " + settings.getBoolean("checkModmail", false));
		if (settings.getBoolean("checkModmail", false)) {
			checkMessages(context, true);
		}
		log("checkModqueue is " + settings.getBoolean("checkModqueue", false));
		if (settings.getBoolean("checkModqueue", false)) {
			checkModqueue(context);
		}
	}

	/**
	 * Checks if the user has any unread messages
	 * 
	 * @param context
	 *            Context of the app
	 */
	private void checkMessages(Context context, boolean modmail) {
		String url = "";
		if (modmail) {
			url = "http://www.reddit.com/message/moderator/unread/.json";
		} else {
			url = "http://www.reddit.com/message/unread/.json";
		}
		NameValuePair header = new BasicNameValuePair("Cookie",
				"reddit_session=" + settings.getString("cookie", ""));

		AsyncRequest req = new AsyncRequest(url, null, Arrays.asList(header),
				new MessageCallback(context, modmail),
				AsyncRequest.REQUEST_TYPE.GET);
		req.start();

	}

	/**
	 * Checks if there are any messages in the user's modqueues
	 * 
	 * @param context
	 *            Context of the app
	 */
	private void checkModqueue(Context context) {
		String url = "http://www.reddit.com/r/mod/about/modqueue.json";
		NameValuePair header = new BasicNameValuePair("Cookie",
				"reddit_session=" + settings.getString("cookie", ""));

		AsyncRequest req = new AsyncRequest(url, null, Arrays.asList(header),
				new ModqueueCallback(context), AsyncRequest.REQUEST_TYPE.GET);
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

	/**
	 * Called with the result of querying reddit for messages, both normal and
	 * moderator
	 * 
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	public class MessageCallback implements RequestCallback {
		/** Used for notifications */
		Context context;

		/** Used to keep state */
		boolean modmail;

		/**
		 * Initializes the callback with the context
		 * 
		 * @param context
		 *            Context of the app
		 */
		public MessageCallback(Context context, boolean modmail) {
			this.context = context;
			this.modmail = modmail;
		}

		/**
		 * Reads the results and determines if a notification is required
		 * 
		 * @param o
		 *            Object returned from AsyncRequest
		 */
		public void doOnResult(Object o) {
			String topTimeString = "";
			String contentSuffix = "";
			String intentURL = "";
			String contentTitle = "";
			String tickerText = "";
			String type = "";
			int notificationID = 0;
			int icon = 0;
			if (modmail) {
				topTimeString = "topModmailMessageTime";
				contentSuffix = " new modmail messages. Click here to view them!";
				intentURL = "http://www.reddit.com/message/moderator";
				type = "modmail ";
				notificationID = MODMAIL_NOTIFICATION_ID;
				icon = R.drawable.mod_notification;
			} else {
				topTimeString = "topMessageTime";
				contentSuffix = " new reddit messages. Click here to view them!";
				intentURL = "http://www.reddit.com/message/unread";
				notificationID = NOTIFICATION_ID;
				icon = R.drawable.notification;
			}
			// tickerText = contentTitle;

			try {
				String jsonString = (String) o;
				JSONTokener tokener = new JSONTokener(jsonString);
				JSONObject jsonResult = new JSONObject(tokener);

				JSONObject data = jsonResult.getJSONObject("data");
				settingsEditor.putString("modhash", data.getString("modhash"));
				settingsEditor.commit();

				JSONArray children = data.getJSONArray("children");

				int numMessages = children.length();
				if (numMessages == 0) {
					log("No new messages");
					notificationManager.cancel(notificationID);
				} else {
					log("New messages!");

					JSONObject topMessageData = children.getJSONObject(0)
							.getJSONObject("data");
					String createdString = topMessageData.getString("created");
					NumberFormat nf = new DecimalFormat("###.##");
					long topMessageTime = nf.parse(createdString).longValue();
					if (topMessageTime > settings.getLong(topTimeString, 0)) {
						log("Notifying");
						// Only notify on a new top message
						settingsEditor.putLong(topTimeString, topMessageTime);
						settingsEditor.commit();
						// Taken from
						// http://developer.android.com/guide/topics/ui/notifiers/notifications.html
						long when = System.currentTimeMillis();
						CharSequence contentText = "";
						if (numMessages > 1) {
							contentTitle = numMessages + " new " + type
									+ "messages";
							tickerText = contentTitle;
							contentText = "You have " + numMessages
									+ contentSuffix;
						} else {
							contentTitle = "1 new " + type + "message";
							tickerText = contentTitle;
							String author = topMessageData.getString("author");
							String body = topMessageData.getString("body");
							String subreddit = topMessageData
									.getString("subreddit");
							contentText = author + " via " + subreddit + ": "
									+ body;
						}

						// Build list of messages for expanded notification
						ArrayList<String> expandedContentTexts = new ArrayList<String>();
						for (int i = 0; i < children.length(); i++) {
							JSONObject child = children.getJSONObject(i)
									.getJSONObject("data");
							String author = child.getString("author");
							String body = child.getString("body");
							String subreddit = child.getString("subreddit");
							String expandedContentText = author + " via "
									+ subreddit + ": " + body;
							expandedContentTexts.add(expandedContentText);
						}

						Intent notificationIntent = new Intent(
								Intent.ACTION_VIEW, Uri.parse(intentURL));
						PendingIntent contentIntent = PendingIntent
								.getActivity(context, 0, notificationIntent, 0);

						NotificationCompat.Builder builder = new NotificationCompat.Builder(
								context);
						builder.setContentTitle(contentTitle)
								.setTicker(tickerText)
								.setContentText(contentText).setSmallIcon(icon)
								.setContentIntent(contentIntent).setWhen(when);

						NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
						inboxStyle.setBigContentTitle(contentTitle);
						for (String expandedContentText : expandedContentTexts) {
							inboxStyle.addLine(expandedContentText);
						}

						builder.setStyle(inboxStyle);

						Notification notification = builder.build();
						notification.defaults |= Notification.DEFAULT_SOUND;
						notification.defaults |= Notification.DEFAULT_VIBRATE;
						notification.flags |= Notification.FLAG_AUTO_CANCEL;

						notificationManager
								.notify(notificationID, notification);
					}
				}

			} catch (Exception e) {
				log("Error getting messages: " + e.toString());
			}

		}
	}

	/**
	 * Called with the result of querying reddit for modqueue info
	 * 
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	public class ModqueueCallback implements RequestCallback {

		/** Used for notifications */
		Context context;

		public ModqueueCallback(Context context) {
			this.context = context;
		}

		/**
		 * Reads the results and updates settings editor with a list of full
		 * modqueues
		 * 
		 * @param o
		 *            Object returned from AsyncRequest
		 */
		public void doOnResult(Object o) {
			settingsEditor.putLong("prevTopModqueueTime",
					settings.getLong("currentTopModqueueTime", 0));
			settingsEditor.commit();

			try {
				String jsonString = (String) o;
				JSONTokener tokener = new JSONTokener(jsonString);
				JSONObject jsonResult = new JSONObject(tokener);

				JSONObject data = jsonResult.getJSONObject("data");
				settingsEditor.putString("modhash", data.getString("modhash"));
				settingsEditor.commit();

				JSONArray children = data.getJSONArray("children");

				int numMessages = children.length();
				if (numMessages == 0) {
					log("No new modqueues");
					notificationManager.cancel(MODQUEUE_NOTIFICATION_ID);
				} else {
					log("New modqueues!");

					JSONObject topMessageData = children.getJSONObject(0)
							.getJSONObject("data");
					String createdString = topMessageData.getString("created");
					NumberFormat nf = new DecimalFormat("###.##");
					long topMessageTime = nf.parse(createdString).longValue();
					if (topMessageTime > settings.getLong("topModqueueTime", 0)) {
						log("Notifying");
						// Only notify on a new top message
						settingsEditor.putLong("topModqueueTime",
								topMessageTime);
						settingsEditor.commit();
						// Taken from
						// http://developer.android.com/guide/topics/ui/notifiers/notifications.html
						long when = System.currentTimeMillis();

						CharSequence contentText = "";
						if (numMessages == 1) {
							contentText = "1 new item in your modqueue!";
						} else {
							contentText = numMessages
									+ " new items in your modqueue";
						}

						// Build list of messages for expanded notification
						ArrayList<String> expandedContentTexts = new ArrayList<String>();
						for (int i = 0; i < children.length(); i++) {
							JSONObject child = children.getJSONObject(i)
									.getJSONObject("data");
							String author = child.getString("author");
							String title = child.getString("title");
							String subreddit = child.getString("subreddit");
							String expandedContentText = author + " via "
									+ subreddit + ": " + title;
							expandedContentTexts.add(expandedContentText);
						}

						Intent notificationIntent = new Intent(
								Intent.ACTION_VIEW,
								Uri.parse("http://www.reddit.com/r/mod/about/modqueue"));
						PendingIntent contentIntent = PendingIntent
								.getActivity(context, 0, notificationIntent, 0);

						NotificationCompat.Builder builder = new NotificationCompat.Builder(
								context);
						builder.setContentTitle("New modqueue items")
								.setTicker("New modqueue items")
								.setContentText(contentText)
								.setSmallIcon(R.drawable.mod_notification)
								.setContentIntent(contentIntent).setWhen(when);

						NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
						inboxStyle.setBigContentTitle("New modqueue items");
						for (String expandedContentText : expandedContentTexts) {
							inboxStyle.addLine(expandedContentText);
						}

						builder.setStyle(inboxStyle);

						Notification notification = builder.build();
						notification.defaults |= Notification.DEFAULT_SOUND;
						notification.defaults |= Notification.DEFAULT_VIBRATE;
						notification.flags |= Notification.FLAG_AUTO_CANCEL;

						notificationManager.notify(MODQUEUE_NOTIFICATION_ID,
								notification);
					}
				}

			} catch (Exception e) {
				log("Error getting messages: " + e.toString());
			}
		}
	}
}
