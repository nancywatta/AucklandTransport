package com.example.nancy.aucklandtransport;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.example.nancy.aucklandtransport.BackgroundJobs.RouteIntentService;
import com.example.nancy.aucklandtransport.Utils.Constant;
import com.example.nancy.aucklandtransport.datatype.Photo;
import com.example.nancy.aucklandtransport.datatype.Place;
import com.example.nancy.aucklandtransport.datatype.TouristPlaces;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * PlaceDialogFragment class is the dialog window which is opened on clicking a marker.
 * It shows place name, vicinity and photo corresponding to the marked place.
 *
 * Created by Nancy on 9/9/14.
 */
public class PlaceDialogFragment extends DialogFragment {
    private static final String TAG = PlaceDialogFragment.class.getSimpleName();

    TextView mTVPhotosCount = null;
    TextView mTVVicinity = null;
    TextView mDurText = null;
    ViewFlipper mFlipper = null;
    Place mPlace = null;
    DisplayMetrics mMetrics = null;
    Context context;
    Button addBtn;
    EditText mDuration;
    TouristPlaces touristPlaces;
//    String markerId;


    public PlaceDialogFragment(){
        super();
    }

    public PlaceDialogFragment(Place place, DisplayMetrics dm, Context context){
        super();
        this.mPlace = place;
        this.mMetrics = dm;
        this.context = context;
    }

    public void setTouristPlaces(TouristPlaces object) {
        this.touristPlaces = object;
    }

//    public void setMarkerRef(String markerId) {
//        this.markerId = markerId;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // For retaining the fragment on screen rotation
        setRetainInstance(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_layout, null);

        // Getting reference to ViewFlipper
        mFlipper = (ViewFlipper) v.findViewById(R.id.flipper);

        // Getting reference to TextView to display photo count
        mTVPhotosCount = (TextView) v.findViewById(R.id.tv_photos_count);

        // Getting reference to TextView to display place vicinity
        mTVVicinity = (TextView) v.findViewById(R.id.tv_vicinity);

        addBtn = (Button)v.findViewById(R.id.btn_add);

        mDuration = (EditText)v.findViewById(R.id.et_location);

        mDurText = (TextView) v.findViewById(R.id.text_view1);

