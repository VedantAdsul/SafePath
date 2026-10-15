package com.vedant.safepath;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.vedant.safepath.Data.UserProfileRepository;
import com.vedant.safepath.Model.UserProfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class RegistrationActivity extends AppCompatActivity {

    private TextInputLayout emailLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText emailEditText, passwordEditText, confirmPasswordEditText;
    private MaterialButton registerButton;
    private TextView loginLink, termsConditions;
    CheckBox termsCheckbox;

    private FirebaseAuth mAuth;
    private UserProfileRepository userProfileRepository;
    private static final String TAG = "RegistrationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        FirebaseInAppMessaging.getInstance().setMessagesSuppressed(true);

        mAuth = FirebaseAuth.getInstance();
        userProfileRepository = new UserProfileRepository(getApplication());

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        emailLayout = findViewById(R.id.email_layout);
        passwordLayout = findViewById(R.id.password_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);
        emailEditText = findViewById(R.id.email_register);
        passwordEditText = findViewById(R.id.pass_register);
        confirmPasswordEditText = findViewById(R.id.confirm_pass_register);
        registerButton = findViewById(R.id.button_register);
        loginLink = findViewById(R.id.new_login);
        termsCheckbox = findViewById(R.id.terms_checkbox);
        termsConditions = findViewById(R.id.terms_condition);
    }

    private void setupListeners() {
        registerButton.setOnClickListener(v -> registerUserWithEmail());
        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
        });

        termsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            registerButton.setEnabled(isChecked);
        });
        termsConditions.setOnClickListener(v -> {
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


    }

    private boolean validateForm() {
        boolean isValid = true;
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Valid email is required.");
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        if (TextUtils.isEmpty(password) || password.length() < 8) {
            passwordLayout.setError("Password must be at least 8 characters.");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match.");
            isValid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }
        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void registerUserWithEmail() {
        if (!validateForm()) return;

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        handleSuccessfulRegistration(mAuth.getCurrentUser());
                    } else {
                        Toast.makeText(RegistrationActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleSuccessfulRegistration(FirebaseUser user) {
        if (user == null) return;

        String uid = user.getUid();
        String email = user.getEmail() != null ? user.getEmail() : "";
        String name = user.getDisplayName() != null ? user.getDisplayName() : "";

        UserProfile profile = new UserProfile(uid, name, email, "", "", "", "", "","");

        userProfileRepository.upsertProfile(profile, success -> {
            if (success) {
                Log.d(TAG, "Minimal profile created and saved for new user.");
            } else {
                Log.w(TAG, "Failed to save minimal profile for new user.");
            }
            navigateToHome();
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(RegistrationActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
