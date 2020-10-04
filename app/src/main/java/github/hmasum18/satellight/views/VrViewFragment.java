package github.hmasum18.satellight.views;

import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.vr.sdk.widgets.pano.VrPanoramaView;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import github.hmasum18.satellight.R;
import github.hmasum18.satellight.models.SatelliteData;
import github.hmasum18.satellight.models.TrajectoryData;
import github.hmasum18.satellight.utils.GlobeUtils;
import github.hmasum18.satellight.views.vr.Rotation;
import github.hmasum18.satellight.views.vr.Satellite;

import static com.google.vr.cardboard.ThreadUtils.runOnUiThread;


public class VrViewFragment extends Fragment {


    private static final String TAG = "VrViewFragment";
    private MapsActivity mapsActivity;

    private VrPanoramaView mVRPanoramaView;
    private ImageView satelliteView;
    private TextView directionText;

    private int index = 2;

    private Satellite satellite = new Satellite(0, 26.058495f);
    public List<TrajectoryData> activeSatDataList = new ArrayList<>(); //store the currently active satellite

    private String[] images = {
            "25654595177_7f4f9ad12a_k.jpg","31903386543_6e3a302f12_k.jpg","33817337141_d81d7acae9_k.jpg","34770759254_db8e47f6c8_k.jpg","50113651081_9135acc2bd_o.jpg","50131496382_234fea5e45_k.jpg","50132874773_c0c0096a18_o.jpg","50144016137_d269a5390b_o.jpg","50225566643_699dfb4155_o.jpg","50231114598_a57528d65d_o.jpg","50259899302_6394fb84d7_o.jpg"
    };

    private Rotation rotation;

    //view
    Button mArduinoConnectBtn;

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
        View rootView = inflater.inflate(R.layout.fragment_vr_view, container, false);

        mVRPanoramaView = rootView.findViewById(R.id.vrPanoramaView);
        satelliteView = rootView.findViewById(R.id.satellight);
        mArduinoConnectBtn = rootView.findViewById(R.id.vrViewFrag_arduinoConnectBtn);
        directionText = rootView.findViewById(R.id.direction_text);

        satelliteView.setImageResource(R.drawable.satellite_mono);

        fetchInitialData();
        loadPhotoSphere();
        setupRotation();

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mArduinoConnectBtn.setOnClickListener(v -> {
            Toast.makeText(getContext(),"Connecting with Arduino",Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(),"Connected Successfully. Determining the location of "+mapsActivity.activeSatCode+" in 3d space.",Toast.LENGTH_LONG).show();
                }
            },1000);
        });
    }

    /**
     * fetch all the necessary data
     */
    public void fetchInitialData(){
        activeSatDataList = mapsActivity.allSatDatFromSSCMap.get(mapsActivity.activeSatCode);
        if(activeSatDataList == null)
            activeSatDataList = mapsActivity.allSatelliteData.get(mapsActivity.activeSatCode).getTrajectoryDataList();
        initSatPosition();
    }

    public void initSatPosition(){
        Log.w(TAG,"active satDataSize:"+ activeSatDataList.size());

        long timeIntervalBetweenTwoData = (activeSatDataList.get(1).getTimestamp()-activeSatDataList.get(0).getTimestamp())/1000; //in sec

        long currentTimestamp = System.currentTimeMillis();
        long duration = (currentTimestamp - activeSatDataList.get(0).getTimestamp())/1000;
        Log.w(TAG,"fetchData: Duration:"+duration);
        double percent = (double)duration/timeIntervalBetweenTwoData;

        int currentIdx = (int)percent;
        currentIdx = Math.max(currentIdx, 0);

        Log.w(TAG," iniitSat: currentIdx:"+currentIdx);
        percent -= currentIdx;

        TrajectoryData startData =  activeSatDataList.get(currentIdx);
        TrajectoryData secondData = activeSatDataList.get(1+currentIdx);

        double azimuth = startData.getAzimuth()*(1-percent) + percent*secondData.getAzimuth();
        double elevation = (1-percent)*startData.getElevation() + percent*secondData.getElevation();

        Log.w(TAG," azimuth:"+azimuth+" elevation: "+elevation);
        satellite = new Satellite((float)azimuth-90,(float)elevation);

        //for debug
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/mm/yyyy,hh:mm:ss aa");
                String date = simpleDateFormat.format(new Date( System.currentTimeMillis() ) );
                Log.w(TAG,""+mapsActivity.mDateTV.getText());
                mapsActivity.mDateTV.setText("Date:"+ date+"\n"+String.format("azimuth: %.2f Elevation: %.2f"
                        ,satellite.getHorizontalAngle()+90,satellite.getVerticalAngle()));
            }
        },1000);
    }


    @Override
    public void onStart() {
        super.onStart();
        rotation.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        rotation.stop();

        mVRPanoramaView.pauseRendering();
    }

    @Override
    public void onResume() {
        super.onResume();
        rotation.start();
        mVRPanoramaView.resumeRendering();
    }

    @Override
    public void onDestroy() {
        mVRPanoramaView.shutdown();
        rotation.stop();
        super.onDestroy();
    }

    private void loadPhotoSphere() {
        VrPanoramaView.Options options = new VrPanoramaView.Options();
        InputStream inputStream = null;

        AssetManager assetManager = getActivity().getAssets();
        try {
            inputStream = assetManager.open(images[index%images.length]);
            options.inputType = VrPanoramaView.Options.TYPE_MONO;
            mVRPanoramaView.loadImageFromBitmap(BitmapFactory.decodeStream(inputStream), options);
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setupRotation() {
        rotation = new Rotation(this.getContext());
        Rotation.RotationListener cl = getRotationListener();
        rotation.setListener(cl);
    }

    private Rotation.RotationListener getRotationListener() {
        return (azimuth, vAngle) -> runOnUiThread(() -> {
            float m_azimuth = azimuth + 90;
            final float DEVIATION = 10;

            if ((m_azimuth>=360-DEVIATION && m_azimuth<=360) || (m_azimuth>=0 && m_azimuth<=DEVIATION))
                directionText.setText("N");
            else if (m_azimuth>=90-DEVIATION && m_azimuth<=90+DEVIATION)
                directionText.setText("E");
            else if (m_azimuth>=180-DEVIATION && m_azimuth<=180+DEVIATION)
                directionText.setText("S");
            else if (m_azimuth>=270-DEVIATION && m_azimuth<=270+DEVIATION)
                directionText.setText("W");
            else
                directionText.setText("");

            Pair<Float, Float> pair = satellite.getPosition(azimuth, vAngle);
            if (pair != null) {
                satelliteView.setVisibility(View.VISIBLE);

                float HBias = 0.5f + pair.first;
                float VBias = 0.5f - pair.second;

               //Log.d(TAG, "getRotationListener: _ " + HBias + " | " + VBias);
               // Log.d(TAG, "getRotationListener: " + satelliteView.getDrawable());

                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) satelliteView.getLayoutParams();
                params.horizontalBias = HBias;
                params.verticalBias = VBias;
                satelliteView.setLayoutParams(params);

            } else {
                satelliteView.setVisibility(View.GONE);
            }
        });
    }
}