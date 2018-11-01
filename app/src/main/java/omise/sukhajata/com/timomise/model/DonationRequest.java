package omise.sukhajata.com.timomise.model;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DonationRequest {
    public final String token;
    public final String name;
    public final int amount;

    public DonationRequest(String _token, String _name, int _amount) {
        this.token = _token;
        this.name = _name;
        this.amount = _amount;
    }

}
