package com.example.gapfix;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

import io.agora.chat.ChatClient;
import io.agora.chat.ChatOptions;

public class GapFixApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initAgoraChat();
        initCloudinary();
        updateFcmToken();
        setupOrientationLock();
    }

    private void setupOrientationLock() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
                
                boolean isTablet = activity.getResources().getConfiguration().smallestScreenWidthDp >= 600;
                if (!isTablet) {
                    
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
            @Override public void onActivityStarted(@NonNull Activity activity) {}
            @Override public void onActivityResumed(@NonNull Activity activity) {}
            @Override public void onActivityPaused(@NonNull Activity activity) {}
            @Override public void onActivityStopped(@NonNull Activity activity) {}
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }

    private void initAgoraChat() {
        ChatOptions options = new ChatOptions();
        String appKey = "71200010442#200015438"; 
        options.setAppKey(appKey);
        options.setAcceptInvitationAlways(true);
        options.setAutoLogin(true);
        ChatClient.getInstance().init(this, options);
    }

    private void initCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dbugqpl3m");
            MediaManager.init(this, config);
        } catch (Exception e) {
        }
    }

    public static void updateFcmToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String token = task.getResult();
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users");
                    
                    
                    userRef.child("Student").child(uid).get().addOnCompleteListener(sTask -> {
                        if (sTask.isSuccessful() && sTask.getResult().exists()) {
                            userRef.child("Student").child(uid).child("fcmToken").setValue(token);
                        }
                    });

                    
                    userRef.child("Tutor").child(uid).get().addOnCompleteListener(tTask -> {
                        if (tTask.isSuccessful() && tTask.getResult().exists()) {
                            userRef.child("Tutor").child(uid).child("fcmToken").setValue(token);
                        }
                    });
                }
            });
        }
    }

    public static void fetchTokenAndLogin(String uid) {
        return;
    }
}
