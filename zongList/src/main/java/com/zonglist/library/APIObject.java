package com.zonglist.library;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.zonglist.R;
import com.zonglist.library.*;

import android.content.Context;
import android.util.Log;

public class APIObject{
    private final String API_HOST = "http://10.42.0.1/api/";
    Context context;
    private JSONParser jsonParser;
    DatabaseHandler db;
    HashMap<String, String> user;

    public APIObject(){
        jsonParser = new JSONParser();
    }

    public JSONObject getPlaylists(Context context){
        String endpoint = API_HOST+"get_playlists";
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        db = new DatabaseHandler(context);
        HashMap<String, String> user = db.getUserDetails();
        params.add(new BasicNameValuePair("user", user.get("email")));
        params.add(new BasicNameValuePair("pass", user.get("pass")));
        JSONObject json = jsonParser.getJSONFromUrl(endpoint, params);
        return json;
    }

    public JSONObject getPlaylist(Context context, String list_id){
        String endpoint = API_HOST+"get_playlist_songs/"+list_id;
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        db = new DatabaseHandler(context);
        HashMap<String, String> user = db.getUserDetails();
        params.add(new BasicNameValuePair("user", user.get("email")));
        params.add(new BasicNameValuePair("pass", user.get("pass")));
        JSONObject json = jsonParser.getJSONFromUrl(endpoint, params);
        return json;
    }

}