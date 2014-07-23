package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

public class RoutesActivity extends Activity {
    String fromLoc;
    String toLoc;
    String fromCoords = "";
    String toCoords = "";
    Boolean isDeparture;
    long timeSinceEpoch;
    ListView list;
    RoutesAdaptar adapter;
    private ArrayList<Route> routes = null;
    TextView origin;
    TextView destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routes);

        Intent intent = getIntent();
        fromLoc = intent.getStringExtra(MainApp.FROM_LOCATION);
        toLoc = intent.getStringExtra(MainApp.TO_LOCATION);
        isDeparture = intent.getBooleanExtra(MainApp.ISDEPARTURE, true);
        timeSinceEpoch = intent.getLongExtra(MainApp.TIME, 0);
        fromCoords = intent.getStringExtra(MainApp.FROM_COORDS);
        toCoords = intent.getStringExtra(MainApp.TO_COORDS);

        if(fromCoords != "" && !fromCoords.equals(""))
            History.saveHistory(this, fromLoc, "", fromCoords);
        if(toCoords != "" && !toCoords.equals(""))
            History.saveHistory(this, toLoc, "", toCoords);

        list=(ListView)findViewById(R.id.list);
        origin = (TextView)findViewById(R.id.textView1);
        destination = (TextView)findViewById(R.id.textView2);
        origin.setText("From : " + fromLoc);
        destination.setText("To :   " + toLoc);

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl();

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);

        // Click event for single list row
        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                SharedPreferences settings = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("route", routes.get(position).getJsonString());
                editor.commit();

                Intent myIntent = new Intent(view.getContext(), RouteInfoScreen.class);
                myIntent.putExtra("from", fromLoc);
                myIntent.putExtra("to", toLoc);
                startActivity(myIntent);

            }
        });
    }

    private String getDirectionsUrl(){

        try {
            // encoding special characters like space in the user input place
            fromLoc = URLEncoder.encode(fromLoc, "utf-8");
            toLoc = URLEncoder.encode(toLoc, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        // Origin of route
        String str_origin = "origin="+fromLoc;

        // Destination of route
        String str_dest = "destination="+toLoc;

        // Sensor enabled
        String sensor = "sensor=false";

        String key = "key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw";

        String mode = "mode=transit";

        String time ="";
        String parameters = "";
        // Building the parameters to the web service
        if(isDeparture == true)
            parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+key + "&departure_time=" + Long.toString(timeSinceEpoch) +"&"+mode
                    + "&alternatives=true";
        else
            parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+key + "&arrival_time=" + Long.toString(timeSinceEpoch) +"&"+mode
        + "&alternatives=true";

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
    private class ParserTask extends AsyncTask<String, Integer, ArrayList<Route>>{

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
            if(result.size()<1){
                Toast.makeText(getBaseContext(), "No Routes Available", Toast.LENGTH_SHORT).show();
                return;
            }

            routes = result;
            if(fromCoords!="" && !fromCoords.equals("")) {
                fromCoords = ((Route) result.get(0)).getStartLocation().toString();
                History.saveHistory(RoutesActivity.this, fromLoc, "", fromCoords);
            }
            if(toCoords!="" && !toCoords.equals("")) {
                toCoords = ((Route) result.get(0)).getEndLocation().toString();
                History.saveHistory(RoutesActivity.this, toLoc, "", toCoords);
            }

            // Getting adapter by passing xml data ArrayList
            adapter=new RoutesAdaptar(RoutesActivity.this, result);
            list.setAdapter(adapter);

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.routes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
