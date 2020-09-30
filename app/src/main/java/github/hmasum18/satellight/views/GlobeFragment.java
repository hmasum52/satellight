package github.hmasum18.satellight.views;

import android.animation.ValueAnimator;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.util.Log;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import github.hmasum18.satellight.R;
import github.hmasum18.satellight.experimental.AtmosphereLayer;
import github.hmasum18.satellight.models.SatelliteBasicData;
import github.hmasum18.satellight.utils.GlobeUtils;
import github.hmasum18.satellight.utils.Utils;
import github.hmasum18.satellight.viewModel.MainViewModel;
import gov.nasa.worldwind.Navigator;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Location;
import gov.nasa.worldwind.geom.Offset;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layer.BackgroundLayer;
import gov.nasa.worldwind.layer.BlueMarbleLandsatLayer;
import gov.nasa.worldwind.layer.RenderableLayer;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.shape.Label;
import gov.nasa.worldwind.shape.Placemark;
import gov.nasa.worldwind.shape.TextAttributes;


public class GlobeFragment extends Fragment implements Choreographer.FrameCallback {

    public static final String TAG = "GlobeFragment:";

    //data sources
    private MainViewModel mainViewModel;

    //views
    private WorldWindow worldWindow;
    private RenderableLayer renderableLayer; //layer in which we can render our satellites and locations
    private Placemark lastplackmark;
    private boolean scaleUp = false;

    private Button mMapSwitchButton;
    private TextView mSatLatTV,mSatLngTV,mSatHeightTV,mSatSpeedTV,mDateTV;
    private ChipGroup mSatelliteChipGroup;

    //day night Animation settings
    private   Location sunLocation = new Location(	1.6,18.6);
    //protected double cameraDegreesPerSecond = 2.0;
    protected double lightLatDegreesPerSec = (0+15/60+0.1/3600)/60; //dif 0° 15' 00.1" direction west
    protected  double lightLngDegreesPerSec = (0+0/60+1/3600)/60; //dif 0° 00' 01.0" south
    protected long lastFrameTimeNanos;
    private AtmosphereLayer atmosphereLayer;
    protected boolean activityPaused;

    //data from nasa ssc api
    ArrayList<String> satCodeList = new ArrayList<>(Arrays.asList(
            "sun","iss","goes13","noaa19","aqua","cassiope","moon"
    ));
    public  Map<String, ArrayList<SatelliteBasicData> > allSatDatFromSSCMap = new HashMap<>();

    //for currently tracked satellite
    public String activeSatCode = "iss"; //this is iss by default but will change when another one selected
    public String prevSatCode = "";
    public Position activeSatPosition = new Position(0,0,0);
    public List<SatelliteBasicData> activeSatDataList = new ArrayList<>(); //store the currently active satellite
    public long timeIntervalBetweenTwoData = 0; //in seconds
    public int activeSatDataListIdx = -1; //what index data is applicable currently
    public boolean startSatAnimation = false; //after camera moved to activated satellite position start moving the satellite
    public ValueAnimator activeCameraValueAnimator; //move the camera to animatedly



    /**
     * Creates a new WorldWindow (GLSurfaceView) object.
     */
    public WorldWindow createWorldWindow(){
        this.worldWindow = new WorldWindow(getContext());
        worldWindow.getLayers().addLayer(new BackgroundLayer());
        worldWindow.getLayers().addLayer(new BlueMarbleLandsatLayer());
        return worldWindow;
    }

    public WorldWindow getWorldWindow(){
        return this.worldWindow;
    }

   /* @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
        //make the activity landscape for this fragment
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //data sources
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // Inflate the layout for this fragment
        //views
        View rootView = inflater.inflate(R.layout.fragment_globe, container, false);

        mMapSwitchButton = rootView.findViewById(R.id.globeFrag_googleMapBtn);
        //get the layout where we will add our globe
        FrameLayout globeLayout = rootView.findViewById(R.id.globe);
        // Add the WorldWindow view object to the layout that was reserved for the globe.
        globeLayout.addView(this.createWorldWindow());

        mSatLatTV = rootView.findViewById(R.id.globeFrag_latTV);
        mSatLngTV = rootView.findViewById(R.id.globeFrag_lngTV);
        mSatHeightTV = rootView.findViewById(R.id.globeFrag_heightTV);
        mSatSpeedTV = rootView.findViewById(R.id.globeFrag_speedTV);
        mDateTV = rootView.findViewById(R.id.globeFrag_dateTV);

        mSatelliteChipGroup = rootView.findViewById(R.id.globeFrag_satelliteCG);

        return rootView;
    }

    //fetch all the necessary data from mainViewModel
    public void fetchInitialData(){
        //fetch sub solar point
        /*mainViewModel.getCurrentSubSolarPointLocation().observe(getViewLifecycleOwner(), location ->{
            sunLocation = location;
            Log.w(TAG,"subSolarPoint new fetchedData: "+"sun lat:"+location.latitude+" sun lng: "+location.longitude);
        });*/

