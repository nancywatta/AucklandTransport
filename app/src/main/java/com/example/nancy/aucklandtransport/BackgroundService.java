package com.example.nancy.aucklandtransport;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
public class BackgroundService extends Service implements LocationListener {
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

    public BackgroundService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        timer = new Timer(getString(R.string.Timer));
        timer.schedule(updateTask, 0, timerInverval);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        reminderTime = Integer.parseInt(prefs.getString("prefDepNotifInterval", "5")) * 60 * 1000;

        api = new GoogleAPI();

        // get a hangle on the location manager
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

                    processRoute();
                }
                Log.i("DEBUG!", ""+allowCoords);
            }
        }
    };

    private void processRoute() {
        if (route == null || currentLocation == null) {
            if (route == null && !isRouteSet && !isNotRouteInSettings) {
                getRouteFromSettings();
            }
            Log.i(TAG, "processRoute error: "+String.valueOf(route)+" "+String.valueOf(currentLocation));
            return;
        }
        prevMinDist = minDist;
        prevMinDistIdx = minDistIdx;

        minDist = 9999999;
        minDistIdx = -1;

        int lastCurrentStep = currentStep;
        //Log.i(TAG, "lastCurrentStep: "+lastCurrentStep+" "+currentStep);
        RouteStep lastRouteStep = null;
        //RouteStep lastRouteStep = null;
        String lastName = "";

        for (int j=0; j<route.getSteps().size(); j++) {
            RouteStep s = route.getSteps().get(j);

            currentTime = Calendar.getInstance();
            long now =  currentTime.getTimeInMillis() /1000L;

            //for (int i=0; i < s.path.size(); i++) {
              //  PathSegment p = s.path.get(i);

                Location dest = new Location("");
                dest.setLatitude(s.getEndLoc().latitude);
                dest.setLongitude(s.getEndLoc().longitude);
                float dist = currentLocation.distanceTo(dest); // Approximate distance in meters

                if (dist < minDist) {
                    minDist = dist;
                    minDistIdx = j;
                    currentStep = j;
                    lastRouteStep = s;
                }

                if (s.getTransportName() == R.string.tr_metro
                        || s.getTransportName() == R.string.tr_train) {

                    long time1 = s.getDeparture().getSeconds();
                    long time2 = s.getArrival().getSeconds();
                    if (now>time1 && now + 120 >= time2  && now < time2 && time1 >0) {
                        createNotification(String.format(getString(R.string.NotifyExit), getString( s.getTransportName() )),
                                getString(R.string.app_name),
                                getString(R.string.NotifyNextStop), true, true, Toast.LENGTH_LONG);
                    }
                }
            //}
            if ( dist < 2 ) {
                //! This is the next to last stop and probably were getting away from it
                if (s.getTransportName() != R.string.tr_walk 			// currentState == STATE_IM_INBUS
                        && s.getTransportName() != R.string.tr_metro
                        && s.getTransportName() != R.string.tr_train) {

                    //showDialog(getString(R.string.appName), getString(R.string.bsDialogNextStop));
                    createNotification(String.format(getString(R.string.NotifyExit), getString( s.getTransportName() )),
                            getString(R.string.app_name),
                            getString(R.string.busNotifyExitText), true, true, Toast.LENGTH_LONG);
                }
            }
        }


        if ((minDistIdx == prevMinDistIdx && prevMinDist < minDist) // Probably were leaving the last route leg
                ||
                (minDistIdx < prevMinDistIdx)) // Probably we are moving to the next step
        {
            Log.i(TAG, "Probably we are leaving the last route leg: "+minDistIdx+" "+minDist);
            nextRouteStep();

        }
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
        editor.remove("routeStarted");
        editor.remove("isRouteSet");
        editor.commit();
        locationManager.removeUpdates(this);
        currentState = STATE_DO_NOTHING;
        route = null;
        prevRouteString = "";
    }

    public void startRoute() {
        Log.i(TAG, "SERVICE deleteRoute");
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

                List<HashMap<String, String>> recs = api.getReverseGeocode(new LatLng(l.getLatitude(), l.getLongitude()));
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
