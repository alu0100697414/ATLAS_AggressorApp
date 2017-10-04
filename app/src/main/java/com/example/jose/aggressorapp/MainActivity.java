package com.example.jose.aggressorapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.toolbox.Volley;
import com.example.jose.aggressorapp.utils.GpsTracker;
import com.example.jose.aggressorapp.utils.Request;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String LOG = "MainActivity";
    private static final int ACCESS_FINE_LOCATION_CALLBACK = 0;

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);;
    private GpsTracker gpsTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Request.requestQueue = Volley.newRequestQueue(this);

        // Request permission.
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_CALLBACK);
        }

        gpsTrack = new GpsTracker(this);
        // Shows dialog to activate GPS if it is not activated
        if(!gpsTrack.canGetLocation()) {
            AlertDialog.Builder bt_dialog = new AlertDialog.Builder(this);
            bt_dialog.setTitle("Activar GPS");
            bt_dialog.setMessage("Por favor, active el servicio GPS.");
            bt_dialog.setCancelable(false);
            bt_dialog.setPositiveButton("Activar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent toGPSEnable = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(toGPSEnable);
                }
            });
            bt_dialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // If aggressor doesn't activate GPS
                }
            });
            bt_dialog.show();
        }

        Runnable pingService = new Runnable() {
            public void run() {
                try {
                    sendPingToServer();
                }
                catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                }
            }
        };

        executor.scheduleAtFixedRate(pingService, 0, 15, TimeUnit.SECONDS);
    }

    public void sendPingToServer() throws ClassNotFoundException,
            InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, IOException {

        Map<String, String> data = new HashMap<String, String>();
        data.put("victim_mac", "78:4B:87:76:4C:82");

        gpsTrack = new GpsTracker(this);

        if (gpsTrack != null && gpsTrack.canGetLocation()){
            data.put("latitude_aggressor", String.valueOf(gpsTrack.getLocation().getLatitude()));
            data.put("longitude_aggressor", String.valueOf(gpsTrack.getLocation().getLongitude()));
        } else {
            data.put("latitude_aggressor", "null");
            data.put("longitude_aggressor", "null");
            Toast.makeText(gpsTrack,
                    "No se ha podido obtener la posición GPS.",
                    Toast.LENGTH_SHORT).
                    show();
        }

        String url_server = "http://4tl4s.duckdns.org:8000";

        Request.pingAggressorDevice(data, url_server, this, getApplicationContext());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ACCESS_FINE_LOCATION_CALLBACK: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this,
                            "Permiso activado correctamente.",
                            Toast.LENGTH_SHORT).
                            show();
                    gpsTrack = new GpsTracker(this);

                    if (gpsTrack.canGetLocation()) {

                    }
                } else {
                    Toast.makeText(this,
                            "No se ha activado el permiso.",
                            Toast.LENGTH_SHORT).
                            show();
                }
                return;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(!executor.isShutdown()){
            executor.shutdown();
        }
    }
}
