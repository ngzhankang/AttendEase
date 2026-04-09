package com.example.attendease.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendease.R;
import com.example.attendease.databinding.ActivitySplashBinding;
import com.example.attendease.ui.lecturer.LecturerMainActivity;
import com.example.attendease.ui.student.StudentMainActivity;
import com.example.attendease.util.Constants;

public class SplashActivity extends AppCompatActivity {

    // delay a 1.5s in the very beginning loading page (splashactivity) before app routes to login
    private static final int SPLASH_DELAY_MS = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new Handler(Looper.getMainLooper()).postDelayed(this::routeFromSession, SPLASH_DELAY_MS);
    }

    private void routeFromSession() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String userId = prefs.getString(Constants.KEY_LOGGED_IN_USER_ID, null);
        String role   = prefs.getString(Constants.KEY_ROLE, null);

        Intent intent;
        if (userId != null && !userId.isEmpty() && role != null) {
            intent = Constants.ROLE_LECTURER.equals(role)
                    ? new Intent(this, LecturerMainActivity.class)
                    : new Intent(this, StudentMainActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        startActivity(intent);
        applyFadeTransition();
        finish();
    }

    private void applyFadeTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out);
        } else {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }
}
