package com.example.nancy.aucklandtransport.BackgroundTask;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.SimpleAdapter;

import com.example.nancy.aucklandtransport.History;
import com.example.nancy.aucklandtransport.Parser.PlaceJSONParser;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * GooglePlacesTask returns place predictions in response to an HTTP request.
 * The request specifies a textual search string and optional geographic bounds.
 * The service is used to provide autocomplete functionality for text-based searches,
 * by returning places such as businesses, addresses and points of interest as a user types.
 *
 * Created by Nancy on 10/5/14.
 */
public class GooglePlacesTask extends AsyncTask<String, Void, String> {

    // used to store the textual search string input by user
    String prefix="";

    AutoCompleteTextView textView;
    Context mContext;

    public GooglePlacesTask(Context context, AutoCompleteTextView textView,
            String prefix) {
        this.mContext = context;
        this.textView = textView;
        this.prefix = prefix;
    }

    @Override
    protected String doInBackground(String... place) {
        // For storing data from web service
        String data = "";

        String key = "key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw";

        String input="";

        try {
            input = "input=" + URLEncoder.encode(place[0], "utf-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }

        // place type to be searched
        String types = "types=geocode";

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = input+"&"+types+"&"+sensor+"&"+key+"&"+"components=country:nz";

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/place/autocomplete/"+output+"?"+parameters;

        try{
            // Fetching the data from web service
            data = downloadUrl(url);
        }catch(Exception e){
            Log.d("Background Task", e.toString());
        }
        return data;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        // Creating ParserTask
        ParserTask parserTask = new ParserTask();

        // Starting Parsing the JSON string returned by Web Service
        parserTask.execute(result);
    }

    /** A class to parse the Google Place Autocomplete in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>>{

        JSONObject jObject;

        // Parsing the data in non-ui thread
        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;

            PlaceJSONParser placeJsonParser = new PlaceJSONParser();

            try{
                jObject = new JSONObject(jsonData[0]);

                // Getting the parsed data as a List construct
                places = placeJsonParser.parse(jObject);

            }catch(Exception e){
                Log.d("Exception", e.toString());
            }
            return places;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {

            String[] from = new String[] { "description"};
            int[] to = new int[] { android.R.id.text1 };

            List<HashMap<String, String>> finalResult = new ArrayList<HashMap<String, String>>();

            /**
             * In addition to places returned from Google, also populate
             * the places visited in the past that are stored in Shared Preferences
             * in the autocomplete drop down list.
            */
            ArrayList<String> historyPlaces = History.getHistoryArray();
            for(String place:historyPlaces) {
                if(place.startsWith(prefix)) {
                    HashMap<String, String> hm = new HashMap<String, String>();
                    hm.put("description", place);
                    finalResult.add(hm);
                }
            }
            finalResult.addAll(result);

            // Creating a SimpleAdapter for the AutoCompleteTextView
            SimpleAdapter adapter = new SimpleAdapter(mContext, finalResult, android.R.layout.simple_list_item_1, from, to);

            textView.setAdapter(adapter);
        }
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

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null){
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
}
