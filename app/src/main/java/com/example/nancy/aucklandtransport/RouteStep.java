package com.example.nancy.aucklandtransport;

import com.google.android.gms.maps.model.LatLng;

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

    protected String depTime;
    protected String arrTime;
    private String vehicleName;
    private String shortName;

    List<LatLng> latlng;

        //public ArrayList<PathSegment> path;

        //public boolean hasRemindedArr = false;

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
                     String firstLoc, String lastLoc, String type, String depTime, String arrTime,
                     String vehicleName, String shortName, List<LatLng> latlng) {
        this.distance = distance;
        this.duration = duration;
        this.desc = desc;
        this.firstLoc = firstLoc;
        this.lastLoc = lastLoc;
        this.type = type;
        this.depTime = depTime;
        this.arrTime = arrTime;
        this.vehicleName = vehicleName;
        this.shortName = shortName;
        this.latlng = latlng;
    }

    public RouteStep(String type, String name) {
        this.type = type;
        this.shortName = name;
    }

    public String getDistance() { return distance; }

    public String getDuration() { return  duration; }

    public String getDesc() { return desc; }

    public String getDepTime() { return depTime; }

    public  String getArrTime() { return  arrTime; }

    public String getVehicleName() { return vehicleName; }

    public String getShortName() { return shortName; }

    public List<LatLng> getLatlng() { return latlng; }

}
