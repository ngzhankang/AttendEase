package com.example.attendease.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "attendance",
        indices = {@Index(value = {"lesson_id", "student_id"}, unique = true)})
public class Attendance {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "lesson_id")
    public long lessonId;

    @NonNull
    @ColumnInfo(name = "student_id")
    public String studentId;

    public long timestamp;

    public String method;

    public Attendance(long lessonId, @NonNull String studentId, long timestamp, String method) {
        this.lessonId = lessonId;
        this.studentId = studentId;
        this.timestamp = timestamp;
        this.method = method;
    }
}
