package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

    private String routeString;
    Route route = null;
    ListView listView;

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
                /* convertView = mInflater.inflate(R.layout.route_list_item, null); */
                convertView = mInflater.inflate(R.layout.route_list_row, null);
                holder = new ViewHolder();
                /* holder.text = (TextView) convertView.findViewById(R.id.TextRouteDep); */
                holder.text1 = (TextView) convertView.findViewById(R.id.TextRouteDur);
                holder.image = (ImageView) convertView.findViewById(R.id.RouteInfoIcon);
                /* holder.text2 = (TextView) convertView.findViewById(R.id.TextRouteLen);
                holder.text3 = (TextView) convertView.findViewById(R.id.TextRouteArr); */
                holder.text4 = (TextView) convertView.findViewById(R.id.TextRouteBus);
                holder.text5 = (TextView) convertView.findViewById(R.id.TextRouteAddress);
                /*holder.row1 = (TableRow) convertView.findViewById(R.id.RouteInfoRow1);
                holder.row2 = (TableRow) convertView.findViewById(R.id.RouteInfoRow2); */
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            RouteStep step = route.getSteps().get(position);

            if(!step.getDepTime().equals("")) {
                /*holder.text.setText(step.getDepTime());*/
                holder.text1.setText(step.getDepTime() + " - " + step.getArrTime());
            }
            else
                holder.text1.setText(step.getDistance() + " (" + step.getDuration() + ")");
            holder.image.setImageResource(step.getIconId());

            /*holder.text2.setText(step.getDistance());

            holder.text3.setText(step.getArrTime()); */

           /* if (step.firstLoc != null && !step.firstLoc.equals("null")
                    && step.lastLoc != null && !step.lastLoc.equals("null"))
                holder.text5.setText(step.firstLoc + " --- " + step.lastLoc);
            else holder.text5.setText(""); */

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
        getRoute();
        listView = (ListView) findViewById(R.id.RouteInfoScreenListView);
        if(route!=null)
        listView.setAdapter(new EfficientAdapter(RouteInfoScreen.this, route));
    }

    private void getRoute() {
        SharedPreferences settings = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        try {
            routeString = settings.getString("route", "");

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
}
