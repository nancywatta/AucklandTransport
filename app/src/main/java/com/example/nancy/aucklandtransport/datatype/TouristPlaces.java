package com.example.nancy.aucklandtransport.datatype;

import android.util.Log;

import com.example.nancy.aucklandtransport.Route;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 * TouristPlaces is used to store all the places a user will visit
 * when using Explore City Option. The class will also store the
 * connecting routes of all these places.
 *
 * Created by Nancy on 10/16/14.
 */
public class TouristPlaces {

    private static final String TAG = TouristPlaces.class.getSimpleName();

    /*
    array of places to be visited
     */
    private static ArrayList<Place> placesArray;

    /*
    array of route the user will follow
     */
    private static ArrayList<Route> routesArray;

    /*
    starting point of the user journey
     */
    public static String startAddress;

    /*
    ending point of the user journey
     */
    public static String endAddress;

    /*
    departure time of the journey, time in seconds since epoch
    input by the user in the Tourist Planner Activity
     */
    private static long departureTime;

    // Links lat and lng with the formatted address
    //HashMap<String, String> mHMReference = new HashMap<String, String>();

    public ArrayList<Route> getRoutesArray() { return routesArray; }

    public String getStartAddress() { return startAddress; }

    public String getEndAddress() { return endAddress; }

    public long getDepartureTime() {
        Log.d(TAG, "departureTime: " + departureTime);
        return departureTime; }

    public void setDepartureTime(long value) { departureTime = value; }

    public void setArray() {
        if(placesArray != null) placesArray.clear();
        if(routesArray != null) routesArray.clear();
    }

    /**
     * Add the given route in the routes array
     *
     * @param start - Start location address
     * @param startName - start location short name
     * @param end - End location address
     * @param endName - end location short name
     * @param array - route from start to end to be added in routes array
     * @param timeSinceEpoch - Departure time of the input route.
     */
    public void addRoute(String start, String startName,
                         String end, String endName, ArrayList<Route> array, long timeSinceEpoch) {
        if(array == null || array.size() < 1) {
            Log.d(TAG, " Input array Zero");
            return;
        }

        if(routesArray == null)
            routesArray = new ArrayList<Route>();


        Log.d(TAG, "Valid timeSinceEpoch");
        for (Route route : array) {
            if (timeSinceEpoch != 0) {
                /*
                In case the input route fetched from Google does not have the
                arrival and departure time set. So we manually set the departure and arrival times
                based on the input time.
                 */
                if (route.getArrival().seconds == 0) {
                    Log.d(TAG, "Arrival Null");
                    route.getArrival().seconds = timeSinceEpoch +
                            route.getDuration().seconds;
                    Date date = new Date(route.getArrival().seconds * 1000L); // *1000 is to convert seconds to milliseconds
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a"); // the format of your date
                    route.getArrival().travelTime = sdf.format(date);
                }
                if (route.getDeparture().seconds == 0) {
                    Log.d(TAG, "Departure Null: " + timeSinceEpoch);
                    route.getDeparture().seconds = timeSinceEpoch;
                    Date date = new Date(timeSinceEpoch * 1000L); // *1000 is to convert minutes to milliseconds
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a"); // the format of your date
                    route.getDeparture().travelTime = sdf.format(date);
                }
            }
            route.setStartTouristPlace(start);
            route.setEndTouristPlace(end);
            route.setStartTouristName(startName);
            route.setEndTouristName(endName);
        }

        routesArray.addAll(array);
    }

