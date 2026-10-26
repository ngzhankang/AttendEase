package com.example.attendease.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.attendease.data.model.StudentAttendanceStatus;
import com.example.attendease.data.model.StudentAttendanceSummary;

import java.util.List;

@Dao
public interface AttendanceDao {

    @Query("SELECT COUNT(*) FROM attendance a " +
           "INNER JOIN lessons l ON a.lesson_id = l.lessonId " +
           "WHERE a.student_id = :studentId AND l.module_id = :moduleId")
    int getAttendanceCount(String studentId, long moduleId);

    @Query("SELECT * FROM attendance WHERE lesson_id = :lessonId AND student_id = :studentId LIMIT 1")
    Attendance getRecord(long lessonId, String studentId);

    @Query("SELECT COUNT(*) FROM attendance WHERE lesson_id = :lessonId")
    int getAttendanceCountForLesson(long lessonId);

    @Query("SELECT u.student_or_staff_id AS studentId, u.name, " +
            "CASE WHEN a.lesson_id IS NOT NULL THEN 1 ELSE 0 END AS present " +
            "FROM users u " +
            "INNER JOIN enrollments e ON u.student_or_staff_id = e.student_id " +
            "LEFT JOIN attendance a ON a.student_id = u.student_or_staff_id AND a.lesson_id = :lessonId " +
            "WHERE e.module_id = (SELECT module_id FROM lessons WHERE lessonId = :lessonId) " +
            "AND u.role = 'STUDENT' " +
            "ORDER BY u.name")
    LiveData<List<StudentAttendanceStatus>> getAttendanceForLesson(long lessonId);

    @Query("SELECT u.student_or_staff_id AS studentId, u.name, " +
            "COUNT(a.lesson_id) AS attended, " +
            "(SELECT COUNT(*) FROM lessons l WHERE l.module_id = :moduleId) AS totalLessons " +
            "FROM users u " +
            "INNER JOIN enrollments e ON u.student_or_staff_id = e.student_id " +
            "LEFT JOIN attendance a ON a.student_id = u.student_or_staff_id " +
            "   AND a.lesson_id IN (SELECT lessonId FROM lessons WHERE module_id = :moduleId) " +
            "WHERE e.module_id = :moduleId AND u.role = 'STUDENT' " +
            "GROUP BY u.student_or_staff_id " +
            "ORDER BY u.name")
    LiveData<List<StudentAttendanceSummary>> getOverallAttendanceForModule(long moduleId);

    /** Same as getOverallAttendanceForModule, but synchronous for background export. */
    @Query("SELECT u.student_or_staff_id AS studentId, u.name, " +
            "COUNT(a.lesson_id) AS attended, " +
            "(SELECT COUNT(*) FROM lessons l WHERE l.module_id = :moduleId) AS totalLessons " +
            "FROM users u " +
            "INNER JOIN enrollments e ON u.student_or_staff_id = e.student_id " +
            "LEFT JOIN attendance a ON a.student_id = u.student_or_staff_id " +
            "   AND a.lesson_id IN (SELECT lessonId FROM lessons WHERE module_id = :moduleId) " +
            "WHERE e.module_id = :moduleId AND u.role = 'STUDENT' " +
            "GROUP BY u.student_or_staff_id " +
            "ORDER BY u.name")
    List<StudentAttendanceSummary> getOverallAttendanceForModuleSync(long moduleId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Attendance attendance);
}
