package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.androidpn.client.Constants;

/**
 * NotificationUpdates class is the activity that is displayed
 * when user clicks on any notification in the notification
 * drawer.
 *
 * Created by Nancy on 10/13/14.
 */
public class NotificationUpdates extends Activity {

    Intent intent;
    String notifText;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_updates);

        textView = (TextView)findViewById(R.id.notifyText);
        intent = getIntent();
        notifText = intent.getStringExtra(Constants.NOTIFICATION_MESSAGE);

        textView.setText(notifText);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.notification_updates, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
