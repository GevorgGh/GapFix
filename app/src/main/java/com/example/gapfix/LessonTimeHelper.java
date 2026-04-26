package com.example.gapfix;

import android.util.Log;

/**
 * Robust utility class for lesson time calculations using UTC timestamps.
 */
public class LessonTimeHelper {

    private static final String TAG = "LessonTimeHelper";

    /** Minutes before lesson start that the JOIN button becomes active for Tutors. */
    public static final long TUTOR_JOIN_WINDOW_MINUTES = 5;

    /** Default lesson duration in minutes if not specified. */
    public static final long DEFAULT_DURATION_MINUTES = 60;

    /**
     * Returns true if the current time is within the join window.
     * Tutors can join 5 minutes early. Students can only join exactly at start time.
     */
    public static boolean isJoinable(Booking booking, String userType) {
        if (booking == null) return false;
        
        long now = System.currentTimeMillis();
        long start = booking.getTimestamp();
        
        // Tutor can join 5 minutes early, Student joins exactly on time
        long windowStart = "tutor".equalsIgnoreCase(userType) 
                ? start - (TUTOR_JOIN_WINDOW_MINUTES * 60_000L) 
                : start;
        
        // Window closes after the lesson duration (default 60 mins)
        long windowEnd = start + (DEFAULT_DURATION_MINUTES * 60_000L);

        boolean result = now >= windowStart && now <= windowEnd;
        
        Log.d(TAG, "isJoinable check (" + userType + "): now=" + now + ", start=" + start + ", result=" + result);
        return result;
    }

    /**
     * Returns the number of whole minutes until the JOIN button becomes active.
     */
    public static long minutesUntilJoinable(Booking booking, String userType) {
        if (booking == null) return -1;
        
        long start = booking.getTimestamp();
        long windowStart = "tutor".equalsIgnoreCase(userType) 
                ? start - (TUTOR_JOIN_WINDOW_MINUTES * 60_000L) 
                : start;
                
        long diff = windowStart - System.currentTimeMillis();
        
        return diff <= 0 ? 0 : (diff / 60_000L);
    }
}
