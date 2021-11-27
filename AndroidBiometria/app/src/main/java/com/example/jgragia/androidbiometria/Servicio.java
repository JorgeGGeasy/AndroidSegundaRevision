package com.example.jgragia.androidbiometria;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/*
// Nombre fichero: Servicio.java
// Fecha: 16/10/2021
// Autor: Jorge Grau Giannakakis
// Descripción: Esta actividad se encarga de escuchar beacons continuamente
*/
public class Servicio  extends IntentService {

    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    private static final String ETIQUETA_LOG = "Test";

    private long tiempoDeEspera = 10000;

    private boolean seguir = true;
    private String dispositivoEscuchando;
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    private BluetoothLeScanner elEscanner;
    private ScanCallback callbackDelEscaneo = null;
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------

    public Servicio(  ) {
        super("HelloIntentService");
        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.constructor: termina");
    }

    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------

    // Detiene el servicio en segundo plano
    // -> parar() ->
    public void parar () {
        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.parar() " );
        if ( this.seguir == false ) {
            return;
        }
        this.seguir = false;
        this.stopSelf();
        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.parar() : acaba " );
    }

    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    public void onDestroy() {

        Log.d(ETIQUETA_LOG, " ServicioEscucharBeacons.onDestroy() " );

        this.detenerBusquedaDispositivosBTLE();

        this.parar(); // posiblemente no haga falta, si stopService() ya se carga el servicio
    }

    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onHandleIntent(Intent intent) {

        inicializarServicio(intent);

        try {

            while ( this.seguir ) {
                Thread.sleep(tiempoDeEspera);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    } //()

    // Inicializa el servicio, busca automaticamente el dispositivo llamado Jorge Grau Giannakakis, GTI-3A
    // intent: Texto -> inicializarServicio() ->
    // @params Recoge los ajustes de busqueda del dispositivo
    Logica logica = new Logica();
    private void inicializarServicio(Intent intent) {
        this.tiempoDeEspera = intent.getLongExtra("tiempoDeEspera", /* default */ 50000);
        this.seguir = true;
        this.dispositivoEscuchando = intent.getStringExtra("nombreDelDispositivo");
        Log.d(ETIQUETA_LOG, " dispositivoEscuchando=" + dispositivoEscuchando );
        inicializarBlueTooth();
        buscarEsteDispositivoBTLE(dispositivoEscuchando);
    } //()

    // Inicializa el bluetooth
    // -> inicializarBlueTooth() ->
    private void inicializarBlueTooth() {
        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): obtenemos adaptador BT ");

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): habilitamos adaptador BT ");

        bta.enable();

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): habilitado =  " + bta.isEnabled() );

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): estado =  " + bta.getState() );

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): obtenemos escaner btle ");

        this.elEscanner = bta.getBluetoothLeScanner();

        if ( this.elEscanner == null ) {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): Socorro: NO hemos obtenido escaner btle  !!!!");

        }

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): voy a perdir permisos (si no los tuviera) !!!!");


    } // ()

    // Busca el dispositivo llamado Jorge Grau Giannakakis, GTI-3A
    // dispositivoBuscado: Texto -> buscarEsteDispositivoBTLE() ->
    // @params El nombre del dispositivo que buscar
    private void buscarEsteDispositivoBTLE(final String dispositivoBuscado ) {
        Log.d(ETIQUETA_LOG, " buscarEsteDispositivoBTLE(): empieza ");

        Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): instalamos scan callback ");

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult( int callbackType, ScanResult resultado ) {
                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): onScanResult() ");

                byte[] bytes = resultado.getScanRecord().getBytes();

                TramaIBeacon tib = new TramaIBeacon(bytes);

                // Si lo hemos encontrado y su UUID es -EPSG-GTICOYCO2- recibiremos en el major el valor del CO y en el minor el CO2
                if (Utilidades.bytesToString(tib.getUUID()).equals("-EPSG-GTICOYCO2-")){
                    //Creamos un objeto calendar que guardara la fecha
                    Calendar calendar = Calendar.getInstance();
                    double date = calendar.getTimeInMillis();

                    // Instanciamos las variables que usaremos para recopilar los datos
                    float valorCO;
                    float valorCO2;

                    String texto;

                    // Deconstruimos el major
                    texto = Integer.toString(Utilidades.bytesToInt(tib.getMajor()));
                    valorCO = Float.parseFloat(texto);

                    // Deconstruimos el minor
                    texto = Integer.toString(Utilidades.bytesToInt(tib.getMinor()));
                    valorCO2 = Float.parseFloat(texto);

                    Medicion medicionCO2 = new Medicion(valorCO2, MainActivity.getLatitud(), MainActivity.getLongitud(), date, "CO");
                    Medicion medicionCO = new Medicion(valorCO, MainActivity.getLatitud(), MainActivity.getLongitud(), date, "CO2");

                    logica.insertarMedicion(medicionCO2);
                    logica.insertarMedicion(medicionCO);

                    // Las añadimos a MainActivity
                    MainActivity.getInstance().actualizarUltimaMedicionCOCO2(medicionCO, medicionCO2);

                }
                // Si lo hemos encontrado y su UUID es EPSG-GTIO3YTEMP recibiremos en el major el valor del O3 y en el minor la Temperatura
                if (Utilidades.bytesToString(tib.getUUID()).equals("EPSG-GTIO3YTEMP-")){
                    //Creamos un objeto calendar que guardara la fecha
                    Calendar calendar = Calendar.getInstance();
                    double date = calendar.getTimeInMillis();

                    // Instanciamos las variables que usaremos para recopilar los datos
                    float valorO3;
                    float valorTemperatura;

                    String texto;

                    // Deconstruimos el major
                    texto = Integer.toString(Utilidades.bytesToInt(tib.getMajor()));
                    valorO3 = Float.parseFloat(texto);

                    // Deconstruimos el minor
                    texto = Integer.toString(Utilidades.bytesToInt(tib.getMinor()));
                    valorTemperatura = Float.parseFloat(texto);

                    Medicion medicionO3 = new Medicion(valorO3, MainActivity.getLatitud(), MainActivity.getLongitud(), date, "O3");
                    Medicion medicionTemperatura = new Medicion(valorTemperatura, MainActivity.getLatitud(), MainActivity.getLongitud(), date, "Temperatura");

                    logica.insertarMedicion(medicionO3);
                    logica.insertarMedicion(medicionTemperatura);

                    // Las añadimos a MainActivity
                    MainActivity.getInstance().actualizarUltimaMedicionO3Temperatura(medicionO3, medicionTemperatura);

                }

            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): onBatchScanResults() ");

            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): onScanFailed() ");

            }
        };

        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter sf = new ScanFilter.Builder().setDeviceName( dispositivoBuscado ).build();
        filters.add(sf);


        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): empezamos a escanear buscando: " + dispositivoBuscado );
        //Log.d(ETIQUETA_LOG, "  buscarEsteDispositivoBTLE(): empezamos a escanear buscando: " + dispositivoBuscado
        //      + " -> " + Utilidades.stringToUUID( dispositivoBuscado ) );

        this.elEscanner.startScan(filters,settings,this.callbackDelEscaneo);
    } // ()

    // Detiene la busqueda del dispositivo
    // -> detenerBusquedaDispositivosBTLE() ->
    private void detenerBusquedaDispositivosBTLE() {

        if ( this.callbackDelEscaneo == null ) {
            return;
        }
        this.elEscanner.stopScan( this.callbackDelEscaneo );
        this.callbackDelEscaneo = null;

    } // ()
} // class
// -------------------------------------------------------------------------------------------------
// -------------------------------------------------------------------------------------------------