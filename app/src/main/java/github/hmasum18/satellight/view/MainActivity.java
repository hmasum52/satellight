package github.hmasum18.satellight.view;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.neosensory.tlepredictionengine.Tle;

import org.jetbrains.annotations.NotNull;

import java.security.Permission;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import github.hmasum18.satellight.R;
import github.hmasum18.satellight.dagger.component.AppComponent;
import github.hmasum18.satellight.databinding.ActivityMainBinding;
import github.hmasum18.satellight.service.model.SatelliteData;
import github.hmasum18.satellight.service.model.SatelliteTrajectory;
import github.hmasum18.satellight.service.model.TrajectoryData;
import github.hmasum18.satellight.utils.Utils;
import github.hmasum18.satellight.utils.tle.TleToGeo;
import github.hmasum18.satellight.view.adapter.SatelliteListAdapter;
import github.hmasum18.satellight.view.screen.googlemap.DeviceLocationFinder;
import github.hmasum18.satellight.viewModel.MainViewModel;

public class MainActivity extends AppCompatActivity {

    private final static int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101;
    public static final String TAG = "MainActivity:";

    ActivityMainBinding mVB;

    @Inject
    SatelliteListAdapter satelliteListAdapter;

    @Inject
    MainViewModel mainViewModel;

    DeviceLocationFinder deviceLocationFinder;

    //views datas
    public String activeSatCode = "ISS (ZARYA)"; //this is iss by default but will change when another one selected
    public Map<String, ArrayList<TrajectoryData>> allSatDatFromSSCMap = new HashMap<>();
    public Map<String, SatelliteData> allSatelliteData = new HashMap<>();

    //views
    public ConstraintLayout mainActvConstrainLayout;
    private NavController navController;
    private Integer activeFragmentId;
    public ImageButton googleMapIBTN, globeIBTN, modelView3DIBTN, vrViewIBTN;
    public Button mDetailsFragBtn;

    //data from nasa ssc api
    ArrayList<String> satCodeList = new ArrayList<>(Arrays.asList(
            "sun", "moon"
    ));



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVB = ActivityMainBinding.inflate(super.getLayoutInflater());
        setContentView(mVB.getRoot());

        Log.d(TAG, "onCreate: ");

        injectDependencies();

        getDeviceLocation();

        fetchSatelliteDataFromAppScript();

        mVB.rvSatelliteList.setAdapter(satelliteListAdapter);
        satelliteListAdapter.setSearchView(mVB.searchSatellite);

        mainActvConstrainLayout = findViewById(R.id.mainActv_ConstrainLayout);


        navController = Navigation.findNavController(this, R.id.mainActv_nav_host_frag);
        googleMapIBTN = findViewById(R.id.mainAcvt_google_map_btn);
        globeIBTN = findViewById(R.id.mainAcvt_1stPersonView_btn);
        modelView3DIBTN = findViewById(R.id.mainAcvt_webViewIBTN);
        vrViewIBTN = findViewById(R.id.mainAcvt_vrView_btn);

        mDetailsFragBtn = findViewById(R.id.mainActv_detailsBtn);


        googleMapIBTN.setOnClickListener(v -> {
            if (activeFragmentId != R.id.googleMapFragment) {
                mDetailsFragBtn.setVisibility(View.VISIBLE);
                navController.navigate(R.id.googleMapFragment);
                setStartDestination(R.id.googleMapFragment);
            }
        });

        globeIBTN.setOnClickListener(v -> {
            if (activeFragmentId != R.id.globeFragment) {
                mDetailsFragBtn.setVisibility(View.VISIBLE);
                navController.navigate(R.id.globeFragment);
                setStartDestination(R.id.globeFragment);
            }
        });

        modelView3DIBTN.setOnClickListener(v -> {
            if (activeFragmentId != R.id.modelViewFragment
                    && !activeSatCode.equals("moon")) {
                mDetailsFragBtn.setVisibility(View.GONE);
                navController.navigate(R.id.modelViewFragment);
                setStartDestination(R.id.modelViewFragment);
            } else if (activeSatCode.equals("moon")) {
                Toast.makeText(getApplicationContext(), "Moon data not available", Toast.LENGTH_SHORT).show();
            }
        });

        vrViewIBTN.setOnClickListener(v -> {
            if (activeFragmentId != R.id.vrViewFragment
                    && !activeSatCode.equals("moon")) {
                mDetailsFragBtn.setVisibility(View.VISIBLE);
                navController.navigate(R.id.vrViewFragment);
                setStartDestination(R.id.vrViewFragment);
            } else if (activeSatCode.equals("moon")) {
                Toast.makeText(getApplicationContext(), "Moon data not available", Toast.LENGTH_SHORT).show();
            }
        });

