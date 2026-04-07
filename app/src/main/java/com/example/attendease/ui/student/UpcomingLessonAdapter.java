package com.example.attendease.ui.student;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendease.data.model.UpcomingLessonInfo;
import com.example.attendease.databinding.ItemUpcomingLessonBinding;

public class UpcomingLessonAdapter extends ListAdapter<UpcomingLessonInfo, UpcomingLessonAdapter.ViewHolder> {

    public UpcomingLessonAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<UpcomingLessonInfo> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<UpcomingLessonInfo>() {
                @Override
                public boolean areItemsTheSame(@NonNull UpcomingLessonInfo a, @NonNull UpcomingLessonInfo b) {
                    return a.lessonId == b.lessonId;
                }

                @Override
                public boolean areContentsTheSame(@NonNull UpcomingLessonInfo a, @NonNull UpcomingLessonInfo b) {
                    return a.lessonId == b.lessonId
                            && a.date.equals(b.date)
                            && a.startTime.equals(b.startTime);
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUpcomingLessonBinding binding = ItemUpcomingLessonBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemUpcomingLessonBinding binding;

        ViewHolder(ItemUpcomingLessonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(UpcomingLessonInfo info) {
            binding.tvModuleName.setText(info.moduleName);
            binding.tvLessonNumber.setText(
                    binding.getRoot().getContext().getString(
                            com.example.attendease.R.string.lesson_number_format,
                            info.lessonNumber));
            binding.tvTimeRange.setText(
                    binding.getRoot().getContext().getString(
                            com.example.attendease.R.string.time_range_format,
                            info.startTime, info.endTime));
            binding.tvVenue.setText(info.venue != null ? info.venue : "");
        }
    }
}
