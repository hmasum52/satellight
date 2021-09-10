package github.hmasum18.satellight.service.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import github.hmasum18.satellight.dagger.module.network.NetworkModule;
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
    NetworkModule.NasaSSCApi nasaSSCApi;

    @Inject
    NetworkModule.SatelliteDataSource satelliteDataSource;

    @Inject
    NetworkModule.CelestrakApi celestrakApi;

    @Inject
    public MainRepo(){
    }

    public LiveData<List<Satellite>> getSatelliteDataList(){
        fetchSatelliteDate(); // overwrite the existing data when success.
        return satelliteDao.getAllSatelliteData();
    }

    public void fetchSatelliteDate(){
        Type type = new TypeToken<List<Satellite>>(){}.getType();
        ApiCaller<List<Satellite>> caller = new ApiCaller<>(type, satelliteDataSource.getRetrofit());

        caller.GETJson("data.json")
                .addOnFinishListener(new OnFinishListener<List<Satellite>>() {
                    @Override
                    public void onSuccess(List<Satellite> satellites) {
                        roomExecutorService.execute(()->{
                            satelliteDao.insert(satellites);
                            fetchTLEData(satellites);
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "onFailure: failed fetching satellite data from app script backend");
                    }
                });
    }

    private void fetchTLEData(List<Satellite> satellites) {
        for (Satellite sat : satellites) {
            ApiCaller<String> caller = new ApiCaller<>(String.class, celestrakApi.getRetrofit());
            caller.GETString("gp.php?CATNR="+sat.getId()+"&FORMAT=TLE")
                    .addOnFinishListener(new OnFinishListener<String>() {
                        @Override
                        public void onSuccess(String s) {
                            Log.d(TAG, "onSuccess: id: "+sat.getId());
                            Log.d(TAG, "onSuccess: "+s);
                            String[] lines = s.split("\n");
                            if(lines.length==3){
                                Log.d(TAG, "onSuccess: name: "+lines[0]);
                                Log.d(TAG, "onSuccess: line1: "+lines[1]);
                                Log.d(TAG, "onSuccess: line2: "+lines[2]);
                                sat.setTleLine1(lines[1]);
                                sat.setTleLine2(lines[2]);
                                roomExecutorService.execute(()->{
                                    satelliteDao.insert(sat);
                                });
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "onFailure: failed fetching tle for "+sat.getId(), e);
                        }
                    });
        }
    }

}
