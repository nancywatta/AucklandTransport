package com.example.nancy.aucklandtransport;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;

/**
 * Created by Nancy on 9/20/14.
 */
public class RouteEngine {

    private static final String TAG = RouteEngine.class.getSimpleName();

    private int routeState = Constant.INIT;
    private int nextStop = 0;
    private long searchInterval = 0;
    private boolean firstTime = true;
    private float prevDistance = 0;
    private boolean getRealTime = false;
    private RouteStep s = null;
    private int currentStep = 0;
    BackgroundService service;
    private boolean isNotRouteInSettings = false;
    Route route;
    private int offRouteCount = 0;
    GoogleAPI googleAPI = new GoogleAPI();
    Context context;

    public RouteEngine(BackgroundService service, Context context)
    {
        this.service = service;
        this.context = context;
    }

    public void routeEngine(Route mRoute, String mActivity, Location currentLocation) {
        if (mRoute == null || currentLocation == null) {
            Log.d(TAG, "isRouteSet : " + service.isRouteSet + " isNotRouteInSettings: " + isNotRouteInSettings);
            if (mRoute == null && !service.isRouteSet && !isNotRouteInSettings) {
                getRouteFromSettings();
                mRoute = route;
            }
            Log.i(TAG, "processRoute error: " + String.valueOf(mRoute) + " " + String.valueOf(currentLocation));
            return;
        }

        Location dest = new Location("");
        PathSegment pathSegment;
        float dist;

        switch (routeState) {
            case Constant.INIT:
                nextStop = 0;
                searchInterval = 0;
                firstTime = true;
                prevDistance = 0;
                getRealTime = false;
                s = mRoute.getSteps().get(currentStep);
                routeState = Constant.CHANGE_OVER;
                break;

            case Constant.CHANGE_OVER:
                if (currentStep >= mRoute.getSteps().size())
                    routeState = Constant.FINISHED;
                if (s.getTransportName() == R.string.tr_walk) {
                    //&& mActivity.compareTo("On Foot") == 0) {
                    Log.d(TAG, "I am Walking");
                    pathSegment = s.getPath().get(nextStop);
                    service.createNotification(pathSegment.getInstruction(),
                            context.getResources().getString(R.string.app_name),
                            pathSegment.getInstruction(), true, true, Toast.LENGTH_LONG);
                    routeState = Constant.WALKING;
                } else if(s.getTransportName() == R.string.tr_bus) {
                    //&& mActivity.compareTo("In Vehicle") == 0) {
                    Log.d(TAG, "I am in Bus");
                    routeState = Constant.BUS;
                }
                else if (s.isTransit()) {
                    //&& mActivity.compareTo("In Vehicle") == 0) {
                    Log.d(TAG, "I am in Bus");
                    routeState = Constant.TRANSIT;
                } else
                    break;

                currentStep++;
                if(currentStep < mRoute.getSteps().size() &&
                    mRoute.getSteps().get(currentStep).getTransportName() == R.string.tr_bus) {
                    
                }

                break;
            case Constant.PRE_CHANGE_OVER:
//                dest.setLatitude(s.getEndLoc().latitude);
//                dest.setLongitude(s.getEndLoc().longitude);
//                dist = currentLocation.distanceTo(dest); // Approximate distance in meters
                //if (dist < 2)
                    routeState = Constant.INIT;
                break;

            case Constant.WALKING:
                pathSegment = s.getPath().get(nextStop);
                long maxSeconds = pathSegment.getTravelTime().getSeconds();
                offRouteCount = 0;

                if (!pathSegment.isNotified) {
                    //searchInterval = 5;
//                    if (firstTime) {
//                        searchInterval = fixIntervalForWalk(maxSeconds);
//                        if (searchInterval > 5)
//                            getRealTime = true;
//                        firstTime = false;
//                    } else {
//                        if (getRealTime) {
//                            GoogleAPI googleAPI = new GoogleAPI();
//                            long seconds = googleAPI.getDuration(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
//                                    pathSegment.getEndLoc());
//                            searchInterval = fixIntervalForWalk(seconds);
//
//                            if (searchInterval == 5)
//                                getRealTime = false;
//
//                            if (route.getSteps().get(currentStep).isTransit()) {
//                                Calendar c = Calendar.getInstance();
//                                c.add(Calendar.SECOND, (int) seconds);
//                                long diff = (route.getSteps().get(currentStep).getDeparture().getSeconds() * 1000L) - c.getTimeInMillis();
//                                if (diff < 120) {
//                                    createNotification(getString(R.string.RunningLateText),
//                                            getString(R.string.app_name),
//                                            getString(R.string.RunningLateText), true, true, Toast.LENGTH_LONG);
//                                }
//                            }
//                        } else
//                            searchInterval = 5;
//                    }
                    dest.setLatitude(pathSegment.getEndLoc().latitude);
                    dest.setLongitude(pathSegment.getEndLoc().longitude);
                    dist = currentLocation.distanceTo(dest); // Approximate distance in meters
                    Log.d(TAG, "distance : " + dist);
                    if (prevDistance != 0) {
                        long nextdist = 0;
                        if (nextStop < s.getPath().size() - 1)
                            nextdist = s.getPath().get(nextStop + 1).getDistance();
                        if (prevDistance + nextdist + 50 < dist + nextdist) {
                            offRouteCount++;
                            routeState = Constant.OFF_ROUTE;
                            Log.d(TAG, "Off Route : " + nextStop + " : prev " +
                                    prevDistance + " : dist " + dist);
                            Log.i(TAG, "Off Route LOCATION!!!!! " + currentLocation.getLatitude() + " " + currentLocation.getLongitude());
                        } else
                            prevDistance = dist;
                    } else
                        prevDistance = dist;
                    if (dist < 20) {
                        nextStop++;
                        if (nextStop < s.getPath().size())
                            service.createNotification(s.getPath().get(nextStop).getInstruction(),
                                    context.getResources().getString(R.string.app_name),
                                    s.getPath().get(nextStop).getInstruction(), true, true, Toast.LENGTH_LONG);
                        pathSegment.isNotified = true;
                        prevDistance = 0;
                        if (nextStop >= s.getPath().size())
                            routeState = Constant.PRE_CHANGE_OVER;
                        else
                            routeState = Constant.WALKING;
                        //searchInterval = 0;
                        firstTime = true;
                    }
                }

                break;

            case Constant.TRANSIT:
                if (searchInterval <= 0) {
                    searchInterval = (s.getDuration().getSeconds() / 2);
                    Log.d(TAG, "searchInterval zero " + searchInterval);
                    Calendar c = Calendar.getInstance();
                    //long diff = (s.getArrival().getSeconds() * 1000L) - c.getTimeInMillis();
                    long diff = (s.getArrival().getSeconds() * 1000L) - (s.getDeparture().getSeconds() * 1000L);
                    String str = String.format(context.getResources().getString(R.string.ArrivalText), Math.round(diff / (1000 * 60)));
                    service.createNotification(str,
                            context.getResources().getString(R.string.app_name), str, true,
                            true, Toast.LENGTH_LONG);

                    dest.setLatitude(s.getEndLoc().latitude);
                    dest.setLongitude(s.getEndLoc().longitude);
                    dist = currentLocation.distanceTo(dest);

                    if (dist < 20 || searchInterval <= 120)
                        routeState = Constant.PRE_CHANGE_OVER;
                } else if (searchInterval > 0) {
                    Log.d(TAG, "searchInterval not zero " + searchInterval);
                    searchInterval--;
                }
                break;

            case Constant.STOPPED:
                break;

            case Constant.OFF_ROUTE:
                pathSegment = s.getPath().get(nextStop);
                dest.setLatitude(pathSegment.getEndLoc().latitude);
                dest.setLongitude(pathSegment.getEndLoc().longitude);
                dist = currentLocation.distanceTo(dest); // Approximate distance in meters

                if (prevDistance != 0) {
                    long nextdist = 0;
                    if (nextStop < s.getPath().size() - 1)
                        nextdist = s.getPath().get(nextStop + 1).getDistance();
                    if (prevDistance + nextdist + 50 < dist + nextdist) {
                        offRouteCount++;
                    } else
                        routeState = Constant.WALKING;

                    if (offRouteCount > 5)
                        routeState = Constant.REROUTE;
                }

                break;

            case Constant.REROUTE:
                offRouteCount = 0;
                service.createNotification(context.getResources().getString(R.string.OffRouteText),
                        context.getResources().getString(R.string.app_name),
                        context.getResources().getString(R.string.OffRouteText), true, true, Toast.LENGTH_LONG);

                RouteStep reRouteStep = googleAPI.getRoute(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    s.getEndLoc());
                Log.d(TAG, "rerouteStep: " + reRouteStep);

                currentStep--;
                mRoute.getSteps().set(currentStep, reRouteStep);
                routeState = Constant.INIT;
                break;

            case Constant.FINISHED:
                service.routeDone();
                routeState = Constant.DEFAULT;
                break;

            default:
                break;

        }
    }

