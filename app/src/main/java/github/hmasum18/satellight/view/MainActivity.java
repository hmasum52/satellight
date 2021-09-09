package github.hmasum18.satellight.view;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import github.hmasum18.satellight.R;
import github.hmasum18.satellight.dagger.component.AppComponent;
import github.hmasum18.satellight.service.model.SatelliteData;
import github.hmasum18.satellight.service.model.TrajectoryData;
import github.hmasum18.satellight.utils.tle.SatelliteJs;
import github.hmasum18.satellight.utils.tle.TleToGeo;
import github.hmasum18.satellight.viewModel.MainViewModel;
import github.hmasum18.satellight.view.fragment.GoogleMapFragment;

public class MainActivity extends AppCompatActivity {

    private final static int  PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101;
    public static final String TAG = "MainActivity:";
    public boolean isLocationPermissionGranted = false;
    public LatLng deviceLatLng = new LatLng(28.5728816,-80.6500725); //kennedy space center

    @Inject
    MainViewModel mainViewModel;

    @Inject
    SatelliteJs satelliteJs;
    
    //views datas
    public String activeSatCode = "ISS (ZARYA)"; //this is iss by default but will change when another one selected
    public Map<String, ArrayList<TrajectoryData>> allSatDatFromSSCMap = new HashMap<>();
    public Map<String, SatelliteData> allSatelliteData = new HashMap<>();
    public long lastRequestedTimestampBegin = 0;
    public long lastRequestedTimestampEnd = 0;

    //views
    private LinearLayout allViewsLinearLayout;
    public ProgressBar progressBar;
    public ConstraintLayout mainActvConstrainLayout;
    private NavController navController;
    private Integer activeFragmentId;
    public ImageButton googleMapIBTN,globeIBTN,modelView3DIBTN,vrViewIBTN;
    public Button mDetailsFragBtn;
    public TextView mSatNameTV,mSatLatTV,mSatLngTV,mSatHeightTV,mSatSpeedTV,mDateTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: ");
        
        injectDependencies();
        
        fetchSatelliteDataFromAppScript();

        mainActvConstrainLayout = findViewById(R.id.mainActv_ConstrainLayout);
       // mainActvConstrainLayout.setVisibility(View.GONE);
        progressBar = findViewById(R.id.wave_spinkit);

        navController = Navigation.findNavController(this,R.id.mainActv_nav_host_frag );
        googleMapIBTN = findViewById(R.id.mainAcvt_googleMapIBTN);
        globeIBTN = findViewById(R.id.mainAcvt_1stPersonViewIBTN);
        modelView3DIBTN = findViewById(R.id.mainAcvt_webViewIBTN);
        vrViewIBTN = findViewById(R.id.mainAcvt_vrViewIBTN);

        mDetailsFragBtn = findViewById(R.id.mainActv_detailsBtn);

        allViewsLinearLayout = findViewById(R.id.allViewsLinearLayout);
        mSatNameTV = findViewById(R.id.mainActv_satNameTV);
        mSatLatTV = findViewById(R.id.mainActv_latTV);
        mSatLngTV = findViewById(R.id.mainActv_lngTV);
        mSatHeightTV = findViewById(R.id.mainActv_heightTV);
        mSatSpeedTV = findViewById(R.id.mainActv_speedTV);
        mDateTV = findViewById(R.id.mainActv_dateTV);

        googleMapIBTN.setOnClickListener(v -> {
            if(activeFragmentId!=R.id.googleMapFragment){
                mDetailsFragBtn.setVisibility(View.VISIBLE);
                allViewsLinearLayout.setVisibility(View.VISIBLE);
                navController.navigate(R.id.googleMapFragment);
                setStartDestination(R.id.googleMapFragment);
            }
        });

        globeIBTN.setOnClickListener(v -> {
            if(activeFragmentId!=R.id.globeFragment){
                mDetailsFragBtn.setVisibility(View.VISIBLE);
                allViewsLinearLayout.setVisibility(View.VISIBLE);
                navController.navigate(R.id.globeFragment);
                setStartDestination(R.id.globeFragment);
            }
        });

        modelView3DIBTN.setOnClickListener(v -> {
            if(activeFragmentId!=R.id.modelViewFragment
            &&!activeSatCode.equals("moon")){
                mDetailsFragBtn.setVisibility(View.GONE);
                allViewsLinearLayout.setVisibility(View.GONE);
                navController.navigate(R.id.modelViewFragment);
                setStartDestination(R.id.modelViewFragment);
            }else if(activeSatCode.equals("moon")){
                Toast.makeText(getApplicationContext(),"Moon data not available",Toast.LENGTH_SHORT).show();
            }
        });

