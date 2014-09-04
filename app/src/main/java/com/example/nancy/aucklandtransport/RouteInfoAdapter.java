package com.example.nancy.aucklandtransport;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Nancy on 9/4/14.
 */
public class RouteInfoAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private Context context;
    private Route route;

    public RouteInfoAdapter(Context context, Route route) {
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

        if(step.isTransit()) {
            holder.text1.setText(step.getDeparture().getTravelTime()
                    + " - " + step.getArrival().getTravelTime());
            holder.text5.setText(step.getType() + " --- "
                    + step.getShortName() + "(" + step.getVehicleName() + ")");
            holder.text4.setText(step.getDepartureStop() + " To " + step.getArrivalStop());
        }
        else {
            holder.text1.setText(step.getDistance() + " (" + step.getDuration() + ")");
            holder.text4.setText(step.getDesc());
            holder.text5.setText("");
        }
        holder.image.setImageResource(step.getIconId());

        return convertView;
    }

    static class ViewHolder {
        TextView text1;
        ImageView image;
        TextView text4;
        TextView text5;
    }
}
