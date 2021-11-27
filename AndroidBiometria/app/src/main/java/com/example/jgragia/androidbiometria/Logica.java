package com.example.jgragia.androidbiometria;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
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
}
