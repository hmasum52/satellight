package github.hmasum18.satellight.service.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;

import github.hmasum18.satellight.dagger.module.network.NetworkModule;
import github.hmasum18.satellight.service.api.ApiCaller;
import github.hmasum18.satellight.service.api.OnFinishListener;
import github.hmasum18.satellight.service.model.TrajectoryData;
import github.hmasum18.satellight.utils.tle.TleToGeo;

public class NasaSSCApiRepo {
    private static final String TAG = "NasaSSCApiRepo";

    @Inject
    NetworkModule.NasaSSCApi nasaSSCApi;

    @Inject
    public NasaSSCApiRepo() {
    }

    //store basic data of a satellite
    private final Map<String,ArrayList<TrajectoryData>> satMap = new HashMap<>();
    //shortName, ArrayList
    private final MutableLiveData<Map<String,ArrayList<TrajectoryData>> > satBasicDataMutableMap
            = new MutableLiveData<>();

    public LiveData<Map<String, ArrayList<TrajectoryData>>> getLocationOfSatelliteFromSSC(
            ArrayList<String> satCodeList, String fromTime, String toTime){
        if(satBasicDataMutableMap.getValue() == null)
            for (String satCode : satCodeList) {
                callSatelliteDataFromSSCByName(satCode, fromTime, toTime);
            }
        return satBasicDataMutableMap;
    }


    private void  callSatelliteDataFromSSCByName(String satCode,String fromTime,String toTime){

        ApiCaller<String> caller = new ApiCaller<>(String.class, nasaSSCApi.getRetrofit());
        String relativePath = "locations/"+satCode+"/"+fromTime+","+toTime+"/geo";
        Map<String, Object> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        caller.GETString( relativePath, headers)
                .addOnFinishListener(new OnFinishListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        try{
                            JSONObject responseJson = new JSONObject(s);
                            Log.w(TAG,"SSC data fetching: process "+responseJson.optJSONObject("Result").optString("StatusCode"));
                            parseSSCLocationJSON(responseJson);
                            // Log.w(TAG,responseJsonString);
                        }catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "callSatelliteDataFromSSCByName:onFailure: ", e);
                    }
                });
    }

    private void parseSSCLocationJSON(JSONObject jsonObject){
        JSONObject result = jsonObject.optJSONObject("Result");
        //Log.w(TAG,"firstData:"+firstData);
        JSONArray dataArray =result.optJSONArray("Data").optJSONArray(1);

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject data = dataArray.optJSONObject(i);
            // Log.w(TAG,"dataObject:"+data);

            String satCode = data.optString("Id");

            JSONObject coordinates = data.optJSONArray("Coordinates").optJSONArray(1).optJSONObject(0);
            //Log.w(TAG,"dataObject:"+coordinates);
            JSONArray latitudes = coordinates.optJSONArray("Latitude").optJSONArray(1);
            JSONArray longitudes = coordinates.optJSONArray("Longitude").optJSONArray(1);

            JSONArray times = data.optJSONArray("Time").optJSONArray(1);

            JSONArray radialLengths = data.optJSONArray("RadialLength").optJSONArray(1);


            ArrayList<TrajectoryData> trajectoryDataArrayList = new ArrayList<>();
            for (int j = 0; j < latitudes.length(); j++) {
                double height = radialLengths.optDouble(j) - TleToGeo.getEarthRadiusAt(latitudes.optDouble(j));
                try {
                    double lat = latitudes.optDouble(j);
                    double lng = longitudes.optDouble(j)>180? longitudes.optDouble(j)-360 : longitudes.optDouble(j);

                    String timeString = times.optJSONArray(j).optString(1);
                    SimpleDateFormat utcDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sssZ");
                    utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date utcDateTime = utcDateFormat.parse(timeString);
                    SimpleDateFormat localFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sssZ");
                    String localTimeString = localFormat.format(new Date(utcDateTime.getTime()));
                    Date localDateTime = localFormat.parse(localTimeString);
                    //Log.w(TAG,"localTime:"+localTimeString);

                    TrajectoryData trajectoryData = new TrajectoryData(
                            satCode,lat,lng ,height, localDateTime.getTime());
                    // Log.w(TAG,"data: "+satelliteBasicData.toString());

                    trajectoryDataArrayList.add(trajectoryData);

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            Log.w(TAG," total size of "+satCode+":"+trajectoryDataArrayList.size());
            satMap.put(satCode,trajectoryDataArrayList);
            //aSatelliteBasicDataMutableList.postValue(satelliteBasicDataArrayList);
        }
        if(satMap.size()>=2){
            satBasicDataMutableMap.postValue(satMap);
        }

    }
}
