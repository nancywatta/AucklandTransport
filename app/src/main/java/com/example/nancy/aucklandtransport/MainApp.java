package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.example.nancy.aucklandtransport.History.PlaceItem;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class MainApp extends FragmentActivity {

    AutoCompleteTextView origin;
    AutoCompleteTextView destination;
    PlacesTask placesTask;
    ParserTask parserTask;
    String fromCoords= "";
    String toCoords = "";
    public final static String ADDRSTR = "com.example.nancy.aucklandtransport.ADDRESS";
    public final static String FROM_LOCATION = "com.example.nancy.aucklandtransport.FROMADDRESS";
    public final static String TO_LOCATION = "com.example.nancy.aucklandtransport.TOADDRESS";
    public final static String TIME = "com.example.nancy.aucklandtransport.TIME";
    public final static String FROM_ADDRSTR = "com.example.nancy.aucklandtransport.FROM_ADDRSTR";
    public final static String TO_ADDRSTR = "com.example.nancy.aucklandtransport.TO_ADDRSTR";
    public final static String FROM_COORDS = "com.example.nancy.aucklandtransport.FROM_COORDS";
    public final static String TO_COORDS = "com.example.nancy.aucklandtransport.TO_COORDS";
    public final static String ORIGIN = "com.example.nancy.aucklandtransport.ORIGIN";
    public final static String ISDEPARTURE = "com.example.nancy.aucklandtransport.ISDEPARTURE";
    static final int PICK_ADDRESS_REQUEST = 1;

    // Connection detector class
    ConnectionDetector cd;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();

    // flag for Internet connection status
    Boolean isInternetPresent = false;

    int mDay;
    int mMonth;
    int mYear;
    int hour;
    int minute;
    TimePickerFragment timeFragment = null;
    DatePickerDialogFragment dateFragment = null;
    SharedPreferences prefs;

    private ArrayAdapter<String> autoCompleteAdapter;
    ArrayList<PlaceItem> history;

    private void locationConsent(boolean showDialog) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // if run for the first time, ask for coords permissions
        String a = prefs.getString("allowLoc", "");
        if (a.equals("") && showDialog) {
            final SharedPreferences.Editor editor = prefs.edit();

            AlertDialog alertDialog = new AlertDialog.Builder(MainApp.this).create();
            alertDialog.setTitle(getString(R.string.allowLocTitle));
            alertDialog.setMessage(getString(R.string.allowLocText));
            alertDialog.setButton(getString(R.string.allowLocAgree), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    editor.putString("allowLoc", "Yes");
                    editor.commit();
                } });
            alertDialog.setButton2(getString(R.string.allowLocDisagree), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    editor.putString("allowLoc", "No");
                    editor.commit();
                } });
            alertDialog.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check whether we're recreating a previously destroyed instance
        setContentView(R.layout.activity_main_app);
        // creating connection detector class instance
        cd = new ConnectionDetector(getApplicationContext());
        // Check if Internet present
        isInternetPresent = cd.isConnectingToInternet();
        if (!isInternetPresent) {
            // Internet Connection is not present
            alert.showAlertDialog(MainApp.this, "Internet Connection Error",
                    "Please connect to working Internet connection", false);
            // stop executing code by return
            return;
        }

        locationConsent(true);
        history = History.getHistory(this);

        autoCompleteAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, History.getHistoryAsArray());

        origin = (AutoCompleteTextView) findViewById(R.id.editText1);
        origin.setThreshold(1);

        origin.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                placesTask = new PlacesTask();
                placesTask.execute(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }

        });

        /*origin.setAdapter(autoCompleteAdapter);

        origin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    origin.showDropDown();
            }
        });

        origin.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if(hasFocus){
                    origin.showDropDown();
                }
            }
        });
        */

        origin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    destination.requestFocus();
                    return true;
                }
                return false;
            }
        });

        destination = (AutoCompleteTextView) findViewById(R.id.editText2);
        destination.setThreshold(1);

        destination.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                placesTask = new PlacesTask();
                placesTask.execute(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }
        });

        destination.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(destination.getWindowToken(),
                            InputMethodManager.RESULT_UNCHANGED_SHOWN);
                    return true;
                }
                return false;
            }
        });

        /*destination.setAdapter(autoCompleteAdapter);

        destination.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if(hasFocus){
                    destination.showDropDown();
                }
            }
        }); */

        Calendar calendar = Calendar.getInstance();
        ((Button)findViewById(R.id.button2)).setText(calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE));
        ((Button)findViewById(R.id.button3)).setText(calendar.get(Calendar.DAY_OF_MONTH) + "/" +
                calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR));

        if (savedInstanceState != null) {
            // Restore value of members from saved state
            origin.setText(savedInstanceState.getString("From Location"));
            destination.setText(savedInstanceState.getString("To Location"));
        }

        Bundle b = getIntent().getExtras();
        if (b != null) {
            locationConsent(false);
        }

    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches all places from GooglePlaces AutoComplete Web Service
    private class PlacesTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... place) {
            // For storing data from web service
            String data = "";

            String key = "key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw";

            String input="";

            try {
                input = "input=" + URLEncoder.encode(place[0], "utf-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }

            // place type to be searched
            String types = "types=geocode";

            // Sensor enabled
            String sensor = "sensor=false";

            // Building the parameters to the web service
            String parameters = input+"&"+types+"&"+sensor+"&"+key;

            // Output format
            String output = "json";

            // Building the url to the web service
            String url = "https://maps.googleapis.com/maps/api/place/autocomplete/"+output+"?"+parameters;

            try{
                // Fetching the data from web service
                data = downloadUrl(url);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // Creating ParserTask
            parserTask = new ParserTask();

            // Starting Parsing the JSON string returned by Web Service
            parserTask.execute(result);
        }
    }
    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>>{

        JSONObject jObject;

        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;

            PlaceJSONParser placeJsonParser = new PlaceJSONParser();

            try{
                jObject = new JSONObject(jsonData[0]);

                // Getting the parsed data as a List construct
                places = placeJsonParser.parse(jObject);

            }catch(Exception e){
                Log.d("Exception", e.toString());
            }
            return places;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {

            String[] from = new String[] { "description"};
            int[] to = new int[] { android.R.id.text1 };

            // Creating a SimpleAdapter for the AutoCompleteTextView
            SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), result, android.R.layout.simple_list_item_1, from, to);

            // Setting the adapter
            origin.setEllipsize(TextUtils.TruncateAt.END);
            origin.setSingleLine();
            origin.setHorizontallyScrolling(true);
            origin.setAdapter(adapter);
            destination.setAdapter(adapter);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_app, menu);
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

    public void showMapOfFromLoc(View v) {
        AutoCompleteTextView origin1 = (AutoCompleteTextView) findViewById(R.id.editText1);
        try {
            String fromAddress = origin1.getText().toString(); // Get address
            fromAddress = fromAddress.replace(' ' , '+');
            //Intent geoIntent = new Intent (android.content.Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + fromAddress));  Prepare intent
            Intent intent = new Intent(this, DisplayMapActivity.class);
            intent.putExtra(ADDRSTR, fromAddress);
            intent.putExtra(ORIGIN, true);
            startActivityForResult(intent, PICK_ADDRESS_REQUEST);
            } catch (Exception e){
        }
    }

    public void showMapOfToLoc(View v) {
        try {
            String toAddress = destination.getText().toString(); // Get address
            toAddress = toAddress.replace(' ' , '+');
            Intent intent = new Intent(this, DisplayMapActivity.class);
            intent.putExtra(ADDRSTR, toAddress);
            intent.putExtra(ORIGIN, false);
            startActivityForResult(intent, PICK_ADDRESS_REQUEST);
        } catch (Exception e){
        }
    }

    public void onRadioButtonClicked(View view) {

    }

    public void showAllRoutes(View view) {
        RadioButton leaveAfter = (RadioButton) findViewById(R.id.radioButton2);

        try {
            String toAddress = destination.getText().toString(); // Get address
            //toAddress = toAddress.replace(' ' , '+');
            String fromAddress = origin.getText().toString(); // Get address
            //fromAddress = fromAddress.replace(' ' , '+');

            //Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            Calendar calendar = Calendar.getInstance(Locale.getDefault());

            if(dateFragment!=null && timeFragment !=null) {
                calendar.clear();
                calendar.set(Calendar.MONTH, dateFragment.mMonth);
                calendar.set(Calendar.YEAR, dateFragment.mYear);
                calendar.set(Calendar.DAY_OF_MONTH, dateFragment.mDay);
                calendar.set(Calendar.HOUR_OF_DAY, timeFragment.mHour);
                calendar.set(Calendar.MINUTE, timeFragment.mMinute);
            }
            else if(timeFragment !=null) {
                calendar.set(Calendar.HOUR_OF_DAY, timeFragment.mHour);
                calendar.set(Calendar.MINUTE, timeFragment.mMinute);
            }
            else if (dateFragment!=null){
                calendar.set(Calendar.MONTH, dateFragment.mMonth);
                calendar.set(Calendar.YEAR, dateFragment.mYear);
                calendar.set(Calendar.DAY_OF_MONTH, dateFragment.mDay);
            }

            long secondsSinceEpoch = calendar.getTimeInMillis() / 1000L;

            Log.d("MainApp", "secondsSinceEpoch " + secondsSinceEpoch +
                     " Hour: " + calendar.get(Calendar.HOUR_OF_DAY)
            + " Month: " + calendar.get(Calendar.MONTH) + " Year: " + calendar.get(Calendar.YEAR) + " Date: " + calendar.get(Calendar.DAY_OF_MONTH)
            + " Day of Month: " + calendar.get(Calendar.DAY_OF_MONTH) + " " + calendar.getTime());

            Intent intent = new Intent(this, RoutesActivity.class);
            intent.putExtra(FROM_LOCATION, fromAddress);
            intent.putExtra(TO_LOCATION, toAddress);
            intent.putExtra(TIME, secondsSinceEpoch);
            intent.putExtra(FROM_COORDS, fromCoords);
            intent.putExtra(TO_COORDS, toCoords);
            if(leaveAfter.isChecked() == true)
                intent.putExtra(ISDEPARTURE, true);
            else
                intent.putExtra(ISDEPARTURE, false);
            startActivity(intent);
        } catch (Exception e){
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (PICK_ADDRESS_REQUEST) : {
                if (resultCode == Activity.RESULT_OK) {
                    // TODO Extract the data returned from the child Activity.
                    String fromAddr = data.getStringExtra(FROM_ADDRSTR);
                    if(fromAddr!=null && !fromAddr.equals(""))
                        origin.setText(fromAddr);
                    String toAddr = data.getStringExtra(TO_ADDRSTR);
                    if(toAddr!=null && !toAddr.equals(""))
                        destination.setText(toAddr);
                    String fCoords = data.getStringExtra(FROM_COORDS);
                    if(fCoords!=null)
                        fromCoords = fCoords;
                    String tCoords = data.getStringExtra(TO_COORDS);
                    if(tCoords!=null)
                        toCoords = tCoords;
                }
                break;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);

        // Save the user's current game state
        savedInstanceState.putString("From Location", origin.getText().toString());
        savedInstanceState.putString("To Location", destination.getText().toString());
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
        origin.setText(savedInstanceState.getString("From Location"));
        destination.setText(savedInstanceState.getString("To Location"));
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        /*SharedPreferences settings = getSharedPreferences(getString(R.string.appSettings), MODE_PRIVATE);

        origin = (AutoCompleteTextView) findViewById(R.id.editText1);
        //Initialize to the default value if first run or restore the saved value
        origin.setText(settings.getString(getString(R.string.fromLocation), null)); */
    }

    @Override
    protected void onResume() {
        super.onResume();

        locationConsent(false);
    }


    @Override
    protected void onStop()
    {
        super.onStop();

        /*SharedPreferences settings = getSharedPreferences(getString(R.string.appSettings), MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        //Save Value
        editor.putString(getString(R.string.fromLocation), origin.getText().toString());
        editor.commit(); */
    }

    public void showTimePickerDialog(View v) {
        timeFragment = new TimePickerFragment();
        timeFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void showDatePickerDialog(View v) {
        dateFragment = new DatePickerDialogFragment();
        dateFragment.show(getSupportFragmentManager(), "datePicker");
    }

}
