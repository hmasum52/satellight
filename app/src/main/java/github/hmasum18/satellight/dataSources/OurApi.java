package github.hmasum18.satellight.dataSources;

import android.util.Log;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OurApi {
    public static final String TAG = "OurApi:";
    //create and init Data access object to fetch data from this api
    private OurApiDao ourApiDao;

    //to make the class singleton
    private static OurApi nasaSSCApiInstance;
    private OurApi(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://us-central1-smsapi-3b6ac.cloudfunctions.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        //init the Dao
        //retrofit will implement this interface
        ourApiDao = retrofit.create(OurApiDao.class);

        Log.w(TAG," retrofit create OurApi successfully.");
    }

    public static OurApi getInstance(){
        if(nasaSSCApiInstance == null)
            nasaSSCApiInstance = new OurApi();
        return nasaSSCApiInstance;
    }

    public OurApiDao ourApiDao() {return ourApiDao;};
}
