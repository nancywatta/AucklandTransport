package com.example.nancy.aucklandtransport;

import android.os.AsyncTask;
import android.util.Log;

import com.example.nancy.aucklandtransport.Parser.DirectionsJSONParser;
import com.example.nancy.aucklandtransport.datatype.Route;
import com.example.nancy.aucklandtransport.datatype.RouteStep;

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
import java.util.Calendar;
import java.util.Locale;

/**
 * BestRoutes class to find new routes available to user in case
 * there is more than 5 minutes for his current Bus to arrive and there are other
 * shorter routes available or routes with minimum transfers.
 *
 * Created by Nancy on 10/12/14.
 */
public class BestRoutes {

    /*
    Debugging tag for the BestRoutes class
     */
    private static final String TAG = BestRoutes.class.getSimpleName();

    // start location of the journey
    String fromLoc;

    // end location of the journey
    String toLoc;

    // Notification message informing users of the new routes
    String message;

    // Index of the BUS route step in the existing RouteStep array
    int busIndex;

    long timeSinceEpoch;

    // current route that the user is following
    Route existingRoute;

    // reference to the RouteEngine class
    RouteEngine routeEngine;
    Calendar c = Calendar.getInstance(Locale.getDefault());

    public BestRoutes(String fromLoc, String toLoc, int busIndex, Route route,
                      String message) {
        this.fromLoc = fromLoc;
        this.toLoc = toLoc;
        this.busIndex = busIndex;
        this.timeSinceEpoch = c.getTimeInMillis()/1000L;
        this.existingRoute = route;
        this.message = message;
    }

    public void setRouteEngine(RouteEngine routeEngine) {
        this.routeEngine = routeEngine;
    }

    public void findBestRoutes() {
        // Getting URL to the Google Directions API
        String url = getDirectionsUrl();

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
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

        Log.d(TAG, "url: " + url);

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
            if(iStream != null)
                iStream.close();
            if(urlConnection != null)
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

    /** A class to parse the Google Directions in JSON format */
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
                return;
            }

            bestRoutes(result);
        }
    }

    private void bestRoutes(ArrayList<Route> result) {
        RouteStep presentRoute = existingRoute.getSteps().get(busIndex);
        Boolean flag = false;
        ArrayList<Route> finalResult = new ArrayList<Route>();

        for(int index=0; index < result.size(); index++) {
            RouteStep routeStep = result.get(index).getSteps().get(0);

            // remove the route from array, if route same as
            // current route is returned from Google.
            if (routeStep.isTransit() &&
                    routeStep.getShortName().compareTo(presentRoute.getShortName()) == 0
                    && routeStep.getDeparture().getTravelTime().compareTo(presentRoute.getDeparture().getTravelTime()) == 0) {
                result.remove(index);
                break;
            }
        }

        long departureTime = c.getTimeInMillis()/1000L;
                //presentRoute.getDeparture().getSeconds();
        long arrivalTime = existingRoute.getArrival().getSeconds();

        long diff = arrivalTime - departureTime;

        int existingTransfers=0;

        // Count the no of transfers from his current routeStep
        for(int index=busIndex; index <existingRoute.getSteps().size(); index++ ) {
            RouteStep routeStep = existingRoute.getSteps().get(index);
            if(routeStep.isTransit())
                existingTransfers++;
        }

        Log.d(TAG, "diff: " + diff + " existingTransfers: " + existingTransfers);

        for (int index=0; index < result.size(); index++) {
            Route route1 = result.get(index);
            long newdiff = route1.getArrival().getSeconds() - departureTime;
            Log.d(TAG, "newDiff: " + newdiff + " getTotalTransfers: " +
                    route1.getTotalTransfers());

            /*
            if new route duration is greater that current route and also
            the no of transfers is greater,
            then do no add in final result.
             */
            if(newdiff > diff &&
                    route1.getTotalTransfers() >= existingTransfers) {
                Log.d(TAG, "Remove");
            } else {
                Log.d(TAG, "Add");
                finalResult.add(route1);
            }
        }

        // alert user about new routes
        if(routeEngine != null && finalResult.size() > 0)
            routeEngine.notifyUser(finalResult, message);

    }
}
