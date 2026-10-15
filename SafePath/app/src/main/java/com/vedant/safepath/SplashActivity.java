package com.vedant.safepath;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        TextView tagline = findViewById(R.id.splash_tagline);

        FirebaseInAppMessaging.getInstance().setMessagesSuppressed(true);

        logo.setAlpha(0f);
        logo.setScaleX(0.85f);
        logo.setScaleY(0.85f);
        tagline.setAlpha(0f);

        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1000)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        tagline.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, SPLASH_DURATION);
    }
}
