package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.nancy.aucklandtransport.Adapters.RoutesAdaptar;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

/**
 * AlternateRoute class to show new routes available to user in case
 * there is more than 5 minutes for his current Bus to arrive and there are other
 * shorter routes available.
 *
 * Created by Nancy on 7/3/14.
 */
public class AlternateRoute extends Activity {

    /*
    Debugging tag for the AlternateRoute class
     */
    private static final String TAG = AlternateRoute.class.getSimpleName();

    // start location of the journey
    String fromLoc;

    // end location of the journey
    String toLoc;

    // reference to the listview to be populated with routes array
    ListView list;

    // Adapter for routes array
    RoutesAdaptar adapter;

    // array of alternate routes for given origin and destination
    private ArrayList<Route> routes = null;
    TextView origin;
    TextView destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alternate_route);

        // Get the new routes from routeEngine
        routes = RouteEngine.newRoutes;
        fromLoc = routes.get(0).getStartAddress();
        try {
            // encoding special characters like space in the user input place
            fromLoc = URLDecoder.decode(fromLoc, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        toLoc = routes.get(0).getEndAddress();
        try {
            // encoding special characters like space in the user input place
            toLoc = URLDecoder.decode(toLoc, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Getting reference to ListView to display routes
        list=(ListView)findViewById(R.id.list);

        // Getting reference to TextView to display origin address
        origin = (TextView)findViewById(R.id.textView1);

        // Getting reference to TextView to display destination address
        destination = (TextView)findViewById(R.id.textView2);

        origin.setText("From : " + fromLoc);
        destination.setText("To :   " + toLoc);

        adapter=new RoutesAdaptar(AlternateRoute.this, routes);
        list.setAdapter(adapter);

        // Click event for single list row
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent myIntent = new Intent(view.getContext(), RouteInfo.class);
                myIntent.putExtra("route", routes.get(position).getJsonString());
                myIntent.putExtra("from", fromLoc);
                myIntent.putExtra("to", toLoc);
                startActivity(myIntent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.alternate_route, menu);
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
