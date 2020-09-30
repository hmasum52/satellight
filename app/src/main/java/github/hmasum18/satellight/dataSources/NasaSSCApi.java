package github.hmasum18.satellight.dataSources;

import android.util.Log;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * This class is a singleton
 */
public class NasaSSCApi {

    public static final String TAG = "NasaSSEApi:";
    //create and init Data access object to fetch data from this api
    private NasaSSCApiDao nasaSSCApiDao;

    //to make the class singleton
    private static NasaSSCApi nasaSSCApiInstance;
    private NasaSSCApi(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://sscweb.gsfc.nasa.gov/WS/sscr/2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        //init the Dao
        //retrofit will implement this interface
        nasaSSCApiDao = retrofit.create(NasaSSCApiDao.class);

        Log.w(TAG," retrofit create NasaSSEApi successfully.");
    }

    public static NasaSSCApi getInstance(){
        if(nasaSSCApiInstance == null)
            nasaSSCApiInstance = new NasaSSCApi();
        return nasaSSCApiInstance;
    }

    public NasaSSCApiDao nasaSSEApiDao(){return nasaSSCApiDao;};
}
