package com.example.nancy.aucklandtransport;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Nancy on 10/5/14.
 */
public class RealTimeJSONParser {

    public String parse(JSONObject jObject) {
        String actualArrivalTime ="";
        try {
            actualArrivalTime = jObject.getString("ActualArrivalTime");

            if(actualArrivalTime.equals("") || actualArrivalTime  == null)
                return actualArrivalTime;
            else {
                try {
                    SimpleDateFormat toFullDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date fullDate = toFullDate.parse(actualArrivalTime);
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    String shortTimeStr = sdf.format(fullDate);
                    return shortTimeStr;
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
        return actualArrivalTime;
    }
}
