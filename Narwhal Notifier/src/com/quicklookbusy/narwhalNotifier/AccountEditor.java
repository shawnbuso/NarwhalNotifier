/*
 * AccountEditor.java
 * 
 * Defines the class which controls the Activity used to log in and out of the user's account
 * 
 * Copyright (C) Shawn Busolits, 2012 All Rights Reserved
 */

package com.quicklookbusy.narwhalNotifier;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import busoLibs.asyncRequest.AsyncRequest;
import busoLibs.asyncRequest.RequestCallback;

import com.google.ads.AdRequest;
import com.google.ads.AdView;

/**
 * An extension of Activity which presents and controls the account editing
 * screen
 * 
 * @author Shawn Busolits
 * @version 1.0
 */
public class AccountEditor extends Activity {

	/** Tag for log messages */
	String logTag = "AccountEditor";

	/** Field containing the username */
	EditText unameField;
	/** Field containing the password for the user */
	EditText passField;
	/** Button used to save info */
	Button saveButton;
	/** Button used to log out */
	Button logoutButton;
	/** Label used to give results of logging in or out */
	TextView loginFeedbackLabel;

	/** Used to store state about the app */
	SharedPreferences settings;
	/** Used to edit state about the app */
	Editor settingsEditor;
	
	/** Loading dialog for async login */
	ProgressDialog loadingDialog;
	
	/** Used to set feedback after login */
	Handler handler;

