package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.example.nancy.aucklandtransport.Adapters.ExpandableListAdapter;
import com.example.nancy.aucklandtransport.datatype.TouristPlaces;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * TouristRouteInfo class is used to display the entire tourist tour
 * consisting of various destination points. The class makes use of the
 * Expandable list view to display every leg of the entire journey.
 *
 * Created by Nancy on 10/7/14.
 */
public class TouristRouteInfo extends Activity {

    /*
    Debugging tag for the TouristRouteInfo class
     */
    private static final String TAG = TouristRouteInfo.class.getSimpleName();

    /*
    reference to ExpandableListAdapter to link list view with data
     */
    ExpandableListAdapter listAdapter;

    /*
    reference to ExpandableListView to display routes array
     */
    ExpandableListView expListView;

    TouristPlaces touristPlaces = new TouristPlaces();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tourist_route_info);

        Toast.makeText(getBaseContext(),
                "Long press the Group list for Start Navigation Activity",
                Toast.LENGTH_LONG).show();

        // get the listview
//        expListView = getExpandableListView();
//        metrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(metrics);
//        width = metrics.widthPixels;
//        //this code for adjusting the group indicator into right side of the view
//        expListView.setIndicatorBounds(width - GetDipsFromPixel(10), width - GetDipsFromPixel(10));

        /*
        Getting reference to ExpandableListView to display routes array
        */
        expListView = (ExpandableListView) findViewById(R.id.lvExp);

        Intent intent = getIntent();
        ArrayList<Route> arrayList = new ArrayList<Route>();

        // receive the route string from intent
        String routeString = intent.getStringExtra("route");
        // If not empty then the tourist journey consist of only one route
        if(routeString != null) {
            try {
                Route route = new Route(routeString);
                arrayList.add(route);
                listAdapter = new ExpandableListAdapter(this, arrayList);
            } catch (JSONException e) {
                listAdapter = new ExpandableListAdapter(this, touristPlaces.getRoutesArray());
            }
        } // If input route empty, fetch array from tourist places class
        else {
            listAdapter = new ExpandableListAdapter(this, touristPlaces.getRoutesArray());
        }

        // setting list adapter
        expListView.setAdapter(listAdapter);


        final ArrayList<Route> finalArrayList = arrayList.size() < 1
                ? touristPlaces.getRoutesArray() : arrayList;
        // Listview on child click listener
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                Route route = null;

                // The first row of every child is the link to ManageRoute activity
                if(childPosition == 0) {
                    try {
                        route = new Route(finalArrayList.get(groupPosition).getJsonString());
                    } catch (JSONException e) {
                        Log.d(TAG, e.getMessage());
                    }
                    if(route != null) {
                        Intent myIntent = new Intent(TouristRouteInfo.this, ManageRoute.class);
                        myIntent.putExtra("route", route.getJsonString());
                        myIntent.putExtra("from", route.getStartAddress());
                        myIntent.putExtra("to", route.getEndAddress());
                        startActivity(myIntent);
                    }
                    return false;
                }

                // Leaving the first row, rest child items are the route steps
                int childPos = childPosition - 1;
                Intent myIntent = new Intent(TouristRouteInfo.this, PathElevation.class);

                try {
                     route = new Route(finalArrayList.get(groupPosition).getJsonString());
                } catch (JSONException e) {
                    Log.d(TAG, e.getMessage());
                }
                if(route != null) {
                    myIntent.putExtra("IS_TRANSIT", route.getSteps().get(childPos).isTransit());
                    myIntent.putExtra("PathJSON", route.getSteps().get(childPos).getJsonString());
                    startActivity(myIntent);
                }
                return false;
            }
        });

        // Listview on child long click listener
        expListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                int itemType = ExpandableListView.getPackedPositionType(id);

                if(itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    int groupPosition = ExpandableListView.getPackedPositionGroup(id);

                    Route route = null;
                    try {
                        route = new Route(finalArrayList.get(groupPosition).getJsonString());
                    } catch (JSONException e) {
                        Log.d(TAG, e.getMessage());
                    }
                    if(route != null) {
                        Intent myIntent = new Intent(TouristRouteInfo.this, ManageRoute.class);
                        myIntent.putExtra("route", route.getJsonString());
                        myIntent.putExtra("from", route.getStartAddress());
                        myIntent.putExtra("to", route.getEndAddress());
                        startActivity(myIntent);
                    }
                }
                return false;
            }
        });
    }

//    public int GetDipsFromPixel(float pixels)
//    {
//        // Get the screen's density scale
//        final float scale = getResources().getDisplayMetrics().density;
//        // Convert the dps to pixels, based on density scale
//        return (int) (pixels * scale + 0.5f);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tourist_route_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_home:
                Intent exploreActivity = new Intent(TouristRouteInfo.this, HomePage.class);
                startActivity(exploreActivity);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
