package com.example.attendease.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendease.R;
import com.example.attendease.data.local.AppDatabase;
import com.example.attendease.data.local.User;
import com.example.attendease.databinding.ActivityLoginBinding;
import com.example.attendease.ui.lecturer.LecturerMainActivity;
import com.example.attendease.ui.student.StudentMainActivity;
import com.example.attendease.util.Constants;
import com.example.attendease.util.HashUtils;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Student is the default selected role
        binding.toggleRole.check(R.id.btnRoleStudent);

        binding.toggleRole.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) updateIdHint(checkedId);
        });

        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        // Allow IME "Done" to trigger login
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            attemptLogin();
            return true;
        });
    }

    private void updateIdHint(int checkedButtonId) {
        binding.tilUserId.setHint(checkedButtonId == R.id.btnRoleStudent
                ? getString(R.string.label_student_id)
                : getString(R.string.label_staff_id));
    }

    private void attemptLogin() {
        hideKeyboard();

        String id       = getText(binding.etUserId);
        String password = getText(binding.etPassword);
        boolean valid   = true;

        if (TextUtils.isEmpty(id)) {
            binding.tilUserId.setError(getString(R.string.error_field_required));
            valid = false;
        } else {
            binding.tilUserId.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_field_required));
            valid = false;
        } else {
            binding.tilPassword.setError(null);
        }

        if (!valid) return;

        binding.tvLoginError.setVisibility(View.GONE);
        binding.btnLogin.setEnabled(false);
        binding.progressLogin.setVisibility(View.VISIBLE);

        boolean isStudent = binding.toggleRole.getCheckedButtonId() == R.id.btnRoleStudent;
        String expectedRole = isStudent ? Constants.ROLE_STUDENT : Constants.ROLE_LECTURER;
        String passwordHash = HashUtils.sha256(password);

        Executors.newSingleThreadExecutor().execute(() -> {
            User user = AppDatabase.getInstance(this).userDao().getUserByStaffId(id);

            handler.post(() -> {
                binding.progressLogin.setVisibility(View.GONE);
                binding.btnLogin.setEnabled(true);

                boolean authenticated = user != null
                        && expectedRole.equals(user.role)
                        && passwordHash.equals(user.passwordHash);

                if (!authenticated) {
                    binding.tvLoginError.setVisibility(View.VISIBLE);
                    return;
                }

                // Persist session
                getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putString(Constants.KEY_LOGGED_IN_USER_ID, user.studentOrStaffId)
                        .putString(Constants.KEY_ROLE, user.role)
                        .apply();

                // Navigate — clear back stack so user cannot return to Login
                Class<?> dest = Constants.ROLE_LECTURER.equals(user.role)
                        ? LecturerMainActivity.class
                        : StudentMainActivity.class;
                Intent intent = new Intent(this, dest);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN,
                            R.anim.fade_in, R.anim.fade_out);
                } else {
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                }
            });
        });
    }

    private static String getText(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }
}
