package com.example.nancy.aucklandtransport;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.media.AsyncPlayer;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.example.nancy.aucklandtransport.APIs.GoogleAPI;
import com.example.nancy.aucklandtransport.Utils.Constant;
import com.example.nancy.aucklandtransport.Utils.LocationUtils;
import com.example.nancy.aucklandtransport.datatype.Route;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

import org.androidpn.client.Constants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

//import android.location.LocationListener;


/**
 * BackgroundService class is the service that keeps running in the
 * background even when the application is not visible.
 * This class is used to track the user's location and provide him
 * the sound and vibration alerts throughout the journey.
 *
 * Created by Nancy on 7/29/14.
 */
public class BackgroundService extends Service implements
        LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    /*
    Debugging tag for the BackgroundService class
     */
    private static final String TAG = BackgroundService.class.getSimpleName();
    //private LocationManager locationManager;
    private int currentState = 0;

    private Location currentLocation = null;

    private Boolean isRouteStartedShown = false;
    private GoogleAPI api;
    float angle = 0;
    double lat, lng;

    public Route route;

    Calendar currentTime;
    Calendar depTime;
    Calendar arrTime;

    private boolean isGPSOn = false;

    public boolean isRouteSet = false;
    private Boolean isSameRoute = false;

    public String prevRouteString = "";

    SharedPreferences prefs;
    private boolean allowCoords;
    private Handler handle;
    public int reminderTime;
    private Handler mHandler;
    private Uri alarmSound;

    private Notification notification = null;
    private PendingIntent contentIntent;
    private Intent notificationIntent;
    private int toastLength = Toast.LENGTH_SHORT;
    String lastText = "";
    private AsyncPlayer aPlayer = new AsyncPlayer("aPlayer");
    public long timerInverval = 1000L;

    private boolean hasRemindedDep = false;

    private BroadcastReceiver myReceiver;
    String mActivity = "Still";


    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    public BackgroundService() {
        super();
    }

    private RouteEngine routeEngine;

    List<HashMap<String, String>> geoPlaces;

    public void  setGeoPlaces(List<HashMap<String, String>> value) {
         this.geoPlaces = value;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        myReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                if (action.equals("com.example.nancy.aucklandtransport.ACTIVITY_RECOGNITION_DATA")) {
                    mActivity = intent.getExtras().getString("Activity");
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.nancy.aucklandtransport.ACTIVITY_RECOGNITION_DATA");
        registerReceiver(myReceiver, filter);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        reminderTime = Integer.parseInt(prefs.getString("prefDepNotifInterval", "5")) * 60 * 1000;

        api = new GoogleAPI(BackgroundService.this);

        // get a handle on the location manager
        //locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        currentState = Constant.STATE_DO_NOTHING;

        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();

        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);

        /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
        mLocationClient.connect();

        routeEngine = new RouteEngine(this, getApplicationContext());

        handle = new Handler();

        alarmSound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.ringtone);
    }

    private Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            if (route != null && currentLocation != null) {

                if (currentState != Constant.STATE_DO_NOTHING && route != null) {
                    currentTime = Calendar.getInstance();
                    if (currentTime.compareTo(depTime) > 0 &&
                            !routeEngine.isStartedJourney()) {
                        long diff = route.getDeparture().getSeconds() * 1000L
                                - currentTime.getTimeInMillis(); // depTime.getTimeInMillis();

                        if (!hasRemindedDep) {
                            String str = String.format(getString(R.string.DepartureText1),
                                    Math.round(diff / (1000 * 60)));
                            if ( diff <0 && diff <= -(5 * 60 * 1000L))
                                str = String.format(getString(R.string.DepartureText3),
                                        Math.round(-diff / (1000 * 60)));
                            else if (diff <= 0)
                                str = String.format(getString(R.string.DepartureText2),
                                        Math.round(-diff / (1000 * 60)));

                            createNotification(getString(R.string.DepartureTimer),
                                    getString(R.string.app_name), str, true,
                                    (diff > reminderTime - 1500), Toast.LENGTH_LONG);
                            hasRemindedDep = true;
                        }
                        if (diff < 0 && !isRouteStartedShown) {
                            createNotification(getString(R.string.RouteStart),
                                    getString(R.string.app_name),
                                    getString(R.string.RouteStart), false, false, Toast.LENGTH_SHORT);
                            startRoute();
                            isRouteStartedShown = true;
                        }
                    }

                    if (currentTime.getTimeInMillis() - arrTime.getTimeInMillis() > reminderTime) {
                        changeState(Constant.STATE_DO_NOTHING);
                    }

                    routeEngine.routeEngine(route, mActivity, currentLocation);
                }
            }

            if(mHandler!=null)
                mHandler.postDelayed(mRunnable, 1000);
        }
    };

    public void routeDone() {
        createNotification(getString(R.string.RouteDone),
                getString(R.string.app_name),
                getString(R.string.RouteDone), false, false, Toast.LENGTH_LONG);
        changeState(Constant.STATE_DO_NOTHING);
    }

    @Override
    public void onDestroy() {

        deleteRoute();

        // If the client is connected
        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }

        // After disconnect() is called, the client is considered "dead".
        mLocationClient.disconnect();

        //locationManager.removeUpdates(this);

        super.onDestroy();

        if(mHandler != null)
            mHandler.removeCallbacks(mRunnable);
        mHandler = null;

