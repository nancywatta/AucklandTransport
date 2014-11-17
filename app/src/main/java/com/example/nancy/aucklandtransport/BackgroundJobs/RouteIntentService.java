package com.example.nancy.aucklandtransport.BackgroundJobs;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.nancy.aucklandtransport.Parser.DirectionsJSONParser;
import com.example.nancy.aucklandtransport.datatype.Route;
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
 * a service on a separate handler thread. This class receives all the direction request
 * everytime a tourist adds or deletes a place of interest when building up his itinerary.
 * <p>
 * helper methods.
 *
 * Created by Nancy on 10/16/14.
 */
public class RouteIntentService extends IntentService {

    private static final String TAG = RouteIntentService.class.getSimpleName();

    /*
    Defining all the ACTIONs that the Intent Service will perform
     */
    private static final String ACTION_START = "com.example.nancy.aucklandtransport.action.START";
    private static final String ACTION_END = "com.example.nancy.aucklandtransport.action.END";
    private static final String ACTION_DELETE = "com.example.nancy.aucklandtransport.action.DELETE";
    private static final String ACTION_RECALCULATE = "com.example.nancy.aucklandtransport.action.ACTION_RECALCULATE";

    /*
    Defining all parameters required for responding to users Direction request.
     */
    private static final String START_ADD = "com.example.nancy.aucklandtransport.extra.START_ADD";
    private static final String START_COORDS = "com.example.nancy.aucklandtransport.extra.START_COORDS";
    private static final String INTERMEDIATE_ADD = "com.example.nancy.aucklandtransport.extra.INTERMEDIATE_ADD";
    private static final String INTERMEDIATE_COORDS = "com.example.nancy.aucklandtransport.extra.INTERMEDIATE_COORDS";
    private static final String END_ADD = "com.example.nancy.aucklandtransport.extra.END_ADD";
    private static final String END_COORDS = "com.example.nancy.aucklandtransport.extra.END_COORDS";
    private static final String DURATION = "com.example.nancy.aucklandtransport.extra.DURATION";

    private static TouristPlaces touristPlaces;

    /**
     * Starts this service to perform action Start with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startAction(Context context, TouristPlaces places,
                                      String param1, String param2,
                                      String param3, String param4) {
        touristPlaces = places;
        Intent intent = new Intent(context, RouteIntentService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(START_COORDS, param1);
        intent.putExtra(START_ADD, param2);
        intent.putExtra(INTERMEDIATE_COORDS, param3);
        intent.putExtra(INTERMEDIATE_ADD, param4);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action End with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void endAction(Context context, String param1, String param2,
                                 String param3, String param4, long duration) {
        Intent intent = new Intent(context, RouteIntentService.class);
        intent.setAction(ACTION_END);
        intent.putExtra(INTERMEDIATE_COORDS, param1);
        intent.putExtra(INTERMEDIATE_ADD, param2);
        intent.putExtra(END_COORDS, param3);
        intent.putExtra(END_ADD, param4);
        intent.putExtra(DURATION, duration);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Delete with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void deleteAction(Context context, TouristPlaces places,
                                    String param1, String param2,
                                    String param3,
                                    String param4, String param5) {
        Intent intent = new Intent(context, RouteIntentService.class);
        intent.setAction(ACTION_DELETE);
        intent.putExtra(START_COORDS, param1);
        intent.putExtra(START_ADD, param2);
        intent.putExtra(INTERMEDIATE_ADD, param3);
        intent.putExtra(END_COORDS, param4);
        intent.putExtra(END_ADD, param5);
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
                final String param1 = intent.getStringExtra(START_COORDS);
                final String param2 = intent.getStringExtra(START_ADD);
                final String param3 = intent.getStringExtra(INTERMEDIATE_COORDS);
                final String param4 = intent.getStringExtra(INTERMEDIATE_ADD);
                Log.d(TAG, "START: " + param1 + " --> " + param3);
                handleActionStart(param1, param2, param3, param4);
            } else if (ACTION_END.equals(action)) {
                final String param1 = intent.getStringExtra(INTERMEDIATE_COORDS);
                final String param2 = intent.getStringExtra(INTERMEDIATE_ADD);
                final String param3 = intent.getStringExtra(END_COORDS);
                final String param4 = intent.getStringExtra(END_ADD);
                final long duration = intent.getLongExtra(DURATION, 0);
                Log.d(TAG, "END: " + param1 + " --> " + param3);
                handleActionEnd(param1, param2, param3, param4, duration);
            } else if (ACTION_DELETE.equals(action)) {
                final String param1 = intent.getStringExtra(START_COORDS);
                final String param2 = intent.getStringExtra(START_ADD);
                final String param3 = intent.getStringExtra(INTERMEDIATE_ADD);
                final String param4 = intent.getStringExtra(END_COORDS);
                final String param5 = intent.getStringExtra(END_ADD);
                Log.d(TAG, "DELETE: " + param1 + " --> " + param3 + " ---> " + param4);
                handleActionDelete(param1, param2, param3, param4, param5);
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
    private void handleActionStart(String startCoords, String startAdd,
                                   String intCoords, String intAdd) {
        /*
         get the departure time of the journey.
          */
        long duration = touristPlaces.getTimeOfStart();

