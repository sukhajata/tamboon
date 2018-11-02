package omise.sukhajata.com.timomise.utility;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Console;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import co.omise.android.models.Token;
import omise.sukhajata.com.timomise.interfaces.DownloadCallback;
import omise.sukhajata.com.timomise.model.Charity;
import omise.sukhajata.com.timomise.model.DonationRequest;

public class ApiManager {

    private static ApiManager instance;
    private static String apiBase = "https://omise-tim.herokuapp.com/";
    private static String charitiesUrl = apiBase + "charities";
    private static String donationsURl = apiBase + "donations";

    private Context mContext;
    private RequestQueue mRequestQueue;

    //private Constructor for internal usage
    private ApiManager(Context context) {
        mContext = context;
        mRequestQueue = Volley.newRequestQueue(mContext);
    }

    public static ApiManager getInstance(Context context) {
        if (instance == null) {
            instance = new ApiManager(context);
        }

        return instance;
    }

    public void getCharities(final DownloadCallback callback) {

        JsonArrayRequest arrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                charitiesUrl,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Moshi moshi = new Moshi.Builder().build();
                        Type type = Types.newParameterizedType(List.class, Charity.class);
                        JsonAdapter<List<Charity>> jsonAdapter = moshi.adapter(type);
                        try {
                            List<Charity> charities = jsonAdapter.fromJson(response.toString());
                            callback.onDownloadCompleted(charities);
                        } catch (IOException ex) {
                            callback.onDownloadError(DownloadCallback.PARSING_ERROR, ex.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("VOLLEY", error.toString());
                        callback.onDownloadError(DownloadCallback.ERROR, parseVolleyError(error));
                    }
                }
        );
        mRequestQueue.add(arrayRequest);
    }

    public void makeDonation(String token, String name, int amount, final DownloadCallback callback) {
        DonationRequest donationRequest = new DonationRequest(token, name, amount);
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<DonationRequest> jsonAdapter = moshi.adapter(DonationRequest.class);
        String jsonData = jsonAdapter.toJson(donationRequest);
        Log.d("DONATION", jsonData);
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JsonObjectRequest objectRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    donationsURl,
                    jsonObject,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String message = (String)response.get("message");
                                callback.onDownloadCompleted(message);
                            } catch (JSONException ex) {
                                callback.onDownloadError(DownloadCallback.PARSING_ERROR, "Unknown response format.");
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            callback.onDownloadError(DownloadCallback.ERROR, parseVolleyError(error));
                        }
                    }
            );
            mRequestQueue.add(objectRequest);
        } catch (JSONException ex) {
            callback.onDownloadError(DownloadCallback.PARSING_ERROR, ex.getMessage());
        }

    }

    public String parseVolleyError(VolleyError error) {
        try {
            String responseBody = new String(error.networkResponse.data, "utf-8");
            Log.e("VOLLEY", responseBody);
            JSONObject data = new JSONObject(responseBody);
            return data.getString("message");
        } catch (UnsupportedEncodingException ex1) {
            Log.e("VOLLEY", "UnsupportedEncodingException: " + ex1.getMessage());
        } catch (JSONException ex2) {
            Log.e("VOLLEY", "JSONEXCEPTION: " + ex2.getMessage());
        }

        return "";
    }

}
