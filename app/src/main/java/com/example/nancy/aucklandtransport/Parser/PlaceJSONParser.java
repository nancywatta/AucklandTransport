package com.example.nancy.aucklandtransport.Parser;

import android.util.Log;

import com.example.nancy.aucklandtransport.datatype.Attribution;
import com.example.nancy.aucklandtransport.datatype.Photo;
import com.example.nancy.aucklandtransport.datatype.Place;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nancy on 7/9/14.
 */
public class PlaceJSONParser {

    /** Receives a JSONObject and returns a list */
    public List<HashMap<String,String>> parse(JSONObject jObject){

        JSONArray jPlaces = null;
        try {
            /** Retrieves all the elements in the 'places' array */
            jPlaces = jObject.getJSONArray("predictions");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        /** Invoking getPlaces with the array of json object
         * where each json object represent a place
         */
        return getPlaces(jPlaces);
    }

    private List<HashMap<String, String>> getPlaces(JSONArray jPlaces){
        int placesCount = jPlaces.length();
        List<HashMap<String, String>> placesList = new ArrayList<HashMap<String,String>>();
        HashMap<String, String> place = null;

        /** Taking each place, parses and adds to list object */
        for(int i=0; i<placesCount;i++){
            try {
                /** Call getPlace with place JSON object to parse the place */
                place = getPlace((JSONObject)jPlaces.get(i));
                placesList.add(place);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return placesList;
    }

    /** Parsing the Place JSON object */
    private HashMap<String, String> getPlace(JSONObject jPlace){

        HashMap<String, String> place = new HashMap<String, String>();

        String id="";
        String reference="";
        String description="";

        try {

            description = jPlace.getString("description");
            id = jPlace.getString("id");
            reference = jPlace.getString("reference");

            place.put("description", description);
            place.put("_id",id);
            place.put("reference",reference);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return place;
    }

    /** Receives a JSONObject and returns a list */
    public Place[] placeParse(JSONObject jObject){

        JSONArray jPlaces = null;
        try {
            /** Retrieves all the elements in the 'places' array */
            jPlaces = jObject.getJSONArray("results");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        /** Invoking getPlaces with the array of json object
         * where each json object represent a place
         */
        return getNearbyPlaces(jPlaces);
    }

    private Place[] getNearbyPlaces(JSONArray jPlaces){
        int placesCount = jPlaces.length();
        Place[] places = new Place[placesCount];

        /** Taking each place, parses and adds to list object */
        for(int i=0; i<placesCount;i++){
            try {
                /** Call getPlace with place JSON object to parse the place */
                places[i] = getNearByPlace((JSONObject)jPlaces.get(i));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return places;
    }

    /** Parsing the Place JSON object */
    private Place getNearByPlace(JSONObject jPlace){

        Place place = new Place();

        try {
            // Extracting Place name, if available
            if(!jPlace.isNull("name")){
                place.mPlaceName = jPlace.getString("name");
            }

            // Extracting Place Vicinity, if available
            if(!jPlace.isNull("vicinity")){
                place.mVicinity = jPlace.getString("vicinity");
            }

            if(!jPlace.isNull("photos")){
                JSONArray photos = jPlace.getJSONArray("photos");
                place.mPhotos = new Photo[photos.length()];
                for(int i=0;i<photos.length();i++){
                    place.mPhotos[i] = new Photo();
                    place.mPhotos[i].mWidth = ((JSONObject)photos.get(i)).getInt("width");
                    place.mPhotos[i].mHeight = ((JSONObject)photos.get(i)).getInt("height");
                    place.mPhotos[i].mPhotoReference = ((JSONObject)photos.get(i)).getString("photo_reference");
                    JSONArray attributions = ((JSONObject)photos.get(i)).getJSONArray("html_attributions");
                    place.mPhotos[i].mAttributions = new Attribution[attributions.length()];
                    for(int j=0;j<attributions.length();j++){
                        place.mPhotos[i].mAttributions[j] = new Attribution();
                        place.mPhotos[i].mAttributions[j].mHtmlAttribution = attributions.getString(j);
                    }
                }
            }

            place.mLat = jPlace.getJSONObject("geometry").getJSONObject("location").getString("lat");
            place.mLng = jPlace.getJSONObject("geometry").getJSONObject("location").getString("lng");

        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("EXCEPTION", e.toString());
        }
        return place;
    }
}
