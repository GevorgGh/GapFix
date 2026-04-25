package com.example.gapfix;

import android.util.Log;

/**
 * Robust utility class for lesson time calculations using UTC timestamps.
 */
public class LessonTimeHelper {

    private static final String TAG = "LessonTimeHelper";

    /** Minutes before lesson start that the JOIN button becomes active. */
    public static final long JOIN_WINDOW_MINUTES = 5;
    
    /** Default lesson duration in minutes if not specified. */
    public static final long DEFAULT_DURATION_MINUTES = 60;

    /**
     * Returns true if the current time is within the join window [start - 5m, end].
     */
    public static boolean isJoinable(Booking booking) {
        if (booking == null) return false;
        
        long now = System.currentTimeMillis();
        long start = booking.getTimestamp();
        
        // Window opens 5 minutes before the lesson starts
        long open = start - (JOIN_WINDOW_MINUTES * 60_000L);
        
        // Window closes after the lesson duration (default 60 mins)
        long close = start + (DEFAULT_DURATION_MINUTES * 60_000L);

        boolean result = now >= open && now <= close;
        
        Log.d(TAG, "isJoinable check: now=" + now + ", start=" + start + ", result=" + result);
        return result;
    }

    /**
     * Returns the number of whole minutes until the JOIN button becomes active.
     */
    public static long minutesUntilJoinable(Booking booking) {
        if (booking == null) return -1;
        
        long start = booking.getTimestamp();
        long open = start - (JOIN_WINDOW_MINUTES * 60_000L);
        long diff = open - System.currentTimeMillis();
        
        return diff <= 0 ? 0 : (diff / 60_000L);
    }
}
