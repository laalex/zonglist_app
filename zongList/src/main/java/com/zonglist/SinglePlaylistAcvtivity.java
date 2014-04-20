package com.zonglist;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
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
import java.io.File;
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
    private static final String S_STATUS = "is_downloaded";
    private static final String S_VID = "video_id";

    private final String API_HOST = "http://beta.zonglist.com/api/";
    private final String HOST = "http://beta.zonglist.com/";

    public ProgressDialog progress1,progress2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_playlist_acvtivity);

        if(ICBootstrap(this)) {
            progress1 = new ProgressDialog(this);
            progress2 = new ProgressDialog(this);
            progress1.setCancelable(false);progress2.setCancelable(false);
            progress1.setIndeterminate(true);progress2.setIndeterminate(false);
            Intent intent = getIntent();
            list_id = intent.getStringExtra("playlist_id");
            list_name = intent.getStringExtra("list_name");

            android.support.v7.app.ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle(list_name);
            actionBar.setDisplayHomeAsUpEnabled(true);

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
            progress1.setMax(0);
            progress1.setProgress(0);
            progress1.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress1.setTitle("Downloading playlist: " + list_name);
            progress1.setMessage("The songs from this playlist are being downloaded in background.");
            progress1.show();
            for(int i = 0; i<count;i++){
                HashMap<String,String> song = ((HashMap<String,String>)adapter.getItem(i));
                //Song represents the details of the current song
                new DownloadFile().execute(song.get("download_url"),"true",song.get("song_name"));
            }
            //Delay for a few seconds
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    progress1.dismiss();
                    Toast.makeText(getApplicationContext(), "Your data is downloaded in background.",
                            Toast.LENGTH_LONG).show();
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
                    s_name = s_name.replace("-"+j.getString(S_VID)+".mp3","");
                    String s_id = j.getString(S_ID);
                    String s_down = j.getString(S_DOWNLOAD);
                    String s_isdown = j.getString(S_STATUS);
                    //Check if s_isdown == 0 or 1 and build the required message
                    if(s_isdown.equals("0")){
                        s_isdown = "Inactive";
                    } else {
                        s_isdown = "Active";
                    }
                    HashMap<String,String> map = new HashMap<String, String>();
                    map.put(S_NAME,s_name);
                    map.put(S_ID,s_id);
                    map.put(S_DOWNLOAD,s_down);
                    map.put(S_STATUS,s_isdown);
                    songs.add(map);
                    list = (ListView) findViewById(R.id.playlist_songs);
                    adapter = new SimpleAdapter(SinglePlaylistAcvtivity.this,songs,R.layout.single_song_list,
                            new String[] {S_NAME,S_STATUS}, new int[] {R.id.song_name,R.id.song_status});
                    list.setAdapter(adapter);

                    /** Set click listener for the buttons in the view */
                    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
                            final int pos_i = i;
                            final long pos_l = l;

                            final HashMap<String,String> val = (HashMap<String,String>)list.getItemAtPosition(pos_i);

                            final TextView song_download_action = (TextView) view.findViewById(R.id.song_download_action);
                            song_download_action.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    String song_name = val.get(S_NAME);
                                    String download_url = val.get(S_DOWNLOAD);
                                    progress2.setMax(100);
                                    progress2.setProgress(0);
                                    progress2.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                    progress2.setTitle("Downloading: " + song_name);
                                    progress2.show();
                                    new DownloadFile().execute(download_url);
                                }
                            });

                            final TextView youtube_view = (TextView) view.findViewById(R.id.youtube_view);
                            youtube_view.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    String video_id = val.get(S_VID);
                                    try{
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + video_id));
                                        startActivity(intent);
                                    }catch (ActivityNotFoundException ex){
                                        Intent intent=new Intent(Intent.ACTION_VIEW,
                                                Uri.parse("http://www.youtube.com/watch?v="+video_id));
                                        startActivity(intent);
                                    }
                                }
                            });

                            final TextView song_status = (TextView) view.findViewById(R.id.song_status);
                            song_status.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    String status = val.get(S_STATUS);
                                    String msg;
                                    Log.d("STATUS",status);
                                    if(status.equals("Inactive")){ msg = "The song cache has expired. The download will take a little bit longer"; }
                                    else { msg = "The song can be downloaded immediately!"; }
                                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                                }
                            });

                            /*Fire download on this song

                            */
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
                URL mp3url = new URL(HOST+"download/hash/"+url[0]+"/"+user.get("uid")+"/MOBILE_ALLOWED_FROM");
                URLConnection conn = mp3url.openConnection();
                conn.connect();
                // this will be useful so that you can show a tipical 0-100% progress bar
                int lenghtOfFile = conn.getContentLength();
                // downlod the file
                InputStream input = new BufferedInputStream(mp3url.openStream());
                String songName = "";
                if(!url[2].equals(null)){
                    //Then we have the song name
                    songName = url[2]+"[www.zonglist.com].mp3";
                } else {
                    songName = url[0]+"[www.zonglist.com].mp3";
                }
                //Check if ZongList directory exists
                if(!new File(storageLocation+"/ZongList").exists()){
                    new File(storageLocation+"/ZongList").mkdir();
                }
                OutputStream output = new FileOutputStream(storageLocation+"/ZongList/"+songName);
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress((int) (total * 100 / lenghtOfFile));

                    output.write(data, 0, count);
                }
                progress2.dismiss();
                output.flush();
                output.close();
                input.close();
                removeTask();
                if(thisUrl[1] != null && thisUrl[1].equals("true")){
                    allTasksComplete();
                }
            } catch (Exception e) {}


            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values){
            progress2.setProgress(values[0]);
        }

        @Override
        protected void onPreExecute(){
            addTask();
        }


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
