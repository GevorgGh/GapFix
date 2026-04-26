package com.example.gapfix;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

public class TutorFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "GapFix_FCM";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "!!! FCM MESSAGE RECEIVED !!!");
        
        if (remoteMessage.getData().size() > 0) {
            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("message");
            String bId = remoteMessage.getData().get("bookingId");
            boolean isCall = Boolean.parseBoolean(remoteMessage.getData().get("isCall"));
            
            showNotification(title, message, bId, isCall);
        } else if (remoteMessage.getNotification() != null) {
            showNotification(remoteMessage.getNotification().getTitle(), 
                             remoteMessage.getNotification().getBody(), null, false);
        }
    }

    private void showNotification(String title, String message, String bId, boolean isCall) {
        String channelId = isCall ? "gapfix_call_notifications" : "gapfix_notifications";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, 
                isCall ? "Call Notifications" : "General Notifications", 
                NotificationManager.IMPORTANCE_HIGH);
            if (isCall) {
                channel.enableVibration(true);
                channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            }
            manager.createNotificationChannel(channel);
        }

        // Open VideoCallActivity for calls, MainActivity for others
        Intent intent = new Intent(this, isCall ? VideoCallActivity.class : MainActivity.class);
        if (bId != null) {
            intent.putExtra("BOOKING_ID", bId);
            intent.putExtra("IS_INCOMING", isCall);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, new Random().nextInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(isCall ? NotificationCompat.CATEGORY_CALL : NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (isCall) {
            // Full-screen intent makes it pop up like a real call
            builder.setFullScreenIntent(pendingIntent, true);
            builder.addAction(R.drawable.baseline_mic_24, "ANSWER", pendingIntent);
            builder.setOngoing(true);
        }

        manager.notify(isCall ? 2001 : new Random().nextInt(1000), builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        GapFixApplication.updateFcmToken();
    }

    public static void showStaticNotification(Context context, String title, String message) {
        String channelId = "gapfix_notifications";
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "System Updates", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify(new Random().nextInt(), builder.build());
    }
}
