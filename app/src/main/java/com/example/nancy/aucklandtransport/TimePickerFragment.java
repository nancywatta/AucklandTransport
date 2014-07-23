package com.example.nancy.aucklandtransport;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Button;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Created by Nancy on 7/1/14.
 */
public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {

    int mHour;
    int mMinute;
    boolean isTimeSet;

    public TimePickerFragment() {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        isTimeSet = false;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, mHour, mMinute,
                false);
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

        mHour = hourOfDay;
        mMinute = minute;
        isTimeSet = true;
        ((Button)getActivity().findViewById(R.id.button2)).setText(hourOfDay + ":" + minute);
    }
}