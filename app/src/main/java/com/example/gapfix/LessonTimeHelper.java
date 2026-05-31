package com.example.gapfix;
public class LessonTimeHelper {
    public static final long TUTOR_JOIN_WINDOW_MINUTES = 5;
    public static final long DEFAULT_DURATION_MINUTES = 60;
    public static boolean isJoinable(Booking booking, String userType) {
        if (booking == null) return false;
        long now = System.currentTimeMillis();
        long start = booking.getTimestamp();
        long windowStart = "tutor".equalsIgnoreCase(userType)
                ? start - (TUTOR_JOIN_WINDOW_MINUTES * 60_000L)
                : start;
        long duration = booking.getDuration() > 0 ? booking.getDuration() : DEFAULT_DURATION_MINUTES;
        long windowEnd = start + (duration * 60_000L);
        return now >= windowStart && now <= windowEnd;
    }
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
