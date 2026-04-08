package com.example.attendease.data.model;

import androidx.room.ColumnInfo;

public class UpcomingLessonInfo {

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
}
