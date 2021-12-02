package com.example.jgragia.androidbiometria;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
// Nombre fichero: Logica.java
// Fecha: 16/10/2021
// Autor: Jorge Grau Giannakakis
// Descripción: Es la logica que se conecta a la base de datos
*/

public class Logica {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String TAG = "Test";

    List<Double> array_CO = new ArrayList<Double>();
    List<Double> array_CO2 = new ArrayList<Double>();
    List<Double> array_O3 = new ArrayList<Double>();
    List<Double> array_Temperatura = new ArrayList<Double>();

    String resultado = "";

    // Sube la medicion a la base de datos en firebase
    // medicion: Medicion -> insertarMedicion() ->
    // @params La ultima medicion generada por este usuario
    public void insertarMedicion(Medicion medicion) {

        // Creamos un hashmap de la medicion que vamos a subir
        Map<String, Object> medicionASubir = new HashMap<>();
        medicionASubir.put("fecha", medicion.getFecha());
        medicionASubir.put("tipo", medicion.getTipo());
        medicionASubir.put("valor", medicion.getValor());
        medicionASubir.put("latitud", medicion.getLatitud());
        medicionASubir.put("longitud", medicion.getLongitud());
/*
        // Añadimos el dato
        db.collection("mediciones")
                .add(medicionASubir)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });

 */
    }

    public void recibirJornada(){

        double timestamp = (new Date().getTime());
        double haceUnaHora = timestamp - 864000000;

        // .whereGreaterThanOrEqualTo("fecha", haceUnaHora).whereLessThanOrEqualTo("fecha", timestamp)
        db.collection("mediciones").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {

                            List<DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();
                            for (DocumentSnapshot d : list) {

                                if(d.get("tipo").toString().equals("CO")){
                                    double valor = (double)d.getLong("valor");
                                    insertarEnSuArray(valor, array_CO);
                                }
                                else if(d.get("tipo") == "CO2"){
                                    double valor = (double)d.getLong("valor");
                                    insertarEnSuArray(valor, array_CO2);
                                }
                                else if(d.get("tipo") == "O3"){
                                    double valor = (double)d.getLong("valor");
                                    insertarEnSuArray(valor, array_O3);
                                }
                                else if(d.get("tipo") == "Temperatura"){
                                    double valor = (double)d.getLong("valor");
                                    insertarEnSuArray(valor, array_Temperatura);
                                }
                            }

                            int mediaCO = calcularMedia(array_CO);
                            int mediaCO2 = calcularMedia(array_CO2);
                            int mediaO3 = calcularMedia(array_O3);
                            int mediaTemperatura = calcularMedia(array_Temperatura);
                            resultado = comprobarLimites(mediaCO, mediaCO2, mediaO3, mediaTemperatura);
                            MainActivity.getInstance().actualizarJornada(mediaCO, mediaCO2, mediaO3, mediaTemperatura, resultado);
                        } else {

                            Log.e("Firebase", "Completamente vacio");
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    /**
     * Inserta los valores en un array que se usara posteriormente para calcular la media
     * @param {Medicion} mediciones Objeto con todos los datos de la base de datos.
     * @param {double[]} array El array con los valores de las medias
     */
    private void insertarEnSuArray(double mediciones, List<Double> array){
        array.add(mediciones);
    }

    /**
     * Calcula la media dado un array
     * @param {double[]} array El array con los valores de las medias
     */
    private int calcularMedia(List<Double> array){
        int media = 0;
        if (array.size() > 0){
            for(int i = 0; i < array.size(); i++){
                media += array.get(i);
            }
            media = media/array.size();
        }
        return media;
    }

    private String comprobarLimites(int valorCO, int valorCO2, int valorO3, int valorTemperatura){
        String resultado = "";

        // Aqui van los limites oficiales, si hay baremos tambien se pueden incluir introduciendo la cercania
        if (valorCO > 10){
            resultado += "Hay valores muy altos de CO en tu zona ";
        }
        if (valorCO2 > 30){
            resultado += "Hay valores altos de CO2 en tu zona ";
        }
        if (valorO3 > 180){
            resultado += "Hay valores muy altos de O3 en tu zona ";
        }
        if (valorTemperatura > 30){
            resultado += "Hay valores muy altos de Temperatura en tu zona ";
        }

        if (resultado.equals("")){
            resultado = "Tu calidad del aire es perfecta";
        }

        return resultado;
    }
}
