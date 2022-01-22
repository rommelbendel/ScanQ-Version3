package com.rommelbendel.scanQ;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface VokabelnDao {
    @Query("SELECT * FROM VOKABELN")
    LiveData<List<Vokabel>> getAlle();

    @Query("SELECT * FROM VOKABELN WHERE markiert = :markiert")
    LiveData<List<Vokabel>> getMarkierte(boolean markiert);

    @Query("SELECT * FROM VOKABELN WHERE kategorie = :kategorie")
    LiveData<List<Vokabel>> getKategorieVokabeln(String kategorie);

    @Query("SELECT * FROM VOKABELN WHERE kategorie = :kategorie AND answered = 0")
    LiveData<List<Vokabel>> getUntrainedCategoryVocabs(String kategorie);

    @Query("SELECT * FROM VOKABELN WHERE  kategorie = :kategorie AND richtig < falsch")
    LiveData<List<Vokabel>> getBadCategoryVocabs(String kategorie);

    @Query("SELECT * FROM VOKABELN WHERE  kategorie = :kategorie AND richtig > falsch")
    LiveData<List<Vokabel>> getTrainedCategoryVocabs(String kategorie);

    @Query("SELECT * FROM VOKABELN WHERE answered = 0")
    LiveData<List<Vokabel>> getUntrainedVocabs();

    @Query("SELECT * FROM VOKABELN WHERE richtig < falsch")
    LiveData<List<Vokabel>> getBadVocabs();

    @Query("SELECT * FROM VOKABELN WHERE richtig > falsch")
    LiveData<List<Vokabel>> getTrainedVocabs();

    @Query("SELECT markiert FROM VOKABELN WHERE vokabelDE = :de")
    LiveData<Boolean> isMarkiert(String de);

    @Query("SELECT vokabelDE FROM VOKABELN WHERE vokabelENG = :vokabelENG LIMIT 1")
    LiveData<String> getAntwortDE(String vokabelENG);

    @Query("SELECT vokabelENG FROM VOKABELN WHERE vokabelDE = :vokabelDE LIMIT 1")
    LiveData<String> getAntwortENG(String vokabelDE);

    @Query("SELECT answered FROM VOKABELN WHERE id = :id")
    LiveData<Integer> getAnswered(int id);

    @Query("SELECT * FROM VOKABELN WHERE answered = :answered")
    LiveData<List<Vokabel>> getAlreadyAnswered(int answered);

    @Query("SELECT id FROM VOKABELN WHERE vokabelDE = :de")
    LiveData<Integer> getId(String de);

    @Query("UPDATE VOKABELN SET vokabelENG = :neu WHERE vokabelENG = :alt")
    void updateVokabelENG(String alt, String neu);

    @Query("UPDATE VOKABELN SET vokabelDE = :neu WHERE vokabelDE = :alt")
    void updateVokabelDE(String alt, String neu);

    @Query("UPDATE VOKABELN SET markiert = :markiert WHERE vokabelDE = :de")
    void updateMarkierung(String de, boolean markiert);

    @Query("UPDATE VOKABELN SET answered = :answered WHERE id = :id")
    void updateAnswered(int id, int answered);

    @Query("UPDATE VOKABELN SET richtig = :richtig WHERE id = :id")
    void updateRichtige(int id, int richtig);

    @Query("UPDATE VOKABELN SET falsch = :falsch WHERE id = :id")
    void updateFalsche(int id, int falsch);

    @Query("UPDATE VOKABELN SET kategorie = :newCategory WHERE kategorie = :oldCategory")
    void updateCategory(String oldCategory, String newCategory);

    @Query("DELETE FROM VOKABELN WHERE id = :id")
    void deleteVokabelWithId(int id);

    @Query("DELETE FROM VOKABELN WHERE vokabelENG = :vokabelENG")
    void deleteVokabelWithENG(String vokabelENG);

    @Query("DELETE FROM VOKABELN WHERE vokabelDE = :vokabelDE")
    void deleteVokabelWithDE(String vokabelDE);

    @Query("DELETE FROM VOKABELN WHERE kategorie = :kategorie")
    void deleteKategorie(String kategorie);

    @Query("DELETE FROM VOKABELN")
    void deleteAlles();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertVokabel(Vokabel vokabel);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAlleVokabeln(Vokabel... vokabeln);

    @Update
    void updateVokabel(Vokabel vokabel);

    @Delete
    void deleatVokabel(Vokabel vokabel);
}