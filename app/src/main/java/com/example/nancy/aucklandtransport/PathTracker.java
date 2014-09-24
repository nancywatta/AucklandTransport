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

public class PathTracker extends Activity {

    private static final String TAG = PathTracker.class.getSimpleName();
    Intent intent;
    String message;
    String toAddress;
    String toCoords;
    TextView mMessageText;
    Button yesButton;
    Button noButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_tracker);

        mMessageText = (TextView)findViewById(R.id.message);
        yesButton = (Button)findViewById(R.id.YesButton);
        noButton = (Button)findViewById(R.id.NoButton);

        intent = getIntent();
        message = intent.getStringExtra("MESSAGE");
        toAddress = intent.getStringExtra("TO_ADDRESS");
        toCoords = intent.getStringExtra("TO_COORDS");
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
                Intent myIntent = new Intent(PathTracker.this, MainApp.class);
                myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                myIntent.putExtra("TO_ADDRESS",toAddress);
                myIntent.putExtra("TO_COORDS", toCoords);
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
