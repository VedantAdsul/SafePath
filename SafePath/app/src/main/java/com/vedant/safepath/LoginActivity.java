package com.vedant.safepath;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.vedant.safepath.Data.UserProfileRepository;
import com.vedant.safepath.Model.UserProfile;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailEditText, passwordEditText;
    private TextView forgotPassword, newRegisterLink;
    private MaterialButton loginButton, googleSignInButton;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private UserProfileRepository userProfileRepository;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        userProfileRepository = new UserProfileRepository(getApplication());

        FirebaseInAppMessaging.getInstance().setMessagesSuppressed(true);

        initializeViews();
        configureGoogleSignIn();
        setupListeners();
    }

    private void initializeViews() {
        emailLayout = findViewById(R.id.email_layout);
        passwordLayout = findViewById(R.id.password_layout);
        emailEditText = findViewById(R.id.email_login);
        passwordEditText = findViewById(R.id.pass_login);
        forgotPassword = findViewById(R.id.forgotPassword);
        loginButton = findViewById(R.id.button_login);
        newRegisterLink = findViewById(R.id.new_register_link);
        googleSignInButton = findViewById(R.id.google_sign_in_button);
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            Log.w(TAG, "Google sign in failed", e);
                            Toast.makeText(this, "Google Sign-In Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> loginUserWithEmail());
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        newRegisterLink.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegistrationActivity.class)));
        forgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    private boolean validateForm() {
        boolean isValid = true;
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Valid email is required.");
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required.");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }
        return isValid;
    }

    private void loginUserWithEmail() {
        if (!validateForm()) return;

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Login Successful.", Toast.LENGTH_SHORT).show();
                        handleSuccessfulLogin(mAuth.getCurrentUser());
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Google Sign-In successful.", Toast.LENGTH_SHORT).show();
                        handleSuccessfulLogin(mAuth.getCurrentUser());
                    } else {
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleSuccessfulLogin(FirebaseUser user) {
        if (user == null) return;
        String uid = user.getUid();

        FirebaseDatabase.getInstance().getReference("users").child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                userProfileRepository.syncProfileFromServer(uid, this::navigateToHome);
            } else {
                Log.d(TAG, "No profile found in DB. Creating a minimal profile.");
                String name = user.getDisplayName() != null ? user.getDisplayName() : "";
                String email = user.getEmail() != null ? user.getEmail() : "";
                String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

                UserProfile newProfile = new UserProfile(uid, name, email, "", "", "", "", photoUrl,"");
                userProfileRepository.upsertProfile(newProfile, success -> {
                    if (success) {
                        Log.d(TAG, "Minimal profile created and saved.");
                    } else {
                        Log.w(TAG, "Failed to save minimal profile.");
                    }
                    navigateToHome();
                });
            }
        });
    }

    private void navigateToHome() {
        Intent homeIntent = new Intent(LoginActivity.this, HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
        finish();
    }

    private void handleForgotPassword() {
        String email = emailEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter your registered email to reset password.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Password reset email sent.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Failed to send reset email.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
