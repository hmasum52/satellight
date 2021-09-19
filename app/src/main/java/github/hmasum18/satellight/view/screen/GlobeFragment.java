package github.hmasum18.satellight.view.screen;

import android.animation.ValueAnimator;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import com.google.android.gms.maps.model.LatLng;
import com.neosensory.tlepredictionengine.Tle;

import java.util.ArrayList;

import javax.inject.Inject;

import github.hmasum18.satellight.R;
import github.hmasum18.satellight.databinding.FragmentGlobeBinding;
import github.hmasum18.satellight.databinding.FragmentGoogleMapBinding;
import github.hmasum18.satellight.experimental.AtmosphereLayer;
import github.hmasum18.satellight.service.model.Satellite;
import github.hmasum18.satellight.service.model.SatelliteTrajectory;
import github.hmasum18.satellight.service.model.TrajectoryData;
import github.hmasum18.satellight.utils.tle.TleToGeo;
import github.hmasum18.satellight.view.OnSelectedSatelliteUpdateListener;
import github.hmasum18.satellight.view.adapter.SatelliteListAdapter;
import github.hmasum18.satellight.view.screen.googlemap.DeviceLocationFinder;
import github.hmasum18.satellight.viewModel.MainViewModel;
import github.hmasum18.satellight.view.MainActivity;
import gov.nasa.worldwind.Navigator;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Location;
import gov.nasa.worldwind.geom.Offset;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layer.BlueMarbleLandsatLayer;
import gov.nasa.worldwind.layer.BlueMarbleLayer;
import gov.nasa.worldwind.layer.RenderableLayer;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.shape.Label;
import gov.nasa.worldwind.shape.TextAttributes;


public class GlobeFragment extends Fragment implements Choreographer.FrameCallback {

    public static final String TAG = "GlobeFragment:";

    //for accessing common method of all the fragments
    MainActivity mainActivity;

    private FragmentGlobeBinding mVB;

    @Inject
    DeviceLocationFinder deviceLocationFinder;

    @Inject
    SatelliteListAdapter satelliteListAdapter;

    @Inject
    MainViewModel mainViewModel;

    //views
    @Inject
    WorldWindow worldWindow;

    private RenderableLayer renderableLayer; //layer in which we can render our satellites and locations

    //private Placemark lastplackmark;
    private boolean scaleUp = false;


    //day night Animation settings
    private   Location sunLocation = new Location(	1.6,18.6);
    //protected double cameraDegreesPerSecond = 2.0;
    protected double lightLatDegreesPerSec = (0+15/60+0.1/3600)/60; //dif 0° 15' 00.1" direction west
    protected  double lightLngDegreesPerSec = (0+0/60+1/3600)/60; //dif 0° 00' 01.0" south
    protected long lastFrameTimeNanos;
    private AtmosphereLayer atmosphereLayer;
    protected boolean fragmentPaused;


    //for currently tracked satellite
    public String prevSatCode = "";
    public Position activeSatPosition = new Position(0,0,0);
    public long intervalInSec = 10; //in seconds
    public boolean satelliteMoving = false; //after camera moved to activated satellite position start moving the satellite
    public ValueAnimator activeCameraValueAnimator; //move the camera to animatedly


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");

        //make the activity landscape for this fragment
        //getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
  
        mainActivity = (MainActivity) this.getActivity();
        mainActivity.activityComponent.inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        // Inflate the layout for this fragment
        //views
        mVB = FragmentGlobeBinding.inflate(inflater, container, false);

        mVB.globeFrameLayout.addView(this.worldWindow);

        return mVB.getRoot();
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: ");

        // Create a layer to display the  labels and satellites
        renderableLayer = new RenderableLayer();
        worldWindow.getLayers().addLayer(renderableLayer);

        deviceLocationFinder.requestDeviceLocation(latLng -> {
            //design the label
            TextAttributes textAttributes = new TextAttributes();
            textAttributes.setTextColor(new Color(0,0,0,1));  //black
            textAttributes.setOutlineColor(new Color(1,1,1,1)); //white
            textAttributes.setOutlineWidth(5);
            textAttributes.setTextOffset(Offset.bottomRight());

            Position devicePosition = new Position(latLng.latitude, latLng.longitude,0);
            Label label = new Label(devicePosition,"Bangladesh",textAttributes);
            label.setRotation(WorldWind.RELATIVE_TO_GLOBE); //will rotate when we will rotate the globe
            renderableLayer.addRenderable(label); // add the label to layer
        });

