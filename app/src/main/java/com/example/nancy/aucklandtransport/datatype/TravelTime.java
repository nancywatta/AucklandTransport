package com.example.nancy.aucklandtransport.datatype;

/**
 * TravelDistance is used to store the duration in seconds and
 * also its string format.
 *
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
