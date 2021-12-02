package com.example.jgragia.androidbiometria;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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

    private NotificationManagerCompat notificationManager;
    public static final String CANAL1 = "Alertas";
    public static final String CANAL2 = "Segundo plano";
    public static final String CANAL3 = "Alertas nodo";
    private boolean alertaNodo = false;
    private int contador = 0;
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    private static final String ETIQUETA_LOG = "Dispositivo";

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
        notificationManager.cancel(1);
        notificationManager.cancel(2);
        notificationManager.cancel(3);
        notificationManager.cancel(4);

        this.parar(); // posiblemente no haga falta, si stopService() ya se carga el servicio
    }

    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onHandleIntent(Intent intent) {

        inicializarServicio(intent);
        notificationManager = NotificationManagerCompat.from(this);
        // Se crean los canales de notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal1 = new NotificationChannel(
                    CANAL1,
                    "Canal alertas",
                    NotificationManager.IMPORTANCE_HIGH
            );
            canal1.setDescription("En este canal se muestran las alertas que recibe el dispostivo");

            NotificationChannel canal2 = new NotificationChannel(
                    CANAL2,
                    "Canal aviso segundo plano",
                    NotificationManager.IMPORTANCE_LOW
            );
            canal2.setDescription("Este canal se usa para mostrar si el servicio esta activo o no");

            NotificationChannel canal3 = new NotificationChannel(
                    CANAL3,
                    "Canal aviso estado del nodo",
                    NotificationManager.IMPORTANCE_HIGH
            );
            canal3.setDescription("Este canal se usa para gestionar los mensajes del nodo");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(canal1);
            manager.createNotificationChannel(canal2);
            manager.createNotificationChannel(canal3);
        }

        Notification notification = new NotificationCompat.Builder(this, CANAL2)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Servicio en segundo plano")
                .setContentText("Servicio en segundo plano activo")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        notificationManager.notify(2, notification);

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

                mostrarInformacionDispositivoBTLE( resultado );

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

                    // Aqui van los limites oficiales, si hay baremos tambien se pueden incluir introduciendo la cercania
                    if (valorCO > 10){
                        alertaValor("CO", "por encima");
                    }
                    else if (valorCO > 8){
                        alertaValor("CO", "muy cerca");
                    }

                    if (valorCO2 > 30){
                        alertaValor("CO2", "por encima");
                    }
                    else if (valorCO2 > 28){
                        alertaValor("CO2", "muy cerca");
                    }
                    else if (valorCO2 > 25){
                        alertaValor("CO2", "cerca");
                    }

                    // Aqui las lecturas erroneas, si hay alguna no guardamos la medida
                    if (valorCO < 0 || valorCO2 < 0){
                        alertaNodoLecturaErronea();
                        return;
                    }

                    Medicion medicionCO2 = new Medicion(valorCO2, MainActivity.getLatitud(), MainActivity.getLongitud(), date, "CO");
                    Medicion medicionCO = new Medicion(valorCO, MainActivity.getLatitud(), MainActivity.getLongitud(), date, "CO2");

                    logica.insertarMedicion(medicionCO2);
                    logica.insertarMedicion(medicionCO);

                    // Las añadimos a MainActivity
                    MainActivity.getInstance().actualizarUltimaMedicionCOCO2(medicionCO, medicionCO2);

                    // Distancia del RSSI
                    int rssi = resultado.getRssi();
                    calcularDistancia(rssi);

                    contador = 0;
                    alertaNodo = false;
                    notificationManager.cancel(3);
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

                    // Aqui van los limites oficiales, si hay baremos tambien se pueden incluir introduciendo la cercania
                    if (valorO3 > 180){
                        alertaValor("O3", "muy por encima");
                    }
                    else if (valorO3 > 120){
                        alertaValor("O3", "por encima");
                    }
                    else if (valorO3 > 100){
                        alertaValor("O3", "cerca");
                    }

                    if (valorTemperatura > 30){
                        alertaValor("Temperatura", "cerca");
                    }

                    // Aqui las lecturas erroneas
                    if (valorO3 < 0 || valorTemperatura < 0){
                        alertaNodoLecturaErronea();
                        return;
                    }


                    Medicion medicionO3 = new Medicion(valorO3, MainActivity.getLatitud(), MainActivity.getLongitud(), date, "O3");
                    Medicion medicionTemperatura = new Medicion(valorTemperatura, MainActivity.getLatitud(), MainActivity.getLongitud(), date, "Temperatura");

                    logica.insertarMedicion(medicionO3);
                    logica.insertarMedicion(medicionTemperatura);

                    // Las añadimos a MainActivity
                    MainActivity.getInstance().actualizarUltimaMedicionO3Temperatura(medicionO3, medicionTemperatura);

                    // Distancia del RSSI
                    int rssi = resultado.getRssi();
                    calcularDistancia(rssi);
                    contador = 0;
                    alertaNodo = false;
                    notificationManager.cancel(3);
                }
                else {
                    // Si no hemos recibido beacons de nuestro sensor en 1 minuto aparece una alerta
                    contador +=1;
                    if (contador > 60 && alertaNodo == false){
                        alertaNodo();
                    }
                }
            }

            public void calcularDistancia(int rssi){
                Log.e("Test", rssi +"");
                if (rssi >= -55){
                    MainActivity.getInstance().actualizarRSSI("Cerca");
                }
                else if (rssi > -70){
                    MainActivity.getInstance().actualizarRSSI("Proximo");
                }
                else if (rssi >= -70){
                    MainActivity.getInstance().actualizarRSSI("Lejos");
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

    private void alertaValor(String tipo, String problema){

        Notification notification = new NotificationCompat.Builder(this, CANAL1)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Alerta " + tipo)
                .setContentText("El gas " + tipo + " esta " + problema + " de los limites oficiales")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();

        notificationManager.notify(1, notification);
/*
        NotificationCompat.Builder notificacion = new NotificationCompat.Builder(this, CANAL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Alerta, el gas " + tipo + " ha superado el limite")
                        .setContentText("El gas " + tipo + " esta " + problema + " de las medidas oficiales")
                        .setPriority(NotificationCompat.PRIORITY_HIGH);
        PendingIntent intencionPendiente = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class), 0);
        notificacion.setContentIntent(intencionPendiente);
        notificationManager.notify(NOTIFICACION_ID, notificacion.build());

 */
    }

    private void alertaNodo(){

        Notification notification = new NotificationCompat.Builder(this, CANAL3)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Alerta nodo")
                .setContentText("El nodo parece estar desconectado o fuera del alcance ")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();

        notificationManager.notify(3, notification);
        alertaNodo = true;
    }

    private void alertaNodoLecturaErronea(){
        Notification notification = new NotificationCompat.Builder(this, CANAL3)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Alerta nodo")
                .setContentText("El nodo parece tener problemas, lectura erronea")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();
        notificationManager.notify(4, notification);
    }

    private void mostrarInformacionDispositivoBTLE( ScanResult resultado ) {

        BluetoothDevice bluetoothDevice = resultado.getDevice();
        byte[] bytes = resultado.getScanRecord().getBytes();
        int rssi = resultado.getRssi();

        Log.d(ETIQUETA_LOG, " ****************************************************");
        Log.d(ETIQUETA_LOG, " ****** DISPOSITIVO DETECTADO BTLE ****************** ");
        Log.d(ETIQUETA_LOG, " ****************************************************");
        Log.d(ETIQUETA_LOG, " nombre = " + bluetoothDevice.getName());
        Log.d(ETIQUETA_LOG, " toString = " + bluetoothDevice.toString());

        Log.d(ETIQUETA_LOG, " dirección = " + bluetoothDevice.getAddress());
        Log.d(ETIQUETA_LOG, " rssi = " + rssi );

        Log.d(ETIQUETA_LOG, " bytes = " + new String(bytes));
        Log.d(ETIQUETA_LOG, " bytes (" + bytes.length + ") = " + Utilidades.bytesToHexString(bytes));

        TramaIBeacon tib = new TramaIBeacon(bytes);

        Log.d(ETIQUETA_LOG, " ----------------------------------------------------");
        Log.d(ETIQUETA_LOG, " prefijo  = " + Utilidades.bytesToHexString(tib.getPrefijo()));
        Log.d(ETIQUETA_LOG, "          advFlags = " + Utilidades.bytesToHexString(tib.getAdvFlags()));
        Log.d(ETIQUETA_LOG, "          advHeader = " + Utilidades.bytesToHexString(tib.getAdvHeader()));
        Log.d(ETIQUETA_LOG, "          companyID = " + Utilidades.bytesToHexString(tib.getCompanyID()));
        Log.d(ETIQUETA_LOG, "          iBeacon type = " + Integer.toHexString(tib.getiBeaconType()));
        Log.d(ETIQUETA_LOG, "          iBeacon length 0x = " + Integer.toHexString(tib.getiBeaconLength()) + " ( "
                + tib.getiBeaconLength() + " ) ");
        Log.d(ETIQUETA_LOG, " uuid  = " + Utilidades.bytesToHexString(tib.getUUID()));
        Log.d(ETIQUETA_LOG, " uuid  = " + Utilidades.bytesToString(tib.getUUID()));
        Log.d(ETIQUETA_LOG, " major  = " + Utilidades.bytesToHexString(tib.getMajor()) + "( "
                + Utilidades.bytesToInt(tib.getMajor()) + " ) ");
        Log.d(ETIQUETA_LOG, " minor  = " + Utilidades.bytesToHexString(tib.getMinor()) + "( "
                + Utilidades.bytesToInt(tib.getMinor()) + " ) ");
        Log.d(ETIQUETA_LOG, " txPower  = " + Integer.toHexString(tib.getTxPower()) + " ( " + tib.getTxPower() + " )");
        Log.d(ETIQUETA_LOG, " ****************************************************");

    } // ()
} // class
// -------------------------------------------------------------------------------------------------
// -------------------------------------------------------------------------------------------------