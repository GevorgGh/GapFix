package com.example.gapfix;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SubjectHelper {
    private static final Map<String, String> translationMap = new ConcurrentHashMap<>();
    private static boolean isLoading = false;

    public interface OnTranslationLoadedListener {
        void onTranslationsReady();
    }

    public static void loadTranslations(Context context, OnTranslationLoadedListener listener) {
        if (isLoading) return;
        isLoading = true;
        FirebaseDatabase.getInstance().getReference("Subjects").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                translationMap.clear();
                String lang = LocaleHelper.getLanguage(context);
                for (DataSnapshot data : snapshot.getChildren()) {
                    Object value = data.getValue();
                    if (value instanceof String) {
                        String s = (String) value;
                        translationMap.put(s, s);
                    } else if (value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> translations = (Map<String, String>) value;
                        String canonical = translations.get("en");
                        String translated = translations.get(lang);
                        if (translated == null) translated = canonical;
                        if (canonical != null) translationMap.put(canonical, translated);
                    }
                }
                isLoading = false;
                if (listener != null) listener.onTranslationsReady();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isLoading = false;
            }
        });
    }

    public static String getTranslatedSubject(String canonical) {
        if (canonical == null) return "";
        String translated = translationMap.get(canonical);
        return translated != null ? translated : canonical;
    }
}
