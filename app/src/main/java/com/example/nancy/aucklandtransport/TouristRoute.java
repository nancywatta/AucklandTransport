package com.example.nancy.aucklandtransport;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nancy.aucklandtransport.APIs.SurveyAPI;
import com.example.nancy.aucklandtransport.Adapters.CustomSpinnerAdapter;
import com.example.nancy.aucklandtransport.BackgroundTask.NearbyPlacesTask;
import com.example.nancy.aucklandtransport.Parser.DirectionsJSONParser;
import com.example.nancy.aucklandtransport.Utils.Constant;
import com.example.nancy.aucklandtransport.Utils.HTTPConnect;
import com.example.nancy.aucklandtransport.datatype.Place;
import com.example.nancy.aucklandtransport.datatype.Route;
import com.example.nancy.aucklandtransport.datatype.RouteStep;
import com.example.nancy.aucklandtransport.datatype.TouristPlaces;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * TouristRoute class is the activity displaying the route selected by the user
 * in TouristPlanner activity on the Map.
 * The activity also allows a user to search for nearby places of Interest and
 * add or delete in the final list to be visited.
 *
 * Created by Nancy on 9/9/14.
 */
public class TouristRoute extends FragmentActivity implements
        MyAlertDialogWIndow.AlertPositiveListener {

    /*
    Debugging tag for the TouristRoute class
     */
    private static final String TAG = TouristRoute.class.getSimpleName();

    private View mProgressView;
    private View mRouteInfoView;
    EditText etLocation;

    String fromLoc;
    String toLoc;
    String fromCoords = "";
    String toCoords = "";

    // Departure time in seconds since epoch
    long timeSinceEpoch;

    // Arrival time in seconds since epoch
    long arrivalSecEpoch;

    private Route route = null;

    Boolean isRouteSave = false;

    // GoogleMap
    GoogleMap mGoogleMap;

    // Spinner in which the location types are stored
    //Spinner mSprPlaceType;

    // Spinner in which the distance are stored
    Spinner mSprDistance;

    // Spinner in which the location near which the user wants to search are stored
    Spinner mSprLocation;

    // A button to find the near by places
    //Button mBtnFind = null;

    Button mBtnSearch = null;

    // Stores near by places
    Place[] mPlaces = null;

    // A String array containing place types sent to Google Place service
    //String[] mPlaceType = null;

    // A String array containing place types displayed to user
    //String[] mPlaceTypeName = null;

    // A String array containing distnace in meters to be sent to Google Place service
    String[] mDistanceValue = null;

    // A String array containing distance displayed to user
    String[] mDistanceStr = null;

    // A String array containing locations
    String[] mLocationStr = null;

    // The location at which user touches the Google Map
    LatLng mLocation = null;

    // Links marker id and place object
    HashMap<String, Place> mHMReference = new HashMap<String, Place>();

    public static HashMap<String, Place> mAddedReference = new HashMap<String, Place>();

    Bundle savedInstanceState;

    TouristPlaces touristPlaces = new TouristPlaces();

    RouteReceiver mReceiver;
    IntentFilter mFilter;

    CustomSpinnerAdapter customAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tourist_route);

        mRouteInfoView = findViewById(R.id.route_info_form);
        mProgressView = findViewById(R.id.login_progress);
        this.savedInstanceState = savedInstanceState;

        // Instantiating BroadcastReceiver
        mReceiver = new RouteReceiver();

        // Creating an IntentFilter with action
        mFilter = new IntentFilter(Constant.BROADCAST_ACTION);

        // Registering BroadcastReceiver with this activity for the intent filter
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, mFilter);

        Intent intent = getIntent();
        fromLoc = intent.getStringExtra(Constant.FROM_LOCATION);
        try {
            // encoding special characters like space in the user input place
            fromLoc = URLDecoder.decode(fromLoc, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        toLoc = intent.getStringExtra(Constant.TO_LOCATION);
        try {
            // encoding special characters like space in the user input place
            toLoc = URLDecoder.decode(toLoc, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        timeSinceEpoch = intent.getLongExtra(Constant.TIME, 0);
        arrivalSecEpoch = intent.getLongExtra(Constant.ARRIVE_TIME, 0);

        fromCoords = intent.getStringExtra(Constant.FROM_COORDS);
        toCoords = intent.getStringExtra(Constant.TO_COORDS);

        Log.d(TAG, "fromCoords" + fromCoords);
        if (fromCoords != "" && !fromCoords.equals(""))
            History.saveHistory(this, fromLoc, "", fromCoords);
        if (toCoords != "" && !toCoords.equals(""))
            History.saveHistory(this, toLoc, "", toCoords);
        if (fromCoords != "" && !fromCoords.equals("") &&
                toCoords != "" && !toCoords.equals("")) {
            isRouteSave = true;
            History.saveRoute(this, fromLoc, toLoc, fromCoords, toCoords);
        }

        // Getting reference to EditText to get the user input location
        etLocation = (EditText) findViewById(R.id.et_location);

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

        // Delete text on cross button click
        etLocation.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;

                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (etLocation.getRight() - etLocation.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        etLocation.setText("");
                    }
                }
                return false;
            }
        });

        showProgress(true);

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl();

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);

        trackUsageRequest();
    }

    private void trackUsageRequest() {
        SurveyAPI surveyAPI = new SurveyAPI(getApplicationContext());
        surveyAPI.getServerCount();
    }

    private void addPolyLine() {

        drawMarker(route.getStartLocation(), BitmapDescriptorFactory.HUE_VIOLET);

        drawMarker(route.getEndLocation(), BitmapDescriptorFactory.HUE_VIOLET);

        if(touristPlaces.getPlacesArray() != null ) {
            mAddedReference.clear();
            for(Place place: touristPlaces.getPlacesArray()) {
                Marker m = drawMarker(new LatLng(
                        Double.parseDouble(place.mLat),
                        Double.parseDouble(place.mLng)), BitmapDescriptorFactory.HUE_VIOLET);
                mAddedReference.put(m.getId(), place);
            }
        }

        ArrayList<LatLng> points = null;
        PolylineOptions lineOptions = null;

        if(touristPlaces.getRoutesArray() != null &&
                touristPlaces.getRoutesArray().size() > 0 ) {

            for(int index=0; index < touristPlaces.getRoutesArray().size(); index++) {
                Route route1 = null;
                try {
                    route1 = new Route(touristPlaces.getRoutesArray().get(index).getJsonString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    continue;
                }
                // Traversing through all the routes
                for (int i = 0; i < route1.getSteps().size(); i++) {
                    points = new ArrayList<LatLng>();
                    lineOptions = new PolylineOptions();

                    // Fetching i-th route
                    RouteStep path = route1.getSteps().get(i);

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
        }
        else {
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
    }

    private String getDirectionsUrl(){

        String fromAdd = fromLoc;
        String toAdd = toLoc;
        try {
            // encoding special characters like space in the user input place
            fromAdd = URLEncoder.encode(fromLoc, "utf-8");
            toAdd = URLEncoder.encode(toLoc, "utf-8");
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

        String time ="";

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+key +
                "&region=nz" + "&departure_time=" + Long.toString(timeSinceEpoch) +"&"+mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        Log.d(TAG, "url: " + url);

        return url;
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = HTTPConnect.downloadUrl(url[0]);
            }catch(Exception e){
                Log.d(TAG, "Background Task " + e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            RouteParserTask parserTask = new RouteParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /** A class to parse the Google Directions in JSON format */
    private class RouteParserTask extends AsyncTask<String, Integer, ArrayList<Route>>{

        // Parsing the data in non-ui thread
        @Override
        protected ArrayList<Route> doInBackground(String... jsonData) {

            JSONObject jObject;
            ArrayList<Route> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(ArrayList<Route> result) {
            showProgress(false);

            if(result == null || result.size()<1){
                Toast.makeText(getBaseContext(), "No Routes Available", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                route = new Route(result.get(0).getJsonString());
                Log.d(TAG, "route: " + route.getSteps().size());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(route.getArrival() != null && arrivalSecEpoch!=0 &&
                    arrivalSecEpoch < route.getArrival().getSeconds()) {
                Toast.makeText(getBaseContext(), "Please be aware that the arrival time of the Journey" +
                        "selected is greater than the input Arrival Time", Toast.LENGTH_LONG).show();
            }

            if(fromCoords=="" || fromCoords.equals("") || fromCoords == null) {
                Log.d(TAG, "post");
                LatLng temp = ((Route) result.get(0)).getStartLocation();
                fromCoords = temp.latitude + "," + temp.longitude;
                History.saveHistory(TouristRoute.this, fromLoc, "", fromCoords);
            }
            if(toCoords=="" || toCoords.equals("") || toCoords == null) {
                Log.d(TAG, "postto");
                LatLng temp = ((Route) result.get(0)).getEndLocation();
                toCoords = temp.latitude + "," + temp.longitude;
                History.saveHistory(TouristRoute.this, toLoc, "", toCoords);
            }
            if(!isRouteSave)
                History.saveRoute(TouristRoute.this, fromLoc, toLoc, fromCoords, toCoords);

            if (route != null) {
                //mLocation = route.getStartLocation();
                touristPlaces.setArray();
                TouristPlaces.startAddress = route.getStartAddress();
                TouristPlaces.endAddress = route.getEndAddress();
                touristPlaces.setDepartureTime(timeSinceEpoch);
            }

            handleMap();
        }
    }

    public void handleMap() {
    // Array of place types
        //mPlaceType = getResources().getStringArray(R.array.place_type);

        // Array of place type names
        //mPlaceTypeName = getResources().getStringArray(R.array.place_type_name);

        // Array of distance value
        mDistanceValue = getResources().getStringArray(R.array.distance_value);

        // Array of distance names
        mDistanceStr = getResources().getStringArray(R.array.distance_string);

        // Array of distance names
        mLocationStr = getResources().getStringArray(R.array.location_string);

        // Creating an array adapter with an array of Place types
        // to populate the spinner
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(TouristRoute.this,
//                android.R.layout.simple_spinner_dropdown_item,
//                mPlaceTypeName);

        // Getting reference to the Spinner
        //mSprPlaceType = (Spinner) findViewById(R.id.spr_place_type);

        // Setting adapter on Spinner to set place types
        //mSprPlaceType.setAdapter(adapter);

        // Creating an array adapter with an array of Distance string
        // to populate the spinner
        ArrayAdapter<String> disAdapter = new ArrayAdapter<String>(TouristRoute.this,
                android.R.layout.simple_spinner_dropdown_item,
                mDistanceStr);

        // Getting reference to the Spinner
        mSprDistance = (Spinner) findViewById(R.id.spr_distance);

        // Setting adapter on Spinner to set distance
        mSprDistance.setAdapter(disAdapter);

        mSprLocation = (Spinner) findViewById(R.id.spr_location);

        customAdapter = new CustomSpinnerAdapter(getBaseContext(),
                        TouristRoute.this, R.layout.spinner_row, mLocationStr);

        mSprLocation.setAdapter(customAdapter);

        // Getting reference to Find Button
        //mBtnFind = (Button) findViewById(R.id.btn_find);

        // Getting reference to btn_find of the layout activity_main
        mBtnSearch = (Button) findViewById(R.id.btn_search);

        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (status != ConnectionResult.SUCCESS) { // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, TouristRoute.this, requestCode);
            dialog.show();

        } else { // Google Play Services are available

            // Getting reference to the SupportMapFragment
            SupportMapFragment fragment =
                    (SupportMapFragment) getSupportFragmentManager()
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
                        Marker m = drawMarker(point, Constant.UNDEFINED_COLOR);

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
//            mBtnFind.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//
//                    //int selectedPosition = mSprPlaceType.getSelectedItemPosition();
//                    String type = ""; //mPlaceType[selectedPosition];
//
//                    mGoogleMap.clear();
//
//                    if (route != null)
//                        addPolyLine();
//
//                    if (mLocation == null) {
//                        Toast.makeText(TouristRoute.this,
//                                "Please mark a location", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    drawMarker(mLocation, BitmapDescriptorFactory.HUE_GREEN);
//
//                    StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
//                    sb.append("location=" + mLocation.latitude + "," + mLocation.longitude);
//                    sb.append("&radius=500");
//                    sb.append("&types=" + type);
//                    sb.append("&sensor=true");
//                    sb.append("&key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw");
//
//                    // Creating a new non-ui thread task to download Google place json data
//                    NearbyPlacesTask placesTask = new NearbyPlacesTask(mGoogleMap,
//                            mHMReference, mPlaces);
//
//                    // Invokes the "doInBackground()" method of the class PlaceTask
//                    placesTask.execute(sb.toString());
//                }
//            });

            // Defining button click event listener for the find button
            View.OnClickListener findClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Getting user input location
                    String location = etLocation.getText().toString();
                    etLocation.clearFocus();

                    InputMethodManager inputManager = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);

                    if (location != null && !location.equals("")) {

                        String coords = "";
                        int selectedPosition = mSprDistance.getSelectedItemPosition();
                        String radius = mDistanceValue[selectedPosition];

                        selectedPosition = mSprLocation.getSelectedItemPosition();
                        Log.d(TAG, "selectedPosition: " + selectedPosition);

                        if (selectedPosition == 2 &&
                                mLocation == null) {
                            Toast.makeText(TouristRoute.this,
                                    "Please mark a location", Toast.LENGTH_SHORT).show();
                            return;
                        } else if(selectedPosition == 0) {
                            LatLng latLng = route.getStartLocation();
                            coords = latLng.latitude + "," + latLng.longitude;
                        } else if (selectedPosition == 1) {
                            LatLng latLng = route.getEndLocation();
                            coords = latLng.latitude + "," + latLng.longitude;
                        } else
                            coords = mLocation.latitude + "," + mLocation.longitude;

                        mGoogleMap.clear();

                        if (route != null)
                            addPolyLine();

                        StringBuilder sb =
                                new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
                        try {
                            // encoding special characters like space in the user input place
                            location = URLEncoder.encode(location, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        sb.append("&location=" + coords);
                        sb.append("&radius="+radius);
                        sb.append("&keyword=" + location);
                        sb.append("&sensor=true");
                        sb.append("&key=" + getString(R.string.API_KEY));

                        Log.d(TAG, "url: " + sb.toString());

                        // Creating a new non-ui thread task to download Google place json data
                        NearbyPlacesTask placesTask = new NearbyPlacesTask(mGoogleMap,
                                mHMReference, mPlaces);

                        placesTask.setTextSearch();

                        // Invokes the "doInBackground()" method of the class NearbyPlacesTask
                        placesTask.execute(sb.toString());

                    }
                    else {
                        AlertDialog alertDialog = new AlertDialog.Builder(TouristRoute.this).create();
                        alertDialog.setTitle("Validation Error");
                        alertDialog.setMessage("Enter a Search Keyword like attractions, restaurant etc");
                        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            } });
                        alertDialog.show();
                    }
                }
            };

            // Setting button click event listener for the find button
            mBtnSearch.setOnClickListener(findClickListener);

            if (route != null) {
                Log.d(TAG, "not Null");
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

                    // Set spinner item to tap on screen
                    mSprLocation.setSelection(2);

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

                    return markerClick(marker);
                }
            });

            // Setting click event handler for InfoWIndow
            mGoogleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                @Override
                public void onInfoWindowClick(Marker marker) {
                    markerClick(marker);
                }
            });
        }
    }

    private boolean  markerClick(Marker marker) {
        if(mAddedReference != null)
            Log.d(TAG, "mAddedReference: " +  mAddedReference.size());

        // If touched at User input location
        if (!mHMReference.containsKey(marker.getId())
                && !mAddedReference.containsKey(marker.getId()))
            return false;

        Log.d(TAG, "Reached Here");
        // Getting place object corresponding to the currently clicked Marker
        Place place = null;
        if (mHMReference.containsKey(marker.getId()) )
            place = mHMReference.get(marker.getId());
        else
            place = mAddedReference.get(marker.getId());

        // Creating an instance of DisplayMetrics
        DisplayMetrics dm = new DisplayMetrics();

        // Getting the screen display metrics
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        // Creating a dialog fragment to display the photo
        PlaceDialogFragment dialogFragment =
                new PlaceDialogFragment(place, dm, TouristRoute.this);

        dialogFragment.setTouristPlaces(touristPlaces);

//                    dialogFragment.setMarkerRef(marker.getId());

        // Getting a reference to Fragment Manager
        FragmentManager fm = getSupportFragmentManager();

        // Starting Fragment Transaction
        FragmentTransaction ft = fm.beginTransaction();

        // Adding the dialog fragment to the transaction
        ft.add(dialogFragment, "TAG");

        // Committing the fragment transaction
        ft.commit();

        return false;
    }
    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mRouteInfoView.setVisibility(show ? View.GONE : View.VISIBLE);
            mRouteInfoView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRouteInfoView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mRouteInfoView.setVisibility(show ? View.GONE : View.VISIBLE);
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

        if(route!= null && latLng.equals(route.getEndLocation())) {
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.finish_flag));
        } else if(route!= null && latLng.equals(route.getStartLocation())) {
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_flag));
        }
        else if(color != Constant.UNDEFINED_COLOR)
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(color));

        // Placing a marker on the touched position
        Marker m = mGoogleMap.addMarker(markerOptions);

        return m;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tourist_route, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_home) {
            Intent exploreActivity = new Intent(TouristRoute.this, HomePage.class);
            startActivity(exploreActivity);
            return true;
//        } else if(id == R.id.action_map) {
//            Intent mapActivity = new Intent(TouristRoute.this, DisplayMapActivity.class);
//            mapActivity.putExtra(Constant.ORIGIN, true);
//            startActivityForResult(mapActivity, Constant.PICK_ADDRESS_REQUEST);
        } else if ( id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void addAddress(View v) {
        Intent mapActivity = new Intent(TouristRoute.this, DisplayMapActivity.class);
        mapActivity.putExtra(Constant.ORIGIN, true);
        startActivityForResult(mapActivity, Constant.PICK_ADDRESS_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (Constant.PICK_ADDRESS_REQUEST) : {
                if (resultCode == Activity.RESULT_OK) {
                    // Clears all the existing markers
                    mGoogleMap.clear();

                    if (route != null)
                        addPolyLine();

                    String fromAddress = data.getStringExtra(Constant.FROM_ADDRSTR);
                    if(fromAddress!=null) {
                        String fCoords = data.getStringExtra(Constant.FROM_COORDS);
                        int index = fCoords.indexOf(',');
                        String latitude  = "", longitude = "";
                        if(index > 0) {
                            latitude = fCoords.substring(0, index);
                            longitude = fCoords.substring(index+1);
                        }

                        Place place = new Place();
                        place.mPlaceName = fromAddress;
                        place.mLat = latitude;
                        place.mLng = longitude;

                        // Creating a marker
                        MarkerOptions markerOptions = new MarkerOptions();

                        LatLng locLatLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));

                        // Setting the position for the marker
                        markerOptions.position(locLatLng);

                        // Setting the title for the marker
                        markerOptions.title(fromAddress);

                        markerOptions.snippet("Tap here to add location");

                        // Placing a marker on the touched position
                        Marker m = mGoogleMap.addMarker(markerOptions);
                        mHMReference.put(m.getId(), place);
                        m.showInfoWindow();
                        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locLatLng, 15));
                    }
                }
                break;
            }
        }
    }

    // Defining a BroadcastReceiver
    private class RouteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, " RoutesArray: " + touristPlaces.getRoutesArray());

            mGoogleMap.clear();

            addPolyLine();
        }
    }

    public void showAllRoutes(View view) {
        Intent routeInfoActivity = new Intent(TouristRoute.this, TouristRouteInfo.class);
        if(touristPlaces.getRoutesArray() == null || touristPlaces.getRoutesArray().size() < 1)
            routeInfoActivity.putExtra("route", route.getJsonString());
        startActivity(routeInfoActivity);
    }

    @Override
    public void onBackPressed() {

        if( !(touristPlaces.getRoutesArray() == null ||
                touristPlaces.getRoutesArray().size() < 1 )) {
            /** Instantiating the DialogFragment */
            MyAlertDialogWIndow alert = new MyAlertDialogWIndow();

            Bundle args = new Bundle();
            args.putString("message", "Are you sure you want to exit this activity? You will loose the saved places.");
            alert.setArguments(args);

            /** Opening the dialog window */
            alert.show(getSupportFragmentManager(), "Alert_Dialog");
        } else
            super.onBackPressed();
    }

    /** Defining button click listener for the OK button of the alert dialog window */
    @Override
    public void onPositiveClick(boolean isLocationSet) {
        super.onBackPressed();
    }

}
