package com.example.attendease.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EnrollmentDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Enrollment enrollment);

    @Query("SELECT student_id FROM enrollments WHERE module_id = :moduleId")
    List<String> getStudentIdsForModule(long moduleId);

    @Query("SELECT COUNT(*) FROM enrollments WHERE module_id = :moduleId")
    int getEnrolledCountForModule(long moduleId);
}
