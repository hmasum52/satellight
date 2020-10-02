package github.hmasum18.satellight.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import github.hmasum18.satellight.R;
import github.hmasum18.satellight.views.GlobeFragment;
import github.hmasum18.satellight.views.GoogleMapFragment;
import github.hmasum18.satellight.views.MapsActivity;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layer.RenderableLayer;
import gov.nasa.worldwind.render.ImageSource;
import gov.nasa.worldwind.shape.Placemark;
import gov.nasa.worldwind.shape.PlacemarkAttributes;

public class GlobeUtils {
    public static Placemark addSatelliteToRenderableLayer(RenderableLayer renderableLayer, Position position, int satIconId){
        Placemark placemark = Placemark.createWithImage(position, ImageSource.fromResource(satIconId));

        PlacemarkAttributes placemarkAttributes =  placemark.getAttributes();

        placemarkAttributes.setImageScale(0.2f);
        renderableLayer.addRenderable(placemark);

        return placemark;
    }

    public static void addSatelliteToChipGroup(Fragment fragment, ChipGroup chipGroup, String name, int iconDrawableId){

        Chip chip = new Chip(fragment.getContext());
        chip.setChipIcon(fragment.getActivity().getDrawable(iconDrawableId) );
        chip.setChipIconSize(120f);
        chip.setHeight(180);
        chip.setIconStartPadding(20f);
        chip.setText(name);
        chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E5E5")));
        chip.setOnClickListener(v -> {
            String code =  Utils.sscSatCodeMap.get(name); //get the ssc sat code
            if(fragment instanceof  GoogleMapFragment){
                GoogleMapFragment temp = (GoogleMapFragment) fragment;
                temp.activeSatDataList = temp.allSatDatFromSSCMap.get(code);
                MapsActivity mapsActivity = (MapsActivity) fragment.getActivity();
                temp.prevSatCode = mapsActivity.activeSatCode;
                mapsActivity.activeSatCode = code;
                temp.initSatPosition();
            }else{ //instance of globe fragment
                GlobeFragment temp = (GlobeFragment) fragment;
                temp.activeSatDataList = temp.allSatDatFromSSCMap.get(code);
                MapsActivity mapsActivity = (MapsActivity) fragment.getActivity();
                temp.prevSatCode = mapsActivity.activeSatCode;
                mapsActivity.activeSatCode = code;
                temp.initSatPosition();
            }

        });
        chipGroup.addView(chip);
    }
}
