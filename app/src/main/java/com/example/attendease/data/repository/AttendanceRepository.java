package com.example.attendease.data.repository;

import com.example.attendease.data.local.AppDatabase;
import com.example.attendease.data.local.Attendance;
import com.example.attendease.data.local.AttendanceDao;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AttendanceRepository {

    private final AttendanceDao attendanceDao;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public AttendanceRepository(AppDatabase db) {
        this.attendanceDao = db.attendanceDao();
    }

    public void insert(Attendance attendance) {
        executor.execute(() -> attendanceDao.insert(attendance));
    }

    public int getCountSync(String studentId, long moduleId) {
        return attendanceDao.getAttendanceCount(studentId, moduleId);
    }
}
