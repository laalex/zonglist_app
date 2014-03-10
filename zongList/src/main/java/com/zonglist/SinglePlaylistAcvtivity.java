package com.zonglist;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
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
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.zonglist.library.APIObject;
import com.zonglist.library.DatabaseHandler;
import com.zonglist.library.UserFunctions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import static android.os.Environment.getExternalStorageDirectory;

public class SinglePlaylistAcvtivity extends ActionBarActivity {
    public int numOfTasks = 0;
    DatabaseHandler db;
    ProgressDialog preloader;
    ListAdapter adapter;
    ListView list;
    TextView title;
    String list_id;
    String list_name;
    Button viewList;
    ArrayList<HashMap<String,String>> songs = new ArrayList<HashMap<String,String>>();

    public String[] thisUrl;

    String storageLocation;

    private static final String S_NAME = "song_name";
    private static final String S_ID = "song_id";
    private static final String S_DOWNLOAD = "download_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_playlist_acvtivity);

        if(ICBootstrap(this)) {

            Intent intent = getIntent();
            list_id = intent.getStringExtra("playlist_id");
            list_name = intent.getStringExtra("list_name");

            android.support.v7.app.ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle(list_name);
            actionBar.setDisplayHomeAsUpEnabled(true);

            preloader = new ProgressDialog(this);
            preloader.setIndeterminate(false);
            preloader.setCancelable(false);

            storageLocation = Environment.getExternalStorageDirectory().toString();

            db = new DatabaseHandler(getApplicationContext());

            new LoadPlaylistAsync().execute(list_id);

        }

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.single_playlist_acvtivity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        if(id == android.R.id.home){
            Intent home =new Intent(this, PlayLists.class);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /* Download entire playlist at once */
    public void castDownloadPlaylist(View view){
        if(list != null){
            //The list is not empty: Get forward and download all the songs
            Integer count = adapter.getCount();//Get the number of songs
            preloader.setMax(0);
            preloader.setProgress(0);
            preloader.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            preloader.setTitle("Downloading playlist: "+list_name);
            preloader.show();
            for(int i = 0; i<count;i++){
                HashMap<String,String> song = ((HashMap<String,String>)adapter.getItem(i));
                //Song represents the details of the current song
                new DownloadFile().execute(song.get("song_name"),"true");
            }
            //Delay for a few seconds
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    preloader.dismiss();
                    Toast.makeText(getApplicationContext(), "Your playlist "+list_name+" is being downloaded in background. Please wait or close the application. You will be notified when the playlist will be downloaded",
                            5000).show();
                }
            }, 3000);

        } else {
            Button dld = (Button) findViewById(R.id.action_download_all);
            dld.setVisibility(View.INVISIBLE);
        }
    }


    /* Background loader for playlists */

    class LoadPlaylistAsync extends AsyncTask<String,JSONObject,JSONObject> {

        @Override
        protected JSONObject doInBackground(String... params) {
            APIObject api = new APIObject();
            JSONObject json = api.getPlaylist(getApplicationContext(),params[0]);
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
                    String s_name = j.getString(S_NAME);
                    s_name = s_name.replace('_',' ');//Replace the underlines
                    String s_id = j.getString(S_ID);
                    String s_down = j.getString(S_DOWNLOAD);
                    HashMap<String,String> map = new HashMap<String, String>();
                    map.put(S_NAME,s_name);
                    map.put(S_ID,s_id);
                    map.put(S_DOWNLOAD,s_down);
                    songs.add(map);
                    list = (ListView) findViewById(R.id.playlist_songs);
                    adapter = new SimpleAdapter(SinglePlaylistAcvtivity.this,songs,R.layout.single_song_list,
                            new String[] {S_NAME}, new int[] {R.id.song_name});
                    list.setAdapter(adapter);


                    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            HashMap<String,String> val = (HashMap<String,String>)list.getItemAtPosition(i);
                            String song_id = val.get(S_ID);
                            String song_name = val.get(S_NAME);
                            String download_url = val.get(S_DOWNLOAD);
                            //Fire download on this song
                            preloader.setMax(100);
                            preloader.setProgress(0);
                            preloader.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            preloader.setTitle("Downloading: "+song_name);
                            preloader.show();
                            new DownloadFile().execute(song_name);
                        }
                    });

                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


    }


    private class DownloadFile extends AsyncTask<String, Integer, String>{

        @Override
        protected String doInBackground(String... url) {
            int count;
            thisUrl = url;
            try {
                //Get user ID
                HashMap<String, String> user = db.getUserDetails();
                URL mp3url = new URL("http://stardust.alexandrulamba.com/youtube2mp3/downloads/"+user.get("uid")+"/"+url[0].replace(' ','_'));
                URLConnection conn = mp3url.openConnection();
                conn.connect();
                // this will be useful so that you can show a tipical 0-100% progress bar
                int lenghtOfFile = conn.getContentLength();
                // downlod the file
                InputStream input = new BufferedInputStream(mp3url.openStream());
                OutputStream output = new FileOutputStream(storageLocation+"/Download/"+url[0].replace(' ','_'));
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    if(url[1] == null || !url[1].equals("true")) {
                        publishProgress((int) (total * 100 / lenghtOfFile));
                    }
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
                removeTask();
                if(thisUrl[1] != null && thisUrl[1].equals("true")){
                    allTasksComplete();
                }
            } catch (Exception e) {}

            dismissProgress();
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values){
            preloader.setProgress(values[0]);
        }

        @Override
        protected void onPreExecute(){
            addTask();
        }


    }

    public void setProgress(Integer progress){
        preloader.setProgress(progress);
    }

    public void dismissProgress(){
        preloader.setProgress(0);
        preloader.dismiss();
    }

    public void addTask(){
        numOfTasks++;
    }

    public void removeTask(){
        numOfTasks--;
    }

    public void allTasksComplete(){

        if(numOfTasks ==0){

            //do what you want to do if all tasks are finished
            Intent intent = new Intent(this, PlayLists.class);
            PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

            // Build notification
            // Actions are just fake
            Notification noti = new Notification.Builder(getApplicationContext())
                    .setContentTitle("Your playlist has been downloaded!")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentText(list_name + " is now on your Downloads folder")
                    .setContentIntent(pIntent).build();
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // hide the notification after its selected
            noti.flags |= Notification.FLAG_AUTO_CANCEL;

            notificationManager.notify(0, noti);

            Log.e("Download tasks completed","true");
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
