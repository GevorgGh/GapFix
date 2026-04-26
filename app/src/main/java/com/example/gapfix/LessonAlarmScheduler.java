package com.example.gapfix;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Helper that registers / cancels an AlarmManager alarm for a lesson reminder using UTC timestamps.
 */
public class LessonAlarmScheduler {

    private static final String TAG = "LessonAlarmScheduler";

    /**
     * Schedule a reminder alarm for the given booking using UTC timestamp.
     */
    public static void schedule(Context context,
                                String bookingId,
                                long timestamp,
                                String subject,
                                String role) {

        // The alarm fires at (lessonStartTime − 5 minutes).
        // FIX: Using TUTOR_JOIN_WINDOW_MINUTES since JOIN_WINDOW_MINUTES was renamed/removed
        long fireAt = timestamp - (LessonTimeHelper.TUTOR_JOIN_WINDOW_MINUTES * 60_000L);

        if (fireAt <= System.currentTimeMillis()) {
            Log.d(TAG, "Alarm time already passed for booking: " + bookingId);
            return;
        }

        int requestCode = bookingId.hashCode();

        Intent intent = new Intent(context, LessonReminderReceiver.class);
        intent.putExtra(LessonReminderReceiver.EXTRA_BOOKING_ID,  bookingId);
        intent.putExtra(LessonReminderReceiver.EXTRA_BOOKING_ID  + "_code", requestCode);
        intent.putExtra(LessonReminderReceiver.EXTRA_SUBJECT,     subject);
        intent.putExtra(LessonReminderReceiver.EXTRA_ROLE,        role);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, fireAt, pendingIntent);
                Log.d(TAG, "Exact alarm scheduled for: " + bookingId + " at " + fireAt);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent);
                Log.w(TAG, "Exact alarm permission not granted; using inexact alarm.");
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, fireAt, pendingIntent);
            Log.d(TAG, "Exact alarm scheduled for: " + bookingId);
        }
    }

    /** Cancel a previously scheduled reminder for a booking. */
    public static void cancel(Context context, String bookingId) {
        int requestCode = bookingId.hashCode();
        Intent intent = new Intent(context, LessonReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            AlarmManager alarmManager =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                Log.d(TAG, "Alarm cancelled for booking: " + bookingId);
            }
        }
    }
}
