package com.example.gapfix;

import android.app.Application;
import android.util.Log;

import com.google.firebase.functions.FirebaseFunctions;

import java.util.Map;

import io.agora.CallBack;
import io.agora.chat.ChatClient;
import io.agora.chat.ChatOptions;
import io.agora.exceptions.ChatException;

public class GapFixApplication extends Application {

    private static final String TAG = "GapFix-Agora";

    @Override
    public void onCreate() {
        super.onCreate();
        initAgoraChat();
    }

    private void initAgoraChat() {
        ChatOptions options = new ChatOptions();
        // Updated AppKey from your latest screenshot
        String appKey = "71200010442#200015438"; 

        options.setAppKey(appKey);
        options.setAcceptInvitationAlways(true);
        options.setAutoLogin(true);

        ChatClient.getInstance().init(this, options);
        Log.d(TAG, "Agora Chat Initialized");
    }

    /**
     * Entry point for Agora Login used by Login, Signup, and Home activities.
     */
    public static void fetchTokenAndLogin(String uid) {
        if (ChatClient.getInstance().isLoggedIn()) {
            Log.d(TAG, "Already logged in to Agora");
            return;
        }

        // --- TEST MODE TOKEN ---
        String testToken = "007eJxTYDj752xby47SOc9XlvzfJv6d29Lqzqeu3ayrZDIXFuv47VqgwGBiaGBpZJhimmZobGBikmphaZhslppoYWxuaWRqamqcnLD+VmZDICODtcdjFkYGVgZGBiYGEJ+BAQA4Sh9b"; 
        
        if (!testToken.isEmpty()) {
            Log.d(TAG, "Using TEST TOKEN for login...");
            performAgoraLogin(uid, testToken);
            return;
        }

        // --- PRODUCTION MODE: FETCH FROM FIREBASE ---
        Log.d(TAG, "Fetching Agora token from Firebase Cloud Functions...");
        FirebaseFunctions.getInstance()
                .getHttpsCallable("getAgoraToken")
                .call()
                .addOnSuccessListener(result -> {
                    Object dataObj = result.getData();
                    if (dataObj instanceof Map) {
                        Map<String, Object> data = (Map<String, Object>) dataObj;
                        String token = (String) data.get("token");
                        if (token != null) performAgoraLogin(uid, token);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Firebase Function error: " + e.getMessage()));
    }

    private static void performAgoraLogin(String uid, String token) {
        ChatClient.getInstance().loginWithToken(uid, token, new CallBack() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Agora Login Success for UID: " + uid);
            }

            @Override
            public void onError(int code, String error) {
                Log.e(TAG, "Agora Login Failed. Code: " + code + ", Error: " + error);
                
                if (code == 204) {
                    // User not found in Agora. Attempt auto-registration.
                    Log.d(TAG, "User not found (204). Attempting auto-registration...");
                    registerAndLogin(uid, token);
                }
            }
        });
    }

    private static void registerAndLogin(String uid, String token) {
        new Thread(() -> {
            try {
                // Register using UID as password (requires 'Open' registration in console)
                ChatClient.getInstance().createAccount(uid, uid);
                Log.d(TAG, "Auto-registration successful for: " + uid);
                performAgoraLogin(uid, token);
            } catch (ChatException e) {
                Log.e(TAG, "Auto-registration failed: " + e.getDescription() + " (Code: " + e.getErrorCode() + ")");
                if (e.getErrorCode() == 208) {
                    Log.e(TAG, "FIX REQUIRED: Change 'Registration Mode' to 'Open' in Agora Console > Chat > Basic Information.");
                }
            }
        }).start();
    }
}
