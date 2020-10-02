package github.hmasum18.satellight.dataSources;

import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OurApiDao {

    @POST("getTrajectory")
    Call<ResponseBody> getAllSatelliteInfo(@Body JsonObject jsonObject);
}
