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

/**
 * Created by Nancy on 9/20/14.
 */
public class AucklandPublicTransportAPI {
    private static final String TAG = AucklandPublicTransportAPI.class.getSimpleName();

    private static String main_url="";
    private Context mContext;

    public AucklandPublicTransportAPI(Context context) {
        this.mContext = context;
        main_url = "http://" + mContext.getResources().getString(R.string.IP_ADDRESS) + ":8080/apt-server/";
    }

    //http://localhost:8080/apt-server/ScheduleJob?lat=-36.861798&lng=174.74301&route=030&tripType=0&username=96ecf6226e93493e8c6c31e72e48114b

    public void startServerTracking(LatLng busStop, String busNumber) {

        String location = "lat=" + busStop.latitude + "&lng=" + busStop.longitude;

        String routeName = "route=" + busNumber + "&tripType=0";

        String serviceName = Context.TELEPHONY_SERVICE;
        TelephonyManager m_telephonyManager = (TelephonyManager) mContext.getSystemService(serviceName);
        String IMEI;
        IMEI = m_telephonyManager.getDeviceId();

        // TODO remove hardcoding
        //String url = main_url + "ScheduleJob?" + location + "&" + routeName + "&username=" + IMEI;
        String url = main_url + "ScheduleJob?" + "lat=-36.861798&lng=174.74301&route=030&tripType=0&username=9f72abdd7ddd4bca814529e3695d7c9c";

        Log.d(TAG, "url : " +  url);

        ScheduleTask scheduleTask = new ScheduleTask();

        // Start downloading json data from Google Directions API
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
