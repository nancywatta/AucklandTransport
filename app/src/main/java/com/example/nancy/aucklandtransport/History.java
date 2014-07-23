package com.example.nancy.aucklandtransport;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

/**
 * Created by Nancy on 7/23/14.
 */
public class History {

    private static ArrayList<PlaceItem> history = null;

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

    public static ArrayList<PlaceItem> getHistory(Context context){
        if (history == null) init(context);
        return history;
    }

    public static void init(Context context) {
        SharedPreferences settings = context.getSharedPreferences(context.getString(R.string.PREFS_NAME), 0);
        Map<String, ?> all = settings.getAll();

        history = new ArrayList<PlaceItem>();

        JSONObject rec;
        String key, val;
        Object[] keys = (Object[])all.keySet().toArray();

        for (int i=0; i<keys.length; i++) {

            key = (String)keys[i];
            Log.i("HISTORY!!!!!!", "Key: "+key+" val:"+settings.getString(key, ""));
            try {
                if (key.indexOf("history_") == 0) {
                    val = settings.getString(key, "");
                    if (val.equals("")) continue;
                    rec = new JSONObject(val);
                    history.add(
                            new PlaceItem(rec.getString("name"), rec.getString("address"), rec.getInt("count"), rec.getString("coords"))
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

    }

    public static String[] getHistoryAsArray() {
        ArrayList<String> s = new ArrayList<String>();
        for (int i=0; i<history.size(); i++) {
            s.add(history.get(i).address);
        }
        String[] res = (String[])s.toArray(new String[1]);
        return res;
    }
}