//        timer.cancel();
//        timer = null;

        Log.i(TAG, "SERVICE DESTROYED");
        unregisterReceiver(myReceiver);
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(LocationUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            return false;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        startPeriodicUpdates();
        // TODO remove mock mode
        mLocationClient.setMockMode(true);

    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {

    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /**
     * In response to a request to start updates, send a request
     * to Location Services
     */
    private void startPeriodicUpdates() {

        mLocationClient.requestLocationUpdates(mLocationRequest, this);

    }

    /**
     * In response to a request to stop updates, send a request to
     * Location Services
     */
    private void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates(this);

    }

    public void onLocationChanged(Location loc) {
        currentLocation = loc;
        if (currentLocation.getProvider() == LocationManager.GPS_PROVIDER) {
            angle = currentLocation.getBearing();
        }

        lat = (int) (currentLocation.getLatitude() * 1E6);
        lng = (int) (currentLocation.getLongitude() * 1E6);
        Log.i(TAG, "LOCATION!!!!! " + currentLocation.getLatitude() + " " + currentLocation.getLongitude());
        synchronized (listeners) {
            for (IBackgroundServiceListener listener : listeners) {
                try {
                    listener.handleGPSUpdate(currentLocation.getLatitude(), currentLocation.getLongitude(), angle);
                } catch (RemoteException e) {
                    Log.e(TAG, "listener is " + listener);
                }
            }
        }
    }

    public void deleteRoute() {
        Log.i(TAG, "SERVICE deleteRoute");
        SharedPreferences settings = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("route");
        editor.remove("routeStarted").commit();
        editor.remove("isRouteSet").commit();
        editor.commit();

        // If the client is connected
        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }

        if(mHandler != null)
            mHandler.removeCallbacks(mRunnable);
        mHandler = null;
        //locationManager.removeUpdates(this);

        currentState = Constant.STATE_DO_NOTHING;
        route = null;
        prevRouteString = "";
    }

    public void startRoute() {
        Log.i(TAG, "SERVICE startRoute");
        allowCoords = prefs.getString("allowLoc", "dgdsfg").equals("Yes") ? true : false;
        SharedPreferences settings = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("routeStarted", true);
        editor.commit();
    }

    public void addressDiscovered(String s) {
        synchronized (listeners) {
            for (IBackgroundServiceListener listener : listeners) {
                try {
                    listener.addressDiscovered(s);
                } catch (RemoteException e) {
                    Log.e(TAG, "in getting address", e);
                    Log.e(TAG, "listener is " + listener);
                }
            }
        }
    }

    public void locationDiscovered(Location l) {
        if (l == null) return;
        synchronized (listeners) {
            for (IBackgroundServiceListener listener : listeners) {
                try {
                    listener.locationDiscovered(l.getLatitude(), l.getLongitude());
                } catch (RemoteException e) {
                    Log.e(TAG, "in locationDiscovered", e);
                    Log.e(TAG, "listener is " + listener);
                }
            }
        }
    }

    /**
     * The actual API
     */
    public void setServiceRoute(String r) {
        Log.i("FROM SERVICE", r);
        try {

            Log.i(TAG, "is not new? " + prevRouteString.equals(r));
            if (route != null && prevRouteString.equals(r)) {
                // This is the same route
                isSameRoute = true;
            } else {
                hasRemindedDep = false;
                isSameRoute = false;
                this.route = new Route(r);
            }

            prevRouteString = r;

            depTime = Calendar.getInstance();
            arrTime = Calendar.getInstance();
            depTime.setTimeInMillis(route.getDeparture().getSeconds() * 1000L - reminderTime);
            arrTime.setTimeInMillis(route.getArrival().getSeconds() * 1000L);

            Log.i(TAG, "DATES: " + arrTime + " " + depTime);
            if (!mLocationClient.isConnected())
                mLocationClient.connect();

            if(mLocationClient.isConnected())
                mLocationClient.requestLocationUpdates(mLocationRequest, this);

            mHandler = new Handler();
            mHandler.postDelayed(mRunnable, timerInverval);
            routeEngine.setRouteState(Constant.INIT);
            routeEngine.setStartedJourney(false);
            routeEngine.resetStep();

            isRouteSet = true;
            handle.post(new Runnable() {
                public void run() {
                    changeState(Constant.STATE_START_ROUTE);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "setServiceRoute", e);
        }
    }

    private void changeState(int state) {
        currentState = state;
        if (state == Constant.STATE_START_ROUTE) {
            //mLocationClient.requestLocationUpdates(mLocationRequest, this);
            if (!isSameRoute) {
                createNotification(getString(R.string.RouteStart), getString(R.string.app_name), getString(R.string.RouteStart), false, false, Toast.LENGTH_LONG);
                isRouteStartedShown = true;
            }
        } else if (state == Constant.STATE_DO_NOTHING) {
            deleteRoute();
            createNotification(getString(R.string.RouteFinish), getString(R.string.app_name), getString(R.string.RouteFinishText), false, false, Toast.LENGTH_LONG);
            cancelNotification();
        }
    }

    public void getAddressFromGoogle(final Location l) {
        if (l == null) return;
        Log.i(TAG, "getAddressFromGoogle " + l);
        new Thread(new Runnable() {
            public void run() {
                api.getReverseGeocode(new LatLng(l.getLatitude(), l.getLongitude()),
                        getString(R.string.API_KEY));

                if(geoPlaces != null) {
                    if (geoPlaces.size() > 0) {
                        HashMap<String, String> hmPlace = geoPlaces.get(0);
                        addressDiscovered(hmPlace.get("formatted_address"));
                    }
                }
            }
        }).run();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return apiEndpoint;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     * The exposed API implementation
     */
    private List<IBackgroundServiceListener> listeners = new ArrayList<IBackgroundServiceListener>();

    private IBackgroundServiceAPI.Stub apiEndpoint = new IBackgroundServiceAPI.Stub() {

        public void setRoute(String route) {
            Log.i(TAG, "setRoute: " + route);
            setServiceRoute(route);
        }

        public void cancelRoute(int notify) {
            deleteRoute();
            cancelNotification(notify > 0);
        }

        public void addListener(IBackgroundServiceListener l) {
            synchronized (listeners) {
                listeners.add(l);
            }
        }

        public void removeListener(IBackgroundServiceListener l) {
            synchronized (listeners) {
                listeners.remove(l);
            }
        }

        public int requestLastKnownAddress(int getAddress) {
            Log.i(TAG, "SERVICE requestLastKnownAddress");
            if (!mLocationClient.isConnected())
                mLocationClient.connect();

            if(mLocationClient.isConnected()) {
                Location l1 = mLocationClient.getLastLocation();
                //getServiceLastKnownLocation();
                Log.i(TAG, "requestLastKnownAddress:\n" + String.valueOf(l1));
                currentLocation = l1;
                if (l1 == null) {
                    addressDiscovered("");
                    return 0;
                }

                locationDiscovered(l1);
                if (getAddress > 0)
                    getAddressFromGoogle(l1);
                return 1;
            }
            return 0;
        }

        public boolean isGPSOn() {
            boolean gpson = servicesConnected();
            //locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            Log.i(TAG, "GPS status: " + gpson);
            return gpson;
        }
    };

    public void createNotification(String ticker, String title, String text, boolean vibrate, boolean sound, int tlength) {
        String ns = Context.NOTIFICATION_SERVICE;
        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        long when = System.currentTimeMillis();

        notificationIntent = new Intent(this, RouteMapActivity.class);
        notificationIntent.putExtra(Constants.NOTIFICATION_MESSAGE, text);
        notificationIntent
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notification = new Notification(R.drawable.notification, ticker, when);

        // Creating an intent for broadcastreceiver
        Intent broadcastIntent = new Intent(Constant.BROADCAST_NOTIFICATION);
        // Attaching data to the intent
        broadcastIntent.putExtra(Constants.NOTIFICATION_MESSAGE, text);
        // Sending the broadcast
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);

        //notificationIntent.putExtra(Constants.NOTIFICATION_MESSAGE, text);

        contentIntent =
                PendingIntent.getActivity(this, 0, notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        notification.defaults = 0;
        notification.defaults |= Notification.DEFAULT_LIGHTS;

        if (vibrate) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }

        if (sound) {
            aPlayer.play(this, alarmSound, false, AudioManager.STREAM_SYSTEM);
        }

        notification.ledARGB = 0xff00ff00;
        notification.ledOnMS = 300;
        notification.ledOffMS = 1000;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;

        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_NO_CLEAR;

        notification.setLatestEventInfo(this, title, text, contentIntent);

        lastText = text;
        toastLength = tlength;
        mNotificationManager.notify(Constant.NOTIFICATION_ID, notification);
        handle.post(new Runnable() {
            public void run() {
                try {
                    Toast.makeText(BackgroundService.this, lastText, toastLength).show();
                } catch (Exception e) {
                }
                ;
            }
        });
    }

    public void cancelNotification() {
        cancelNotification(true);
    }

    public void cancelNotification(final Boolean cancel) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);


        mNotificationManager.cancel(Constant.NOTIFICATION_ID);

        handle.post(new Runnable() {
            public void run() {
                if (cancel)
                    Toast.makeText(BackgroundService.this, getString(R.string.RouteCancel), Toast.LENGTH_SHORT).show();
            }
        });

    }

}
