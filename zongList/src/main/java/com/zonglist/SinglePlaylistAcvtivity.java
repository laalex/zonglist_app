package com.zonglist;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.Intent;
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
    DatabaseHandler db;
    ProgressDialog preloader;
    ListView list;
    TextView title;
    Button viewList;
    ArrayList<HashMap<String,String>> songs = new ArrayList<HashMap<String,String>>();

    String storageLocation;

    private static final String S_NAME = "song_name";
    private static final String S_ID = "song_id";
    private static final String S_DOWNLOAD = "download_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_playlist_acvtivity);

        Intent intent = getIntent();
        String list_id = intent.getStringExtra("playlist_id");
        String list_name = intent.getStringExtra("list_name");

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Playlist information");
        actionBar.setSubtitle(list_name);
        actionBar.setDisplayHomeAsUpEnabled(true);

        preloader = new ProgressDialog(this);
        preloader.setIndeterminate(false);
        preloader.setMax(100);
        preloader.setProgress(0);
        preloader.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        preloader.setTitle("Downloading your song");

        storageLocation = Environment.getExternalStorageDirectory().toString();

        db = new DatabaseHandler(getApplicationContext());

        new LoadPlaylistAsync().execute(list_id);

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
    public void castDownloadEntirePlaylist(){
        if(list != null){
            //The list is not empty: Get forward and download all the songs
            Log.e("List data",list.toString());
        } else {
            return;
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
                    ListAdapter adapter = new SimpleAdapter(SinglePlaylistAcvtivity.this,songs,R.layout.single_song_list,
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
                Log.e("Storage location:",storageLocation);
                OutputStream output = new FileOutputStream(storageLocation+"/Download/"+url[0].replace(' ','_'));
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    publishProgress((int)(total*100/lenghtOfFile));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
            } catch (Exception e) {}

            dismissProgress();
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values){
            Log.e("Progress changed","inc");
            preloader.setProgress(values[0]);
        }
    }

    public void setProgress(Integer progress){
        preloader.setProgress(progress);
    }

    public void dismissProgress(){
        preloader.dismiss();
    }

}
