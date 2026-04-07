package com.example.attendease.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.attendease.data.local.AppDatabase;
import com.example.attendease.data.local.Attendance;
import com.example.attendease.data.local.Module;
import com.example.attendease.data.local.User;
import com.example.attendease.data.model.LecturerScheduleItem;
import com.example.attendease.data.model.LessonWithAttendanceCount;
import com.example.attendease.util.Constants;
import com.example.attendease.util.DateUtils;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LecturerViewModel extends AndroidViewModel {

    private final String lecturerId;
    private final AppDatabase db;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private final LiveData<List<LecturerScheduleItem>> todaySchedule;
    private final LiveData<List<Module>> modules;

    public LecturerViewModel(@NonNull Application app) {
        super(app);
        SharedPreferences prefs = app.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        lecturerId = prefs.getString(Constants.KEY_LOGGED_IN_USER_ID, "1000001");
        db = AppDatabase.getInstance(app);
        todaySchedule = db.lessonDao().getTodayLessonsForLecturer(lecturerId, DateUtils.today());
        modules = db.moduleDao().getModulesForLecturer(lecturerId);
    }

    public LiveData<List<LecturerScheduleItem>> getTodaySchedule() { return todaySchedule; }

    public LiveData<List<Module>> getModules() { return modules; }

    public LiveData<List<LessonWithAttendanceCount>> getLessonsForModule(long moduleId) {
        return db.lessonDao().getLessonsWithAttendanceCount(moduleId);
    }

    /** Returns enrolled student count synchronously — call only from a background thread. */
    public int getEnrolledCountSync(long moduleId) {
        return db.enrollmentDao().getEnrolledCountForModule(moduleId);
    }

    public void setLessonActive(long lessonId, boolean active) {
        executor.execute(() -> db.lessonDao().setActive(lessonId, active));
    }

    /**
     * Add a manual attendance record for a student.
     * Validates: student exists, enrolled in module, not already attended.
     */
    public void addManualAttendance(long lessonId, long moduleId,
                                    String studentId, ManualAttendanceCallback callback) {
        executor.execute(() -> {
            Handler h = new Handler(Looper.getMainLooper());
            User user = db.userDao().getUserByStaffId(studentId);
            if (user == null) {
                h.post(() -> callback.onError(null));
                return;
            }
            List<String> enrolled = db.enrollmentDao().getStudentIdsForModule(moduleId);
            if (!enrolled.contains(studentId)) {
                h.post(() -> callback.onNotEnrolled());
                return;
            }
            Attendance existing = db.attendanceDao().getRecord(lessonId, studentId);
            if (existing != null) {
                h.post(() -> callback.onAlreadyAttended(user.name));
                return;
            }
            Attendance attendance = new Attendance(
                    lessonId, studentId, System.currentTimeMillis(), "MANUAL");
            db.attendanceDao().insert(attendance);
            h.post(() -> callback.onSuccess(user.name));
        });
    }

    public interface ManualAttendanceCallback {
        void onSuccess(String studentName);
        void onAlreadyAttended(String studentName);
        void onNotEnrolled();
        void onError(String message);
    }
}
