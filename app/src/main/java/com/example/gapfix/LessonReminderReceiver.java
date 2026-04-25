package com.example.gapfix;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

/**
 * BroadcastReceiver fired by AlarmManager 5 minutes before a lesson.
 * Shows a high-priority local notification to both student and tutor.
 */
public class LessonReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_BOOKING_ID  = "bookingId";
    public static final String EXTRA_SUBJECT      = "subject";
    public static final String EXTRA_TIME         = "lessonTime";
    public static final String EXTRA_ROLE         = "role"; // "student" | "tutor"

    private static final String CHANNEL_ID   = "gapfix_lesson_reminders";
    private static final String CHANNEL_NAME = "Lesson Reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String subject    = intent.getStringExtra(EXTRA_SUBJECT);
        String lessonTime = intent.getStringExtra(EXTRA_TIME);
        String role       = intent.getStringExtra(EXTRA_ROLE);

        if (subject == null)    subject    = "Your lesson";
        if (lessonTime == null) lessonTime = "";

        // Deep-link to the correct home screen based on role
        Class<?> targetClass = "tutor".equals(role)
                ? HomeTutorActivity.class
                : HomeStudentActivity.class;

        Intent tapIntent = new Intent(context, targetClass);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                intent.getIntExtra(EXTRA_BOOKING_ID + "_code", 0),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel (idempotent on Android O+)
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Reminders fired 5 minutes before each lesson");
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);

        String title = "⏰ Lesson starting soon!";
        String body  = subject + " starts in 5 minutes at " + lessonTime.split("-")[0].trim();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500});

        manager.notify(intent.getIntExtra(EXTRA_BOOKING_ID + "_code", (int) System.currentTimeMillis()),
                builder.build());
    }
}
