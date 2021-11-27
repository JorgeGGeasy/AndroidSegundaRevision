package com.example.jgragia.androidbiometria;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/*
// Nombre fichero: MainActivity.java
// Fecha: 16/10/2021
// Autor: Jorge Grau Giannakakis
// Descripción: Es la actividad principal de la aplicación
*/

public class MainActivity extends AppCompatActivity implements LocationListener {

    // --------------------------------------------------------------
    // --------------------------------------------------------------

    private static final int CODIGO_PETICION_PERMISOS = 11223344;

    private static MainActivity instancia;
    private TextView ultimaMedidaTextoCO;
    private TextView ultimaMedidaTextoCO2;
    private TextView ultimaMedidaTextoO3;
    private TextView ultimaMedidaTextoTemperatura;
    private Button arrancar;
    private Button detener;


    protected LocationManager locationManager;

    private static double latitud;
    private static double longitud;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializamos los permisos
        inicializarPermisosBluetooth();

        // Recogemos todos los elementos de la vista
        ultimaMedidaTextoCO = findViewById(R.id.ultimaMedicionCO);
        ultimaMedidaTextoCO2 = findViewById(R.id.ultimaMedicionCO2);
        ultimaMedidaTextoO3 = findViewById(R.id.ultimaMedicionO3);
        ultimaMedidaTextoTemperatura = findViewById(R.id.ultimaMedicionTemperatura);

        detener = findViewById(R.id.detener);
        detener.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                detenerServicio();
            }
        });
        detener.setBackgroundColor(0xFFE60505);

        arrancar = findViewById(R.id.arrancar);
        arrancar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                arrancarServicio();
            }
        });
        arrancar.setClickable(false);
        arrancar.setBackgroundColor(0xFF1F7004);

        // Arrancamos el servicio
        startService(new Intent(MainActivity.this, Servicio.class));
        instancia = this;

        // Arrancamos la localización
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    // Devuelve una instancia de MainActivity
    // -> getInstance() -> instancia : MainActivity
    // @returns La instancia de MainActivity
    public static MainActivity getInstance() {
        return instancia;
    }

    // Devuelve el valor de latitud
    // -> getLatitud() -> latitud : double
    // @returns La coordenada de la latitud
    public static double getLatitud() {
        return latitud;
    }

    // Devuelve el valor de longitud
    // -> getLongitud() -> longitud : double
    // @returns La coordenada de la longitud
    public static double getLongitud() {
        return longitud;
    }

    // Actualiza la medicion que se muestra en pantalla
    // medicionCO : Medicion, medicionCO2 : Medicion -> actualizarUltimaMedicionCOCO2() ->
    // @params La ultima medicion generada por este usuario
    public void actualizarUltimaMedicionCOCO2(Medicion medicionCO, Medicion medicionCO2){
        ultimaMedidaTextoCO.setText(Double.toString(medicionCO.getValor()));
        ultimaMedidaTextoCO2.setText(Double.toString(medicionCO2.getValor()));
    }

    // Actualiza la medicion que se muestra en pantalla
    // medicionO3 : Medicion, medicionTemp : Medicion -> actualizarUltimaMedicionO3Temperatura() ->
    // @params La ultima medicion generada por este usuario
    public void actualizarUltimaMedicionO3Temperatura(Medicion medicionO3,Medicion medicionTemp){
        ultimaMedidaTextoO3.setText(Double.toString(medicionO3.getValor()));
        ultimaMedidaTextoTemperatura.setText(Double.toString(medicionTemp.getValor()));
    }


    // Detiene el servicio en segundo plano
    // -> detenerServicio() ->
    public void detenerServicio(){
        stopService(new Intent(MainActivity.this, Servicio.class));
        // Desactiva el boton detener y los cambia de color
        arrancar.setClickable(true);
        detener.setClickable(false);
        arrancar.setBackgroundColor(0xFF42ED09);
        detener.setBackgroundColor(0xFF700404);
    }

    // Enciende el servivicio en segundo plano
    // -> arrancarServicio() ->
    public void arrancarServicio(){
        startService(new Intent(MainActivity.this, Servicio.class));
        // Desactiva el boton arrancar y los cambia de color
        arrancar.setClickable(false);
        detener.setClickable(true);
        arrancar.setBackgroundColor(0xFF1F7004);
        detener.setBackgroundColor(0xFFE60505);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("Test","Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
        latitud = location.getLatitude();
        longitud = location.getLongitude();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }


    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private void inicializarPermisosBluetooth() {
        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION},
                    CODIGO_PETICION_PERMISOS);
        } else {
            Log.d("Test", " inicializarBlueTooth(): parece que YA tengo los permisos necesarios !!!!");

        }
    }// ()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults);

        switch (requestCode) {
            case CODIGO_PETICION_PERMISOS:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d("Test", " onRequestPermissionResult(): permisos concedidos  !!!!");
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                }  else {

                    Log.d("Test", " onRequestPermissionResult(): Socorro: permisos NO concedidos  !!!!");

                }
                return;
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    } // ()

}