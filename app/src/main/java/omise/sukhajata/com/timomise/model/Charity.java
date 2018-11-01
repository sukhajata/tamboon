package omise.sukhajata.com.timomise.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Charity {
    public final String id;
    public final String name;
    public final String logo_url;

    public Charity(String _id, String _name, String _logo_url) {
        this.id = _id;
        this.name = _name;
        this.logo_url = _logo_url;
    }

    @Override
    public String toString() {
        return name;
    }
}
