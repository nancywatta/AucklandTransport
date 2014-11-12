package com.example.nancy.aucklandtransport.BackgroundTask;

import android.os.AsyncTask;
import android.util.Log;

import com.example.nancy.aucklandtransport.Parser.PlaceJSONParser;
import com.example.nancy.aucklandtransport.Utils.Constant;
import com.example.nancy.aucklandtransport.datatype.Place;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/**
 * NearbyPlacesTask lets you search for places within a specified area.
 * You can refine your search request by supplying keywords or specifying the
 * type of place you are searching for.
 *
 * Created by Nancy on 10/6/14.
 */
public class NearbyPlacesTask extends AsyncTask<String, Integer, String> {
    String data = null;

    /*
    GoogleMap
     */
    GoogleMap mGoogleMap;

    private boolean isTextSearch = false;

    /*
    Links marker id and place object
     */
    HashMap<String, Place> mHMReference;

    /*
    Stores near by places
     */
    Place[] mPlaces;

    public void setTextSearch() { isTextSearch = true; }

    public NearbyPlacesTask(GoogleMap googleMap,
                            HashMap<String, Place> hmReference, Place[] places) {
        this.mGoogleMap = googleMap;
        this.mHMReference = hmReference;
        this.mPlaces = places;
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
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

    /**
     * Invoked by execute() method of this object
     */
    @Override
    protected String doInBackground(String... url) {
        try {
            data = downloadUrl(url[0]);
        } catch (Exception e) {
            Log.d("Background Task", e.toString());
        }
        return data;
    }

    /**
     * Executed after the complete execution of doInBackground() method
     */
    @Override
    protected void onPostExecute(String result) {
        ParserTask parserTask = new ParserTask();

        /*
        Start parsing the Google places in JSON format
        Invokes the "doInBackground()" method of ParserTask
         */
        parserTask.execute(result);
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, Place[]> {

        JSONObject jObject;

        /*
        Invoked by execute() method of this object
         */
        @Override
        protected Place[] doInBackground(String... jsonData) {

            Place[] places = null;
            PlaceJSONParser placeJsonParser = new PlaceJSONParser();

            try {
                jObject = new JSONObject(jsonData[0]);
                /** Getting the parsed data as a List construct */
                places = placeJsonParser.placeParse(jObject);

            } catch (Exception e) {
                Log.d("Exception", e.toString());
            }
            return places;
        }

        /*
        Executed after the complete execution of doInBackground() method
         */
        @Override
        protected void onPostExecute(Place[] places) {

            mPlaces = places;
            if(places == null)
                return;

            for (int i = 0; i < places.length; i++) {
                Place place = places[i];

                /*
                Getting latitude of the place
                 */
                double lat = Double.parseDouble(place.mLat);

                /*
                Getting longitude of the place
                 */
                double lng = Double.parseDouble(place.mLng);

                LatLng latLng = new LatLng(lat, lng);

                if(i==0 && isTextSearch)
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                Marker m = drawMarker(latLng, Constant.UNDEFINED_COLOR);

                /*
                Adding place reference to HashMap with marker id as HashMap key
                to get its reference in infowindow click event listener
                 */
                mHMReference.put(m.getId(), place);
            }

        }
    }

    /**
     * Drawing marker at latLng with color
     */
    private Marker drawMarker(LatLng latLng, float color) {
        /*
        Creating a marker
         */
        MarkerOptions markerOptions = new MarkerOptions();

        /*
        Setting the position for the marker
         */
        markerOptions.position(latLng);

        /*
        Defining a standard icon
         */
        if (color != Constant.UNDEFINED_COLOR)
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(color));

        /*
        Placing a marker on the touched position
         */
        Marker m = mGoogleMap.addMarker(markerOptions);

        return m;
    }
}
