package com.example.attendease.ui.student;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendease.R;
import com.example.attendease.databinding.ItemCalendarDayBinding;
import com.example.attendease.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    public interface OnDateClickListener {
        void onDateClick(String dateStr);
    }

    private List<CalendarDay> days = new ArrayList<>();
    private OnDateClickListener listener;

    public CalendarAdapter(OnDateClickListener listener) {
        this.listener = listener;
    }

    public void setDays(List<CalendarDay> days) {
        this.days = days;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarDayBinding binding = ItemCalendarDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DayViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        holder.bind(days.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {

        private final ItemCalendarDayBinding binding;

        DayViewHolder(ItemCalendarDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CalendarDay day, OnDateClickListener listener) {
            Context ctx = binding.getRoot().getContext();

            if (day.isPadding()) {
                binding.tvDay.setText("");
                binding.tvDay.setBackground(null);
                binding.dotLecture.setVisibility(View.INVISIBLE);
                binding.dotExam.setVisibility(View.INVISIBLE);
                binding.dotRecess.setVisibility(View.INVISIBLE);
                binding.getRoot().setOnClickListener(null);
                return;
            }

            binding.tvDay.setText(String.valueOf(day.dayOfMonth));

            // Background circle: selected > today > none
            if (day.isSelected) {
                binding.tvDay.setBackground(
                        ContextCompat.getDrawable(ctx, R.drawable.calendar_day_selected_bg));
                binding.tvDay.setTextColor(ContextCompat.getColor(ctx, R.color.white));
            } else if (day.isToday) {
                binding.tvDay.setBackground(
                        ContextCompat.getDrawable(ctx, R.drawable.calendar_day_today_bg));
                binding.tvDay.setTextColor(ContextCompat.getColor(ctx, R.color.colorPrimary));
            } else {
                binding.tvDay.setBackground(null);
                binding.tvDay.setTextColor(ContextCompat.getColor(ctx, android.R.color.black));
            }

            // Dot indicators
            boolean hasLecture = day.dotTypes.contains(Constants.LESSON_TYPE_LECTURE);
            boolean hasExam    = day.dotTypes.contains(Constants.LESSON_TYPE_EXAM);
            boolean hasRecess  = day.dotTypes.contains(Constants.LESSON_TYPE_RECESS);

            binding.dotLecture.setVisibility(hasLecture ? View.VISIBLE : View.INVISIBLE);
            binding.dotExam.setVisibility(hasExam ? View.VISIBLE : View.INVISIBLE);
            binding.dotRecess.setVisibility(hasRecess ? View.VISIBLE : View.INVISIBLE);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null && day.dateStr != null) {
                    listener.onDateClick(day.dateStr);
                }
            });
        }
    }
}
