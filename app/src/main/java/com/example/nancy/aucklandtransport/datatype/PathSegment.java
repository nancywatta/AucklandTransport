package com.example.nancy.aucklandtransport.datatype;

import android.text.Html;

import com.example.nancy.aucklandtransport.datatype.TravelTime;
import com.google.android.gms.maps.model.LatLng;

/**
 * PathSegment class represents the turn by turn instruction
 * of the walking step of the user's journey.
 *
 * Created by Nancy on 8/3/14.
 */
public class PathSegment {
    LatLng startLoc;
    LatLng endLoc;
    TravelTime travelTime;
    public boolean isNotified;
    public long distance;
    String instruction;

    public PathSegment(LatLng startLoc, LatLng endLoc, String time, long value,
                       String instruction, long distance) {
        this.startLoc = startLoc;
        this.endLoc = endLoc;
        this.travelTime = new TravelTime(time, value);
        this.isNotified = false;
        this.instruction = Html.fromHtml(instruction).toString();
        this.distance = distance;
    }

    public LatLng getStartLoc() { return startLoc;}
    public LatLng getEndLoc() { return endLoc; }
    public TravelTime getTravelTime() { return travelTime; }
    public String getInstruction() { return instruction; }
    public long getDistance() { return distance; }
}
