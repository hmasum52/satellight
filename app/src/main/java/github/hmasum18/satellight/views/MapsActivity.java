package github.hmasum18.satellight.views;

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

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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

    //data sources
    private MainViewModel mainViewModel;

    public String activeSatCode = "ISS (ZARYA)"; //this is iss by default but will change when another one selected
    private LinearLayout allViewsLinearLayout;
    public ProgressBar progressBar;
    public ConstraintLayout mainActvConstrainLayout;


    public Map<String, ArrayList<TrajectoryData>> allSatDatFromSSCMap = new HashMap<>();
    public Map<String, SatelliteData> allSatelliteData = new HashMap<>();

    //views
    private NavController navController;
    private Integer activeFragmentId;
    public ImageButton googleMapIBTN,globeIBTN,modelView3DIBTN,vrViewIBTN;
    private TextView mSatNameTV,mSatLatTV,mSatLngTV,mSatHeightTV,mSatSpeedTV,mDateTV;

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

        allViewsLinearLayout = findViewById(R.id.allViewsLinearLayout);
        mSatNameTV = findViewById(R.id.mainActv_satNameTV);
        mSatLatTV = findViewById(R.id.mainActv_latTV);
        mSatLngTV = findViewById(R.id.mainActv_lngTV);
        mSatHeightTV = findViewById(R.id.mainActv_heightTV);
        mSatSpeedTV = findViewById(R.id.mainActv_speedTV);
        mDateTV = findViewById(R.id.mainActv_dateTV);

        googleMapIBTN.setOnClickListener(v -> {
            if(activeFragmentId!=R.id.googleMapFragment){
                allViewsLinearLayout.setVisibility(View.VISIBLE);
                navController.navigate(R.id.googleMapFragment);
                setStartDestination(R.id.googleMapFragment);
            }
        });

        globeIBTN.setOnClickListener(v -> {
            if(activeFragmentId!=R.id.globeFragment){
                allViewsLinearLayout.setVisibility(View.VISIBLE);
                navController.navigate(R.id.globeFragment);
                setStartDestination(R.id.globeFragment);
            }
        });

        modelView3DIBTN.setOnClickListener(v -> {
            if(activeFragmentId!=R.id.modelViewFragment){
                allViewsLinearLayout.setVisibility(View.GONE);
                navController.navigate(R.id.modelViewFragment);
                setStartDestination(R.id.modelViewFragment);
            }
        });

        vrViewIBTN.setOnClickListener(v -> {
            if(activeFragmentId!=R.id.vrViewFragment){
                allViewsLinearLayout.setVisibility(View.GONE);
                navController.navigate(R.id.vrViewFragment);
                setStartDestination(R.id.vrViewFragment);
            }
        });

        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                activeFragmentId = controller.getCurrentDestination().getId();
            }
        });
    }

    public void updateUI(Map<String,Object> data){
        mSatNameTV.setText("Name:"+data.get("satName"));
        mSatLatTV.setText("Lat:"+String.format("%.3f",data.get("lat") ) );
        mSatLngTV.setText("Lng:"+String.format("%.3f",data.get("lng")) );
        mSatHeightTV.setText("Height:"+String.format("%.2f",data.get("height"))+"km");

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
}