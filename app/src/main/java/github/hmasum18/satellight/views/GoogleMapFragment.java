package github.hmasum18.satellight.views;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import github.hmasum18.satellight.R;
import github.hmasum18.satellight.models.SatelliteBasicData;
import github.hmasum18.satellight.models.SatelliteData;
import github.hmasum18.satellight.models.TrajectoryData;
import github.hmasum18.satellight.utils.GlobeUtils;
import github.hmasum18.satellight.utils.MapUtils;
import github.hmasum18.satellight.utils.Utils;
import github.hmasum18.satellight.viewModel.MainViewModel;
import gov.nasa.worldwind.geom.Position;

public class GoogleMapFragment extends Fragment implements OnMapReadyCallback{

    private final static int  PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101;
    public static final String TAG = "GoogleMapFragment:";

    //for accessing common method of all the fragments
    MapsActivity mapsActivity;

    //data source
    private MainViewModel mainViewModel;

    //view
    private ChipGroup mSatelliteChipGroup;

    private GoogleMap mMap;
    private LocationCallback locationCallback;

    //to get the device location
    private FusedLocationProviderClient mFusedLocationProviderClient;
    int[] rawMapStyles = {R.raw.dark_map,R.raw.night_map,R.raw.aubergine_map,R.raw.assassins_creed_map};

    //to show satellite
    public TrajectoryData previousSatData;
    public TrajectoryData currentSatData;
    private Marker movingSatelliteMarker; //satellite icon as marker
    private ArrayList<Polyline> drawnPolyLine = new ArrayList<>(); //polyline drawn to show a satellite trajectory


    //data from nasa ssc api
    ArrayList<String> satCodeList = new ArrayList<>(Arrays.asList(
            "sun","moon"
    ));


