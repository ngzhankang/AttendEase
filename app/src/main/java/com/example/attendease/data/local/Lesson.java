package com.example.attendease.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "lessons")
public class Lesson {

    @PrimaryKey(autoGenerate = true)
    public long lessonId;

    @ColumnInfo(name = "module_id")
    public long moduleId;

    @ColumnInfo(name = "lesson_number")
    public int lessonNumber;

    @NonNull
    public String date;

    @NonNull
    @ColumnInfo(name = "start_time")
    public String startTime;

    @NonNull
    @ColumnInfo(name = "end_time")
    public String endTime;

    public String venue;

    public String type;

    @ColumnInfo(name = "is_active")
    public boolean isActive;

    public Lesson(long moduleId, int lessonNumber, @NonNull String date,
                  @NonNull String startTime, @NonNull String endTime,
                  String venue, String type) {
        this.moduleId = moduleId;
        this.lessonNumber = lessonNumber;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.venue = venue;
        this.type = type;
        this.isActive = false;
    }
}
