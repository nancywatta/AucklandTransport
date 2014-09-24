package com.example.nancy.aucklandtransport;

import android.content.Context;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/**
 * Created by Nancy on 9/20/14.
 */
public class AucklandPublicTransportAPI {
    private static final String TAG = AucklandPublicTransportAPI.class.getSimpleName();

    private static String main_url="";
    private Context mContext;
    //private SharedPreferences sharedPrefs;

    public AucklandPublicTransportAPI(Context context) {
        this.mContext = context;
//        sharedPrefs = mContext.getSharedPreferences(Constants.SHARED_PREFERENCE_NAME,
//                Context.MODE_PRIVATE);
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

    public void startServerTracking(LatLng busStop, String busNumber) {

        String location = "lat=" + busStop.latitude + "&lng=" + busStop.longitude;

        String routeName = "route=" + busNumber + "&tripType=0";

        String serviceName = Context.TELEPHONY_SERVICE;
        TelephonyManager m_telephonyManager = (TelephonyManager) mContext.getSystemService(serviceName);
        String userName = "username=" + m_telephonyManager.getDeviceId();

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

    public void stopServerTracking(LatLng busStop, String busNumber) {
        String location = "lat=" + busStop.latitude + "&lng=" + busStop.longitude;

        String routeName = "route=" + busNumber + "&tripType=0";

        String serviceName = Context.TELEPHONY_SERVICE;
        TelephonyManager m_telephonyManager = (TelephonyManager) mContext.getSystemService(serviceName);
        String userName = "username=" + m_telephonyManager.getDeviceId();

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

}