package com.example.gapfix;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
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

import java.util.Random;

public class CallNotificationService extends Service {

    private static final String TAG = "CallNotifService";
    private static final String CHANNEL_ID_CALLS = "gapfix_call_notifications";
    private static final String CHANNEL_ID_SERVICE = "gapfix_service_channel";

    private DatabaseReference notifRef;
    private ChildEventListener childEventListener;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, buildSilentForegroundNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && notifRef == null) {
            startListening(user.getUid());
        }
        return START_STICKY;
    }

    private void startListening(String uid) {
        notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(uid);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                // Within last 2 minutes to prevent ghost rings
                if (timestamp != null && System.currentTimeMillis() - timestamp < 120_000) {
                    String title = snapshot.child("title").getValue(String.class);
                    String message = snapshot.child("message").getValue(String.class);
                    String bId = snapshot.child("bookingId").getValue(String.class);
                    boolean isCall = snapshot.child("isCall").exists();

                    showCallNotification(title, message, bId, isCall);
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };

        notifRef.addChildEventListener(childEventListener);
    }

    private void showCallNotification(String title, String message, String bId, boolean isCall) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel callChannel = new NotificationChannel(CHANNEL_ID_CALLS, "Calls", NotificationManager.IMPORTANCE_HIGH);
            callChannel.enableVibration(true);
            callChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(callChannel);
        }

        Intent callIntent = new Intent(this, VideoCallActivity.class);
        callIntent.putExtra("BOOKING_ID", bId);
        callIntent.putExtra("IS_INCOMING", isCall);
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, new Random().nextInt(), callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_CALLS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true) // Key for the ringing screen
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOngoing(isCall);

        if (isCall) {
            builder.addAction(R.drawable.baseline_mic_24, "ANSWER", pendingIntent);
        }

        manager.notify(new Random().nextInt(1000), builder.build());
    }

    private Notification buildSilentForegroundNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_SERVICE, "Background", NotificationManager.IMPORTANCE_MIN);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID_SERVICE)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("GapFix is active")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    @Override
    public void onDestroy() {
        if (notifRef != null) notifRef.removeEventListener(childEventListener);
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
