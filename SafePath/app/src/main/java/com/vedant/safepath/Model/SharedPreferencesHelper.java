package com.vedant.safepath.Model;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.gms.maps.GoogleMap;

public class SharedPreferencesHelper {

    private static final String PREFS_NAME = "SafePathSettings";

    public static final String PREF_MAP_TYPE = "pref_map_type";
    public static final String PREF_SAFE_ZONES = "pref_safe_zones";
    public static final String PREF_LOCATION_FREQ = "pref_location_freq";
    public static final String PREF_THEME = "pref_theme";

    public static final String PREF_SOS_SMS = "pref_sos_sms";
    public static final String PREF_SOS_CALL = "pref_sos_call";
    public static final String PREF_SOS_RECORD = "pref_sos_record";
    public static final String PREF_SOS_SIREN = "pref_sos_siren";
    public static final String PREF_SOS_FLASH = "pref_sos_flash";

    public static final long FREQ_LOW_MS = 30000;
    public static final long FREQ_MEDIUM_MS = 10000;
    public static final long FREQ_HIGH_MS = 5000;

    private final SharedPreferences sharedPreferences;

    public SharedPreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setMapType(int mapType) {
        sharedPreferences.edit().putInt(PREF_MAP_TYPE, mapType).apply();
    }

    public int getMapType() {
        return sharedPreferences.getInt(PREF_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);
    }

    public void setShowSafeZones(boolean show) {
        sharedPreferences.edit().putBoolean(PREF_SAFE_ZONES, show).apply();
    }

    public boolean getShowSafeZones() {
        return sharedPreferences.getBoolean(PREF_SAFE_ZONES, true);
    }

    public void setLocationFrequency(int frequency) {
        sharedPreferences.edit().putInt(PREF_LOCATION_FREQ, frequency).apply();
    }

    public int getLocationFrequency() {
        return sharedPreferences.getInt(PREF_LOCATION_FREQ, 1);
    }

    public long getLocationUpdateInterval() {
        int frequency = getLocationFrequency();
        switch (frequency) {
            case 0:
                return FREQ_LOW_MS;
            case 2:
                return FREQ_HIGH_MS;
            case 1:
            default:
                return FREQ_MEDIUM_MS;
        }
    }

    public void setTheme(int themeMode) {
        sharedPreferences.edit().putInt(PREF_THEME, themeMode).apply();
    }

    public int getTheme() {
        return sharedPreferences.getInt(PREF_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setSosSmsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(PREF_SOS_SMS, enabled).apply();
    }

    public boolean getSosSmsEnabled() {
        return sharedPreferences.getBoolean(PREF_SOS_SMS, true);
    }

    public void setSosCallEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(PREF_SOS_CALL, enabled).apply();
    }

    public boolean getSosCallEnabled() {
        return sharedPreferences.getBoolean(PREF_SOS_CALL, false);
    }

    public void setSosRecordEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(PREF_SOS_RECORD, enabled).apply();
    }

    public boolean getSosRecordEnabled() {
        return sharedPreferences.getBoolean(PREF_SOS_RECORD, false);
    }

    public void setSosSirenEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(PREF_SOS_SIREN, enabled).apply();
    }

    public boolean getSosSirenEnabled() {
        return sharedPreferences.getBoolean(PREF_SOS_SIREN, true);
    }

    public void setSosFlashEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(PREF_SOS_FLASH, enabled).apply();
    }

    public boolean getSosFlashEnabled() {
        return sharedPreferences.getBoolean(PREF_SOS_FLASH, true);
    }
}
