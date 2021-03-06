package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TabHost;
import android.widget.TextView;

import com.example.nancy.aucklandtransport.APIs.GoogleAPI;
import com.example.nancy.aucklandtransport.BackgroundJobs.GPSTracker;
import com.example.nancy.aucklandtransport.BackgroundTask.GooglePlacesTask;
import com.example.nancy.aucklandtransport.History.PlaceItem;
import com.example.nancy.aucklandtransport.History.RouteHistoryItem;
import com.example.nancy.aucklandtransport.Utils.ConnectionDetector;
import com.example.nancy.aucklandtransport.Utils.Constant;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * MainApp class is the activity asking for user to enter the origin
 * and destination of the journey to be planned.
 * The activity also allows a user to select a place in the origin or destination
 * from the Recent Places Tab.
 * It also allows a user to select the entire route from the Recent Route Tab.
 *
 * Created by Nancy on 7/3/14.
 */
public class MainApp extends FragmentActivity {

    /*
    Debugging tag for the MainApp class
     */
    private static final String TAG = MainApp.class.getSimpleName();

    /*
    AutoCompleteTextView for the departure location
     */
    AutoCompleteTextView origin;

    /*
    AutoCompleteTextView for the end location
     */
    AutoCompleteTextView destination;

    Button date;
    Button time;
    String fromCoords ="";
    String toCoords="";
    String prefix="";

    /*
    Connection detector class
     */
    ConnectionDetector cd;

    /*
    Alert Dialog Manager
     */
    AlertDialogManager alert = new AlertDialogManager();

    /*
    flag for Internet connection status
     */
    Boolean isInternetPresent = false;

    /*
    Time picker fragment for departure/arrival time
     */
    TimePickerFragment timeFragment = null;

    /*
    Date picker fragment for journey date
     */
    DatePickerDialogFragment dateFragment = null;

    SharedPreferences prefs;

    ArrayList<PlaceItem> history;
    ArrayList<RouteHistoryItem> routes;
    private int lastSelectedRoute = -1;
    ListView myPlaces, myRoutes;
    History.PlaceAdapter placeAdapter;
    History.RoutesAdapter routesAdapter;
    //private ArrayAdapter<String> autoCompleteAdapter;

    private int lastSelectedPlace = -1;

    GPSTracker gps;
    GoogleAPI googleAPI;

    private Boolean launchedFromParamsAlready = false;
    private String lastLocDisc = "";

    private IBackgroundServiceAPI api = null;

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
        routes = History.getRoutes(this);

        // Getting reference to AutoCompleteTextView to get the user input origin location
        origin = (AutoCompleteTextView) findViewById(R.id.editText1);

        // Get the User's Current Location
        gps = new GPSTracker(getBaseContext());

        googleAPI = new GoogleAPI();

        // Convert the users current location coordinates into human readable address
        googleAPI.getReverseGeocode(new LatLng(gps.getLatitude(), gps.getLongitude()),
                getString(R.string.API_KEY));

        if(googleAPI.getCurrentAddress() != null)
            origin.setText(googleAPI.getCurrentAddress());

        origin.setThreshold(1);

