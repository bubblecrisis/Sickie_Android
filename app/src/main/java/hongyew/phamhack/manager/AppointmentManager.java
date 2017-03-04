package hongyew.phamhack.manager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.androidannotations.annotations.EBean;

/**
 * Created by hongyew on 4/03/2017.
 */
@EBean
public class AppointmentManager {
    
    public DatabaseReference appointmentRef() {
        return FirebaseDatabase.getInstance().getReference("appointments");
    }
    public DatabaseReference appointmentRef(String key) {
        return appointmentRef().child(key);
    }
    
}
