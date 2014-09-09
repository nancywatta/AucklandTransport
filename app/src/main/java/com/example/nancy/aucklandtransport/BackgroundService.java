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
import android.util.Log;
import android.widget.Toast;

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
import java.util.Timer;
import java.util.TimerTask;

//import android.location.LocationListener;


/**
 * Created by Nancy on 7/29/14.
 */
public class BackgroundService extends Service implements
        LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener{

    private static final String TAG = BackgroundService.class.getSimpleName();
    //private LocationManager locationManager;
    private int currentState = 0;
    public static int STATE_DO_NOTHING	= 0;
    public static int STATE_START_ROUTE	= 1;

    private Location currentLocation = null;
    private Location previousLocation = null;
    private Boolean discoverAddress = false;
    private int currentStep = 0;

    private Boolean isRouteStartedShown = false;
    private GoogleAPI api;
    float angle = 0;
    double lat, lng;

    private String lastKnownAddress = "";

    public Route route;

    Calendar currentTime;
    Calendar depTime;
    Calendar arrTime;

    private boolean isGPSOn = false;

    private boolean isRouteSet = false;
    private boolean isNotRouteInSettings = false;
    private Boolean isSameRoute = false;

    private String prevRouteString = "";

    SharedPreferences prefs;
    private boolean allowCoords;
    private Handler handle;
    public int reminderTime;
    private Timer timer;
    private Uri alarmSound;

    private Notification notification = null;
    private PendingIntent contentIntent;
    private Intent notificationIntent;
    private int toastLength = Toast.LENGTH_SHORT;
    public static final int NOTIFICATION_ID = 12345;
    String lastText = "";
    private AsyncPlayer aPlayer = new AsyncPlayer("aPlayer");
    public long timerInverval = 1000L;

    private boolean hasRemindedDep = false;

    private BroadcastReceiver myReceiver;
    String mActivity="Still";

    // Flags for routeEngine
    private int routeState = Constant.INIT ;
    private int nextStop = 0;
    private long searchInterval =0;
    private boolean firstTime = true;
    private float prevDistance = 0;
    private boolean getRealTime = false;
    RouteStep s= null;

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    public BackgroundService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        myReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                if(action.equals("com.example.nancy.aucklandtransport.ACTIVITY_RECOGNITION_DATA")){
                    mActivity = intent.getExtras().getString("Activity");
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.nancy.aucklandtransport.ACTIVITY_RECOGNITION_DATA");
        registerReceiver(myReceiver, filter);

        timer = new Timer(getString(R.string.Timer));
        timer.schedule(updateTask, 0, timerInverval);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        reminderTime = Integer.parseInt(prefs.getString("prefDepNotifInterval", "5")) * 60 * 1000;

        api = new GoogleAPI();

        // get a handle on the location manager
        //locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        currentState = STATE_DO_NOTHING;

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

        handle = new Handler();

        alarmSound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alarm_clock);
    }

    private TimerTask updateTask = new TimerTask() {
        @Override
        public void run() {
            if (route != null && currentLocation != null) {

                if (currentState != STATE_DO_NOTHING && route != null) {
                    currentTime = Calendar.getInstance();
                        if (currentTime.compareTo(depTime) > 0) {
                            long diff = route.getDeparture().getSeconds()*1000L - currentTime.getTimeInMillis(); // depTime.getTimeInMillis();

                            if (!hasRemindedDep) {
                                String str = String.format(getString(R.string.DepartureText1), Math.round(diff/(1000*60)));
                                if (diff <= 0) str = String.format(getString(R.string.DepartureText2), Math.round(-diff/(1000*60)));
                                createNotification(getString(R.string.DepartureTimer), getString(R.string.app_name), str, true, (diff > reminderTime - 1500), Toast.LENGTH_LONG);
                                hasRemindedDep = true;
                            }
                            if (diff < 0 && !isRouteStartedShown) {
                                createNotification(getString(R.string.RouteStart), getString(R.string.app_name), getString(R.string.RouteStart), false, false, Toast.LENGTH_SHORT);
                                startRoute();
                                isRouteStartedShown = true;
                            }
                        }

                    if (currentTime.getTimeInMillis() -  arrTime.getTimeInMillis() > reminderTime) {
                        changeState(STATE_DO_NOTHING);
                    }

                    routeEngine();
                }
            }
        }
    };

    private void routeEngine() {
        if (route == null || currentLocation == null) {
            if (route == null && !isRouteSet && !isNotRouteInSettings) {
                getRouteFromSettings();
            }
            Log.i(TAG, "processRoute error: "+String.valueOf(route)+" "+String.valueOf(currentLocation));
            return;
        }

        Location dest = new Location("");
        PathSegment pathSegment;
        float dist;

        switch(routeState)
        {
            case Constant.INIT:
                nextStop = 0;
                searchInterval= 0;
                firstTime = true;
                prevDistance = 0;
                getRealTime = false;
                s = route.getSteps().get(currentStep);
                routeState = Constant.CHANGE_OVER;
                break;
            case Constant.CHANGE_OVER:
                if(currentStep >= route.getSteps().size())
                    routeState = Constant.FINISHED;
                if (s.getTransportName() == R.string.tr_walk) {
                        //&& mActivity.compareTo("On Foot") == 0) {
                    Log.d(TAG, "I am Walking");
                    routeState = Constant.WALKING;
                    currentStep++;
                    break;
                }
                else if((s.getTransportName() == R.string.tr_train || s.getTransportName() == R.string.tr_bus
                        || s.getTransportName() == R.string.tr_boat || s.getTransportName() == R.string.tr_metro)
                        && mActivity.compareTo("In Vehicle") == 0) {
                    routeState = Constant.TRANSIT;
                    currentStep++;
                    break;
                }
                else
                    break;
            case Constant.PRE_CHANGE_OVER:
                dest.setLatitude(s.getEndLoc().latitude);
                dest.setLongitude(s.getEndLoc().longitude);
                dist = currentLocation.distanceTo(dest); // Approximate distance in meters
                if(dist<2)
                    routeState = Constant.INIT;
                break;

            case Constant.WALKING:
                pathSegment = s.getPath().get(nextStop);
                long maxSeconds = pathSegment.getTravelTime().getSeconds();

                if(searchInterval==0 && !pathSegment.isNotified) {
                    if(firstTime) {
                        searchInterval = fixInterval(maxSeconds);
                        if(searchInterval > 5)
                            getRealTime = true;
                        firstTime = false;
                    }
                    else {
                        if(getRealTime) {
                            GoogleAPI googleAPI = new GoogleAPI();
                            long seconds = googleAPI.getDuration(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    pathSegment.getEndLoc());
                            searchInterval = fixInterval(seconds);

                            if(searchInterval == 5)
                                getRealTime = false;

                            if (route.getSteps().get(currentStep).isTransit()) {
                                Calendar c = Calendar.getInstance();
                                c.add(Calendar.SECOND, (int) seconds);
                                long diff = (route.getSteps().get(currentStep).getDeparture().getSeconds() * 1000L) - c.getTimeInMillis();
                                if (diff < 120) {
                                    createNotification(getString(R.string.RunningLateText),
                                            getString(R.string.app_name),
                                            getString(R.string.RunningLateText), true, true, Toast.LENGTH_LONG);
                                }
                            }
                        }
                        else
                            searchInterval = 5;
                    }
                    dest.setLatitude(pathSegment.getEndLoc().latitude);
                    dest.setLongitude(pathSegment.getEndLoc().longitude);
                    dist = currentLocation.distanceTo(dest); // Approximate distance in meters
                    if(prevDistance !=0) {
                        long nextdist = 0;
                        if(nextStop < s.getPath().size() - 1 )
                            nextdist = s.getPath().get(nextStop+1).getDistance();
                        if(prevDistance + nextdist + 5 < dist + nextdist) {
                            routeState = Constant.OFF_ROUTE;
                            Log.d(TAG, "Off Route : " + nextStop + " : prev " +
                                    prevDistance + " : dist " + dist);
                            Log.i(TAG, "Off Route LOCATION!!!!! " + currentLocation.getLatitude() + " " + currentLocation.getLongitude());
                        }
                    }
                    prevDistance = dist;
                    if(dist < 10) {
                        createNotification(pathSegment.getInstruction(),
                                getString(R.string.app_name),
                                pathSegment.getInstruction(), true, true, Toast.LENGTH_LONG);
                        pathSegment.isNotified = true;
                        nextStop++;
                        prevDistance = 0;
                        if(nextStop >= s.getPath().size())
                            routeState = Constant.PRE_CHANGE_OVER;
                        //searchInterval = 0;
                        firstTime = true;
                    }
                }
                else if(searchInterval>0)
                {
                    searchInterval--;
                }

                break;
            case Constant.TRANSIT:
                break;
            case Constant.STOPPED:
                break;
            case Constant.OFF_ROUTE:
                createNotification(getString(R.string.OffRouteText),
                        getString(R.string.app_name),
                        getString(R.string.OffRouteText), true, true, Toast.LENGTH_LONG);
                routeState = Constant.WALKING;
                break;
            case Constant.FINISHED:
                routeDone();
                routeState = Constant.DEFAULT;
                break;
            default:
                break;

        }

    }

    long fixInterval(long maxTime)
    {
        long interval;
        /*if(maxTime >200)
        {
            interval = 140;
        }
        else if(maxTime>160)
        {
            interval = 100;
        }
        else if(maxTime>120)
        {
            interval = 60;
        }
        else if(maxTime>80)
        {
            interval = 20;
        }
        else*/ if(maxTime > 59)
        {
            interval = 25;
        }
        else
        {
            interval = 5;
        }
        return interval;
    }

    private void nextRouteStep() {
        if (currentStep + 1 < route.getSteps().size()) {}
        else routeDone();
    }

    private void routeDone() {
        createNotification(getString(R.string.RouteDone),
                getString(R.string.app_name),
                getString(R.string.RouteDone), false, false, Toast.LENGTH_LONG);
    }

    @Override
    public void onDestroy() {
        // If the client is connected
        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }

        // After disconnect() is called, the client is considered "dead".
        mLocationClient.disconnect();

        //locationManager.removeUpdates(this);

        super.onDestroy();
        timer.cancel();
        timer = null;

        Log.i(TAG, "SERVICE DESTROYED");
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
    // TODO Auto-generated method stub
        previousLocation = currentLocation;
        currentLocation = loc;
        if (currentLocation.getProvider() == LocationManager.GPS_PROVIDER) {
            angle = currentLocation.getBearing();
        }

        lat = (int) (currentLocation.getLatitude()*1E6);
        lng = (int) (currentLocation.getLongitude()*1E6);
        Log.i(TAG, "LOCATION!!!!! " + currentLocation.getLatitude() + " " + currentLocation.getLongitude());
        synchronized (listeners) {
            for (IBackgroundServiceListener listener : listeners) {
                try {
                    listener.handleGPSUpdate(currentLocation.getLatitude(), currentLocation.getLongitude(), angle);
                } catch (RemoteException e) {
                    Log.e(TAG, "listener is "+listener);
                }
            }
        }
        if (discoverAddress) {
            getAddressFromGoogle(loc);
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

        mLocationClient.disconnect();

        //locationManager.removeUpdates(this);

        currentState = STATE_DO_NOTHING;
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

    public void getRouteFromSettings() {
        Log.i(TAG, "SERVICE getRouteFromSettings");
        SharedPreferences settings = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        try {
            String routeString = settings.getString("route", "");

            if (!routeString.equals("")) route = new Route(routeString);

            prevRouteString = routeString;
            isNotRouteInSettings = false;
            return;
        } catch ( Exception e ) {
        }
        isNotRouteInSettings = true;
    }

    public void addressDiscovered(String s) {
        synchronized (listeners) {
            for (IBackgroundServiceListener listener : listeners) {
                try {
                    listener.addressDiscovered(s);
                } catch (RemoteException e) {
                    Log.e(TAG, "in getting address", e);
                    Log.e(TAG, "listener is "+listener);
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
                    Log.e(TAG, "listener is "+listener);
                }
            }
        }
    }

    /** The actual API */
    public void setServiceRoute(String r) {
        Log.i("FROM SERVICE", r);
        try {

            Log.i(TAG, "is not new? "+prevRouteString.equals(r));
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
            depTime.setTimeInMillis(route.getDeparture().getSeconds()*1000L - reminderTime);
            arrTime.setTimeInMillis(route.getArrival().getSeconds()*1000L);

            Log.i(TAG, "DATES: " + arrTime + " " + depTime);

            isRouteSet = true;
            handle.post(new Runnable() {
                public void run() {
                    changeState(STATE_START_ROUTE);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "setServiceRoute", e);
        }
    }

    private void changeState(int state) {
        currentState = state;
        if (state == STATE_START_ROUTE) {
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
            currentStep = 0;
            if (!isSameRoute) {
                createNotification(getString(R.string.RouteStart), getString(R.string.app_name), getString(R.string.RouteStart), false, false, Toast.LENGTH_LONG);
                isRouteStartedShown = true;
            }
        } else if (state == STATE_DO_NOTHING) {
            deleteRoute();
            createNotification(getString(R.string.RouteFinish), getString(R.string.app_name), getString(R.string.RouteFinishText), false, false, Toast.LENGTH_LONG);
            cancelNotification();
        }
    }

    public void getAddressFromGoogle(final Location l) {
        if (l == null) return;
        Log.i(TAG, "getAddressFromGoogle "+l);
        new Thread(new Runnable() {
            public void run() {
                api.getReverseGeocode(new LatLng(l.getLatitude(), l.getLongitude()));
                List<HashMap<String, String>> recs = api.geoPlaces;
                if (recs.size() > 0) {
                    HashMap<String, String> hmPlace = recs.get(0);
                    lastKnownAddress = hmPlace.get("formatted_address");
                    addressDiscovered(hmPlace.get("formatted_address"));
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

    /** The exposed API implementation */
    private List<IBackgroundServiceListener> listeners = new ArrayList<IBackgroundServiceListener>();

    private IBackgroundServiceAPI.Stub apiEndpoint = new IBackgroundServiceAPI.Stub() {

        public void setRoute(String route) {
            Log.i(TAG, "setRoute: "+route);
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
            Location l1 = mLocationClient.getLastLocation();
                    //getServiceLastKnownLocation();
            Log.i(TAG, "requestLastKnownAddress:\n"+String.valueOf(l1));
            previousLocation = currentLocation;
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

        public boolean isGPSOn() {
            boolean gpson = servicesConnected();
                    //locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            Log.i(TAG,"GPS status: "+gpson);
            return gpson;
        }
    };

    public void createNotification(String ticker, String title, String text,  boolean vibrate, boolean sound, int tlength) {
        String ns = Context.NOTIFICATION_SERVICE;
        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        long when = System.currentTimeMillis();

        notificationIntent = new Intent(this, NotificationUpdates.class);
        notificationIntent.putExtra(Constants.NOTIFICATION_MESSAGE, text);
        notificationIntent
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notification = new Notification(R.drawable.notification, ticker, when);

        notificationIntent.putExtra(Constants.NOTIFICATION_MESSAGE, text);

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
        mNotificationManager.notify(NOTIFICATION_ID, notification);
        handle.post(new Runnable() {
            public void run() {
                try {
                    Toast.makeText(BackgroundService.this, lastText, toastLength).show();
                } catch(Exception e) {};
            }
        });
    }

    public void cancelNotification() {
        cancelNotification(true);
    }

    public void cancelNotification(final Boolean cancel) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);


        mNotificationManager.cancel(NOTIFICATION_ID);

        handle.post(new Runnable() {
            public void run() {
                if (cancel) Toast.makeText(BackgroundService.this, getString(R.string.RouteCancel), Toast.LENGTH_SHORT).show();
            }
        });

    }

}
