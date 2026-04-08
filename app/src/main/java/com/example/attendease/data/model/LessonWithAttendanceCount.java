package com.example.attendease.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import com.example.attendease.data.local.Lesson;

/** Query result: one Lesson row plus the count of attendance records for it. */
public class LessonWithAttendanceCount {

    @Embedded
    public Lesson lesson;

    @ColumnInfo(name = "attendance_count")
    public int attendanceCount;
}
