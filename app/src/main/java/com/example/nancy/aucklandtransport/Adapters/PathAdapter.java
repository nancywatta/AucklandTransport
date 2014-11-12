package com.example.nancy.aucklandtransport.Adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.nancy.aucklandtransport.PathSegment;
import com.example.nancy.aucklandtransport.R;
import com.example.nancy.aucklandtransport.RouteStep;

/**
 * Path Adapter class is used to provide a customized
 * list view of the turn by turn instructions of the walking path
 * of the user's journey.
 *
 * Created by Nancy on 9/4/14.
 */
public class PathAdapter extends BaseAdapter {
    /*
     Instantiates layout XML file into the required customized list view
      */
    private LayoutInflater mInflater;
    private Context context;

    /*
    RouteStep object containing the array of walking steps
     */
    private RouteStep routeStep;

    public PathAdapter(Context context, RouteStep routeStep1) {
        mInflater = LayoutInflater.from(context);
        this.context = context;
        this.routeStep = routeStep1;
    }

    public int getCount() {
        return routeStep.getPath().size();
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
            convertView = mInflater.inflate(R.layout.path_list_row, null);
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.TextPath);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        PathSegment step = routeStep.getPath().get(position);

        if(step.getInstruction().equals("") || step.getInstruction() == ""
                || step.getInstruction() == null)
            holder.text.setText(routeStep.getDesc());
        else
            holder.text.setText(Html.fromHtml(step.getInstruction()));

        return convertView;
    }

    static class ViewHolder {
        TextView text;
    }
}
