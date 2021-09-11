package github.hmasum18.satellight.view.screen.googlemap;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import javax.inject.Inject;

import github.hmasum18.satellight.view.App;

public class DeviceLocationFinder {
    //to get the device location
    public final static int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101;
    public static final String TAG = "DeviceLocationFinder->";
    private final Context context;
    private ActivityResultCaller activityResultCaller;
    private LatLng deviceLatLng = null;

    private LocationCallback locationCallback;

    // see : https://developer.android.com/training/permissions/requesting
    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private final ActivityResultLauncher<String> requestPermissionLauncher;


    public DeviceLocationFinder(Context context, ActivityResultCaller activityResultCaller) {
        this.context = context;
        this.activityResultCaller = activityResultCaller;

        requestPermissionLauncher = activityResultCaller.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                // required
                this.getLocationPermission();
            }
        });

        this.getLocationPermission();
    }

    public interface OnDeviceLocationFoundListener {
        void onDeviceLocationFound(LatLng latLng);
    }

    public boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isDeviceLocationFound() {
        return deviceLatLng != null;
    }

    public void requestDeviceLocation(OnDeviceLocationFoundListener onDeviceLocationFoundListener) {
        // Construct a FusedLocationProviderClient.
        FusedLocationProviderClient mFusedLocationProviderClient
                = LocationServices.getFusedLocationProviderClient(context);
        /**
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (isPermissionGranted()) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Location location = task.getResult();
                        if (location == null) {
                            Log.d(TAG, "onComplete:location is null");
                            return;
                        }
                        Log.d(TAG, "onComplete:location found");
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        deviceLatLng = currentLocation;
                        if (onDeviceLocationFoundListener != null)
                            onDeviceLocationFoundListener.onDeviceLocationFound(currentLocation);
                    } else {
                        Log.d(TAG, "getDeviceLocation onComplete: location not found");
                    }
                });
            } else {
                this.getLocationPermission();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        /*LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5 * 1000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        deviceLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "onLocationResult: " + " location found , Lat: " + location.getLatitude() + " lng: " + location.getLongitude());
                        break;
                    }
                }
                mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
            }
        };*/
    }

    public LatLng getDeviceLatLng() {
        return deviceLatLng;
    }

    /**
     * get the user permission to access device location
     */
    public void getLocationPermission() {
        if (!isPermissionGranted())
            // get the location permission from user
            // this will prompt user a dialog to give the location permission
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            /*ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);*/
    }
}

