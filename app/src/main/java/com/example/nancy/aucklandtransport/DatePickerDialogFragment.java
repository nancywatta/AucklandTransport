package com.example.nancy.aucklandtransport;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Button;
import android.widget.DatePicker;

import java.util.Calendar;

/**
 * Created by Nancy on 7/19/14.
 */
public class DatePickerDialogFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener{
    int mDay;
    int mMonth;
    int mYear;

    public DatePickerDialogFragment() {
        // Assign current Date and Time Values to Variables
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){

        /** Opening the DatePickerDialog window */
        return new DatePickerDialog(getActivity(), this, mYear, mMonth, mDay);
    }

    public void onDateSet(DatePicker view,  int yearSelected,
                          int monthOfYear, int dayOfMonth) {

        mYear = yearSelected;
        mMonth = monthOfYear;
        mDay = dayOfMonth;
        ((Button)getActivity().findViewById(R.id.button3))
                .setText(twodigits(dayOfMonth) + "/" + twodigits((monthOfYear+1)) + "/" + yearSelected);
    }

    private String twodigits(int i) {
        return (i > 9 ? "" + i : "0"+i);
    }
}
