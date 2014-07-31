package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Nancy on 7/13/14.
 */
public class RoutesAdaptar extends BaseAdapter {
    private Activity activity;
    private ArrayList<Route> result;
    private static LayoutInflater inflater=null;

    public RoutesAdaptar(Activity a, ArrayList<Route> d) {
        activity = a;
        result=d;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return result.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView==null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.list_row, null);
            holder.text = (TextView)convertView.findViewById(R.id.title); // title
            holder.text1 = (TextView)convertView.findViewById(R.id.duration); // duration
            holder.ly = (LinearLayout)convertView.findViewById(R.id.images);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Fetching i-th route
        Route path = result.get(position);

        holder.text.setText(path.getDeparture().getTravelTime() + " - " + path.getArrival().getTravelTime() + " (" + path.getDuration() + ")");
        holder.text1.setText(path.getDistance());

        if(path.getSteps()!=null) {
            holder.ly.removeAllViews();
            populateLinks(holder.ly, path.getSteps());

            /*for(int pos=0; pos< path.getSteps().size(); pos++ ) {
                RouteStep routeStep = path.getSteps().get(pos);
                ImageView image = new ImageView(activity.getApplicationContext());
                image.setImageResource(routeStep.getIconId());
                holder.ly.addView(image);

                if(routeStep.getVehicleName() != "" && !routeStep.getVehicleName().equals("")) {
                    TextView name = new TextView(activity.getApplicationContext());
                    name.setText(routeStep.getVehicleName());
                    holder.ly.addView(name);
                }
                if(pos < path.getSteps().size() -1 ) {
                    ImageView next = new ImageView(activity.getApplicationContext());
                    next.setImageResource(R.drawable.ic_action_next_item);
                    holder.ly.addView(next);
                }
            }*/
        }

        return convertView;
    }

    static class ViewHolder {
        TextView text;
        TextView text1;
        LinearLayout ly;
    }

    private void populateLinks(LinearLayout ll, ArrayList<RouteStep> collection) {

        Display display = activity.getWindowManager().getDefaultDisplay();
        int maxWidth = display.getWidth() - 100;
        Log.d("maxWidth ", "is : " + maxWidth);

        if (collection.size() > 0) {
            LinearLayout llAlso = new LinearLayout(activity.getApplicationContext());
            llAlso.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));
            llAlso.setOrientation(LinearLayout.HORIZONTAL);

            int widthSoFar = 0;
            for(int pos=0; pos< collection.size(); pos++ ) {
                RouteStep samItem = collection.get(pos);
                ImageView image = new ImageView(activity.getApplicationContext());
                image.setImageResource(samItem.getIconId());
                image.measure(0, 0);
                widthSoFar += image.getMeasuredWidth();
                Log.d("Width ", "so far : " + widthSoFar);

                if (widthSoFar >= maxWidth) {
                    Log.d("Limit", "cross");
                    ll.addView(llAlso);

                    llAlso = new LinearLayout(activity.getApplicationContext());
                    llAlso.setLayoutParams(new LayoutParams(
                            LayoutParams.FILL_PARENT,
                            LayoutParams.WRAP_CONTENT));
                    llAlso.setOrientation(LinearLayout.HORIZONTAL);

                    llAlso.addView(image);
                    widthSoFar = image.getMeasuredWidth();
                } else {
                    llAlso.addView(image);
                }

                if(samItem.getShortName() != "" && !samItem.getShortName().equals("")) {
                    TextView name = new TextView(activity.getApplicationContext());
                    name.setText(samItem.getShortName());
                    name.measure(0,0);
                    widthSoFar += name.getMeasuredWidth();
                    Log.d("Width ", "so far : " + widthSoFar);

                    if (widthSoFar >= maxWidth) {
                        Log.d("Limit", "cross");
                        ll.addView(llAlso);

                        llAlso = new LinearLayout(activity.getApplicationContext());
                        llAlso.setLayoutParams(new LayoutParams(
                                LayoutParams.FILL_PARENT,
                                LayoutParams.WRAP_CONTENT));
                        llAlso.setOrientation(LinearLayout.HORIZONTAL);

                        llAlso.addView(name);
                        widthSoFar = name.getMeasuredWidth();
                    } else {
                        llAlso.addView(name);
                    }
                }
                if(pos < collection.size() -1 ) {
                    ImageView next = new ImageView(activity.getApplicationContext());
                    next.setImageResource(R.drawable.ic_action_next_item);
                    next.measure(0,0);
                    widthSoFar += next.getMeasuredWidth();
                    Log.d("Width ", "so far : " + widthSoFar);

                    if (widthSoFar >= maxWidth) {
                        Log.d("Limit", "cross");
                        ll.addView(llAlso);

                        llAlso = new LinearLayout(activity.getApplicationContext());
                        llAlso.setLayoutParams(new LayoutParams(
                                LayoutParams.FILL_PARENT,
                                LayoutParams.WRAP_CONTENT));
                        llAlso.setOrientation(LinearLayout.HORIZONTAL);

                        llAlso.addView(next);
                        widthSoFar = next.getMeasuredWidth();
                    } else {
                        llAlso.addView(next);
                    }
                }

            }

            ll.addView(llAlso);
        }
    }
}
