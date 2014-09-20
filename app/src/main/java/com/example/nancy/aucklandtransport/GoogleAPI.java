package com.example.nancy.aucklandtransport;

import android.os.AsyncTask;
import android.util.Log;

import com.example.nancy.aucklandtransport.datatype.TravelTime;
import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nancy on 7/29/14.
 */
public class GoogleAPI {

    private static final String TAG = "GoogleAPI";

    private static final String main_url = "https://maps.googleapis.com/maps/api/geocode/json?";

    private LatLng startLoc;
    private LatLng endLoc;

    private TravelTime travelTime;

    private long remainingSeconds;

    private RouteStep routeStep;

    List<HashMap<String, String>> geoPlaces;

    public List<HashMap<String, String>> getReverseGeocode(LatLng data) {
        String latilong = "latlng=" + data.latitude +","+data.longitude;

        String sensor = "sensor=false";

        String key = "key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw";

        // url , from where the geocoding data is fetched
        String url = main_url + latilong + "&" + sensor + "&" + key;
        Log.i(TAG, "URL: "+url);

        GeoCodeTask geoCodeTask = new GeoCodeTask();

        // Start downloading json data from Google Directions API
        geoCodeTask.execute(url);

        return geoPlaces;
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
            //Log.i(TAG, "Status:[" + response.getStatusLine().toString() + "]");
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

    // Fetches data from url passed
    private class GeoCodeTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            GeoParserTask geoParserTask = new GeoParserTask();

            // Invokes the thread for parsing the JSON data
            geoParserTask.execute(result);
        }
    }

    /** A class to parse the Geocoding Places in non-ui thread */
    class GeoParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>>{

        JSONObject jObject;

        // Invoked by execute() method of this object
        @Override
        protected List<HashMap<String,String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;
            GeocodeJSONParser parser = new GeocodeJSONParser();

            try{
                jObject = new JSONObject(jsonData[0]);

                /** Getting the parsed data as a an ArrayList */
                places = parser.parse(jObject);

            }catch(Exception e){
                Log.d("Exception", e.toString());
            }
            return places;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<HashMap<String,String>> list) {
            if(list.size()<1){
                return;
            }

            geoPlaces = list;
        }
    }

    public long getDuration(LatLng startLoc, LatLng endLoc) {

        this.startLoc = startLoc;
        this.endLoc = endLoc;

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl();

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);

        return remainingSeconds;
    }

    public RouteStep getRoute(LatLng startLoc, LatLng endLoc) {

        this.startLoc = startLoc;
        this.endLoc = endLoc;

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl();

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);

        return routeStep;
    }

    private String getDirectionsUrl(){

        // Origin of route
        String str_origin = "origin="+startLoc.latitude+","+startLoc.longitude;

        // Destination of route
        String str_dest = "destination="+endLoc.latitude+","+endLoc.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        String key = "key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw";

        String mode = "mode=walking";

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+key + "&region=nz" + "&"+mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        Log.d("url", url);

        return url;
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, RouteStep>{

        // Parsing the data in non-ui thread
        @Override
        protected RouteStep doInBackground(String... jsonData) {

            JSONObject jObject;
            RouteStep walkRoute = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                walkRoute = parser.parseRouteStep(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return walkRoute;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(RouteStep result) {
            if(result == null){
                return;
            }
            routeStep = result;
        }
    }
}
