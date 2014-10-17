package com.example.nancy.aucklandtransport.datatype;

import android.util.Log;

import com.example.nancy.aucklandtransport.Route;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Nancy on 10/16/14.
 */
public class TouristPlaces {

    private static final String TAG = TouristPlaces.class.getSimpleName();
    private static ArrayList<Place> placesArray;
    private static ArrayList<Route> routesArray;
    public static String startAddress;
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