        // Adding textWatcher to provide place predictions as user types.
        origin.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                prefix = s.toString();
                GooglePlacesTask placesTask = new GooglePlacesTask(getBaseContext(),
                        origin, prefix);
                placesTask.execute(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Auto-generated method stub
            }

        });

        /*
        Move cursor to destination textbox when click on Next button
         on Keyboard
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

        /*
        Clear the text of origin when user clicks on delete Button
         */
        origin.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;

                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (origin.getRight() - origin.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        origin.setText("");
                    }
                }
                return false;
            }
        });

        // Getting reference to AutoCompleteTextView to get the user input destination location
        destination = (AutoCompleteTextView) findViewById(R.id.editText2);
        destination.setThreshold(1);

        // Adding textWatcher to provide place predictions as user types.
        destination.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                prefix = s.toString();
                GooglePlacesTask placesTask = new GooglePlacesTask(getBaseContext(),
                        destination, prefix);
                placesTask.execute(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Auto-generated method stub
            }
        });

        // Hide Keyboard when user is done with typing
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

        // Clear the text of destination when user clicks on delete Button
        destination.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;

                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (destination.getRight() - destination.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        destination.setText("");
                    }
                }
                return false;
            }
        });

        TabHost tabs = (TabHost)findViewById(R.id.TabHost01);
        tabs.setup();

        // Adding Tab for recent places and routes stored in Shared Preferences
        Log.d(TAG, "size" + history.size());
        if (history.size() > 0 || routes.size() > 0) {

            TabHost.TabSpec spec1 = tabs.newTabSpec("tag1");

            spec1.setContent(R.id.myPlacesList);

            View tabIndicator = LayoutInflater.from(this).inflate(R.layout.tab_indicator, tabs.getTabWidget(), false);
            ((TextView) tabIndicator.findViewById(R.id.title)).setText(R.string.TabPlaces);
            ((ImageView) tabIndicator.findViewById(R.id.icon)).setImageResource(android.R.drawable.ic_menu_mylocation);

            spec1.setIndicator(tabIndicator);

            tabs.addTab(spec1);

            TabHost.TabSpec spec2 = tabs.newTabSpec("tag2");
            spec2.setContent(R.id.myRoutesList);
            View tabIndicator2 = LayoutInflater.from(this).inflate(R.layout.tab_indicator, tabs.getTabWidget(), false);
            ((TextView) tabIndicator2.findViewById(R.id.title)).setText(R.string.TabRoutes);
            ((ImageView) tabIndicator2.findViewById(R.id.icon)).setImageResource(android.R.drawable.ic_menu_myplaces);
            spec2.setIndicator(tabIndicator2);

            tabs.addTab(spec2);
        } else tabs.setVisibility(View.GONE);

        // getting reference for the ListView to display recent Places
        myPlaces = (ListView)findViewById(R.id.myPlacesList);

        // getting reference for the ListView to display recent Routes
        myRoutes = (ListView)findViewById(R.id.myRoutesList);

        placeAdapter = new History.PlaceAdapter(this);
        refreshYourAdapter();
        routesAdapter = new History.RoutesAdapter(this);

        myPlaces.setAdapter(placeAdapter);
        myRoutes.setAdapter(routesAdapter);

        myPlaces.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {

                lastSelectedPlace = arg2;

                final String[] items = new String[]{
                        getString(R.string.TabPlacesMenuFrom),
                        getString(R.string.TabPlacesMenuTo),
                        getString(R.string.TabPlacesMenuDelete)
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(MainApp.this);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        PlaceItem h = history.get(lastSelectedPlace);
                        switch(which) {
                            case 0: origin.setText(h.address);
                                fromCoords = h.coords;
                                break;
                            case 1: destination.setText(h.address);
                                toCoords = h.coords;
                                break;
                            case 2:
                                History.remove(MainApp.this, "history_"+history.get(lastSelectedPlace).address);
                                History.init(MainApp.this);
                                history = History.getHistory(MainApp.this);
                                placeAdapter.notifyDataSetChanged();
                                break;
                        }

                    }
                });
                builder.show();
            }
        });

        myRoutes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                lastSelectedRoute = arg2;
                final String[] items = new String[]{
                        getString(R.string.TabRoutesMenuSet),
                        getString(R.string.TabRoutesMenuSetBackwards),
                        getString(R.string.TabRoutesMenuDelete)
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(MainApp.this);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        History.RouteHistoryItem r = routes.get(lastSelectedRoute);
                        switch(which) {
                            case 0:
                                origin.setText(r.start);
                                destination.setText(r.end);
                                fromCoords = r.coords;
                                toCoords = r.coords2;
                                break;
                            case 1:
                                origin.setText(r.end);
                                destination.setText(r.start);
                                fromCoords = r.coords2;
                                toCoords = r.coords;
                                break;
                            case 2:
                                History.remove(MainApp.this, "route_"+r.start+"_"+r.end);
                                History.init(MainApp.this);
                                routes = History.getRoutes(MainApp.this);
                                routesAdapter.notifyDataSetChanged();
                                break;
                        }

                    }
                });
                builder.show();
            }
        });

