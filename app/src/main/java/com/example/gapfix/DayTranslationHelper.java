package com.example.gapfix;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;

public class DayTranslationHelper {
    public static String translateDay(Context context, String day) {
        if (day == null) return "Current";
        
        switch (day.toLowerCase()) {
            case "mondays": return context.getString(R.string.day_mondays);
            case "tuesdays": return context.getString(R.string.day_tuesdays);
            case "wednesdays": return context.getString(R.string.day_wednesdays);
            case "thursdays": return context.getString(R.string.day_thursdays);
            case "fridays": return context.getString(R.string.day_fridays);
            case "saturdays": return context.getString(R.string.day_saturdays);
            case "sundays": return context.getString(R.string.day_sundays);
            case "monday": return context.getString(R.string.day_monday);
            case "tuesday": return context.getString(R.string.day_tuesday);
            case "wednesday": return context.getString(R.string.day_wednesday);
            case "thursday": return context.getString(R.string.day_thursday);
            case "friday": return context.getString(R.string.day_friday);
            case "saturday": return context.getString(R.string.day_saturday);
            case "sunday": return context.getString(R.string.day_sunday);
            default: return day;
        }
    }
    
    public static String[] getTranslatedDaysArray(Context context) {
        return new String[] {
            context.getString(R.string.day_mondays),
            context.getString(R.string.day_tuesdays),
            context.getString(R.string.day_wednesdays),
            context.getString(R.string.day_thursdays),
            context.getString(R.string.day_fridays),
            context.getString(R.string.day_saturdays),
            context.getString(R.string.day_sundays)
        };
    }
    
    public static String getEnglishDayFromTranslated(Context context, String translated) {
        if (translated.equals(context.getString(R.string.day_mondays))) return "Mondays";
        if (translated.equals(context.getString(R.string.day_tuesdays))) return "Tuesdays";
        if (translated.equals(context.getString(R.string.day_wednesdays))) return "Wednesdays";
        if (translated.equals(context.getString(R.string.day_thursdays))) return "Thursdays";
        if (translated.equals(context.getString(R.string.day_fridays))) return "Fridays";
        if (translated.equals(context.getString(R.string.day_saturdays))) return "Saturdays";
        if (translated.equals(context.getString(R.string.day_sundays))) return "Sundays";
        return translated;
    }
}
