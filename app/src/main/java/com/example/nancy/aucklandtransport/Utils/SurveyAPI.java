package com.example.nancy.aucklandtransport.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.example.nancy.aucklandtransport.R;

import org.androidpn.client.Constants;

/**
 * Class to determine the eligibility of the user to take the survey
 *
 * Created by Nancy on 11/8/14.
 */
public class SurveyAPI {
    private static final String TAG = SurveyAPI.class.getSimpleName();

    private static String main_url="";
    private Context mContext;
    private SharedPreferences sharedPrefs;
    private String deviceId;
    private int usageCount;

    public SurveyAPI(Context context) {
        this.mContext = context;
        sharedPrefs = mContext.getSharedPreferences(Constants.SHARED_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        main_url = "http://" +
                mContext.getResources().getString(R.string.IP_ADDRESS) + ":8080/apt-server/";

        String serviceName = Context.TELEPHONY_SERVICE;
        TelephonyManager m_telephonyManager = (TelephonyManager) mContext.getSystemService(serviceName);
        this.deviceId = m_telephonyManager.getDeviceId();
        this.usageCount = sharedPrefs.getInt(Constant.USAGE_COUNT, 0);
    }

    /*
    get from server the number of times the user has actually used the APT application
     */
    public void getServerCount() {
        String url = main_url + "processUserActivity?" +
                "username=" + this.deviceId;

        Log.d(TAG, "url : " + url);

        UsageCountTask usageCountTask = new UsageCountTask();

        usageCountTask.execute(url);
    }

    private class UsageCountTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = HTTPConnect.downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {

            if(result == null)
                return;

            Log.d(TAG, "surevy result: " + result);
            int serverCount = Integer.parseInt(result);
            if(usageCount + 1 == serverCount || usageCount == serverCount
                    || (serverCount > 0 && serverCount <= Constant.SURVEY_ELIG_COUNT)) {
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putInt(Constant.USAGE_COUNT, serverCount);
                editor.commit();
            }

        }
    }

    /**
     * if the user has used the application more than four times,
     * show him the link for survey
     *
     * @return boolean
     */
    public boolean getUsageCount() {
        if(usageCount >= Constant.SURVEY_ELIG_COUNT)
            return true;
        return false;
    }
}
