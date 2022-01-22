package com.rommelbendel.scanQ;

import android.app.Application;

import androidx.lifecycle.LiveData;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WeekdayRepository {
    private final WeekdayDao weekdayDao;

    WeekdayRepository(@NotNull Application application) {
        Datenbank db = Datenbank.getDatenbank(application);
        weekdayDao = db.weekdayDao();
    }

    public LiveData<List<Weekday>> getAllDays() {
        return weekdayDao.getAllDays();
    }

    public LiveData<Weekday> getDay(final int dayOfWeek) {
        return weekdayDao.getDay(dayOfWeek);
    }

    public LiveData<Integer> getSumOfTrainedVocabs() {
        return weekdayDao.getSumOfTrainedVocabs();
    }

    public LiveData<Float> getAverageOfTrainedVocabs() {
        return weekdayDao.getAverageOfTrainedVocabs();
    }

    public void insertWeekday(final Weekday weekday) {
        Datenbank.databaseWriteExecutor.execute(() -> weekdayDao.insert(weekday));
    }

    public void insertAllWeekdays(final Weekday... weekdays) {
        Datenbank.databaseWriteExecutor.execute(() -> weekdayDao.insertAll(weekdays));
    }

    public void incrementNumberOfTrainedVocabs(final int dayOfWeek) {
        Datenbank.databaseWriteExecutor.execute(() -> weekdayDao.incrementTrainingValue(dayOfWeek));
    }

    public void incrementNumberOfRightAnswers(final int dayOfWeek) {
        Datenbank.databaseWriteExecutor.execute(() -> weekdayDao.incrementRightAnswers(dayOfWeek));
    }

    public void incrementNumberOfWrongAnswers(final int dayOfWeek) {
        Datenbank.databaseWriteExecutor.execute(() -> weekdayDao.incrementWrongAnswers(dayOfWeek));
    }

    public void updateDate(final long newDate, final int dayOfWeek) {
        Datenbank.databaseWriteExecutor.execute(() -> weekdayDao.updateDate(newDate, dayOfWeek));
    }

    public void updateDay(final long newDate, final int dayOfWeek) {
        Datenbank.databaseWriteExecutor.execute(() -> weekdayDao.updateDay(newDate, dayOfWeek));
    }
}
