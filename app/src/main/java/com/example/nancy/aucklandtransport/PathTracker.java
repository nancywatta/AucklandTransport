package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.nancy.aucklandtransport.Utils.Constant;

import java.util.Calendar;
import java.util.Locale;

/**
 * PathTracker class is used to provide an option to user to
 * change route.
 *
 * Created by Nancy on 7/28/14.
 */
public class PathTracker extends Activity {

    private static final String TAG = PathTracker.class.getSimpleName();
    Intent intent;
    String message;
    String fromAddress;
    String toAddress;
    String fromCoords;
    String toCoords;
    TextView mMessageText;
    Button yesButton;
    Button noButton;
    Boolean isBusNotify;
    int busIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_tracker);

        mMessageText = (TextView)findViewById(R.id.message);
        yesButton = (Button)findViewById(R.id.YesButton);
        noButton = (Button)findViewById(R.id.NoButton);

        intent = getIntent();
        message = intent.getStringExtra("MESSAGE");
        fromAddress = intent.getStringExtra(Constant.FROM_LOCATION);
        toAddress = intent.getStringExtra(Constant.TO_LOCATION);
        fromCoords = intent.getStringExtra(Constant.FROM_COORDS);
        toCoords = intent.getStringExtra(Constant.TO_COORDS);
        isBusNotify = intent.getBooleanExtra("IS_VEHICLE", false);
        busIndex = intent.getIntExtra("BUS_INDEX",-1);

        if(isBusNotify) {
            noButton.setVisibility(View.GONE);
            yesButton.setText("OK");
            RouteInfoFragment.onBoardBtn.setVisibility(View.VISIBLE);
        }

        mMessageText.setText(message);

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Change Route!!!");
//                Intent myIntent = new Intent(PathTracker.this, MainApp.class);
//                myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                myIntent.putExtra("TO_ADDRESS",toAddress);
//                myIntent.putExtra("TO_COORDS", toCoords);
                Intent myIntent = new Intent(PathTracker.this, AlternateRoute.class);
                myIntent.putExtra(Constant.FROM_LOCATION, fromAddress);
                myIntent.putExtra(Constant.TO_LOCATION, toAddress);
                Calendar c = Calendar.getInstance(Locale.getDefault());
                myIntent.putExtra(Constant.TIME, (c.getTimeInMillis()/ 1000L));
                myIntent.putExtra(Constant.FROM_COORDS, fromCoords);
                myIntent.putExtra(Constant.TO_COORDS, toCoords);
                myIntent.putExtra("BUS_INDEX", busIndex);
                startActivity(myIntent);
                finish();
                return;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.path_tracker, menu);
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
