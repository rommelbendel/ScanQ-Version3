package com.rommelbendel.scanQ;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface KategorienDao {

    @Query("SELECT * FROM KATEGORIEN")
    LiveData<List<Kategorie>> getAlleKategorien();

    @Query("SELECT * FROM KATEGORIEN WHERE name = :searchName")
    LiveData<List<Kategorie>> getCategoriesWithName(String searchName);

    @Query("SELECT COUNT(name) FROM KATEGORIEN")
    LiveData<Integer> getNumberOfCategories();

    @Query("UPDATE KATEGORIEN SET name = :nameNew WHERE name = :nameOld")
    void updateKategorieName(String nameOld, String nameNew);

    @Query("DELETE FROM KATEGORIEN WHERE name = :name")
    void deleteWithName(String name);

    @Insert
    void insertKategorie(Kategorie kategorie);

    @Insert
    void insertAlleKategorien(Kategorie... kategorien);
}
