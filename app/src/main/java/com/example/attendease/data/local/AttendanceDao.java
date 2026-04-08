package com.example.attendease.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Attendance attendance);
}
