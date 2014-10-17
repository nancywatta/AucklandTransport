package com.example.nancy.aucklandtransport.Parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * ElevationJSONParser class receives a JSONObject
 * and parses the JSON String according to Output parameters of
 * Google Elevation API to store in custom data Objects.
 *
 * Created by Nancy on 8/1/14.
 */
public class ElevationJSONParser {
    public ArrayList<Double> parse(JSONObject jObject) {
        ArrayList<Double> elevations = new ArrayList<Double>() ;
        JSONArray jResults = null;
        try{
            /** Traversing all results */
            jResults = jObject.getJSONArray("results");
            for(int i=0;i<jResults.length();i++) {
                JSONObject jResult = jResults.getJSONObject(i);

                /** Getting elevation from the json data */
                elevations.add(jResult.getDouble("elevation"));
            }

        }catch ( JSONException e) {
            e.printStackTrace();
        }catch (Exception e){
        }

        return elevations;
    }
}
