package hongyew.phamhack;

import org.androidannotations.annotations.sharedpreferences.SharedPref;

/**
 * Created by hongyew on 13/10/2016.
 */
@SharedPref(SharedPref.Scope.APPLICATION_DEFAULT)
public interface AppPreference {

    String appointmentKey();

}
