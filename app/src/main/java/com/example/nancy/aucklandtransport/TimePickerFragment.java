package com.example.nancy.aucklandtransport;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Button;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * TimePickerFragment class is used to create a dialog that prompts the user for the time of day
 * using a view for selecting the time of day, in AM/PM mode.
 * The hour, each minute digit, and AM/PM (if applicable) can be controlled by vertical spinners.
 *
 * Created by Nancy on 7/1/14.
 */
public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {

    int mHour;
    int mMinute;
    boolean isTimeSet;
    int buttonId;

    public TimePickerFragment() {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        isTimeSet = false;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        buttonId = getArguments().getInt("ButtonId");
        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, mHour, mMinute,
                false);
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

        mHour = hourOfDay;
        mMinute = minute;
        isTimeSet = true;

        // set the Button text to the time selected
        ((Button)getActivity().findViewById(buttonId))
                .setText(twodigits(hourOfDay) + ":" + twodigits(minute));
    }

    private String twodigits(int i) {
        return (i > 9 ? "" + i : "0"+i);
    }
}