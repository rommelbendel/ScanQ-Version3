package com.rommelbendel.scanQ;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class KategorienViewModel extends AndroidViewModel {

    private final KategorienRepository kategorienRepository;
    private final LiveData<List<Kategorie>> alleKategorien;

    public KategorienViewModel(@NonNull Application application) {
        super(application);

        kategorienRepository = new KategorienRepository(application);
        alleKategorien = kategorienRepository.getAlleKategorien();
    }

    public LiveData<List<Kategorie>> getAlleKategorien() {
        return alleKategorien;
    }

    public LiveData<Integer> getNumberOfCategories() {
        return kategorienRepository.getNumberOfCategories();
    }

    public LiveData<List<Kategorie>> getCategoriesWithName(@NotNull final String searchName) {
        return kategorienRepository.getCategoriesWithName(searchName);
    }

    public void updateCategoryName(@NotNull String nameOld, @NotNull String nameNew) {
        kategorienRepository.updateCategoryName(nameOld, nameNew);
    }

    public void deleteWithName(@NotNull String name) {
        kategorienRepository.deleteWithName(name);
    }

    public void insertKategorie(@NotNull Kategorie kategorie) {
        kategorienRepository.insertKategorie(kategorie);
    }

    public void insertAlleKategorien(@NotNull Kategorie... kategorien) {
        kategorienRepository.insertAlleKategorien(kategorien);
    }
}
