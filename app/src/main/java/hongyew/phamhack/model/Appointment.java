package hongyew.phamhack.model;

import com.google.firebase.database.Exclude;

/**
 * Created by hongyew on 4/03/2017.
 */

public class Appointment {
    public String conference;
    public Long time;
    public User user;
    
    @Exclude
    public String key;
    
    @Exclude
    public boolean busy = false;
    
    @Exclude
    public boolean disabled = false;
    
    public static class User {
        public String name;
        public Long age;
        public String email;
        public User() {}
        
        public User(String name, Long age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }
    }
}
