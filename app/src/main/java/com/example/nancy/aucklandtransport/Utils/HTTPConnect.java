package com.example.nancy.aucklandtransport.Utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HTTPConnect class used to define method to download
 * json data from url.
 *
 * Created by Nancy on 10/6/14.
 */
public class HTTPConnect {

    /** A method to download json data from url */
    public static String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            /*
            Creating an http connection to communicate with url
             */
            urlConnection = (HttpURLConnection) url.openConnection();

            /*
            Connecting to url
             */
            urlConnection.connect();

            /*
            Reading data from url
             */
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
}
