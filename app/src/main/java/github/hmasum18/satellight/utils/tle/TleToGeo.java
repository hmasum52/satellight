package github.hmasum18.satellight.utils.tle;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.neosensory.tlepredictionengine.TemeGeodeticConverter;
import com.neosensory.tlepredictionengine.Tle;

import java.math.BigDecimal;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalField;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import github.hmasum18.satellight.service.model.SatelliteTrajectory;

/**
 * @see com.neosensory.tlepredictionengine.TlePredictionEngine for details
 */
public class TleToGeo {
    private static final String TAG = "TleToGeo";


    public static SatelliteTrajectory getSatellitePosition(Tle tle, LatLng latLng) {
        return getSatellitePosition(tle, Calendar.getInstance().getTime(), latLng);
    }

    public static SatelliteTrajectory getSatellitePosition(Tle tle, long timestamp, LatLng latLng) {
        return getSatellitePosition(tle, new Date(timestamp), latLng);
    }

    public static SatelliteTrajectory getSatellitePosition(Tle tle, Date date, LatLng latLng) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);

        Date currentTime = cal.getTime();
        double[][] rv = tle.getRV(currentTime);
        return new SatelliteTrajectory(rv, currentTime, latLng.latitude ,latLng.longitude); //y
    }

    /**
     * ref : https://rechneronline.de/earth-radius/
     *
     * @param lat
     * @return
     */
    public static double getEarthRadiusAt(double lat) {
        lat = lat * Math.PI / 180; //convert to radian as Math.cos() function takes angle in radian;

        double e = 6378.137; //earth radius at the equator at sea level
        double p = 6356.752; //earth radius at the pole at sea level

        double nominator = (e * e * Math.cos(lat)) * (e * e * Math.cos(lat)) + (p * p * Math.sin(lat)) * (p * p * Math.sin(lat));
        double denominator = (e * Math.cos(lat)) * (e * Math.cos(lat)) + (p * Math.sin(lat)) * (p * Math.sin(lat));

        return Math.sqrt(nominator / denominator); //calculate earth radius and return
    }

    public static Ecf eciToEcf(Eci eci, double gmst) {
        // ccar.colorado.edu/ASEN5070/handouts/coordsys.doc
        //
        // [X]     [C -S  0][X]
        // [Y]  =  [S  C  0][Y]
        // [Z]eci  [0  0  1][Z]ecf
        //
        //
        // Inverse:
        // [X]     [C  S  0][X]
        // [Y]  =  [-S C  0][Y]
        // [Z]ecf  [0  0  1][Z]eci

        double x = (eci.x * Math.cos(gmst)) + (eci.y * Math.sin(gmst));
        double y = (eci.x * (-Math.sin(gmst))) + (eci.y * Math.cos(gmst));
        return new Ecf(x, y, eci.z);
    }

    public static Ecf geodeticToEcf(Geodetic geodetic) {
        double latitude = geodetic.lat;
        double longitude = geodetic.lng;
        double height = geodetic.height;

        double a = 6378.137;
        double b = 6356.7523142;
        double f = (a - b) / a;
        double e2 = ((2 * f) - (f * f));
        double normal = a / Math.sqrt(1 - (e2 * (Math.sin(latitude) * Math.sin(latitude))));

        double x = (normal + height) * Math.cos(latitude) * Math.cos(longitude);
        double y = (normal + height) * Math.cos(latitude) * Math.sin(longitude);
        double z = ((normal * (1 - e2)) + height) * Math.sin(latitude);

        return new Ecf(x, y, z);
    }


    // for look angle
    public static Point3d topocentric(Geodetic observerGeodetic, Ecf satelliteEcf) {
        // http://www.celestrak.com/columns/v02n02/
        // TS Kelso's method, except I'm using ECF frame
        // and he uses ECI.

        double latitude = observerGeodetic.lat;
        double longitude = observerGeodetic.lng;

        Ecf observerEcf = geodeticToEcf(observerGeodetic);

        double rx = satelliteEcf.x - observerEcf.x;
        double ry = satelliteEcf.y - observerEcf.y;
        double rz = satelliteEcf.z - observerEcf.z;

        double topS = ((Math.sin(latitude) * Math.cos(longitude) * rx)
                + (Math.sin(latitude) * Math.sin(longitude) * ry))
                - (Math.cos(latitude) * rz);

        double topE = (-Math.sin(longitude) * rx)
                + (Math.cos(longitude) * ry);

        double topZ = (Math.cos(latitude) * Math.cos(longitude) * rx)
                + (Math.cos(latitude) * Math.sin(longitude) * ry)
                + (Math.sin(latitude) * rz);

        return new Point3d(topS, topE, topZ);
    }

    /**
     * @param {Object} tc
     * @param {Number} tc.topS Positive horizontal vector S due south.
     * @param {Number} tc.topE Positive horizontal vector E due east.
     * @param {Number} tc.topZ Vector Z normal to the surface of the earth (up).
     * @returns {Object}
     */
    public static LookAngles topocentricToLookAngles(Point3d tc) {
        double topS = tc.x;
        double topE = tc.y;
        double topZ = tc.z;

        double rangeSat = Math.sqrt((topS * topS) + (topE * topE) + (topZ * topZ));
        double El = Math.asin(topZ / rangeSat);
        double Az = Math.atan2(-topE, topS) + Math.PI;

        return new LookAngles(Az, El, rangeSat);
    }

    public static LookAngles ecfToLookAngles(Geodetic observerGeodetic, Ecf satelliteEcf) {
        Point3d topocentricCoords = topocentric(observerGeodetic, satelliteEcf);
        return topocentricToLookAngles(topocentricCoords);
    }

    public static Geodetic eciToGeodetic(Eci eci, double gmst) {
        // http://www.celestrak.com/columns/v02n03/
        double a = 6378.137;
        double b = 6356.7523142;
        double R = Math.sqrt((eci.x * eci.x) + (eci.y * eci.y));
        double f = (a - b) / a;
        double e2 = ((2 * f) - (f * f));

        double pi = Math.PI;
        double twoPi = 2 * pi;

        double longitude = Math.atan2(eci.y, eci.x) - gmst;
        while (longitude < -pi) {
            longitude += twoPi;
        }
        while (longitude > pi) {
            longitude -= twoPi;
        }

        double kmax = 20;
        int k = 0;
        double latitude = Math.atan2(
                eci.z,
                Math.sqrt((eci.x * eci.x) + (eci.y * eci.y))
        );
        double C = 0;
        while (k < kmax) {
            C = 1 / Math.sqrt(1 - (e2 * (Math.sin(latitude) * Math.sin(latitude))));
            latitude = Math.atan2(eci.z + (a * C * e2 * Math.sin(latitude)), R);
            k += 1;
        }
        double height = (R / Math.cos(latitude)) - (a * C);
        return new Geodetic(latitude, longitude, height);
    }

    public static class Point3d {
        public double x;
        public double y;
        public double z;

        public Point3d(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }

    // https://en.wikipedia.org/wiki/Earth-centered_inertial
    public static class Eci extends Point3d {

        public Eci(double x, double y, double z) {
            super(x, y, z);
        }
    }

    // https://en.wikipedia.org/wiki/ECEF
    public static class Ecf extends Point3d {
        public Ecf(double x, double y, double z) {
            super(x, y, z);
        }
    }

    public static class Geodetic {
        public double lat;
        public double lng;
        public double height;

        public Geodetic(double lat, double lng, double height) {
            this.lat = lat;
            this.lng = lng;
            this.height = height;
        }
    }

    public static class LookAngles {
        public double azimuth;
        public double elevation;
        public double rangeSat; // Range in km

        public LookAngles(double azimuth, double elevation, double rangeSat) {
            this.azimuth = azimuth;
            this.elevation = elevation;
            this.rangeSat = rangeSat;
        }

        @Override
        public String toString() {
            return "LookAngles"+new Gson().toJson(this);
        }
    }

}
