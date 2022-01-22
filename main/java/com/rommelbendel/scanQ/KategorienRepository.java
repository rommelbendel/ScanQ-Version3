package com.rommelbendel.scanQ;

import android.app.Application;

import androidx.lifecycle.LiveData;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class KategorienRepository {

    private final KategorienDao kategorienDao;

    private LiveData<List<Kategorie>> alleKategorien;

    KategorienRepository(@NotNull Application application) {
        Datenbank db = Datenbank.getDatenbank(application);

        kategorienDao = db.kategorienDao();
        alleKategorien = kategorienDao.getAlleKategorien();
    }

    public LiveData<List<Kategorie>> getAlleKategorien() {
        return alleKategorien;
    }

    public LiveData<List<Kategorie>> getCategoriesWithName(final String searchName) {
        return kategorienDao.getCategoriesWithName(searchName);
    }

    public LiveData<Integer> getNumberOfCategories() {
        return kategorienDao.getNumberOfCategories();
    }

    public void updateCategoryName(final String nameOld, final String nameNew) {
        Datenbank.databaseWriteExecutor.execute(new Runnable() {
            @Override
            public void run() {
                kategorienDao.updateKategorieName(nameOld, nameNew);
            }
        });
    }

    public void deleteWithName(final String name) {
        Datenbank.databaseWriteExecutor.execute(new Runnable() {
            @Override
            public void run() {
                kategorienDao.deleteWithName(name);
            }
        });
    }

    public void insertKategorie(final Kategorie kategorie) {
        Datenbank.databaseWriteExecutor.execute(new Runnable() {
            @Override
            public void run() {
                kategorienDao.insertKategorie(kategorie);
            }
        });
    }

    public void insertAlleKategorien(final Kategorie... kategorien) {
        Datenbank.databaseWriteExecutor.execute(new Runnable() {
            @Override
            public void run() {
                kategorienDao.insertAlleKategorien(kategorien);
            }
        });
    }
}
