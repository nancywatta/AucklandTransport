package com.example.nancy.aucklandtransport;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * Created by Nancy on 8/11/14.
 */
public class ActivityRecognitionService extends IntentService {

    private String TAG = this.getClass().getSimpleName();

    public ActivityRecognitionService() {
        super("My Activity Recognition Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)){
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            //Log.d(TAG, getType(result.getMostProbableActivity().getType()));
            Intent i = new Intent("com.example.nancy.aucklandtransport.ACTIVITY_RECOGNITION_DATA");
            i.putExtra("Activity", getType(result.getMostProbableActivity().getType()) );
            i.putExtra("Confidence", result.getMostProbableActivity().getConfidence());
            sendBroadcast(i);
        }
    }

    private String getType(int type){
        if(type == DetectedActivity.UNKNOWN)
        return "Unknown";
        else if(type == DetectedActivity.IN_VEHICLE)
        return "In Vehicle";
        else if(type == DetectedActivity.ON_BICYCLE)
        return "On Bicycle";
        else if(type == DetectedActivity.ON_FOOT)
        return "On Foot";
        else if(type == DetectedActivity.STILL)
        return "Still";
        else if(type == DetectedActivity.TILTING)
        return "Tilting";
        else
        return "";
    }


}