        /*
        if the departure time returned is zero, it means it is the first route
        and thus take the departure time as the time that was input by user in the
        Tourist Planner activity.
         */
        if(duration == 0)
            duration = touristPlaces.getDepartureTime();

        /*
         Getting URL to the Google Directions API
          */
        String url = getDirectionsUrl(startCoords, intCoords,duration);
        /*
         For storing data from web service
          */
        String jsonData = "";
        JSONObject jObject;
        ArrayList<Route> routes = null;

        try {
            /*
             Fetching the data from web service
              */
            jsonData = HTTPConnect.downloadUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            /*
             Parsing the data in non-ui thread
              */
            jObject = new JSONObject(jsonData);
            DirectionsJSONParser parser = new DirectionsJSONParser();

            /*
             Starts parsing data
              */
            routes = parser.parse(jObject);
        }catch(Exception e){
            e.printStackTrace();
        }

        if(routes != null && routes.size() > 0) {
            /**
             *  When adding a new destination point, delete the last route
             *  from the routes array. This is because consider an example
             *  where the existing routes array contains below two routes
             *  Start -> A, A -> End
             *  Thus when adding a new place B in between A and End,
             *  route A -> End should be deleted and instead
             *  the routes array should now contain below three routes.
             *  Start -> A, A -> B, B -> End
             */
            touristPlaces.deletePreviousRoute();

        /*
        Add the new route fetched into the routes array
         */
            touristPlaces.addRoute(startCoords, startAdd,
                    intCoords, intAdd, routes, duration);

        /*
         Creating an intent for broadcastreceiver
          */
            Intent broadcastIntent = new Intent(Constant.BROADCAST_ACTION);
        /*
         Sending the broadcast
          */
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
        }

    }

    private String getDirectionsUrl(String param1, String param2, long secondsSinceEpoch){

        String fromAdd = param1;
        String toAdd = param2;
        try {
            /*
             encoding special characters like space in the user input place
              */
            fromAdd = URLEncoder.encode(fromAdd, "utf-8");
            toAdd = URLEncoder.encode(toAdd, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        /*
         Origin of route
          */
        String str_origin = "origin="+fromAdd;

        /*
         Destination of route
          */
        String str_dest = "destination="+toAdd;

        /*
         Sensor enabled
          */
        String sensor = "sensor=false";

        String key = "key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw";

        String mode = "mode=transit";

        if(secondsSinceEpoch == 0) {
            Log.d(TAG, "Getting Current Time");
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            secondsSinceEpoch = calendar.getTimeInMillis() / 1000L;
        }

        /*
         Building the parameters to the web service
          */
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+key +
                "&region=nz" + "&departure_time=" + Long.toString(secondsSinceEpoch) +"&"+mode;

        /*
         Output format
          */
        String output = "json";

        /*
         Building the url to the web service
          */
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        Log.d(TAG, "url:" + url);

        return url;
    }

    /**
     * Handle action End in the provided background thread with the provided
     * parameters.
     */
    private void handleActionEnd(String intCoords, String intAdd,
                                 String endCoords, String endAdd, long duration) {

        /**
         * get Departure time adding the duration the user is going to stay at the current
         * place
         */
        long departureTime = touristPlaces.getDepartureTime(duration);

        /*
         Getting URL to the Google Directions API
          */
        String url = getDirectionsUrl(intCoords, endCoords, departureTime);

        /*
         For storing data from web service
          */
        String jsonData = "";
        JSONObject jObject;
        ArrayList<Route> routes = null;

        try {
            /*
             Fetching the data from web service
              */
            jsonData = HTTPConnect.downloadUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            /*
             Parsing the data in non-ui thread
              */
            jObject = new JSONObject(jsonData);
            DirectionsJSONParser parser = new DirectionsJSONParser();

            /*
             Starts parsing data
              */
            routes = parser.parse(jObject);
        }catch(Exception e){
            e.printStackTrace();
        }

        if(routes != null && routes.size() > 0) {
        /*
        Add the new route fetched into the routes array
         */
            touristPlaces.addRoute(intCoords, intAdd,
                    endCoords, endAdd, routes, departureTime);

        /*
         Creating an intent for broadcastreceiver
          */
            Intent broadcastIntent = new Intent(Constant.BROADCAST_ACTION);
        /*
         Sending the broadcast
          */
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
        }
    }

    /**
     * Handle action Delete in the provided background thread with the provided
     * parameters.
     */
    private void handleActionDelete(String startCoords, String startAdd,
                                    String inCoords, String endCoords, String endAdd) {

        /*
         get the departure time of the journey.
          */
        long duration = touristPlaces.getTimeOfStart(inCoords);

        /*
         Getting URL to the Google Directions API
          */
        String url = getDirectionsUrl(startCoords, endCoords,duration);

        /*
         For storing data from web service
          */
        String jsonData = "";
        JSONObject jObject;
        ArrayList<Route> routes = null;

        try {
            /*
             Fetching the data from web service
              */
            jsonData = HTTPConnect.downloadUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            /*
             Parsing the data in non-ui thread
              */
            jObject = new JSONObject(jsonData);
            DirectionsJSONParser parser = new DirectionsJSONParser();

            /*
             Starts parsing data
              */
            routes = parser.parse(jObject);
        }catch(Exception e){
            e.printStackTrace();
        }

        /**
         * Delete the routes with inCoords as the start and end address
         * And add the fetched route in place of the two routes deleted.
         *  Consider an example where the existing routes array contains
         *  below three routes
         *  Start -> A, A -> B, B -> End
         *  Thus when deleting place B from the tourist itinerary,
         *  routes A -> B and B -> End should be deleted and instead
         *  add the route A -> End. So the routes array should now contain
         *  below two routes.
         *  Start -> A, A -> End
         */
        touristPlaces.deleteRoute(startCoords, startAdd,
                endCoords, endAdd, inCoords, routes, duration, true);

        /*
         Creating an intent for broadcastreceiver
          */
        Intent broadcastIntent = new Intent(Constant.BROADCAST_ACTION);
        /*
         Sending the broadcast
          */
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }

    /**
     * Handle action recalculate in the provided background thread with the provided
     * parameters.
     */
    private void handleActionRecalculate(String param1, String param2,
                                         long duration) {
        /*
         get the departure time of the journey adding the duration the user is
         going to stay at the current place
          */
        long departureTime = touristPlaces.getTimeOfDepart(param1, duration);

        /*
        Getting URL to the Google Directions API
         */
        String url = getDirectionsUrl(param1, param2,departureTime);

        /*
         For storing data from web service
          */
        String jsonData = "";
        JSONObject jObject;
        ArrayList<Route> routes = null;

        try {
            /*
             Fetching the data from web service
              */
            jsonData = HTTPConnect.downloadUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try{
            /*
             Parsing the data in non-ui thread
              */
            jObject = new JSONObject(jsonData);
            DirectionsJSONParser parser = new DirectionsJSONParser();

            /*
            Starts parsing data
             */
            routes = parser.parse(jObject);
        }catch(Exception e){
            e.printStackTrace();
        }

        /**
         * Delete the existing route from param1 location to param2 location
         * Instead add the fetched route with new departure time
         * from param1 location to param2 location
         */
        touristPlaces.deleteRoute(param1, "", param2, "",
                param1, routes, departureTime, false);

        /*
        Creating an intent for broadcastreceiver
         */
        Intent broadcastIntent = new Intent(Constant.BROADCAST_ACTION);
        /*
        Sending the broadcast
         */
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

    }
}
