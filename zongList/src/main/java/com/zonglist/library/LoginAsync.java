package com.zonglist.library;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.zonglist.LoginActivity;
import com.zonglist.PlayLists;
import com.zonglist.library.DatabaseHandler;
import com.zonglist.R;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginAsync extends AsyncTask<String,JSONObject,JSONObject> {
    Context context;
    UserFunctions userFunction;
    TextView txtView;

    public LoginAsync(Context context){
        this.context = context.getApplicationContext();
    }

    protected JSONObject doInBackground(String... params) {
        LoginActivity.hideLoginerror();
        UserFunctions userFunction = new UserFunctions();
        if (params.length != 2)
            return null;
        JSONObject json = userFunction.loginUser(params[0], params[1]);
        return json;
    }
    protected void onPostExecute(JSONObject json) {
        // check for login response
        try {

            Log.e("JSON",json.getString("logged_in"));
            if ((json.getString("logged_in") != null) && (!json.getString("logged_in").equals("false"))) {
                Log.e("Got into creating db","1");
                //We are logged in -> Add the user to the database and head to the dashboard
                JSONObject user = json.getJSONObject("user");
                UserFunctions userFunction = new UserFunctions();
                userFunction.logoutUser(context);
                DatabaseHandler db = new DatabaseHandler(context);
                userFunction.logoutUser(context);
                db.addUser(user.getString("username"),user.getString("uid"),user.getString("password"));
                //Create new intent to dashboard
                Intent home = new Intent(context, PlayLists.class);
                home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(home);
            } else {
                Log.e("Got into error","2");
                //Canot login -> Show message
                LoginActivity.setLoginError();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}



