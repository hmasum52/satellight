package github.hmasum18.satellight.service.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import github.hmasum18.satellight.dagger.module.NetworkModule;
import github.hmasum18.satellight.service.api.ApiCaller;
import github.hmasum18.satellight.service.api.OnFinishListener;
import github.hmasum18.satellight.service.model.Satellite;
import github.hmasum18.satellight.service.room.SatelliteDao;

public class MainRepo{
    private static final String TAG = "MainRepo";

    @Inject
    ExecutorService roomExecutorService;

    @Inject
    SatelliteDao satelliteDao;

    @Inject
    NetworkModule.SatelliteDataSource backend;

    @Inject
    public MainRepo(){
    }

    public LiveData<List<Satellite>> getSatelliteDataList(){
        fetchSatelliteDate(); // overwrite the existing data when success.
        return satelliteDao.getAllSatelliteData();
    }

    public void fetchSatelliteDate(){
        Type type = new TypeToken<List<Satellite>>(){}.getType();
        ApiCaller<List<Satellite>> caller = new ApiCaller<>(type,backend.getRetrofit());

        caller.GETJson("data.json")
                .addOnFinishListener(new OnFinishListener<List<Satellite>>() {
                    @Override
                    public void onSuccess(List<Satellite> satellites) {
                        roomExecutorService.execute(()->{
                            satelliteDao.insert(satellites);
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "onFailure: failed fetching satellite data from app script backend");
                    }
                });
    }

}
