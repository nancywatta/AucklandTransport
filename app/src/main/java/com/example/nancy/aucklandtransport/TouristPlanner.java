package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.example.nancy.aucklandtransport.BackgroundJobs.GPSTracker;
import com.example.nancy.aucklandtransport.BackgroundTask.GooglePlacesTask;
import com.example.nancy.aucklandtransport.Utils.ConnectionDetector;
import com.example.nancy.aucklandtransport.Utils.Constant;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * TouristPlanner class is the activity asking for user to enter the origin
 * and destination of the journey to be planned.
 * The activity also allows a user to select a place in the origin or destination
 * from the Recent Places Tab.
 * It also allows a user to select the entire route from the Recent Route Tab.
 *
 * Created by Nancy on 9/9/14.
 */
public class TouristPlanner extends FragmentActivity {

    private static final String TAG = TouristPlanner.class.getSimpleName();
    ArrayList<History.PlaceItem> history;
    ArrayList<History.RouteHistoryItem> routes;
    AutoCompleteTextView origin;
    AutoCompleteTextView destination;

    TimePickerFragment leaveTimeFragment = null;
    TimePickerFragment arriveTimeFragment = null;
    DatePickerDialogFragment dateFragment = null;

    // Connection detector class
    ConnectionDetector cd;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();

    // flag for Internet connection status
    Boolean isInternetPresent = false;

    Button date;
    Button leaveTime;
    Button arriveTime;
    CheckBox arriveCheck;

    String fromCoords ="";
    String toCoords="";

    GPSTracker gps;
    GoogleAPI googleAPI;

    String prefix="";

