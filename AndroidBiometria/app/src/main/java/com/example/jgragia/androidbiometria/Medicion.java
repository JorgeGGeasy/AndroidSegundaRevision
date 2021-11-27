package com.example.jgragia.androidbiometria;

/*
// Nombre fichero: Medicion.java
// Fecha: 16/10/2021
// Autor: Jorge Grau Giannakakis
// Descripción: Objeto medicion
*/

public class Medicion {

    private double valor;
    private double latitud;
    private double longitud;
    private double fecha;
    private String tipo;

    public double getValor() {
        return valor;
    }

    public void setValor(int valor) {
        this.valor = valor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(int latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(int longitud) {
        this.longitud = longitud;
    }

    public double getFecha() {
        return fecha;
    }

    public void setFecha(double fecha) {
        this.fecha = fecha;
    }

    public Medicion(double valor, double latitud, double longitud, double fecha, String tipo) {
        this.valor = valor;
        this.latitud = latitud;
        this.longitud = longitud;
        this.fecha = fecha;
        this.tipo = tipo;
    }
}