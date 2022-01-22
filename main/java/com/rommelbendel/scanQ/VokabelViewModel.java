package com.rommelbendel.scanQ;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import org.jetbrains.annotations.NotNull;

import java.util.List;


public class VokabelViewModel extends AndroidViewModel {

    private VokabelRepository vokabelRepository;

    private LiveData<List<Vokabel>> alleVokabeln;

    public VokabelViewModel(@NotNull Application application) {
        super(application);
        vokabelRepository = new VokabelRepository(application);
        alleVokabeln = vokabelRepository.getAlleVokabeln();
    }

    public LiveData<List<Vokabel>> getAlleVokabeln() {
        return alleVokabeln;
    }

    public LiveData<List<Vokabel>> getCategoryVocabs(@NotNull String categoryName) {
        return vokabelRepository.getCategoryVocabs(categoryName);
    }

    public LiveData<List<Vokabel>> getUntrainedCategoryVocabs(@NotNull final String categoryName) {
        return vokabelRepository.getUntrainedCategoryVocabs(categoryName);
    }

    public LiveData<List<Vokabel>> getBadCategoryVocabs(@NotNull final String categoryName) {
        return vokabelRepository.getBadCategoryVocabs(categoryName);
    }

    public LiveData<List<Vokabel>> getTrainedCategoryVocabs(@NotNull final String categoryName) {
        return vokabelRepository.getTrainedCategoryVocabs(categoryName);
    }

    public LiveData<List<Vokabel>> getUntrainedVocabs() {
        return vokabelRepository.getUntrainedVocabs();
    }

    public LiveData<List<Vokabel>> getTrainedVocabs() {
        return vokabelRepository.getTrainedVocabs();
    }

    public LiveData<List<Vokabel>> getBadVocabs() {
        return vokabelRepository.getBadVocabs();
    }

    public void insertVokabel(@NotNull Vokabel vokabel) {
        vokabelRepository.insertVokabel(vokabel);
    }

    public void insertAlleVokabeln(@NotNull Vokabel... vokabeln) {
        vokabelRepository.insertAlleVokabeln(vokabeln);
    }

    public void updateMarking(@NotNull String german, boolean marked) {
        vokabelRepository.changeMarking(german, marked);
    }

    public void updateVokabelENG(@NotNull String oldENG, @NotNull String newENG) {
        vokabelRepository.updateVokabelENG(oldENG, newENG);
    }

    public void updateVokabelDE(@NotNull String oldDE, @NotNull String newDE) {
        vokabelRepository.updateVokabelDE(oldDE, newDE);
    }

    public void updateAnswered(final int id, final int answered) {
        vokabelRepository.updateAnswered(id, answered);
    }

    public void updateCountRightAnswers(final int id, final int countRight) {
        vokabelRepository.updateCountRightAnswers(id, countRight);
    }

    public void updateCountWrongAnswers(final int id, final int countWrong) {
        vokabelRepository.updateCountWrongAnswers(id, countWrong);
    }

    public void updateCategory(@NotNull final String oldCategory,
                               @NotNull final String newCategory) {
        vokabelRepository.updateCategory(oldCategory, newCategory);
    }

    public void deleteVokabelWithDE(@NotNull String deutsch) {
        vokabelRepository.deleteVokabelWithDE(deutsch);
    }

    public void deleteVokabelWithENG(@NotNull String englisch) {
        vokabelRepository.deleteVokabelWithENG(englisch);
    }

    public void deleteVokabel(@NotNull Vokabel vokabel) {
        vokabelRepository.deleteVokabel(vokabel);
    }

    public void deleteCategory(@NotNull final String categoryName) {
        vokabelRepository.deleteCategory(categoryName);
    }
}