       fetchSatDataFromSSC(satCodeList);
    }

    public void fetchSatDataFromSSC(ArrayList<String> satCodeList){
        //fetch satellite data from nasa SSC
        String fromTime = Utils.getTimeAsString(System.currentTimeMillis()-(1000*60*10)); //before 10 min of current timestamp
        String toTime = Utils.getTimeAsString(System.currentTimeMillis()+1000*60*30); //after 30 min of current timestamp
        Log.w(TAG,fromTime+" && "+toTime);
        mainViewModel.getLocationOfSatellite(satCodeList,fromTime,toTime).observe(getViewLifecycleOwner(),satelliteBasicDataMap -> {
            Log.w(TAG,"number of sat data in the map:"+satelliteBasicDataMap.size());
            if(satelliteBasicDataMap.size()>=7){
                allSatDatFromSSCMap = satelliteBasicDataMap;
                activeSatDataList = satelliteBasicDataMap.get(activeSatCode);
                Log.w(TAG," recievedMap:"+satelliteBasicDataMap);
                locateSun();
                initSatPosition();
            }
        });
    }


    public void locateSun(){
        ArrayList<SatelliteBasicData> sunDataList = allSatDatFromSSCMap.get("sun");
        long timeDiff = (activeSatDataList.get(1).getTimestamp()-activeSatDataList.get(0).getTimestamp())/1000; //in sec

        long currentTimestamp = System.currentTimeMillis();
        long duration = (currentTimestamp - sunDataList.get(0).getTimestamp())/1000;
        double percent = (double)duration/timeDiff;

        int currentIdx = (int)percent;
        currentIdx = Math.max(currentIdx, 0);

        Log.w(TAG," init sun position : currentIdx:"+currentIdx);
        percent -= currentIdx;

        SatelliteBasicData startData =  sunDataList.get(currentIdx);
        SatelliteBasicData secondData = sunDataList.get(1+currentIdx);

        double lat = startData.getLat()*(1-percent) + percent*secondData.getLat();
        double lng = (1-percent)*startData.getLng() + percent*secondData.getLng();

        sunLocation = new Location(lat,lng);
        Log.w(TAG,"subSolarPoint new fetchedData: "+"sun lat:"+lat+" sun lng: "+lng);
    }

    public void initSatPosition(){

        startSatAnimation = false;
        if(activeCameraValueAnimator !=null && activeCameraValueAnimator.isRunning()){
            activeCameraValueAnimator.end();
            Log.w(TAG," stopped running camera value animator");
        }


        Log.w(TAG,"active satDataSize:"+ activeSatDataList.size());

        timeIntervalBetweenTwoData = (activeSatDataList.get(1).getTimestamp()-activeSatDataList.get(0).getTimestamp())/1000; //in sec

        long currentTimestamp = System.currentTimeMillis();
        long duration = (currentTimestamp - activeSatDataList.get(0).getTimestamp())/1000;
        Log.w(TAG,"fetchData: Duration:"+duration);
        double percent = (double)duration/timeIntervalBetweenTwoData;

        int currentIdx = (int)percent;
        currentIdx = Math.max(currentIdx, 0);

        Log.w(TAG," iniitSat: currentIdx:"+currentIdx);
        percent -= currentIdx;

        SatelliteBasicData startData =  activeSatDataList.get(currentIdx);
        SatelliteBasicData secondData = activeSatDataList.get(1+currentIdx);

        double lat = startData.getLat()*(1-percent) + percent*secondData.getLat();
        double lng = (1-percent)*startData.getLng() + percent*secondData.getLng();
        double altitude = (1-percent)*startData.getHeight() + percent*secondData.getHeight();

        activeSatPosition = new Position(lat, lng, altitude);
        if(lastplackmark != null){
            removeSatellite(lastplackmark);
        }
        lastplackmark = GlobeUtils.addSatelliteToRenderableLayer(renderableLayer, activeSatPosition,R.drawable.satellite_one);;

        Log.w(TAG,"init: "+startData.getId());
        if(altitude<2000&& (worldWindow.getNavigator().getAltitude()/1000)<2000 && !startData.getId().equals(prevSatCode)){
            Position temp = new Position(lat/3,lng/3,altitude+5000);
            moveCamera(temp,500,"sat");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    moveCamera(activeSatPosition,600,"sat");
                }
            },500);
        }else{
            moveCamera(activeSatPosition,1100,"sat");
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startSatAnimation = true;
                activeSatDataListIdx = -1;
            }
        },1100);

        Map<String,Object> dataMap = new HashMap<>();
        dataMap.put("lat", startData.getLat());
        dataMap.put("lng", startData.getLng());
        dataMap.put("height", startData.getHeight());
        dataMap.put("timestamp", startData.getTimestamp());
        updateUI(dataMap);
    }


    public void updateUI(Map<String,Object> data){
        mSatLatTV.setText("Lat:"+String.format("%.3f",data.get("lat") ) );
        mSatLngTV.setText("Lng:"+String.format("%.3f",data.get("lng")) );
        mSatHeightTV.setText("Height:"+String.format("%.2f",data.get("height"))+"km");
        mDateTV.setText("Date:"+ new Date( (long)data.get("timestamp") ));
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GlobeUtils.addSatelliteToChipGroup(this,mSatelliteChipGroup,"ISS",R.drawable.satellite_one);
        GlobeUtils.addSatelliteToChipGroup(this,mSatelliteChipGroup,"NOAA-19",R.drawable.satellite_one);
        GlobeUtils.addSatelliteToChipGroup(this,mSatelliteChipGroup,"Aqua",R.drawable.satellite_one);
        GlobeUtils.addSatelliteToChipGroup(this,mSatelliteChipGroup,"GOES-13",R.drawable.satellite_one);
        GlobeUtils.addSatelliteToChipGroup(this,mSatelliteChipGroup,"CASSIOPE",R.drawable.satellite_one);
        GlobeUtils.addSatelliteToChipGroup(this,mSatelliteChipGroup,"MOON",R.drawable.satellite_one);
        //fetch all the data from mainViewModel
        fetchInitialData();

        mMapSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(GlobeFragment.this).navigate(R.id.action_globeFragment_to_googleMapFragment);
            }
        });

        // Create a layer to display the  labels and satellites
        renderableLayer = new RenderableLayer();
        worldWindow.getLayers().addLayer(renderableLayer);

        //design the label
        TextAttributes textAttributes = new TextAttributes();
        textAttributes.setTextColor(new Color(0,0,0,1));  //black
        textAttributes.setOutlineColor(new Color(1,1,1,1)); //white
        textAttributes.setOutlineWidth(5);
        textAttributes.setTextOffset(Offset.bottomRight());

        Position bheramara = new Position(24.019686, 88.993371,0);
        Label label = new Label(bheramara,"Bangladesh",textAttributes);
        label.setRotation(WorldWind.RELATIVE_TO_GLOBE); //will rotate when we will rotate the globe

        renderableLayer.addRenderable(label); // add the label to layer
        

        //dummy sun location
        //for day-night feature add the sun location
        Location sunLocation = new Location(1.6,13.4);
        atmosphereLayer = new AtmosphereLayer();
        atmosphereLayer.setLightLocation(sunLocation);
        worldWindow.getLayers().addLayer(atmosphereLayer);

        // Use this Activity's Choreographer to animate the day-night cycle.
        // and also animate the globe
        Choreographer.getInstance().postFrameCallback(this);
    }
    public void removeSatellite(Placemark placemark){
        renderableLayer.removeRenderable(placemark);
    }

    public void moveCamera(Position position,long moveDuration,String mode){
        //debug
       // position.altitude = 1e7*2.1;

        double currentCameraLat = worldWindow.getNavigator().getLatitude();
        double currentCameraLng = worldWindow.getNavigator().getLongitude();
        double currentCameraHeight = worldWindow.getNavigator().getAltitude()/1000; //converts to km

        activeCameraValueAnimator = ValueAnimator.ofFloat((float)currentCameraLat,(float)position.latitude);
        activeCameraValueAnimator.setInterpolator(new LinearInterpolator());
        activeCameraValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float percent = animation.getAnimatedFraction();
                //Log.w(TAG," sat lat percent :"+percent);
                double lat = (1-percent)*currentCameraLat+percent*position.latitude;
                double lng = (1-percent)*currentCameraLng+percent*position.longitude;
                double altitude = (1-percent)*currentCameraHeight+percent*position.altitude;

                worldWindow.getNavigator().setLatitude(lat);
                worldWindow.getNavigator().setLongitude(lng);
                worldWindow.getNavigator().setAltitude((altitude*1000)); //converts to meter agian

                if(mode.equals("sat")){
                    Map<String,Object> dataMap = new HashMap<>();
                    dataMap.put("lat",lat);
                    dataMap.put("lng",lng);
                    dataMap.put("height",altitude);
                    dataMap.put("timestamp",System.currentTimeMillis());

                    Position currentSatPos = new Position(lat,lng,position.altitude*1000);
                    removeSatellite(lastplackmark);
                    lastplackmark = GlobeUtils.addSatelliteToRenderableLayer(renderableLayer,currentSatPos,R.drawable.satellite_one);
                    updateUI(dataMap);
                }
            }
        });
        activeCameraValueAnimator.setDuration(moveDuration);
        activeCameraValueAnimator.start();
    }

    /**
     * Resumes the WorldWindow's rendering thread
     */
    @Override
    public void onResume() {
        super.onResume();
        this.worldWindow.onResume(); // resumes a paused rendering thread

        // Resume the day-night cycle animation.
        this.activityPaused = false;
        this.lastFrameTimeNanos = 0;
        Choreographer.getInstance().postFrameCallback(this);
    }

    /**
     * Pauses the WorldWindow's rendering thread
     */
    @Override
    public void onPause() {
        super.onPause();
        this.worldWindow.onPause(); // pauses the rendering thread

        // Stop running the night animation when this activity is paused.
        this.activityPaused = true;
        this.lastFrameTimeNanos = 0;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (this.lastFrameTimeNanos != 0) {
            // Compute the frame duration in seconds.
            double frameDurationSeconds = (frameTimeNanos - this.lastFrameTimeNanos) * 1.0e-9;
           // double cameraDegrees = (frameDurationSeconds * this.cameraDegreesPerSecond);

            //find lat and lng difference in degree
            double lightLatDiffDegrees = (frameDurationSeconds * this.lightLatDegreesPerSec);
            double lightLngDiffDegrees = (frameDurationSeconds * this.lightLngDegreesPerSec);

            this.sunLocation.latitude -= lightLatDiffDegrees;
            double lat  = this.sunLocation.latitude<-90 ? -this.sunLocation.latitude -90  : this.sunLocation.latitude;
            this.sunLocation.longitude -= lightLngDiffDegrees;
            double lng = this.sunLocation.longitude<-180 ? -this.sunLocation.longitude-180 : this.sunLocation.longitude;

                    // Move the navigator to simulate the Earth's rotation about its axis.
            Navigator navigator = getWorldWindow().getNavigator();
            //navigator.setLongitude(navigator.getLongitude() - cameraDegrees);

            // Move the sun location to simulate the Sun's rotation about the Earth.
            this.sunLocation.set(lat , lng);
            this.atmosphereLayer.setLightLocation(this.sunLocation);

            // Redraw the WorldWindow to display the above changes.
            this.getWorldWindow().requestRedraw();
        }

        if (!this.activityPaused) { // stop animating when this Activity is paused
            Choreographer.getInstance().postFrameCallback(this);
        }

        //after satellite data loaded
        if(activeSatDataList.size()>0)
        {
            animateSatellite();
        }


        this.lastFrameTimeNanos = frameTimeNanos;
    }

    public void animateSatellite(){
        long currentTimestamp = System.currentTimeMillis();
        long duration = (currentTimestamp - activeSatDataList.get(0).getTimestamp())/1000;

        int idx = (int)( duration/timeIntervalBetweenTwoData);
       // Log.w(TAG,"current idx:"+idx);
        if(idx>=0 && idx< activeSatDataList.size()-1 && idx != activeSatDataListIdx && startSatAnimation)
        {
            activeSatDataListIdx = idx;
            Log.w(TAG,"acitveSat:"+ activeSatDataListIdx);
            SatelliteBasicData data = activeSatDataList.get(idx+1);
            Position pos = new Position(data.getLat(),data.getLng(),data.getHeight());
            long timestampDiff = activeSatDataList.get(idx+1).getTimestamp() - activeSatDataList.get(idx).getTimestamp();
            Log.w(TAG,"timestampDiff "+ timestampDiff);

            moveCamera(pos,timestampDiff,"sat"); //as data difference is 60 sec

            if(activeSatDataListIdx>activeSatDataList.size()-2)
                fetchSatDataFromSSC(satCodeList);
        }
    }

}