        mDetailsFragBtn.setOnClickListener(v -> {
            if (activeFragmentId != R.id.detailsFragment
                    && !activeSatCode.equals("moon")) {
                mDetailsFragBtn.setVisibility(View.GONE);
                navController.navigate(R.id.detailsFragment);
                setStartDestination(R.id.detailsFragment);
            } else if (activeSatCode.equals("moon")) {
                Toast.makeText(getApplicationContext(), "Moon data not available", Toast.LENGTH_SHORT).show();
            }
        });

        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                activeFragmentId = controller.getCurrentDestination().getId();
                Log.w(TAG, "acitveFragmentId:" + activeFragmentId);
            }
        });
    }

    private void getDeviceLocation() {
        if(deviceLocationFinder == null)
            deviceLocationFinder = new DeviceLocationFinder( this);
        deviceLocationFinder.requestDeviceLocation(new DeviceLocationFinder.OnDeviceLocationFoundListener() {
            @Override
            public void onDeviceLocationFound(LatLng latLng) {
                Log.d(TAG, "onDeviceLocationFound: " + latLng);
            }
        });
    }

    public void setDeviceLocationFinder(DeviceLocationFinder deviceLocationFinder) {
        this.deviceLocationFinder = deviceLocationFinder;
    }

    public DeviceLocationFinder getDeviceLocationFinder() {
        return deviceLocationFinder;
    }

    private void injectDependencies() {
        AppComponent appComponent = ((App) getApplication()).getAppComponent();
        appComponent.inject(this);
    }

    private void fetchSatelliteDataFromAppScript() {

        fetchSatDataFromSSC(satCodeList);

        mainViewModel.getSatelliteDataList()
                .observe(this, satellites -> {
                    Log.d(TAG, "fetchSatelliteDataFromAppScript: size: " + satellites.size());
                    satelliteListAdapter.setSatelliteList(satellites);
                });
    }

    /**
     * name of the function suggest what it does
     * @param satCodeList
     */
    public void fetchSatDataFromSSC(ArrayList<String> satCodeList) {
        //fetch satellite data from nasa SSC
        long t = System.currentTimeMillis();
        String fromTime = Utils.getTimeAsString(t - (1000 * 60 * 5)); //before 5 min of current timestamp
        String toTime = Utils.getTimeAsString(t + 1000 * 60 * 20); //after 20 min of current timestamp
        Log.d(TAG, "SSC API: "+fromTime + " && " + toTime);
        mainViewModel.getLocationOfSatellite(satCodeList, fromTime, toTime)
                .observe(this, satelliteBasicDataMap -> {
            Log.d(TAG, "number of sat data in the map:" + satelliteBasicDataMap.size());
        });
    }

    public void updateUI(Map<String, Object> data) {
        SatelliteData satelliteData;
        if (!activeSatCode.equals("moon")) {
            satelliteData = allSatelliteData.get(activeSatCode);
        } else {
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/mm/yyyy,hh:mm:ss aa");
        String date = simpleDateFormat.format(new Date((long) data.get("timestamp")));


    }

    public void setStartDestination(Integer fragmentId) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.mainActv_nav_host_frag);
        NavInflater inflater = navHostFragment.getNavController().getNavInflater();
        NavGraph navGraph = inflater.inflate(R.navigation.main_nav_graph);
        navGraph.setStartDestination(fragmentId);
        navHostFragment.getNavController().setGraph(navGraph);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        //calculateLLA();
    }

    public void calculateLLA() {
       /* // bangabandhu
        String line1 = "1 43463U 18044A   21251.91522778 -.00000369  00000+0  00000+0 0  9996";
        String line2 = "2 43463   0.0263 249.6537 0002469 282.7932 264.3275  1.00273201 12236";*/

        // iss
        String line1 = "1 25544U 98067A   21252.50949163  .00001550  00000+0  36756-4 0  9990";
        String line2 = "2 25544  51.6444 285.7692 0003404  13.6408 132.5925 15.48614249301665";

        Tle tle = new Tle(line1, line2);

        double lat = 22.68726;
        double lng = 91.7853;

        SatelliteTrajectory position = TleToGeo.getSatellitePosition(tle, new LatLng(lat, lng));

        Log.d(TAG, "calculateLLA: " + position);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "onRequestPermissionsResult: code: "+requestCode);
        Log.d(TAG, "onRequestPermissionsResult: length: "+permissions.length);
        for (String s :
                permissions) {
            Log.d(TAG, "onRequestPermissionsResult: "+s);
            if(s.equals("android.permission.ACCESS_FINE_LOCATION")){
                if (grantResults.length > 0
                        && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                }
            }
        }
    }
}