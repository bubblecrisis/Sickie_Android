package hongyew.phamhack.ui;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;

import hongyew.phamhack.MainApplication;
import hongyew.phamhack.R;
import hongyew.phamhack.manager.ConferenceManager;

@EActivity(R.layout.buy_complete_activity)
public class BuyCompleteActivity extends AppCompatActivity {
    @App
    MainApplication application;

    @Extra
    String total;
    
    @Extra
    String appointmentKey;
    
    @ViewById(R.id.total_value)
    TextView totalView;
    
    @Bean
    ConferenceManager conferenceManager;

    ProgressDialog progressDialog;
    
    @AfterViews
    protected void init() {
        totalView.setText(total);
        
    }

    @Click(R.id.ok_button)
    void okClicked() {
        finish();
    }
}
