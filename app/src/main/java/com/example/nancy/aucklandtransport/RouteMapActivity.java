package com.example.nancy.aucklandtransport;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class RouteMapActivity extends FragmentActivity {

    private static final String TAG = RouteMapActivity.class.getSimpleName();
    private GoogleMap googleMap;
    private String routeString;
    Route route = null;
    private boolean isRouteSet = false;
    private IBackgroundServiceAPI api = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_map);

        getRoute();

        Log.i(TAG, "trying to bind service "+BackgroundService.class.getName());
        Intent servIntent = new Intent(BackgroundService.class.getName());
        startService(servIntent);
        Log.i(TAG, "starting service "+servIntent.toString());
        bindService(servIntent, serviceConection, 0);

        SupportMapFragment supportMapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapPath);

        googleMap = supportMapFragment.getMap();
        if (googleMap == null) {
            Toast.makeText(this, "Google Maps not available",
                    Toast.LENGTH_LONG).show();
            return;
        } else {
            googleMap.setMyLocationEnabled(true);
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            googleMap.getUiSettings().setCompassEnabled(true);
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);

            if(route != null) {
                if(!isRouteSet)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(route.getStartLocation(), 15));

                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(route.getStartLocation()));
                marker.showInfoWindow();
                marker = null;
                marker = googleMap.addMarker(new MarkerOptions()
                .position(route.getEndLocation()));
                marker.showInfoWindow();

                Log.d("Inside ", "Polyline");
                ArrayList<LatLng> points = null;
                PolylineOptions lineOptions = null;
                // Traversing through all the routes
                for (int i = 0; i < route.getSteps().size(); i++) {
                    points = new ArrayList<LatLng>();
                    lineOptions = new PolylineOptions();

                    // Fetching i-th route
                    RouteStep path = route.getSteps().get(i);

                    for(int l=0;l< path.getLatlng().size(); l++) {
                        points.add(path.getLatlng().get(l));
                    }

                    if(path.isTransit())
                        lineOptions.color(Color.RED);
                    else
                        lineOptions.color(Color.BLUE);
                    // Adding all the points in the route to LineOptions
                    lineOptions.addAll(points);
                    lineOptions.width(5);
                    googleMap.addPolyline(lineOptions);
                }

            }
        }
    }

    private void getRoute() {
        SharedPreferences settings = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        try {
            routeString = settings.getString("route", "");
            isRouteSet = settings.getBoolean("isRouteSet", false);

            if (!routeString.equals("")) route = new Route(routeString);
            else {
                Intent intent = getIntent();
                routeString = intent.getStringExtra("route");
                route = new Route(routeString);
            }

        } catch ( Exception e ) {
            Log.e("ERROR", "Couldn't get the route from JSONobj");
        }
    }

    private ServiceConnection serviceConection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Service disconnected!");
            api = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            api = IBackgroundServiceAPI.Stub.asInterface(service);

            Log.i(TAG, "Service connected! "+api.toString());
            try {
                api.addListener(serviceListener);
                api.requestLastKnownAddress(0);
            } catch(Exception e) {
                Log.e(TAG, "ERROR!!", e);
            }
        }
    };

    /**
     * Service interaction stuff
     */
    private IBackgroundServiceListener serviceListener = new IBackgroundServiceListener.Stub() {

        public void locationDiscovered(double lat, double lon)
                throws RemoteException {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat, lon)).zoom(15).build();
            if (isRouteSet)
                googleMap.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition));
        }

        public void handleGPSUpdate(double lat, double lon, float angle) throws RemoteException {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat, lon)).zoom(15).build();

            if (isRouteSet)
                googleMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(cameraPosition));
        }

        public void addressDiscovered(String address) throws RemoteException {

        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (api != null) api.removeListener(serviceListener);
            unbindService(serviceConection);
            Log.i(TAG, "unbind ");
        } catch(Exception e) {
            Log.e(TAG, "ERROR!!", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.route_map, menu);
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
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
