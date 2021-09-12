package github.hmasum18.satellight.view.screen.googlemap;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.chip.ChipGroup;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import github.hmasum18.satellight.R;
import github.hmasum18.satellight.dagger.component.ActivityComponent;
import github.hmasum18.satellight.dagger.component.AppComponent;
import github.hmasum18.satellight.dagger.module.ActivityModule;
import github.hmasum18.satellight.databinding.FragmentGoogleMapBinding;
import github.hmasum18.satellight.service.model.Satellite;
import github.hmasum18.satellight.service.model.SatelliteTrajectory;
import github.hmasum18.satellight.utils.GlobeUtils;
import github.hmasum18.satellight.utils.MapUtils;
import github.hmasum18.satellight.utils.tle.TleToGeo;
import github.hmasum18.satellight.view.App;
import github.hmasum18.satellight.view.adapter.SatelliteListAdapter;
import github.hmasum18.satellight.viewModel.MainViewModel;
import github.hmasum18.satellight.view.MainActivity;

public class GoogleMapFragment extends Fragment implements OnMapReadyCallback {
    public static final String TAG = "GoogleMapFragment:";

    //for accessing common method of all the fragments
    MainActivity mainActivity;

    @Inject
    DeviceLocationFinder deviceLocationFinder;

    @Inject
    SatelliteListAdapter satelliteListAdapter; // to get the current selected satellite data

    //data source
    @Inject
    MainViewModel mainViewModel;

    //view
    private FragmentGoogleMapBinding mVB;

    private GoogleMap mMap;
    int[] rawMapStyles = {R.raw.dark_map, R.raw.night_map, R.raw.aubergine_map, R.raw.assassins_creed_map};
    private Marker movingSatelliteMarker; //satellite icon as marker
    private ArrayList<Polyline> drawnPolyLine = new ArrayList<>(); //polyline drawn to show a satellite trajectory

    //to show satellite
    public SatelliteTrajectory startPoint;
    public SatelliteTrajectory endPoint;
    public long timeIntervalBetweenTwoData = 10 * 1000; //10 sec
    public boolean startSatAnimation = false; //after camera moved to activated satellite position start moving the satellite
    public ValueAnimator activeSatAnimator;
    public ViewPropertyAnimator satelliteAnimation;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        mainActivity = (MainActivity) this.getActivity();

        injectDependencies();
        mainActivity.activityComponent.inject(this);

