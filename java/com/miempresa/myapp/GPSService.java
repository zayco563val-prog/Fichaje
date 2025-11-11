package com.miempresa.myapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GPSService extends Service {

    private static final String CHANNEL_ID = "gps_service_channel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private File rutaDatos;

    @Override
    public void onCreate() {
        super.onCreate();

        // Definir archivo de guardado
        File storageDir = Environment.getExternalStorageDirectory();
        rutaDatos = new File(storageDir, "fichajes_service.json");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoreo activo")
                .setContentText("Servicio GPS en ejecución")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        startForeground(1, notification);

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(60000); // 1 minuto
        locationRequest.setFastestInterval(60000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    registrarUbicacion(location);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            Log.e("GPSService", "Permiso de ubicación no concedido", e);
        }
    }

    private void registrarUbicacion(Location location) {
        String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        JSONObject registro = new JSONObject();
        try {
            registro.put("Fecha", fecha);
            registro.put("Hora", hora);
            registro.put("Latitud", location.getLatitude());
            registro.put("Longitud", location.getLongitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Leer registros existentes
        JSONArray data = new JSONArray();
        if (rutaDatos.exists()) {
            try (FileReader reader = new FileReader(rutaDatos)) {
                char[] buffer = new char[(int) rutaDatos.length()];
                reader.read(buffer);
                String content = new String(buffer);
                if (!content.isEmpty()) {
                    data = new JSONArray(content);
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }

        // Agregar nuevo registro
        data.put(registro);

        // Guardar JSON
        try (FileWriter writer = new FileWriter(rutaDatos)) {
            writer.write(data.toString(2));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("GPSService", "Ubicación registrada: " + registro.toString());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "GPS Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
