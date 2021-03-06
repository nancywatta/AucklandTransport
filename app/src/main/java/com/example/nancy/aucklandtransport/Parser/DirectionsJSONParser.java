package com.example.nancy.aucklandtransport.Parser;

import android.util.Log;

import com.example.nancy.aucklandtransport.datatype.PathSegment;
import com.example.nancy.aucklandtransport.datatype.Route;
import com.example.nancy.aucklandtransport.datatype.RouteStep;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * DirectionsJSONParser class receives a JSONObject
 * and parses the JSON String according to Output parameters of
 * Google Directions API to store in custom data Objects.
 * <p/>
 * Created by Nancy on 7/9/14.
 */
public class DirectionsJSONParser {

    /**
     * Receives a JSONObject and returns a list of routes
     */
    public ArrayList<Route> parse(JSONObject jObject) {

        ArrayList<Route> routes = new ArrayList<Route>();
        JSONArray jRoutes = null;
        JSONArray jLegs = null;
        JSONArray jSteps = null;
        JSONObject jDistance = null;
        JSONObject jDuration = null;
        JSONObject jDepartureTime = null;
        JSONObject jArrivalTime = null;
        String type = "";
        String name = "";
        JSONObject transit = null;
        long depSec = 0, arrSec = 0;
        String depTime = "", arrTime = "";

        try {

            jRoutes = jObject.getJSONArray("routes");

            /** Traversing all routes */
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");


                /** Traversing all legs */
                for (int j = 0; j < jLegs.length(); j++) {
                    jDistance = null;
                    jDuration = null;
                    jDepartureTime = null;
                    jArrivalTime = null;
                    jSteps = null;
                    depSec = 0;
                    arrSec = 0;
                    depTime = "";
                    arrTime = "";

                    JSONObject leg = jLegs.getJSONObject(j);

                    /** Getting distance from the json data */
                    jDistance = leg.getJSONObject("distance");

                    /** Getting duration from the json data */
                    jDuration = leg.getJSONObject("duration");

                    try {

                        if(!leg.isNull("departure_time")) {
                            jDepartureTime = leg.getJSONObject("departure_time");
                            depSec = jDepartureTime.getLong("value");
                            depTime = jDepartureTime.getString("text");
                        }

                        if(!leg.isNull("arrival_time")) {
                            jArrivalTime = leg.getJSONObject("arrival_time");
                            arrSec = jArrivalTime.getLong("value");
                            arrTime = jArrivalTime.getString("text");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Double lat = leg.getJSONObject("start_location").getDouble("lat");
                    Double lng = leg.getJSONObject("start_location").getDouble("lng");

                    Double lat1 = leg.getJSONObject("end_location").getDouble("lat");
                    Double lng1 = leg.getJSONObject("end_location").getDouble("lng");

                    /*
                    Creating the Route Object
                     */
                    Route route = new Route(jDistance.getString("text"), jDuration.getString("text"),
                            jDuration.getLong("value"),
                            leg.getString("start_address"), leg.getString("end_address"),
                            depTime, depSec, arrTime, arrSec,
                            new LatLng(lat, lng), new LatLng(lat1, lng1), leg.toString());

                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    /** Traversing all steps */
                    for (int k = 0; k < jSteps.length(); k++) {
                        Double startLat, startLng;
                        String departTime = "", arrivalTime = "";
                        depSec = 0;
                        arrSec = 0;
                        transit = null;
                        type = "";
                        name = "";
                        JSONObject step = jSteps.getJSONObject(k);
                        try {
                            if(!step.isNull("transit_details"))
                                transit = step.getJSONObject("transit_details");
                        } catch (Exception e) {
                        }


                        if (transit != null) {
                            type = transit.getJSONObject("line").getJSONObject("vehicle").getString("type");
                            name = transit.getJSONObject("line").getString("short_name");
                            startLat = transit.getJSONObject("departure_stop").getJSONObject("location").getDouble("lat");
                            startLng = transit.getJSONObject("departure_stop").getJSONObject("location").getDouble("lng");
                            departTime = transit.getJSONObject("departure_time").getString("text");
                            depSec = transit.getJSONObject("departure_time").getLong("value");
                            arrivalTime = transit.getJSONObject("arrival_time").getString("text");
                            arrSec = transit.getJSONObject("arrival_time").getLong("value");
                        } else {
                            startLat = step.getJSONObject("start_location").getDouble("lat");
                            startLng = step.getJSONObject("start_location").getDouble("lng");
                        }

                        /*
                        Creating Route Step Object
                         */
                        RouteStep routeStep = new RouteStep(type, name, new LatLng(startLat, startLng),
                                departTime, depSec, arrivalTime, arrSec);

                        /*
                        Adding the created object in the array of route steps of the
                        corresponding Route Object
                         */
                        route.getSteps().add(routeStep);
                    }
                    if (route != null) {
                        Log.i("routes", "added");
                        routes.add(route);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
        return routes;
    }

    /**
     * Receives a JSONObject and returns RouteStep
     */
    public RouteStep parseRouteStep(JSONObject jObject) {
        RouteStep routeStep = null;

        JSONArray jRoutes = null;
        JSONArray jLegs = null;
        JSONObject jDistance = null;
        JSONObject jDuration = null;
        JSONArray jSteps = null;
        JSONArray Steps = null;
        String startAddr = "";
        String endAddr = "";
        String type = "";
        String departTime = "";
        String arrivalTime = "";
        String shortName = "";
        String name = "";
        String departureStop = "", arrivalStop = "", instruc = "";
        Double startLat, startLng, endLat, endLng;
        long arrSec, depSec;

        try {

            jRoutes = jObject.getJSONArray("routes");

            /** Traversing all routes */
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                String polyline = "";
                polyline = ((JSONObject) jRoutes.get(i)).getJSONObject("overview_polyline").getString("points");
                List<LatLng> list = new ArrayList<LatLng>();
                list = PolyUtil.decode(polyline);

                /** Traversing all legs */
                for (int j = 0; j < jLegs.length(); j++) {
                    jDistance = null;
                    jDuration = null;
                    jSteps = null;
                    type = "";
                    name = "";
                    shortName = "";
                    arrSec = 0;
                    depSec = 0;
                    endAddr = "";
                    startAddr = "";
                    departTime = "";
                    arrivalTime = "";
                    departureStop = "";
                    arrivalStop = "";
                    instruc = "";

                    JSONObject leg = jLegs.getJSONObject(j);

                    /** Getting distance from the json data */
                    jDistance = leg.getJSONObject("distance");

                    /** Getting duration from the json data */
                    jDuration = leg.getJSONObject("duration");

                    startLat = leg.getJSONObject("start_location").getDouble("lat");
                    startLng = leg.getJSONObject("start_location").getDouble("lng");

                    endLat = leg.getJSONObject("end_location").getDouble("lat");
                    endLng = leg.getJSONObject("end_location").getDouble("lng");

                    startAddr = leg.getString("start_address");
                    endAddr = leg.getString("end_address");

                    routeStep = new RouteStep(jDistance.getString("text"), jDistance.getLong("value"),
                            jDuration.getString("text"), jDuration.getLong("value"),
                            instruc, startAddr, endAddr, type, departTime, depSec,
                            arrivalTime, arrSec, name,
                            shortName, list, new LatLng(startLat, startLng), new LatLng(endLat, endLng), "WALKING",
                            departureStop, arrivalStop, leg.toString());

                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    /** Traversing all steps */
                    for (int k = 0; k < jSteps.length(); k++) {
                        type = "";
                        name = "";
                        JSONObject path = jSteps.getJSONObject(k);

                        instruc = "";
                        startLat = path.getJSONObject("start_location").getDouble("lat");
                        startLng = path.getJSONObject("start_location").getDouble("lng");
                        endLat = path.getJSONObject("end_location").getDouble("lat");
                        endLng = path.getJSONObject("end_location").getDouble("lng");
                        departTime = path.getJSONObject("duration").getString("text");
                        depSec = path.getJSONObject("duration").getLong("value");
                        try {
                            if(!path.isNull("html_instructions"))
                                instruc = path.getString("html_instructions");
                        } catch (Exception e) {
                        }
                        PathSegment p = new PathSegment(new LatLng(startLat, startLng), new LatLng(endLat, endLng),
                                departTime, depSec, instruc,
                                path.getJSONObject("distance").getLong("value"));
                        routeStep.add(p);
                    }
                }
            }
        } catch (
                JSONException e
                )

        {
            e.printStackTrace();
        } catch (
                Exception e
                )

        {
        }

        return routeStep;
    }

    public ArrayList<Route> parse1(JSONObject jObject) {

        ArrayList<Route> routes = new ArrayList<Route>();
        JSONArray jRoutes = null;
        JSONArray jLegs = null;
        JSONObject jDistance = null;
        JSONObject jDuration = null;

        try {

            jRoutes = jObject.getJSONArray("routes");

            /** Traversing all routes */
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");


                /** Traversing all legs */
                for (int j = 0; j < jLegs.length(); j++) {

                    JSONObject leg = jLegs.getJSONObject(j);

                    /** Getting distance from the json data */
                    jDistance = leg.getJSONObject("distance");

                    /** Getting duration from the json data */
                    jDuration = leg.getJSONObject("duration");

                    Double lat = leg.getJSONObject("start_location").getDouble("lat");
                    Double lng = leg.getJSONObject("start_location").getDouble("lng");

                    Double lat1 = leg.getJSONObject("end_location").getDouble("lat");
                    Double lng1 = leg.getJSONObject("end_location").getDouble("lng");

                    Route route = new Route(jDistance.getString("text"), jDuration.getString("text"),
                            jDuration.getLong("value"),
                            leg.getString("start_address"), leg.getString("end_address"),
                            new LatLng(lat, lng), new LatLng(lat1, lng1), leg.toString());

                    if (route != null) {
                        Log.i("routes", "added");
                        routes.add(route);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
        return routes;
    }
}

/*
{
   "routes" : [
      {
         "bounds" : {
            "northeast" : {
               "lat" : -36.8573044,
               "lng" : 174.9248943
            },
            "southwest" : {
               "lat" : -37.0435925,
               "lng" : 174.7609776
            }
         },
         "copyrights" : "Map data ©2014 Google",
         "legs" : [
            {
               "arrival_time" : {
                  "text" : "2:29am",
                  "time_zone" : "Pacific/Auckland",
                  "value" : 1405175358
               },
               "departure_time" : {
                  "text" : "1:28am",
                  "time_zone" : "Pacific/Auckland",
                  "value" : 1405171715
               },
               "distance" : {
                  "text" : "27.7 km",
                  "value" : 27737
               },
               "duration" : {
                  "text" : "1 hour 1 min",
                  "value" : 3643
               },
               "end_address" : "Takanini, New Zealand",
               "end_location" : {
                  "lat" : -37.0397004,
                  "lng" : 174.9241291
               },
               "start_address" : "9b/14 Upper Queen Street, Auckland, 1010, New Zealand",
               "start_location" : {
                  "lat" : -36.858507,
                  "lng" : 174.7611247
               },
               "steps" : [
                  {
                     "distance" : {
                        "text" : "0.1 km",
                        "value" : 143
                     },
                     "duration" : {
                        "text" : "2 mins",
                        "value" : 139
                     },
                     "end_location" : {
                        "lat" : -36.85731800000001,
                        "lng" : 174.76178
                     },
                     "html_instructions" : "Walk to 520 Queen St",
                     "polyline" : {
                        "points" : "t|}_F_`ti`@QIUIYKs@Uq@Ua@Qa@ME?BW"
                     },
                     "start_location" : {
                        "lat" : -36.858507,
                        "lng" : 174.7611247
                     },
                     "steps" : [
                        {
                           "distance" : {
                              "text" : "70 m",
                              "value" : 70
                           },
                           "duration" : {
                              "text" : "1 min",
                              "value" : 59
                           },
                           "end_location" : {
                              "lat" : -36.8579199,
                              "lng" : 174.7613933
                           },
                           "html_instructions" : "Head \u003cb\u003enortheast\u003c/b\u003e on \u003cb\u003eUpper Queen St\u003c/b\u003e toward \u003cb\u003eKarangahape Rd\u003c/b\u003e",
                           "polyline" : {
                              "points" : "t|}_F_`ti`@QIUIYKs@U"
                           },
                           "start_location" : {
                              "lat" : -36.858507,
                              "lng" : 174.7611247
                           },
                           "travel_mode" : "WALKING"
                        },
                        {
                           "distance" : {
                              "text" : "73 m",
                              "value" : 73
                           },
                           "duration" : {
                              "text" : "1 min",
                              "value" : 80
                           },
                           "end_location" : {
                              "lat" : -36.85731800000001,
                              "lng" : 174.76178
                           },
                           "html_instructions" : "Continue onto \u003cb\u003eQueen St\u003c/b\u003e\u003cdiv style=\"font-size:0.9em\"\u003eDestination will be on the right\u003c/div\u003e",
                           "polyline" : {
                              "points" : "~x}_Fuati`@q@Ua@Qa@ME?BW"
                           },
                           "start_location" : {
                              "lat" : -36.8579199,
                              "lng" : 174.7613933
                           },
                           "travel_mode" : "WALKING"
                        }
                     ],
                     "travel_mode" : "WALKING"
                  },
                  {
                     "distance" : {
                        "text" : "26.8 km",
                        "value" : 26770
                     },
                     "duration" : {
                        "text" : "48 mins",
                        "value" : 2880
                     },
                     "end_location" : {
                        "lat" : -37.043534,
                        "lng" : 174.9198
                     },
                     "html_instructions" : "Bus towards Papakura",
                     "polyline" : {
                        "points" : "fu}_Fcdti`@vBhAGaDjBqG|H~C~E|C~CpAbDx@rE~@|@qBl@cG`AyFdCwRfCaSh@{FhDeV~@iJl@sDnD^jGrAlGdApAh@|Sq@~AF`KaOnCeDvDaGrAeBpOwLlBkA`DoChJwJxEcFpBsBvG_KjJ}NvAsAzNqUp@w@zBsCpCqDrMkMjWyVzB_Bn\\k\\`HuGb@kBSgFj@wBxHsGtCcBjA}A|BkFpAiI~AqAdJwAlAs@`EmFpIwKnIsK|BcCfM}J`C}ApIgHbF{GhByBpC{A`JcBpO{DzF{@tQqBrDaAhCiCnGeGp]ma@zSiLvn@kMj\\k`@pNoDna@qWbMoL~GcGjQ_PzP_T~J_ZzHgTzc@u^|b@mMbNaSz`@gA|OaTpa@w@nNeEdMuDxR{DpUuGtSaSdZcZrh@e\\tTyZjW}^jQsW"
                     },
                     "start_location" : {
                        "lat" : -36.85731800000001,
                        "lng" : 174.76178
                     },
                     "transit_details" : {
                        "arrival_stop" : {
                           "location" : {
                              "lat" : -37.043534,
                              "lng" : 174.9198
                           },
                           "name" : "166 Great South Rd Takanini"
                        },
                        "arrival_time" : {
                           "text" : "2:19am",
                           "time_zone" : "Pacific/Auckland",
                           "value" : 1405174740
                        },
                        "departure_stop" : {
                           "location" : {
                              "lat" : -36.85731800000001,
                              "lng" : 174.76178
                           },
                           "name" : "520 Queen St"
                        },
                        "departure_time" : {
                           "text" : "1:31am",
                           "time_zone" : "Pacific/Auckland",
                           "value" : 1405171860
                        },
                        "headsign" : "Papakura",
                        "line" : {
                           "agencies" : [
                              {
                                 "name" : "Auckland Transport",
                                 "phone" : "09-355 3553",
                                 "url" : "http://www.maxx.co.nz/"
                              }
                           ],
                           "color" : "#abf2f2",
                           "name" : "Civic Centre To Papakura Via Great South Rd",
                           "short_name" : "N47",
                           "text_color" : "#000000",
                           "vehicle" : {
                              "icon" : "//maps.gstatic.com/mapfiles/transit/iw/6/bus.png",
                              "name" : "Bus",
                              "type" : "BUS"
                           }
                        },
                        "num_stops" : 52
                     },
                     "travel_mode" : "TRANSIT"
                  },
                  {
                     "distance" : {
                        "text" : "0.8 km",
                        "value" : 824
                     },
                     "duration" : {
                        "text" : "10 mins",
                        "value" : 618
                     },
                     "end_location" : {
                        "lat" : -37.0397004,
                        "lng" : 174.9241291
                     },
                     "html_instructions" : "Walk to Takanini, New Zealand",
                     "polyline" : {
                        "points" : "`abaFw_sj`@JLoAhBQsA{@iE]wBGa@AIOg@CK?CCIAGEYIe@m@qDkA{GQ}@EOEIE?G?aCn@iBl@qA`@u@V"
                     },
                     "start_location" : {
                        "lat" : -37.043534,
                        "lng" : 174.9198
                     },
                     "steps" : [
                        {
                           "distance" : {
                              "text" : "65 m",
                              "value" : 65
                           },
                           "duration" : {
                              "text" : "1 min",
                              "value" : 49
                           },
                           "end_location" : {
                              "lat" : -37.0431877,
                              "lng" : 174.9192031
                           },
                           "html_instructions" : "Head \u003cb\u003enorthwest\u003c/b\u003e on \u003cb\u003eGreat South Rd/Urban Route 3\u003c/b\u003e toward \u003cb\u003eWalter Strevens Drive\u003c/b\u003e",
                           "polyline" : {
                              "points" : "`abaFw_sj`@JLoAhB"
                           },
                           "start_location" : {
                              "lat" : -37.043534,
                              "lng" : 174.9198
                           },
                           "travel_mode" : "WALKING"
                        },
                        {
                           "distance" : {
                              "text" : "38 m",
                              "value" : 38
                           },
                           "duration" : {
                              "text" : "1 min",
                              "value" : 33
                           },
                           "end_location" : {
                              "lat" : -37.0430978,
                              "lng" : 174.9196172
                           },
                           "html_instructions" : "Turn \u003cb\u003eright\u003c/b\u003e onto \u003cb\u003eWalter Strevens Drive\u003c/b\u003e",
                           "maneuver" : "turn-right",
                           "polyline" : {
                              "points" : "|~aaF_|rj`@QsA"
                           },
                           "start_location" : {
                              "lat" : -37.0431877,
                              "lng" : 174.9192031
                           },
                           "travel_mode" : "WALKING"
                        },
                        {
                           "distance" : {
                              "text" : "0.5 km",
                              "value" : 496
                           },
                           "duration" : {
                              "text" : "6 mins",
                              "value" : 377
                           },
                           "end_location" : {
                              "lat" : -37.041627,
                              "lng" : 174.9248867
                           },
                           "html_instructions" : "Continue onto \u003cb\u003eTaka St\u003c/b\u003e",
                           "polyline" : {
                              "points" : "j~aaFs~rj`@{@iE]wBGa@AIOg@CK?CCIAGEYIe@m@qDkA{GQ}@EOEI"
                           },
                           "start_location" : {
                              "lat" : -37.0430978,
                              "lng" : 174.9196172
                           },
                           "travel_mode" : "WALKING"
                        },
                        {
                           "distance" : {
                              "text" : "0.2 km",
                              "value" : 225
                           },
                           "duration" : {
                              "text" : "3 mins",
                              "value" : 159
                           },
                           "end_location" : {
                              "lat" : -37.0397004,
                              "lng" : 174.9241291
                           },
                           "html_instructions" : "Turn \u003cb\u003eleft\u003c/b\u003e onto \u003cb\u003eTakanini School Rd\u003c/b\u003e",
                           "maneuver" : "turn-left",
                           "polyline" : {
                              "points" : "duaaFq_tj`@E?G?aCn@iBl@qA`@u@V"
                           },
                           "start_location" : {
                              "lat" : -37.041627,
                              "lng" : 174.9248867
                           },
                           "travel_mode" : "WALKING"
                        }
                     ],
                     "travel_mode" : "WALKING"
                  }
               ],
               "via_waypoint" : []
            }
         ],
         "overview_polyline" : {
            "points" : "t|}_F_`ti`@uBu@{Bu@BWvBhAGaDjBqG|H~C~E|C~CpAbDx@rE~@|@qBl@cG`AyFlGyf@h@{FhDeV~@iJl@sDnD^jGrAlGdApAh@|Sq@~AF`KaOnCeDvDaGrAeBpOwLlBkA`DoChJwJjIwIbS}ZvAsAzNqUlDkEpCqDrMkMjWyVzB_Bn\\k\\`HuGb@kBSgFj@wBxHsGtCcBjA}A|BkFpAiI~AqAdJwAlAs@`EmF`TkX|BcCfM}J`C}ApIgHbF{GhByBpC{A`JcBpO{DzF{@tQqBrDaAxKoKp]ma@zSiLvn@kMj\\k`@pNoDna@qWbMoL~GcGjQ_PzP_T~J_ZzHgTzc@u^|b@mMbNaSz`@gA|OaTpa@w@t\\{JxR{DpUuGtSaSdZcZrh@e\\tTyZjW}^jQsWJLoAhBQsA{@iEe@yCUaAUqAyBmMWmAEIE?iCn@qFfB"
         },
         "summary" : "",
         "warnings" : [
            "Walking directions are in beta.    Use caution – This route may be missing sidewalks or pedestrian paths."
         ],
         "waypoint_order" : []
      }
   ],
   "status" : "OK"
}
*/
