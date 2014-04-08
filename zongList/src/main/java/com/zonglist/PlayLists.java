package com.zonglist;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.zonglist.library.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class PlayLists extends ActionBarActivity {


    ListView list;
    TextView title;
    Button viewList;
    ArrayList<HashMap<String,String>> playlists = new ArrayList<HashMap<String,String>>();

    private static final String PL_NAME = "name";
    private static final String PL_ID = "list_id";
    private static final String PL_COUNT ="count";
    private static final String PL_CREATED ="date_created";

    UserFunctions userFunctions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(ICBootstrap(this)) {

            ActionBar actionBar = getActionBar();
            actionBar.setTitle("Select playlist");
            //Check login
            userFunctions = new UserFunctions();
            if (userFunctions.isUserLoggedIn(getApplicationContext())) {
                //User is logged in
                setContentView(R.layout.activity_play_lists);
                new LoadPlaylistAsync().execute();
            } else {
                //User is not logged in -> Redirect to the login activity
                Intent login = new Intent(getApplicationContext(), LoginActivity.class);
                login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(login);
                //Closing dashboard screen
                finish();
            }
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.play_lists, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //Go to settings intent
        if (id == R.id.action_settings) {
            return true;
        }

        //Do logout
        if(id == R.id.action_logout){
            UserFunctions userFunction = new UserFunctions();
            userFunction.logoutUser(getApplicationContext());
            //We've logged out the user. Go to the login intent
            Intent login = new Intent(getApplicationContext(), LoginActivity.class);
            login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(login);
            //Closing dashboard screen
            finish();
        }


        return super.onOptionsItemSelected(item);
    }




    /* Background loader for playlists */

    class LoadPlaylistAsync extends AsyncTask<String,JSONObject,JSONObject>{

        @Override
        protected JSONObject doInBackground(String... params) {
            APIObject api = new APIObject();
            JSONObject json = api.getPlaylists(getApplicationContext());
            return json;
        }

        protected void onPostExecute(JSONObject json) {
            try {
                //We have the JSON with the playlists. Iterate trough it and apply it to the list
                String PL_data = json.getString("data");
                //Iterate trough playlists
                JSONArray data = new JSONArray(PL_data);
                for(int i = 0; i < data.length(); i++){
                    JSONObject j = data.getJSONObject(i);
                    String pl_name = j.getString(PL_NAME);
                    String pl_id = j.getString(PL_ID);
                    String count = j.getString(PL_COUNT);
                    String date_created = j.getString(PL_CREATED);
                    HashMap<String,String> map = new HashMap<String, String>();
                    map.put(PL_NAME,pl_name);
                    map.put(PL_ID,pl_id);
                    map.put(PL_COUNT,count);
                    map.put(PL_CREATED,date_created);
                    playlists.add(map);
                    list = (ListView) findViewById(R.id.my_playlists);
                    ListAdapter adapter = new SimpleAdapter(PlayLists.this,playlists,R.layout.dash_playlist_item,
                            new String[] {PL_NAME,PL_COUNT,PL_CREATED}, new int[] {R.id.playlist_name,R.id.playlist_count,R.id.date_created});
                    list.setAdapter(adapter);

                    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            HashMap<String,String> val = (HashMap<String,String>)list.getItemAtPosition(i);
                            String list_id = val.get("list_id");
                            String list_name = val.get("name");
                            //Create a new intent and go to playlist view
                            Intent viewSingle = new Intent(getApplicationContext(),SinglePlaylistAcvtivity.class);
                            viewSingle.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            viewSingle.putExtra("playlist_id",list_id);
                            viewSingle.putExtra("list_name",list_name);
                            startActivity(viewSingle);
                            finish();
                        }
                    });
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


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