        if(mPlace!=null){

            // Setting the title for the Dialog Fragment
            getDialog().setTitle(mPlace.mPlaceName);

            // Array of references of the photos
            Photo[] photos = mPlace.mPhotos;

            // Setting Photos count
            mTVPhotosCount.setText("Photos available : " + photos.length);

            // Setting the vicinity of the place
            mTVVicinity.setText(mPlace.mVicinity);

            // check if the place is added by user in to Visit list. If Yes,
            // then do not show Add Place Button, instead show Delete Button.
            if(touristPlaces.checkExisting(mPlace)) {
                mPlace.isAdded = true;
                mDurText.setVisibility(View.GONE);
                mDuration.setVisibility(View.GONE);
                setButtonToDelete();
            }
            else {
                mPlace.isAdded = false;
                setButtonToAdd();
            }

            // Creating an array of ImageDownloadTask to download photos
            ImageDownloadTask[] imageDownloadTask = new ImageDownloadTask[photos.length];

            int width = (int)(mMetrics.widthPixels*3)/4;
            int height = (int)(mMetrics.heightPixels*1)/2;

            String url = "https://maps.googleapis.com/maps/api/place/photo?";
            String key = "key=AIzaSyCOA_RXGLEYFgJyKJjGhVDkIwfkIAr0diw";
            String sensor = "sensor=true";
            String maxWidth="maxwidth=" + width;
            String maxHeight = "maxheight=" + height;
            url = url + "&" + key + "&" + sensor + "&" + maxWidth + "&" + maxHeight;

            // Traversing through all the photoreferences
            for(int i=0;i<photos.length;i++){
                // Creating a task to download i-th photo
                imageDownloadTask[i] = new ImageDownloadTask();

                String photoReference = "photoreference="+photos[i].mPhotoReference;

                // URL for downloading the photo from Google Services
                url = url + "&" + photoReference;

                // Downloading i-th photo from the above url
                imageDownloadTask[i].execute(url);
            }
        }
        return v;
    }

    private void setButtonToDelete() {
        addBtn.setText("Delete Place");
        addBtn.setOnClickListener(deleteButtonListener);
    }

    private void setButtonToAdd() {
        addBtn.setText(R.string.str_btn_Add);
        addBtn.setOnClickListener(addButtonListener);
    }

    private View.OnClickListener deleteButtonListener = new View.OnClickListener() {

        public void onClick(View v) {
            if(mPlace.isAdded) {
                // call background service to recalculate route on delete place
                callIntentServiceToDelete();
                touristPlaces.delete(mPlace);
//                TouristRoute.mAddedReference.remove(markerId);
                mPlace.isAdded = false;
                dismiss();
            }
        }
    };

    private View.OnClickListener addButtonListener = new View.OnClickListener() {

        public void onClick(View v) {
            if(!mPlace.isAdded) {
                if(TextUtils.isEmpty(mDuration.getText())) {
                    mDuration.setError(getString(R.string.error_field_required));
                    return;
                }
                mPlace.duration = Integer.parseInt(mDuration.getText().toString());
                mPlace.isAdded = true;
                touristPlaces.add(mPlace);

                // call background service to recalculate route on add place
                callIntentServiceToAdd();
//                TouristRoute.mAddedReference.put(markerId, mPlace);
                dismiss();
            }
        }
    };

    // Background Service to calculate route for the newly added place
    private void callIntentServiceToAdd() {
        String placeCoords = mPlace.mLat + "," + mPlace.mLng;
        RouteIntentService.startAction(context, touristPlaces,
                touristPlaces.getPreviousAdd(mPlace), placeCoords);
        Place nextPlace = touristPlaces.getNextAddress(mPlace);
        String nextAddress = "";
        if(nextPlace == null)
            nextAddress = touristPlaces.endAddress;
        else
            nextAddress = nextPlace.mLat + "," + nextPlace.mLng;
        RouteIntentService.endAction(context,
                placeCoords, nextAddress, mPlace.duration);
    }

    // Background Service to calculate route when delete place
    private void callIntentServiceToDelete() {

        String prevAdd = touristPlaces.getPreviousAdd(mPlace);
        String nextAdd = "";
        Place nextPlace = touristPlaces.getNextAddress(mPlace);
        long duration = 0;
        if(nextPlace == null)
            nextAdd = touristPlaces.endAddress;
        else {
            nextAdd = nextPlace.mLat + "," + nextPlace.mLng;
            duration = nextPlace.duration;
        }

        String placeCoords = mPlace.mLat + "," + mPlace.mLng;
        if(!(prevAdd.compareTo(TouristPlaces.startAddress) == 0 &&
                nextAdd.compareTo(TouristPlaces.endAddress) == 0)) {
            RouteIntentService.deleteAction(context, touristPlaces,
                    prevAdd, placeCoords, nextAdd);

            while (nextAdd.compareTo(TouristPlaces.endAddress) != 0) {
                Place nextToNextPlace = touristPlaces.getNextAddress(nextPlace);
                String nextToNextAdd = "";
                if(nextToNextPlace == null)
                    nextToNextAdd = touristPlaces.endAddress;
                else {
                    nextToNextAdd = nextToNextPlace.mLat + "," + nextToNextPlace.mLng;
                }
                RouteIntentService.recalculateAction(context,
                        nextAdd, nextToNextAdd, duration);

                if(nextToNextPlace != null)
                    duration = nextToNextPlace.duration;
                nextAdd = nextToNextAdd;
                nextPlace = nextToNextPlace;
            }
//            if(nextAdd.compareTo(TouristPlaces.endAddress) != 0) {
//                RouteIntentService.endAction(context,
//                        placeCoords, nextAdd, duration);
//            }
        } else {
            Log.d(TAG, " Initial Route");
            touristPlaces.getRoutesArray().clear();
            //touristPlaces.deleteRoute(null, null, placeCoords, null, 0, true);
            // Creating an intent for broadcastreceiver
            Intent broadcastIntent = new Intent(Constant.BROADCAST_ACTION);
            // Sending the broadcast
            LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
        }
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    private Bitmap downloadImage(String strUrl) throws IOException {
        Bitmap bitmap=null;
        InputStream iStream = null;
        try{
            URL url = new URL(strUrl);

            /** Creating an http connection to communicate with url */
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            /** Connecting to url */
            urlConnection.connect();

            /** Reading data from url */
            iStream = urlConnection.getInputStream();

            /** Creating a bitmap from the stream returned from the url */
            bitmap = BitmapFactory.decodeStream(iStream);

        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
        }
        return bitmap;
    }

    /**
     * This is an inner class of PlaceDialogFragment and is extending the class AsyncTask.
     * This is used to download image from Google Places Web Service.
     */
    private class ImageDownloadTask extends AsyncTask<String, Integer, Bitmap> {
        Bitmap bitmap = null;
        @Override
        protected Bitmap doInBackground(String... url) {
            try{
                // Starting image download
                bitmap = downloadImage(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            // Creating an instance of ImageView to display the downloaded image
            ImageView iView = new ImageView(context);

            // Setting the downloaded image in ImageView
            iView.setImageBitmap(result);

            // Adding the ImageView to ViewFlipper
            mFlipper.addView(iView);

            // Showing download completion message
            if(getActivity() != null)
                Toast.makeText(getActivity().getBaseContext(), "Image downloaded successfully", Toast.LENGTH_SHORT).show();
        }
    }

}
