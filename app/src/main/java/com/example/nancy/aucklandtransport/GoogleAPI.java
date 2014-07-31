package com.example.nancy.aucklandtransport;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nancy on 7/29/14.
 */
public class GoogleAPI {

    private static final String TAG = "GoogleAPI";

    private static final String main_url = "https://maps.googleapis.com/maps/api/geocode/json?";

    public List<HashMap<String, String>> getReverseGeocode(LatLng data) {
        String latilong = "latlng=" + data.latitude +","+data.longitude;

        String sensor = "sensor=false";

        String key = "key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw";

        // url , from where the geocoding data is fetched
        String url = main_url + latilong + "&" + sensor + "&" + key;
        //Log.i(TAG, "URL: "+url);
        return getGeocodeCoords(queryUrl(url));
    }

    public List<HashMap<String, String>> getGeocodeCoords(String json) {
        List<HashMap<String, String>> places = null;
        GeocodeJSONParser parser = new GeocodeJSONParser();

        try{
            JSONObject jObject = new JSONObject(json);

            /** Getting the parsed data as a an ArrayList */
            places = parser.parse(jObject);

        }catch(Exception e){
            Log.d("Exception",e.toString());
        }
        return places;
    }

    public String queryUrl(String url) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(url);
        HttpResponse response;

        try {
            response = httpclient.execute(httpget);
            Log.i(TAG, "Url:" + url + "");
            Log.i(TAG, "Status:[" + response.getStatusLine().toString() + "]");
            HttpEntity entity = response.getEntity();

            if (entity != null) {

                InputStream instream = entity.getContent();

                BufferedReader r = new BufferedReader(new InputStreamReader(instream));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line);
                }
                instream.close();

                String result = total.toString();
                return result;
            }
        } catch (ClientProtocolException e) {
            Log.e(TAG, "There was a protocol based error", e);
        } catch (Exception e) {
            Log.e(TAG, "There was some error", e);
        }

        return null;
    }
}
