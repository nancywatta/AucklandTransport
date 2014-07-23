package com.example.nancy.aucklandtransport.datatype;

/**
 * Created by Nancy on 7/13/14.
 */
public class Coords {
    public double x;
    public double y;

    public Coords(String coord) {
        int pos = coord.indexOf(",");
        this.x = Double.parseDouble(coord.substring(pos+1, coord.length()));
        this.y = Double.parseDouble(coord.substring(0, pos));
    }

    public Coords(Double x, Double y) {
        this.y = y;
        this.x = x;
    }

    public String toString() {
        return ""+y+","+x;
    }

    public int xToInt() {
        return (int) (x*1E6);
    }

    public int yToInt() {
        return (int) (y*1E6);
    }
}
