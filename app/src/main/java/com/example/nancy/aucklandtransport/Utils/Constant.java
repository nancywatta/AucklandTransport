package com.example.nancy.aucklandtransport.Utils;

/**
 * Created by Nancy on 8/12/14.
 */
public class Constant {
    public final static int INIT=0;
    public final static int PRE_CHANGE_OVER=1;
    public final static int CHANGE_OVER=2;
    public final static int WALKING=3;
    public final static int TRANSIT=4;
    public final static int OFF_ROUTE=5;
    public final static int STOPPED=6;
    public final static int FINISHED=7;
    public final static int REROUTE=8;
    public final static int BUS=9;
    public final static int DEFAULT=10;
    public final static int STATE_DO_NOTHING = 11;
    public final static int STATE_START_ROUTE = 12;

    public static final int NOTIFICATION_ID = 12345;
    public static boolean IS_CHANGE_ROUTE = false;
    public static boolean SPEED_CHECK_IND = true;
    public static float USER_SPEED = 0;

    // Specifies the drawMarker() to draw the marker with default color
    public static final float UNDEFINED_COLOR = -1;

    // Action Name to IntentFilter
    public static final String BROADCAST_ACTION = "com.example.nancy.aucklandtransport.BROADCAST";
}
