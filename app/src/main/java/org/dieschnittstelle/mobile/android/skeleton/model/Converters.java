package org.dieschnittstelle.mobile.android.skeleton.model;

import androidx.room.TypeConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Converters {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

    // Converter für LocalDate
    @TypeConverter
    public static LocalDate fromDateString(String value) {
        return value == null ? null : LocalDate.parse(value, DATE_FORMATTER);
    }

    @TypeConverter
    public static String dateToString(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMATTER);
    }

    // Converter für LocalTime
    @TypeConverter
    public static LocalTime fromTimeString(String value) {
        return value == null ? null : LocalTime.parse(value, TIME_FORMATTER);
    }

    @TypeConverter
    public static String timeToString(LocalTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }
}
