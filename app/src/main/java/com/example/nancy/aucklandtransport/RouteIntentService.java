package com.example.nancy.aucklandtransport;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.nancy.aucklandtransport.Parser.DirectionsJSONParser;
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

    private static final String START_ADD = "com.example.nancy.aucklandtransport.extra.START_ADD";
    private static final String INTERMEDIATE_ADD = "com.example.nancy.aucklandtransport.extra.INTERMEDIATE_ADD";
    private static final String END_ADD = "com.example.nancy.aucklandtransport.extra.END_ADD";

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
    public static void endAction(Context context, String param1, String param2) {
        Intent intent = new Intent(context, RouteIntentService.class);
        intent.setAction(ACTION_END);
        intent.putExtra(INTERMEDIATE_ADD, param1);
        intent.putExtra(END_ADD, param2);
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
                Log.d(TAG, "END: " + param1 + " --> " + param2);
                handleActionEnd(param1, param2);
            }
        }
    }

    /**
     * Handle action Start in the provided background thread with the provided
     * parameters.
     */
    private void handleActionStart(String param1, String param2) {
        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(param1, param2);
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

        touristPlaces.deletePreviousRoute();
        touristPlaces.addRoute(routes);

        // Creating an intent for broadcastreceiver
        Intent broadcastIntent = new Intent(Constant.BROADCAST_ACTION);
        // Sending the broadcast
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }

    private String getDirectionsUrl(String param1, String param2){

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

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        long secondsSinceEpoch = calendar.getTimeInMillis() / 1000L;

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
    private void handleActionEnd(String param1, String param2) {
        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(param1, param2);
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
        touristPlaces.addRoute(routes);

        // Creating an intent for broadcastreceiver
        Intent broadcastIntent = new Intent(Constant.BROADCAST_ACTION);
        // Sending the broadcast
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }
}