       /* deviceLocationFinder = mainActivity.getDeviceLocationFinder();
        if(deviceLocationFinder == null){
            deviceLocationFinder = new DeviceLocationFinder(mainActivity);
            mainActivity.setDeviceLocationFinder(deviceLocationFinder);
        }*/
    }

    private void injectDependencies() {
        AppComponent appComponent = ((App) mainActivity.getApplication()).getAppComponent();
        mainActivity.activityComponent = appComponent.activityComponentBuilder()
                .activityModule(new ActivityModule(mainActivity))
                .build();
        mainActivity.activityComponent.inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        // Inflate the layout for this fragment
        mVB = FragmentGoogleMapBinding.inflate(inflater, container, false);
        return mVB.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: ");
        // initialize map
        initMap();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
    }

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach: ");
    }

    /**
     * get the map fragment and start the MapAsync
     */
    public void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.googleMap);
        try {
            mapFragment.getMapAsync(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is called when map async task is finished
     *
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this.getContext(),rawMapStyles[3]));
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        deviceLocationFinder.requestDeviceLocation(latLng -> {
            Log.d(TAG, "onMapReady: location found: "+latLng);
            getSelectedSatelliteData();
        });
    }

    private void getSelectedSatelliteData() {
        Log.d(TAG, "getSelectedSatelliteData: ");
        satelliteListAdapter.setSelectedSatelliteUpdateListener(selectedSatellite -> {
            if(selectedSatellite == null)
                return;
            Satellite satellite = satelliteListAdapter.getSelectedSatellite();
            Log.d(TAG, "getSelectedSatelliteData: selected satellite: "+satellite.getName());
            startPoint = TleToGeo.getSatellitePosition(
                    satellite.extractTle(),
                    deviceLocationFinder.getDeviceLatLng());
            endPoint = TleToGeo.getSatellitePosition(
                    satellite.extractTle(),
                    System.currentTimeMillis() + timeIntervalBetweenTwoData,
                    deviceLocationFinder.getDeviceLatLng()
            );

            initSatPosition();
        });
    }


    public void initSatPosition() {
        Log.d(TAG, "initSatPosition: inside.");
        //make null so that know line is drawn
        startSatAnimation = false;
        if (activeSatAnimator != null && activeSatAnimator.isRunning()) {
            Log.d(TAG, "Previous satellite animator is removed");
            activeSatAnimator.end();
            activeSatAnimator = null;
        }

        // remove all the polyline drawn for previous satellite
        for (Polyline polyline : drawnPolyLine) {
            polyline.remove();
        }
        drawnPolyLine = new ArrayList<>();

        updateSatelliteLocation(startPoint, 0);

       /* new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startSatAnimation = true;
                moveSatellite();
            }
        }, timeIntervalBetweenTwoData);*/
    }

    /**
     * This recursive function helps to move the satellite
     */
    public void moveSatellite() {
        //Handler handler = new Handler();
        if (!startSatAnimation) {
            return;
        }

        satelliteAnimation = mVB.getRoot()
                .animate().scaleX(1.0f)
                .setDuration(timeIntervalBetweenTwoData)
                .withEndAction(() -> {
                    // new start data
                    // new end data
                    //updateSatelliteLocation(, timeIntervalBetweenTwoData);
                   // moveSatellite();
                });
    }


    /**
     * Move the map camera to a certain lat lng
     *
     * @param latLng
     * @param zoom
     */
    public void moveCamera(LatLng latLng, float zoom) {
        //  Log.w(TAG,"moveCamera: moving camera to: lat: "+latLng.latitude+" , lng: "+latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    /**
     * add satellite icon as marker at a certain lat lng
     *
     * @param latLng
     * @return
     */
    private Marker addSatelliteAndGet(LatLng latLng) {
        Log.d(TAG, "addSatelliteAndGet: ");
        BitmapDescriptor defaultSatBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(MapUtils.getSatelliteBitmap(this.getContext()));
       // BitmapDescriptor urlSatBitmapDescriptor = getMarkerIconFromDrawable(GlobeUtils.satelliteIconMap.get(name));

        //if satellite image is not loaded from the link use the default satellite image as descriptor for marker
        BitmapDescriptor bitmapDescriptor = /*urlSatBitmapDescriptor != null ? urlSatBitmapDescriptor :*/ defaultSatBitmapDescriptor;
        return mMap.addMarker(new MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor));
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        if (drawable == null) {
            Log.d(TAG, " drawable is null");
            return null;
        }
        Log.d(TAG, " drawable is not null");

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public void animateCamera(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(0f).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    /**
     * update the satellite location which help us to achieve the satellite animation
     *
     * @param trajectoryData is the lat lng where the satellite will move to
     */
    private void updateSatelliteLocation(SatelliteTrajectory trajectoryData, long durationMilli) {
        moveCamera(trajectoryData.getLatLng(), 0f);
        Log.d(TAG, "updateSatelliteLocation: "+trajectoryData);
        if (movingSatelliteMarker != null)
            movingSatelliteMarker.remove(); // remove the previous marker to redraw the marker

        movingSatelliteMarker = addSatelliteAndGet(new LatLng(trajectoryData.getLat(),
                trajectoryData.getLng()));

        if (startPoint == null) {
            startPoint = endPoint = trajectoryData;
            movingSatelliteMarker.setPosition(new LatLng(trajectoryData.getLat(), trajectoryData.getLng()));
            movingSatelliteMarker.setAnchor(0.4f, 0.4f);

           /* Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("satName", mainActivity.activeSatCode);
            dataMap.put("lat", trajectoryData.getLat());
            dataMap.put("lng", trajectoryData.getLng());
            dataMap.put("height", trajectoryData.getAlt());
            dataMap.put("velocity", trajectoryData.getSpeed());
            dataMap.put("timestamp", System.currentTimeMillis());
            mainActivity.updateUI(dataMap);*/

            animateCamera(new LatLng(trajectoryData.getLat(), trajectoryData.getLng()));
            //moveCamera(latLng,0f);
        } else {
            startPoint = endPoint;
            endPoint = trajectoryData;
            movingSatelliteMarker.setPosition(new LatLng(trajectoryData.getLat(), trajectoryData.getLng()));
            movingSatelliteMarker.setAnchor(0.4f, 0.4f);

            activeSatAnimator = MapUtils.satelliteAnimation();
            activeSatAnimator.setDuration(durationMilli);
            activeSatAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (endPoint != null && endPoint != null) {

                        if (startSatAnimation) {
                            double multiplier = animation.getAnimatedFraction();

                            //ease in the value
                            LatLng nextLocation = new LatLng(
                                    multiplier * endPoint.getLat() + (1 - multiplier) * startPoint.getLat(),
                                    multiplier * endPoint.getLng() + (1 - multiplier) * startPoint.getLng()
                            );

                            double nextHeight = multiplier * endPoint.getHeight() + (1 - multiplier) * startPoint.getHeight();
                            double nextVelocity = multiplier * endPoint.getSpeed() + (1 - multiplier) * startPoint.getSpeed();

                            movingSatelliteMarker.setPosition(nextLocation);
                            movingSatelliteMarker.setAnchor(0.5f, 0.5f);

                            Map<String, Object> dataMap = new HashMap<>();
                            dataMap.put("satName", mainActivity.activeSatCode);
                            dataMap.put("lat", nextLocation.latitude);
                            dataMap.put("lng", nextLocation.longitude);
                            dataMap.put("height", nextHeight);
                            dataMap.put("velocity", nextVelocity);
                            dataMap.put("timestamp", System.currentTimeMillis());
                            mainActivity.updateUI(dataMap);

                            //animateCamera(nextLocation);
                            // moveCamera(nextLocation,0f);
                            addLineBetweenTwoPoints(new LatLng(startPoint.getLat(), startPoint.getLng()), nextLocation);
                        }
                    }
                }
            });
            activeSatAnimator.start();
        }
    }

    public void addLineBetweenTwoPoints(final LatLng from, final LatLng to) {
        final PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.add(from);
        polylineOptions.add(to);
        polylineOptions.width(8f);
        polylineOptions.color(Color.RED);

        Polyline polyline = mMap.addPolyline(polylineOptions);
        drawnPolyLine.add(polyline);
        // animatePolyLineBetweenTwoPoints(polyline,from,to);
    }

    /**
     * get the deviceLocation and then call for satellite data
     */
    public void getDeviceLocation() {
        /**
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (deviceLocationFinder.isDeviceLocationFound()) {
            initSatPosition();
        } else {
            deviceLocationFinder.requestDeviceLocation(latLng -> {
                if (ActivityCompat.checkSelfPermission(mainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }

            });
        }

    }




    @Override
    public void onPause() {
        super.onPause();
        if (satelliteAnimation != null)
            satelliteAnimation.cancel();
        if (activeSatAnimator != null && activeSatAnimator.isRunning()) {
            activeSatAnimator.end();
        }
    }
}