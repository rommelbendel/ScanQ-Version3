package com.rommelbendel.scanQ;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class WeekdayViewModel extends AndroidViewModel {

    private final WeekdayRepository weekdayRepository;

    public WeekdayViewModel(@NonNull Application application) {
        super(application);
        weekdayRepository = new WeekdayRepository(application);
    }

    public LiveData<List<Weekday>> getAllDays() {
        return weekdayRepository.getAllDays();
    }

    public LiveData<Weekday> getDay(final int dayOfWeek) {
        return weekdayRepository.getDay(dayOfWeek);
    }

    public LiveData<Integer> getSumOfTrainedVocabs() {
        return weekdayRepository.getSumOfTrainedVocabs();
    }

    public LiveData<Float> getAverageOfTrainedVocabs() {
        return weekdayRepository.getAverageOfTrainedVocabs();
    }

    public void insertAllWeekdays(final Weekday... weekdays) {
        weekdayRepository.insertAllWeekdays(weekdays);
    }

    public void insertWeekday(final Weekday weekday) {
        weekdayRepository.insertWeekday(weekday);
    }

    public void incrementNumberOfTrainedVocabs(final int dayOfWeek) {
        weekdayRepository.incrementNumberOfTrainedVocabs(dayOfWeek);
    }

    public void incrementNumberOfRightAnswers(final int dayOfWeek) {
        weekdayRepository.incrementNumberOfRightAnswers(dayOfWeek);
    }

    public void incrementNumberOfWrongAnswers(final int dayOfWeek) {
        weekdayRepository.incrementNumberOfWrongAnswers(dayOfWeek);
    }

    public void updateDate(final long newDate, final int dayOfWeek) {
        weekdayRepository.updateDate(newDate, dayOfWeek);
    }

    public void updateDay(final long newDate, final int dayOfWeek) {
        weekdayRepository.updateDay(newDate, dayOfWeek);
    }
}
