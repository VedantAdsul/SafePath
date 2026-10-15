package com.vedant.safepath;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    protected NetworkManager networkManager;
    private View topBanner;
    private View bottomPopup;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        networkManager = new NetworkManager(this);

        networkManager.getNetworkAvailability().observe(this, isAvailable -> {
            if (isAvailable) {
                onNetworkAvailable();
            } else {
                onNetworkLost();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        networkManager.registerCallback();
    }

    @Override
    protected void onStop() {
        super.onStop();
        networkManager.unregisterCallback();
    }
    @Override
    protected void onResume() {
        super.onResume();
        networkManager.checkNetworkAndDispatch();
    }

    protected void onNetworkAvailable() {
        hideTopBanner();
        hideBottomPopup();
    }

    protected void onNetworkLost() {
    }

    private ViewGroup getRootView() {
        return (ViewGroup) findViewById(android.R.id.content);
    }

    protected void showTopBanner() {
        if (topBanner == null) {
            topBanner = getLayoutInflater().inflate(R.layout.layout_top_banner, getRootView(), false);
            getRootView().addView(topBanner);
        }
        if (topBanner.getVisibility() == View.GONE) {
            topBanner.setBackgroundColor(Color.parseColor("#D32F2F"));
            ((TextView) topBanner.findViewById(R.id.top_banner_text)).setText("No Internet Connection");

            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            topBanner.startAnimation(slideDown);
            topBanner.setVisibility(View.VISIBLE);
        }
    }

    protected void hideTopBanner() {
        if (topBanner != null && topBanner.getVisibility() == View.VISIBLE) {
            TextView bannerText = topBanner.findViewById(R.id.top_banner_text);
            bannerText.setText("Back Online!");
            topBanner.setBackgroundColor(Color.parseColor("#4CAF50"));

            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            slideUp.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}
                @Override public void onAnimationEnd(Animation animation) {
                    topBanner.setVisibility(View.GONE);
                    bannerText.setText("No Internet Connection");
                    topBanner.setBackgroundColor(Color.parseColor("#D32F2F"));
                }
            });
            topBanner.startAnimation(slideUp);
        }
    }

    protected void showBottomPopup() {
        if (bottomPopup == null) {
            bottomPopup = getLayoutInflater().inflate(R.layout.layout_bottom_popup, getRootView(), false);
            getRootView().addView(bottomPopup);
        }
        if (bottomPopup.getVisibility() == View.GONE) {
            bottomPopup.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F")));
            ((TextView) bottomPopup.findViewById(R.id.bottom_popup_text)).setText("You are offline. Please check your connection.");

            Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_from_bottom);
            bottomPopup.startAnimation(slideIn);
            bottomPopup.setVisibility(View.VISIBLE);
        }
    }

    protected void hideBottomPopup() {
        if (bottomPopup != null && bottomPopup.getVisibility() == View.VISIBLE) {
            TextView popupText = bottomPopup.findViewById(R.id.bottom_popup_text);
            popupText.setText("Back Online!");
            bottomPopup.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

            Animation slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_to_bottom);
            slideOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    bottomPopup.setVisibility(View.GONE);
                    popupText.setText("You are offline. Please check your connection.");
                    bottomPopup.setBackgroundColor(Color.parseColor("#D32F2F"));
                }
            });
            bottomPopup.startAnimation(slideOut);
        }
    }
}
