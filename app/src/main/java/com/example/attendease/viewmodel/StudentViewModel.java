package com.example.attendease.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;

import com.example.attendease.data.local.AppDatabase;
import com.example.attendease.data.local.Module;
import com.example.attendease.data.model.ModuleAttendanceInfo;
import com.example.attendease.data.model.UpcomingLessonInfo;
import com.example.attendease.util.Constants;
import com.example.attendease.util.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class StudentViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final String studentId;

    private final LiveData<List<UpcomingLessonInfo>> upcomingLessons;
    private final LiveData<Boolean> isAttendanceLive;
    private final MediatorLiveData<List<ModuleAttendanceInfo>> attendanceOverview;

    public StudentViewModel(Application application) {
        super(application);

        SharedPreferences prefs = application.getSharedPreferences(
                Constants.PREFS_NAME, Context.MODE_PRIVATE);
        studentId = prefs.getString(Constants.KEY_LOGGED_IN_USER_ID, "1006001");

        db = AppDatabase.getInstance(application);

        upcomingLessons = db.lessonDao().getUpcomingLessons(studentId, DateUtils.today());

        isAttendanceLive = Transformations.map(
                db.lessonDao().getActiveLessonCount(studentId),
                count -> count != null && count > 0
        );

        attendanceOverview = new MediatorLiveData<>();
        LiveData<List<Module>> modulesLd = db.moduleDao().getModulesForStudent(studentId);
        attendanceOverview.addSource(modulesLd, modules -> {
            if (modules == null) return;
            Executors.newSingleThreadExecutor().execute(() -> {
                List<ModuleAttendanceInfo> list = new ArrayList<>();
                for (Module m : modules) {
                    int count = db.attendanceDao().getAttendanceCount(studentId, m.moduleId);
                    ModuleAttendanceInfo info = new ModuleAttendanceInfo();
                    info.moduleId = m.moduleId;
                    info.moduleName = m.moduleName;
                    info.attended = count;
                    info.total = 48;
                    list.add(info);
                }
                attendanceOverview.postValue(list);
            });
        });
    }

    public LiveData<List<UpcomingLessonInfo>> getUpcomingLessons() {
        return upcomingLessons;
    }

    public LiveData<Boolean> isAttendanceLive() {
        return isAttendanceLive;
    }

    public LiveData<List<ModuleAttendanceInfo>> getAttendanceOverview() {
        return attendanceOverview;
    }

    public String getStudentId() {
        return studentId;
    }
}
