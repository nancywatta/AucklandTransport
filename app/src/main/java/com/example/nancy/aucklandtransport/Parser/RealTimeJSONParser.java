package com.example.nancy.aucklandtransport.Parser;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * RealTimeJSONParser class receives a JSONObject
 * and parses the JSON String according to Output parameters sent
 * by our private application server to store in HashMap.
 *
 * Created by Nancy on 10/5/14.
 */
public class RealTimeJSONParser {

    /** Receives a JSONObject and returns a hashMap of scheduled and real time data */
    public HashMap<String, Date> parse(JSONObject jObject) {
        String actualArrivalTime ="", expectedArrivalTime = "";
        HashMap<String, Date> dates = new HashMap<String, Date>();

        Date arrivalTime = null;
        Date expectedTime = null;
        try {
            actualArrivalTime = jObject.getString("ActualArrivalTime");

            /**
             * Getting actualArrivalTime from the json data. This is the scheduled
             * time of Bus Arrival.
             */
            if(actualArrivalTime.equals("") || actualArrivalTime  == null) {
                dates.put("ActualArrivalTime", arrivalTime);
            }
            else {
                try {
                    SimpleDateFormat toFullDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date fullDate = toFullDate.parse(actualArrivalTime);
                    dates.put("ActualArrivalTime", fullDate);
                } catch (android.net.ParseException e) {
                    Log.d("ParseException: " , e.getMessage());
                    e.printStackTrace();
                }
            }

            /**
             * Getting expectedArrivalTime from the json data. This is the real
             * time of Bus Arrival.
             */
            expectedArrivalTime = jObject.getString("ExpectedArrivalTime");
            if(expectedArrivalTime.equals("") || expectedArrivalTime  == null) {
                dates.put("ExpectedArrivalTime", expectedTime);
            }
            else {
                try {
                    SimpleDateFormat toFullDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date fullDate = toFullDate.parse(expectedArrivalTime);
                    dates.put("ExpectedArrivalTime", fullDate);
                } catch (android.net.ParseException e) {
                    Log.d("ParseException: " , e.getMessage());
                    e.printStackTrace();
                }
            }
        }catch (JSONException e) {
            Log.d("JSONException: " , e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.d("Exception: " , e.getMessage());
            e.printStackTrace();
        }
        return dates;
    }
}
