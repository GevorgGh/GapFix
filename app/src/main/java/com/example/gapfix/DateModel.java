package com.example.gapfix;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class DateModel {
    private String dayName;
    private String dayNumber;
    private Date fullDate;

    public DateModel(Date date) {
        this.fullDate = date;
        this.dayName = new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
        this.dayNumber = new SimpleDateFormat("dd", Locale.getDefault()).format(date);
    }

    public String getDayName() { return dayName; }
    public String getDayNumber() { return dayNumber; }
    public Date getFullDate() { return fullDate; }
}