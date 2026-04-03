package com.example.attendease.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.attendease.data.local.AppDatabase;
import com.example.attendease.data.local.Module;
import com.example.attendease.data.model.CalendarDotInfo;
import com.example.attendease.data.model.DayLessonInfo;
import com.example.attendease.data.model.ModuleAttendanceInfo;
import com.example.attendease.data.model.UpcomingLessonInfo;
import com.example.attendease.util.Constants;
import com.example.attendease.util.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class StudentViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final String studentId;

    // --- Home ---
    private final LiveData<List<UpcomingLessonInfo>> upcomingLessons;
    private final LiveData<Boolean> isAttendanceLive;
    private final MediatorLiveData<List<ModuleAttendanceInfo>> attendanceOverview;

    // --- Calendar ---
    // int[2]: [0]=year, [1]=month (1–12)
    private final MutableLiveData<int[]> displayMonthYM;
    private final LiveData<List<CalendarDotInfo>> calendarDots;
    private final MutableLiveData<String> selectedDate = new MutableLiveData<>();
    private final LiveData<List<DayLessonInfo>> dayLessons;

    public StudentViewModel(Application application) {
        super(application);

        SharedPreferences prefs = application.getSharedPreferences(
                Constants.PREFS_NAME, Context.MODE_PRIVATE);
        studentId = prefs.getString(Constants.KEY_LOGGED_IN_USER_ID, "1006001");

        db = AppDatabase.getInstance(application);

        // --- Home ---
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

        // --- Calendar ---
        Calendar today = Calendar.getInstance();
        displayMonthYM = new MutableLiveData<>(
                new int[]{today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1});

        calendarDots = Transformations.switchMap(displayMonthYM, ym -> {
            String start = String.format(Locale.US, "%04d-%02d-01", ym[0], ym[1]);
            String end = endOfMonth(ym[0], ym[1]);
            return db.lessonDao().getCalendarDots(studentId, start, end);
        });

        dayLessons = Transformations.switchMap(selectedDate, date -> {
            if (date == null || date.isEmpty()) {
                MutableLiveData<List<DayLessonInfo>> empty = new MutableLiveData<>();
                empty.setValue(Collections.emptyList());
                return empty;
            }
            return db.lessonDao().getLessonsForDate(studentId, date);
        });
    }

    // ---- Home getters ----

    public LiveData<List<UpcomingLessonInfo>> getUpcomingLessons() { return upcomingLessons; }
    public LiveData<Boolean> isAttendanceLive() { return isAttendanceLive; }
    public LiveData<List<ModuleAttendanceInfo>> getAttendanceOverview() { return attendanceOverview; }
    public String getStudentId() { return studentId; }

    // ---- Calendar getters & actions ----

    public LiveData<int[]> getDisplayMonthYM() { return displayMonthYM; }
    public LiveData<List<CalendarDotInfo>> getCalendarDots() { return calendarDots; }
    public LiveData<String> getSelectedDate() { return selectedDate; }
    public LiveData<List<DayLessonInfo>> getDayLessons() { return dayLessons; }

    public void prevMonth() {
        int[] ym = displayMonthYM.getValue();
        if (ym == null) return;
        int year = ym[0], month = ym[1];
        month--;
        if (month < 1) { month = 12; year--; }
        displayMonthYM.setValue(new int[]{year, month});
    }

    public void nextMonth() {
        int[] ym = displayMonthYM.getValue();
        if (ym == null) return;
        int year = ym[0], month = ym[1];
        month++;
        if (month > 12) { month = 1; year++; }
        displayMonthYM.setValue(new int[]{year, month});
    }

    public void selectDate(String dateStr) {
        selectedDate.setValue(dateStr);
    }

    // ---- Helpers ----

    /** Returns the last date of the given month as "yyyy-MM-dd". */
    private static String endOfMonth(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, lastDay);
    }
}
