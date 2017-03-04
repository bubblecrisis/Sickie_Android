package hongyew.phamhack;

import android.app.Application;

import com.facebook.stetho.Stetho;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EApplication;

import hongyew.phamhack.api.TwillioApi;
import hongyew.phamhack.model.Appointment;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by hongyew on 29/09/2016.
 */

@EApplication
public class MainApplication extends Application {

    @AfterInject
    public void init() {
        Timber.plant(new Timber.DebugTree());
        initializeCustomFont();
        Stetho.initializeWithDefaults(this);
    }
    
    public Retrofit retrofit() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        
        Retrofit retrofit = new Retrofit.Builder()
                                .baseUrl("https://1vwj8sfnm3.execute-api.ap-southeast-2.amazonaws.com")
                                .client(client)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();
        
        return retrofit;
    }
    
    public TwillioApi twillioApi() {
        return retrofit().create(TwillioApi.class);
    }

    public Appointment.User appointmentSelf() {
        return new Appointment.User("Hong Yew", 30L);
    };
    
    
    private void initializeCustomFont() {
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/HelveticaNeue-Light.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());
    }

}
