package com.example.attendease.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.attendease.data.local.AppDatabase;
import com.example.attendease.data.local.User;
import com.example.attendease.util.Constants;
import com.example.attendease.util.HashUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SharedProfileViewModel extends AndroidViewModel {

    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

    private final AppDatabase db;
    private final String staffId;
    private final String role;
    private final LiveData<User> currentUser;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SharedProfileViewModel(Application application) {
        super(application);
        SharedPreferences prefs = application.getSharedPreferences(
                Constants.PREFS_NAME, Context.MODE_PRIVATE);
        staffId = prefs.getString(Constants.KEY_LOGGED_IN_USER_ID, "");
        role    = prefs.getString(Constants.KEY_ROLE, Constants.ROLE_STUDENT);
        db = AppDatabase.getInstance(application);
        currentUser = db.userDao().observeUserByStaffId(staffId);
    }

    public LiveData<User> getCurrentUser() { return currentUser; }
    public String getRole() { return role; }

    public void changePassword(String currentPwd, String newPwd, ActionCallback callback) {
        executor.execute(() -> {
            User user = db.userDao().getUserByStaffId(staffId);
            if (user == null) {
                post(callback, false, "User not found");
                return;
            }
            if (!user.passwordHash.equals(HashUtils.sha256(currentPwd))) {
                post(callback, false, "Current password is incorrect");
                return;
            }
            user.passwordHash = HashUtils.sha256(newPwd);
            db.userDao().update(user);
            post(callback, true, null);
        });
    }

    /** Resets the password to the default seed value "password". */
    public void resetPassword(ActionCallback callback) {
        executor.execute(() -> {
            User user = db.userDao().getUserByStaffId(staffId);
            if (user == null) {
                post(callback, false, "User not found");
                return;
            }
            user.passwordHash = HashUtils.sha256("password");
            db.userDao().update(user);
            post(callback, true, null);
        });
    }

    /** Clears the session from SharedPreferences. The caller must then finish the activity. */
    public void logout() {
        getApplication().<android.app.Application>getApplicationContext()
                .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply();
    }

    private void post(ActionCallback cb, boolean success, String error) {
        mainHandler.post(() -> {
            if (success) cb.onSuccess();
            else cb.onError(error);
        });
    }
}
