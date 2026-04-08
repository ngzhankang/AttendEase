package com.example.attendease.data.repository;

import com.example.attendease.data.local.AppDatabase;
import com.example.attendease.data.local.User;
import com.example.attendease.data.local.UserDao;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UserRepository {

    private final UserDao userDao;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public UserRepository(AppDatabase db) {
        this.userDao = db.userDao();
    }

    public User getUserSync(String staffId) {
        return userDao.getUserByStaffId(staffId);
    }

    public void updateUser(User user) {
        executor.execute(() -> userDao.update(user));
    }
}
