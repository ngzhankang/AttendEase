package com.example.attendease.data.model;

public class StudentAttendanceSummary {
    public String studentId;
    public String name;
    public int attended;
    public int totalLessons;

    public double getPercentage() {
        if (totalLessons == 0) return 0;
        return (attended * 100.0) / totalLessons;
    }
}
