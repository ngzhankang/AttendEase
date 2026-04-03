package com.example.attendease.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "enrollments",
        indices = {@Index(value = {"student_id", "module_id"}, unique = true)})
public class Enrollment {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "student_id")
    public String studentId;

    @ColumnInfo(name = "module_id")
    public long moduleId;

    public Enrollment(@NonNull String studentId, long moduleId) {
        this.studentId = studentId;
        this.moduleId = moduleId;
    }
}
