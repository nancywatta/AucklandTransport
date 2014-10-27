package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView;

import com.example.nancy.aucklandtransport.Adapters.ExpandableListAdapter;
import com.example.nancy.aucklandtransport.datatype.TouristPlaces;

import org.json.JSONException;

import java.util.ArrayList;

public class TouristRouteInfo extends Activity {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    TouristPlaces touristPlaces = new TouristPlaces();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tourist_route_info);

        // get the listview
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
        } else
            listAdapter = new ExpandableListAdapter(this, touristPlaces.getRoutesArray());

        // setting list adapter
        expListView.setAdapter(listAdapter);
    }


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
