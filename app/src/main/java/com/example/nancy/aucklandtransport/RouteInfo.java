package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class RouteInfo extends Activity {

    private static final String TAG = RouteInfo.class.getSimpleName();
    private String routeString;
    Route route = null;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_info);

        getRoute();
        listView = (ListView) findViewById(R.id.RouteInfoScreenListView);
        if(route!=null)
            listView.setAdapter(new RouteInfoAdapter(RouteInfo.this, route));

        // Click event for single list row
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent myIntent = new Intent(view.getContext(), PathElevation.class);
                myIntent.putExtra("IS_TRANSIT", route.getSteps().get(position).isTransit());
                myIntent.putExtra("PathJSON", route.getSteps().get(position).getJsonString());
                startActivity(myIntent);
            }
        });
    }

    private void getRoute() {
        SharedPreferences settings = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        try {
            Intent intent = getIntent();
            routeString = intent.getStringExtra("route");
            if (!routeString.equals("")) route = new Route(routeString);
            else {
                Log.d(TAG, "Shared Working :)");
                routeString = settings.getString("route", "");
                route = new Route(routeString);
            }
        } catch ( Exception e ) {
            Log.e("ERROR", "Couldn't get the route from JSONobj");
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        getRoute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.route_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
