package github.hmasum18.satellight.views;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import github.hmasum18.satellight.R;
import github.hmasum18.satellight.models.SatelliteData;
import github.hmasum18.satellight.models.TrajectoryData;
import github.hmasum18.satellight.viewModel.MainViewModel;

public class MapsActivity extends AppCompatActivity {

    private final static int  PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101;
    public static final String TAG = "MapsActivity:";
    public boolean isLocationPermissionGranted = false;
    public LatLng deviceLatLng;

    //data sources
    private MainViewModel mainViewModel;
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
        setContentView(R.layout.activity_maps);

        mainActvConstrainLayout = findViewById(R.id.mainActv_ConstrainLayout);
        mainActvConstrainLayout.setVisibility(View.GONE);
        progressBar = findViewById(R.id.wave_spinkit);

        navController = Navigation.findNavController(this,R.id.mainActv_nav_host_frag );
        googleMapIBTN = findViewById(R.id.mainAcvt_googleMapIBTN);
        globeIBTN = findViewById(R.id.mainAcvt_1stPersonViewIBTN);
        modelView3DIBTN = findViewById(R.id.mainAcvt_3dGlobeIBTN);
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
        isLocationPermissionGranted = false;
        Log.w(TAG,"onRequestPermissionsResult");
        switch (requestCode){
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                if(grantResults.length>0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    isLocationPermissionGranted = true;
                    //get location and then fetch data
                    Log.w(TAG," location permission granted ..requesting device location");
                   // getDeviceLocation();
                    Fragment navHosFragment = getSupportFragmentManager().findFragmentById(R.id.mainActv_nav_host_frag);
                    GoogleMapFragment fragment = (GoogleMapFragment) navHosFragment.getChildFragmentManager().getFragments().get(0);
                    fragment.getDeviceLocation();
                }
        }
    }


}