package com.rommelbendel.scanQ;

import android.app.Application;

import androidx.lifecycle.LiveData;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VokabelRepository {

    private final VokabelnDao repoVokabelnDao;

    private LiveData<List<Vokabel>> repoAlleVokabeln;
    private LiveData<List<Vokabel>> repoMarkierteVokabeln;
    private LiveData<List<Vokabel>> repoKategorieVokabeln;
    private LiveData<Boolean> repoIsMarkiert;
    private LiveData<String> repoAntwortDE;
    private LiveData<String> repoAntwortENG;
    private LiveData<Integer> repoAnswered;
    private LiveData<Vokabel> repoAlreadyAnswered;
    private LiveData<Integer> repoId;

    VokabelRepository(@NotNull Application application) {
        Datenbank db = Datenbank.getDatenbank(application);
        repoVokabelnDao = db.vokabelnDao();

        repoAlleVokabeln = repoVokabelnDao.getAlle();
    }

    LiveData<List<Vokabel>> getAlleVokabeln() {
        return repoAlleVokabeln;
    }

    public LiveData<List<Vokabel>> getCategoryVocabs(String categoryName) {
        return repoVokabelnDao.getKategorieVokabeln(categoryName);
    }

    public LiveData<List<Vokabel>> getUntrainedCategoryVocabs(final String categoryName) {
        return repoVokabelnDao.getUntrainedCategoryVocabs(categoryName);
    }

    public LiveData<List<Vokabel>> getBadCategoryVocabs(final String categoryName) {
        return repoVokabelnDao.getBadCategoryVocabs(categoryName);
    }

    public LiveData<List<Vokabel>> getTrainedCategoryVocabs(final String categoryName) {
        return repoVokabelnDao.getTrainedCategoryVocabs(categoryName);
    }

    public LiveData<List<Vokabel>> getUntrainedVocabs() {
        return repoVokabelnDao.getUntrainedVocabs();
    }

    public LiveData<List<Vokabel>> getBadVocabs() {
        return repoVokabelnDao.getBadVocabs();
    }

    public LiveData<List<Vokabel>> getTrainedVocabs() {
        return repoVokabelnDao.getTrainedVocabs();
    }

    /*
    LiveData<List<Vokabel>> getAlleMarkiertenVokabeln(boolean markiert) {
        return repoVokabelnDao.getMarkierte(markiert);
    }

    LiveData<List<Vokabel>> getAlleKategorieVokabeln(String kategorie) {
        return repoVokabelnDao.getKategorieVokabeln(kategorie);
    }

    LiveData<Boolean> isVokabelMarkiert(String de) {
        return repoVokabelnDao.isMarkiert(de);
    }

    LiveData<String> getAntwortTextDE(String vokabelENG) {
        return repoVokabelnDao.getAntwortDE(vokabelENG);
    }

    LiveData<String> getAntwortTextENG(String vokabelDE) {
        return repoVokabelnDao.getAntwortENG(vokabelDE);
    }

    LiveData<Integer> getAnsweredCount(int id) {
        return repoVokabelnDao.getAnswered(id);
    }

    LiveData<List<Vokabel>> getAlreadyAnsweredVokabeln(int answers) {
        return repoVokabelnDao.getAlreadyAnswered(answers);
    }
    */

    void changeMarking(final String german, final boolean marked) {
        Datenbank.databaseWriteExecutor.execute(() -> repoVokabelnDao.updateMarkierung(german, marked));
    }

    void updateVokabelENG(final String oldENG, final String newENG) {
        Datenbank.databaseWriteExecutor.execute(() -> repoVokabelnDao.updateVokabelENG(oldENG, newENG));
    }

    void updateVokabelDE(final String oldDE, final String newDE) {
        Datenbank.databaseWriteExecutor.execute(() -> repoVokabelnDao.updateVokabelDE(oldDE, newDE));
    }

    void updateAnswered(final int id, final int answered) {
        Datenbank.databaseWriteExecutor.execute(() -> repoVokabelnDao.updateAnswered(id, answered));
    }

    void updateCountRightAnswers(final int id, final int countRight) {
        Datenbank.databaseWriteExecutor.execute(() -> repoVokabelnDao.updateRichtige(id, countRight));
    }

    void updateCountWrongAnswers(final int id, final int countWrong) {
        Datenbank.databaseWriteExecutor.execute(() -> repoVokabelnDao.updateFalsche(id, countWrong));
    }

    void updateCategory(final String oldCategory, final String newCategory) {
        Datenbank.databaseWriteExecutor.execute(() ->
                repoVokabelnDao.updateCategory(oldCategory, newCategory));
    }

    void insertAlleVokabeln(final Vokabel... vokabeln) {
        Datenbank.databaseWriteExecutor.execute(() -> repoVokabelnDao.insertAlleVokabeln(vokabeln));
    }

    void insertVokabel(final Vokabel vokabel) {
        Datenbank.databaseWriteExecutor.execute(() -> repoVokabelnDao.insertVokabel(vokabel));
    }

    void deleteVokabelWithDE(final String deutsch) {
        Datenbank.databaseWriteExecutor.execute(() -> repoVokabelnDao.deleteVokabelWithDE(deutsch));
    }

    void deleteVokabelWithENG(final String englisch) {
        Datenbank.databaseWriteExecutor.execute(() -> repoVokabelnDao.deleteVokabelWithENG(englisch));
    }

    void deleteVokabel(final Vokabel vokabel) {
        Datenbank.databaseWriteExecutor.execute(() -> repoVokabelnDao.deleatVokabel(vokabel));
    }

    void deleteCategory(final String categoryName) {
        Datenbank.databaseWriteExecutor.execute(() -> repoVokabelnDao.deleteKategorie(categoryName));
    }

}
