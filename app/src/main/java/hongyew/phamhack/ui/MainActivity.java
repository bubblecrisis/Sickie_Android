package hongyew.phamhack.ui;

import android.support.v7.widget.Toolbar;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import hongyew.phamhack.MainApplication;
import hongyew.phamhack.R;
import hongyew.phamhack.api.TwillioApi;
import hongyew.phamhack.model.AccessToken;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@EActivity(R.layout.main_activity)
public class MainActivity extends AbstractDrawerActivity {
    @App
    MainApplication application;

    @ViewById(R.id.toolbar)
    Toolbar toolbar;

    @AfterViews
    protected void init() {
        super.init();
        getSupportActionBar().setTitle("Template App");
    
    
        TwillioApi api = application.twillioApi();
        api.getToken().enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                if (response.isSuccessful()) {
                    VideoActivity_.intent(MainActivity.this)
                        .twillioToken(response.body().token)
                        .start();
                    finish();
                }
            }
    
            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
        
            }
        });
    }

    @Override
    protected Toolbar getToolbar() {
        return toolbar;
    }

}