        vrViewIBTN.setOnClickListener(v -> {
            if(activeFragmentId!=R.id.vrViewFragment
            &&!activeSatCode.equals("moon")){
                mDetailsFragBtn.setVisibility(View.VISIBLE);
                allViewsLinearLayout.setVisibility(View.VISIBLE);
                navController.navigate(R.id.vrViewFragment);
                setStartDestination(R.id.vrViewFragment);
            }else if(activeSatCode.equals("moon")){
                Toast.makeText(getApplicationContext(),"Moon data not available",Toast.LENGTH_SHORT).show();
            }
        });

        mDetailsFragBtn.setOnClickListener(v -> {
            if(activeFragmentId!=R.id.detailsFragment
                    &&!activeSatCode.equals("moon")){
                mDetailsFragBtn.setVisibility(View.GONE);
                allViewsLinearLayout.setVisibility(View.GONE);
                navController.navigate(R.id.detailsFragment);
                setStartDestination(R.id.detailsFragment);
            }else if(activeSatCode.equals("moon")){
                Toast.makeText(getApplicationContext(),"Moon data not available",Toast.LENGTH_SHORT).show();
            }
        });

        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                activeFragmentId = controller.getCurrentDestination().getId();
                Log.w(TAG,"acitveFragmentId:"+activeFragmentId);
            }
        });
    }

    private void injectDependencies() {
        AppComponent appComponent = ((App)getApplication()).getAppComponent();
        appComponent.inject(this);
    }

    private void fetchSatelliteDataFromAppScript() {
        mainViewModel.getSatelliteDataList()
                .observe(this, satellites -> {
                    Log.d(TAG, "fetchSatelliteDataFromAppScript: size: "+satellites.size());
                });
    }

    public void updateUI(Map<String,Object> data){
        SatelliteData satelliteData ;
        if(!activeSatCode.equals("moon")) {
            satelliteData = allSatelliteData.get(activeSatCode);
            mSatNameTV.setText("Name:"+data.get("satName")+"\n"+"Country:"+satelliteData.getCountryName());
        }else {
            mSatNameTV.setText("Name:"+data.get("satName"));
        }

        mSatLatTV.setText("Lat:"+String.format("%.3f",data.get("lat") ) );
        mSatLngTV.setText("Lng:"+String.format("%.3f",data.get("lng")) );
        mSatHeightTV.setText("Height:"+String.format("%.2f",data.get("height"))+"km");
        mSatSpeedTV.setText("Velocity:"+String.format("%.2f",data.get("velocity"))+"km/h");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/mm/yyyy,hh:mm:ss aa");
        String date = simpleDateFormat.format(new Date( (long)data.get("timestamp") ) );

        mDateTV.setText("Date:"+ date);

    }

    public void setStartDestination(Integer fragmentId){
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.mainActv_nav_host_frag);
        NavInflater inflater = navHostFragment.getNavController().getNavInflater();
        NavGraph navGraph = inflater.inflate(R.navigation.main_nav_graph);
        navGraph.setStartDestination(fragmentId);
        navHostFragment.getNavController().setGraph(navGraph);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        isLocationPermissionGranted = false;
        Log.w(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isLocationPermissionGranted = true;
                    //get location and then fetch data
                    Log.w(TAG, " location permission granted ..requesting device location");
                    // getDeviceLocation();
                    Fragment navHosFragment = getSupportFragmentManager().findFragmentById(R.id.mainActv_nav_host_frag);
                    GoogleMapFragment fragment = (GoogleMapFragment) navHosFragment.getChildFragmentManager().getFragments().get(0);
                    fragment.getDeviceLocation();
                }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        calculateLLA();
      
    }

    public void calculateLLA() {
        // bangabandhu
        String line1 = "1 43463U 18044A   21251.91522778 -.00000369  00000+0  00000+0 0  9996";
        String line2 = "2 43463   0.0263 249.6537 0002469 282.7932 264.3275  1.00273201 12236";

        // iss
      /*  String line1 = "1 25544U 98067A   20259.60133280  .00000295  00000-0  13495-4 0  9993";
        String line2 = "2 25544  51.6439 261.0466 0000952 103.4889 359.3413 15.48949242246035";*/

        TleToGeo tleToGeo = new TleToGeo(line1, line2);

        double lat = 22.68726;
        double lng = 91.7853;

        TleToGeo.SatelliteTrajectory position = tleToGeo.getSatellitePosition(lat, lng);

        Log.d(TAG, "calculateLLA: "+position);

        try{
            JSONObject object = new JSONObject();
            object.put("line1", line1);
            object.put("line2",line2);
            object.put("lat", lat);
            object.put("lng", lng);
            object.put("time", System.currentTimeMillis());
           /* JSONObject object = new JSONObject();
            object.put("line1", "1 25544U 98067A   21252.18003037  .00001330  00000+0  32699-4 0  9993");
            object.put("line2","2 25544  51.6447 287.3970 0003352  12.1752  96.0942 15.48611890301610");
            object.put("lat", 22.68726);
            object.put("lng", 91.7853);
            object.put("time", System.currentTimeMillis());*/
            satelliteJs.calculateSateData(object);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}