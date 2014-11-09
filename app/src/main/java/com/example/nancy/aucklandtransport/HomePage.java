package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.example.nancy.aucklandtransport.Utils.SurveyAPI;

public class HomePage extends Activity {

    private Button button;
    private SurveyAPI surveyAPI = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        button = (Button) findViewById(R.id.buttonUrl);

        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(HomePage.this, WebActivity.class);
                startActivity(intent);
            }

        });

        surveyAPI = new SurveyAPI(getApplicationContext());
        if(surveyAPI.getUsageCount())
            button.setVisibility(View.VISIBLE);
    }

    public void goToSimplePlanner(View view) {
        try {
            Intent intent = new Intent(this, MainApp.class);
            startActivity(intent);
        } catch (Exception e){
        }
    }

    public void goToTouristPlanner(View view) {
        try {
            Intent intent = new Intent(this, TouristPlanner.class);
            startActivity(intent);
        } catch (Exception e){
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.home_page, menu);
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
