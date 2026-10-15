package com.vedant.safepath;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.vedant.safepath.Model.SharedPreferencesHelper;

public class SafePathApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(this);

        int themeMode = prefsHelper.getTheme();

        AppCompatDelegate.setDefaultNightMode(themeMode);
    }
}
