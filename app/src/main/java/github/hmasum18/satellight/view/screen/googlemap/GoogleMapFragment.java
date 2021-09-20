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

import android.os.Handler;
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
import com.neosensory.tlepredictionengine.Tle;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import javax.inject.Inject;

import github.hmasum18.satellight.R;
import github.hmasum18.satellight.databinding.FragmentGoogleMapBinding;
import github.hmasum18.satellight.service.model.SatelliteTrajectory;
import github.hmasum18.satellight.utils.LatLngInterpolator;
import github.hmasum18.satellight.utils.MapUtils;
import github.hmasum18.satellight.utils.tle.TleToGeo;
import github.hmasum18.satellight.view.adapter.SatelliteListAdapter;
import github.hmasum18.satellight.viewModel.MainViewModel;
import github.hmasum18.satellight.view.MainActivity;

// life cycle
// onAttach->onCreate->onCreateView->onViewCreated->onResume->initMap->onMapReady->
// getSelectedSatelliteData()-> updateTrajectoryData(), initSatellitePosition() ->
// updateSatelliteLocation()
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
    public long timeIntervalBetweenTwoData = 20 * 1000; //20 sec
    public boolean satelliteMoving = false; //after camera moved to activated satellite position start moving the satellite
    private Handler satelliteMoveHandler;
    public ValueAnimator activeSatAnimator;
    public ViewPropertyAnimator satelliteAnimation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        mainActivity = (MainActivity) this.getActivity();
        if (mainActivity.activityComponent == null)
            mainActivity.initActivityComponent();
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
        satelliteMoveHandler = new Handler();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
        // initialize map
        initMap();
    }

    /**
     * get the map fragment and start the MapAsync
     */
    public void initMap() {
        Log.d(TAG, "initMap: ");
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
        Log.d(TAG, "onMapReady: ");
        mMap = googleMap;
        //mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this.getContext(),rawMapStyles[3]));
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        deviceLocationFinder.requestDeviceLocation(latLng -> {
            Log.d(TAG, "onMapReady: location found: " + latLng);

            // enable user location
            enableUserLocation();

            getSelectedSatelliteData();
        });
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(mainActivity
                , Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    private void getSelectedSatelliteData() {
        Log.d(TAG, "getSelectedSatelliteData: ");
        satelliteListAdapter.setSelectedSatelliteUpdateListener(selectedSatellite -> {
            if (selectedSatellite == null)
                return;
            Log.d(TAG, "getSelectedSatelliteData: selected satellite: " + selectedSatellite.getName());
            initSatPosition();
        });
    }

    private SatelliteTrajectory getSatelliteTrajectoryData(long timestamp) {
        Log.d(TAG, "updateTrajectoryData: ");
        Tle tle = satelliteListAdapter.getSelectedSatellite().extractTle();
        return TleToGeo.getSatellitePosition(tle, timestamp, deviceLocationFinder.getDeviceLatLng());
    }


    public void initSatPosition() {
        Log.d(TAG, "initSatPosition: inside.");
        //make null so that know line is drawn
        satelliteMoving = false; // finish the satellite move thread
        if (activeSatAnimator != null && activeSatAnimator.isRunning()) {
            Log.d(TAG, "Previous satellite animator is removed");
            activeSatAnimator.end();
        }

        // remove all the polyline drawn for previous satellite
        for (Polyline polyline : drawnPolyLine) {
            polyline.remove();
        }
        drawnPolyLine = new ArrayList<>();

        // add satellite marker in startPoint
        // init new selected satellite position.
        startPoint = getSatelliteTrajectoryData(System.currentTimeMillis());

        // add new satellite and move camera to new position
        addNewSatelliteToMap(startPoint.getLatLng());

        satelliteMoving = true;
        moveSatellite(); // recursive function that animates the satellite
    }

    private void addNewSatelliteToMap(LatLng latLng) {
        if (movingSatelliteMarker != null)
            movingSatelliteMarker.remove(); // remove the previous marker to redraw the marker

        // draw new satellite
        movingSatelliteMarker = addSatelliteMarker(latLng);

        movingSatelliteMarker.setPosition(latLng);
        movingSatelliteMarker.setAnchor(0.4f, 0.4f);

        // animate and move camera to new lat lng
        animateCamera(latLng);
    }

    /**
     * This recursive function helps to move the satellite
     */
    public void moveSatellite() {
        if (!satelliteMoving) { // thread finish condition.
            return;
        }

        if (activeSatAnimator != null && activeSatAnimator.isRunning()) {
            activeSatAnimator.end();
        }
        // next end point
        endPoint = getSatelliteTrajectoryData(System.currentTimeMillis() + timeIntervalBetweenTwoData);
        updateSatelliteLocation(endPoint, timeIntervalBetweenTwoData);

        satelliteMoveHandler.postDelayed(this::moveSatellite, timeIntervalBetweenTwoData);
    }

    /**
     * update the satellite location which help us to achieve the satellite animation
     */
    private void updateSatelliteLocation(SatelliteTrajectory to, long durationMilli) {
        final LatLng startPosition = movingSatelliteMarker.getPosition();
        final LatLng endPosition = to.getLatLng();

        activeSatAnimator = MapUtils.satelliteAnimation(durationMilli);
        activeSatAnimator.addUpdateListener(animation -> {
            if (satelliteMoving) {
                float fraction = animation.getAnimatedFraction();
                Log.d(TAG, "updateSatelliteLocation: fraction: " + fraction);
                //ease in the value
                LatLng nextLocation = LatLngInterpolator.interpolate(fraction, startPosition, endPosition);

                movingSatelliteMarker.setPosition(nextLocation);

                // animateCamera(nextLocation);
                // moveCamera(nextLocation,0f);
                addLineBetweenTwoPoints(startPosition, nextLocation);
            }
        });
        activeSatAnimator.start();
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
    private Marker addSatelliteMarker(LatLng latLng) {
        Log.d(TAG, "addSatelliteAndGet: ");
        BitmapDescriptor defaultSatBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(MapUtils.getSatelliteBitmap(mainActivity));
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

    /**
     * animate and move camera
     *
     * @param latLng is the end destination from current position.
     */
    public void animateCamera(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(0f).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
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

    @Override
    public void onPause() {
        super.onPause();
        satelliteMoving = false;
        if (satelliteAnimation != null)
            satelliteAnimation.cancel();
        if (activeSatAnimator != null && activeSatAnimator.isRunning()) {
            activeSatAnimator.end();
        }
    }
}