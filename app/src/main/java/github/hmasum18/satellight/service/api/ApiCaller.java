package github.hmasum18.satellight.service.api;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * This class fetch data from the remote server as json object or json array
 *
 * It calls api end point for data by retrofit2
 * and notify the repositories when data is received.
 *
 * @author Hasan Masum
 * @version 1.0
 * @since 21/01/2021
 */
public class ApiCaller<T>{
    public static final String TAG = "JsonApiCaller:-->";
    private final ApiEndPoints apiEndPoints; //api endpoints
    private OnFinishListener<T> onFinishListener;
    private final Gson gson = new Gson();
    private final Type type;

    /**
     *  if we have a class name "Dummy"
     *  <ul>
     *      <li>type is Dummy.class for Dummy object</li>
     *      <li>type is Dummy[].class for a array of Dummy class</li>
     *      <li>type is new TypeToken<List<Dummy>>(){}.getType() for a list of Dummy class</li>
     *  </ul>
     *
     // * @param type is the type of response we want to get
     */
    public ApiCaller(Type type, Retrofit retrofit){
        this.type = type;
        this.apiEndPoints = retrofit.create(ApiEndPoints.class);
    }
    public void addOnFinishListener(OnFinishListener<T> onFinishListener) {
        this.onFinishListener = onFinishListener;
    }

    public ApiCaller<T> GETJson(String relativePath){
        Call<JsonElement> call = apiEndPoints.GET(relativePath);
        this.enqueueJsonRequest(call);
        return this;
    }

    public ApiCaller<T> GETJson(Map<String,String> headerMap, String relativePath){
        Call<JsonElement> call = apiEndPoints.GET(headerMap,relativePath);
        this.enqueueJsonRequest(call);
        return this;
    }

    public ApiCaller<T> POSTJson(String relativePath, Object object) {
        Call<JsonElement> call = apiEndPoints.POST(relativePath, object);
        Log.d(TAG, "POST()=> posting json element ");
        this.enqueueJsonRequest(call);
        return this;
    }

    public ApiCaller<T> POSTJson(Map<String,String> headerMap, String relativePath, Object object) {
        Call<JsonElement> call = apiEndPoints.POST(headerMap,relativePath, object);
        Log.d(TAG, "POST()=> posting json element ");
        this.enqueueJsonRequest(call);
        return this;
    }

    public void GETString(String relativePath){
        Call<ResponseBody> call = apiEndPoints.GETResponseBody(relativePath);
        enqueueRequest(call);
    }

    public void GETString(String relativePath, Map<String, Object> header){
        Call<ResponseBody> call = apiEndPoints.GETResponseBody(header, relativePath);
        enqueueRequest(call);
    }

    private void enqueueRequest(Call<ResponseBody> call){
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if(response.isSuccessful()){
                    ResponseBody responseBody = response.body();
                    try {
                        String string = responseBody.string();
                        onFinishListener.onSuccess((T)string);
                        Log.d(TAG,"enqueueRequest>onResponse: "+call.request().url()+" call is successful");
                    } catch (IOException e) {
                        e.printStackTrace();
                        onFinishListener.onFailure(e);
                    }
                }else{
                    onFinishListener.onFailure(new Exception("Error "+response.code()));
                    Log.d(TAG,"enqueueRequest>onResponse:  process is not successful. Response code: "+response.code());
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                String className = t.getClass().toString();
                onFinishListener.onFailure(new Exception(t));
                if(className.endsWith("UnknownHostException") )
                    Log.d(TAG,"enqueueRequest > Server is not responding");
                t.printStackTrace();
            }
        });
    }

    private void enqueueJsonRequest(Call<JsonElement> call){
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if(response.isSuccessful()){
                    JsonElement json = response.body();
                    Log.d(TAG,"enqueueRequest>onResponse: "+call.request().url()+" call is successful");
                    Log.d(TAG,"enqueueRequest>onResponse:"+type);
                    onFinishListener.onSuccess(gson.fromJson(json,type));
                }else{
                    onFinishListener.onFailure(new Exception("Error "+response.code()));
                    Log.d(TAG,"enqueueRequest>onResponse:  process is not successful. Response code: "+response.code());
                }
            }
            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                String className = t.getClass().toString();
                onFinishListener.onFailure(new Exception(t));
                if(className.endsWith("UnknownHostException") )
                    Log.d(TAG,"enqueueRequest > Server is not responding");
                t.printStackTrace();
            }
        });
    }
}
