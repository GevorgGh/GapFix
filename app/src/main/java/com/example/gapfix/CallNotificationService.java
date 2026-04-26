package com.example.gapfix;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.Random;

public class CallNotificationService extends Service {

    private static final String TAG = "CallNotifService";
    private static final String CHANNEL_ID_CALLS = "gapfix_call_notifications";
    private static final String CHANNEL_ID_SERVICE = "gapfix_service_channel";

    private Query notifQuery;
    private ChildEventListener childEventListener;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        
        Notification notification = buildSilentForegroundNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1001, notification);
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID_SERVICE, "GapFix Active", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(serviceChannel);

            NotificationChannel callChannel = new NotificationChannel(CHANNEL_ID_CALLS, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH);
            callChannel.enableVibration(true);
            callChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(callChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && notifQuery == null) {
            startListening(user.getUid());
        }
        return START_STICKY;
    }

    private void startListening(String uid) {
        notifQuery = FirebaseDatabase.getInstance().getReference("Notifications")
                .child(uid)
                .orderByChild("timestamp")
                .startAt(System.currentTimeMillis());

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String title = snapshot.child("title").getValue(String.class);
                String message = snapshot.child("message").getValue(String.class);
                String bId = snapshot.child("bookingId").getValue(String.class);
                boolean isCall = snapshot.child("isCall").exists();
                
                if (isCall) {
                    handleIncomingCall(title, message, bId);
                } else {
                    showGeneralNotification(title, message, bId);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        notifQuery.addChildEventListener(childEventListener);
    }

    private void handleIncomingCall(String title, String message, String bId) {
        // 1. Prepare the Intent
        Intent callIntent = new Intent(this, VideoCallActivity.class);
        callIntent.putExtra("BOOKING_ID", bId);
        callIntent.putExtra("IS_INCOMING", true);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, new Random().nextInt(), callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 2. Show the Notification with fullScreenIntent (Official way to pop up Activity from background)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_CALLS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title != null ? title : "Incoming Call")
                .setContentText(message != null ? message : "Someone is calling you...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true) // This pops up the activity
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(true)
                .setVibrate(new long[]{1000, 1000, 1000, 1000})
                .addAction(R.drawable.baseline_mic_24, "ANSWER", pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(2002, builder.build());

        // 3. Force Start Activity (Additional push for foreground state)
        try {
            startActivity(callIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to force startActivity: " + e.getMessage());
        }
    }

    private void showGeneralNotification(String title, String message, String bId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, new Random().nextInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_CALLS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(2003, builder.build());
    }

    private Notification buildSilentForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID_SERVICE)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("GapFix Calling Service")
                .setContentText("Checking for incoming calls...")
                .build();
    }

    @Override
    public void onDestroy() {
        if (notifQuery != null) notifQuery.removeEventListener(childEventListener);
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
