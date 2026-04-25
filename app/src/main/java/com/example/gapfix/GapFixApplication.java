package com.example.gapfix;

import android.app.Application;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import io.agora.chat.ChatClient;
import io.agora.chat.ChatOptions;

public class GapFixApplication extends Application {

    private static final String TAG = "GapFix-Application";

    @Override
    public void onCreate() {
        super.onCreate();
        initAgoraChat();
        updateFcmToken();
    }

    private void initAgoraChat() {
        ChatOptions options = new ChatOptions();
        String appKey = "71200010442#200015438"; 
        options.setAppKey(appKey);
        options.setAcceptInvitationAlways(true);
        options.setAutoLogin(true);
        ChatClient.getInstance().init(this, options);
    }

    public static void updateFcmToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String token = task.getResult();
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users");
                    
                    // Check Student branch
                    userRef.child("Student").child(uid).get().addOnCompleteListener(sTask -> {
                        if (sTask.isSuccessful() && sTask.getResult().exists()) {
                            userRef.child("Student").child(uid).child("fcmToken").setValue(token);
                        }
                    });

                    // Check Tutor branch
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
