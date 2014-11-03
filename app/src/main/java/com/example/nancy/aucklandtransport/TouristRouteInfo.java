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

public class TouristRouteInfo extends Activity {

    private static final String TAG = TouristRouteInfo.class.getSimpleName();
    ExpandableListAdapter listAdapter;
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

        expListView = (ExpandableListView) findViewById(R.id.lvExp);

        Intent intent = getIntent();
        ArrayList<Route> arrayList = new ArrayList<Route>();

        String routeString = intent.getStringExtra("route");
        if(routeString != null) {
            try {
                Route route = new Route(routeString);
                arrayList.add(route);
                listAdapter = new ExpandableListAdapter(this, arrayList);
            } catch (JSONException e) {
                listAdapter = new ExpandableListAdapter(this, touristPlaces.getRoutesArray());
            }
        } else {
            listAdapter = new ExpandableListAdapter(this, touristPlaces.getRoutesArray());
        }

        // setting list adapter
        expListView.setAdapter(listAdapter);

        // Listview on child click listener
        final ArrayList<Route> finalArrayList = arrayList.size() < 1
                ? touristPlaces.getRoutesArray() : arrayList;
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {

                Intent myIntent = new Intent(TouristRouteInfo.this, PathElevation.class);
                Route route = null;
                try {
                     route = new Route(finalArrayList.get(groupPosition).getJsonString());
                } catch (JSONException e) {
                    Log.d(TAG, e.getMessage());
                }
                if(route != null) {
                    myIntent.putExtra("IS_TRANSIT", route.getSteps().get(childPosition).isTransit());
                    myIntent.putExtra("PathJSON", route.getSteps().get(childPosition).getJsonString());
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
