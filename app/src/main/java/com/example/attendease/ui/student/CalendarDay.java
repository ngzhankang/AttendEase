package com.example.attendease.ui.student;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one cell in the 42-cell (6×7) calendar grid.
 * Padding cells (before the 1st and after the last day) have dayOfMonth == 0.
 */
public class CalendarDay {

    public int dayOfMonth;       // 0 = padding/empty cell
    public String dateStr;       // "yyyy-MM-dd", null for padding cells
    public boolean isToday;
    public boolean isSelected;
    public List<String> dotTypes; // lesson types present on this date

    public CalendarDay() {
        dotTypes = new ArrayList<>();
    }

    public boolean isPadding() {
        return dayOfMonth == 0;
    }
}