    /**
     * Delete the route from start to intermediateAdd
     * Also delete intermediateAdd to end if the input isDelete indicator
     * is set to true.
     * After successfully deleting, add the input route  into the routes array
     *
     * @param start - Start location address
     * @param startName - start location short name
     * @param end - End location address
     * @param endName - end location short name
     * @param intermediateAdd - intermediate location address, place between the
     *                        start and end location
     * @param array - route from start to end to be added in routes array
     * @param timeSinceEpoch - Departure time of the input route.
     */
    public void deleteRoute(String start, String startName,
                            String end, String endName, String intermediateAdd,
                            ArrayList<Route> array, long timeSinceEpoch, boolean isDelete) {
        if(routesArray == null)
            return;
        else if(array == null || array.size() < 1) {
            Log.d(TAG, " Input array Zero");
            return;
        }

        int index;
        for(index = 0; index < routesArray.size(); index++) {
            Route route = routesArray.get(index);
            if(route.getEndTouristPlace().compareTo(intermediateAdd) == 0 && isDelete)
                break;
            else if(route.getStartTouristPlace().compareTo(intermediateAdd) == 0 && !isDelete) {
                startName = route.getStartTouristName();
                endName = route.getEndTouristName();
                break;
            }
        }

        Log.d(TAG , "deleteRoute: " + routesArray.size() + " deleteAddress: "
                + intermediateAdd + " index: " + index);

        /*
        remove route from start address to intermediateAdd
         */
        routesArray.remove(index);
        /*
        remove route from intermediateAdd to end address
         */
        if(isDelete)
            routesArray.remove(index);


        Log.d(TAG, "Valid timeSinceEpoch");
        for (Route route : array) {
            if (timeSinceEpoch != 0) {
                /*
                In case the input route fetched from Google does not have the
                arrival and departure time set. So we manually set the departure and arrival times
                based on the input time.
                 */
                if (route.getArrival().seconds == 0) {
                    Log.d(TAG, "Arrival Null");
                    route.getArrival().seconds = timeSinceEpoch +
                            route.getDuration().seconds;
                    Date date = new Date(route.getArrival().seconds * 1000L); // *1000 is to convert seconds to milliseconds
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a"); // the format of your date
                    route.getArrival().travelTime = sdf.format(date);
                }
                if (route.getDeparture().seconds == 0) {
                    Log.d(TAG, "Departure Null: " + timeSinceEpoch);
                    route.getDeparture().seconds = timeSinceEpoch;
                    Date date = new Date(timeSinceEpoch * 1000L); // *1000 is to convert minutes to milliseconds
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a"); // the format of your date
                    route.getDeparture().travelTime = sdf.format(date);
                }
            }
            route.setStartTouristPlace(start);
            route.setEndTouristPlace(end);
            route.setStartTouristName(startName);
            route.setEndTouristName(endName);
        }

        /*
        add route from start address to end address disconnecting the intermediateAdd
         */
        routesArray.addAll(index, array);
    }

    /**
     * Get the departure time of the route ending with
     * intermediateAdd
     *
     * @param intermediateAdd
     * @return departure time in seconds since epoch
     */
    public long getTimeOfStart(String intermediateAdd) {
        if(routesArray == null)
            return 0;

        Log.d(TAG, "deleteAddress: " + intermediateAdd);
        int index;
        for(index = 0; index < routesArray.size(); index++) {
            Route route = routesArray.get(index);
            Log.d(TAG, "EndAdress: " + route.getEndTouristPlace());
            if(route.getEndTouristPlace().compareTo(intermediateAdd) == 0)
                break;
        }

        return routesArray.get(index).getDeparture().getSeconds();
    }

    /**
     * Get the arrival time of the route ending with
     * intermediateAdd plus the duration passed in the input
     *
     * @param intermediateAdd
     * @return arrival time in seconds since epoch
     */
    public long getTimeOfDepart(String intermediateAdd, long duration) {
        if(routesArray == null)
            return 0;

        Log.d(TAG, "deleteAddress: " + intermediateAdd);
        int index;
        for(index = 0; index < routesArray.size(); index++) {
            Route route = routesArray.get(index);
            Log.d(TAG, "EndAdress: " + route.getEndTouristPlace());
            if(route.getEndTouristPlace().compareTo(intermediateAdd) == 0)
                break;
        }

        return routesArray.get(index).getArrival().getSeconds() + (duration*60);
    }

    /**
     *  When adding a new destination point, delete the last route
     *  from the routes array. This is because consider an example
     *  where the existing routes array contains below two routes
     *  Start -> A, A -> End
     *  Thus when adding a new place B in between A and End,
     *  route A -> End should be deleted and instead
     *  the routes array should now contain below three routes.
     *  Start -> A, A -> B, B -> End
     */
    public void deletePreviousRoute() {
        if(routesArray == null)
            return;

        int size = routesArray.size();
        if(routesArray.size() > 1) {
            routesArray.remove(size - 1);
        }
    }

