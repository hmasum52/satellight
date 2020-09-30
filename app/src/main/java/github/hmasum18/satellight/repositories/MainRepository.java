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
        for (String satCode : satCodeList) {
            callSatelliteDataFromSSCByName(satCode, fromTime, toTime);
        }
        return satBasicDataMutableMap;
    }

    /**
     * calls the SubSolarPointFinderTask to find SubSolarPoint from web
     * @return current Location of SubSolarPoint when found
     */
    public LiveData<Location> getCurrentSubSolarPointLocation() {
        new SubSolarPointFinderTask().execute();
        return currentSubSolarPointLocation;
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

    /**
     * this async task fetch html response from https://www.timeanddate.com/worldclock/sunearth.php
     * and then parse the html response with Jsoup and init currentSubSolarPointLocation LiveData
     */
    private class SubSolarPointFinderTask extends AsyncTask<Void,Void,Void> {

        StringBuilder stringBuilder = new StringBuilder();

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                URL url = new URL("https://www.timeanddate.com/worldclock/sunearth.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                //now get the response html as string
                BufferedReader responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String responseLine;
                while( (responseLine = responseReader.readLine()) != null){
                    stringBuilder.append(responseLine);
                }
                responseReader.close();
                connection.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //parse html responses with Jsoup
            Document htmlResponseDoc = Jsoup.parse(stringBuilder.toString());

            String searchText = "Position of the Sun: Subsolar Point";
            //Log.w(TAG,"doc: "+htmlResponseDoc.toString());
            if(stringBuilder.toString().contains(searchText)){
               // Log.w(TAG,"doc: "+htmlResponseDoc.toString());
/*
                int idx =  htmlResponseDoc.toString().indexOf("The Sun is currently above");
                int idx2 = htmlResponseDoc.toString().indexOf("The Moon is currently above",idx);
                Log.w(TAG,"out: "+htmlResponseDoc.toString().substring(idx,idx2));*/

                Elements elements = htmlResponseDoc.getElementsByTag("p");

                //find for ex: On Friday, 18 September 2020, 18:48:00 UTC the Sun is at its zenith at Latitude: 1° 28' North, Longitude: 103° 32' West
                String subSolarPointPosition = "";
                for (Element element : elements) {

                    String text = element.text();
                    if(text.contains("Sun is at its zenith")){
                        Log.w(TAG,"SubSolarPointFinderTask:"+text);
                        subSolarPointPosition = text;
                    }
                }
                int idx = subSolarPointPosition.indexOf("Latitude: ")+"Latitude: ".length();
                int idx1 = subSolarPointPosition.indexOf(",",idx);
                //ex: 1° 28' North
                String latString = subSolarPointPosition.substring(idx,idx1);
                String[] temp = latString.split(" ");

                double latitude = 0;
                if(temp.length == 3){
                    latitude = Double.parseDouble(temp[0].substring(0,temp[0].length()-1)); //1.0000
                    latitude += Double.parseDouble(temp[1].substring(0,temp[1].length()-1))/60d;//1.466666666
                    latitude = temp[2].equals("North") ? latitude : -latitude; //1.4666666
                }
                Log.w(TAG,"latitude: "+latitude);

                idx = subSolarPointPosition.indexOf("Longitude: ")+"Longitude: ".length();
                //ex: 103° 32' West
                String lngString = subSolarPointPosition.substring(idx);

                Log.w(TAG,"latString: "+latString+" lngString: "+lngString);
                temp = lngString.split(" ");

                double longitude = 0;
                if(temp.length == 3){
                    longitude = Double.parseDouble(temp[0].substring(0,temp[0].length()-1)); //103.0000
                    longitude += Double.parseDouble(temp[1].substring(0,temp[1].length()-1))/60d; //103.5333333
                    longitude = temp[2].equals("East") ? longitude : -longitude;//-103.533333
                }
                Log.w(TAG,"latitude: "+longitude);

               // gov.nasa.worldwind.geom.
                Location subSolarPointLocation = new Location(latitude,longitude);
                currentSubSolarPointLocation.postValue(subSolarPointLocation);
            }else{
                Log.w(TAG,"SubSolarPointFinderTask: sun sub solar point not found");
            }
        }
    }
}
