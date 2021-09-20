package github.hmasum18.satellight.utils;

import com.google.android.gms.maps.model.LatLng;

// https://www.youtube.com/watch?v=WKfZsCKSXVQ
// https://stackoverflow.com/questions/14864664/animating-markers-on-google-maps-v2
public class LatLngInterpolator {
    public static LatLng interpolate(float fraction, LatLng a, LatLng b) {
        double lat = (b.latitude - a.latitude) * fraction + a.latitude;
        double lngDelta = b.longitude - a.longitude;

        // Take the shortest path across the 180th meridian.
        if (Math.abs(lngDelta) > 180) {
            lngDelta -= Math.signum(lngDelta) * 360;
        }

        // we don't have to worry about lng > 180 or lng < -180
        // LatLng will fix it.
        double lng = lngDelta * fraction + a.longitude;
        return new LatLng(lat, lng);
    }
}
