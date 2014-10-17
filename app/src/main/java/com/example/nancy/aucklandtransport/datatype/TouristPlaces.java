package com.example.nancy.aucklandtransport.datatype;

import android.util.Log;

import com.example.nancy.aucklandtransport.Route;

import java.util.ArrayList;
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

    public ArrayList<Route> getRoutesArray() { return routesArray; }

    public void addRoute(ArrayList<Route> array) {
        if(array == null || array.size() < 1) {
            Log.d(TAG, " Input array Zero");
            return;
        }

        if(routesArray == null)
            routesArray = new ArrayList<Route>();

        routesArray.addAll(array);
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
        if(routesArray.size() > 1) {
            routesArray.remove(routesArray.size() - 1);
        }
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
}