    //for currently tracked satellite
    public String prevSatCode = "";
    public List<TrajectoryData> activeSatDataList = new ArrayList<>(); //store the currently active satellite
    public long timeIntervalBetweenTwoData = 0; //in seconds
    public int activeSatDataListIdx = -1; //what index data is applicable currently
    public boolean startSatAnimation = false; //after camera moved to activated satellite position start moving the satellite
    public ValueAnimator activeSatAnimator;
    public ViewPropertyAnimator satelliteAnimation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
        mapsActivity = (MapsActivity) this.getActivity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_google_map, container, false);
        mSatelliteChipGroup = rootView.findViewById(R.id.googleMapFrag_satelliteCG);
        //init the data source
        mainViewModel  = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //init the map
       // mapsActivity.progressBar.setVisibility(View.GONE);
        initMap();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this.getContext(),rawMapStyles[3]));
        //mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        GlobeUtils.addSatelliteToChipGroup(this,mSatelliteChipGroup,"moon",R.drawable.ic_moon);

        //get permission from user to access the location info
        getLocationPermission();
        fetchInitialData();
    }

    /**
     * get the map fragment and start the MapAsync
     */
    public void initMap(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.googleMap);
        mapFragment.getMapAsync(this);
    }


    /**
     * fetch all the necessary data from mainViewModel
     */
    public void fetchInitialData(){
        fetchSatDataFromSSC(satCodeList);
    }

    /**
     * name of the function suggest what it does
     * @param satCodeList
     */
    public void fetchSatDataFromSSC(ArrayList<String> satCodeList){
        //fetch satellite data from nasa SSC
        String fromTime = Utils.getTimeAsString(System.currentTimeMillis()-(1000*60*5)); //before 5 min of current timestamp
        String toTime = Utils.getTimeAsString(System.currentTimeMillis()+1000*60*20); //after 20 min of current timestamp
        Log.w(TAG,fromTime+" && "+toTime);
        mainViewModel.getLocationOfSatellite(satCodeList,fromTime,toTime).observe(requireActivity(),satelliteBasicDataMap -> {
            Log.w(TAG,"number of sat data in the map:"+satelliteBasicDataMap.size());
            if(satelliteBasicDataMap.size()>=2){
                mapsActivity.allSatDatFromSSCMap = satelliteBasicDataMap;
               // Log.w(TAG," recievedMap:"+satelliteBasicDataMap);
                if(activeSatDataList.size() == 0 && mapsActivity.isLocationPermissionGranted){
                    Log.w(TAG," requesting device loction from fetchSatDataFromSSC function");
                    getDeviceLocation();
                }
            }
        });
    }

    public void initSatPosition(){
        //make null so that know line is drawn
        startSatAnimation = false;
        previousSatData = null;
        currentSatData = null;
        if(activeSatAnimator!=null && activeSatAnimator.isRunning()){
            Log.w(TAG,"Previous satellite animator is removed");
            activeSatAnimator.end();
            activeSatAnimator = null;
        }
        for (Polyline polyline : drawnPolyLine) {
            polyline.remove();
        }
        drawnPolyLine = new ArrayList<>();

        timeIntervalBetweenTwoData = (activeSatDataList.get(1).getTimestamp()-activeSatDataList.get(0).getTimestamp())/1000; //in sec

        long currentTimestamp = System.currentTimeMillis();
        long duration = (currentTimestamp - activeSatDataList.get(0).getTimestamp())/1000;
        Log.w(TAG,"fetchData: Duration:"+duration);
        double percent = (double)duration/timeIntervalBetweenTwoData;

        int currentIdx = (int)percent;
        currentIdx = Math.max(currentIdx, 0);

        Log.w(TAG," iniitSat: currentIdx:"+currentIdx);

        if(currentIdx>=activeSatDataList.size()-3){
            long start = mapsActivity.lastRequestedTimestampEnd - 1000*60*5;
            long end = mapsActivity.lastRequestedTimestampEnd + 1000*60*20;
            mainViewModel.callForDataAgain(start,end,mapsActivity.deviceLatLng);
        }
        percent -= currentIdx;

        TrajectoryData startData =  activeSatDataList.get(currentIdx);
        TrajectoryData secondData = activeSatDataList.get(1+currentIdx);

        double lat = startData.getLat()*(1-percent) + percent*secondData.getLat();
        double lng = (1-percent)*startData.getLng() + percent*secondData.getLng();
        double altitude = (1-percent)*startData.getHeight() + percent*secondData.getHeight();

        TrajectoryData trajectoryData = new TrajectoryData(
                startData.getShortName(),lat,lng,altitude, System.currentTimeMillis());

        Log.w(TAG," initial location: "+trajectoryData);
        updateSatelliteLocation(trajectoryData,0);
        long remainingDuration = (long)(timeIntervalBetweenTwoData*(1-percent));
        updateSatelliteLocation(activeSatDataList.get(currentIdx+1), remainingDuration);
        activeSatDataListIdx = currentIdx+1;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startSatAnimation = true;
                moveSatellite();
            }
        },remainingDuration);
    }

    /**
     * This recursive function helps to move the satellite
     */
    public void moveSatellite(){
        //Handler handler = new Handler();
        if(!startSatAnimation) {
            return;
        }

        satelliteAnimation = mapsActivity.googleMapIBTN.animate().scaleX(1.0f).setDuration(timeIntervalBetweenTwoData*1000)
        .withEndAction(new Runnable() {
            @Override
            public void run() {
                activeSatDataListIdx++;
                Log.w(TAG," activeSatDataIdx of "+mapsActivity.activeSatCode+":"+activeSatDataListIdx);
                if(activeSatDataListIdx<activeSatDataList.size()-3){
                    updateSatelliteLocation(activeSatDataList.get(activeSatDataListIdx),timeIntervalBetweenTwoData );
                    moveSatellite();
                }else{
                    long start = mapsActivity.lastRequestedTimestampEnd - 1000*60*5;
                    long end = mapsActivity.lastRequestedTimestampEnd + 1000*60*20;
                    mainViewModel.callForDataAgain(start,end,mapsActivity.deviceLatLng);
                }
            }
        });
    }



    /**
     * Move the map camera to a certain lat lng
     * @param latLng
     * @param zoom
     */
    public void moveCamera(LatLng latLng, float zoom){
        //  Log.w(TAG,"moveCamera: moving camera to: lat: "+latLng.latitude+" , lng: "+latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));
    }

    /**
     * add satellite icon as marker at a certain lat lng
     * @param latLng
     * @return
     */
    private Marker addSatelliteAndGet(LatLng latLng,String name){
        BitmapDescriptor bitmapDescriptor1 = BitmapDescriptorFactory.fromBitmap(MapUtils.getSatelliteBitmap(this.getContext()));
        BitmapDescriptor bitmapDescriptor2 = getMarkerIconFromDrawable(GlobeUtils.satelliteIconMap.get(name));

        BitmapDescriptor bitmapDescriptor = bitmapDescriptor2!=null?  bitmapDescriptor2 : bitmapDescriptor1;
        return mMap.addMarker(new MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor));
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        if(drawable == null){
            Log.w(TAG," drawable is null");
            return  null;
        }
        Log.w(TAG," drawable is not null");

        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public void animateCamera(LatLng latLng){
        CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(0f).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    /**
     * update the satellite location which help us to achieve the satellite animation
     * @param satData is the lat lng where the satellite will move to
     */
    private void updateSatelliteLocation(TrajectoryData satData,long duration){
        if(movingSatelliteMarker!=null)
            movingSatelliteMarker.remove();
        movingSatelliteMarker = addSatelliteAndGet(new LatLng(satData.getLat(), satData.getLng()),satData.getShortName());

        if(previousSatData == null){
            currentSatData = satData;
            previousSatData = currentSatData;
            movingSatelliteMarker.setPosition(new LatLng(satData.getLat(),satData.getLng()));
            movingSatelliteMarker.setAnchor(0.4f,0.4f);

            Map<String,Object> dataMap = new HashMap<>();
            dataMap.put("satName",mapsActivity.activeSatCode);
            dataMap.put("lat", satData.getLat());
            dataMap.put("lng", satData.getLng());
            dataMap.put("height", satData.getHeight());
            dataMap.put("velocity",satData.getVelocity());
            dataMap.put("timestamp", System.currentTimeMillis());
            mapsActivity.updateUI(dataMap);

            animateCamera(new LatLng(satData.getLat(),satData.getLng()));
            //moveCamera(latLng,0f);
        }else{
            previousSatData = currentSatData;
            currentSatData = satData;
            movingSatelliteMarker.setPosition(new LatLng(satData.getLat(),satData.getLng()));
            movingSatelliteMarker.setAnchor(0.4f,0.4f);

            activeSatAnimator = MapUtils.satelliteAnimation();
            activeSatAnimator.setDuration(duration*1000);
            activeSatAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if(currentSatData != null && currentSatData != null){

                        if(startSatAnimation){
                            double multiplier = animation.getAnimatedFraction();

                            //ease in the value
                            LatLng nextLocation = new LatLng(
                                    multiplier * currentSatData.getLat() + (1 - multiplier) * previousSatData.getLat() ,
                                    multiplier * currentSatData.getLng() + (1 - multiplier) * previousSatData.getLng()
                            );

                            double nextHeight = multiplier * currentSatData.getHeight() + (1 - multiplier) * previousSatData.getHeight();
                            double nextVelocity = multiplier * currentSatData.getVelocity() + (1 - multiplier) * previousSatData.getVelocity();

                            movingSatelliteMarker.setPosition(nextLocation);
                            movingSatelliteMarker.setAnchor(0.5f,0.5f);

                            Map<String,Object> dataMap = new HashMap<>();
                            dataMap.put("satName",mapsActivity.activeSatCode);
                            dataMap.put("lat", nextLocation.latitude);
                            dataMap.put("lng", nextLocation.longitude);
                            dataMap.put("height", nextHeight);
                            dataMap.put("velocity",nextVelocity);
                            dataMap.put("timestamp", System.currentTimeMillis());
                            mapsActivity.updateUI(dataMap);

                            //animateCamera(nextLocation);
                            // moveCamera(nextLocation,0f);
                            addLineBetweenTwoPoints(new LatLng(previousSatData.getLat(),previousSatData.getLng()),nextLocation);
                        }
                    }
                }
            });
            activeSatAnimator.start();
        }
    }

    public void addLineBetweenTwoPoints(final LatLng from, final LatLng to){
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
        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.getActivity());

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5*1000);

        locationCallback = new LocationCallback(){

            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(locationResult == null){
                    Toast.makeText(getContext(),"Your location data not found . Please turn on your location. Using default location",Toast.LENGTH_SHORT).show();
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        mapsActivity.deviceLatLng = new LatLng(location.getLatitude(),location.getLongitude());
                       Toast.makeText(getContext()," location found , Lat: "+location.getLatitude()+" lng: "+location.getLongitude(),Toast.LENGTH_SHORT).show();
                        break;
                    }else{
                        Toast.makeText(getContext(),"Your location data not found . Please turn on your location. Using default location",Toast.LENGTH_SHORT).show();
                    }
                }
                if(mFusedLocationProviderClient!=null){
                    mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
                }
            }
        };


        /**
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {

            if (mapsActivity.isLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if(task.isSuccessful()){
                            Log.w(TAG,"getDeviceLocation onComplete: location found");
                            Location location = task.getResult();
                            if(location!=null){
                                mapsActivity.deviceLatLng = new LatLng(	location.getLatitude(),location.getLongitude() );
                            }else{
                                Toast.makeText(getContext(),"Your location data not found . Please turn on your location. Using default location",Toast.LENGTH_SHORT).show();
                                mFusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,null);
                            }
                            Log.w(TAG," requesting data from "+mapsActivity.deviceLatLng);

                            mMap.setMyLocationEnabled(true); //is location is found

                            mapsActivity.lastRequestedTimestampBegin = System.currentTimeMillis()-1000*60*5;
                            mapsActivity.lastRequestedTimestampEnd = System.currentTimeMillis()+1000*60*20;

                            mainViewModel.getAllSatelliteData(mapsActivity.lastRequestedTimestampBegin //before 5 minutes
                                    ,mapsActivity.lastRequestedTimestampEnd //after 20 minutes
                                    ,mapsActivity.deviceLatLng).observe(GoogleMapFragment.this.getViewLifecycleOwner(),stringSatelliteDataMap -> {

                                mapsActivity.allSatelliteData = stringSatelliteDataMap;
                                activeSatDataList = mapsActivity.allSatDatFromSSCMap.get(mapsActivity.activeSatCode);
                                if(activeSatDataList == null)
                                    activeSatDataList = stringSatelliteDataMap.get(mapsActivity.activeSatCode).getTrajectoryDataList();

                                for (Map.Entry<String, SatelliteData> temp :
                                        stringSatelliteDataMap.entrySet()) {
                                    GlobeUtils.addSatelliteToChipGroup(GoogleMapFragment.this, mSatelliteChipGroup,
                                            temp.getKey(), //shortName
                                            temp.getValue().getIconUrl()
                                            );
                                }

                                mapsActivity.progressBar.setVisibility(View.GONE);
                                mapsActivity.mainActvConstrainLayout.setVisibility(View.VISIBLE);

                                initSatPosition();
                            });
                        }else{
                            Log.w(TAG,"getDeviceLocation onComplete: location not found");
                        }
                    }
                });
            }
        }catch (SecurityException e){
            e.printStackTrace();
        }
    }

    /**
     * get the user permission to access device location
     */
    public void getLocationPermission(){
        if(ContextCompat.checkSelfPermission(
                this.getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        ){
            mapsActivity.isLocationPermissionGranted = true;
        } else {
            //get the location permission from user
            ActivityCompat.requestPermissions(this.getActivity(),
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }



    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(satelliteAnimation != null )
        satelliteAnimation.cancel();
        if(activeSatAnimator != null && activeSatAnimator.isRunning()){
            activeSatAnimator.end();
        }
    }
}