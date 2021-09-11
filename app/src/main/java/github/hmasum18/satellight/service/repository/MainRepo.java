package github.hmasum18.satellight.service.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import github.hmasum18.satellight.dagger.module.network.NetworkModule;
import github.hmasum18.satellight.service.api.ApiCaller;
import github.hmasum18.satellight.service.api.OnFinishListener;
import github.hmasum18.satellight.service.model.Satellite;
import github.hmasum18.satellight.service.room.SatelliteDao;

public class MainRepo {
    private static final String TAG = "MainRepo";

    @Inject
    ExecutorService roomExecutorService;

    @Inject
    SatelliteDao satelliteDao;

    @Inject
    NetworkModule.SatelliteDataSource satelliteDataSource;

    @Inject
    NetworkModule.TLEApi TLEApi;

    @Inject
    public MainRepo() {
    }

    public LiveData<List<Satellite>> getSatelliteDataList() {
        fetchSatelliteDate(); // overwrite the existing data when success.
        return satelliteDao.getAllSatelliteData();
    }

    public void fetchSatelliteDate() {
        Type type = new TypeToken<List<Satellite>>() {
        }.getType();
        ApiCaller<List<Satellite>> caller = new ApiCaller<>(type, satelliteDataSource.getRetrofit());

        caller.GETJson("data.json")
                .addOnFinishListener(new OnFinishListener<List<Satellite>>() {
                    @Override
                    public void onSuccess(List<Satellite> satellites) {
                        roomExecutorService.execute(() -> {
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
            ApiCaller<String> caller = new ApiCaller<>(String.class, TLEApi.getRetrofit());
            caller.GETString("" + sat.getId())
                    .addOnFinishListener(new OnFinishListener<String>() {
                        @Override
                        public void onSuccess(String s) {
                            try {
                                JSONObject tle = new JSONObject(s);
                                if(tle.has("line1")){
                                    Log.d(TAG, "onSuccess: tle found, sat: "+tle.optString("name"));
                                    sat.setTleLine1(tle.optString("line1"));
                                    sat.setTleLine2(tle.optString("line2"));
                                    roomExecutorService.execute(()->{
                                        satelliteDao.insert(sat);
                                    });
                                }else
                                    throw  new Exception();
                            } catch (Exception e) {
                                Log.d(TAG, "onSuccess: tle data not found, sat code: " + sat.getId());
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "onFailure: failed fetching tle for " + sat.getId(), e);
                        }
                    });
        }
    }

}
