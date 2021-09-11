package github.hmasum18.satellight.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import github.hmasum18.satellight.service.model.Satellite;
import github.hmasum18.satellight.service.model.TrajectoryData;
import github.hmasum18.satellight.service.repository.MainRepo;
import github.hmasum18.satellight.service.repository.NasaSSCApiRepo;


@Singleton
public class MainViewModel extends ViewModel{
    public static final String TAG = "MainViewModel:";

    @Inject
    MainRepo mainRepo;

    @Inject
    NasaSSCApiRepo nasaSSCApiRepo;

    @Inject
    public MainViewModel() {

    }

    public LiveData<List<Satellite>> getSatelliteDataList(){
        return mainRepo.getSatelliteDataList();
    }

    public LiveData<Map<String,ArrayList<TrajectoryData>>> getLocationOfSatellite(ArrayList<String> satIdList, String fromTime, String toTime){
        return nasaSSCApiRepo.getLocationOfSatelliteFromSSC(satIdList,fromTime,toTime);
    }

}
