package com.rommelbendel.scanQ;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.joda.time.LocalDate;

@Entity(tableName = "Weekday")
public class Weekday {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "DayOfWeek")
    private int dayOfWeek;

    @ColumnInfo(name = "NameOfDay")
    @NonNull private String nameOfDay;

    @ColumnInfo(name = "Date")
    private long date;

    @ColumnInfo(name = "NumberOfTrainedVocabs", defaultValue = "0")
    private int numberOfTrainedVocabs;

    @ColumnInfo(name = "NumberOfRightAnswers", defaultValue = "0")
    private int numberOfRightAnswers;

    @ColumnInfo(name = "NumberOfWrongAnswers", defaultValue = "0")
    private int numberOfWrongAnswers;

    public Weekday(@NonNull final String nameOfDay, final long date) {
        this.nameOfDay = nameOfDay;
        this.date = date;
    }

    @Ignore
    public Weekday(@NonNull final String nameOfDay) {
        this.nameOfDay = nameOfDay;
        LocalDate localDate = LocalDate.now();
        this.date = localDate.toDate().getTime();
    }

    @Ignore
    public Weekday(@NonNull final String nameOfDay, final long date,
                   final int numberOfTrainedVocabs, final int numberOfRightAnswers,
                   final int numberOfWrongAnswers) {
        this.nameOfDay = nameOfDay;
        this.date = date;
        this.numberOfTrainedVocabs = numberOfTrainedVocabs;
        this.numberOfRightAnswers = numberOfRightAnswers;
        this.numberOfWrongAnswers = numberOfWrongAnswers;
    }

    public Weekday(final int dayOfWeek,@NonNull final String name, final long time) {
        this.dayOfWeek = dayOfWeek;
        this.nameOfDay = name;
        this.date = time;
    }

    public int getDayOfWeek() {
        return this.dayOfWeek;
    }

    public @NonNull String getNameOfDay() {
        return this.nameOfDay;
    }

    public long getDate() {
        return this.date;
    }

    public int getNumberOfTrainedVocabs() {
        return this.numberOfTrainedVocabs;
    }

    public int getNumberOfRightAnswers() {
        return this.numberOfRightAnswers;
    }

    public int getNumberOfWrongAnswers() {
        return this.numberOfWrongAnswers;
    }

    public void setDayOfWeek(final int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setNameOfDay(final @NonNull String nameOfDay) {
        this.nameOfDay = nameOfDay;
    }

    public void setDate(final long date) {
        this.date = date;
    }

    public void setNumberOfTrainedVocabs(final int numberOfTrainedVocabs) {
        this.numberOfTrainedVocabs = numberOfTrainedVocabs;
    }

    public void setNumberOfRightAnswers(final int numberOfRightAnswers) {
        this.numberOfRightAnswers = numberOfRightAnswers;
    }

    public void setNumberOfWrongAnswers(final int numberOfWrongAnswers) {
        this.numberOfWrongAnswers = numberOfWrongAnswers;
    }

    @Override
    public String toString() {
        return "Weekday{" +
                "dayOfWeek=" + dayOfWeek +
                ", nameOfDay='" + nameOfDay + '\'' +
                ", date=" + date +
                ", numberOfTrainedVocabs=" + numberOfTrainedVocabs +
                ", numberOfRightAnswers=" + numberOfRightAnswers +
                ", numberOfWrongAnswers=" + numberOfWrongAnswers +
                '}';
    }
}
