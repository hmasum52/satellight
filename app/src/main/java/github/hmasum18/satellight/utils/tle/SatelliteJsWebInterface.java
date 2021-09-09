package github.hmasum18.satellight.utils.tle;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class SatelliteJsWebInterface {
    private static final String TAG = "SatelliteJsWebInterface";

    Context mContext;

    /** Instantiate the interface and set the context */
    public SatelliteJsWebInterface(Context c) {
        mContext = c;
    }

    @JavascriptInterface
    public void onAzimuthCalculated(double azimuth){
        Log.d(TAG, "onAzimuthCalculated: azimuth: "+azimuth);
    }

    @JavascriptInterface
    public void onElevationCalculated(double elevation){
        Log.d(TAG, "onElevationCalculated: "+elevation);
    }
}
