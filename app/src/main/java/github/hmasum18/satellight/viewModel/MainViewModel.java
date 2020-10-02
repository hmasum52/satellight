package github.hmasum18.satellight.viewModel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import github.hmasum18.satellight.models.SatelliteBasicData;
import github.hmasum18.satellight.repositories.MainRepository;

public class MainViewModel extends ViewModel {

    public static final String TAG = "MainViewModel:";
    private MainRepository mainRepository;

    public MainViewModel() {
        mainRepository = new MainRepository();
    }

    public LiveData<Map<String,ArrayList<SatelliteBasicData>>> getLocationOfSatellite(ArrayList<String> satIdList, String fromTime, String toTime){
        return mainRepository.getLocationOfSatellites(satIdList,fromTime,toTime);
    }
}
