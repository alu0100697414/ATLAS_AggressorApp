package com.example.jose.aggressorapp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.Volley;
import com.example.jose.aggressorapp.utils.GpsTracker;
import com.example.jose.aggressorapp.utils.Network;
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
    private static final int RESULT_CODE_GPS = 123;

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);;
    private GpsTracker gpsTrack;

    private static final String VICTIM_MAC = "78:4B:87:76:4C:82";
    private static final String SERVER_URL = "https://4tl4s.duckdns.org:443";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Shared preferences
        prefs = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE);
        editor = prefs.edit();

        // Init Volley
        Request.requestQueue = Volley.newRequestQueue(this);

        // Request GPS permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_CALLBACK);
        }

        // HTTPS support
        new Network().handleSSLHandshake();

        // Init GpsTracker object
        gpsTrack = new GpsTracker(this);

        // Shows dialog to activate GPS if it is not activated
        if(!gpsTrack.canGetLocation()) {

            AlertDialog.Builder gps_dialog = new AlertDialog.Builder(this);

            gps_dialog.setTitle("Activar GPS");
            gps_dialog.setMessage("Por favor, active el servicio GPS.");
            gps_dialog.setCancelable(false);

            gps_dialog.setPositiveButton("ACTIVAR", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent settingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(settingIntent, RESULT_CODE_GPS);
                }
            });

            gps_dialog.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TextView distance = (TextView) findViewById(R.id.distance_number);
                    distance.setText("GPS desactivado");
                }
            });

            gps_dialog.show();

        } else { // Start sending ping to server

            Runnable pingService = new Runnable() {
                public void run() {
                    try { sendPingToServer(); }
                    catch (ClassNotFoundException e) { e.printStackTrace(); }
                    catch (NoSuchAlgorithmException e) { e.printStackTrace(); }
                    catch (IOException e) { e.printStackTrace(); }
                    catch (NoSuchProviderException e) { e.printStackTrace(); }
                    catch (InvalidKeyException e) { e.printStackTrace(); }
                }
            };

            executor.scheduleAtFixedRate(pingService, 0, 15, TimeUnit.SECONDS);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        // Diálogo para introducir la info del agresor
        if(id == R.id.action_user){

            LayoutInflater factory = LayoutInflater.from(this);
            final View textEntryView = factory.inflate(R.layout.user_dialog, null);

            final EditText input1 = (EditText) textEntryView.findViewById(R.id.contact_name);
            final EditText input2 = (EditText) textEntryView.findViewById(R.id.contact_phone);

            input1.setText(prefs.getString("name", ""));
            input2.setText(prefs.getString("number", ""));

            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Información de contacto")
                    .setView(textEntryView)
                    .setPositiveButton("ACTUALIZAR",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    editor.putString("name", input1.getText().toString());
                                    editor.putString("number", input2.getText().toString());
                                    editor.commit();

                                    Toast.makeText(getApplicationContext(),
                                            "Datos de contacto actualizados",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                    .setNegativeButton("CANCELAR",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Toast.makeText(getApplicationContext(),
                                            "Operación cancelada",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
            alert.show();
        }

        // Diálogo para introducir la info del servidor
        if(id == R.id.action_server){

            LayoutInflater factory = LayoutInflater.from(this);
            final View textEntryView = factory.inflate(R.layout.server_dialog, null);

            final EditText input1 = (EditText) textEntryView.findViewById(R.id.url_server);
            final EditText input2 = (EditText) textEntryView.findViewById(R.id.mac_victim);

            input1.setText(prefs.getString("url_server", SERVER_URL));
            input2.setText(prefs.getString("mac_victim", VICTIM_MAC));

            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Acceso al servidor")
                    .setView(textEntryView)
                    .setPositiveButton("ACTUALIZAR",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    editor.putString("url_server", input1.getText().toString());
                                    editor.putString("mac_victim", input2.getText().toString());
                                    editor.commit();

                                    Toast.makeText(getApplicationContext(),
                                            "Datos del servidor actualizados",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                    .setNegativeButton("CANCELAR",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Toast.makeText(getApplicationContext(),
                                            "Operación cancelada",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
            alert.show();
        }

        return super.onOptionsItemSelected(item);
    }

    // Method to send ping to server
    public void sendPingToServer() throws ClassNotFoundException,
            InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, IOException {

        // Victim mac
        Map<String, String> data = new HashMap<String, String>();
        data.put("victim_mac", prefs.getString("mac_victim", VICTIM_MAC));

        // Aggressor info
        data.put("aggressor_name", prefs.getString("name", ""));
        data.put("aggressor_number", prefs.getString("number", ""));

        // Battery info
        BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
        int batLevel = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }

        data.put("aggressor_battery", String.valueOf(batLevel));

        // GPS position
        gpsTrack = null;
        gpsTrack = new GpsTracker(this);

        if (gpsTrack != null && gpsTrack.canGetLocation() && gpsTrack.getLocation() != null){
            Log.d(LOG, String.valueOf(gpsTrack.canGetLocation()));
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

        // Server url
        String url_server = prefs.getString("url_server", SERVER_URL);

        Request.pingAggressorDevice(data, url_server, this, getApplicationContext());
    }

    @Override
    // Permission callback
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ACCESS_FINE_LOCATION_CALLBACK: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);

                    Toast.makeText(this,
                            "Permiso activado correctamente.",
                            Toast.LENGTH_SHORT).
                            show();
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // GPS activation result
        if (requestCode == RESULT_CODE_GPS) {

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
                        Intent SettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        //SettingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivityForResult(SettingIntent, 123);
                    }
                });
                bt_dialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TextView distance = (TextView) findViewById(R.id.distance_number);
                        distance.setText("GPS desactivado");
                    }
                });
                bt_dialog.show();
            } else {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
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