    ListView myPlaces, myRoutes;
    History.PlaceAdapter placeAdapter;
    History.RoutesAdapter routesAdapter;
    private int lastSelectedPlace = -1;
    private int lastSelectedRoute = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tourist_planner);

        // creating connection detector class instance
        cd = new ConnectionDetector(getApplicationContext());
        // Check if Internet present
        isInternetPresent = cd.isConnectingToInternet();
        if (!isInternetPresent) {
            // Internet Connection is not present
            alert.showAlertDialog(TouristPlanner.this, "Internet Connection Error",
                    "Please connect to working Internet connection", false);
            // stop executing code by return
            return;
        }

        arriveCheck = (CheckBox)findViewById(R.id.arriveChk);

        if(arriveTimeFragment==null)
            arriveTimeFragment = new TimePickerFragment();

        if(leaveTimeFragment==null)
            leaveTimeFragment = new TimePickerFragment();

        history = History.getHistory(this);
        routes = History.getRoutes(this);

        origin = (AutoCompleteTextView) findViewById(R.id.editText1);
        gps = new GPSTracker(getBaseContext());
        googleAPI = new GoogleAPI();

        googleAPI.getReverseGeocode(new LatLng(gps.getLatitude(), gps.getLongitude()));

        if(googleAPI.geoPlaces != null)
            origin.setText(googleAPI.geoPlaces.get(0).get("formatted_address"));

        origin.setThreshold(1);

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
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }

        });

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

        origin.setOnTouchListener(new View.OnTouchListener() {
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

        destination = (AutoCompleteTextView) findViewById(R.id.editText2);
        destination.setThreshold(1);

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

        destination.setOnTouchListener(new View.OnTouchListener() {
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

        myPlaces = (ListView)findViewById(R.id.myPlacesList);
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

                AlertDialog.Builder builder = new AlertDialog.Builder(TouristPlanner.this);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        History.PlaceItem h = history.get(lastSelectedPlace);
                        switch(which) {
                            case 0: origin.setText(h.address);
                                fromCoords = h.coords;
                                break;
                            case 1: destination.setText(h.address);
                                toCoords = h.coords;
                                break;
                            case 2:
                                History.remove(TouristPlanner.this, "history_"+history.get(lastSelectedPlace).address);
                                History.init(TouristPlanner.this);
                                history = History.getHistory(TouristPlanner.this);
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

                AlertDialog.Builder builder = new AlertDialog.Builder(TouristPlanner.this);
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
                                History.remove(TouristPlanner.this, "route_"+r.start+"_"+r.end);
                                History.init(TouristPlanner.this);
                                routes = History.getRoutes(TouristPlanner.this);
                                routesAdapter.notifyDataSetChanged();
                                break;
                        }

                    }
                });
                builder.show();
            }
        });

        leaveTime = (Button)findViewById(R.id.button6);
        arriveTime = (Button)findViewById(R.id.button5);
        date = (Button)findViewById(R.id.button3);

        if (savedInstanceState != null) {
            // Restore value of members from saved state
            origin.setText(savedInstanceState.getString("From Location"));
            destination.setText(savedInstanceState.getString("To Location"));
            date.setText(savedInstanceState.getString("Date"));
            leaveTime.setText(savedInstanceState.getString("LeaveTime"));
            arriveTime.setText(savedInstanceState.getString("ArriveTime"));
        }
        else
        {
            updateTime();
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

    public void showRoute(View view) {

        try {
            String toAddress = destination.getText().toString(); // Get address
            String fromAddress = origin.getText().toString(); // Get address
            long secondsSinceEpoch = getTimeSinceEpoch(leaveTimeFragment, dateFragment);
            long arrivalSecEpoch = getTimeSinceEpoch(arriveTimeFragment, dateFragment);

            if(toAddress.equals("") || toAddress==null) {
                AlertDialog alertDialog = new AlertDialog.Builder(TouristPlanner.this).create();
                alertDialog.setTitle("Validation Error");
                alertDialog.setMessage("Please enter the Destination");
                alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    } });
                alertDialog.show();

            } else if(secondsSinceEpoch >= arrivalSecEpoch && arriveCheck.isChecked()) {
                AlertDialog alertDialog = new AlertDialog.Builder(TouristPlanner.this).create();
                alertDialog.setTitle("Validation Error");
                alertDialog.setMessage("Arrival Time should be greater than Departure");
                alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    } });
                alertDialog.show();
            }
            else {
                if (fromAddress.equals("") || fromAddress == null) {

                    if(googleAPI.geoPlaces != null)
                        fromAddress = googleAPI.geoPlaces.get(0).get("formatted_address");
                    Log.d(TAG, "fromAdd " + fromAddress);
                }

                Intent intent = new Intent(this, TouristRoute.class);
                intent.putExtra(Constant.FROM_LOCATION, fromAddress);
                intent.putExtra(Constant.TO_LOCATION, toAddress);
                intent.putExtra(Constant.TIME, secondsSinceEpoch);
                intent.putExtra(Constant.FROM_COORDS, fromCoords);
                intent.putExtra(Constant.TO_COORDS, toCoords);
                intent.putExtra(Constant.ARRIVE_TIME, arrivalSecEpoch);
                startActivity(intent);
            }
        } catch (Exception e){
            e.printStackTrace();

        }
    }

    public long getTimeSinceEpoch(TimePickerFragment timeFrag,
            DatePickerDialogFragment dateFrag) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        if (dateFrag != null && timeFrag != null) {
            calendar.clear();
            calendar.set(Calendar.MONTH, dateFrag.mMonth);
            calendar.set(Calendar.YEAR, dateFrag.mYear);
            calendar.set(Calendar.DAY_OF_MONTH, dateFrag.mDay);
            calendar.set(Calendar.HOUR_OF_DAY, timeFrag.mHour);
            calendar.set(Calendar.MINUTE, timeFrag.mMinute);
        } else if (timeFrag != null) {
            calendar.set(Calendar.HOUR_OF_DAY, timeFrag.mHour);
            calendar.set(Calendar.MINUTE, timeFrag.mMinute);
        } else if (dateFrag != null) {
            calendar.set(Calendar.MONTH, dateFrag.mMonth);
            calendar.set(Calendar.YEAR, dateFrag.mYear);
            calendar.set(Calendar.DAY_OF_MONTH, dateFrag.mDay);
        }

        Log.d(TAG, "secondsSinceEpoch " + calendar.getTimeInMillis() +
                " Hour: " + calendar.get(Calendar.HOUR_OF_DAY)
                + " Month: " + calendar.get(Calendar.MONTH) + " Year: " + calendar.get(Calendar.YEAR) + " Date: " + calendar.get(Calendar.DAY_OF_MONTH)
                + " Day of Month: " + calendar.get(Calendar.DAY_OF_MONTH) + " " + calendar.getTime());

        return (calendar.getTimeInMillis() / 1000L);
    }

    public void showTimePickerDialog(View v) {
        if(leaveTimeFragment==null)
            leaveTimeFragment = new TimePickerFragment();
        Bundle bdl = new Bundle(2);
        bdl.putInt("ButtonId", R.id.button6);
        leaveTimeFragment.setArguments(bdl);
        leaveTimeFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void showArriveTimeDialog(View v) {
        if(arriveTimeFragment==null)
            arriveTimeFragment = new TimePickerFragment();
        Bundle bdl = new Bundle(2);
        bdl.putInt("ButtonId", R.id.button5);
        arriveTimeFragment.setArguments(bdl);
        arriveTimeFragment.show(getSupportFragmentManager(), "timePicker");
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        placeAdapter.notifyDataSetChanged();
        switch(requestCode) {
            case (Constant.PICK_ADDRESS_REQUEST) : {
                if (resultCode == Activity.RESULT_OK) {
                    // TODO Extract the data returned from the child Activity.
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
        savedInstanceState.putString("LeaveTime", leaveTime.getText().toString());
        savedInstanceState.putString("ArriveTime", arriveTime.getText().toString());
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
        origin.setText(savedInstanceState.getString("From Location"));
        destination.setText(savedInstanceState.getString("To Location"));
        date.setText(savedInstanceState.getString("Date"));
        arriveTime.setText(savedInstanceState.getString("ArriveTime"));
        leaveTime.setText(savedInstanceState.getString("LeaveTime"));
    }

    @Override
    protected void onResume() {
        super.onResume();

        myPlaces = (ListView)findViewById(R.id.myPlacesList);
        history = History.getHistory(this);

        placeAdapter = new History.PlaceAdapter(this);
        refreshYourAdapter();
        myPlaces.setAdapter(placeAdapter);

        updateTime();
    }

    private void updateTime() {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        leaveTime = (Button)findViewById(R.id.button6);
        arriveTime = (Button)findViewById(R.id.button5);
        date = (Button)findViewById(R.id.button3);

        if(leaveTimeFragment != null) {
            calendar.set(Calendar.HOUR_OF_DAY, leaveTimeFragment.mHour);
            calendar.set(Calendar.MINUTE, leaveTimeFragment.mMinute);
        }
        leaveTime.setText(twodigits(calendar.get(Calendar.HOUR_OF_DAY)) + ":" + twodigits(calendar.get(Calendar.MINUTE)));

        calendar = Calendar.getInstance(Locale.getDefault());
        if(arriveTimeFragment != null) {
            calendar.set(Calendar.HOUR_OF_DAY, arriveTimeFragment.mHour);
            calendar.set(Calendar.MINUTE, arriveTimeFragment.mMinute);
        }
        arriveTime.setText(twodigits(calendar.get(Calendar.HOUR_OF_DAY)) + ":" + twodigits(calendar.get(Calendar.MINUTE)));
        int month = calendar.get(Calendar.MONTH) +1;
        date.setText(twodigits(calendar.get(Calendar.DAY_OF_MONTH)) + "/" +
                twodigits(month) + "/" + calendar.get(Calendar.YEAR));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tourist_planner, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_home) {
            Intent exploreActivity = new Intent(TouristPlanner.this, HomePage.class);
            startActivity(exploreActivity);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
    }
}
