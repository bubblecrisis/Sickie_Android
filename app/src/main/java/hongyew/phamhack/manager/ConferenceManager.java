package hongyew.phamhack.manager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

/**
 * Created by hongyew on 4/03/2017.
 */
@EBean
public class ConferenceManager {
    
    @Bean
    AppointmentManager appointmentManager;
    
    public DatabaseReference confRef() {
        return FirebaseDatabase.getInstance().getReference("conference");
    }
    
    public DatabaseReference confRef(String room) {
        return confRef().child(room);
    }
    
    public DatabaseReference basketRef(String room) {
        return appointmentManager.appointmentRef(room).child("basket");
    }
    
}
