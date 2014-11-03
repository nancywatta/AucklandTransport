package com.example.nancy.aucklandtransport.Adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.nancy.aucklandtransport.R;
import com.example.nancy.aucklandtransport.Route;
import com.example.nancy.aucklandtransport.RouteStep;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by Nancy on 10/25/14.
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private Context _context;
    private ArrayList<Route> _listDataHeader; // header titles
    // child data in format of header title, child title
    //private HashMap<String, List<String>> _listDataChild;

    public ExpandableListAdapter(Context context, ArrayList<Route> listDataHeader) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        //this._listDataChild = listChildData;
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
//        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
//                .get(childPosititon);
        if(childPosititon == 0)
            return null;

        int childPos = childPosititon - 1;
        Route route = null;
        try {
            route = new Route(this._listDataHeader.get(groupPosition).getJsonString());
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return route.getSteps().get(childPos);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final RouteStep childRoute = (RouteStep) getChild(groupPosition, childPosition);

        final ViewHolder holder;
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.route_list_row, null);
            holder = new ViewHolder();
            holder.text1 = (TextView) convertView.findViewById(R.id.TextRouteDur);
            holder.image = (ImageView) convertView.findViewById(R.id.RouteInfoIcon);
            holder.text4 = (TextView) convertView.findViewById(R.id.TextRouteBus);
            holder.text5 = (TextView) convertView.findViewById(R.id.TextRouteAddress);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if(childPosition == 0) {
            holder.image.setImageResource(android.R.drawable.ic_menu_directions);
            holder.text5.setVisibility(View.GONE);
            holder.text1.setVisibility(View.GONE);
            holder.text4.setText("Go to Navigation Page");
        }
        else {
            if(childRoute ==null)
                return convertView;

            if (childRoute.isTransit()) {
                holder.text5.setVisibility(View.VISIBLE);
                holder.text1.setText(childRoute.getDeparture().getTravelTime()
                        + " - " + childRoute.getArrival().getTravelTime());

                holder.text5.setText(childRoute.getType() + " --- "
                        + childRoute.getShortName() + "(" + childRoute.getVehicleName() + ")");
                holder.text4.setText(childRoute.getDepartureStop() + " To " + childRoute.getArrivalStop());
            } else {
                holder.text5.setVisibility(View.GONE);
                holder.text1.setText(childRoute.getDistance().getTravelDistance() + " (" + childRoute.getDuration().getTravelTime() + ")");
                holder.text4.setText(childRoute.getDesc());
            }
            holder.image.setImageResource(childRoute.getIconId());
        }

        return convertView;
    }

    static class ViewHolder {
        TextView text1;
        ImageView image;
        TextView text4;
        TextView text5;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
//        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
//                .size();
        return this._listDataHeader.get(groupPosition).getSteps().size() + 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {

        Route route = (Route) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);

        String startAdd = "";
        if(route.getStartTouristName().isEmpty())
            startAdd = route.getStartAddress();
        else {
            startAdd = route.getStartTouristName();
        }
        lblListHeader.setText(startAdd);

        if(groupPosition == 0) {
            lblListHeader.setCompoundDrawablesWithIntrinsicBounds(R.drawable.start, 0, 0, 0);
        } else
            lblListHeader.setCompoundDrawablesWithIntrinsicBounds(R.drawable.from_marker, 0, 0, 0);

        TextView lblListEnd = (TextView) convertView
                .findViewById(R.id.lblListEnd);
        lblListEnd.setTypeface(null, Typeface.BOLD);

        String endAdd = "";
        if(route.getEndTouristName().isEmpty())
            endAdd = route.getEndAddress();
        else
            endAdd = route.getEndTouristName();
        lblListEnd.setText(endAdd);

        if(groupPosition != this._listDataHeader.size() -1 ) {
            lblListEnd.setCompoundDrawablesWithIntrinsicBounds(R.drawable.to_marker, 0, 0, 0);
        } else
            lblListEnd.setCompoundDrawablesWithIntrinsicBounds(R.drawable.finish, 0, 0, 0);

        TextView journeyDetails = (TextView) convertView
                .findViewById(R.id.title);

        if(route.getDeparture() != null && route.getArrival() != null)
            journeyDetails.setText(route.getDeparture().getTravelTime() +
                " - " + route.getArrival().getTravelTime() +
                " (" + route.getDuration().getTravelTime() + ")");

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
