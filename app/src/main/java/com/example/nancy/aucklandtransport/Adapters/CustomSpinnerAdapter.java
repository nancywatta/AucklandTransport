package com.example.nancy.aucklandtransport.Adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.nancy.aucklandtransport.R;

/**
 * This class is used to populate the location Spinner
 * around which to search for places of interest
 * with an Image and a Text.
 *
 * Created by Nancy on 10/24/14.
 */
public class CustomSpinnerAdapter extends ArrayAdapter<String> {

    Context mContext;
    Activity activity;
    String[] strings;

    int arr_images[] = { R.drawable.start_line,
            R.drawable.finish_line,
            R.drawable.finger_touch};


    public CustomSpinnerAdapter(Context context, Activity a, int textViewResourceId,   String[] objects) {
        super(context, textViewResourceId, objects);
        this.mContext = context;
        this.activity = a;
        this.strings = objects;
    }

    @Override
    public View getDropDownView(int position, View convertView,ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater=activity.getLayoutInflater();
        View row=inflater.inflate(R.layout.spinner_row, parent, false);

        TextView label=(TextView)row.findViewById(R.id.location);
        label.setText(strings[position]);

        ImageView icon=(ImageView)row.findViewById(R.id.image);
        icon.setImageResource(arr_images[position]);

        return row;
    }
}