        //fetch all the data from mainViewModel
        fetchInitialData();
    }


    /**
     * Resumes the WorldWindow's rendering thread
     */
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        this.worldWindow.onResume(); // resumes a paused rendering thread

        // Resume the day-night cycle animation.
        this.fragmentPaused = false;
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
        if(activeCameraValueAnimator!=null
                && activeCameraValueAnimator.isRunning())
            activeCameraValueAnimator.end();

        Choreographer.getInstance().removeFrameCallback(this);
        this.fragmentPaused = true;
        this.lastFrameTimeNanos = 0;
        this.worldWindow.onPause();
        Log.w(TAG," globe fragment paused and detached ");
    }

    /**
     * fetch all the necessary data from mainViewModel
     */
    public void fetchInitialData(){
        locateSun();
        satelliteListAdapter.setSelectedSatelliteUpdateListener(new OnSelectedSatelliteUpdateListener() {
            @Override
            public void onSelectedSatelliteUpdate(Satellite satellite) {
                // when selected satellite is changed
                // we init the new satellite data
                initSatPosition(satellite);
            }
        });

    }


    /**
     * This method get sun data from the map that is got from Nasa SSC web service
     * Then locate the sun at its sub solar point to make day night animation
     * called from fetchSatDataFromSSE method
     */
    public void locateSun(){
        ArrayList<TrajectoryData> sunDataList = mainActivity.allSatDatFromSSCMap.get("sun");

        if(sunDataList.size()<2)
            return;

        long timeDiff = (sunDataList.get(1).getTimestamp()-sunDataList.get(0).getTimestamp())/1000; //in sec

        long currentTimestamp = System.currentTimeMillis();
        long duration = (currentTimestamp - sunDataList.get(0).getTimestamp())/1000;
        double percent = (double)duration/timeDiff;

        int currentIdx = (int)percent;
        currentIdx = Math.max(currentIdx, 0);

        Log.w(TAG," init sun position : currentIdx:"+currentIdx);
        percent -= currentIdx;

        TrajectoryData startData =  sunDataList.get(currentIdx);
        TrajectoryData secondData = sunDataList.get(1+currentIdx);

        double lat = startData.getLat()*(1-percent) + percent*secondData.getLat();
        double lng = (1-percent)*startData.getLng() + percent*secondData.getLng();

        sunLocation = new Location(lat,lng);
        Log.w(TAG,"subSolarPoint: "+"sun lat:"+lat+" sun lng: "+lng);
        //dummy sun location
        //for day-night feature add the sun location
        atmosphereLayer = new AtmosphereLayer();
        atmosphereLayer.setLightLocation(sunLocation);
        worldWindow.getLayers().addLayer(atmosphereLayer);
    }

    public void moveCamera(Position position,long moveDuration,double prevV,double currentV){
        double currentCameraLat = worldWindow.getNavigator().getLatitude();
        double currentCameraLng = worldWindow.getNavigator().getLongitude();
        double currentCameraHeight = worldWindow.getNavigator().getAltitude()/1000; //converts to km

        activeCameraValueAnimator = ValueAnimator.ofFloat((float)currentCameraLat,(float)position.latitude);
        activeCameraValueAnimator.setInterpolator(new LinearInterpolator());
        activeCameraValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if(fragmentPaused)
                    return;

                float percent = animation.getAnimatedFraction();
                //Log.w(TAG," sat lat percent :"+percent);
                double lat = (1-percent)*currentCameraLat+percent*position.latitude;
                double lng = (1-percent)*currentCameraLng+percent*position.longitude;
                double altitude = (1-percent)*currentCameraHeight+percent*position.altitude;
                double velocity = (1-percent)*prevV+percent*currentV;

                worldWindow.getNavigator().setLatitude(lat);
                worldWindow.getNavigator().setLongitude(lng);
                worldWindow.getNavigator().setAltitude((altitude*1000)); //converts to meter agian
            }
        });
        activeCameraValueAnimator.setDuration(moveDuration);
        activeCameraValueAnimator.start();
    }


    /**
     * locate the satellite and navigator camera accordingly after satellite data is initialized
     *  called from fetchSatDataFromSSE method
     */
    public void initSatPosition(Satellite satellite){
        Log.d(TAG, "initSatPosition: satellite name: "+satellite.getName());

        satelliteMoving = false;
        if(activeCameraValueAnimator !=null && activeCameraValueAnimator.isRunning()){
            activeCameraValueAnimator.end();
            Log.w(TAG," stopped running camera value animator");
        }

        Tle tle = satellite.extractTle();
        LatLng latLng = deviceLocationFinder.getDeviceLatLng();
        SatelliteTrajectory startPoint = TleToGeo.getSatellitePosition(tle,System.currentTimeMillis(),latLng);
        SatelliteTrajectory endPoint = TleToGeo.getSatellitePosition(tle,System.currentTimeMillis() + intervalInSec*1000,latLng);


        double lat = startPoint.getLat();
        double lng = startPoint.getLng();
        double altitude = startPoint.getHeight();
        double velocity = startPoint.getSpeed();

        activeSatPosition = new Position(lat, lng, altitude);

        if(altitude<2000 && (worldWindow.getNavigator().getAltitude()/1000)<2000){
            Position temp = new Position(lat/3,lng/3,altitude+5000);
            moveCamera(temp,500,velocity,velocity/2);
            new Handler().postDelayed(() ->
                    moveCamera(activeSatPosition,500, velocity/2,velocity)
                    ,500);
        }else{
            moveCamera(activeSatPosition,500,velocity,velocity);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                satelliteMoving = true;
            }
        },1050);
    }




    /**
     * provide us every frame which helps to animate the satellite smoothly
     * idea taken from World wind api example of day night animation
     * @param frameTimeNanos
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        if (this.lastFrameTimeNanos != 0) {
            // Compute the frame duration in seconds.
            double frameDurationSeconds = (frameTimeNanos - this.lastFrameTimeNanos) * 1.0e-9;
            // double cameraDegrees = (frameDurationSeconds * this.cameraDegreesPerSecond);

            // find lat and lng difference in degree
            double lightLatDiffDegrees = (frameDurationSeconds * this.lightLatDegreesPerSec);
            double lightLngDiffDegrees = (frameDurationSeconds * this.lightLngDegreesPerSec);

            this.sunLocation.latitude -= lightLatDiffDegrees;
            double lat  = this.sunLocation.latitude<-90 ? -this.sunLocation.latitude -90  : this.sunLocation.latitude;
            this.sunLocation.longitude -= lightLngDiffDegrees;
            double lng = this.sunLocation.longitude<-180 ? -this.sunLocation.longitude-180 : this.sunLocation.longitude;

                    // Move the navigator to simulate the Earth's rotation about its axis.
            Navigator navigator = worldWindow.getNavigator();
            //navigator.setLongitude(navigator.getLongitude() - cameraDegrees);

            // Move the sun location to simulate the Sun's rotation about the Earth.
            this.sunLocation.set(lat , lng);
            this.atmosphereLayer.setLightLocation(this.sunLocation);

            // Redraw the WorldWindow to display the above changes.
            this.worldWindow.requestRedraw();
        }

        // if the fragment is not stopped then call the function again to
        // continue the animation
        if (!this.fragmentPaused) {
            Choreographer.getInstance().postFrameCallback(this);
        }else{
            Choreographer.getInstance().removeFrameCallback(this);
        }

        this.lastFrameTimeNanos = frameTimeNanos;
    }


    /**
     * animate the satellite and move the camera after certain amount of time
     */
    /*public void animateSatellite(){
        long currentTimestamp = System.currentTimeMillis();
        long duration = (currentTimestamp - activeSatDataList.get(0).getTimestamp())/1000;

        int idx = (int)( duration/ intervalInSec);
       // Log.w(TAG,"current idx:"+idx);
        if(idx>=0 && idx< activeSatDataList.size()-1 && idx != activeSatDataListIdx && satelliteMoving)
        {
            activeSatDataListIdx = idx;
            Log.w(TAG,"acitveSat:"+ activeSatDataListIdx);
            TrajectoryData prevData = activeSatDataList.get(idx);
            TrajectoryData data = activeSatDataList.get(idx+1);
            Position position = new Position(data.getLat(),data.getLng(),data.getHeight());
            long timestampDiff = activeSatDataList.get(idx+1).getTimestamp() - activeSatDataList.get(idx).getTimestamp();
            Log.w(TAG,"timestampDiff "+ timestampDiff);

            //fetch dat again
            if(idx>= activeSatDataList.size()-3){
                *//*long start = mainActivity.lastRequestedTimestampEnd - 1000*60*5;
                long end = mainActivity.lastRequestedTimestampEnd + 1000*60*20;*//*
               // mainViewModel.callForDataAgain(start,end, mainActivity.deviceLatLng);
            }
            moveCamera(position,timestampDiff,"sat",prevData.getVelocity(),data.getVelocity()); //as data difference is 60 sec
        }
    }*/

}

