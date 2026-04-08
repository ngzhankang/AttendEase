package com.example.attendease.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.attendease.data.model.CalendarDotInfo;
import com.example.attendease.data.model.DayLessonInfo;
import com.example.attendease.data.model.LecturerScheduleItem;
import com.example.attendease.data.model.LessonWithAttendanceCount;
import com.example.attendease.data.model.UpcomingLessonInfo;

import java.util.List;

@Dao
public interface LessonDao {

    // --- Home fragment ---

    @Query("SELECT l.lessonId AS lesson_id, m.module_name AS module_name, " +
           "l.lesson_number AS lesson_number, l.date, l.start_time AS start_time, " +
           "l.end_time AS end_time, l.venue " +
           "FROM lessons l " +
           "INNER JOIN modules m ON l.module_id = m.moduleId " +
           "INNER JOIN enrollments e ON m.moduleId = e.module_id " +
           "WHERE e.student_id = :studentId AND l.date >= :today " +
           "ORDER BY l.date ASC, l.start_time ASC LIMIT 5")
    LiveData<List<UpcomingLessonInfo>> getUpcomingLessons(String studentId, String today);

    @Query("SELECT COUNT(*) FROM lessons l " +
           "INNER JOIN enrollments e ON l.module_id = e.module_id " +
           "WHERE e.student_id = :studentId AND l.is_active = 1")
    LiveData<Integer> getActiveLessonCount(String studentId);

    // --- Calendar fragment ---

    /** Returns one row per lesson in range, for dot indicators. */
    @Query("SELECT l.date, l.type " +
           "FROM lessons l " +
           "INNER JOIN enrollments e ON l.module_id = e.module_id " +
           "WHERE e.student_id = :studentId " +
           "AND l.date >= :startDate AND l.date <= :endDate " +
           "ORDER BY l.date ASC")
    LiveData<List<CalendarDotInfo>> getCalendarDots(String studentId, String startDate, String endDate);

    /** All lessons on a specific date for the day-detail panel. */
    @Query("SELECT l.lessonId AS lesson_id, m.module_name AS module_name, " +
           "l.lesson_number AS lesson_number, l.date, l.start_time AS start_time, " +
           "l.end_time AS end_time, l.venue, l.type " +
           "FROM lessons l " +
           "INNER JOIN modules m ON l.module_id = m.moduleId " +
           "INNER JOIN enrollments e ON m.moduleId = e.module_id " +
           "WHERE e.student_id = :studentId AND l.date = :date " +
           "ORDER BY l.start_time ASC")
    LiveData<List<DayLessonInfo>> getLessonsForDate(String studentId, String date);

    // --- Lecturer ---

    /** Today's schedule for a lecturer across all their modules. */
    @Query("SELECT l.lessonId AS lesson_id, m.module_name AS module_name, " +
           "l.lesson_number AS lesson_number, l.date, l.start_time AS start_time, " +
           "l.end_time AS end_time, l.venue, l.is_active AS is_active " +
           "FROM lessons l " +
           "INNER JOIN modules m ON l.module_id = m.moduleId " +
           "WHERE m.lecturer_id = :lecturerId AND l.date = :today " +
           "ORDER BY l.start_time ASC")
    LiveData<List<LecturerScheduleItem>> getTodayLessonsForLecturer(String lecturerId, String today);

    /** All lessons for a module with per-lesson attendance count. */
    @Query("SELECT l.*, " +
           "(SELECT COUNT(*) FROM attendance a WHERE a.lesson_id = l.lessonId) AS attendance_count " +
           "FROM lessons l " +
           "WHERE l.module_id = :moduleId ORDER BY l.lesson_number ASC")
    LiveData<List<LessonWithAttendanceCount>> getLessonsWithAttendanceCount(long moduleId);

    @Query("SELECT * FROM lessons WHERE module_id = :moduleId ORDER BY lesson_number ASC")
    LiveData<List<Lesson>> getLessonsForModule(long moduleId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Lesson lesson);

    @Query("UPDATE lessons SET is_active = :active WHERE lessonId = :lessonId")
    void setActive(long lessonId, boolean active);
}
