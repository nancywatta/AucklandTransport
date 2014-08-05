package com.example.nancy.aucklandtransport;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nancy.aucklandtransport.MyAlertDialogWIndow.AlertPositiveListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

public class DisplayMapActivity extends FragmentActivity implements AlertPositiveListener{

    private GoogleMap googleMap;
    MarkerOptions markerOptions;
    LatLng latLng;
    GPSTracker gps;
    EditText etLocation;
    Boolean isOrigin;
    Intent output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        output = getIntent();
        setContentView(R.layout.activity_display_map);

        SupportMapFragment supportMapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        Intent intent = getIntent();
        isOrigin = intent.getBooleanExtra(MainApp.ORIGIN, true);
        String message = intent.getStringExtra(MainApp.ADDRSTR);

        // Getting reference to EditText to get the user input location
        etLocation = (EditText) findViewById(R.id.et_location);

        etLocation.setText(message);
        etLocation.setSelection(etLocation.getText().length());

        etLocation.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                etLocation.setSelectAllOnFocus(true);
            }
        });

        etLocation.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etLocation.getWindowToken(),
                            InputMethodManager.RESULT_UNCHANGED_SHOWN);
                    return true;
                }
                return false;
            }
        });

        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        // Showing status
        if(status!= ConnectionResult.SUCCESS){ // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();

        }else { // Google Play Services are available

            // Getting a reference to the map
            googleMap = supportMapFragment.getMap();
            if (googleMap == null) {
                Toast.makeText(this, "Google Maps not available",
                        Toast.LENGTH_LONG).show();
            } else {
                googleMap.setMyLocationEnabled(true);
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                googleMap.getUiSettings().setCompassEnabled(true);
                googleMap.getUiSettings().setZoomControlsEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                googleMap.setIndoorEnabled(false);
            }

            if (message != null && !message.equals("")) {
                startDownload(message);
            } else {
                gps = new GPSTracker(getBaseContext());
                latLng = new LatLng(gps.getLatitude(), gps.getLongitude());

                startReverseGeo(latLng);
            }

            // Getting reference to btn_find of the layout activity_main
            Button btn_find = (Button) findViewById(R.id.btn_find);

            // Defining button click event listener for the find button
            OnClickListener findClickListener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Getting user input location
                    String location = etLocation.getText().toString();
                    etLocation.clearFocus();

                    if (location != null && !location.equals("")) {
                        startDownload(location);
                    }
                }
            };

            // Setting button click event listener for the find button
            btn_find.setOnClickListener(findClickListener);

            if (googleMap != null) {
                // Setting a click event handler for the map
                googleMap.setOnMapClickListener(new OnMapClickListener() {

                    @Override
                    public void onMapClick(LatLng latLng) {

                        startReverseGeo(latLng);
                    }
                });
            }

            if (googleMap != null) {
                // Setting click event handler for InfoWIndow
                googleMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

                    @Override
                    public void onInfoWindowClick(Marker marker) {

                        FragmentManager fm = getFragmentManager();

                        /** Instantiating the DialogFragment */
                        MyAlertDialogWIndow alert = new MyAlertDialogWIndow();

                        /** Opening the dialog window */
                        alert.show(fm, "Alert_Dialog");
                    }
                });
            }
        }
    }

    /** Defining button click listener for the OK button of the alert dialog window */
    @Override
    public void onPositiveClick(boolean isLocationSet) {
        if(isOrigin == true)
            output.putExtra(MainApp.FROM_ADDRSTR, etLocation.getText().toString());
        else
            output.putExtra(MainApp.TO_ADDRSTR, etLocation.getText().toString());
        setResult(RESULT_OK, output);
        finish();
    }

    private void startDownload(String message) {
        String url = "https://maps.googleapis.com/maps/api/geocode/json?";

        try {
            // encoding special characters like space in the user input place
            message = URLEncoder.encode(message, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String address = "address=" + message;

        String sensor = "sensor=false";

        // url , from where the geocoding data is fetched
        url = url + address + "&" + sensor + "&" + "region=nz" + "&" + "key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw";

        // Instantiating DownloadTask to get places from Google Geocoding service
        // in a non-ui thread
        DownloadTask downloadTask = new DownloadTask();

        // Start downloading the geocoding places
        downloadTask.execute(url);
    }

    private void startReverseGeo(LatLng latLng) {
    // Creating a marker
        markerOptions = new MarkerOptions();

        // Setting the position for the marker
        markerOptions.position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
        markerOptions.snippet("Tap here to set location");

        // Clears the previously touched position
        googleMap.clear();

        // Animating to the touched position
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        // Placing a marker on the touched position
        googleMap.addMarker(markerOptions);

        String url = "https://maps.googleapis.com/maps/api/geocode/json?";

        String latilong = "latlng=" + latLng.latitude +","+latLng.longitude;

        String sensor = "sensor=false";

        String key = "key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw";

        // url , from where the geocoding data is fetched
        url = url + latilong + "&" + sensor + "&" + key + "&" + "region=nz";

        // Instantiating DownloadTask to get places from Google Geocoding service
        // in a non-ui thread
        DownloadTask downloadTask = new DownloadTask();

        // Start downloading the geocoding places
        downloadTask.execute(url);
    }

    private String downloadUrl(String strUrl) throws IOException{
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

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null){
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

    /** A class, to download Places from Geocoding webservice */
    private class DownloadTask extends AsyncTask<String, Integer, String>{

        String data = null;

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try{
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result){

            // Instantiating ParserTask which parses the json data from Geocoding webservice
            // in a non-ui thread
            ParserTask parserTask = new ParserTask();

            // Start parsing the places in JSON format
            // Invokes the "doInBackground()" method of the class ParseTask
            parserTask.execute(result);
        }
    }

    /** A class to parse the Geocoding Places in non-ui thread */
    class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>>{

        JSONObject jObject;

        // Invoked by execute() method of this object
        @Override
        protected List<HashMap<String,String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;
            GeocodeJSONParser parser = new GeocodeJSONParser();

            try{
                jObject = new JSONObject(jsonData[0]);

                /** Getting the parsed data as a an ArrayList */
                places = parser.parse(jObject);

            }catch(Exception e){
                Log.d("Exception",e.toString());
            }
            return places;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(List<HashMap<String,String>> list){

            // Clears all the existing markers
            googleMap.clear();

            for(int i=0;i<list.size();i++){



                // Getting a place from the places list
                HashMap<String, String> hmPlace = list.get(i);

                // Getting latitude of the place
                double lat = Double.parseDouble(hmPlace.get("lat"));

                // Getting longitude of the place
                double lng = Double.parseDouble(hmPlace.get("lng"));

                // Getting name
                String name = hmPlace.get("formatted_address");

                // Locate the first location
                if(i==0) {
                    // Creating a marker
                    markerOptions = new MarkerOptions();
                    latLng = new LatLng(lat, lng);

                    // Setting the position for the marker
                    markerOptions.position(latLng);

                    // Setting the title for the marker
                    markerOptions.title(name);

                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
                    markerOptions.snippet("Tap here to set location");

                    // Placing a marker on the touched position
                    Marker marker = googleMap.addMarker(markerOptions);
                    marker.showInfoWindow();
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    etLocation.setText(name);
                    String temp = latLng.latitude + "," + latLng.longitude;
                    if(isOrigin == true)
                        output.putExtra(MainApp.FROM_COORDS, temp);
                    else
                        output.putExtra(MainApp.TO_COORDS, temp);
                    setResult(RESULT_OK, output);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.display_map, menu);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(false); // disable the button
            actionBar.setDisplayHomeAsUpEnabled(false); // remove the left caret
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        /*int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }*/
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
