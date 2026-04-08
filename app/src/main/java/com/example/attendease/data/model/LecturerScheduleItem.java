package com.example.attendease.data.model;

import androidx.room.ColumnInfo;

/** One row in the Lecturer Home "today's schedule" list. */
public class LecturerScheduleItem {

    @ColumnInfo(name = "lesson_id")
    public long lessonId;

    @ColumnInfo(name = "module_name")
    public String moduleName;

    @ColumnInfo(name = "lesson_number")
    public int lessonNumber;

    public String date;

    @ColumnInfo(name = "start_time")
    public String startTime;

    @ColumnInfo(name = "end_time")
    public String endTime;

    public String venue;

    @ColumnInfo(name = "is_active")
    public boolean isActive;
}
