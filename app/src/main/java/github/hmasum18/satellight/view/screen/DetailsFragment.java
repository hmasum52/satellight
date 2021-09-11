package github.hmasum18.satellight.view.screen;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import github.hmasum18.satellight.R;
import github.hmasum18.satellight.service.model.SatelliteData;
import github.hmasum18.satellight.view.MainActivity;


public class DetailsFragment extends Fragment {

    MainActivity mainActivity;
    //data
    SatelliteData satelliteData;

    //views
    private ImageView mCountryFlag;
    private TextView mBasicInfo, mDescription, mUseCases;
    private LinearLayout gallery ;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
        mainActivity = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //data
        if(!mainActivity.activeSatCode.equals("moon"))
        satelliteData = mainActivity.allSatelliteData.get(mainActivity.activeSatCode);

        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_details, container, false);

        mCountryFlag = rootView.findViewById(R.id.detailsFrag_countryFlagIV);
        mBasicInfo = rootView.findViewById(R.id.detailsFrag_satBasicInfo);
        mDescription = rootView.findViewById(R.id.detailsFrag_descriptionId);
        mUseCases = rootView.findViewById(R.id.detailsFrag_useCasesId);

        gallery = rootView.findViewById(R.id.detailsFrag_imageGallery);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Glide.with(this)
                .load(satelliteData.getCountryFlagLink())
                .into(mCountryFlag);

        StringBuilder basicInfo = new StringBuilder();
        basicInfo.append(satelliteData.getFullName()).append("\n\n");
        basicInfo.append("Country: ").append(satelliteData.getCountryName()).append("\n");
        basicInfo.append("Type: ").append(satelliteData.getType()).append("\n\n");
        basicInfo.append("Launch Date: ").append(satelliteData.getLaunchDate()).append("\n");;
        basicInfo.append("Launch Mass: ").append(satelliteData.getLaunchMass()).append("\n");
        basicInfo.append("Mission Duration: ").append(satelliteData.getMissionDuration()).append("\n");;


        mBasicInfo.setText(basicInfo.toString());
        mDescription.setText(satelliteData.getDescription());

        StringBuilder useCases = new StringBuilder();
        for (String use : satelliteData.getUseCases()) {
            useCases.append("  - ").append(use).append("\n");
        }
        mUseCases.setText(useCases.toString());


        //LayoutInflater inflater = LayoutInflater.from(this.getContext());
        for (int i = 0; i < satelliteData.getRealImages().size(); i++) {
         //  View view1 = inflater.inflate(R.layout.image_item,gallery,false);
           ImageView imageView = new ImageView(this.getContext());
           Glide.with(this)
                   .load(satelliteData.getRealImages().get(i))
                   .into(imageView);

           gallery.addView(imageView);
        }

    }
}