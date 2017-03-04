package hongyew.phamhack.ui;

import android.app.ProgressDialog;
import android.support.v7.widget.Toolbar;

import org.android1liner.ui.ProgressUitls;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Click;
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

    ProgressDialog progressDialog;
    
    @AfterViews
    protected void init() {
        super.init();
        getSupportActionBar().setTitle(getString(R.string.app_name));
    }

    @Override
    protected Toolbar getToolbar() {
        return toolbar;
    }
    
    void videoConference() {
        TwillioApi api = application.twillioApi();
        progressDialog = ProgressUitls.showProgress(this);
        api.getToken().enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(Call<AccessToken> call, Response<AccessToken> response) {
                if (response.isSuccessful()) {
                    ProgressUitls.hideProgress(progressDialog);
                    VideoActivity_.intent(MainActivity.this)
                        .twillioToken(response.body().token)
                        .start();
                }
            }
        
            @Override
            public void onFailure(Call<AccessToken> call, Throwable t) {
                ProgressUitls.hideProgress(progressDialog);
            }
        });
    }
    
    @Click(R.id.main_button_conference)
    void videoConferenceClicked() {
        videoConference();
    }
    
    @Click(R.id.main_button_appointment)
    void appointmentClicked() {
        AppointmentActivity_.intent(this).start();
    }
}
