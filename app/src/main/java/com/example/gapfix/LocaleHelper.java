package com.example.gapfix;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public class LocaleHelper {

    public static void setLocale(Context context, String language) {
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(language);
        AppCompatDelegate.setApplicationLocales(appLocales);
    }

    public static String getLanguage(Context context) {
        LocaleListCompat currentAppLocales = AppCompatDelegate.getApplicationLocales();
        if (currentAppLocales.isEmpty()) {
            return Locale.getDefault().getLanguage();
        }
        Locale locale = currentAppLocales.get(0);
        return locale != null ? locale.getLanguage() : Locale.getDefault().getLanguage();
    }

    public static Context onAttach(Context context) {
        return context;
    }
}