    /**
     * Get the departure time of the last route in the
     * routes array
     *
     * @return departure time in seconds since epoch
     */
    public long getTimeOfStart() {
        if(routesArray == null) {
            Log.d(TAG, "start of Journey");
            return 0;
        }

        int size = routesArray.size();
        if(size > 1) {
            return routesArray.get(size - 1).getDeparture().getSeconds();
        }
        return 0;
    }

    /**
     * Add the place of interest in the tourist places array
     *
     * @param place
     */
    public void add(Place place) {
        if(placesArray == null)
            placesArray = new ArrayList<Place>();
        placesArray.add(place);

//        Log.d(TAG , "Add:" + placesArray.size());
    }

    /**
     * Delete the place of interest from the tourist places array
     *
     * @param place
     */
    public void delete(Place place) {
        if(placesArray == null)
            return;

        for (Iterator<Place> it = placesArray.iterator(); it.hasNext(); ) {
            Place place1 = it.next();
            if (place1.compare(place)) {
                it.remove();
                Log.d(TAG , "Delete:");
            }
        }

//        if(placesArray != null)
//        Log.d("Size" , "Delete:" + placesArray.size());
    }

    /**
     * Check if the input place exist in the tourist places array
     *
     * @param place
     * @return boolean indicator
     */
    public boolean checkExisting(Place place) {
        if(placesArray == null)
            return false;
        for(Place place1: placesArray) {
            if(place1.compare(place))
                return true;
        }
        return false;
    }

    public ArrayList<Place> getPlacesArray() { return placesArray; }

    /**
     * returns the previous place to be visited from the
     * given Place. If No previous place of Interest, then return the
     * starting point of Journey.
     */
//    public String getPreviousAdd(Place place) {
//        if(placesArray == null)
//            return "";
//        if(placesArray.size() <= 1)
//            return startAddress;
//
//        for(int index=0; index < placesArray.size(); index++) {
//            Place place1 = placesArray.get(index);
//            if (place1.compare(place)) {
//                if(index == 0)
//                    return startAddress;
//                Place tempPlace = placesArray.get(index - 1);
//                return tempPlace.mLat + "," + tempPlace.mLng;
//            }
//        }
//
//        return "";
//    }

    /**
     * returns the place that is previous to the input place
     * in the tourist places array. If No previous place of Interest,
     * then return NULL
     */
    public Place getPreviousAdd(Place place) {
        if(placesArray == null)
            return null;
        if(placesArray.size() <= 1)
            return null;

        for(int index=0; index < placesArray.size(); index++) {
            Place place1 = placesArray.get(index);
            if (place1.compare(place)) {
                if(index == 0)
                    return null;
                return placesArray.get(index - 1);
            }
        }

        return null;
    }

    /**
     * returns the next place to be visited from the
     * given Place.
     */
    public Place getNextAddress(Place place) {
        if(placesArray == null)
            return null;
        if(placesArray.size() <= 1)
            return null;

        for(int index=0; index < placesArray.size(); index++) {
            Place place1 = placesArray.get(index);
            if (place1.compare(place) && index < placesArray.size() - 1) {
                //Place tempPlace = placesArray.get(index + 1);
                return placesArray.get(index + 1);
            }
        }

        return null;
    }

    /**
     * Get the arrival time of the last route in the
     * routes array plus the duration passed in the input
     *
     * @param duration
     * @return long - arrival time in seconds since epoch
     */
    public long getDepartureTime(long duration) {
        if(routesArray == null)
            return 0;

        int lastIndex = routesArray.size() - 1;
        Route route = routesArray.get(lastIndex);
        if(route.getArrival().seconds != 0) {
            Log.d(TAG, " Not Zero ARRIVAL Seconds");
            return route.getArrival().getSeconds() + (duration*60);
        }
        return 0;
    }
}
