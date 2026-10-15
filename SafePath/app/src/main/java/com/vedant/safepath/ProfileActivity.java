package com.vedant.safepath;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.vedant.safepath.Data.UserProfileRepository;
import com.vedant.safepath.Model.UserProfile;
import com.vedant.safepath.Viewmodel.ProfileViewModel;
import com.vedant.safepath.Viewmodel.ProfileViewModelFactory;

public class ProfileActivity extends AppCompatActivity {

    private ShapeableImageView profileImageView;
    private TextInputEditText nameEditText, emailEditText, phoneEditText, addressEditText, bloodGroupEditText, allergiesEditText, emergencyContactEditText;
    private TextInputLayout phoneLayout;;
    private MaterialButton saveButton, cancelButton;

    private FirebaseAuth mAuth;
    private ProfileViewModel profileViewModel;
    private Uri selectedImageUri;
    private UserProfile currentUserProfile;
    private String currentUid;
    private boolean isProfileComplete = false;

    private static final String PREFS_NAME = "AppSettings";
    public static final String KEY_EMERGENCY_CONTACT = "primary_emergency_contact";


    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).into(profileImageView);
                    Toast.makeText(this, "Image selected. Tap 'Save' to update.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            handleLogout();
            return;
        }
        currentUid = currentUser.getUid();

        UserProfileRepository repository = new UserProfileRepository(getApplication());
        ProfileViewModelFactory factory = new ProfileViewModelFactory(repository, currentUid);
        profileViewModel = new ViewModelProvider(this, factory).get(ProfileViewModel.class);

        initializeViews();
        setupClickListeners();
        loadAndObserveProfile();
        setupBackButtonInterceptor();
    }

    private void initializeViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        profileImageView = findViewById(R.id.profile_image);
        nameEditText = findViewById(R.id.edit_text_name);
        emailEditText = findViewById(R.id.edit_text_email);
        phoneEditText = findViewById(R.id.edit_text_phone);
        phoneLayout = findViewById(R.id.phone_layout);
        addressEditText = findViewById(R.id.edit_text_address);
        bloodGroupEditText = findViewById(R.id.edit_text_blood_group);
        allergiesEditText = findViewById(R.id.edit_text_allergies);
        emergencyContactEditText = findViewById(R.id.edit_text_emergency_contact);
        saveButton = findViewById(R.id.button_save);
        cancelButton = findViewById(R.id.button_cancel);
    }

    private void setupBackButtonInterceptor() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isProfileComplete) {
                    finish();
                } else {
                    Toast.makeText(ProfileActivity.this, "Please complete and save your profile.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupClickListeners() {
        profileImageView.setOnClickListener(v -> openImagePicker());
        findViewById(R.id.change_photo_text).setOnClickListener(v -> openImagePicker());
        saveButton.setOnClickListener(v -> saveUserProfileData());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void loadAndObserveProfile() {
        profileViewModel.getUserProfile().observe(this, userProfile -> {
            if (userProfile != null) {
                currentUserProfile = userProfile;
                updateUI(userProfile);
                checkProfileCompleteness(userProfile);
            } else {
                profileViewModel.syncProfile();
            }
        });
    }

    private void checkProfileCompleteness(UserProfile profile) {
        isProfileComplete = profile.getPhone() != null && !profile.getPhone().trim().isEmpty()
                && !profile.getName().trim().isEmpty() && profile.getEmergencyContact() != null
                && !profile.getAddress().trim().isEmpty();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(isProfileComplete);
        }
        cancelButton.setVisibility(isProfileComplete ? View.VISIBLE : View.GONE);

        if (!isProfileComplete) {
            Toast.makeText(this, "Please complete your profile to continue.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateUI(UserProfile profile) {
        nameEditText.setText(profile.getName());
        emailEditText.setText(profile.getEmail());
        phoneEditText.setText(profile.getPhone());
        addressEditText.setText(profile.getAddress());
        bloodGroupEditText.setText(profile.getBloodGroup());
        allergiesEditText.setText(profile.getAllergies());
        emergencyContactEditText.setText(profile.getEmergencyContact());

        String imageUrlString = profile.getProfileImageUrl();
        Uri imageUri = null;
        if (imageUrlString != null && !imageUrlString.isEmpty()) {
            imageUri = Uri.parse(imageUrlString);
        }

        Glide.with(this)
                .load(imageUri)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(profileImageView);
    }

    private void saveUserProfileData() {
        String phone = phoneEditText.getText().toString().trim();

        if (TextUtils.isEmpty(phone)) {
            phoneLayout.setError("Phone number is required to proceed.");
            Toast.makeText(this, "Please enter your phone number.", Toast.LENGTH_SHORT).show();
            return;
        } else {
            phoneLayout.setError(null);
        }

        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String bloodGroup = bloodGroupEditText.getText().toString().trim();
        String allergies = allergiesEditText.getText().toString().trim();
        String emergencyContact = emergencyContactEditText.getText().toString().trim();

        String photoUrlToSave = (selectedImageUri != null)
                ? selectedImageUri.toString()
                : (currentUserProfile != null ? currentUserProfile.getProfileImageUrl() : "");

        UserProfile updatedProfile = new UserProfile(
                currentUid, name, email, phone, address, bloodGroup, allergies, photoUrlToSave, emergencyContact
        );

        SharedPreferences prefs = getSharedPreferences("SafePathPrefs", MODE_PRIVATE);
        prefs.edit().putString(KEY_EMERGENCY_CONTACT, emergencyContact).apply();

        profileViewModel.updateProfile(updatedProfile, success -> {
            if (success) {
                Toast.makeText(ProfileActivity.this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                updateFirebaseAuthProfile(name, photoUrlToSave);
                startActivity(new Intent(ProfileActivity.this, HomeActivity.class));
                finish();
            } else {
                Toast.makeText(ProfileActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFirebaseAuthProfile(String name, String photoUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest.Builder profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name);
            if (photoUrl != null && !photoUrl.isEmpty()) {
                profileUpdates.setPhotoUri(Uri.parse(photoUrl));
            }
            user.updateProfile(profileUpdates.build());
        }
    }

    private void handleLogout() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
