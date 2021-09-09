package github.hmasum18.satellight.utils.tle;

import android.util.Log;

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

/**
 * @see com.neosensory.tlepredictionengine.TlePredictionEngine for details
 */
public class TleToGeo {
    private static final String TAG = "TleToGeo";
    private Tle tle;

    public TleToGeo(String line1, String line2) {
        this.tle = new Tle(line1, line2);
    }

    public SatelliteTrajectory getSatellitePosition(double deviceLat, double deviceLng) {
        return getSatellitePosition(Calendar.getInstance().getTime(), deviceLat, deviceLng);
    }

    public SatelliteTrajectory getSatellitePosition(Date date, double deviceLat, double deviceLng) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);

        Date currentTime = cal.getTime();
        double[][] rv = tle.getRV(currentTime);
        return new SatelliteTrajectory(rv, currentTime, deviceLat ,deviceLng); //y
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

    public static class SatelliteTrajectory {
        private final double x;
        private final double y;
        private final double z;
        private final Eci positionEci;
        private final Eci velocityEci;
        private final double lat;
        private final double lng;
        private final double alt;
        private double speed;
        private Date time;
        private double azimuth;
        private double elevation;
        private double deviceLat;
        private double deviceLng;

        public SatelliteTrajectory(double[][] rv, Date currentTime, double deviceLat, double deviceLng) {
            this.deviceLat = deviceLat;
            this.deviceLng = deviceLng;

            // r is satellite position vector in km using TEME (from center of Earth)
            // v is the velocity vector in 3d space.
            this.x = rv[0][0];
            this.y = rv[0][1];
            this.z = rv[0][2];
            this.positionEci = new Eci(x, y, z);

            // lat lang alt
            double[] latLonAlt = TemeGeodeticConverter.getLatLonAlt(x, y, z, currentTime);

            this.lat = latLonAlt[0];
            this.lng = latLonAlt[1];
            this.alt = latLonAlt[2];

            double vx = rv[1][0];
            double vy = rv[1][1];
            double vz = rv[1][2];

            this.velocityEci = new Eci(vx, vy, vz);

            this.speed = Math.sqrt(vx * vx + vy * vy + vz * vz);

            this.time = currentTime;

            this.calculateAzimuthElevation(deviceLat, deviceLng);
        }

        public Date getTime() {
            return time;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public double getLat() {
            return lat;
        }

        public double getLng() {
            return lng;
        }

        public double getSpeed() {
            return speed;
        }

        public double getAlt() {
            return alt;
        }

        public double getAzimuth() {
            return azimuth;
        }

        public double getElevation() {
            return elevation;
        }

        // what is azimuth and elevation
        // https://www.youtube.com/watch?v=T4ABS-ILhyc
        public void calculateAzimuthElevation(double latDeg, double lngDeg) {
            this.deviceLat = latDeg;
            this.deviceLng = lngDeg;
            Geodetic observerGeodetic = new Geodetic(Math.toRadians(latDeg), Math.toRadians(lngDeg), 0.37);

            // You will need GMST for some of the coordinate transforms.
            // http://en.wikipedia.org/wiki/Sidereal_time#Definition
            double gmSt = TemeGeodeticConverter.getGmst(
                    TemeGeodeticConverter.getJulianTime(time));

            Log.d(TAG, "calculateAzimuthElevation: gmst: "+gmSt);

            // You can get ECF, Geodetic, Look Angles
            Ecf positionEcf  = this.eciToEcf(positionEci, gmSt);
            //Log.d(TAG, "calculateAzimuthElevation: positionEcf: "+positionEcf);
            LookAngles lookAngles  = this.ecfToLookAngles(observerGeodetic, positionEcf);
            //Log.d(TAG, "calculateAzimuthElevation: "+lookAngles);

            // Look Angles may be accessed by `azimuth`, `elevation` properties.
            double azimuthRad  = lookAngles.azimuth;
            double elevationRad = lookAngles.elevation;

            this.azimuth = Math.toDegrees(azimuthRad);
            this.elevation = Math.toDegrees(elevationRad);
        }

        @Override
        public String toString() {
            return "SatelliteTrajectory:" + new Gson().toJson(this);
        }


        private Ecf eciToEcf(Eci eci, double gmst) {
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

        private Ecf geodeticToEcf(Geodetic geodetic) {
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
        private Point3d topocentric(Geodetic observerGeodetic, Ecf satelliteEcf) {
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
        private LookAngles topocentricToLookAngles(Point3d tc) {
            double topS = tc.x;
            double topE = tc.y;
            double topZ = tc.z;

            double rangeSat = Math.sqrt((topS * topS) + (topE * topE) + (topZ * topZ));
            double El = Math.asin(topZ / rangeSat);
            double Az = Math.atan2(-topE, topS) + Math.PI;

            return new LookAngles(Az, El, rangeSat);
        }

        private LookAngles ecfToLookAngles(Geodetic observerGeodetic, Ecf satelliteEcf) {
            Point3d topocentricCoords = topocentric(observerGeodetic, satelliteEcf);
            return topocentricToLookAngles(topocentricCoords);
        }

        private Geodetic eciToGeodetic(Eci eci, double gmst) {
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

    }

    public static class Point3d {
        double x;
        double y;
        double z;

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
        double lat;
        double lng;
        double height;

        public Geodetic(double lat, double lng, double height) {
            this.lat = lat;
            this.lng = lng;
            this.height = height;
        }
    }

    public static class LookAngles {
        double azimuth;
        double elevation;
        double rangeSat; // Range in km

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
