package com.example.nancy.aucklandtransport;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

/**
 * History class is used to save the user's searches in the
 * Shared Preferences.
 *
 * Created by Nancy on 7/23/14.
 */
public class History {

    private static ArrayList<PlaceItem> history = null;
    private static ArrayList<RouteHistoryItem> routes = null;

    public static class RouteHistoryItem {
        RouteHistoryItem(String s, String e, int c, String crds, String crds2) {
            start = s; end = e; useCount = c; coords = crds; coords2 = crds2;
        }

        public String toJSON() {
            return "{ start: \""+start+"\", end: \""+end+"\", count: "+useCount+", coords: \""+coords+"\", coords2: \""+coords2+"\"  }";
        }

        public String start;
        public String end;
        public int useCount;
        public String coords, coords2;
    }

    public static String[] getHistoryAsArray() {
        ArrayList<String> s = new ArrayList<String>();
        for (int i=0; i<history.size(); i++) {
            s.add(history.get(i).address);
        }
        String[] res = (String[])s.toArray(new String[1]);
        return res;
    }

    public static ArrayList<String> getHistoryArray() {
        ArrayList<String> s = new ArrayList<String>();
        for (int i=0; i<history.size(); i++) {
            s.add(history.get(i).address);
        }
        return s;
    }

    public static class PlaceItem {
        PlaceItem(String n, String a, int c, String crds) {
            name = n; useCount = c; address = a; coords = crds;
        }

        public String toJSON() {
            return "{ name: \""+name+"\", address: \""+address+"\", count: "+useCount+", coords: \""+coords+"\" }";
        }

        public String name;
        public String address;
        public int useCount;
        public String coords;
    }

    public static void saveHistory(Context context, String address, String name, String coords) {
        SharedPreferences settings = context.getSharedPreferences(context.getString(R.string.PREFS_NAME), 0);

        String key = "history_"+address;

        String val = settings.getString(key, "");
        PlaceItem h;
        if (!val.equals("")) {
            try {
                JSONObject rec = new JSONObject(val);
                h = new PlaceItem(rec.getString("name"), rec.getString("address"), rec.getInt("count"), rec.getString("coords"));
                h.useCount++;
            }catch (Exception e) {
                Log.e("HISTORY", "Error", e);
                h = new PlaceItem(name, address, 1, coords);
            }
        } else h = new PlaceItem(name, address, 1, coords);

        if (!name.equals("")) h.name = name;
        h.address = address;

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, h.toJSON());
        editor.commit();
    }

    public static void saveRoute(Context context, String start, String end, String coords, String coords2) {
        SharedPreferences settings = context.getSharedPreferences(context.getString(R.string.PREFS_NAME), 0);

        String key = "route_"+start+"_"+end;
        String val = settings.getString(key, "");
        RouteHistoryItem r;
        if (!val.equals("")) {
            try {
                JSONObject rec = new JSONObject(val);
                r = new RouteHistoryItem(rec.getString("start"), rec.getString("end"), rec.getInt("count"), rec.getString("coords"), rec.getString("coords2"));
                r.useCount++;
            }catch (Exception e) {
                Log.e("HISTORY", "Error", e);
                r = new RouteHistoryItem(start, end, 1, coords, coords2);
            }
        } else r = new RouteHistoryItem(start, end, 1, coords, coords2);

        r.start = start; r.end = end;
        //Log.i("SAVE HISTORY saveRoute", "r.toJSON(): "+r.toJSON());
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, r.toJSON());
        editor.commit();
        //init(context);
    }

    public static ArrayList<PlaceItem> getHistory(Context context){
        //if (history == null)
        init(context);
        return history;
    }

    public static ArrayList<RouteHistoryItem> getRoutes(Context context){
        //if (routes == null)
            init(context);
        return routes;
    }

    public static void init(Context context) {
        SharedPreferences settings = context.getSharedPreferences(context.getString(R.string.PREFS_NAME), 0);
        Map<String, ?> all = settings.getAll();

        history = new ArrayList<PlaceItem>();
        routes = new ArrayList<RouteHistoryItem>();

        JSONObject rec;
        String key, val;
        Object[] keys = (Object[])all.keySet().toArray();

        for (int i=0; i<keys.length; i++) {

            key = (String)keys[i];
            try {
                if (key.indexOf("history_") == 0) {
                    val = settings.getString(key, "");
                    if (val.equals("")) continue;
                    rec = new JSONObject(val);
                    history.add(
                            new PlaceItem(rec.getString("name"), rec.getString("address"), rec.getInt("count"), rec.getString("coords"))
                    );
                } else if (key.indexOf("route_") == 0) {
                    val = settings.getString(key, "");
                    if (val.equals("")) continue;
                    rec = new JSONObject(val);
                    routes.add(
                            new RouteHistoryItem(rec.getString("start"), rec.getString("end"), rec.getInt("count"), rec.getString("coords"), rec.getString("coords2"))
                    );
                }
            } catch (Exception e) {
                Log.e("HISTORY", "Error", e);
            }
        }

        Collections.sort(history, new Comparator<PlaceItem>() {
            public int compare(PlaceItem object1, PlaceItem object2) {
                // TODO Auto-generated method stub
                return object2.useCount - object1.useCount;
            }
        });

        Collections.sort(routes, new Comparator<RouteHistoryItem>() {
            public int compare(RouteHistoryItem object1, RouteHistoryItem object2) {
                // TODO Auto-generated method stub
                return object2.useCount - object1.useCount;
            }
        });
    }

    public static class PlaceAdapter extends BaseAdapter {
        private LayoutInflater mInflater;


        public PlaceAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return History.history.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            PlaceItem h = History.getHistory(mInflater.getContext()).get(position);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.historyitem, null);
                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.HistoryName);
                holder.address = (TextView) convertView.findViewById(R.id.HistoryAddr);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            //Log.i("-------------HISTORY------", h.address+" "+h.name);
            if (h.name.equals("")) {
                holder.name.setText(h.address);
                holder.address.setText(used(h.useCount)); // +h.useCount
            } else {
                holder.name.setText(h.name);
                holder.address.setText(h.address+", "+used(h.useCount)); // +" "+h.useCount
            }
            return convertView;
        }

        static class ViewHolder {
            TextView name;
            TextView address;
        }

        public synchronized   void refreshAdapter() {
            notifyDataSetChanged();
        }
    }

    public static class RoutesAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public RoutesAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return History.routes.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            RouteHistoryItem r = History.getRoutes(mInflater.getContext()).get(position);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.historyitem, null);
                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.HistoryName);
                holder.address = (TextView) convertView.findViewById(R.id.HistoryAddr);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.name.setText(r.start+" - "+r.end);
            holder.address.setText(used(r.useCount)); // +r.useCount

            return convertView;
        }

        static class ViewHolder {
            TextView name;
            TextView address;
        }
    }

    public static String used(int t) {
        return "selected "+( t > 1 ? t+" times" : "one time");
    }

    public static void remove(Context context, String key) {
        SharedPreferences settings = context.getSharedPreferences(context.getString(R.string.PREFS_NAME), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(key);
        editor.commit();
    }
}
