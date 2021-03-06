package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * AlertDialogManager class is used to show alert messages.
 *
 * Created by Nancy on 7/3/14.
 */
public class AlertDialogManager {
    /**
     * Function to display simple Alert Dialog
     * @param context - application context
     * @param title - alert dialog title
     * @param message - alert message
     * @param status - success/failure (used to set icon)
     *               - pass null if you don't want icon
     * */
    public void showAlertDialog(final Context context, String title, String message,
                                Boolean status) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();

        /*
        Setting Dialog Title
         */
        alertDialog.setTitle(title);

        /*
        Setting Dialog Message
         */
        alertDialog.setMessage(message);

        if(status != null)
            /*
            Setting alert dialog icon
             */
            alertDialog.setIcon((status) ? R.drawable.ic_action_done : R.drawable.ic_action_remove);

        /*
        Setting OK Button
         */
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ((Activity)context).finish();
            }
        });

        /*
        Showing Alert Message
         */
        alertDialog.show();
    }
}
