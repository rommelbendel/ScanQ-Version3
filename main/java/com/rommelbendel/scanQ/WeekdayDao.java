package com.rommelbendel.scanQ;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WeekdayDao {
    @Query("UPDATE Weekday SET NumberOfTrainedVocabs = NumberOfTrainedVocabs + 1 " +
            "WHERE DayOfWeek = :dayOfWeek")
    void incrementTrainingValue(int dayOfWeek);

    @Query("UPDATE Weekday SET NumberOfRightAnswers = NumberOfRightAnswers + 1 " +
            "WHERE DayOfWeek = :dayOfWeek")
    void incrementRightAnswers(int dayOfWeek);

    @Query("UPDATE Weekday SET NumberOfWrongAnswers = NumberOfWrongAnswers + 1 " +
            "WHERE DayOfWeek = :dayOfWeek")
    void incrementWrongAnswers(int dayOfWeek);

    @Query("UPDATE Weekday SET Date = :newDate WHERE DayOfWeek = :dayOfWeek")
    void updateDate(long newDate, int dayOfWeek);

    @Query("UPDATE Weekday SET DATE = :newDate, NumberOfTrainedVocabs = 0, " +
            "NumberOfRightAnswers = 0, NumberOfWrongAnswers = 0 WHERE DayOfWeek = :dayOfWeek")
    void updateDay(long newDate, int dayOfWeek);

    @Query("SELECT * FROM Weekday ORDER BY Date DESC")
    LiveData<List<Weekday>> getAllDays();

    @Query("SELECT * FROM Weekday WHERE DayOfWeek = :dayOfWeek LIMIT 1")
    LiveData<Weekday> getDay(int dayOfWeek);

    @Query("SELECT SUM(NumberOfTrainedVocabs) FROM Weekday")
    LiveData<Integer> getSumOfTrainedVocabs();

    @Query("SELECT AVG(NumberOfTrainedVocabs) FROM Weekday")
    LiveData<Float> getAverageOfTrainedVocabs();

    @Insert
    void insert(Weekday weekday);

    @Insert
    void insertAll(Weekday... weekdays);

}
