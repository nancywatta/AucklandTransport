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
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
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

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by Nancy on 7/29/14.
 */
public class BackgroundService extends Service implements
        LocationListener {
    private static final String TAG = BackgroundService.class.getSimpleName();
    private LocationManager locationManager;
    private int currentState = 0;
    public static int STATE_DO_NOTHING	= 0;
    public static int STATE_START_ROUTE	= 1;
    public static int STATE_IM_INBUS	= 2;
    public static int STATE_LAST_STOP	= 3;
    public static int STATE_LAZY_MODE	= 4;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private Location currentLocation = null;
    private Location previousLocation = null;
    private Boolean discoverAddress = false;
    private int currentStep = 0;
    float minDist = 9999999;
    int minDistIdx = -1;
    public int minTimeGPS = 2 * 1000;
    public int minTimeNetwork = 1 * 1000;
    private Boolean isRouteStartedShown = false;
    private GoogleAPI api;
    float angle = 0;
    double lat, lng;

    private String lastKnownAddress = "";
    public boolean isGPSProviderOn		= false;
    public boolean isNetworkProviderOn	= false;
    public boolean isWIFIProviderOn		= false;
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
    public float BUS_SPEED = 4.5f; // m/s

    public float prevSpeed = 0;
    private boolean hasRemindedDep = false;
    float prevMinDist, prevMinDistIdx;
    private BroadcastReceiver myReceiver;
    String mActivity="Still";

    // Flags for routeEngine
    private int routeState = Constant.INIT ;
    private int nextStop = 0;
    private long searchInterval =0;
    private boolean firstTime = true;
    private float prevDistance = 0;
    private boolean getRealTime = false;

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
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        currentState = STATE_DO_NOTHING;

        Log.i(TAG, "SERVICE CREATED");
        handle = new Handler();

        alarmSound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alarm_clock);
    }

    private TimerTask updateTask = new TimerTask() {
        @Override
        public void run() {
            if (route != null && currentLocation != null) {
                float speed = currentLocation.getSpeed();

                if (currentLocation != null) { //  && currentLocation.getProvider() == LocationManager.GPS_PROVIDER
                    if (prevSpeed > 0) {
                        if (speed >= BUS_SPEED && prevSpeed < BUS_SPEED && currentState != STATE_IM_INBUS) {
                            changeState(STATE_IM_INBUS);
                        }
                    }
                    prevSpeed = speed;
                }
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
                Log.i("DEBUG!", ""+allowCoords);
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

        RouteStep s = route.getSteps().get(currentStep);
        Location dest = new Location("");
        PathSegment pathSegment = s.getPath().get(nextStop);
        float dist;

        switch(routeState)
        {
            case Constant.INIT:
                nextStop = 0;
                searchInterval= 0;
                firstTime = true;
                prevDistance = 0;
                getRealTime = false;
                routeState = Constant.CHANGE_OVER;
                break;
            case Constant.CHANGE_OVER:
                if (s.getTransportName() == R.string.tr_walk && mActivity.compareTo("On Foot") == 0) {
                    routeState = Constant.WALKING;
                    changeState(STATE_LAST_STOP);
                }
                else if((s.getTransportName() == R.string.tr_train || s.getTransportName() == R.string.tr_bus
                        || s.getTransportName() == R.string.tr_boat || s.getTransportName() == R.string.tr_metro)
                        && mActivity.compareTo("In Vehicle") == 0) {
                    routeState = Constant.TRANSIT;
                    changeState(STATE_IM_INBUS);
                }
                else
                    return;
                searchInterval = 0;
                prevDistance = 0;
                firstTime = true;
                getRealTime = false;
                currentStep++;
                if(currentStep >= route.getSteps().size())
                    routeState = Constant.FINISHED;
                break;
            case Constant.PRE_CHANGE_OVER:
                dest.setLatitude(s.getEndLoc().latitude);
                dest.setLongitude(s.getEndLoc().longitude);
                dist = currentLocation.distanceTo(dest); // Approximate distance in meters
                if(dist<2)
                    routeState = Constant.CHANGE_OVER;
                break;

            case Constant.WALKING:
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

                            if (route.getSteps().get(currentStep + 1).isTransit()) {
                                Calendar c = Calendar.getInstance();
                                c.add(Calendar.SECOND, (int) seconds);
                                long diff = (route.getSteps().get(currentStep + 1).getDeparture().getSeconds() * 1000L) - c.getTimeInMillis();
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
                        if(nextStop < s.getPath().size())
                            nextdist = s.getPath().get(nextStop+1).getDistance();
                        if(prevDistance + nextdist <= dist + nextdist)
                            routeState = Constant.OFF_ROUTE;
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
        changeState(STATE_LAST_STOP);
        if (currentStep + 1 < route.getSteps().size()) {}
        else routeDone();
    }

    private void routeDone() {
        createNotification(getString(R.string.RouteDone),
                getString(R.string.app_name),
                getString(R.string.RouteDone), false, false, Toast.LENGTH_LONG);

        changeState(STATE_LAZY_MODE);
    }

    @Override
    public void onDestroy() {
        locationManager.removeUpdates(this);

        super.onDestroy();
        timer.cancel();
        timer = null;

        Log.i(TAG, "SERVICE DESTROYED");
    }

    public void onProviderDisabled(String pr) {
        Log.v(TAG, "ProviderEnabled: " + pr);
    }

    public void onProviderEnabled(String pr) {
        Log.v(TAG, "ProviderEnabled: "+pr);
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {

        switch (status) {
            case LocationProvider.OUT_OF_SERVICE:
                Log.v(TAG, "Status Changed: Out of Service");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Log.v(TAG, "Status Changed: Temporarily Unavailable");
                break;
            case LocationProvider.AVAILABLE:
                Log.v(TAG, "Status Changed: Available");
                break;
        }
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
        Log.i(TAG, "LOCATION!!!!! "+currentLocation.getLatitude()+" "+currentLocation.getLongitude());
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
        locationManager.removeUpdates(this);
        currentState = STATE_DO_NOTHING;
        route = null;
        prevRouteString = "";
    }

    public void startRoute() {
        Log.i(TAG, "SERVICE startRoute");
        allowCoords = prefs.getString("allowLoc", "dgdsfg").equals("Yes") ? true : false;
        Log.i("DEBUG!!", prefs.getString("allowLoc", "Yes"));
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

    public Location getServiceLastKnownLocation() {
        Location l1 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location l2 = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Location l3 = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (l1 == null && l2 == null && l3 != null) return l3;

        Log.i(TAG, "getServiceLastKnownLocation:\n"+String.valueOf(l1)+"\n"+String.valueOf(l2)+"\n"+String.valueOf(l2)+"\n");

        if ((l1 != null || l2 != null )) {
            if (isBetterLocation(l1, l2)) return l1;
        }
        return l2;
    }

    /** Taken from http://developer.android.com/guide/topics/location/obtaining-user-location.html
     * Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        } else if (location == null) return false;

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeGPS,  0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTimeNetwork,  0, this);
            currentStep = 0;
            if (!isSameRoute) {
                createNotification(getString(R.string.RouteStart), getString(R.string.app_name), getString(R.string.RouteStart), false, false, Toast.LENGTH_LONG);
                isRouteStartedShown = false;
            }
        } else if (state == STATE_IM_INBUS) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,  0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,  0,this);

        } else if (state == STATE_LAST_STOP) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeGPS,  0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTimeNetwork,  0, this);
        } else if (state == STATE_LAZY_MODE) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000 * 60*60,  0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000 * 60*60,  0, this);
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
            Location l1 = getServiceLastKnownLocation();
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
            boolean gpson = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            Log.i(TAG,"GPS status: "+gpson);
            return gpson;
        }
    };

    public void createNotification(String ticker, String title, String text,  boolean vibrate, boolean sound, int tlength) {
        String ns = Context.NOTIFICATION_SERVICE;
        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

        long when = System.currentTimeMillis();
        if (notification == null) {
            notificationIntent = new Intent(this, RouteInfoScreen.class);
//            notificationIntent.putExtra("currentStep", currentStep);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        notification = new Notification(R.drawable.notification, ticker, when);
        contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
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
