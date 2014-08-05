package com.example.nancy.aucklandtransport;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Nancy on 8/3/14.
 */
public class PathSegment {
    LatLng startLoc;
    LatLng endLoc;
    public PathSegment(LatLng startLoc, LatLng endLoc) {
        this.startLoc = startLoc;
        this.endLoc = endLoc;
    }
    public LatLng getStartLoc() { return startLoc;}
    public LatLng getEndLoc() { return endLoc; }
}
