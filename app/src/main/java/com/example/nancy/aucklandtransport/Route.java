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
    protected TravelTime departure;
    protected TravelTime arrival;
    protected String endAddress;
    protected String startAddress;
    protected String jsonString;
    LatLng startLocation;
    LatLng endLocation;
    private TravelTime duration;
    private String startTouristPlace;
    private String endTouristPlace;

    public Route(String distance, String duration, long durSeconds, String startAddress, String endAddress,
                 String depTime, long depSeconds, String arrTime, long arrSeconds, LatLng startLocation, LatLng endLocation, String jsonString) {
        this.distance = distance;
        this.duration = new TravelTime(duration, durSeconds);
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.departure = new TravelTime(depTime,depSeconds);
        this.arrival = new TravelTime(arrTime,arrSeconds);
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.jsonString = jsonString;
        steps = new ArrayList<RouteStep>();
    }

    public Route(String distance, String duration, long durSeconds, String startAddress, String endAddress,
                 LatLng startLocation, LatLng endLocation, String jsonString) {
        this.distance = distance;
        this.duration = new TravelTime(duration, durSeconds);
        this.startAddress = startAddress;
        this.endAddress = endAddress;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.jsonString = jsonString;
    }

    public boolean showrealTime() {
        RouteStep firstStep = steps.get(0);
        if (firstStep.getTransportName() == R.string.tr_bus) {
            return true;
        } else if (!firstStep.isTransit() && steps.size() > 1) {
            RouteStep secondStep = steps.get(1);
            if (secondStep.getTransportName() == R.string.tr_bus)
                return true;
        }

        return false;
    }

    public int getTotalTransfers(){
        int count = 0;
        for(RouteStep routeStep: steps) {
            if(routeStep.isTransit())
                count++;
        }
        return count;
    }

    public Route(String json) throws JSONException {
        this(new JSONObject(json));
    }

    public Route(JSONObject obj) throws JSONException {
        try {

            JSONObject jDistance = null;
            JSONObject jDuration = null;
            JSONObject jDepartureTime = null;
            JSONObject jArrivalTime = null;
            JSONArray jSteps = null;
            JSONObject transit = null;
            JSONArray Steps = null;
            String startAddr = "";
            String endAddr = "";
            String type = "";
            String departTime = "";
            String arrivalTime = "";
            String shortName = "";
            String name = "";
            String travelMode = "";
            String departureStop = "", arrivalStop = "", instruc = "";
            Double startLat, startLng, endLat, endLng;
            long arrSec, depSec;

            jDistance = obj.getJSONObject("distance");

            /** Getting duration from the json data */
            jDuration = obj.getJSONObject("duration");

            try {
                jDepartureTime = obj.getJSONObject("departure_time");
                jArrivalTime = obj.getJSONObject("arrival_time");
                this.departure = new TravelTime(jDepartureTime.getString("text"), jDepartureTime.getLong("value"));
                this.arrival = new TravelTime(jArrivalTime.getString("text"), jArrivalTime.getLong("value"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            this.distance = jDistance.getString("text");
            this.duration = new TravelTime(jDuration.getString("text"), jDuration.getLong("value"));
            this.startAddress = obj.getString("start_address");
            this.endAddress = obj.getString("end_address");

            startLat = obj.getJSONObject("start_location").getDouble("lat");
            startLng = obj.getJSONObject("start_location").getDouble("lng");
            this.startLocation = new LatLng(startLat, startLng);
            endLat = obj.getJSONObject("end_location").getDouble("lat");
            endLng = obj.getJSONObject("end_location").getDouble("lng");
            this.endLocation = new LatLng(endLat, endLng);

            jSteps = obj.getJSONArray("steps");

            this.steps = new ArrayList<RouteStep>();

            /** Traversing all steps */
            for (int k = 0; k < jSteps.length(); k++) {
                transit = null;
                type = "";
                name = "";
                shortName = "";
                arrSec = 0;
                depSec = 0;
                endAddr = "";
                startAddr = "";
                departTime = "";
                arrivalTime = "";
                travelMode = "";
                departureStop = "";
                arrivalStop = "";
                JSONObject step = jSteps.getJSONObject(k);

                jDistance = step.getJSONObject("distance");
                jDuration = step.getJSONObject("duration");

                try {
                    transit = step.getJSONObject("transit_details");
                } catch (Exception e) {
                }

                travelMode = step.getString("travel_mode");

                if (transit != null) {
                    startAddr = transit.getJSONObject("departure_stop").getString("name");
                    endAddr = transit.getJSONObject("arrival_stop").getString("name");
                    departTime = transit.getJSONObject("departure_time").getString("text");
                    arrivalTime = transit.getJSONObject("arrival_time").getString("text");
                    type = transit.getJSONObject("line").getJSONObject("vehicle").getString("type");
                    name = transit.getJSONObject("line").getString("name");
                    shortName = transit.getJSONObject("line").getString("short_name");
                    startLat = transit.getJSONObject("departure_stop").getJSONObject("location").getDouble("lat");
                    startLng = transit.getJSONObject("departure_stop").getJSONObject("location").getDouble("lng");
                    endLat = transit.getJSONObject("arrival_stop").getJSONObject("location").getDouble("lat");
                    endLng = transit.getJSONObject("arrival_stop").getJSONObject("location").getDouble("lng");
                    arrSec = transit.getJSONObject("arrival_time").getLong("value");
                    depSec = transit.getJSONObject("departure_time").getLong("value");
                    departureStop = transit.getJSONObject("departure_stop").getString("name");
                    arrivalStop = transit.getJSONObject("arrival_stop").getString("name");
                } else {
                    startLat = step.getJSONObject("start_location").getDouble("lat");
                    startLng = step.getJSONObject("start_location").getDouble("lng");
                    endLat = step.getJSONObject("end_location").getDouble("lat");
                    endLng = step.getJSONObject("end_location").getDouble("lng");
                }
                Log.d(" Reached ", " k: " + k + " " + "routeStep");

                String polyline = "";
                polyline = (String) ((JSONObject) step.get("polyline")).get("points");
                List<LatLng> list = new ArrayList<LatLng>();
                list = PolyUtil.decode(polyline);

                RouteStep routeStep = new RouteStep(jDistance.getString("text"), jDistance.getLong("value"),
                        jDuration.getString("text"), jDuration.getLong("value"),
                        step.getString("html_instructions"), startAddr, endAddr, type, departTime, depSec,
                        arrivalTime, arrSec, name,
                        shortName, list, new LatLng(startLat, startLng), new LatLng(endLat, endLng), travelMode,
                        departureStop, arrivalStop, step.toString());

                if (transit == null) {
                    Steps = ((JSONObject) jSteps.get(k)).getJSONArray("steps");

//                for (LatLng subList: list) {
//                    Log.d(" Mock Locations : " , subList.latitude + " " + subList.longitude);
//                }

                    for (int i = 0; i < Steps.length(); i++) {
                        JSONObject path = Steps.getJSONObject(i);
                        instruc = "";
                        startLat = path.getJSONObject("start_location").getDouble("lat");
                        startLng = path.getJSONObject("start_location").getDouble("lng");
                        endLat = path.getJSONObject("end_location").getDouble("lat");
                        endLng = path.getJSONObject("end_location").getDouble("lng");
                        departTime = path.getJSONObject("duration").getString("text");
                        depSec = path.getJSONObject("duration").getLong("value");
                        try {
                            instruc = path.getString("html_instructions");
                        } catch (Exception e) {
                            instruc = step.getString("html_instructions");
                        }
                        PathSegment p = new PathSegment(new LatLng(startLat, startLng), new LatLng(endLat, endLng),
                                departTime, depSec, instruc,
                                path.getJSONObject("distance").getLong("value"));
                        routeStep.add(p);
                    }
                }

                steps.add(routeStep);
            }
        } catch (Exception e) {
            Log.d("Exception: Error", " Check");
        }

        this.jsonString = obj.toString();
    }

    public ArrayList<RouteStep> getSteps () {
        return steps;
    }

    public String getDistance() { return distance; }

    public TravelTime getDuration() { return  duration; }

    public TravelTime getDeparture() { return departure;}

    public  TravelTime getArrival() { return arrival; }

    public String getJsonString() { return jsonString; }

    public LatLng getStartLocation() { return startLocation; }

    public LatLng getEndLocation() { return endLocation; }

    public String getEndAddress() { return endAddress; }

    public String getStartAddress() { return startAddress; }

    public void setStartTouristPlace(String value) { this.startTouristPlace = value;}

    public void setEndTouristPlace(String value) { this.endTouristPlace = value;}

    public String getStartTouristPlace() { return  startTouristPlace; }

    public String getEndTouristPlace() { return endTouristPlace; }
}
