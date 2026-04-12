package com.example.gapfix;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

public class TutorFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() != null) {
            showNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody()
            );
        }
    }

    private void showNotification(String title, String message) {
        String channelId = "gapfix_chat_notifications";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(channelId, "Messages", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true);

        manager.notify(new Random().nextInt(), builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        sendTokenToDatabase(token);
    }

    private void sendTokenToDatabase(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            // Update token in Student branch
            FirebaseDatabase.getInstance().getReference("Users").child("Student").child(uid).child("fcmToken").setValue(token);
            // Update token in Tutor branch (the backend will check both)
            FirebaseDatabase.getInstance().getReference("Users").child("Tutor").child(uid).child("fcmToken").setValue(token);
        }
    }

    public static void showStaticNotification(Context context, String title, String message) {
        String channelId = "gapfix_notifications";
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(channelId, "System", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(new Random().nextInt(), builder.build());
    }
}
