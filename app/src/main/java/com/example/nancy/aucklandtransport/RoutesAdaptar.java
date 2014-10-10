package com.example.nancy.aucklandtransport;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Nancy on 7/13/14.
 */
public class RoutesAdaptar extends BaseAdapter {
    private static final String TAG = RoutesAdaptar.class.getSimpleName();
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

    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if(convertView==null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.list_row, null);
            holder.text = (TextView)convertView.findViewById(R.id.title); // title
            holder.text1 = (TextView)convertView.findViewById(R.id.duration); // duration
            holder.ly = (LinearLayout)convertView.findViewById(R.id.images);
            holder.btnRealTime = (ImageButton)convertView.findViewById(R.id.realTimeButton);
            holder.realTimeData = (TextView)convertView.findViewById(R.id.realTime); // duration
            holder.mProgressView = convertView.findViewById(R.id.loginProgress);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.btnRealTime.setTag(position);

        // Fetching i-th route
        final Route path = result.get(position);

        holder.text.setText(path.getDeparture().getTravelTime() + " - " + path.getArrival().getTravelTime() + " (" + path.getDuration().getTravelTime() + ")");
        holder.text1.setText(path.getDistance());

        holder.realTimeData.setHint(Html.fromHtml("<small><small><small>" +
                "Click refresh for Real Time" + "</small></small></small>"));

        holder.btnRealTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = (Integer) v.getTag();
                Log.d(TAG, "pos: " + pos + " position: " + position);
                if(pos == position) {
                    showProgress(true, holder.mProgressView, holder.realTimeData);

                    AucklandPublicTransportAPI api =
                            new AucklandPublicTransportAPI(activity.getApplicationContext(),
                                    holder.realTimeData, holder.mProgressView, RoutesAdaptar.this);

                    for (int i = 0; i < path.getSteps().size(); i++) {
                        RouteStep routeStep = path.getSteps().get(i);
                        if (routeStep.getTransportName() == R.string.tr_bus) {
                            api.getRealTimeDate(routeStep.getStartLoc(), routeStep.getShortName(),
                                    routeStep.getDeparture().getSeconds());
                            break;
                        } else if (routeStep.isTransit()) {
                            break;
                        }
                    }
                }
            }
        });

        if(path.getSteps() != null) {
            if(path.showrealTime()) {
                holder.btnRealTime.setVisibility(View.VISIBLE);
                holder.realTimeData.setVisibility(View.VISIBLE);
            } else {
                holder.btnRealTime.setVisibility(View.GONE);
                holder.realTimeData.setVisibility(View.GONE);
            }
            holder.ly.removeAllViews();
            populateLinks(holder.ly, path.getSteps(), path);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView text;
        TextView text1;
        LinearLayout ly;
        ImageButton btnRealTime;
        TextView realTimeData;
        View mProgressView;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show, final View mProgressView, final TextView realTimeData) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = activity.getResources().getInteger(android.R.integer.config_shortAnimTime);

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
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            realTimeData.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void populateLinks(LinearLayout ll, ArrayList<RouteStep> collection,
                               Route route) {

        Display display = activity.getWindowManager().getDefaultDisplay();
        int maxWidth = display.getWidth() - 100;

        if (collection.size() > 0) {
            LinearLayout llAlso = new LinearLayout(activity.getApplicationContext());
            llAlso.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));
            llAlso.setOrientation(LinearLayout.HORIZONTAL);

            int widthSoFar = 0;
            for(int pos=0; pos< collection.size(); pos++ ) {
                RouteStep samItem = collection.get(pos);

                TextView image = new TextView(activity.getApplicationContext());
                if(samItem.isTransit()) {
                    image.setText(samItem.getDeparture().getTravelTime());
                }
                else {
                    if(pos == 0) {
                        image.setText(route.getDeparture().getTravelTime());
                    }
                    else {
                        RouteStep item = collection.get(pos-1);
                        image.setText(item.getArrival().getTravelTime());
                    }

                    //image = new ImageView(activity.getApplicationContext());
                    //((ImageView)image).setImageResource(samItem.getIconId());
                    //image.measure(0, 0);
                }
                int id = samItem.getIconId();
                image.setCompoundDrawablesWithIntrinsicBounds(
                        0, //left
                        0, //top
                        0, //right
                        id);//bottom);
                image.setTextColor(Color.parseColor("#996633"));
                image.setTypeface(null, Typeface.BOLD);
                image.measure(0, 0);
                widthSoFar += image.getMeasuredWidth();

                if (widthSoFar >= maxWidth) {
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
                    LinearLayout.LayoutParams params =
                            new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 50, 0, 0);
                    name.setLayoutParams(params);
                    name.setTextColor(Color.parseColor("#FFA500"));
                    name.setTypeface(null, Typeface.BOLD);

                    name.measure(0,0);
                    widthSoFar += name.getMeasuredWidth();

                    if (widthSoFar >= maxWidth) {
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

                    if (widthSoFar >= maxWidth) {
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
