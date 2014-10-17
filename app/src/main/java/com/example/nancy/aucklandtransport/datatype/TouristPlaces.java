package com.example.nancy.aucklandtransport.datatype;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Nancy on 10/16/14.
 */
public class TouristPlaces {
    private static ArrayList<Place> placesArray;

    public void add(Place place) {
        if(placesArray == null)
            placesArray = new ArrayList<Place>();
        placesArray.add(place);

        Log.d("Size" , "Add:" + placesArray.size());
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

        if(placesArray != null)
        Log.d("Size" , "Delete:" + placesArray.size());
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
}
