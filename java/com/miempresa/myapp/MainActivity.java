package com.miempresa.myapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Spinner spinnerUsuarios;
    private TextView estadoText;
    private Button btnIniciar, btnPausar, btnReanudar, btnFinalizar;

    private boolean tracking = false;
    private boolean pausado = false;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private File rutaDatos;

    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final int INTERVALO_UBICACION_MS = 60000; // 1 minuto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // tu layout XML

        spinnerUsuarios = findViewById(R.id.spinnerUsuarios);
        estadoText = findViewById(R.id.estadoText);
        btnIniciar = findViewById(R.id.btnIniciar);
        btnPausar = findViewById(R.id.btnPausar);
        btnReanudar = findViewById(R.id.btnReanudar);
        btnFinalizar = findViewById(R.id.btnFinalizar);

        // Lista de usuarios
        String[] usuarios = {"Usuario1", "Usuario2"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, usuarios);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUsuarios.setAdapter(adapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Ruta del archivo JSON en almacenamiento externo
        rutaDatos = new File(Environment.getExternalStorageDirectory(), "fichajes_service.json");

        // Botones
        btnIniciar.setOnClickListener(v -> iniciarFichaje());
        btnPausar.setOnClickListener(v -> pausarFichaje());
        btnReanudar.setOnClickListener(v -> reanudarFichaje());
        btnFinalizar.setOnClickListener(v -> finalizarFichaje());

        solicitarPermisos();
    }

    private void solicitarPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void iniciarFichaje() {
        if (spinnerUsuarios.getSelectedItem() == null) {
            Toast.makeText(this, "Selecciona un usuario primero", Toast.LENGTH_SHORT).show();
            return;
        }

        tracking = true;
        pausado = false;
        estadoText.setText("Fichaje en curso...");

        startLocationUpdates();
        Toast.makeText(this, "Fichaje iniciado", Toast.LENGTH_SHORT).show();
    }

    private void pausarFichaje() {
        pausado = true;
        estadoText.setText("Fichaje pausado");
        Toast.makeText(this, "Fichaje pausado", Toast.LENGTH_SHORT).show();
    }

    private void reanudarFichaje() {
        pausado = false;
        estadoText.setText("Fichaje en curso...");
        Toast.makeText(this, "Fichaje reanudado", Toast.LENGTH_SHORT).show();
    }

    private void finalizarFichaje() {
        tracking = false;
        estadoText.setText("Fichaje finalizado");
        stopLocationUpdates();
        Toast.makeText(this, "Fichaje finalizado y guardado localmente", Toast.LENGTH_SHORT).show();
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(INTERVALO_UBICACION_MS);
        request.setFastestInterval(INTERVALO_UBICACION_MS);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (tracking && !pausado) {
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lon = locationResult.getLastLocation().getLongitude();
                    registrarUbicacion(lat, lon);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, null);
        }
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void registrarUbicacion(double lat, double lon) {
        try {
            String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String hora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

            JSONObject registro = new JSONObject();
            registro.put("Fecha", fecha);
            registro.put("Hora", hora);
            registro.put("Usuario", spinnerUsuarios.getSelectedItem().toString());
            registro.put("Latitud", lat);
            registro.put("Longitud", lon);

            JSONArray data = new JSONArray();
            if (rutaDatos.exists()) {
                try (FileReader reader = new FileReader(rutaDatos)) {
                    char[] buffer = new char[(int) rutaDatos.length()];
                    reader.read(buffer);
                    String content = new String(buffer);
                    if (!content.isEmpty()) {
                        data = new JSONArray(content);
                    }
                }
            }

            data.put(registro);

            try (FileWriter writer = new FileWriter(rutaDatos)) {
                writer.write(data.toString(2));
            }

            Log.d("GPSService", "Ubicación registrada: " + registro.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permiso requerido para GPS/Almacenamiento", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }
}
