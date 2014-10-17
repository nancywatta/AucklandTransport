package com.example.nancy.aucklandtransport;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.nancy.aucklandtransport.Parser.RealTimeJSONParser;
import com.google.android.gms.maps.model.LatLng;

import org.androidpn.client.Constants;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

/**
 * AucklandPublicTransportAPI to communicate with our private
 * application server to get real time data for Bus and also get
 * Push Notifications from Server.
 *
 * Created by Nancy on 9/20/14.
 */
public class AucklandPublicTransportAPI {
    private static final String TAG = AucklandPublicTransportAPI.class.getSimpleName();

    private static String main_url="";
    private Context mContext;
    private TextView realTimeText;
    private View mProgressView;
    private RoutesAdaptar routesAdaptar;
    private RouteInfoAdapter routeInfoAdapter;
    private long depTime;
    private RouteEngine routeEngine = null;
    private SharedPreferences sharedPrefs;
    private Route route;
    private int routeStep;
    private TextView textView;

    public void setRouteEngine(RouteEngine routeEngine) {
        this.routeEngine = routeEngine;
    }

    public void setRoute(Route route, int routeStep) {
        this.route = route;
        this.routeStep = routeStep;
    }

    public void setTextView(TextView textView) { this.textView = textView;}

    public AucklandPublicTransportAPI(Context context, TextView realTimeText,
                                      View mProgressView, RoutesAdaptar routesAdaptar) {
        this.mContext = context;
        this.realTimeText = realTimeText;
        this.mProgressView = mProgressView;
        this.routesAdaptar = routesAdaptar;
        sharedPrefs = mContext.getSharedPreferences(Constants.SHARED_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        main_url = "http://" + mContext.getResources().getString(R.string.IP_ADDRESS) + ":8080/apt-server/";
    }

    public AucklandPublicTransportAPI(Context context, TextView realTimeText,
                                      View mProgressView, RouteInfoAdapter routeInfoAdapter) {
        this.mContext = context;
        this.realTimeText = realTimeText;
        this.mProgressView = mProgressView;
        this.routeInfoAdapter = routeInfoAdapter;
        sharedPrefs = mContext.getSharedPreferences(Constants.SHARED_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        main_url = "http://" + mContext.getResources().getString(R.string.IP_ADDRESS) + ":8080/apt-server/";
    }

    public AucklandPublicTransportAPI(Context context) {
        this.mContext = context;
        sharedPrefs = mContext.getSharedPreferences(Constants.SHARED_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        main_url = "http://" + mContext.getResources().getString(R.string.IP_ADDRESS) + ":8080/apt-server/";
    }

//    private boolean isRegistered() {
//        return sharedPrefs.contains(Constants.XMPP_USERNAME)
//                && sharedPrefs.contains(Constants.XMPP_PASSWORD);
//    }

    private String newRandomUUID() {
        String uuidRaw = UUID.randomUUID().toString();
        return uuidRaw.replaceAll("-", "");
    }

    //http://localhost:8080/apt-server/ScheduleJob?lat=-36.861798&lng=174.74301&route=030&tripType=0&username=96ecf6226e93493e8c6c31e72e48114b

    // Allow server to start tracking user to give notifications about Bus Arrival
    public void startServerTracking(LatLng busStop, String busNumber) {

        String location = "lat=" + busStop.latitude + "&lng=" + busStop.longitude;

        String routeName = "route=" + busNumber + "&tripType=0";

        String serviceName = Context.TELEPHONY_SERVICE;
        TelephonyManager m_telephonyManager = (TelephonyManager) mContext.getSystemService(serviceName);
        final String userName = "username=" + m_telephonyManager.getDeviceId();

        //String userName = "username=" + sharedPrefs.getString(Constants.XMPP_USERNAME, "");

//        if (!isRegistered()) {
//            final String newUsername = newRandomUUID();
//            final String newPassword = newRandomUUID();
//            SharedPreferences.Editor editor = sharedPrefs.edit();
//            editor.putString(Constants.XMPP_USERNAME,
//                    newUsername);
//            editor.putString(Constants.XMPP_PASSWORD,
//                    newPassword);
//            editor.commit();
//            userName = "username=" + newUsername;
//        }
//        else
//            userName = "username=" + sharedPrefs.getString(Constants.XMPP_USERNAME, "");

        // TODO remove hardcoding
        //String url = main_url + "ScheduleJob?" + location + "&" + routeName + "&" + userName;
        String url = main_url + "ScheduleJob?" +
                "lat=-36.861798&lng=174.74301&route=030&tripType=0&" + userName;

        Log.d(TAG, "url : " +  url);

        ScheduleTask scheduleTask = new ScheduleTask();

        scheduleTask.execute(url);
    }

    // Allow server to stop tracking user
    public void stopServerTracking(LatLng busStop, String busNumber) {
        String location = "lat=" + busStop.latitude + "&lng=" + busStop.longitude;

        String routeName = "route=" + busNumber + "&tripType=0";

        String serviceName = Context.TELEPHONY_SERVICE;
        TelephonyManager m_telephonyManager = (TelephonyManager) mContext.getSystemService(serviceName);
        final String userName = "username=" + m_telephonyManager.getDeviceId();

        //String userName = "username=" + sharedPrefs.getString(Constants.XMPP_USERNAME, "");

        // TODO remove hardcoding
        //String url = main_url + "DeleteJob?" + location + "&" + routeName + "&" + userName;
        String url = main_url + "DeleteJob?" +
                "lat=-36.861798&lng=174.74301&route=030&tripType=0&" + userName;

        Log.d(TAG, "url : " +  url);

        ScheduleTask scheduleTask = new ScheduleTask();

        scheduleTask.execute(url);
    }

    private class ScheduleTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
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

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d(TAG, e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    //http://172.23.208.76:8080/apt-server/showDueTime.do?lat=-36.861798&lng=174.74301&route=030
    // get real time bus arrival data from application server
    public void getRealTimeDate(LatLng busStop, String busNumber, long depTime) {
        this.depTime = depTime;
        String location = "lat=" + busStop.latitude + "&lng=" + busStop.longitude;

        String routeName = "route=" + busNumber;

        String url = main_url + "showDueTime.do?" + location + "&" + routeName;

        Log.d(TAG, "url : " +  url);

        RealTimeTask realTimeTask = new RealTimeTask();

        realTimeTask.execute(url);
    }

    private class RealTimeTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            RealTimeParserTask parserTask = new RealTimeParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /** A class to parse the RealTime data in non-ui thread */
    class RealTimeParserTask extends AsyncTask<String, Integer, HashMap<String, Date>>{

        JSONObject jObject;

        // Invoked by execute() method of this object
        @Override
        protected HashMap<String, Date> doInBackground(String... jsonData) {

            HashMap<String, Date> dates = null;
            RealTimeJSONParser parser = new RealTimeJSONParser();

            try{
                jObject = new JSONObject(jsonData[0]);

                /** Getting the parsed data as a HashMap */
                dates = parser.parse(jObject);

            }catch(Exception e){
                Log.d("Exception", e.toString());
            }
            return dates;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(HashMap<String, Date> dates) {
            if(routesAdaptar != null)
                routesAdaptar.showProgress(false, mProgressView, realTimeText );
            if(routeInfoAdapter!=null)
                routeInfoAdapter.showProgress(false, mProgressView, realTimeText );

            if(dates !=null) {
                Date actualArrivalTime = dates.get("ActualArrivalTime");
                Date expectedArrivalTime = dates.get("ExpectedArrivalTime");

                if (expectedArrivalTime != null && actualArrivalTime != null) {

                    Date depDate = new Date();
                    depDate.setTime(depTime * 1000L);

                    if (depDate.compareTo(actualArrivalTime) == 0) {
                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm");
                        String shortTimeStr = sdf.format(expectedArrivalTime);
                        if (realTimeText != null) {
                            realTimeText.setText(shortTimeStr);

                            // strike the scheduled time of Bus
                            if(textView != null)
                                textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        }
                        if (routeEngine != null) {
                            // check if new routes available
                            checkNewRoute(expectedArrivalTime);
                        }
                        return;
                    }
                }
            }

//            if(routeEngine != null) {
//                SimpleDateFormat toFullDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                try {
//                    Date fullDate = toFullDate.parse("2014-10-10 22:40:00");
//                    Log.d(TAG, "fullDate: " + fullDate.getTime());
//                    routeEngine.setActualArrivalTime(fullDate);
//                } catch (ParseException e) {
//                    e.printStackTrace();
//                }
//            }

            if(realTimeText!=null)
                realTimeText.setText(Html.fromHtml("<small><small><small>" +
                        "Not Available" + "</small></small></small>"));
        }
    }

    /**
     * check for new routes available to user in case
     * there is more than 5 minutes delay for his current Bus to arrive and there are other
     * shorter routes available.
     *
     * @param arrivalTime real time of bus
     */
    private void checkNewRoute(Date arrivalTime) {
        routeEngine.setActualArrivalTime(arrivalTime);
        RouteStep nextRoute = route.getSteps().get(routeStep);

        Log.d(TAG, "arrivalTime: " + arrivalTime);

        if (arrivalTime != null) {
            Calendar c = Calendar.getInstance();
            long diff = (arrivalTime.getTime() - c.getTimeInMillis()) / 1000L;

            //TODO remove hard coding
            diff = 301;

            if ((diff > 300) || diff < 0) {
                String message = "";
                if(diff > 300)
                    message = "Your " +
                        mContext.getResources().getString(nextRoute.getTransportName())
                        + " " + nextRoute.getShortName()
                        + " is arriving in next " + Math.round(diff / 60) + " minute.";
                else
                message = "Your " +
                        mContext.getResources().getString(nextRoute.getTransportName())
                        + " " + nextRoute.getShortName()
                        + " has left.";

                BestRoutes bestRoutes = new BestRoutes(nextRoute.getDepartureStop(),
                        route.getEndAddress(), routeStep, route, message);
                bestRoutes.setRouteEngine(routeEngine);
                bestRoutes.findBestRoutes();
            }
        }
    }

}
