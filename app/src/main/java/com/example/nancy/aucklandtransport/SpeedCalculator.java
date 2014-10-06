package com.example.nancy.aucklandtransport;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.nancy.aucklandtransport.Utils.Constant;

import org.apache.pig.impl.util.ObjectSerializer;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Nancy on 10/3/14.
 */
public class SpeedCalculator {
    private static final String TAG = SpeedCalculator.class.getSimpleName();

    private Context mContext;
    private ArrayList<Float> speedArray;
    private ArrayList<Float> calcSpeed;

    public SpeedCalculator(Context context) {
        this.mContext = context;

        SharedPreferences prefs =
                mContext.getSharedPreferences(
                        mContext.getResources().getString(R.string.PREFS_NAME),
                        Context.MODE_PRIVATE);
        Constant.SPEED_CHECK_IND = prefs.getBoolean("SPEED_CHECK_IND", true);
        Constant.USER_SPEED = prefs.getFloat("USER_SPEED",0);
    }

    public void learnUserSpeed(float speed){
        if(speed <= 0)
            return;
        if(null == calcSpeed)
            calcSpeed = new ArrayList<Float>();
        calcSpeed.add(Float.valueOf(speed));
    }

    public float getAverageSpeed(){
        float sum = 0;
        if(!calcSpeed.isEmpty()) {
            for (Float tempSpeed : calcSpeed) {
                sum += tempSpeed.floatValue();
            }
            return sum / calcSpeed.size();
        }
        return sum;
    }

    public void saveSpeed() {
        getSpeed();

        float averageSpeed = getAverageSpeed();
        Log.d(TAG, "averageSpeed: " +  averageSpeed);

        addSpeed(averageSpeed);
        calcSpeed.clear();
    }

    public float getUserSpeed() {
        float sum = 0;
        for (Float tempSpeed : speedArray) {
            Log.d(TAG, "tempSpeed " + tempSpeed.toString());
            sum += tempSpeed.floatValue();
        }
        return sum / speedArray.size();
    }

    public void addSpeed(float speed) {
        if (null == speedArray) {
            speedArray = new ArrayList<Float>();
        }

        speedArray.add(Float.valueOf(speed));

        //save the task list to preference
        SharedPreferences prefs =
                mContext.getSharedPreferences(
                        mContext.getResources().getString(R.string.PREFS_NAME),
                        Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        try {
            if(speedArray.size() > 9) {
                Constant.SPEED_CHECK_IND = false;
                editor.putBoolean("SPEED_CHECK_IND", Constant.SPEED_CHECK_IND);
                editor.putFloat("USER_SPEED", getUserSpeed());
            }
            editor.putString("SPEEDS", ObjectSerializer.serialize(speedArray));
        } catch (IOException e) {
            e.printStackTrace();
        }
        editor.commit();
    }

    public void getSpeed() {
        if (null == speedArray) {
            speedArray = new ArrayList<Float>();
        }

        //      load tasks from preference
        SharedPreferences prefs = mContext.getSharedPreferences(
                mContext.getResources().getString(R.string.PREFS_NAME),
                Context.MODE_PRIVATE);

        try {
            speedArray = (ArrayList<Float>)
                    ObjectSerializer.deserialize(
                            prefs.getString("SPEEDS",
                                    ObjectSerializer.serialize(new ArrayList<Float>())));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
