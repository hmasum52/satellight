package github.hmasum18.satellight.service.model;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.neosensory.tlepredictionengine.TemeGeodeticConverter;

import java.util.Date;

import github.hmasum18.satellight.utils.tle.TleToGeo;

public class SatelliteTrajectory {
    private static final String TAG = "SatelliteTrajectory";
    private final double x;
    private final double y;
    private final double z;
    private final TleToGeo.Eci positionEci;
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
        this.positionEci = new TleToGeo.Eci(x, y, z);

        // lat lang alt
        double[] latLonAlt = TemeGeodeticConverter.getLatLonAlt(x, y, z, currentTime);

        this.lat = latLonAlt[0];
        this.lng = latLonAlt[1];
        this.alt = latLonAlt[2];

        double vx = rv[1][0];
        double vy = rv[1][1];
        double vz = rv[1][2];

        TleToGeo.Eci velocityEci = new TleToGeo.Eci(vx, vy, vz);

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

    public LatLng getLatLng(){
        return new LatLng(lat, lng);
    }

    public double getSpeed() {
        return speed;
    }

    public double getHeight() {
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
        TleToGeo.Geodetic observerGeodetic =
                new TleToGeo.Geodetic(Math.toRadians(latDeg), Math.toRadians(lngDeg), 0.37);

        // You will need GMST for some of the coordinate transforms.
        // http://en.wikipedia.org/wiki/Sidereal_time#Definition
        double gmSt = TemeGeodeticConverter.getGmst(
                TemeGeodeticConverter.getJulianTime(time));

        Log.d(TAG, "calculateAzimuthElevation: gmst: "+gmSt);

        // You can get ECF, Geodetic, Look Angles
        TleToGeo.Ecf positionEcf  = TleToGeo.eciToEcf(positionEci, gmSt);
        //Log.d(TAG, "calculateAzimuthElevation: positionEcf: "+positionEcf);
        TleToGeo.LookAngles lookAngles  = TleToGeo.ecfToLookAngles(observerGeodetic, positionEcf);
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

}