package github.hmasum18.satellight.repositories;

import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
import github.hmasum18.satellight.models.SatelliteBasicData;
import github.hmasum18.satellight.utils.Utils;
import gov.nasa.worldwind.geom.Location;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainRepository {

    public static final String TAG = "MainRepo:";
    private NasaSSCApiDao nasaSSCApiDao ;

    //store basic data of a satellite
    private  Map<String,ArrayList<SatelliteBasicData>> satMap = new HashMap<>();
    private  MutableLiveData<Map<String,ArrayList<SatelliteBasicData>> > satBasicDataMutableMap = new MutableLiveData<>();

    //for sub solar points ..The SubSolarPoint on a planet is the point at which its sun is perceived to be directly overhead
    private MutableLiveData<Location> currentSubSolarPointLocation = new MutableLiveData<>();


    public MainRepository() {
        NasaSSCApi nasaSSCApi = NasaSSCApi.getInstance();
        nasaSSCApiDao = nasaSSCApi.nasaSSEApiDao();
    }

    public LiveData<Map<String,ArrayList<SatelliteBasicData>> > getLocationOfSatellites(ArrayList<String> satCodeList, String fromTime, String toTime){
        if(satBasicDataMutableMap.getValue() == null)
        for (String satCode : satCodeList) {
            callSatelliteDataFromSSCByName(satCode, fromTime, toTime);
        }
        return satBasicDataMutableMap;
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


            ArrayList<SatelliteBasicData> satelliteBasicDataArrayList = new ArrayList<>();
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
                    Log.w(TAG,"localTime:"+localTimeString);

                    SatelliteBasicData satelliteBasicData = new SatelliteBasicData(satCode,lat,lng,height, localDateTime.getTime());
                    Log.w(TAG,"data: "+satelliteBasicData.toString());

                    satelliteBasicDataArrayList.add(satelliteBasicData);

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            Log.w(TAG," total size of "+satCode+":"+satelliteBasicDataArrayList.size());
            satMap.put(satCode,satelliteBasicDataArrayList);
            //aSatelliteBasicDataMutableList.postValue(satelliteBasicDataArrayList);
        }
        if(satMap.size()>=5){
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



}
