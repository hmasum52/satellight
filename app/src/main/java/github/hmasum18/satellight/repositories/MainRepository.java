package github.hmasum18.satellight.repositories;

import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import github.hmasum18.satellight.dataSources.NasaSSCApi;
import github.hmasum18.satellight.dataSources.NasaSSCApiDao;
import github.hmasum18.satellight.dataSources.OurApi;
import github.hmasum18.satellight.dataSources.OurApiDao;
import github.hmasum18.satellight.models.SatelliteBasicData;
import github.hmasum18.satellight.models.SatelliteData;
import github.hmasum18.satellight.models.TrajectoryData;
import github.hmasum18.satellight.utils.Utils;
import github.hmasum18.satellight.views.vr.Satellite;
import gov.nasa.worldwind.geom.Location;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainRepository {

    public static final String TAG = "MainRepo:";
    private NasaSSCApiDao nasaSSCApiDao ;
    private OurApiDao ourApiDao;

    //store basic data of a satellite
    private  Map<String,ArrayList<TrajectoryData>> satMap = new HashMap<>();
    //shortName, ArrayList
    private  MutableLiveData<Map<String,ArrayList<TrajectoryData>> > satBasicDataMutableMap = new MutableLiveData<>();
    //shortName, SatellitedData
    private  MutableLiveData<Map<String,SatelliteData> > satelliteDataMutableMap = new MutableLiveData<>();

    //for sub solar points ..The SubSolarPoint on a planet is the point at which its sun is perceived to be directly overhead
    private MutableLiveData<Location> currentSubSolarPointLocation = new MutableLiveData<>();


    public MainRepository() {
        NasaSSCApi nasaSSCApi = NasaSSCApi.getInstance();
        nasaSSCApiDao = nasaSSCApi.nasaSSEApiDao();
        OurApi ourApi = OurApi.getInstance();
        ourApiDao = ourApi.ourApiDao();
    }

    public LiveData<Map<String,ArrayList<TrajectoryData>> > getLocationOfSatelliteFromSSC(
            ArrayList<String> satCodeList, String fromTime, String toTime){
        if(satBasicDataMutableMap.getValue() == null)
        for (String satCode : satCodeList) {
            callSatelliteDataFromSSCByName(satCode, fromTime, toTime);
        }
        return satBasicDataMutableMap;
    }

    public LiveData<Map<String,SatelliteData>> getAllSatelliteData(long timestampBegin,long timestampEnd,LatLng userLocation){
        if(satelliteDataMutableMap.getValue() == null) //if no data is fetched yet then
            callSatelliteInfoFromOurApi(timestampBegin,timestampEnd,userLocation);
        return satelliteDataMutableMap;
    }

    private void  callSatelliteDataFromSSCByName(String satCode,String fromTime,String toTime){

        Call<ResponseBody> call = nasaSSCApiDao.getLocationOfSatellite(satCode,fromTime,toTime);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.isSuccessful()){
                    try{
                        String responseJsonString = response.body().string();
                        JSONObject responseJson = new JSONObject(responseJsonString);
                        Log.w(TAG,"SSC data fetching: process "+responseJson.optJSONObject("Result").optString("StatusCode"));
                        parseSSCLocationJSON(responseJson);
                        // Log.w(TAG,responseJsonString);
                    }catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }

                }else{
                    Log.w(TAG,"SSC data fetching: process is not successful "+response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
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
                double height = radialLengths.optDouble(j) - getEarthRadiusAt(latitudes.optDouble(j));
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

    /**
     * ref : https://rechneronline.de/earth-radius/
     * @param lat
     * @return
     */
    private double getEarthRadiusAt(double lat) {
         lat = lat*Math.PI/180; //convert to radian as Math.cos() function takes angle in radian;

        double e = 6378.137; //earth radius at the equator at sea level
        double p = 6356.752; //earth radius at the pole at sea level

        double nominator =  (e*e*Math.cos(lat))*(e*e*Math.cos(lat))+ (p*p*Math.sin(lat))*(p*p*Math.sin(lat));
        double denominator = (e*Math.cos(lat))*(e*Math.cos(lat))+ (p*Math.sin(lat))*(p*Math.sin(lat)) ;

        return Math.sqrt(nominator/denominator); //calculate earth radius and return
    }

    public void callSatelliteInfoFromOurApi(long timestampBegin,long timestampEnd, LatLng latLng){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type","all_trajectory");
        jsonObject.addProperty("freq",20000);
        jsonObject.addProperty("lat",latLng.latitude);
        jsonObject.addProperty("lng",latLng.longitude);
        jsonObject.addProperty("alt",0);
        jsonObject.addProperty("timestamp_begin",timestampBegin);
        jsonObject.addProperty("timestamp_end",timestampEnd);
        Call<ResponseBody> call = ourApiDao.getAllSatelliteInfo(jsonObject);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.isSuccessful()){
                    try{
                        Log.w(TAG,"our api call success");
                        String responseJsonObject = response.body().string();
                        JSONArray responseJson = new JSONArray(responseJsonObject);
                        Log.w(TAG,"our api response"+responseJsonObject);
                        parseSatelliteData(responseJson);
                    }catch (  JSONException | IOException e) {
                        e.printStackTrace();
                    }

                }else{
                    Log.w(TAG,"satellite data call is not successful "+response.code());
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.w(TAG,"failed "+t.getMessage());
            }
        });
    }

    private void parseSatelliteData(JSONArray jsonArray) {
        Map<String,SatelliteData> stringSatelliteDataMap = new HashMap<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject satelliteJsonObject = jsonArray.optJSONObject(i);
            SatelliteData satelliteData =  parseSatelliteInfo(satelliteJsonObject.optJSONObject("info"));
            ArrayList<TrajectoryData> trajectoryDataArrayList
                    = parseSatelliteTrajectory(satelliteJsonObject.optJSONArray("trajectory"),satelliteData.getShortName() );
            satelliteData.setTrajectoryDataList(trajectoryDataArrayList);
            stringSatelliteDataMap.put(satelliteData.getShortName(),satelliteData);
        }
        satelliteDataMutableMap.postValue(stringSatelliteDataMap);
    }

    private SatelliteData parseSatelliteInfo(JSONObject info) {
        SatelliteData satelliteData = new SatelliteData();
        satelliteData.setColor(info.optString("color"));
        satelliteData.setType(info.optString("type"));
        satelliteData.setLaunchDate(info.optString("launch_date"));
        satelliteData.setMissionDuration(info.optString("mission_duration"));
        satelliteData.setLaunchMass(info.optString("launch_mass"));
        satelliteData.setGeoStationary(info.optBoolean("isGeoStationary"));
        satelliteData.setTleLine1(info.optString("tle_line1"));
        satelliteData.setTleLine2(info.optString("tle_line2"));
        satelliteData.setFullName(info.optString("name"));
        satelliteData.setShortName(info.optString("sat_name"));
        satelliteData.setCountryName(info.optString("country_name"));
        satelliteData.setCountryFlagLink(info.optString("country_flag"));
        satelliteData.setIconUrl(info.optString("icon_url"));

        ArrayList<String> temp = new ArrayList<>();
        JSONArray jsonArray = info.optJSONArray("real_images");
        for (int i = 0; i < jsonArray.length(); i++) {
            temp.add(jsonArray.optString(i));
        }
        satelliteData.setRealImages(temp);

        temp = new ArrayList<>();
        jsonArray = info.optJSONArray("use_cases");
        for (int i = 0; i < jsonArray.length(); i++) {
            temp.add(jsonArray.optString(i));
        }
        satelliteData.setUseCases(temp);

        satelliteData.setDescription(info.optString("description"));

        return satelliteData;
    }

    private ArrayList<TrajectoryData> parseSatelliteTrajectory(JSONArray trajectory,String shortName) {
        ArrayList<TrajectoryData> trajectoryDataArrayList = new ArrayList<>();
       // Log.w(TAG,shortName+":"+trajectory.toString());
        for (int i = 0; i < trajectory.length(); i++) {
            JSONObject object = trajectory.optJSONObject(i);
            TrajectoryData trajectoryData = new TrajectoryData(
                    shortName,
                    object.optDouble("lat"),
                    object.optDouble("lng"),
                    object.optDouble("elevation"),
                    object.optDouble("azimuth"),
                    object.optDouble("range"),
                    object.optDouble("height"),
                    object.optDouble("velocity"),
                    object.optLong("timestamp")
            );
            trajectoryDataArrayList.add(trajectoryData);
        }
       // Log.w(TAG,shortName+"\n"+trajectoryDataArrayList);

        return trajectoryDataArrayList;
    }


}
