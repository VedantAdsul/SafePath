package com.vedant.safepath;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vedant.safepath.Data.UserProfileRepository;
import com.vedant.safepath.Model.SharedPreferencesHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SettingActivity extends AppCompatActivity {

    private SharedPreferencesHelper prefsHelper;
    private UserProfileRepository userProfileRepository;

    private AutoCompleteTextView mapTypeDropdown;
    private AutoCompleteTextView locationFreqDropdown;
    private AutoCompleteTextView themeDropdown;
    private MaterialSwitch switchSafeZones;
    private MaterialCheckBox sosSms, sosCall, sosRecord, sosSiren, sosFlash;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        prefsHelper = new SharedPreferencesHelper(this);
        userProfileRepository = new UserProfileRepository(getApplication());

        setupToolbar();
        initializeViews();
        setupDropdowns();
        loadAndDisplayCurrentSettings();
        setupListeners();
    }
    private boolean isDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        int iconColor = isDarkMode() ? Color.WHITE : Color.BLACK;
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.setNavigationIconTint(iconColor);
    }

    private void initializeViews() {
        mapTypeDropdown = findViewById(R.id.map_type_dropdown);
        locationFreqDropdown = findViewById(R.id.location_freq_dropdown);
        themeDropdown = findViewById(R.id.theme_dropdown);
        switchSafeZones = findViewById(R.id.switch_safe_zones);

        sosSms = findViewById(R.id.sos_sms);
        sosCall = findViewById(R.id.sos_call);
        sosRecord = findViewById(R.id.sos_record);
        sosSiren = findViewById(R.id.sos_siren);
        sosFlash = findViewById(R.id.sos_flash);
    }

    private void setupDropdowns() {
        String[] mapTypes = {"Default", "Satellite", "Terrain"};
        ArrayAdapter<String> mapAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, mapTypes);
        mapTypeDropdown.setAdapter(mapAdapter);

        String[] frequencies = {"Low", "Medium", "High"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, frequencies);
        locationFreqDropdown.setAdapter(freqAdapter);

        String[] themes = {"Light", "Dark", "System Default"};
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, themes);
        themeDropdown.setAdapter(themeAdapter);
    }

    private void loadAndDisplayCurrentSettings() {
        int mapType = prefsHelper.getMapType();
        if (mapType == GoogleMap.MAP_TYPE_SATELLITE) mapTypeDropdown.setText("Satellite", false);
        else if (mapType == GoogleMap.MAP_TYPE_TERRAIN) mapTypeDropdown.setText("Terrain", false);
        else mapTypeDropdown.setText("Default", false);

        int freq = prefsHelper.getLocationFrequency();
        if (freq == 0) locationFreqDropdown.setText("Low", false);
        else if (freq == 2) locationFreqDropdown.setText("High", false);
        else locationFreqDropdown.setText("Medium", false);

        int themeMode = prefsHelper.getTheme();
        ArrayAdapter<String> themeAdapter = (ArrayAdapter<String>) themeDropdown.getAdapter();
        if (themeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            themeDropdown.setText(themeAdapter.getItem(0), false);
        } else if (themeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            themeDropdown.setText(themeAdapter.getItem(1), false);
        } else {
            themeDropdown.setText(themeAdapter.getItem(2), false);
        }

        themeDropdown.setAdapter(themeAdapter);

        switchSafeZones.setChecked(prefsHelper.getShowSafeZones());
        sosSms.setChecked(prefsHelper.getSosSmsEnabled());
        sosCall.setChecked(prefsHelper.getSosCallEnabled());
        sosRecord.setChecked(prefsHelper.getSosRecordEnabled());
        sosSiren.setChecked(prefsHelper.getSosSirenEnabled());
        sosFlash.setChecked(prefsHelper.getSosFlashEnabled());
    }

    private void setupListeners() {
        mapTypeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            int selectedMapType = GoogleMap.MAP_TYPE_NORMAL;
            if (position == 1) selectedMapType = GoogleMap.MAP_TYPE_SATELLITE;
            if (position == 2) selectedMapType = GoogleMap.MAP_TYPE_TERRAIN;
            prefsHelper.setMapType(selectedMapType);
        });

        locationFreqDropdown.setOnItemClickListener((parent, view, position, id) -> {
            prefsHelper.setLocationFrequency(position);
        });

        themeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            int selectedTheme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (position == 0) selectedTheme = AppCompatDelegate.MODE_NIGHT_NO;
            if (position == 1) selectedTheme = AppCompatDelegate.MODE_NIGHT_YES;

            if (prefsHelper.getTheme() != selectedTheme) {
                prefsHelper.setTheme(selectedTheme);
                AppCompatDelegate.setDefaultNightMode(selectedTheme);
                recreate();
            }
        });

        switchSafeZones.setOnCheckedChangeListener((buttonView, isChecked) -> prefsHelper.setShowSafeZones(isChecked));
        sosSms.setOnCheckedChangeListener((buttonView, isChecked) -> prefsHelper.setSosSmsEnabled(isChecked));
        sosCall.setOnCheckedChangeListener((buttonView, isChecked) -> prefsHelper.setSosCallEnabled(isChecked));
        sosRecord.setOnCheckedChangeListener((buttonView, isChecked) -> prefsHelper.setSosRecordEnabled(isChecked));
        sosSiren.setOnCheckedChangeListener((buttonView, isChecked) -> prefsHelper.setSosSirenEnabled(isChecked));
        sosFlash.setOnCheckedChangeListener((buttonView, isChecked) -> prefsHelper.setSosFlashEnabled(isChecked));

        findViewById(R.id.item_contact_us).setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:vedantadsul2007@gmail.com"));
            startActivity(Intent.createChooser(emailIntent, "Send Email"));
        });
        findViewById(R.id.item_about_us).setOnClickListener(v -> startActivity(new Intent(SettingActivity.this, AboutActivity.class)));
        findViewById(R.id.item_privacy_policy).setOnClickListener(v -> {
            try {
                InputStream input = getAssets().open("SafePath_PrivacyPolicy_Terms.pdf");
                File outFile = new File(getCacheDir(), "SafePath_PrivacyPolicy_Terms.pdf");
                FileOutputStream output = new FileOutputStream(outFile);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                input.close();
                output.close();

                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", outFile);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        MaterialButton deleteAccountButton = findViewById(R.id.button_delete_account);
        deleteAccountButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? This will remove all your data and cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Error: No user is currently logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        userProfileRepository.deleteUserAccount(user, success -> {
            if (success) {
                Toast.makeText(this, "Account deleted successfully.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Failed to delete account. Please try logging in again.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
