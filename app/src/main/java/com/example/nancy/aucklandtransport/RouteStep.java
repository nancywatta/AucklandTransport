package com.example.nancy.aucklandtransport;

import com.example.nancy.aucklandtransport.datatype.TravelTime;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nancy on 7/14/14.
 */
public class RouteStep {
    public RouteStep () {

    }

    public int iconId = -1;
    public String desc;

    public String distance;
    public String duration;

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

    public RouteStep(String distance, String duration, String desc,
                     String firstLoc, String lastLoc, String type, String depTime, long depSec, String arrTime,
                     long arrSec, String vehicleName, String shortName, List<LatLng> latlng,
                     LatLng startLoc, LatLng endLoc, String travelMode) {
        this.distance = distance;
        this.duration = duration;
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
    }

    public RouteStep(String type, String name) {
        this.type = type;
        this.shortName = name;
    }

    public String getDistance() { return distance; }

    public String getDuration() { return  duration; }

    public String getDesc() { return desc; }

    public TravelTime getDeparture() { return departure; }

    public  TravelTime getArrival() { return  arrival; }

    public String getVehicleName() { return vehicleName; }

    public String getShortName() { return shortName; }

    public List<LatLng> getLatlng() { return latlng; }

    public LatLng getStartLoc() { return startLoc; }

    public LatLng getEndLoc() { return endLoc; }

    //public String getPolyLine() { return polyLine;}

    public String getTravelMode() { return travelMode; }

    public void add(PathSegment p) {
        this.path.add(p);
    }

    public ArrayList<PathSegment> getPath() { return path; }
}
