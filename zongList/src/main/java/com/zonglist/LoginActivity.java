package com.zonglist;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.zonglist.library.DatabaseHandler;
import com.zonglist.library.LoginAsync;
import com.zonglist.library.UserFunctions;

public class LoginActivity extends ActionBarActivity {
    private static TextView loginErrorMsg;
    Button btnLogin;
    Button btnLinkToRegister;
    EditText inputEmail;
    EditText inputPassword;

    // JSON Response node names
    private static String KEY_SUCCESS = "success";
    private static String KEY_ERROR = "error";
    private static String KEY_ERROR_MSG = "error_msg";
    private static String KEY_UID = "uid";
    private static String KEY_NAME = "name";
    private static String KEY_EMAIL = "email";
    private static String KEY_CREATED_AT = "created_at";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        if(ICBootstrap(this)) {

            //Check if the user is already logged in
            UserFunctions uf = new UserFunctions();
            if (!uf.isUserLoggedIn(this)) {
                // Importing all assets like buttons, text fields
                inputEmail = (EditText) findViewById(R.id.username);
                inputPassword = (EditText) findViewById(R.id.password);
                btnLogin = (Button) findViewById(R.id.loginAction);
                loginErrorMsg = (TextView) findViewById(R.id.login_error);

                // Login button Click Event
                btnLogin.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View view) {
                        String email = inputEmail.getText().toString();
                        String password = inputPassword.getText().toString();
                        new LoginAsync(getApplicationContext()).execute(email, password);
                    }
                });
            } else {
                //User is logged in -> Go to the playlists activity
                Intent in = new Intent(this, PlayLists.class);
                in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(in);
                finish();
            }
        }
    }

    public static void setLoginError(){
        loginErrorMsg.setText("Invalid credentials. Try again!");
        loginErrorMsg.setVisibility(View.VISIBLE);
    }

    public static void hideLoginerror(){
        loginErrorMsg.setVisibility(View.INVISIBLE);
    }


    public boolean ICBootstrap(Context context){
        if(!isNetworkConnected()){
            //Not connected to the internet. Go to the no internet activity
            Intent intent = new Intent(context,NoConnection.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return false;
        } else {
            return true;
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            return false;
        } else
            return true;
    }
}

