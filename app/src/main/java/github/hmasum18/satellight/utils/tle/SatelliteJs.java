package github.hmasum18.satellight.utils.tle;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import github.hmasum18.satellight.view.App;

// https://cdnjs.com/libraries/satellite.js/4.1.3
// https://github.com/shashwatak/satellite-js
//@Singleton
public class SatelliteJs{
    private static final String TAG = "SatelliteJs";

    private Context context;
    private WebView webView;

    private boolean isPageLoadFinished = false;

    //@Inject
    public SatelliteJs(App app) {
        this.context = app.getApplicationContext();
        initWebView();
    }

    private void initWebView(){
        webView = new WebView(context);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new SatelliteJsWebInterface(context), "Android");
        webView.loadUrl("file:///android_asset/satellite-js.html");
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "onPageFinished: ");
                isPageLoadFinished = true;
            }
        });
    }

    public void calculateSateData(JSONObject data){
        Log.d(TAG, "calculateSateData: ");
        if(isPageLoadFinished)
            webView.loadUrl("javascript:calculateSatelliteData(\""+StringEscapeUtils.escapeEcmaScript(data.toString())+"\")");
        else
            webView.setWebViewClient(new WebViewClient(){
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    isPageLoadFinished = true;
                    webView.loadUrl("javascript:calculateSatelliteData(\""+StringEscapeUtils.escapeEcmaScript(data.toString())+"\")");
                }
            });
    }
}
