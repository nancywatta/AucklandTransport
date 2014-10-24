package com.example.nancy.aucklandtransport.datatype;

import android.util.Log;

import com.example.nancy.aucklandtransport.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * TouristPlaces is used to store all the places a user will visit
 * when using Explore City Option. The class will also store the
 * connecting routes of all these places.
 *
 * Created by Nancy on 10/16/14.
 */
public class TouristPlaces {

    private static final String TAG = TouristPlaces.class.getSimpleName();

    // array of places to be visited
    private static ArrayList<Place> placesArray;

    // array of route the user will follow
    private static ArrayList<Route> routesArray;

    // starting point of the user journey
    public static String startAddress;

    // ending point of the user journey
    public static String endAddress;

    // departure time of the journey, time in seconds since epoch
    private static long departureTime;

    // Links lat and lng with the formatted address
    HashMap<String, String> mHMReference = new HashMap<String, String>();

    public ArrayList<Route> getRoutesArray() { return routesArray; }

    public long getDepartureTime() { return departureTime; }

    public void setDepartureTime(long value) { departureTime = value; }

    public void setArray() {
        if(placesArray != null) placesArray.clear();
        if(routesArray != null) routesArray.clear();
        if(mHMReference != null) mHMReference.clear();
    }

    public void addRoute(String intermediateAdd, ArrayList<Route> array, long timeSinceEpoch) {
        if(array == null || array.size() < 1) {
            Log.d(TAG, " Input array Zero");
            return;
        }

        if(routesArray == null)
            routesArray = new ArrayList<Route>();

        if(timeSinceEpoch != 0) {
            Log.d(TAG, "Valid timeSinceEpoch");
            for (Route route : array) {
                if (route.getArrival().seconds == 0) {
                    Log.d(TAG, "Arrival Null");
                    route.getArrival().seconds = timeSinceEpoch +
                            route.getDuration().seconds;
                }
                if (route.getDeparture().seconds == 0) {
                    Log.d(TAG, "Arrival Null");
                    route.getDeparture().seconds = timeSinceEpoch;
                }
            }
        }

        routesArray.addAll(array);

        // add the reference of lat and lng with the formatted address
        if(!mHMReference.containsKey(intermediateAdd)) {
            Log.d(TAG, " Adding Reference");
            mHMReference.put(intermediateAdd, array.get(0).getEndAddress());
        }
    }

    public void deleteRoute(String intermediateAdd, ArrayList<Route> array) {
        if(routesArray == null)
            return;
            // only one place in between actual start Address and End Address
        else if(routesArray.size() < 3 ) {
            /**
            * clear the routes array since, the actual route between start and end address
            * is already available in the Shared Preferences
            */
            routesArray.clear();

            // clear the reference of lat and lng with the formatted address
            mHMReference.clear();
            return;
        } else if(array == null || array.size() < 1 ||
                routesArray == null || !mHMReference.containsKey(intermediateAdd)) {
            Log.d(TAG, " Input array Zero");
            return;
        }

        String deleteAddress = mHMReference.get(intermediateAdd);
        int index;
        for(index = 0; index < routesArray.size(); index++) {
            Route route = routesArray.get(0);
            if(route.getEndAddress().compareTo(deleteAddress) == 0)
                break;
        }

        Log.d(TAG , "deleteRoute: " + routesArray.size() + " deleteAddress: "
                + deleteAddress + " index: " + index);

        // remove route from start address to intermediateAdd
        routesArray.remove(index);
        // remove route from intermediateAdd to end address
        routesArray.remove(index);

        // add route from start address to end address disconnecting the intermediateAdd
        routesArray.addAll(index, array);

        // remove the reference of lat and lng with the formatted address
        mHMReference.remove(intermediateAdd);
    }

    public void deletePreviousRoute() {
        if(routesArray == null)
            return;

        /**
         *  if the routeArray contains Route from StartPoint to Point A and
         * Point A to End Point, then when adding place B as a new place
         *  that user would like to visit, remove route Point A to End Point,
         *  instead of this add Point A to Point B and Point B to End Point.
         */
        int size = routesArray.size();
        if(routesArray.size() > 1) {
            routesArray.remove(size - 1);
        }
    }

    public long getTimeOfStart() {
        if(routesArray == null)
            return 0;

        int size = routesArray.size();
        if(size > 1) {
            return routesArray.get(size - 1).getDeparture().getSeconds();
        }
        return 0;
    }

    public void add(Place place) {
        if(placesArray == null)
            placesArray = new ArrayList<Place>();
        placesArray.add(place);

//        Log.d(TAG , "Add:" + placesArray.size());
    }

    public void delete(Place place) {
        if(placesArray == null)
            return;

        for (Iterator<Place> it = placesArray.iterator(); it.hasNext(); ) {
            Place place1 = it.next();
            if (place1.compare(place)) {
                it.remove();
                Log.d(TAG , "Delete:");
            }
        }

//        if(placesArray != null)
//        Log.d("Size" , "Delete:" + placesArray.size());
    }

    public boolean checkExisting(Place place) {
        if(placesArray == null)
            return false;
        for(Place place1: placesArray) {
            if(place1.compare(place))
                return true;
        }
        return false;
    }

    public ArrayList<Place> getPlacesArray() { return placesArray; }

    /**
     * returns the previous place to be visited from the
     * given Place. If No previous place of Interest, then return the
     * starting point of Journey.
     */
    public String getPreviousAdd(Place place) {
        if(placesArray == null)
            return "";
        if(placesArray.size() <= 1)
            return startAddress;

        for(int index=0; index < placesArray.size(); index++) {
            Place place1 = placesArray.get(index);
            if (place1.compare(place)) {
                if(index == 0)
                    return startAddress;
                Place tempPlace = placesArray.get(index - 1);
                return tempPlace.mLat + "," + tempPlace.mLng;
            }
        }

        return "";
    }

    /**
     * returns the next place to be visited from the
     * given Place. If the given place is the last to be visited, then return the
     * ending point of Journey.
     */
    public String getNextAddress(Place place) {
        if(placesArray == null)
            return "";
        if(placesArray.size() <= 1)
            return endAddress;

        for(int index=0; index < placesArray.size(); index++) {
            Place place1 = placesArray.get(index);
            if (place1.compare(place) && index < placesArray.size() - 1) {
                Place tempPlace = placesArray.get(index + 1);
                return tempPlace.mLat + "," + tempPlace.mLng;
            }
        }

        return endAddress;
    }

    public long getDepartureTime(long duration) {
        if(routesArray == null)
            return 0;

        int lastIndex = routesArray.size() - 1;
        Route route = routesArray.get(lastIndex);
        if(route.getArrival().seconds != 0) {
            Log.d(TAG, " Not Zero ARRIVAL Seconds");
            return route.getArrival().getSeconds() + (duration*60);
        }
        return 0;
    }
}
