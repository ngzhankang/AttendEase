package com.example.attendease.ui.student;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendease.R;
import com.example.attendease.data.model.DayLessonInfo;
import com.example.attendease.databinding.ItemDayLessonBinding;
import com.example.attendease.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class DayLessonAdapter extends RecyclerView.Adapter<DayLessonAdapter.ViewHolder> {

    private List<DayLessonInfo> lessons = new ArrayList<>();

    public void setLessons(List<DayLessonInfo> lessons) {
        this.lessons = lessons != null ? lessons : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDayLessonBinding binding = ItemDayLessonBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(lessons.get(position));
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemDayLessonBinding binding;

        ViewHolder(ItemDayLessonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(DayLessonInfo info) {
            binding.tvModuleName.setText(info.moduleName);
            binding.tvLessonNumber.setText(
                    binding.getRoot().getContext().getString(
                            R.string.lesson_number_format, info.lessonNumber));
            binding.tvTimeRange.setText(
                    binding.getRoot().getContext().getString(
                            R.string.time_range_format, info.startTime, info.endTime));
            binding.tvVenue.setText(info.venue != null ? info.venue : "");

            // Type badge
            int typeColor;
            int typeLabel;
            if (Constants.LESSON_TYPE_EXAM.equals(info.type)) {
                typeColor = R.color.colorSuccess;
                typeLabel = R.string.type_exam;
            } else if (Constants.LESSON_TYPE_RECESS.equals(info.type)) {
                typeColor = R.color.colorRecess;
                typeLabel = R.string.type_recess;
            } else {
                typeColor = R.color.colorPrimary;
                typeLabel = R.string.type_lecture;
            }
            binding.tvType.setText(typeLabel);
            binding.tvType.setTextColor(
                    ContextCompat.getColor(binding.getRoot().getContext(), typeColor));
        }
    }
}
