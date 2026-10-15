package com.vedant.safepath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.vedant.safepath.Data.UserProfileRepository;
import com.vedant.safepath.Fragments.EmergencyFragment;
import com.vedant.safepath.Fragments.GuideFragment;
import com.vedant.safepath.Fragments.MapFragment;
import com.vedant.safepath.Model.SharedPreferencesHelper;
import com.vedant.safepath.NotificationPopup; // Import the new popup class

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends BaseActivity {

    private boolean isBackPressedOnce = false;
    private final Handler exitHandler = new Handler(Looper.getMainLooper());

    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private UserProfileRepository userProfileRepository;
    private ActivityResultLauncher<String[]> permissionRequestLauncher;

    private final MapFragment mapFragment = new MapFragment();
    private final EmergencyFragment emergencyFragment = new EmergencyFragment();
    private final GuideFragment guideFragment = new GuideFragment();
    private Fragment activeFragment = mapFragment;
    private View noInternetPage;
    private FrameLayout fragmentContainer;
    private boolean isNetworkConnected = true;
    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        FirebaseInAppMessaging.getInstance().setMessagesSuppressed(false);
        FirebaseInAppMessaging.getInstance().triggerEvent("user_logged_in");

        userProfileRepository = new UserProfileRepository(getApplication());

        initializeFirebaseAndGoogleSignIn();
        initializeViews();
        initializePermissionLauncher();

        setupFragments();
        setupBottomNavigation();
        checkAndRequestPermissions();
        setupThemeListener();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isBackPressedOnce) {
                    finishAffinity();
                    return;
                }
                isBackPressedOnce = true;
                Toast.makeText(HomeActivity.this, "Press again to exit", Toast.LENGTH_SHORT).show();
                exitHandler.postDelayed(() -> isBackPressedOnce = false, 2000);
            }
        });
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.homebottomnavigationview);
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        fragmentContainer = findViewById(R.id.homeframelayout);
        noInternetPage = findViewById(R.id.no_internet_view);
        if (noInternetPage != null) {
            Button retryButton = noInternetPage.findViewById(R.id.btnretry);
            retryButton.setOnClickListener(v -> networkManager.checkNetworkAndDispatch());
        }
    }
    @Override
    protected void onNetworkLost() {
        isNetworkConnected = false;
        invalidateOptionsMenu();

        if (activeFragment instanceof GuideFragment) {
            if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
            if (noInternetPage != null && noInternetPage.getVisibility() == View.GONE) {
                Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_from_right);
                noInternetPage.startAnimation(slideIn);
                noInternetPage.setVisibility(View.VISIBLE);
            }
        } else {
            if (noInternetPage != null) noInternetPage.setVisibility(View.GONE);
            if (fragmentContainer != null) fragmentContainer.setVisibility(View.VISIBLE);
            showBottomPopup();
        }
    }

    @Override
    protected void onNetworkAvailable() {
        super.onNetworkAvailable();
        isNetworkConnected = true;
        invalidateOptionsMenu();
        if (noInternetPage != null && noInternetPage.getVisibility() == View.VISIBLE) {
            Animation slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_to_left);
            slideOut.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}
                @Override public void onAnimationEnd(Animation animation) {
                    noInternetPage.setVisibility(View.GONE);
                }
            });
            noInternetPage.startAnimation(slideOut);
            if (fragmentContainer != null) fragmentContainer.setVisibility(View.VISIBLE);
        }
    }
    private void initializeFirebaseAndGoogleSignIn() {
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logoutItem = menu.findItem(R.id.menulogout);
        if (logoutItem != null) {
            logoutItem.setEnabled(isNetworkConnected);
        }
        return super.onPrepareOptionsMenu(menu);
    }
    private void setupFragments() {
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().add(R.id.homeframelayout, guideFragment, "3").hide(guideFragment).commit();
        fm.beginTransaction().add(R.id.homeframelayout, emergencyFragment, "2").hide(emergencyFragment).commit();
        fm.beginTransaction().add(R.id.homeframelayout, mapFragment, "1").commit();
    }
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.bottomnavmap) {
                showFragment(mapFragment);
            } else if (itemId == R.id.bottomnavemergency) {
                showFragment(emergencyFragment);
            } else if (itemId == R.id.bottomnavguide) {
                showFragment(guideFragment);
            }
            networkManager.checkNetworkAndDispatch();
            return true;
        });
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().hide(activeFragment).show(fragment).commit();
        activeFragment = fragment;
    }


    private void initializePermissionLauncher() {
        permissionRequestLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            Boolean fineLocationGranted = permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false);
            if (fineLocationGranted && mapFragment.isAdded()) {
                mapFragment.onLocationPermissionGranted();
            } else {
                Toast.makeText(this, "Location permission is required for map features.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            permissionRequestLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        MenuItem profile = menu.findItem(R.id.menumyprofile);
        MenuItem notification = menu.findItem(R.id.menunotification);

        int iconColor = isDarkMode() ? Color.WHITE : Color.BLACK;

        profile.setIconTintList(ColorStateList.valueOf(iconColor));
        notification.setIconTintList(ColorStateList.valueOf(iconColor));

        return true;
    }

    private boolean isDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setupThemeListener() {
        themeListener = (sharedPreferences, key) -> {
            if (key != null && key.equals(SharedPreferencesHelper.PREF_THEME)) {
                invalidateOptionsMenu();
            }
        };
        getSharedPreferences("SafePathSettings", MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(themeListener);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menunotification) {
            // --- MODIFICATION START ---
            // Find the view of the menu item to use it as an anchor.
            // This needs to be done carefully. We find the toolbar and then the specific view.
            View anchorView = findViewById(R.id.topAppBar).findViewById(R.id.menunotification);
            if (anchorView != null) {
                NotificationPopup.show(this, anchorView);
            }
            // --- MODIFICATION END ---
            return true;
        } else if (id == R.id.menumyprofile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.menusetting) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        } else if (id == R.id.menulogout) {
            showLogoutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> signOut())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void signOut() {
        userProfileRepository.clearLocalProfileData();

        SharedPreferences loginPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE);
        loginPreferences.edit().putBoolean("isLoggedIn", false).apply();

        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Toast.makeText(HomeActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (themeListener != null) {
            getSharedPreferences("SafePathSettings", MODE_PRIVATE)
                    .unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }
}
