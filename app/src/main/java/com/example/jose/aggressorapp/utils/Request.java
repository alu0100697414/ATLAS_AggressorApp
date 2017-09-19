package com.example.jose.aggressorapp.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jose on 15/09/2017.
 *
 * This class send to the server que GPS position from the 'aggressor'.
 */

public class Request {

    public static RequestQueue requestQueue;

    public static void pingAggressorDevice(Map<String, String> info, String server)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException {

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("latitude_aggressor", info.get("latitude_aggressor"));
        params.put("longitude_aggressor", info.get("longitude_aggressor"));

        Log.i("test", server + "/updateaggressorposition/" + info.get("victim_mac"));

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                com.android.volley.Request.Method.PUT,
                server + "/updateaggressorposition/" + info.get("victim_mac"),
                new JSONObject(params),
                new com.android.volley.Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e("Volley Ping Error ", response.toString());
                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Volley Ping Error ", error.toString());
                    }
                });

        requestQueue.add(jsonObjectRequest);
    }
}