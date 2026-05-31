package com.example.gapfix;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
public class LessonAlarmScheduler {
    public static void schedule(Context context,
                                String bookingId,
                                long timestamp,
                                String subject,
                                String role) {
        long fireAt = timestamp - (LessonTimeHelper.TUTOR_JOIN_WINDOW_MINUTES * 60_000L);
        if (fireAt <= System.currentTimeMillis()) {
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
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent);
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, fireAt, pendingIntent);
        }
    }
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
            }
        }
    }
}
