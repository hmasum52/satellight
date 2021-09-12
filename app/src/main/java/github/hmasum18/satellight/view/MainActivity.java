package github.hmasum18.satellight.view;

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

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.neosensory.tlepredictionengine.Tle;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import github.hmasum18.satellight.R;
import github.hmasum18.satellight.dagger.component.ActivityComponent;
import github.hmasum18.satellight.dagger.component.AppComponent;
import github.hmasum18.satellight.dagger.module.ActivityModule;
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
    public static final String TAG = "MainActivity:";

    public ActivityComponent activityComponent;

    ActivityMainBinding mVB;

    @Inject
    SatelliteListAdapter satelliteListAdapter;

    @Inject
    MainViewModel mainViewModel;

    @Inject
    DeviceLocationFinder deviceLocationFinder;

    //views datas
    public String activeSatCode = "ISS (ZARYA)"; //this is iss by default but will change when another one selected
    public Map<String, ArrayList<TrajectoryData>> allSatDatFromSSCMap = new HashMap<>();
    public Map<String, SatelliteData> allSatelliteData = new HashMap<>();

    //views
    public ConstraintLayout mainActvConstrainLayout;
    private NavController navController;
    private Integer activeFragmentId;

    //data from nasa ssc api
    ArrayList<String> satCodeList = new ArrayList<>(Arrays.asList(
            "sun", "moon"
    ));



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);

        mVB = ActivityMainBinding.inflate(super.getLayoutInflater());
        setContentView(mVB.getRoot());

        
        if(activityComponent == null)
            initActivityComponent();
        activityComponent.inject(this);
        
        getDeviceLocation();

        fetchSatelliteDataFromAppScript();

        mVB.rvSatelliteList.setAdapter(satelliteListAdapter);
        satelliteListAdapter.setSearchView(mVB.searchSatellite);

        mainActvConstrainLayout = findViewById(R.id.mainActv_ConstrainLayout);
        navController = Navigation.findNavController(this, R.id.mainActv_nav_host_frag);

        initNavigationButtons();

        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                activeFragmentId = controller.getCurrentDestination().getId();
                Log.w(TAG, "acitveFragmentId:" + activeFragmentId);
            }
        });
    }

    public void initActivityComponent() {
        AppComponent appComponent = ((App) getApplication()).getAppComponent();
        activityComponent = appComponent.activityComponentBuilder()
                .activityModule(new ActivityModule(this))
                .build();
    }


    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: ");
        deviceLocationFinder.requestDeviceLocation(new DeviceLocationFinder.OnDeviceLocationFoundListener() {
            @Override
            public void onDeviceLocationFound(LatLng latLng) {
                Log.d(TAG, "onDeviceLocationFound: " + latLng);
            }
        });
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

    private void initNavigationButtons() {
        mVB.mainAcvtGoogleMapBtn.setOnClickListener(v -> {
            if (activeFragmentId != R.id.googleMapFragment) {
                mVB.mainActvDetailsBtn.setVisibility(View.VISIBLE);
                navController.navigate(R.id.googleMapFragment);
                setStartDestination(R.id.googleMapFragment);
            }
        });

        mVB.mainAcvt1stPersonViewBtn.setOnClickListener(v -> {
            if (activeFragmentId != R.id.globeFragment) {
                mVB.mainActvDetailsBtn.setVisibility(View.VISIBLE);
                navController.navigate(R.id.globeFragment);
                setStartDestination(R.id.globeFragment);
            }
        });

        mVB.mainAcvtWebViewIBTN.setOnClickListener(v -> {
            if (activeFragmentId != R.id.modelViewFragment
                    && !activeSatCode.equals("moon")) {
                mVB.mainActvDetailsBtn.setVisibility(View.GONE);
                navController.navigate(R.id.modelViewFragment);
                setStartDestination(R.id.modelViewFragment);
            } else if (activeSatCode.equals("moon")) {
                Toast.makeText(getApplicationContext(), "Moon data not available", Toast.LENGTH_SHORT).show();
            }
        });

        mVB.mainAcvtVrViewBtn.setOnClickListener(v -> {
            if (activeFragmentId != R.id.vrViewFragment
                    && !activeSatCode.equals("moon")) {
                mVB.mainActvDetailsBtn.setVisibility(View.VISIBLE);
                navController.navigate(R.id.vrViewFragment);
                setStartDestination(R.id.vrViewFragment);
            } else if (activeSatCode.equals("moon")) {
                Toast.makeText(getApplicationContext(), "Moon data not available", Toast.LENGTH_SHORT).show();
            }
        });

        mVB.mainActvDetailsBtn.setOnClickListener(v -> {
            if (activeFragmentId != R.id.detailsFragment
                    && !activeSatCode.equals("moon")) {
                mVB.mainActvDetailsBtn.setVisibility(View.GONE);
                navController.navigate(R.id.detailsFragment);
                setStartDestination(R.id.detailsFragment);
            } else if (activeSatCode.equals("moon")) {
                Toast.makeText(getApplicationContext(), "Moon data not available", Toast.LENGTH_SHORT).show();
            }
        });
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


}