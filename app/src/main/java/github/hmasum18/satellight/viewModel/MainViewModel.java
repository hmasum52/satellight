package github.hmasum18.satellight.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Map;

import github.hmasum18.satellight.models.SatelliteBasicData;
import github.hmasum18.satellight.models.SatelliteData;
import github.hmasum18.satellight.models.TrajectoryData;
import github.hmasum18.satellight.repositories.MainRepository;

public class MainViewModel extends ViewModel {

    public static final String TAG = "MainViewModel:";
    private MainRepository mainRepository;

    public MainViewModel() {
        mainRepository = new MainRepository();
    }

    public LiveData<Map<String,ArrayList<TrajectoryData>>> getLocationOfSatellite(ArrayList<String> satIdList, String fromTime, String toTime){
        return mainRepository.getLocationOfSatelliteFromSSC(satIdList,fromTime,toTime);
    }

    public LiveData<Map<String, SatelliteData>> getAllSatelliteData(long timestampBegin, long timestampEnd, LatLng userLocation){
       return mainRepository.getAllSatelliteData(timestampBegin,timestampEnd,userLocation);
    }
}