//        autoCompleteAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, History.getHistoryAsArray());
//
//        origin.setAdapter(autoCompleteAdapter);
//        destination.setAdapter(autoCompleteAdapter);

        time = (Button)findViewById(R.id.button2);
        date = (Button)findViewById(R.id.button3);

        if (savedInstanceState != null) {
            // Restore value of members from saved state
            origin.setText(savedInstanceState.getString("From Location"));
            destination.setText(savedInstanceState.getString("To Location"));
            date.setText(savedInstanceState.getString("Date"));
            time.setText(savedInstanceState.getString("Time"));
        }
        else
        {
            Calendar calendar = Calendar.getInstance();
            time.setText(twodigits(calendar.get(Calendar.HOUR_OF_DAY)) + ":" + twodigits(calendar.get(Calendar.MINUTE)));
            int month = calendar.get(Calendar.MONTH) +1;
            date.setText(twodigits(calendar.get(Calendar.DAY_OF_MONTH)) + "/" +
                    twodigits(month) + "/" + calendar.get(Calendar.YEAR));
        }

        Log.i(TAG, "trying to bind service "+BackgroundService.class.getName());
        Intent servIntent = new Intent(BackgroundService.class.getName());
        startService(servIntent);
        Log.i(TAG, "starting service "+servIntent.toString());
        bindService(servIntent, serviceConection, 0);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            locationConsent(false);
        }

    }

    private void refreshYourAdapter() {
        runOnUiThread(new Runnable() {
            public void run() {
                placeAdapter.refreshAdapter();
            }
        });
    }

    private String twodigits(int i) {
        return (i > 9 ? "" + i : "0"+i);
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
        if (id == R.id.action_home) {
            Intent exploreActivity = new Intent(MainApp.this, HomePage.class);
            startActivity(exploreActivity);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Function to open Display Map Activity page to set origin
     *
     * @param v
     */
    public void showMapOfFromLoc(View v) {
        AutoCompleteTextView origin1 = (AutoCompleteTextView) findViewById(R.id.editText1);
        try {
            String fromAddress = origin1.getText().toString(); // Get address
            Intent intent = new Intent(this, DisplayMapActivity.class);
            intent.putExtra(Constant.ADDRSTR, fromAddress);
            intent.putExtra(Constant.ORIGIN, true);
            startActivityForResult(intent, Constant.PICK_ADDRESS_REQUEST);
            } catch (Exception e){
        }
    }

    /**
     * Function to open Display Map Activity page to set Destination
     *
     * @param v
     */
    public void showMapOfToLoc(View v) {
        try {
            String toAddress = destination.getText().toString(); // Get address
            Intent intent = new Intent(this, DisplayMapActivity.class);
            intent.putExtra(Constant.ADDRSTR, toAddress);
            intent.putExtra(Constant.ORIGIN, false);
            startActivityForResult(intent, Constant.PICK_ADDRESS_REQUEST);
        } catch (Exception e){
        }
    }

    public void onRadioButtonClicked(View view) {

    }

    public void showAllRoutes(View view) {
        RadioButton leaveAfter = (RadioButton) findViewById(R.id.radioButton2);

        try {
            String toAddress = destination.getText().toString(); // Get address
            String fromAddress = origin.getText().toString(); // Get address

            // If End address empty, give validation Error
            if(toAddress.equals("") || toAddress==null) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainApp.this).create();
                alertDialog.setTitle("Validation Error");
                alertDialog.setMessage("Please enter the Destination");
                alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    } });
                alertDialog.show();

            }
            else {
                // If from address empty, take current location
                if (fromAddress.equals("") || fromAddress == null) {

                    // Get Current Location
                    if(googleAPI.getCurrentAddress() != null)
                        fromAddress = googleAPI.getCurrentAddress();
                    Log.d(TAG, "fromAdd " + fromAddress);
                }

                Calendar calendar = Calendar.getInstance(Locale.getDefault());

                if (dateFragment != null && timeFragment != null) {
                    calendar.clear();
                    calendar.set(Calendar.MONTH, dateFragment.mMonth);
                    calendar.set(Calendar.YEAR, dateFragment.mYear);
                    calendar.set(Calendar.DAY_OF_MONTH, dateFragment.mDay);
                    calendar.set(Calendar.HOUR_OF_DAY, timeFragment.mHour);
                    calendar.set(Calendar.MINUTE, timeFragment.mMinute);
                } else if (timeFragment != null) {
                    calendar.set(Calendar.HOUR_OF_DAY, timeFragment.mHour);
                    calendar.set(Calendar.MINUTE, timeFragment.mMinute);
                } else if (dateFragment != null) {
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
                intent.putExtra(Constant.FROM_LOCATION, fromAddress);
                intent.putExtra(Constant.TO_LOCATION, toAddress);
                intent.putExtra(Constant.TIME, secondsSinceEpoch);
                intent.putExtra(Constant.FROM_COORDS, fromCoords);
                intent.putExtra(Constant.TO_COORDS, toCoords);
                if (leaveAfter.isChecked() == true)
                    intent.putExtra(Constant.ISDEPARTURE, true);
                else
                    intent.putExtra(Constant.ISDEPARTURE, false);
                startActivity(intent);
            }
        } catch (Exception e){
            e.printStackTrace();

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        placeAdapter.notifyDataSetChanged();
        switch(requestCode) {
            case (Constant.PICK_ADDRESS_REQUEST) : {
                if (resultCode == Activity.RESULT_OK) {
                    // Extract the data returned from the child Activity.
                    String fromAddr = data.getStringExtra(Constant.FROM_ADDRSTR);
                    if(fromAddr!=null && !fromAddr.equals(""))
                        origin.setText(fromAddr);
                    String toAddr = data.getStringExtra(Constant.TO_ADDRSTR);
                    if(toAddr!=null && !toAddr.equals(""))
                        destination.setText(toAddr);

                    String fCoords = data.getStringExtra(Constant.FROM_COORDS);
                    Log.d(TAG, "fCoords" + fCoords);
                    if(fCoords!=null && fCoords!="")
                        fromCoords = fCoords;
                    String tCoords = data.getStringExtra(Constant.TO_COORDS);
                    Log.d(TAG, "tCoords" + tCoords);
                    if(tCoords!=null && tCoords!="")
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
        savedInstanceState.putString("Date", date.getText().toString());
        savedInstanceState.putString("Time", time.getText().toString());
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
        origin.setText(savedInstanceState.getString("From Location"));
        destination.setText(savedInstanceState.getString("To Location"));
        date.setText(savedInstanceState.getString("Date"));
        time.setText(savedInstanceState.getString("Time"));
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        locationConsent(false);
        myPlaces = (ListView)findViewById(R.id.myPlacesList);
        history = History.getHistory(this);

        placeAdapter = new History.PlaceAdapter(this);
        refreshYourAdapter();
        myPlaces.setAdapter(placeAdapter);
    }


    @Override
    protected void onStop()
    {
        super.onStop();
    }

    public void showTimePickerDialog(View v) {
        if(timeFragment==null)
            timeFragment = new TimePickerFragment();
        Bundle bdl = new Bundle(2);
        bdl.putInt("ButtonId", R.id.button2);
        timeFragment.setArguments(bdl);
        timeFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void showDatePickerDialog(View v) {
        if(dateFragment==null)
            dateFragment = new DatePickerDialogFragment();
        Bundle bdl = new Bundle(2);
        bdl.putInt("ButtonId", R.id.button3);
        dateFragment.setArguments(bdl);
        dateFragment.show(getSupportFragmentManager(), "datePicker");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (api != null) api.removeListener(serviceListener);
            unbindService(serviceConection);
        } catch(Exception e) {
            Log.e(TAG, "ERROR!!", e);
        }

        Log.i(TAG, "unbind ");
    }

    /**
     * Service interaction stuff
     */
    private IBackgroundServiceListener serviceListener = new IBackgroundServiceListener.Stub() {

        public void locationDiscovered(double lat, double lon)
                throws RemoteException {
            Log.i(TAG, "locationDiscovered: "+lat+" "+lon);
            lastLocDisc = lon+","+lat;
            if (fromCoords.equals(""))
                fromCoords = lon+","+lat;
        }

        public void handleGPSUpdate(double lat, double lon, float angle) throws RemoteException {
            if (fromCoords.equals(""))
                fromCoords = lon+","+lat;
        }

        public void addressDiscovered(String address) throws RemoteException {
            if (launchedFromParamsAlready) return;
            Bundle b = getIntent().getExtras();
            if (b != null) {
                Log.i(TAG, "addressDiscovered: "+address);
                if (TextUtils.isEmpty(origin.getText())) {
                    if (address.equals("")) {
                        origin.setHint(getString(R.string.EditHintLocating));
                    } else {
                        origin.setHint(address);
                    }
                }
                Log.i(TAG, "Bundle: "+b);
                String toAddress = b.getString("TO_ADDRESS");
                String toCoordsInt = b.getString("TO_COORDS");

                if (toAddress != null && !lastLocDisc.equals("")) {
                    toCoords = toCoordsInt;
                    fromCoords = lastLocDisc;
                    locationConsent(false);
                    launchNextActivity(address, toAddress);
                    launchedFromParamsAlready = true;
                }
            }
        }
    };

    private void launchNextActivity(String fromAddress, String toAddress) {
        // only if we successfully retrieved both from and to
        // coordinates, start the new activity
        Log.i(TAG, "launchNextActivity fromCoords:"+fromCoords+" toCoords:"+toCoords);
        if (fromCoords.compareTo("")!=0 && toCoords.compareTo("")!=0
                && fromAddress.compareTo("")!=0 && toAddress.compareTo("")!=0) {

            Intent myIntent = new Intent(MainApp.this, RoutesActivity.class);
            myIntent.putExtra(Constant.FROM_LOCATION, fromAddress);
            myIntent.putExtra(Constant.TO_LOCATION, toAddress);
            Calendar c = Calendar.getInstance(Locale.getDefault());
            myIntent.putExtra(Constant.TIME, (c.getTimeInMillis()/ 1000L));
            myIntent.putExtra(Constant.FROM_COORDS, fromCoords);
            myIntent.putExtra(Constant.TO_COORDS, toCoords);
            myIntent.putExtra(Constant.ISDEPARTURE, true);
            startActivity(myIntent);
        }
    }

    private ServiceConnection serviceConection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Service disconnected!");
            api = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            api = IBackgroundServiceAPI.Stub.asInterface(service);

            Log.i(TAG, "Service connected! "+api.toString());
            try {
                api.addListener(serviceListener);
                int res = api.requestLastKnownAddress(1);
                Log.i(TAG, "requestLastKnownAddress: "+res);
                api.cancelRoute(0);
            } catch(Exception e) {
                Log.e(TAG, "ERROR!!", e);
            }
        }
    };

    @Override
    public void onBackPressed() {
        finish();
    }
}
