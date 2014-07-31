package com.example.nancy.aucklandtransport;

import android.util.Log;

import com.example.nancy.aucklandtransport.datatype.TravelTime;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nancy on 7/13/14.
 */
public class Route {
    private ArrayList<RouteStep> steps;

    protected String distance;
    protected String duration;
    protected TravelTime departure;
    protected TravelTime arrival;
    protected String endAddress;
    protected String startAddress;
    protected String jsonString;
    LatLng startLocation;
    LatLng endLocation;

    public Route(String distance, String duration, String startAddress, String endAddress,
                 String depTime, long depSeconds, String arrTime, long arrSeconds, LatLng startLocation, LatLng endLocation, String jsonString) {
        this.distance = distance;
        this.duration = duration;
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.departure = new TravelTime(depTime,depSeconds);
        this.arrival = new TravelTime(arrTime,arrSeconds);
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.jsonString = jsonString;
        steps = new ArrayList<RouteStep>();
    }

    public Route(String json) throws JSONException {
        this(new JSONObject(json));
    }

    public Route(JSONObject obj) throws JSONException {

        JSONObject jDistance = null;
        JSONObject jDuration = null;
        JSONObject jDepartureTime = null;
        JSONObject jArrivalTime = null;
        JSONArray jSteps = null;
        JSONObject transit = null;
        //JSONArray Steps = null;
        String startAddr = "";
        String endAddr ="";
        String type = "";
        String departTime = "";
        String arrivalTime = "";
        String shortName = ""; String name="";
        Double startLat, startLng, endLat, endLng;
        long arrSec, depSec;

        jDistance = obj.getJSONObject("distance");

        /** Getting duration from the json data */
        jDuration = obj.getJSONObject("duration");

        jDepartureTime = obj.getJSONObject("departure_time");

        jArrivalTime = obj.getJSONObject("arrival_time");

        this.distance = jDistance.getString("text");
        this.duration = jDuration.getString("text");
        this.startAddress = obj.getString("start_address");
        this.endAddress = obj.getString("end_address");
        this.departure = new TravelTime(jDepartureTime.getString("text"), jDepartureTime.getLong("value"));
        this.arrival = new TravelTime(jArrivalTime.getString("text"), jArrivalTime.getLong("value"));
        startLat = obj.getJSONObject("start_location").getDouble("lat");
        startLng = obj.getJSONObject("start_location").getDouble("lng");
        this.startLocation = new LatLng(startLat,startLng);
        endLat = obj.getJSONObject("end_location").getDouble("lat");
        endLng = obj.getJSONObject("end_location").getDouble("lng");
        this.endLocation = new LatLng(endLat,endLng);

        Log.d("Debug: ", "RouteStep: " + this.distance + " " + this.duration + " "
        + this.startAddress + " " + this.endAddress );

        jSteps = obj.getJSONArray("steps");

        Log.d("Length: ", "JSteps " + jSteps.length());
        this.steps = new ArrayList<RouteStep>();

        /** Traversing all steps */
        for(int k=0;k<jSteps.length();k++){
            transit = null; type = ""; name = ""; shortName = ""; arrSec = 0; depSec = 0;
            endAddr = ""; startAddr = ""; departTime = ""; arrivalTime = "";
            JSONObject step = jSteps.getJSONObject(k);

            jDistance = step.getJSONObject("distance");
            jDuration = step.getJSONObject("duration");

            try {
                transit = step.getJSONObject("transit_details");
            }catch (Exception e) {}

            Log.d(" Reached ", " k: " + k + " " + "transit");
            if(transit!=null) {
                startAddr = transit.getJSONObject("departure_stop").getString("name");
                endAddr = transit.getJSONObject("arrival_stop").getString("name");
                departTime = transit.getJSONObject("departure_time").getString("text");
                arrivalTime = transit.getJSONObject("arrival_time").getString("text");
                type = transit.getJSONObject("line").getJSONObject("vehicle").getString("type");
                name = transit.getJSONObject("line").getString("name");
                shortName = transit.getJSONObject("line").getString("short_name");
                startLat = transit.getJSONObject("arrival_stop").getJSONObject("location").getDouble("lat");
                startLng = transit.getJSONObject("arrival_stop").getJSONObject("location").getDouble("lng");
                endLat = transit.getJSONObject("departure_stop").getJSONObject("location").getDouble("lat");
                endLng = transit.getJSONObject("departure_stop").getJSONObject("location").getDouble("lng");
                arrSec = transit.getJSONObject("arrival_time").getLong("value");
                depSec = transit.getJSONObject("departure_time").getLong("value");
            }
            else {
                startLat = step.getJSONObject("start_location").getDouble("lat");
                startLng = step.getJSONObject("start_location").getDouble("lng");
                endLat = step.getJSONObject("end_location").getDouble("lat");
                endLng = step.getJSONObject("end_location").getDouble("lng");
            }
            Log.d(" Reached ", " k: " + k + " " + "routeStep");

            String polyline = "";
            polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
            List<LatLng> list = new ArrayList<LatLng>();
            list = PolyUtil.decode(polyline);

            RouteStep routeStep = new RouteStep(jDistance.getString("text"), jDuration.getString("text"),
                    step.getString("html_instructions"), startAddr, endAddr, type, departTime, depSec,
                    arrivalTime, arrSec, name,
                    shortName, list, new LatLng(startLat,startLng), new LatLng(endLat,endLng));

            /*Steps = step.getJSONArray("steps");

            for(int i=0;i<Steps.length();i++) {
                JSONObject path = Steps.getJSONObject(i);

            }*/

            steps.add(routeStep);
        }

        this.jsonString = obj.toString();
    }

    public ArrayList<RouteStep> getSteps () {
        return steps;
    }

    public String getDistance() { return distance; }

    public String getDuration() { return  duration; }

    public TravelTime getDeparture() { return departure;}

    public  TravelTime getArrival() { return arrival; }

    public String getJsonString() { return jsonString; }

    public LatLng getStartLocation() { return startLocation; }

    public LatLng getEndLocation() { return endLocation; }

}
