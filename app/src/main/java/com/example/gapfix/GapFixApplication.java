package com.example.gapfix;

import android.app.Application;
import android.util.Log;

public class GapFixApplication extends Application {

    private static final String TAG = "GapFix-App";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "GapFix Application Started");
    }

    public static void fetchTokenAndLogin(String uid) {
        Log.d(TAG, "Agora Chat login skipped (using Firestore for chat)");
    }
}
