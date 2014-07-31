package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

public class RouteInfoScreen extends Activity {
    private static final String TAG = RouteInfoScreen.class.getSimpleName();

    private String routeString;
    Route route = null;
    ListView listView;
    private boolean isRouteSet = false;
    private Boolean routeStarted = false;

    private static class EfficientAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private Context context;
        private Route route;

        public EfficientAdapter(Context context, Route route) {
            mInflater = LayoutInflater.from(context);
            this.context = context;
            this.route = route;
        }

        public int getCount() {
            return route.getSteps().size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.route_list_row, null);
                holder = new ViewHolder();
                holder.text1 = (TextView) convertView.findViewById(R.id.TextRouteDur);
                holder.image = (ImageView) convertView.findViewById(R.id.RouteInfoIcon);
                holder.text4 = (TextView) convertView.findViewById(R.id.TextRouteBus);
                holder.text5 = (TextView) convertView.findViewById(R.id.TextRouteAddress);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            RouteStep step = route.getSteps().get(position);

            if(!step.getDeparture().getTravelTime().equals("")) {
                holder.text1.setText(step.getDeparture().getTravelTime() + " - " + step.getArrival().getTravelTime());
            }
            else
                holder.text1.setText(step.getDistance() + " (" + step.getDuration() + ")");
            holder.image.setImageResource(step.getIconId());

            holder.text4.setText(step.getDesc());

            if(!step.getShortName().equals("") && step.getShortName()!="")
                holder.text5.setText(step.getShortName() + " --- " + step.getVehicleName());
            else
            holder.text5.setText("");

            return convertView;
        }

        static class ViewHolder {
            TextView text;
            TextView text1;
            ImageView image;
            TextView text2;
            TextView text3;
            TextView text4;
            TextView text5;
            TableRow row1;
            TableRow row2;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_info_screen);
        isRouteSet = false;
        getRoute();
        listView = (ListView) findViewById(R.id.RouteInfoScreenListView);
        if(route!=null)
        listView.setAdapter(new EfficientAdapter(RouteInfoScreen.this, route));
        Intent servIntent = new Intent(BackgroundService.class.getName());
        startService(servIntent);
        Log.i(TAG, "starting service "+servIntent.toString());
        bindService(servIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private IBackgroundServiceAPI api = null;
    private boolean isGPSon = false;

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

    private void getRoute() {
        SharedPreferences settings = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        try {
            routeString = settings.getString("route", "");
            routeStarted = settings.getBoolean("routeStarted", false);
            isRouteSet = settings.getBoolean("isRouteSet", false);

            if (routeStarted) isRouteSet = routeStarted;
            if (!routeString.equals("")) route = new Route(routeString);
            else {
                Log.d("Shared Not Working", ":(");
            }

        } catch ( Exception e ) {
            Log.e("ERROR", "Couldn't get the route from JSONobj");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.route_info_screen, menu);
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

    public void ShowMap(View v){
        try {
            Intent myIntent = new Intent(this, RouteMapActivity.class);
            startActivity(myIntent);
        }catch (Exception e) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unbindService(serviceConnection);
            Log.i(TAG, "unbind ");
        } catch(Exception e) {
            Log.e(TAG, "ERROR!!", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        getRoute();
    }

    private boolean setRoute() {
        try {
            if (api != null) {
                api.setRoute(routeString);
                SharedPreferences settings = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("route", routeString);
                editor.putBoolean("isRouteSet", true);
                editor.commit();

                isGPSon = api.isGPSOn();
                if (!isGPSon) {
                    showSettingsAlert();
                    return false;
                }
            }
        } catch(Exception e) {
            Log.e(TAG, "ERROR!!", e);
        }
        return true;
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(RouteInfoScreen.this);

        // Setting Dialog Title
        alertDialog.setTitle("GPS Not Enabled");

        // Setting Dialog Message
        alertDialog
                .setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, 0);
                    }
                });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        startNavigation();
    }

    public void startNavigation() {
        isRouteSet = true;
            Intent setIntent = new Intent(Intent.ACTION_MAIN);
            setIntent.addCategory(Intent.CATEGORY_HOME);
            setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(setIntent);
    }

    public void StartTracking(View v){
        if (!setRoute()) return;
        startNavigation();
    }

    private void cancelRoute() {
        isRouteSet = false;
        try {
            if (api != null) api.cancelRoute(1);
            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(BackgroundService.NOTIFICATION_ID);
        } catch(Exception e) {
            Log.e(TAG, "ERROR!!", e);
        }
    }
    @Override
    public void onBackPressed() {
        cancelRoute();
        super.onBackPressed();
    }
}
