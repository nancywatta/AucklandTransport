package com.example.nancy.aucklandtransport.datatype;

/**
 * Created by Nancy on 7/29/14.
 */
public class TravelTime {
    String travelTime;
    long seconds;

    public TravelTime(String time, long value) {
        this.travelTime = time;
        this.seconds = value;
    }
    public String getTravelTime() { return travelTime; }

    public long getSeconds() { return seconds; }
}
