package com.example.nancy.aucklandtransport.BackgroundTask;

import android.os.AsyncTask;
import android.util.Log;

import com.example.nancy.aucklandtransport.Parser.DirectionsJSONParser;
import com.example.nancy.aucklandtransport.Route;
import com.example.nancy.aucklandtransport.Utils.HTTPConnect;
import com.example.nancy.aucklandtransport.datatype.TouristPlaces;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * GoogleDirectionTask to get directions between locations
 * using an HTTP request.
 *
 * Created by Nancy on 10/17/14.
 */
public class GoogleDirectionTask extends AsyncTask<String, Void, String> {

    TouristPlaces touristPlaces = new TouristPlaces();

    // Downloading data in non-ui thread
    @Override
    protected String doInBackground(String... url) {

        // For storing data from web service
        String data = "";

        try{
            // Fetching the data from web service
            data = HTTPConnect.downloadUrl(url[0]);
        }catch(Exception e){
            Log.d("Background Task", e.toString());
        }
        return data;
    }

    // Executes in UI thread, after the execution of
    // doInBackground()
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        RouteParserTask parserTask = new RouteParserTask();

        // Invokes the thread for parsing the JSON data
        parserTask.execute(result);
    }

    /** A class to parse the Google Directions in JSON format */
    private class RouteParserTask extends AsyncTask<String, Integer, ArrayList<Route>>{

        // Parsing the data in non-ui thread
        @Override
        protected ArrayList<Route> doInBackground(String... jsonData) {

            JSONObject jObject;
            ArrayList<Route> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(ArrayList<Route> result) {
            if(result == null || result.size() < 1)
                return;
        }
    }
}
