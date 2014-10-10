package com.example.nancy.aucklandtransport.Parser;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Nancy on 10/5/14.
 */
public class RealTimeJSONParser {

    public HashMap<String, Date> parse(JSONObject jObject) {
        String actualArrivalTime ="", expectedArrivalTime = "";
        HashMap<String, Date> dates = new HashMap<String, Date>();

        Date arrivalTime = null;
        Date expectedTime = null;
        try {
            actualArrivalTime = jObject.getString("ActualArrivalTime");

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
