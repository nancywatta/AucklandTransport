package com.example.nancy.aucklandtransport;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nancy.aucklandtransport.Parser.DirectionsJSONParser;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;

public class AlternateRoute extends Activity {

    private static final String TAG = AlternateRoute.class.getSimpleName();
    private View mProgressView;
    private View mRouteInfoView;

    String fromLoc;
    String toLoc;
    String fromCoords = "";
    String toCoords = "";
    long timeSinceEpoch;
    ListView list;
    RoutesAdaptar adapter;
    private ArrayList<Route> routes = null;
    TextView origin;
    TextView destination;
    Boolean isRouteSave = false;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alternate_route);

        mRouteInfoView = findViewById(R.id.route_info_form);
        mProgressView = findViewById(R.id.login_progress);

        intent = getIntent();
        fromLoc = intent.getStringExtra(MainApp.FROM_LOCATION);
        try {
            // encoding special characters like space in the user input place
            fromLoc = URLDecoder.decode(fromLoc, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        toLoc = intent.getStringExtra(MainApp.TO_LOCATION);
        try {
            // encoding special characters like space in the user input place
            toLoc = URLDecoder.decode(toLoc, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        timeSinceEpoch = intent.getLongExtra(MainApp.TIME, 0);
        fromCoords = intent.getStringExtra(MainApp.FROM_COORDS);
        toCoords = intent.getStringExtra(MainApp.TO_COORDS);

        Log.d(TAG, "fromCoords" + fromCoords);
        if(fromCoords != "" && !fromCoords.equals(""))
            History.saveHistory(this, fromLoc, "", fromCoords);
        if(toCoords != "" && !toCoords.equals(""))
            History.saveHistory(this, toLoc, "", toCoords);
        if(fromCoords != "" && !fromCoords.equals("") &&
                toCoords != "" && !toCoords.equals("")) {
            isRouteSave = true;
            History.saveRoute(this, fromLoc, toLoc, fromCoords, toCoords);
        }

        list=(ListView)findViewById(R.id.list);
        origin = (TextView)findViewById(R.id.textView1);
        destination = (TextView)findViewById(R.id.textView2);
        origin.setText("From : " + fromLoc);
        destination.setText("To :   " + toLoc);

        showProgress(true);

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl();

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);

        // Click event for single list row
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

            }
        });
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mRouteInfoView.setVisibility(show ? View.GONE : View.VISIBLE);
            mRouteInfoView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRouteInfoView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mRouteInfoView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private String getDirectionsUrl(){

        String fromAdd = fromLoc;
        String toAdd = toLoc;
        try {
            // encoding special characters like space in the user input place
            fromAdd = URLEncoder.encode(fromLoc, "utf-8");
            toAdd = URLEncoder.encode(toLoc, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        // Origin of route
        String str_origin = "origin="+fromAdd;

        // Destination of route
        String str_dest = "destination="+toAdd;

        // Sensor enabled
        String sensor = "sensor=false";

        String key = "key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw";

        String mode = "mode=transit";

        String time ="";
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+key
                + "&region=nz" + "&departure_time=" + Long.toString(timeSinceEpoch) +"&"+mode
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
            showProgress(false);

            if(result.size()<1){
                Toast.makeText(getBaseContext(), "No Routes Available", Toast.LENGTH_SHORT).show();
                return;
            }

            routes = result;
            if(fromCoords=="" || fromCoords.equals("") || fromCoords == null) {
                Log.d(TAG, "post");
                LatLng temp = ((Route) result.get(0)).getStartLocation();
                fromCoords = temp.latitude + "," + temp.longitude;
                History.saveHistory(AlternateRoute.this, fromLoc, "", fromCoords);
            }
            if(toCoords=="" || toCoords.equals("") || toCoords == null) {
                Log.d(TAG, "postto");
                LatLng temp = ((Route) result.get(0)).getEndLocation();
                toCoords = temp.latitude + "," + temp.longitude;
                History.saveHistory(AlternateRoute.this, toLoc, "", toCoords);
            }
            if(!isRouteSave)
                History.saveRoute(AlternateRoute.this, fromLoc, toLoc, fromCoords, toCoords);

            addExistingRoute(result);

            // Getting adapter by passing xml data ArrayList
            adapter=new RoutesAdaptar(AlternateRoute.this, result);
            list.setAdapter(adapter);
        }
    }

    private void addExistingRoute(ArrayList<Route> result) {
        SharedPreferences settings = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        Route route = null;
        try {
            String routeString = settings.getString("route", "");

            if (!routeString.equals(""))
                route = new Route(routeString);

        } catch ( Exception e ) {
            Log.e("ERROR", "Couldn't get the route from JSONobj");
            e.printStackTrace();
            return;
        }

        if(route == null) return;

        int busIndex = intent.getIntExtra("BUS_INDEX",-1);
        RouteStep presentRoute = route.getSteps().get(busIndex);
        Boolean flag = false;

        for(Route route1 : result) {
            RouteStep routeStep = route1.getSteps().get(0);
            if (routeStep.isTransit() &&
                    routeStep.getShortName().compareTo(presentRoute.getShortName()) == 0
                    && routeStep.getDeparture().getTravelTime().compareTo(presentRoute.getDeparture().getTravelTime()) == 0) {
                flag = true;
                break;
            }
        }

        if(!flag) {
            for(int index = 0; index < busIndex; index++)
                route.getSteps().remove(index);

            result.add(route);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.alternate_route, menu);
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
        return super.onOptionsItemSelected(item);
    }
}
