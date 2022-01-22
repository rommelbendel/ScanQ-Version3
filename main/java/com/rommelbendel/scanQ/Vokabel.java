package com.rommelbendel.scanQ;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "vokabeln")
public class Vokabel {
    //Definition der Tabelle
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "vokabelENG")
    private String vokabelENG;

    @ColumnInfo(name = "vokabelDE")
    private String vokabelDE;

    @ColumnInfo(name = "richtig")
    private int richtig;

    @ColumnInfo(name = "falsch")
    private int falsch;

    @ColumnInfo(name = "answered")
    private int answered;

    @ColumnInfo(name = "kategorie")
    private String kategorie;

    @ColumnInfo(name = "markiert")
    private boolean markiert;

    //Konstruktoren
    public Vokabel(String vokabelENG, String vokabelDE, String kategorie) {
        this.vokabelENG = vokabelENG;
        this.vokabelDE = vokabelDE;
        this.richtig = 0;
        this.falsch = 0;
        this.answered = 0;
        this.kategorie = kategorie;
        this.markiert = false;
    }

    @Ignore
    public Vokabel(int id, String vokabelENG, String vokabelDE, String kategorie) {
        this.id = id;
        this.vokabelENG = vokabelENG;
        this.vokabelDE = vokabelDE;
        this.richtig = 0;
        this.falsch = 0;
        this.answered = 0;
        this.kategorie = kategorie;
        this.markiert = false;
    }


    //get Methoden
    public int getId() {
        return id;
    }
    public String getVokabelENG() {
        return vokabelENG;
    }
    public String getVokabelDE() {
        return vokabelDE;
    }
    public int getRichtig() {
        return richtig;
    }
    public int getFalsch() {
        return falsch;
    }
    public int getAnswered() {
        return answered;
    }
    public String getKategorie() {
        return kategorie;
    }
    public boolean isMarkiert() {
        return markiert;
    }

    //set Methoden
    public void setId(int id) {
        this.id = id;
    }
    public void setVokabelENG(String vokabelENG) {
        this.vokabelENG = vokabelENG;
    }
    public void setVokabelDE(String vokabelDE) {
        this.vokabelDE = vokabelDE;
    }
    public void setRichtig(int richtig) {
        this.richtig = richtig;
    }
    public void setFalsch(int falsch) {
        this.falsch = falsch;
    }
    public void setAnswered(int answered) {
        this.answered = answered;
    }
    public void setKategorie(String kategorie) {
        this.kategorie = kategorie;
    }
    public void setMarkiert(boolean markiert) {
        this.markiert = markiert;
    }
}
