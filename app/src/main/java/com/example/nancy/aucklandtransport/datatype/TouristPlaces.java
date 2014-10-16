package com.example.nancy.aucklandtransport.datatype;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Nancy on 10/16/14.
 */
public class TouristPlaces {
    private static ArrayList<LatLng> placesArray;

    public void add(String lat, String lng) {
        if(placesArray == null)
            placesArray = new ArrayList<LatLng>();
        placesArray.add(new LatLng(Double.parseDouble(lat),
                Double.parseDouble(lng)));
    }

    public void delete(String lat, String lng) {
        if(placesArray == null)
            return;

        placesArray.remove(new LatLng(Double.parseDouble(lat),
                Double.parseDouble(lng)));
    }

    public static class PlaceItem {
        PlaceItem(int idx, String lat, String lng) {
            latitude = lat; longitude = lng; index = idx;
        }

        public String toJSON() {
            return "{ index: \""+index+"\", latitude: \""+latitude+"\", longitude: \""+longitude+"\"  }";
        }

        public String latitude;
        public String longitude;
        public int index;
    }
}
