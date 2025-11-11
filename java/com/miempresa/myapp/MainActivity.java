package com.miempresa.myapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
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

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Spinner spinnerUsuarios;
    private TextView estadoText;
    private Button btnIniciar, btnPausar, btnReanudar, btnFinalizar;
    private boolean tracking = false;
    private boolean pausado = false;

    private ArrayList<JSONObject> registros = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private static final int PERMISSION_REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // XML que hay que crear

        spinnerUsuarios = findViewById(R.id.spinnerUsuarios);
        estadoText = findViewById(R.id.estadoText);
        btnIniciar = findViewById(R.id.btnIniciar);
        btnPausar = findViewById(R.id.btnPausar);
        btnReanudar = findViewById(R.id.btnReanudar);
        btnFinalizar = findViewById(R.id.btnFinalizar);

        // Lista de usuarios (puedes cargarla luego desde Google Sheets)
        String[] usuarios = {"Usuario1", "Usuario2"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, usuarios);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUsuarios.setAdapter(adapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnIniciar.setOnClickListener(v -> iniciarFichaje());
        btnPausar.setOnClickListener(v -> pausarFichaje());
        btnReanudar.setOnClickListener(v -> reanudarFichaje());
        btnFinalizar.setOnClickListener(v -> finalizarFichaje());

        solicitarPermisos();
    }

    private void solicitarPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
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
        guardarRegistrosLocal();
        Toast.makeText(this, "Fichaje finalizado y guardado localmente", Toast.LENGTH_SHORT).show();
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(60000); // cada minuto
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (tracking && !pausado) {
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lon = locationResult.getLastLocation().getLongitude();
                    guardarRegistro(lat, lon);
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

    private void guardarRegistro(double lat, double lon) {
        try {
            JSONObject registro = new JSONObject();
            registro.put("Fecha", new Date().toString());
            registro.put("Nombre", spinnerUsuarios.getSelectedItem().toString());
            registro.put("Latitud", lat);
            registro.put("Longitud", lon);
            registros.add(registro);
            System.out.println("Registro guardado: " + registro.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void guardarRegistrosLocal() {
        try {
            JSONArray array = new JSONArray(registros);
            FileOutputStream fos = openFileOutput("registros_offline.json", MODE_PRIVATE);
            fos.write(array.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Manejo de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permiso requerido para GPS", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }
}