	/**
	 * Listens for clicks on the Save button
	 * 
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	public class SaveListener implements OnClickListener {

		/**
		 * Called when the user clicks the Save button
		 * 
		 * @param v
		 *            The view clicked to call this method
		 */
		public void onClick(View v) {
			hideKeyboard(v);
			/*
			 * Log in. If successful, save. If not, print error message
			 */
			String uname = unameField.getText().toString().trim();
			String pass = passField.getText().toString().trim();
			String api_type = "json";

			String url = "https://ssl.reddit.com/api/login/" + uname;
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
			nameValuePairs.add(new BasicNameValuePair("user", uname));
			nameValuePairs.add(new BasicNameValuePair("passwd", pass));
			nameValuePairs.add(new BasicNameValuePair("api_type", api_type));

			AsyncRequest req = new AsyncRequest(url, nameValuePairs,
					new LoginCallback(uname), AsyncRequest.REQUEST_TYPE.POST);
			loadingDialog = ProgressDialog.show(AccountEditor.this, "", "Loading. Please wait...", true);
			req.start();

		}
	}

	/**
	 * Callback for saync request to log in
	 * 
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	public class LoginCallback implements RequestCallback {

		private String uname;
		
		public LoginCallback(String uname) {
			this.uname = uname;
		}
		
		public void doOnResult(Object o) {
			String feedbackString = "";
			int feedbackColor = 0;
			boolean clearPasswordField = false;
			
			String jsonString = (String) o;
			try {
				JSONTokener tokener = new JSONTokener(jsonString);
				JSONObject jsonResult = new JSONObject(tokener);

				Log.d(logTag, "JSON Response: " + jsonResult.toString());

				JSONObject json = jsonResult.getJSONObject("json");
				JSONArray errors = json.getJSONArray("errors");
				if (errors.length() > 0) {
					feedbackString = "Error logging you in: "
							+ errors.getJSONArray(0).getString(1);
					feedbackColor = Color.RED;
					clearPasswordField = true;
					handler.post(new LoginResultRunner(feedbackString, feedbackColor, clearPasswordField));
					clearUserData();
					loadingDialog.dismiss();
					return;
				} else {
					feedbackString = "Success!";
					feedbackColor = Color.GREEN;
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
				feedbackString = "Error sending login info: " + e.toString();
				feedbackColor = Color.RED;
			} finally {
				handler.post(new LoginResultRunner(feedbackString, feedbackColor, clearPasswordField));
				loadingDialog.dismiss();
			}
		}
	}
	
	/**
	 * Modifies the screen when the user logs in
	 * 
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	public class LoginResultRunner implements Runnable {

		/** String to use as feedback */
		String feedbackString;
		/** Color for feedback */
		int feedbackColor;
		/** True if the password field should be cleared, false otherwise */
		boolean clearPasswordField;
		
		/**
		 * Initializes the class
		 * @param feedbackString String to use as feedback 
		 * @param feedbackColor Color for feedback
		 * @param clearPasswordField True if the password field should be cleared, false otherwise
		 */
		public LoginResultRunner(String feedbackString, int feedbackColor, boolean clearPasswordField) {
			this.feedbackString = feedbackString;
			this.feedbackColor = feedbackColor;
			this.clearPasswordField = clearPasswordField;
		}
		
		public void run() {
			loginFeedbackLabel.setText(feedbackString);
			loginFeedbackLabel.setTextColor(feedbackColor);
			if(clearPasswordField) {
				passField.setText("");
			}
		}
	}

	/**
	 * Listens for clicks on the Logout button
	 * 
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	public class LogoutListener implements OnClickListener {

		/**
		 * Called when the user clicks the Logout button
		 * 
		 * @param v
		 *            The view clicked to call this method
		 */
		public void onClick(View v) {
			hideKeyboard(v);
			if (settings.getBoolean("serviceRunning", true)) {
				loginFeedbackLabel
						.setText("Error - cannot log out while the service is running");
				loginFeedbackLabel.setTextColor(Color.RED);
			} else {
				clearUserData();
				unameField.setText("");
				loginFeedbackLabel.setText("Logged out");
				loginFeedbackLabel.setTextColor(Color.GREEN);
			}
		}

	}

	/**
	 * Listens for focus on the text fields
	 * 
	 * @author Shawn Busolits
	 * @version 1.0
	 */
	public class EditTextFocusListener implements OnFocusChangeListener {

		/**
		 * Called when the text field changes focus
		 * 
		 * @param v
		 *            View on which the focus changed
		 * @param hasFocus
		 *            True if the field gained focus, false if it lost focus
		 */
		public void onFocusChange(View v, boolean hasFocus) {
			if (hasFocus) {
				showKeyboard((EditText) v);
			} else {
				hideKeyboard((EditText) v);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.account_editor);
		
		handler = new Handler();

		settings = getSharedPreferences(NarwhalNotifier.PREFS_NAME, 0);
		settingsEditor = settings.edit();

		unameField = (EditText) findViewById(R.id.unameField);
		unameField.setOnFocusChangeListener(new EditTextFocusListener());
		String user = settings.getString("user", "");
		if (!user.equals("")) {
			unameField.setText(user);
		}

		passField = (EditText) findViewById(R.id.passField);
		passField.setOnFocusChangeListener(new EditTextFocusListener());

		saveButton = (Button) findViewById(R.id.saveButton);
		logoutButton = (Button) findViewById(R.id.logoutButton);
		loginFeedbackLabel = (TextView) findViewById(R.id.loginFeedbackLabel);

		loginFeedbackLabel.setText("");

		saveButton.setOnClickListener(new SaveListener());

		logoutButton.setOnClickListener(new LogoutListener());

		AdView adView = (AdView) this.findViewById(R.id.adView);
		adView.loadAd(new AdRequest());
	}

	/**
	 * Shows the virtual keyboard
	 * 
	 * @param et
	 *            Text field to have focus
	 */
	private void showKeyboard(EditText et) {
		InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		// only will trigger it if no physical keyboard is open
		mgr.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
	}

	/**
	 * Hides the virtual keyboard
	 * 
	 * @param v
	 *            View that lost focus
	 */
	private void hideKeyboard(View v) {
		InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}

	/**
	 * Clears the user data stored in the saved app state Used to "log out" the
	 * user
	 */
	private void clearUserData() {
		settingsEditor.putString("user", "");
		settingsEditor.putString("modhash", "");
		settingsEditor.putString("cookie", "");
		settingsEditor.commit();
	}
}
