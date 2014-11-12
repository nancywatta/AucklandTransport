package com.example.nancy.aucklandtransport;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * WebActivity class is used to embed a web page inside our application
 * This web page displays the consent form and survey for our
 * application.
 *
 * Created by Nancy on 11/7/14.
 */
public class WebActivity extends Activity {

    private String errorHtml = "";

    // reference to WebView to display a web page
    private WebView webView;

    private String surveyUrl = "https://www.surveymonkey.com/s/DJHFSRK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        errorHtml = "<html><body><h3>Network Error. Please check the network.</h3></body></html>";

        webView = (WebView) findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);

        webView.loadUrl(surveyUrl);

        webView.setWebViewClient(new WebViewClient(){
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl){
                view.loadData(errorHtml, "text/html", "UTF-8");

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.web, menu);
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
