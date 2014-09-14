package com.example.nancy.aucklandtransport;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Nancy on 9/9/14.
 */
public class PlacesFragment extends Fragment {

    Context context;

    // GoogleMap
    GoogleMap mGoogleMap;

    // Spinner in which the location types are stored
    Spinner mSprPlaceType;

    // A button to find the near by places
    Button mBtnFind = null;

    // Stores near by places
    Place[] mPlaces = null;

    // A String array containing place types sent to Google Place service
    String[] mPlaceType = null;

    // A String array containing place types displayed to user
    String[] mPlaceTypeName = null;

    // The location at which user touches the Google Map
    LatLng mLocation = null;

    // Links marker id and place object
    HashMap<String, Place> mHMReference = new HashMap<String, Place>();

    // Specifies the drawMarker() to draw the marker with default color
    private static final float UNDEFINED_COLOR = -1;

    private String routeString;
    Route route = null;

    private void getRoute() {
        SharedPreferences settings = context.getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        try {
            routeString = settings.getString("route", "");

            if (!routeString.equals("")) route = new Route(routeString);
            else {
                Intent intent = getActivity().getIntent();
                routeString = intent.getStringExtra("route");
                route = new Route(routeString);
                Log.d("Shared Not Working", ":(");
            }

        } catch (Exception e) {
            Log.e("ERROR", "Couldn't get the route from JSONobj");
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View dataView = inflater.inflate(R.layout.fragment_places,
                container, false);
        context = container.getContext();

        getRoute();

        if (route != null)
            mLocation = route.getStartLocation();

        // Array of place types
        mPlaceType = getResources().getStringArray(R.array.place_type);

        // Array of place type names
        mPlaceTypeName = getResources().getStringArray(R.array.place_type_name);

        // Creating an array adapter with an array of Place types
        // to populate the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_spinner_dropdown_item,
                mPlaceTypeName);

        // Getting reference to the Spinner
        mSprPlaceType = (Spinner) dataView.findViewById(R.id.spr_place_type);

        // Setting adapter on Spinner to set place types
        mSprPlaceType.setAdapter(adapter);

        // Getting reference to Find Button
        mBtnFind = (Button) dataView.findViewById(R.id.btn_find);

        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);

        if (status != ConnectionResult.SUCCESS) { // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, getActivity(), requestCode);
            dialog.show();

        } else { // Google Play Services are available

            // Getting reference to the SupportMapFragment
            SupportMapFragment fragment =
                    (SupportMapFragment) getActivity().getSupportFragmentManager()
                            .findFragmentById(R.id.map);

            // Getting Google Map
            mGoogleMap = fragment.getMap();

            // Enabling MyLocation in Google Map
            mGoogleMap.setMyLocationEnabled(true);

            // Handling screen rotation
            if (savedInstanceState != null) {

                // Removes all the existing links from marker id to place object
                mHMReference.clear();

                //If near by places are already saved
                if (savedInstanceState.containsKey("places")) {

                    // Retrieving the array of place objects
                    mPlaces = (Place[]) savedInstanceState.getParcelableArray("places");

                    // Traversing through each near by place object
                    for (int i = 0; i < mPlaces.length; i++) {

                        // Getting latitude and longitude of the i-th place
                        LatLng point = new LatLng(Double.parseDouble(mPlaces[i].mLat),
                                Double.parseDouble(mPlaces[i].mLng));

                        // Drawing the marker corresponding to the i-th place
                        Marker m = drawMarker(point, UNDEFINED_COLOR);

                        // Linkng i-th place and its marker id
                        mHMReference.put(m.getId(), mPlaces[i]);
                    }
                }

                // If a touched location is already saved
                if (savedInstanceState.containsKey("location")) {

                    // Retrieving the touched location and setting in member variable
                    mLocation = (LatLng) savedInstanceState.getParcelable("location");

                    // Drawing a marker at the touched location
                    drawMarker(mLocation, BitmapDescriptorFactory.HUE_GREEN);
                }
            }

            // Setting click event lister for the find button
            mBtnFind.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    int selectedPosition = mSprPlaceType.getSelectedItemPosition();
                    String type = mPlaceType[selectedPosition];

                    mGoogleMap.clear();

                    if (route != null)
                        addPolyLine();

                    if (mLocation == null) {
                        Toast.makeText(context, "Please mark a location", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    drawMarker(mLocation, BitmapDescriptorFactory.HUE_GREEN);

                    StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
                    sb.append("location=" + mLocation.latitude + "," + mLocation.longitude);
                    sb.append("&radius=500");
                    sb.append("&types=" + type);
                    sb.append("&sensor=true");
                    sb.append("&key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw");

                    // Creating a new non-ui thread task to download Google place json data
                    PlacesTask placesTask = new PlacesTask();

                    // Invokes the "doInBackground()" method of the class PlaceTask
                    placesTask.execute(sb.toString());
                }
            });

