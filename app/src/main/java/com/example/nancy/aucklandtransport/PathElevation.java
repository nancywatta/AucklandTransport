package com.example.nancy.aucklandtransport;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.nancy.aucklandtransport.Adapters.PathAdapter;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class PathElevation extends Activity {
    private static final String TAG = PathElevation.class.getSimpleName();
    String pathCoords ="";
    String pathString;
    LinearLayout ly;
    RouteStep routeStep = null;
    ListView listView;
    Boolean isTransit;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_elevation);

        listView = (ListView) findViewById(R.id.WalkingInfoListView);

        Intent intent = getIntent();
        isTransit = intent.getBooleanExtra("IS_TRANSIT", false);
        pathString = intent.getStringExtra("PathJSON");

        if (!pathString.equals("")) try {
            routeStep = new RouteStep(pathString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(!isTransit) {
            Log.d(TAG, "Walking");
            //ly = (LinearLayout) findViewById(R.id.chart);
            imageView = (ImageView) findViewById(R.id.ElevationImage);
            getPolyLine();

            if (routeStep != null) {
                listView.setAdapter(new PathAdapter(PathElevation.this, routeStep));
            }

            // Getting URL to the Google Elevation API
            /*String url = getDirectionsUrl();

            DownloadTask downloadTask = new DownloadTask();

            // Start downloading json data from Google Directions API
            downloadTask.execute(url); */

            // Getting URL to our private Application server for Elevation Image
            String url =  getElevationUrl();

            ElevationTask elevationTask = new ElevationTask();

            // Start downloading image source from private server
            elevationTask.execute(url);
        }
        else {
            Log.d(TAG, "Transit");
            displayTransitInfo();
        }
    }

    private String getElevationUrl() {

        // Building the url to the web service
        String url = "http://" + getString(R.string.IP_ADDRESS) + ":8080/apt-server/showElevationProfile?pathStr="
                + pathCoords.trim();

        Log.d("url", url);

        return url;
    }

    // Fetches data from url passed
    private class ElevationTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d(TAG, "Response : " + result);

            new ImageLoadTask(result, imageView).execute(null, null);
        }
    }

    public class ImageLoadTask extends AsyncTask<Void, Void, Bitmap> {

        private String url;
        private ImageView imageView;

        public ImageLoadTask(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                URL urlConnection = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlConnection
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2048;
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            imageView.setImageBitmap(result);
            View parentView= findViewById(R.id.pathView);
            int height=parentView.getHeight();
            int width=parentView.getWidth();

            RelativeLayout.LayoutParams lp=
                    (RelativeLayout.LayoutParams)imageView.getLayoutParams();
            int percentHeight= (int)(height*.75);
            int percentWidth= width*1;
            lp.height=percentHeight;
            lp.width=percentWidth;
            imageView.setLayoutParams(lp);
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

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
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

    private void getPolyLine() {
        if (routeStep != null) {
            ArrayList<PathSegment> pathArray = routeStep.getPath();
            for (int i=0; i<pathArray.size();i++) {
                PathSegment p = pathArray.get(i);
                LatLng sLocation = p.getStartLoc();
                LatLng eLocation = p.getEndLoc();
                pathCoords = pathCoords + sLocation.latitude + "," + sLocation.longitude
                        + "|" + eLocation.latitude + "," + eLocation.longitude;
                if(i<pathArray.size()-1)
                    pathCoords = pathCoords + "|";
            }
            if(pathArray.size()<1) {
                LatLng sLocation = routeStep.getStartLoc();
                LatLng eLocation = routeStep.getEndLoc();
                pathCoords = pathCoords + sLocation.latitude + "," + sLocation.longitude
                        + "|" + eLocation.latitude + "," + eLocation.longitude;
            }
        }
    }

    private void displayTransitInfo() {
        ly = (LinearLayout) findViewById(R.id.linearLayout);
        if(routeStep != null) {

            TextView et5 = new TextView(this);
            et5.setText("Bus No: " + routeStep.getShortName());
            et5.setTextSize(25);
            et5.setTextColor(Color.parseColor("#10bcc9"));
            et5.setTypeface(Typeface.DEFAULT_BOLD);
            ly.addView(et5);

            View v1 = new View(this);
            v1.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    5
            ));
            v1.setBackgroundColor(Color.parseColor("#10bcc9"));
            v1.setPadding(0,2,0,2);
            ly.addView(v1);

            TextView et = new TextView(this);
            et.setText("From ");
            //et.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.bustop, 0);
            et.setTextSize(20);
            et.setGravity(Gravity.CENTER);
            ly.addView(et);

                /*ImageView iV = new ImageView(this);
                iV.setImageResource(R.drawable.bustop);
                llAlso.addView(iV); */

            TextView et3 = new TextView(this);
            et3.setText(routeStep.getDepartureStop());
            et3.setTextSize(25);
            et3.setTypeface(Typeface.DEFAULT_BOLD);
            et3.setGravity(Gravity.CENTER);
            ly.addView(et3);

            View v = new View(this);
            v.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    5
            ));
            v.setBackgroundColor(Color.parseColor("#B3B3B3"));
            v.setPadding(0,2,0,2);
            ly.addView(v);

            TextView et2 = new TextView(this);
            et2.setText("To ");
            //et2.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.bustop, 0);
            et2.setTextSize(20);
            et2.setGravity(Gravity.CENTER);
            ly.addView(et2);

            TextView et4 = new TextView(this);
            et4.setText(routeStep.getArrivalStop());
            et4.setTextSize(25);
            et4.setTypeface(Typeface.DEFAULT_BOLD);
            et4.setGravity(Gravity.CENTER);
            ly.addView(et4);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.path_elevation, menu);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(false); // disable the button
            actionBar.setDisplayHomeAsUpEnabled(false); // remove the left caret
        }
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

    /*
    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /$$ A class to parse the Google Elevation in JSON format $/
    private class ParserTask extends AsyncTask<String, Integer, ArrayList<Double>>{

        // Parsing the data in non-ui thread
        @Override
        protected ArrayList<Double> doInBackground(String... jsonData) {

            JSONObject jObject;
            ArrayList<Double> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                ElevationJSONParser parser = new ElevationJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(ArrayList<Double> result) {
            if(result.size()<1){
                Toast.makeText(getBaseContext(), "No Elevation Information Available", Toast.LENGTH_SHORT).show();
                return;
            }
            XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
            XYSeries series = new XYSeries("Elevation Chart");
            int steps = 0;
            for (Double r : result) {
                series.add(steps++, r);
            }
            dataset.addSeries(series);
            GraphicalView chartView = ChartFactory.getLineChartView(PathElevation.this, dataset, getRenderer());
            ly.addView(chartView);
        }
    }

    private XYMultipleSeriesRenderer getRenderer() {
        XYSeriesRenderer renderer = new XYSeriesRenderer();
        renderer.setLineWidth(5);
        renderer.setColor(Color.BLUE);
        renderer.setDisplayBoundingPoints(true);
        renderer.setPointStyle(PointStyle.CIRCLE);
        renderer.setPointStrokeWidth(6);
        //renderer.setDisplayChartValues(true);
        XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
        mRenderer.setApplyBackgroundColor(true);
        mRenderer.setBackgroundColor(Color.argb(100, 50, 50, 50));
        mRenderer.addSeriesRenderer(renderer);
        //mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        mRenderer.setPanEnabled(false, false);
        //mRenderer.setYAxisMin(0);
        mRenderer.setShowGrid(true);
        return mRenderer;
    }

    private String getDirectionsUrl(){

        // Sensor enabled
        String sensor = "sensor=false";

        String key = "key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw";

        String parameters = "path=" + pathCoords + "&samples=10" + "&" + sensor + "&" + key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/elevation/"+output+"?"+parameters;

        Log.d("url", url);

        return url;
    }
    */
}
