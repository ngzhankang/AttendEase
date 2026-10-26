package com.example.attendease.util;

import android.content.Context;

import com.example.attendease.data.local.AppDatabase;
import com.example.attendease.data.model.StudentAttendanceSummary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ExportUtils {
    public static File exportModuleAttendance(Context context, AppDatabase db, long moduleId) {
        File file = new File(context.getExternalFilesDir(null), "attendance_module_" + moduleId + ".csv");
        try (FileWriter writer = new FileWriter(file)) {
            writer.append("Student ID,Name,Attended,Total Lessons,Percentage\n");

            List<StudentAttendanceSummary> list = db.attendanceDao()
                    .getOverallAttendanceForModuleSync(moduleId);

            if (list != null) {
                for (StudentAttendanceSummary s : list) {
                    double percent = s.getPercentage();
                    writer.append(String.format("%s,%s,%d,%d,%.2f%%\n",
                            s.studentId, s.name, s.attended, s.totalLessons, percent));
                }
            }
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
