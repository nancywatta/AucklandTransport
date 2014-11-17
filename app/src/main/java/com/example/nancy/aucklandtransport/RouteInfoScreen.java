package com.example.nancy.aucklandtransport;

import android.app.ActionBar;
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
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.nancy.aucklandtransport.Adapters.RouteInfoAdapter;
import com.example.nancy.aucklandtransport.MyAlertDialogWIndow.AlertPositiveListener;
import com.example.nancy.aucklandtransport.Utils.Constant;
import com.example.nancy.aucklandtransport.datatype.Route;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.example.nancy.aucklandtransport.BackgroundJobs.ActivityRecognitionService;

/**
 * RouteInfoScreen class is used to provide the detailed
 * information on the route selected by the user.
 *
 * Created by Nancy on 7/10/14.
 */
public class RouteInfoScreen extends FragmentActivity implements AlertPositiveListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener{
    private static final String TAG = RouteInfoScreen.class.getSimpleName();

    private String routeString;
    Route route = null;
    ListView listView;
    private boolean isRouteSet = false;
    private Boolean routeStarted = false;
    private ActivityRecognitionClient arclient;
    private PendingIntent pIntent;
    private Boolean mInProgress;
    // Constants that define the activity detection interval
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DETECTION_INTERVAL_SECONDS = 1;
    public static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_info_screen);
        isRouteSet = false;
        getRoute();
        listView = (ListView) findViewById(R.id.RouteInfoScreenListView);
        if(route!=null)
        listView.setAdapter(new RouteInfoAdapter(RouteInfoScreen.this, route));
        Intent servIntent = new Intent(BackgroundService.class.getName());
        startService(servIntent);
        Log.i(TAG, "starting service "+servIntent.toString());
        bindService(servIntent, serviceConnection, Context.BIND_AUTO_CREATE);
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
    }

    private IBackgroundServiceAPI api = null;
    private boolean isGPSon = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Service disconnected!");
            api = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            api = IBackgroundServiceAPI.Stub.asInterface(service);
            Log.i(TAG, "Service connected! "+api.toString());
        }
    };

    private void getRoute() {
        SharedPreferences settings = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        try {
            routeStarted = settings.getBoolean("routeStarted", false);
            isRouteSet = settings.getBoolean("isRouteSet", false);

            if (routeStarted) isRouteSet = routeStarted;

            Intent intent = getIntent();
            routeString = intent.getStringExtra("route");
            if (!routeString.equals("")) route = new Route(routeString);
            else {
                Log.d(TAG, "Shared Working :)");
                routeString = settings.getString("route", "");
                route = new Route(routeString);
            }
        } catch ( Exception e ) {
            Log.e("ERROR", "Couldn't get the route from JSONobj");
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.route_info_screen, menu);
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

    public void ShowMap(View v){
        //android:onClick="ShowMap"
        try {
            Intent myIntent = new Intent(this, RouteMapActivity.class);
            myIntent.putExtra("route", routeString);
            startActivity(myIntent);
        }catch (Exception e) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unbindService(serviceConnection);
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
    protected void onResume() {
        super.onResume();

        getRoute();
    }

    private boolean setRoute() {
        try {
            if (api != null) {
                api.setRoute(routeString);
                SharedPreferences settings = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
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
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(RouteInfoScreen.this);

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        startNavigation();
    }

    public void startNavigation() {
        isRouteSet = true;
        mInProgress = false;
        arclient = new ActivityRecognitionClient(this, this, this);
        Intent intent = new Intent(this, ActivityRecognitionService.class);
        pIntent = PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        // Check for Google Play services
        int resp = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resp == ConnectionResult.SUCCESS) {
            startUpdates();

        } else {
            Toast.makeText(this, "Please install Google Play Service.",
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
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
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

    private void cancelRoute() {
        isRouteSet = false;
        try {
            if (api != null) api.cancelRoute(1);
            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Constant.NOTIFICATION_ID);
        } catch(Exception e) {
            Log.e(TAG, "ERROR!!", e);
        }
    }
    @Override
    public void onBackPressed() {
        if(isRouteSet) {
            //FragmentManager fm = getFragmentManager();

            /** Instantiating the DialogFragment */
            MyAlertDialogWIndow alert = new MyAlertDialogWIndow();

            Bundle args = new Bundle();
            args.putString("message", "Do you want to cancel the route?");
            alert.setArguments(args);

            /** Opening the dialog window */
            alert.show(getSupportFragmentManager(), "Alert_Dialog");
        }
        else
            super.onBackPressed();
        //cancelRoute();
    }

    /** Defining button click listener for the OK button of the alert dialog window */
    @Override
    public void onPositiveClick(boolean isLocationSet) {
        cancelRoute();
        super.onBackPressed();
    }
}
