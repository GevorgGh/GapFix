package com.example.gapfix;

import java.io.Serializable;

public class WeeklySlot implements Serializable {
    public String dayName;
    public long startDateMs = -1;
    public int hour = -1;
    public int minute = -1;

    public WeeklySlot(String dayName) {
        this.dayName = dayName;
    }
}
