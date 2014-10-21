package com.example.nancy.aucklandtransport;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

import java.util.HashMap;

/**
 * CustomAutoCompleteTextView is used to create a Customizing AutoCompleteTextView
 * to return Place Description corresponding to the selected item
 *
 * Created by Nancy on 7/9/14.
 */
public class CustomAutoCompleteTextView extends AutoCompleteTextView {
    public CustomAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** Returns the place description corresponding to the selected item */
    @Override
    protected CharSequence convertSelectionToString(Object selectedItem) {
        try {
            /** Each item in the autocompetetextview suggestion list is a hashmap object */
            HashMap<String, String> hm = (HashMap<String, String>) selectedItem;
            return hm.get("description");
        }catch (Exception e) {
            return (String)selectedItem;
        }
    }
}
