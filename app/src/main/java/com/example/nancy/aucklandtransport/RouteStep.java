package com.example.nancy.aucklandtransport;

import com.example.nancy.aucklandtransport.datatype.TravelDistance;
import com.example.nancy.aucklandtransport.datatype.TravelTime;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * RouteStep class represents the single step of the journey.
 * If the routeStep is of the type Walking, it will contain an array
 * of path segment demoting turn by turn instruction.
 *
 * Created by Nancy on 7/14/14.
 */
public class RouteStep {
    public RouteStep () {

    }

    public int iconId = -1;
    public String desc;

    public TravelDistance distance;
    public TravelTime duration;

    public String firstLoc = "";
    public String lastLoc = "";

    public String type = "";

    protected TravelTime departure;
    protected TravelTime arrival;
    private String vehicleName;
    private String shortName;

    List<LatLng> latlng;
    LatLng startLoc;
    LatLng endLoc;
    //private String polyLine;
    private String travelMode;
    private ArrayList<PathSegment> path = new ArrayList<PathSegment>();
    private String jsonString;
    private String departureStop;
    private String arrivalStop;


        public int getIconId() {
            if (iconId > -1) return iconId;

            switch (type) {
                case "BUS": case "INTERCITY_BUS": case "TROLLEYBUS":
                    iconId = R.drawable.bus; break;
                case "METRO_RAIL":
                    iconId = R.drawable.metro; break;
                case "FERRY":
                    iconId = R.drawable.boat; break;
                case "HEAVY_RAIL":case "RAIL":case "MONORAIL":case "COMMUTER_TRAIN":case "HIGH_SPEED_TRAIN":
                    iconId = R.drawable.train; break;

                case "OTHER": default:
                    iconId = R.drawable.man; break;
            }

            return iconId;
        }

        public int getTransportName() {
            switch (type) {
                case "BUS": case "INTERCITY_BUS": case "TROLLEYBUS":
                    return R.string.tr_bus;
                case "METRO_RAIL":
                    return R.string.tr_metro;
                case "FERRY":
                    return R.string.tr_boat;
                case "HEAVY_RAIL":case "RAIL":case "MONORAIL":case "COMMUTER_TRAIN":case "HIGH_SPEED_TRAIN":
                    return R.string.tr_train;

                case "OTHER":
                default:
                    return R.string.tr_walk;
            }
        }

    public boolean isTransit() {
        switch (type) {
            case "BUS":
            case "INTERCITY_BUS":
            case "TROLLEYBUS":
                return true;
            case "METRO_RAIL":
                return true;
            case "FERRY":
                return true;
            case "HEAVY_RAIL":
            case "RAIL":
            case "MONORAIL":
            case "COMMUTER_TRAIN":
            case "HIGH_SPEED_TRAIN":
                return true;
            case "OTHER":
            default:
                return false;
        }
    }

    public RouteStep(String distance, long meters, String duration, long durSec, String desc,
                     String firstLoc, String lastLoc, String type, String depTime, long depSec, String arrTime,
                     long arrSec, String vehicleName, String shortName, List<LatLng> latlng,
                     LatLng startLoc, LatLng endLoc, String travelMode,
                     String departureStop, String arrivalStop, String jsonString) {
        this.distance = new TravelDistance(distance,meters);
        this.duration = new TravelTime(duration, durSec);
        this.desc = desc;
        this.firstLoc = firstLoc;
        this.lastLoc = lastLoc;
        this.type = type;
        this.departure = new TravelTime(depTime,depSec);
        this.arrival = new TravelTime(arrTime,arrSec);
        this.vehicleName = vehicleName;
        this.shortName = shortName;
        this.latlng = latlng;
        this.startLoc = startLoc;
        this.endLoc = endLoc;
        //this.polyLine = polyline;
        this.travelMode = travelMode;
        this.jsonString = jsonString;
        this.departureStop = departureStop;
        this.arrivalStop = arrivalStop;
    }

    public RouteStep(String type, String name, LatLng startLoc,
                     String depTime, long depSec, String arrTime, long arrSec) {
        this.type = type;
        this.shortName = name;
        this.startLoc = startLoc;
        this.departure = new TravelTime(depTime,depSec);
        this.arrival = new TravelTime(arrTime,arrSec);
    }

    public TravelDistance getDistance() { return distance; }

    public TravelTime getDuration() { return  duration; }

    public String getDesc() { return desc; }

    public TravelTime getDeparture() { return departure; }

    public  TravelTime getArrival() { return  arrival; }

    public String getVehicleName() { return vehicleName; }

    public String getShortName() { return shortName; }

    public List<LatLng> getLatlng() { return latlng; }

    public LatLng getStartLoc() { return startLoc; }

    public LatLng getEndLoc() { return endLoc; }

    //public String getPolyLine() { return polyLine;}

    public String getType() { return type; }

    public String getTravelMode() { return travelMode; }

    public void add(PathSegment p) {
        this.path.add(p);
    }

    public ArrayList<PathSegment> getPath() { return path; }

    public RouteStep(String json) throws JSONException {
        this(new JSONObject(json));
    }

    public RouteStep(JSONObject obj) throws JSONException {
        JSONObject transit = null;
        JSONArray Steps = null;
        Double startLat, startLng, endLat, endLng;
        long depSec;
        String departTime = "", instruction = "";

        this.desc = obj.getString("html_instructions");

        try {
            transit = obj.getJSONObject("transit_details");
        }catch (Exception e) {}

        if (transit==null) {
            Steps = obj.getJSONArray("steps");

            for (int i = 0; i < Steps.length(); i++) {
                JSONObject pathS = Steps.getJSONObject(i);
                instruction = "";
                startLat = pathS.getJSONObject("start_location").getDouble("lat");
                startLng = pathS.getJSONObject("start_location").getDouble("lng");
                endLat = pathS.getJSONObject("end_location").getDouble("lat");
                endLng = pathS.getJSONObject("end_location").getDouble("lng");
                departTime = pathS.getJSONObject("duration").getString("text");
                depSec = pathS.getJSONObject("duration").getLong("value");
                try {
                    instruction = pathS.getString("html_instructions");
                } catch (Exception e ) {}
                PathSegment p = new PathSegment(new LatLng(startLat, startLng), new LatLng(endLat, endLng),
                        departTime,depSec, instruction,
                        pathS.getJSONObject("distance").getLong("value"));
                path.add(p);
            }
        }
        else {
            this.shortName = transit.getJSONObject("line").getString("short_name");
            this.departureStop = transit.getJSONObject("departure_stop").getString("name");
            this.arrivalStop = transit.getJSONObject("arrival_stop").getString("name");
        }
    }

    public String getJsonString() { return jsonString; }

    public String getDepartureStop() { return departureStop; }

    public String getArrivalStop() { return arrivalStop;}
}
