package com.example.nancy.aucklandtransport.Adapters;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.nancy.aucklandtransport.APIs.AucklandPublicTransportAPI;
import com.example.nancy.aucklandtransport.R;
import com.example.nancy.aucklandtransport.datatype.Route;
import com.example.nancy.aucklandtransport.datatype.RouteStep;

/**
 * RouteInfoAdapter class is used to have a customized list
 * view displaying the detailed information regarding
 * each step of the user's journey.
 *
 * Created by Nancy on 9/4/14.
 */
public class RouteInfoAdapter extends BaseAdapter {

    /*
     Instantiates layout XML file into the required customized list view
      */
    private LayoutInflater mInflater;
    private Context context;

    /*
     Route object that contains the user's journey details
      */
    private Route route;

    /*
    speedCalculator can be used to display user specific duration for walking
     */
    //private SpeedCalculator speedCalculator;

    public RouteInfoAdapter(Context context, Route route) {
        mInflater = LayoutInflater.from(context);
        this.context = context;
        this.route = route;
        //speedCalculator = new SpeedCalculator(context);
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
        final ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.route_list_row, null);
            holder = new ViewHolder();
            holder.text1 = (TextView) convertView.findViewById(R.id.TextRouteDur);
            holder.image = (ImageView) convertView.findViewById(R.id.RouteInfoIcon);
            holder.text4 = (TextView) convertView.findViewById(R.id.TextRouteBus);
            holder.text5 = (TextView) convertView.findViewById(R.id.TextRouteAddress);
            holder.btnRealTime = (ImageButton) convertView.findViewById(R.id.realTimeBtn);
            holder.realTimeData = (TextView) convertView.findViewById(R.id.realTimeTxt); // duration
            holder.mProgressView = convertView.findViewById(R.id.apiProgress);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final RouteStep step = route.getSteps().get(position);

        if (step.isTransit()) {
            holder.text5.setVisibility(View.VISIBLE);
            holder.text1.setText(step.getDeparture().getTravelTime()
                    + " - " + step.getArrival().getTravelTime());

            /*
             if the step is BUS, show link to get real time
              */
            if (step.getTransportName() == R.string.tr_bus) {
                holder.realTimeData.setVisibility(View.VISIBLE);
                holder.btnRealTime.setVisibility(View.VISIBLE);
                holder.realTimeData.setHint(Html.fromHtml("<small><small><small>" +
                        "Click refresh for Real Time" + "</small></small></small>"));

                /*
                on click listener for the Refresh Button to fetch actual bus arrival
                time from our application server
                 */
                holder.btnRealTime.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showProgress(true, holder.mProgressView, holder.realTimeData);

                        AucklandPublicTransportAPI api =
                                new AucklandPublicTransportAPI(context.getApplicationContext(),
                                        holder.realTimeData, holder.mProgressView, RouteInfoAdapter.this);

                        api.getRealTimeDate(step.getStartLoc(), step.getShortName(), step.getDeparture().getSeconds());
                    }
                });

            }
            holder.text5.setText(step.getType() + " --- "
                    + step.getShortName() + "(" + step.getVehicleName() + ")");
            holder.text4.setText(step.getDepartureStop() + " To " + step.getArrivalStop());
        } else {
            holder.realTimeData.setVisibility(View.GONE);
            holder.btnRealTime.setVisibility(View.GONE);
            holder.text5.setVisibility(View.GONE);
            holder.text1.setText(step.getDistance().getTravelDistance() + " (" + step.getDuration().getTravelTime() + ")");
            holder.text4.setText(step.getDesc());

//            float userSpeed = Constant.USER_SPEED;
//            if(userSpeed == 0) {
//                holder.text5.setVisibility(View.GONE);
//            }
//            else {
//                holder.text5.setVisibility(View.VISIBLE);
//                float time = step.getDistance().getMeters() / userSpeed;
//                holder.text5.setText(Math.round(time/60) + "");
//            }
        }
        holder.image.setImageResource(step.getIconId());

        return convertView;
    }

    static class ViewHolder {
        TextView text1;
        ImageView image;
        TextView text4;
        TextView text5;
        ImageButton btnRealTime;
        TextView realTimeData;
        View mProgressView;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show, final View mProgressView, final TextView realTimeData) {
        /*
         On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
         for very easy animations. If available, use these APIs to fade-in
         the progress spinner.
          */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = context.getResources().getInteger(android.R.integer.config_shortAnimTime);

            realTimeData.setVisibility(show ? View.GONE : View.VISIBLE);
            realTimeData.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    realTimeData.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            /*
            The ViewPropertyAnimator APIs are not available, so simply show
            and hide the relevant UI components.
             */
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            realTimeData.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

}
