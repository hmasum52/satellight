package github.hmasum18.satellight.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public abstract class NetworkModule{
    public static final String SATELLITE_DATA_URL = "https://raw.githubusercontent.com/Hmasum18/satellight-data/master/";
    public static final String TLE_API_BASE_URL = "https://tle.ivanstanojevic.me/api/tle/";
    public static final String NASA_SSC_API = "https://sscweb.gsfc.nasa.gov/WS/sscr/2/";


    @Provides
    @Singleton
    static SatelliteDataSource provideSatelliteDataSource() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .followRedirects(true)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SATELLITE_DATA_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return new SatelliteDataSource(retrofit);
    }

    // https://celestrak.com/NORAD/documentation/gp-data-formats.php
    // https://tle.ivanstanojevic.me/
    @Provides
    @Singleton
    static TLEApi provideTLEApi() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .followRedirects(true)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(TLE_API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return new TLEApi(retrofit);
    }

    @Provides
    @Singleton
    static NasaSSCApi provideNasaSSCApi() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .followRedirects(true)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(NASA_SSC_API)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return new NasaSSCApi(retrofit);
    }

    public static class RetrofitWrapper{
        private final Retrofit retrofit;

        public RetrofitWrapper(Retrofit retrofit) {
            this.retrofit = retrofit;
        }

        public Retrofit getRetrofit() {
            return retrofit;
        }
    }

    public static class SatelliteDataSource {
        private final Retrofit retrofit;

        public SatelliteDataSource(Retrofit retrofit) {
            this.retrofit = retrofit;
        }

        public Retrofit getRetrofit() {
            return retrofit;
        }
    }

    public static class TLEApi {

        private final Retrofit retrofit;

        public TLEApi(Retrofit retrofit) {
            this.retrofit = retrofit;
        }

        public Retrofit getRetrofit() {
            return retrofit;
        }
    }

    public static class NasaSSCApi{

        private final Retrofit retrofit;

        public NasaSSCApi(Retrofit retrofit) {
            this.retrofit = retrofit;
        }

        public Retrofit getRetrofit() {
            return retrofit;
        }
    }
}
