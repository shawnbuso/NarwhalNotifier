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
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
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
			hideKeyboard(v);
			/*
			 * Log in. If successful, save. If not, print error message
			 */
			String uname = unameField.getText().toString();
			String pass = passField.getText().toString();
			String api_type = "json";

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
				loginErrorLabel.setText("Error sending login info: " + e.toString());
			}
		}
	}
	
	public class LogoutListener implements OnClickListener {

		public void onClick(View v) {
			hideKeyboard(v);
			settingsEditor.clear();
			settingsEditor.commit();
			Log.d(logTag, "User: " + settings.getString("user", ""));
			loginErrorLabel.setText("Logged out");
			loginErrorLabel.setTextColor(Color.GREEN);
		}
		
	}
	
	public class EditTextFocusListener implements OnFocusChangeListener {

		public void onFocusChange(View v, boolean hasFocus) {
			if(hasFocus) {
				showKeyboard((EditText) v);
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.account_editor);
		
		settings = getSharedPreferences(NarwhalNotifier.PREFS_NAME, 0);
		settingsEditor = settings.edit();

		unameField = (EditText) findViewById(R.id.unameField);
		unameField.setOnFocusChangeListener(new EditTextFocusListener());
		String user = settings.getString("user", "");
		if(!user.equals("")) {
			unameField.setText(user);
		}
		
		passField = (EditText) findViewById(R.id.passField);
		passField.setOnFocusChangeListener(new EditTextFocusListener());
		
		saveButton = (Button) findViewById(R.id.saveButton);
		logoutButton = (Button) findViewById(R.id.logoutButton);
		loginErrorLabel = (TextView) findViewById(R.id.loginErrorLabel);
		
		loginErrorLabel.setText("");
		
		saveButton.setOnClickListener(new SaveListener());
		
		logoutButton.setOnClickListener(new LogoutListener());
	}
	
	private void showKeyboard(EditText et) {
		InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		// only will trigger it if no physical keyboard is open
		mgr.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
	}
	
	private void hideKeyboard(View v) {
		InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}
}
