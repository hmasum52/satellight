package github.hmasum18.satellight.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import github.hmasum18.satellight.service.model.Satellite;
import github.hmasum18.satellight.service.model.SatelliteData;
import github.hmasum18.satellight.service.model.TrajectoryData;
import github.hmasum18.satellight.service.repository.MainRepo;


@Singleton
public class MainViewModel extends ViewModel {
    public static final String TAG = "MainViewModel:";

    @Inject
    MainRepo mainRepo;

    @Inject
    public MainViewModel() {

    }

    public LiveData<List<Satellite>> getSatelliteDataList(){
        return mainRepo.getSatelliteDataList();
    }

}
