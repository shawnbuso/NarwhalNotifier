package com.quicklookbusy.narwhalNotifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class AccountEditor extends Activity {

	String logTag = "AccountEditor";
	
	EditText unameField;
	EditText passField;
	Button saveButton;
	Button logoutButton;
	TextView loginErrorLabel;
	
	SharedPreferences settings;
	Editor settingsEditor;

	public class SaveListener implements OnClickListener {
		public void onClick(View v) {
			/*
			 * Log in. If successful, save. If not, print error message
			 */
			String uname = unameField.getText().toString();
			String pass = passField.getText().toString();
			String api_type = "json";

			//Taken from http://www.androidsnippets.com/executing-a-http-post-request-with-httpclient
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost("https://ssl.reddit.com/api/login/" + uname);

			try {
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
				nameValuePairs.add(new BasicNameValuePair("user", uname));
				nameValuePairs.add(new BasicNameValuePair("passwd", pass));
				nameValuePairs.add(new BasicNameValuePair("api_type", api_type));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				
				HttpResponse response = httpclient.execute(httppost);
				
				//Taken from http://stackoverflow.com/questions/2845599/how-do-i-parse-json-from-a-java-httpresponse
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				String jsonString = reader.readLine();
				JSONTokener tokener = new JSONTokener(jsonString);
				JSONObject jsonResult = new JSONObject(tokener);
				
				Log.d(logTag, "JSON Response: " + jsonResult.toString());
				
				JSONObject json = jsonResult.getJSONObject("json");
				JSONArray errors = json.getJSONArray("errors");
				if(errors.length() > 0) {
					loginErrorLabel.setText("Error logging you in: " + errors.getJSONArray(0).getString(1));
					loginErrorLabel.setTextColor(Color.RED);
					return;
				}
				else {
					loginErrorLabel.setText("Success!");
					loginErrorLabel.setTextColor(Color.GREEN);
				}
				JSONObject data = json.getJSONObject("data");
				String modhash = (String) data.get("modhash");
				String cookie = (String) data.get("cookie");
				
				Log.d(logTag, "JSON Cookie: " + cookie);
				Log.d(logTag, "JSON Modhash: " + modhash);
				
				settingsEditor.putString("user", uname);
				settingsEditor.putString("modhash", modhash);
				settingsEditor.putString("cookie", cookie);
				settingsEditor.commit();

			} catch (Exception e) {
				Log.d(logTag, "Error sending login info: " + e.toString());
			}
		}
	}
	
	public class LogoutListener implements OnClickListener {

		public void onClick(View v) {
			settingsEditor.clear();
			settingsEditor.commit();
			loginErrorLabel.setText("Logged out");
			loginErrorLabel.setTextColor(Color.GREEN);
		}
		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.account_editor);

		unameField = (EditText) findViewById(R.id.unameField);
		passField = (EditText) findViewById(R.id.passField);
		saveButton = (Button) findViewById(R.id.saveButton);
		logoutButton = (Button) findViewById(R.id.logoutButton);
		loginErrorLabel = (TextView) findViewById(R.id.loginErrorLabel);
		
		loginErrorLabel.setText("");
		
		saveButton.setOnClickListener(new SaveListener());
		
		logoutButton.setOnClickListener(new LogoutListener());
		
		settings = getSharedPreferences(NarwhalNotifier.PREFS_NAME, 0);
		settingsEditor = settings.edit();
	}
}
