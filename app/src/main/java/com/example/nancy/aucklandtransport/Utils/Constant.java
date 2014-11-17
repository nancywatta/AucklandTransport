package com.example.nancy.aucklandtransport.Utils;

/**
 * Constant class is used to define app-wide constants and utilities.
 *
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

    public static final String BROADCAST_NOTIFICATION = "com.example.nancy.aucklandtransport.BROADCAST_NOTIFICATION";

    public final static String FROM_LOCATION = "com.example.nancy.aucklandtransport.FROMADDRESS";
    public final static String TO_LOCATION = "com.example.nancy.aucklandtransport.TOADDRESS";
    public final static String TIME = "com.example.nancy.aucklandtransport.TIME";
    public final static String FROM_COORDS = "com.example.nancy.aucklandtransport.FROM_COORDS";
    public final static String TO_COORDS = "com.example.nancy.aucklandtransport.TO_COORDS";
    public final static String ARRIVE_TIME = "com.example.nancy.aucklandtransport.ARRIVE_TIME";
    public final static String FROM_ADDRSTR = "com.example.nancy.aucklandtransport.FROM_ADDRSTR";
    public final static String TO_ADDRSTR = "com.example.nancy.aucklandtransport.TO_ADDRSTR";
    public final static String ORIGIN = "com.example.nancy.aucklandtransport.ORIGIN";
    public final static String ADDRSTR = "com.example.nancy.aucklandtransport.ADDRESS";
    public final static String ISDEPARTURE = "com.example.nancy.aucklandtransport.ISDEPARTURE";

    public final static String USAGE_COUNT = "com.example.nancy.aucklandtransport.USAGE_COUNT";

    public static final int PICK_ADDRESS_REQUEST = 1;

    public static final int SURVEY_ELIG_COUNT = 5;

    public static String NOTIFICATION_MESSAGE = "";

    // Constants that define the activity detection interval
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DETECTION_INTERVAL_SECONDS = 1;
    public static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;
}
