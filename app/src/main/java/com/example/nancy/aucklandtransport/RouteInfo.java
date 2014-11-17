package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.nancy.aucklandtransport.Adapters.RouteInfoAdapter;
import com.example.nancy.aucklandtransport.Utils.Constant;
import com.example.nancy.aucklandtransport.datatype.Route;

public class RouteInfo extends Activity{

    private static final String TAG = RouteInfo.class.getSimpleName();
    private String routeString;
    Route route = null;
    ListView listView;

    private IBackgroundServiceAPI api = null;

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

        Log.i(TAG, "Trying to bind service "+BackgroundService.class.getName());
        Intent servIntent = new Intent(BackgroundService.class.getName());
        startService(servIntent);
        Log.i(TAG, "starting service "+servIntent.toString());
        bindService(servIntent, serviceConnection, 0);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Service disconnected!");
            api = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            api = IBackgroundServiceAPI.Stub.asInterface(service);

            Log.i(TAG, "Service connected! "+api.toString());
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unbindService(serviceConnection);
        } catch(Exception e) {
            Log.e(TAG, "ERROR!!", e);
        }

        Log.i(TAG, "unbind ");
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

    public void launchNavigation(View v) {
        if(route != null) {
            try {
                if (api != null)
                    api.cancelRoute(1);
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Constant.NOTIFICATION_ID);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            Intent myIntent = new Intent(RouteInfo.this, ManageRoute.class);
            myIntent.putExtra("route", route.getJsonString());
            myIntent.putExtra("from", route.getStartAddress());
            myIntent.putExtra("to", route.getEndAddress());
            startActivity(myIntent);
            finish();
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
