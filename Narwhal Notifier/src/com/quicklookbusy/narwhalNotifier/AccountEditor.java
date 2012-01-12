package com.quicklookbusy.narwhalNotifier;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class AccountEditor extends Activity {
	
	EditText unameField;
    EditText passField;
    Button saveButton;
    Button logoutButton;
    TextView loginErrorLabel;
	
	public class SaveListener implements OnClickListener {
		public void onClick(View v) {
			/*Log in
			 *If successful, save
			 *If not, print error message
			 */
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
        
	}
}
