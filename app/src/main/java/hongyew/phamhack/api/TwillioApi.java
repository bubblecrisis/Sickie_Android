package hongyew.phamhack.api;

import hongyew.phamhack.model.AccessToken;
import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by hongyew on 4/03/2017.
 */

public interface TwillioApi {
    
    @GET("/prod/getAuthToken?identity=HACK")
    Call<AccessToken> getToken();
}
