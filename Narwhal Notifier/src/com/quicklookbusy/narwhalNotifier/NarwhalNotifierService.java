package com.quicklookbusy.narwhalNotifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

public class NarwhalNotifierService extends Service {
	
	public static final int NOTIFICATION_ID = 86949977;
	
	volatile boolean runService;
	
	NotificationManager notificationManager;
	
	String logTag = "NarwhalNotifierService";
	
	SharedPreferences settings;
	Editor settingsEditor;
	
	class RedditHitter extends Thread {
		public void run() {
			while(runService) {
				Log.d(logTag, "In loop");
				//Taken from http://www.androidsnippets.com/executing-a-http-post-request-with-httpclient
				HttpClient httpclient = new DefaultHttpClient();
				String url = "http://www.reddit.com/message/unread/.json";
				HttpGet httpget = new HttpGet(url);
				httpget.setHeader("Cookie", "reddit_session=" + settings.getString("cookie", ""));

				try {
					
					HttpResponse response = httpclient.execute(httpget);
					
					//Taken from http://stackoverflow.com/questions/2845599/how-do-i-parse-json-from-a-java-httpresponse
					BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
					String jsonString = reader.readLine();
					Log.d(logTag, "First Response: " + jsonString.toString());
					JSONTokener tokener = new JSONTokener(jsonString);
					JSONObject jsonResult = new JSONObject(tokener);
					Log.d(logTag, "JSON Response: " + jsonResult.toString());
					
					JSONObject data = jsonResult.getJSONObject("data");
					settingsEditor.putString("modhash", data.getString("modhash"));
					
					JSONArray children = data.getJSONArray("children");
					
					
					if(children.length() == 0) {
						Log.d(logTag, "No new messages");
					}
					else {
						Log.d(logTag, "New messages!");
						
						JSONObject topMessageData = children.getJSONObject(0).getJSONObject("data");
						String topMessageName = topMessageData.getString("name");
						Log.d(logTag, "topMessageData: " + topMessageData.toString());
						
						Log.d(logTag, "Top message name: " + settings.getString("topMessageName", ""));
						Log.d(logTag, "Current message name: " + topMessageName);
						if(!topMessageName.equals(settings.getString("topMessageName", ""))) {
							Log.d(logTag, "Notifying");
							//Only notify on a new top message
							settingsEditor.putString("topMessageName", topMessageName);
							settingsEditor.commit();
							//Taken from http://developer.android.com/guide/topics/ui/notifiers/notifications.html
							int icon = R.drawable.icon;
							CharSequence tickerText = "New reddit message!";
							long when = System.currentTimeMillis();
							Context context = getApplicationContext();
							CharSequence contentTitle = "New reddit message!";
							CharSequence contentText = "You have a new reddit message! Click here to view it!";
							
							Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.reddit.com/message/unread"));
							PendingIntent contentIntent = PendingIntent.getActivity(NarwhalNotifierService.this, 0, notificationIntent, 0);
							
							Notification notification = new Notification(icon, tickerText, when);
							notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
							notification.defaults |= Notification.DEFAULT_SOUND;
							notification.defaults |= Notification.DEFAULT_VIBRATE;
							notification.flags |= Notification.FLAG_AUTO_CANCEL;
						
							notificationManager.notify(NOTIFICATION_ID, notification);
						}
					}
					
				} catch (Exception e) {
					Log.d(logTag, "Error getting messages: " + e.toString());
				}
				
				try {
					Thread.currentThread().sleep((settings.getInt("frequency", 5) * 60 * 1000));
				} catch(Exception e) {
					Log.d(logTag, "Error sleeping: " + e.toString());
				}
			}
			stopSelf();
		}
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        
        String ns = Context.NOTIFICATION_SERVICE;
        notificationManager = (NotificationManager) getSystemService(ns);
        
        settings = getSharedPreferences(NarwhalNotifier.PREFS_NAME, 0);
        settingsEditor = settings.edit();
        
        runService = true;
        RedditHitter hitter = new RedditHitter();
        hitter.start();
        
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
	
	@Override
	public void onDestroy() {
		runService = false;
		Log.d(logTag, "In onDestroy()");
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
