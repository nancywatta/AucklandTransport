package com.example.nancy.aucklandtransport;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;

public class RouteInfoFragment extends Fragment implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        ServiceConnection{

    private static final String TAG = RouteInfoFragment.class.getSimpleName();
    Context context;

    private String routeString;
    Route route = null;

    ListView listView;
    private Button mapBtn=null;
    private Button navigationBtn=null;

    private boolean isRouteSet = false;
    private Boolean routeStarted = false;
    private ActivityRecognitionClient arclient;
    private PendingIntent pIntent;
    private Boolean mInProgress;
    // Constants that define the activity detection interval
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DETECTION_INTERVAL_SECONDS = 20;
    public static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;

    private IBackgroundServiceAPI api = null;
    private boolean isGPSon = false;

    public boolean getRouteSet() { return isRouteSet;}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View dataView = inflater.inflate(R.layout.fragment_route_info,
                container, false);

        context = container.getContext();
        isRouteSet = false;

        mapBtn=(Button)dataView.findViewById(R.id.mapButton);
        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                ShowMap(v);
            }
        });

        navigationBtn=(Button)dataView.findViewById(R.id.startNavigation);
        navigationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                StartTracking(v);
            }
        });

        getRoute();

        listView = (ListView)dataView.findViewById(R.id.RouteInfoScreenListView);
        if(route!=null)
            listView.setAdapter(new RouteInfoAdapter(getActivity(), route));

        // Click event for single list row
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent myIntent = new Intent(view.getContext(), PathElevation.class);
                myIntent.putExtra("IS_TRANSIT", route.getSteps().get(position).isTransit());
                myIntent.putExtra("PathJSON", route.getSteps().get(position).getJsonString());
                startActivity(myIntent);
            }
        });

        return dataView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getActivity().getApplicationContext()
                .bindService(new Intent(getActivity(),
                                BackgroundService.class), this,
                        Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder binder) {
        api=(IBackgroundServiceAPI)binder;
        Log.i(TAG, "Service connected! "+api.toString());
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        disconnect();
    }

    private void disconnect() {
        Log.i(TAG, "Service disconnected!");
        api=null;
    }

    private void getRoute() {
        SharedPreferences settings = context.getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        try {
            routeString = settings.getString("route", "");
            routeStarted = settings.getBoolean("routeStarted", false);
            isRouteSet = settings.getBoolean("isRouteSet", false);

            if (routeStarted) isRouteSet = routeStarted;
            if (!routeString.equals("")) route = new Route(routeString);
            else {
                Intent intent = getActivity().getIntent();
                routeString = intent.getStringExtra("route");
                route = new Route(routeString);
                Log.d("Shared Not Working", ":(");
            }

        } catch ( Exception e ) {
            Log.e("ERROR", "Couldn't get the route from JSONobj");
            e.printStackTrace();
        }
    }

    public void ShowMap(View v){
        //android:onClick="ShowMap"
        try {
            Intent myIntent = new Intent(getActivity(), RouteMapActivity.class);
            myIntent.putExtra("route", routeString);
            startActivity(myIntent);
        }catch (Exception e) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            getActivity().getApplicationContext().unbindService(this);
            disconnect();

            //unbindService(serviceConnection);
            Log.i(TAG, "unbind ");
        } catch(Exception e) {
            Log.e(TAG, "ERROR!!", e);
        }
        if(arclient!=null){
            arclient.removeActivityUpdates(pIntent);
            arclient.disconnect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getRoute();
    }

    private boolean setRoute() {
        try {
            if (api != null) {
                api.setRoute(routeString);
                SharedPreferences settings =
                        context.getSharedPreferences(getString(R.string.PREFS_NAME), 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("route", routeString);
                editor.putBoolean("isRouteSet", true);
                editor.commit();

                isGPSon = api.isGPSOn();
                if (!isGPSon) {
                    showSettingsAlert();
                    return false;
                }
            }
        } catch(Exception e) {
            Log.e(TAG, "ERROR!!", e);
        }
        return true;
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);

        // Setting Dialog Title
        alertDialog.setTitle("GPS Not Enabled");

        // Setting Dialog Message
        alertDialog
                .setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, 0);
                    }
                });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        startNavigation();
    }

    public void startNavigation() {
        isRouteSet = true;
        mInProgress = false;
        arclient = new ActivityRecognitionClient(context, this, this);
        Intent intent = new Intent(context, ActivityRecognitionService.class);
        pIntent = PendingIntent.getService(context, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        // Check for Google Play services
        int resp = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resp == ConnectionResult.SUCCESS) {
            startUpdates();

        } else {
            Toast.makeText(context, "Please install Google Play Service.",
                    Toast.LENGTH_SHORT).show();
        }

        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
    }

    public void startUpdates() {

        // If a request is not already underway
        if (!mInProgress) {
            // Indicate that a request is in progress
            mInProgress = true;
            // Request a connection to Location Services
            arclient.connect();
            //
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        Toast.makeText(context, "Connection Failed", Toast.LENGTH_SHORT).show();
        // Turn off the request flag
        mInProgress = false;
    }

    @Override
    public void onConnected(Bundle arg0) {
        arclient.requestActivityUpdates(DETECTION_INTERVAL_MILLISECONDS, pIntent);
        mInProgress = false;
    }

    @Override
    public void onDisconnected() {
        // Turn off the request flag
        mInProgress = false;
        // Delete the client
        arclient = null;
    }

    public void StartTracking(View v){
        //android:onClick="StartTracking"
        if (!setRoute()) return;
        startNavigation();
    }

    public void cancelRoute() {
        isRouteSet = false;
        try {
            if (api != null) api.cancelRoute(1);
            ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(BackgroundService.NOTIFICATION_ID);
        } catch(Exception e) {
            Log.e(TAG, "ERROR!!", e);
        }
    }
}
