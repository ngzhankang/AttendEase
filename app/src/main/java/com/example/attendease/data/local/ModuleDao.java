package com.example.attendease.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ModuleDao {

    @Query("SELECT m.* FROM modules m " +
           "INNER JOIN enrollments e ON m.moduleId = e.module_id " +
           "WHERE e.student_id = :studentId")
    LiveData<List<Module>> getModulesForStudent(String studentId);

    @Query("SELECT * FROM modules WHERE lecturer_id = :lecturerId")
    LiveData<List<Module>> getModulesForLecturer(String lecturerId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Module module);
}
