/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.javawebserver;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author stei2
 */
public class PuntiVendita {
    private int size;
    private List<PuntoVendita> listaRisultati;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<PuntoVendita> getListaRisultati() {
        return listaRisultati;
    }

    public void setListaRisultati(List<PuntoVendita> listaRisultati) {
        this.listaRisultati = listaRisultati;
    }
}