            if (route != null) {
                CameraUpdate cameraUpdate =
                        CameraUpdateFactory.newLatLngZoom(route.getStartLocation(), 15);
                mGoogleMap.animateCamera(cameraUpdate);
                addPolyLine();
            }

            // Map Click listener
            mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

                @Override
                public void onMapClick(LatLng point) {

                    // Clears all the existing markers
                    mGoogleMap.clear();

                    if (route != null)
                        addPolyLine();

                    // Setting the touched location in member variable
                    mLocation = point;

                    // Drawing a marker at the touched location
                    drawMarker(mLocation, BitmapDescriptorFactory.HUE_GREEN);
                }
            });

            // Marker click listener
            mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

                @Override
                public boolean onMarkerClick(Marker marker) {

                    // If touched at User input location
                    if (!mHMReference.containsKey(marker.getId()))
                        return false;

                    // Getting place object corresponding to the currently clicked Marker
                    Place place = mHMReference.get(marker.getId());

                    // Creating an instance of DisplayMetrics
                    DisplayMetrics dm = new DisplayMetrics();

                    // Getting the screen display metrics
                    getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);

                    // Creating a dialog fragment to display the photo
                    PlaceDialogFragment dialogFragment = new PlaceDialogFragment(place, dm, context);

                    // Getting a reference to Fragment Manager
                    FragmentManager fm = getActivity().getSupportFragmentManager();

                    // Starting Fragment Transaction
                    FragmentTransaction ft = fm.beginTransaction();

                    // Adding the dialog fragment to the transaction
                    ft.add(dialogFragment, "TAG");

                    // Committing the fragment transaction
                    ft.commit();

                    return false;
                }
            });
        }
        return dataView;
    }

    private void addPolyLine() {

        drawMarker(route.getStartLocation(), BitmapDescriptorFactory.HUE_VIOLET);

        drawMarker(route.getEndLocation(), BitmapDescriptorFactory.HUE_VIOLET);

        Log.d("Inside ", "Polyline");
        ArrayList<LatLng> points = null;
        PolylineOptions lineOptions = null;
        // Traversing through all the routes
        for (int i = 0; i < route.getSteps().size(); i++) {
            points = new ArrayList<LatLng>();
            lineOptions = new PolylineOptions();

            // Fetching i-th route
            RouteStep path = route.getSteps().get(i);

            for (int l = 0; l < path.getLatlng().size(); l++) {
                points.add(path.getLatlng().get(l));
            }

            if (path.isTransit())
                lineOptions.color(Color.RED);
            else
                lineOptions.color(Color.BLUE);
            // Adding all the points in the route to LineOptions
            lineOptions.addAll(points);
            lineOptions.width(5);
            mGoogleMap.addPolyline(lineOptions);
        }
    }

    /**
     * A callback function, executed on screen rotation
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {

        // Saving all the near by places objects
        if (mPlaces != null)
            outState.putParcelableArray("places", mPlaces);

        // Saving the touched location
        if (mLocation != null)
            outState.putParcelable("location", mLocation);

        super.onSaveInstanceState(outState);
    }

    /**
     * A method to download json data from argument url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
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
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception while downloading url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    /**
     * A class, to download Google Places
     */
    private class PlacesTask extends AsyncTask<String, Integer, String> {

        String data = null;

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result) {
            ParserTask parserTask = new ParserTask();

            // Start parsing the Google places in JSON format
            // Invokes the "doInBackground()" method of ParserTask
            parserTask.execute(result);
        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, Place[]> {

        JSONObject jObject;

        // Invoked by execute() method of this object
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

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(Place[] places) {

            mPlaces = places;

            for (int i = 0; i < places.length; i++) {
                Place place = places[i];

                // Getting latitude of the place
                double lat = Double.parseDouble(place.mLat);

                // Getting longitude of the place
                double lng = Double.parseDouble(place.mLng);

                LatLng latLng = new LatLng(lat, lng);

                Marker m = drawMarker(latLng, UNDEFINED_COLOR);

                // Adding place reference to HashMap with marker id as HashMap key
                // to get its reference in infowindow click event listener
                mHMReference.put(m.getId(), place);
            }
        }
    }

    /**
     * Drawing marker at latLng with color
     */
    private Marker drawMarker(LatLng latLng, float color) {
        // Creating a marker
        MarkerOptions markerOptions = new MarkerOptions();

        // Setting the position for the marker
        markerOptions.position(latLng);

        if (color != UNDEFINED_COLOR)
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(color));

        // Placing a marker on the touched position
        Marker m = mGoogleMap.addMarker(markerOptions);

        return m;
    }

}
