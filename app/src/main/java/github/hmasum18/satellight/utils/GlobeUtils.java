package github.hmasum18.satellight.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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

    public static final String TAG = "GlobeUtils";
    public static Map<String,Drawable> satelliteIconMap = new HashMap<>();

    public static Placemark addSatelliteToRenderableLayer(RenderableLayer renderableLayer, Position position, int satIconId){
        Placemark placemark = Placemark.createWithImage(position, ImageSource.fromResource(satIconId));

        PlacemarkAttributes placemarkAttributes =  placemark.getAttributes();

        placemarkAttributes.setImageScale(0.2f);
        renderableLayer.addRenderable(placemark);

        return placemark;
    }

/*    public static void addSatelliteToChipGroup(Fragment fragment, ChipGroup chipGroup, String name, String iconDrawableUrl){

        new AsyncTask<String, Integer, Drawable>(){

            @Override
            protected Drawable doInBackground(String... strings) {
                Bitmap bmp = null;
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(iconDrawableUrl).openConnection();
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    bmp = BitmapFactory.decodeStream(input);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Drawable drawable = new BitmapDrawable(Resources.getSystem(), bmp);;
                satelliteIconMap.put(name,drawable);

                return drawable;
            }

            protected void onPostExecute(Drawable result) {
                Chip chip = new Chip(fragment.getContext());
                chip.setChipIcon(result);
                chip.setChipIconSize(80);
                chip.setHeight(130);
                chip.setIconStartPadding(20f);
                chip.setText(name);
                chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E5E5")));
                chip.setOnClickListener(v -> {
                    String code =  ((Chip)v).getText().toString();
                    MapsActivity mapsActivity = (MapsActivity) fragment.getActivity();
                    Log.w(TAG," selected Sat: "+code);
                    if(fragment instanceof  GoogleMapFragment){
                        GoogleMapFragment temp = (GoogleMapFragment) fragment;
                        temp.activeSatDataList = mapsActivity.allSatDatFromSSCMap.get(code);
                        if(temp.activeSatDataList == null)
                            temp.activeSatDataList = mapsActivity.allSatelliteData.get(code).getTrajectoryDataList();
                        temp.prevSatCode = mapsActivity.activeSatCode;
                        mapsActivity.activeSatCode = code;
                        temp.initSatPosition();
                    }else{ //instance of globe fragment
                        GlobeFragment temp = (GlobeFragment) fragment;
                        temp.activeSatDataList = mapsActivity.allSatDatFromSSCMap.get(code);
                        if(temp.activeSatDataList == null)
                            temp.activeSatDataList = mapsActivity.allSatelliteData.get(code).getTrajectoryDataList();
                        temp.prevSatCode = mapsActivity.activeSatCode;
                        mapsActivity.activeSatCode = code;
                        temp.initSatPosition();
                    }

                });
                chipGroup.addView(chip);

            }

        }.execute();


    }*/

    public static void addSatelliteToChipGroup(Fragment fragment, ChipGroup chipGroup, String name, String iconDrawableUrl){

        Glide.with(fragment)
                .load(iconDrawableUrl)
                .centerCrop()
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        resource.setBounds(0,0,50,50);
                        satelliteIconMap.put(name,resource);
                        addChip(fragment,chipGroup,name,resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });

    }

    public static void addSatelliteToChipGroup(Fragment fragment, ChipGroup chipGroup, String name, int iconDrawableId){
        Drawable drawable = fragment.getActivity().getDrawable(iconDrawableId);
        satelliteIconMap.put(name,drawable);

        addChip(fragment,chipGroup,name,drawable);
    }

    public static void addSatelliteToChipGroup(Fragment fragment, ChipGroup chipGroup, String name){
        addChip(fragment,chipGroup,name,satelliteIconMap.get(name));
    }

    public static  void addChip(Fragment fragment, ChipGroup chipGroup, String name,Drawable drawable){
        Chip chip = new Chip(fragment.getContext());
        chip.setChipIcon(drawable);
        chip.setChipIconSize(80);
        chip.setHeight(130);
        chip.setIconStartPadding(20f);
        chip.setText(name);
        chip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5E5E5")));
        chip.setOnClickListener(v -> {
            String code =  ((Chip)v).getText().toString();
            MapsActivity mapsActivity = (MapsActivity) fragment.getActivity();
            Log.w(TAG," selected Sat: "+code);
            if(fragment instanceof  GoogleMapFragment){
                GoogleMapFragment temp = (GoogleMapFragment) fragment;
                temp.activeSatDataList = mapsActivity.allSatDatFromSSCMap.get(code);
                if(temp.activeSatDataList == null)
                    temp.activeSatDataList = mapsActivity.allSatelliteData.get(code).getTrajectoryDataList();
                temp.prevSatCode = mapsActivity.activeSatCode;
                mapsActivity.activeSatCode = code;
                temp.initSatPosition();
            }else{ //instance of globe fragment
                GlobeFragment temp = (GlobeFragment) fragment;
                temp.activeSatDataList = mapsActivity.allSatDatFromSSCMap.get(code);
                if(temp.activeSatDataList == null)
                    temp.activeSatDataList = mapsActivity.allSatelliteData.get(code).getTrajectoryDataList();
                temp.prevSatCode = mapsActivity.activeSatCode;
                mapsActivity.activeSatCode = code;
                temp.initSatPosition();
            }

        });
        chipGroup.addView(chip);
    }

}
