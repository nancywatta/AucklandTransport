package com.example.nancy.aucklandtransport.BackgroundJobs;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.nancy.aucklandtransport.Parser.DirectionsJSONParser;
import com.example.nancy.aucklandtransport.Route;
import com.example.nancy.aucklandtransport.Utils.Constant;
import com.example.nancy.aucklandtransport.Utils.HTTPConnect;
import com.example.nancy.aucklandtransport.datatype.TouristPlaces;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class RouteIntentService extends IntentService {

    private static final String TAG = RouteIntentService.class.getSimpleName();

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_START = "com.example.nancy.aucklandtransport.action.START";
    private static final String ACTION_END = "com.example.nancy.aucklandtransport.action.END";
    private static final String ACTION_DELETE = "com.example.nancy.aucklandtransport.action.DELETE";
    private static final String ACTION_RECALCULATE = "com.example.nancy.aucklandtransport.action.ACTION_RECALCULATE";

    private static final String START_ADD = "com.example.nancy.aucklandtransport.extra.START_ADD";
    private static final String INTERMEDIATE_ADD = "com.example.nancy.aucklandtransport.extra.INTERMEDIATE_ADD";
    private static final String END_ADD = "com.example.nancy.aucklandtransport.extra.END_ADD";
    private static final String DURATION = "com.example.nancy.aucklandtransport.extra.DURATION";

    private static TouristPlaces touristPlaces;

    /**
     * Starts this service to perform action Start with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startAction(Context context, TouristPlaces places,
                                      String param1, String param2) {
        touristPlaces = places;
        Intent intent = new Intent(context, RouteIntentService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(START_ADD, param1);
        intent.putExtra(INTERMEDIATE_ADD, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action End with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void endAction(Context context, String param1, String param2, long duration) {
        Intent intent = new Intent(context, RouteIntentService.class);
        intent.setAction(ACTION_END);
        intent.putExtra(INTERMEDIATE_ADD, param1);
        intent.putExtra(END_ADD, param2);
        intent.putExtra(DURATION, duration);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Delete with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void deleteAction(Context context, TouristPlaces places, String param1,
                                    String param2, String param3) {
        Intent intent = new Intent(context, RouteIntentService.class);
        intent.setAction(ACTION_DELETE);
        intent.putExtra(START_ADD, param1);
        intent.putExtra(INTERMEDIATE_ADD, param2);
        intent.putExtra(END_ADD, param3);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Recalculate with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void recalculateAction(Context context,  String param1,
                                    String param2, long duration) {
        Intent intent = new Intent(context, RouteIntentService.class);
        intent.setAction(ACTION_RECALCULATE);
        intent.putExtra(START_ADD, param1);
        intent.putExtra(END_ADD, param2);
        intent.putExtra(DURATION, duration);
        context.startService(intent);
    }

    public RouteIntentService() {
        super("RouteIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                final String param1 = intent.getStringExtra(START_ADD);
                final String param2 = intent.getStringExtra(INTERMEDIATE_ADD);
                Log.d(TAG, "START: " + param1 + " --> " + param2);
                handleActionStart(param1, param2);
            } else if (ACTION_END.equals(action)) {
                final String param1 = intent.getStringExtra(INTERMEDIATE_ADD);
                final String param2 = intent.getStringExtra(END_ADD);
                final long duration = intent.getLongExtra(DURATION, 0);
                Log.d(TAG, "END: " + param1 + " --> " + param2);
                handleActionEnd(param1, param2, duration);
            } else if (ACTION_DELETE.equals(action)) {
                final String param1 = intent.getStringExtra(START_ADD);
                final String param2 = intent.getStringExtra(INTERMEDIATE_ADD);
                final String param3 = intent.getStringExtra(END_ADD);
                Log.d(TAG, "DELETE: " + param1 + " --> " + param2 + " ---> " + param3);
                handleActionDelete(param1, param2, param3);
            } else if (ACTION_RECALCULATE.equals(action)) {
                final String param1 = intent.getStringExtra(START_ADD);
                final String param2 = intent.getStringExtra(END_ADD);
                final long duration = intent.getLongExtra(DURATION, 0);
                Log.d(TAG, "RECALCULATE: " + param1 + " --> " + param2);
                handleActionRecalculate(param1, param2, duration);
            }
        }
    }

    /**
     * Handle action Start in the provided background thread with the provided
     * parameters.
     */
    private void handleActionStart(String param1, String param2) {
        long duration = touristPlaces.getTimeOfStart();

        if(duration == 0)
            duration = touristPlaces.getDepartureTime();

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(param1, param2,duration);
        // For storing data from web service
        String jsonData = "";
        JSONObject jObject;
        ArrayList<Route> routes = null;

        try {
            // Fetching the data from web service
            jsonData = HTTPConnect.downloadUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            // Parsing the data in non-ui thread
            jObject = new JSONObject(jsonData);
            DirectionsJSONParser parser = new DirectionsJSONParser();

            // Starts parsing data
            routes = parser.parse(jObject);
        }catch(Exception e){
            e.printStackTrace();
        }

        /**
         *  if the param2 is added between param1 and Point A, so
         *  routeArray would contain Route from param1 to Point A then below
         *  function deletePreviousRoute will remove route param1 to Point A.
         *  and function addRoute will add route , param1 to param2.
         */
        touristPlaces.deletePreviousRoute();
        touristPlaces.addRoute(param1,param2,routes, duration);

        // Creating an intent for broadcastreceiver
        Intent broadcastIntent = new Intent(Constant.BROADCAST_ACTION);
        // Sending the broadcast
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }

    private String getDirectionsUrl(String param1, String param2, long secondsSinceEpoch){

        String fromAdd = param1;
        String toAdd = param2;
        try {
            // encoding special characters like space in the user input place
            fromAdd = URLEncoder.encode(fromAdd, "utf-8");
            toAdd = URLEncoder.encode(toAdd, "utf-8");
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

        if(secondsSinceEpoch == 0) {
            Log.d(TAG, "Getting Current Time");
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            secondsSinceEpoch = calendar.getTimeInMillis() / 1000L;
        }

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+key +
                "&region=nz" + "&departure_time=" + Long.toString(secondsSinceEpoch) +"&"+mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        Log.d(TAG, "url:" + url);

        return url;
    }

    /**
     * Handle action End in the provided background thread with the provided
     * parameters.
     */
    private void handleActionEnd(String param1, String param2, long duration) {

        // get Departure time adding the duration the user is going to stay at the current
        // place
        long departureTime = touristPlaces.getDepartureTime(duration);
        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(param1, param2, departureTime);
        String jsonData = "";
        JSONObject jObject;
        ArrayList<Route> routes = null;

        try {
            jsonData = HTTPConnect.downloadUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            jObject = new JSONObject(jsonData);
            DirectionsJSONParser parser = new DirectionsJSONParser();

            // Starts parsing data
            routes = parser.parse(jObject);
        }catch(Exception e){
            e.printStackTrace();
        }
        touristPlaces.addRoute(param1,param2,routes, departureTime);

        // Creating an intent for broadcastreceiver
        Intent broadcastIntent = new Intent(Constant.BROADCAST_ACTION);
        // Sending the broadcast
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }

    /**
     * Handle action Delete in the provided background thread with the provided
     * parameters.
     */
    private void handleActionDelete(String param1, String param2, String param3) {

        long duration = touristPlaces.getTimeOfStart(param2);

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(param1, param3,duration);
        String jsonData = "";
        JSONObject jObject;
        ArrayList<Route> routes = null;

        try {
            jsonData = HTTPConnect.downloadUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            jObject = new JSONObject(jsonData);
            DirectionsJSONParser parser = new DirectionsJSONParser();

            // Starts parsing data
            routes = parser.parse(jObject);
        }catch(Exception e){
            e.printStackTrace();
        }

        touristPlaces.deleteRoute(param1, param3, param2, routes, duration, true);

        // Creating an intent for broadcastreceiver
        Intent broadcastIntent = new Intent(Constant.BROADCAST_ACTION);
        // Sending the broadcast
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }

    /**
     * Handle action recalculate in the provided background thread with the provided
     * parameters.
     */
    private void handleActionRecalculate(String param1, String param2,
                                         long duration) {
        long departureTime = touristPlaces.getTimeOfDepart(param1, duration);

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(param1, param2,departureTime);
        String jsonData = "";
        JSONObject jObject;
        ArrayList<Route> routes = null;

        try {
            jsonData = HTTPConnect.downloadUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            jObject = new JSONObject(jsonData);
            DirectionsJSONParser parser = new DirectionsJSONParser();

            // Starts parsing data
            routes = parser.parse(jObject);
        }catch(Exception e){
            e.printStackTrace();
        }

        touristPlaces.deleteRoute(param1, param2, param1, routes, departureTime, false);

        // Creating an intent for broadcastreceiver
        Intent broadcastIntent = new Intent(Constant.BROADCAST_ACTION);
        // Sending the broadcast
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }
}
