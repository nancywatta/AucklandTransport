package com.example.nancy.aucklandtransport.datatype;

/**
 * Created by Nancy on 10/5/14.
 */
public class TravelDistance {

    String travelDistance;
    long meters;

    public TravelDistance(String text, long value) {
        this.travelDistance = text;
        this.meters = value;
    }
    public String getTravelDistance() { return travelDistance; }

    public long getMeters() { return meters; }

}
