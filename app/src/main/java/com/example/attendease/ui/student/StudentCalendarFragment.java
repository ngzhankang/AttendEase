package com.example.attendease.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.attendease.R;
import com.example.attendease.data.model.CalendarDotInfo;
import com.example.attendease.data.model.DayLessonInfo;
import com.example.attendease.databinding.FragmentStudentCalendarBinding;
import com.example.attendease.util.Constants;
import com.example.attendease.util.DateUtils;
import com.example.attendease.viewmodel.StudentViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StudentCalendarFragment extends Fragment {

    private FragmentStudentCalendarBinding binding;
    private StudentViewModel viewModel;
    private CalendarAdapter calendarAdapter;
    private DayLessonAdapter dayLessonAdapter;

    // Cached state for grid rebuilding
    private int[] currentYM;
    private Map<String, Set<String>> currentDotMap = new HashMap<>();
    private String currentSelectedDate;
    private final String todayStr = DateUtils.today();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(StudentViewModel.class);

        // Calendar grid (6 rows × 7 cols = 42 cells)
        calendarAdapter = new CalendarAdapter(this::onDateClicked);
        binding.rvCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        binding.rvCalendar.setAdapter(calendarAdapter);
        binding.rvCalendar.setNestedScrollingEnabled(false);

        // Day-detail list
        dayLessonAdapter = new DayLessonAdapter();
        binding.rvDayLessons.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDayLessons.setAdapter(dayLessonAdapter);
        binding.rvDayLessons.setNestedScrollingEnabled(false);

        // Month navigation buttons
        binding.btnPrevMonth.setOnClickListener(v -> viewModel.prevMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.nextMonth());

        // Observe display month
        viewModel.getDisplayMonthYM().observe(getViewLifecycleOwner(), ym -> {
            currentYM = ym;
            updateMonthHeader(ym[0], ym[1]);
            rebuildGrid();
        });

        // Observe dot data for current month
        viewModel.getCalendarDots().observe(getViewLifecycleOwner(), dots -> {
            currentDotMap = buildDotMap(dots);
            rebuildGrid();
        });

        // Observe selected date (for highlighting)
        viewModel.getSelectedDate().observe(getViewLifecycleOwner(), date -> {
            currentSelectedDate = date;
            rebuildGrid();
            updateDetailHeader(date);
        });

        // Observe lessons for selected day
        viewModel.getDayLessons().observe(getViewLifecycleOwner(), lessons -> {
            dayLessonAdapter.setLessons(lessons);
            boolean empty = lessons == null || lessons.isEmpty();
            binding.tvNoEvents.setVisibility(
                    (currentSelectedDate != null && !currentSelectedDate.isEmpty() && empty)
                            ? View.VISIBLE : View.GONE);
            binding.rvDayLessons.setVisibility(
                    (!empty) ? View.VISIBLE : View.GONE);
        });
    }

    private void onDateClicked(String dateStr) {
        viewModel.selectDate(dateStr);
    }

    private void updateMonthHeader(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        binding.tvMonthYear.setText(sdf.format(cal.getTime()));
    }

    private void updateDetailHeader(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            binding.tvSelectedDate.setText(R.string.tap_date_hint);
            binding.tvNoEvents.setVisibility(View.GONE);
            binding.rvDayLessons.setVisibility(View.GONE);
            return;
        }
        try {
            SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outFmt = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault());
            binding.tvSelectedDate.setText(outFmt.format(inFmt.parse(dateStr)));
        } catch (Exception e) {
            binding.tvSelectedDate.setText(dateStr);
        }
    }

    /** Convert flat list of CalendarDotInfo into Map<date, Set<type>>. */
    private Map<String, Set<String>> buildDotMap(List<CalendarDotInfo> dots) {
        Map<String, Set<String>> map = new HashMap<>();
        if (dots == null) return map;
        for (CalendarDotInfo dot : dots) {
            if (dot.date == null) continue;
            Set<String> types = map.get(dot.date);
            if (types == null) {
                types = new HashSet<>();
                map.put(dot.date, types);
            }
            types.add(dot.type);
        }
        return map;
    }

    /** Build and submit the 42-cell grid whenever any input changes. */
    private void rebuildGrid() {
        if (currentYM == null) return;
        int year = currentYM[0], month = currentYM[1];

        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);

        // Monday-first offset: Calendar.MONDAY == 2; offset = (dayOfWeek - 2 + 7) % 7
        int firstDow = cal.get(Calendar.DAY_OF_WEEK);
        int startPad = (firstDow - Calendar.MONDAY + 7) % 7;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        List<CalendarDay> grid = new ArrayList<>(42);

        // Padding before 1st
        for (int i = 0; i < startPad; i++) {
            grid.add(new CalendarDay());
        }

        // Days of month
        for (int d = 1; d <= daysInMonth; d++) {
            String dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month, d);
            CalendarDay cell = new CalendarDay();
            cell.dayOfMonth = d;
            cell.dateStr = dateStr;
            cell.isToday = dateStr.equals(todayStr);
            cell.isSelected = dateStr.equals(currentSelectedDate);
            Set<String> types = currentDotMap.get(dateStr);
            if (types != null) cell.dotTypes = new ArrayList<>(types);
            grid.add(cell);
        }

        // Pad to 42
        while (grid.size() < 42) {
            grid.add(new CalendarDay());
        }

        calendarAdapter.setDays(grid);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
