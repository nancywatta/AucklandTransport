package com.example.nancy.aucklandtransport;

import android.content.Context;
import android.content.Intent;
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
    private long remainingTime = 0;
    //private boolean firstTime = true;
    private float prevDistance = 0;
    //private boolean getRealTime = false;
    private RouteStep s = null;
    private int currentStep = 0;
    private BackgroundService service;
    private boolean isNotRouteInSettings = false;
    private Route route;
    private int offRouteCount = 0;
    private GoogleAPI googleAPI = new GoogleAPI();
    private static Context context;
    private AucklandPublicTransportAPI aptApi = null;

    public RouteEngine(BackgroundService service, Context context) {
        this.service = service;
        this.context = context;
        aptApi = new AucklandPublicTransportAPI(context);
    }

    public void setRouteState(int state) {
        this.routeState = state;
    }

    public void resetStep() {
        this.currentStep = 0;
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
                offRouteCount = 0;
                searchInterval = 0;
                //firstTime = true;
                prevDistance = 0;
                //getRealTime = false;
                s = mRoute.getSteps().get(currentStep);
                remainingTime = 0;
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
                } else if (s.getTransportName() == R.string.tr_bus) {
                    //&& mActivity.compareTo("In Vehicle") == 0) {
                    Log.d(TAG, "I am in Bus");
                    remainingTime = s.getDuration().getSeconds();
                    routeState = Constant.BUS;
                } else if (s.isTransit()) {
                    //&& mActivity.compareTo("In Vehicle") == 0) {
                    Log.d(TAG, "I am in TRAIN/FERRY");
                    remainingTime = s.getDuration().getSeconds();
                    routeState = Constant.TRANSIT;
                } else
                    break;

                stopServer(mRoute, currentStep - 1);

                currentStep++;

                startServer(mRoute, currentStep);

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
                offRouteCount = 0;

                if (!pathSegment.isNotified) {
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

                        checkChangeRoute(mRoute, currentStep);
                    }
                }

                break;

            case Constant.TRANSIT:
                if (searchInterval <= 0) {
                    if (remainingTime > 60) {
                        searchInterval = (remainingTime / 2);
                        remainingTime = remainingTime - searchInterval;

                        Calendar c = Calendar.getInstance();
                        //long diff = (s.getArrival().getSeconds() * 1000L) - c.getTimeInMillis();
                        long diff = (s.getArrival().getSeconds() * 1000L) - (s.getDeparture().getSeconds() * 1000L);
                        String str = String.format(context.getResources().getString(R.string.ArrivalText), Math.round(diff / (1000 * 60)));
                        service.createNotification(str,
                                context.getResources().getString(R.string.app_name), str, true,
                                true, Toast.LENGTH_LONG);
                    }
                    Log.d(TAG, "searchInterval zero " + searchInterval);


                    dest.setLatitude(s.getEndLoc().latitude);
                    dest.setLongitude(s.getEndLoc().longitude);
                    dist = currentLocation.distanceTo(dest);

                    // TODO remove search interval
                    if (dist < 40 || searchInterval <= 60) {
                        checkChangeRoute(mRoute, currentStep);
                        routeState = Constant.PRE_CHANGE_OVER;
                    }
                } else if (searchInterval > 0) {
                    searchInterval--;
                }
                break;

            case Constant.BUS:
                if (searchInterval <= 0) {

                    if (remainingTime > 0) {
                        searchInterval = (remainingTime / 2);
                        remainingTime = remainingTime - searchInterval;
                    }
                    Log.d(TAG, "BUS searchInterval zero " + searchInterval);

                    dest.setLatitude(s.getEndLoc().latitude);
                    dest.setLongitude(s.getEndLoc().longitude);
                    dist = currentLocation.distanceTo(dest);

                    Log.d(TAG, " currentStep: " + currentStep +
                            " lat: " + s.getEndLoc().latitude + " dist: " + dist);

                    if (dist < 40) {
                        String str =
                                String.format(context.getResources().getString(R.string.busExitText), dist);
                        service.createNotification(str,
                                context.getResources().getString(R.string.app_name), str, true,
                                true, Toast.LENGTH_LONG);
                        checkChangeRoute(mRoute, currentStep);
                        routeState = Constant.PRE_CHANGE_OVER;
                    }
                } else if (searchInterval > 0) {
                    Log.d(TAG, "BUS searchInterval not zero " + searchInterval);
                    searchInterval--;

                    // TODO remove below code
                    dest.setLatitude(s.getEndLoc().latitude);
                    dest.setLongitude(s.getEndLoc().longitude);
                    dist = currentLocation.distanceTo(dest);
                    Log.d(TAG, " distance BUS: " + dist);
                    if (dist < 40) {
                        String str =
                                String.format(context.getResources().getString(R.string.busExitText), dist);
                        service.createNotification(str,
                                context.getResources().getString(R.string.app_name), str, true,
                                true, Toast.LENGTH_LONG);
                        checkChangeRoute(mRoute, currentStep);
                        routeState = Constant.PRE_CHANGE_OVER;
                    }
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

                if (reRouteStep != null) {
                    currentStep--;
                    mRoute.getSteps().set(currentStep, reRouteStep);
                    routeState = Constant.INIT;
                } else
                    routeState = Constant.WALKING;

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

    private void startServer(Route mRoute, int nextStep) {
        if (nextStep < mRoute.getSteps().size() &&
                mRoute.getSteps().get(nextStep).getTransportName() == R.string.tr_bus) {
            RouteStep busStep = mRoute.getSteps().get(nextStep);
            aptApi.startServerTracking(busStep.getStartLoc(), busStep.getShortName());
        }
    }

    private void stopServer(Route mRoute, int previousStep) {
        if (previousStep >= 0 &&
                mRoute.getSteps().get(previousStep).getTransportName() == R.string.tr_bus) {
            RouteStep busStep = mRoute.getSteps().get(previousStep);
            aptApi.stopServerTracking(busStep.getStartLoc(), busStep.getShortName());
        }
    }

    public void checkChangeRoute(Route mRoute, int nextStep) {
        RouteStep nextRoute = mRoute.getSteps().get(nextStep);

        if (nextRoute.isTransit()) {

            Calendar c = Calendar.getInstance();
            long diff =
                    mRoute.getSteps().get(nextStep).getDeparture().getSeconds()
                            - (c.getTimeInMillis() / 1000L);

            if (diff < 120) {
                Intent myIntent = new Intent(context, PathTracker.class);
                String message = "";
                if (diff < 0)
                    message = "Your " + context.getResources().getString(nextRoute.getTransportName())
                            + " " + nextRoute.getShortName()
                            + " is about to leave. Will you be able to catch?";
                else
                    message = "Will you be able to catch " +
                            context.getResources().getString(nextRoute.getTransportName())
                            + " " + nextRoute.getShortName()
                            + " in next " + Math.round(diff / 60) + " minute?";
                myIntent.putExtra("MESSAGE", message);
                myIntent.putExtra("TO_ADDRESS", mRoute.getEndAddress());
                myIntent.putExtra("TO_COORDS", mRoute.getEndLocation().latitude +
                        "," + mRoute.getEndLocation().longitude);
                myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(myIntent);
//                service.createNotification(context.getResources().getString(R.string.RunningLateText),
//                        context.getResources().getString(R.string.app_name),
//                        context.getResources().getString(R.string.RunningLateText),
//                        true, true, Toast.LENGTH_LONG);
            }
        }
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