    public void getRouteFromSettings() {
        Log.i(TAG, "SERVICE getRouteFromSettings");
        SharedPreferences settings =
                context.getSharedPreferences(context.getResources().getString(R.string.PREFS_NAME), 0);
        try {
            String routeString = settings.getString("route", "");

            if (!routeString.equals("")) route = new Route(routeString);

            service.prevRouteString = routeString;
            isNotRouteInSettings = false;
            return;
        } catch (Exception e) {
            Log.i(TAG, "SERVICE not getRouteFromSettings");
        }
        isNotRouteInSettings = true;
    }

    long fixIntervalForWalk(long maxTime) {
        long interval;
        if (maxTime > 59) {
            interval = 25;
        } else {
            interval = 5;
        }
        return interval;
    }

    long fixIntervalForTransit(int maxTime) {

        return (int) maxTime / 2;

//        if(maxTime >200)
//        {
//            interval = 140;
//        }
//        else if(maxTime>160)
//        {
//            interval = 100;
//        }
//        else if(maxTime>120)
//        {
//            interval = 60;
//        }
//        else if(maxTime>80)
//        {
//            interval = 20;
//        }
//        else if (maxTime > 59) {
//            interval = 25;
//        } else {
//            interval = 5;
//        }
//        return interval;
    }

}
