package com.quicklookbusy.narwhalNotifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

public class NarwhalNotifier extends Activity {
	
	public static final String PREFS_NAME = "NarwhalNotifierPrefs";
	
	ToggleButton serviceButton;
	Spinner frequencySpinner;
	TextView serviceErrorLabel;
	
	String logTag = "NarwhalNotifier";
	
	Intent service;
	
	SharedPreferences settings;
	
	public class AccountEditListener implements OnClickListener {
		
		public void onClick(View v) {
			Intent accountActivity = new Intent(NarwhalNotifier.this, AccountEditor.class);
			startActivity(accountActivity);
		}
	}
	
	public class ServiceListener implements OnClickListener {

		public void onClick(View v) {
				if(serviceButton.isChecked()) {
					
					if(settings.getString("user", "").equals("")) {
						serviceErrorLabel.setText("Error: No user logged in");
						serviceErrorLabel.setTextColor(Color.RED);
					}
					//service = new Intent(NarwhalNotifier.this, NarwhalNotifierService.class);
					//startService(service);
					
					//Taken from http://www.androidsnippets.com/executing-a-http-post-request-with-httpclient
					HttpClient httpclient = new DefaultHttpClient();
					String url = "http://www.reddit.com/message/unread/.json";
					HttpGet httpget = new HttpGet(url);
					//httpget.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
					httpget.setHeader("Cookie", "reddit_session=" + settings.getString("cookie", ""));
					
					//String modhash = settings.getString("modhash", "");

					try {
						//List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
						//nameValuePairs.add(new BasicNameValuePair("uh", modhash));
						//httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
						
						HttpResponse response = httpclient.execute(httpget);
						
						//Taken from http://stackoverflow.com/questions/2845599/how-do-i-parse-json-from-a-java-httpresponse
						BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
						String jsonString = reader.readLine();
						Log.d(logTag, "First Response: " + jsonString.toString());
						JSONTokener tokener = new JSONTokener(jsonString);
						JSONObject jsonResult = new JSONObject(tokener);
						
						Log.d(logTag, "JSON Response: " + jsonResult.toString());
					} catch (Exception e) {
						Log.d(logTag, "Error sending login info: " + e.toString());
					}
				}
				else {
					//Kill service
					if(settings.getBoolean("serviceRunning", false)) {
						stopService(service);
						serviceErrorLabel.setText("Service stopped");
						serviceErrorLabel.setTextColor(Color.GREEN);
					}
					else {
						serviceErrorLabel.setText("Error: Service not running");
						serviceErrorLabel.setTextColor(Color.RED);	
					}
				}
		}		
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        settings = getSharedPreferences(PREFS_NAME, 0);
        
        LinearLayout accountEditTrigger = (LinearLayout) findViewById(R.id.accountEditTrigger);
        accountEditTrigger.setOnClickListener(new AccountEditListener());
        
        TextView subText = (TextView) findViewById(R.id.accountSubtext);
        String user = settings.getString("user", "");
        if(user.equals("")) {
        	subText.setText("Not currently logged in. Click to log in.");
        }
        else {
        	subText.setText("Currently logged in as " + user);
        }
        
        frequencySpinner = (Spinner) findViewById(R.id.frequencySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.planets_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencySpinner.setAdapter(adapter);
        
        serviceButton = (ToggleButton) findViewById(R.id.serviceToggle);
        serviceButton.setOnClickListener(new ServiceListener());
        
        serviceErrorLabel = (TextView) findViewById(R.id.serviceErrorLabel);
    }
}