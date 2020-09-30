package github.hmasum18.satellight.dataSources;

import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface NasaSSCApiDao {

    @Headers("Accept: application/json")
    @POST("locations")
    Call<JSONObject> getLocations(@Body JSONObject object);

    @Headers("Accept:application/json")
    @GET("locations/{name}/{from},{to}/geo")
    Call<ResponseBody> getLocationOfSatellite(@Path("name") String name, @Path("from") String from, @Path("to") String to);
}
