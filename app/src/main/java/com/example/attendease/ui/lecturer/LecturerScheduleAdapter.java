package com.example.attendease.ui.lecturer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendease.data.model.LecturerScheduleItem;
import com.example.attendease.databinding.ItemLecturerScheduleBinding;
import com.example.attendease.R;

public class LecturerScheduleAdapter
        extends ListAdapter<LecturerScheduleItem, LecturerScheduleAdapter.ViewHolder> {

    public LecturerScheduleAdapter() {
        super(DIFF);
    }

    private static final DiffUtil.ItemCallback<LecturerScheduleItem> DIFF =
            new DiffUtil.ItemCallback<LecturerScheduleItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull LecturerScheduleItem a,
                                               @NonNull LecturerScheduleItem b) {
                    return a.lessonId == b.lessonId;
                }
                @Override
                public boolean areContentsTheSame(@NonNull LecturerScheduleItem a,
                                                  @NonNull LecturerScheduleItem b) {
                    return a.isActive == b.isActive &&
                           a.moduleName.equals(b.moduleName) &&
                           a.lessonNumber == b.lessonNumber;
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLecturerScheduleBinding b = ItemLecturerScheduleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemLecturerScheduleBinding b;

        ViewHolder(ItemLecturerScheduleBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(LecturerScheduleItem item) {
            b.tvModuleName.setText(item.moduleName);
            b.tvLessonNumber.setText(
                    b.getRoot().getContext().getString(R.string.lecturer_lesson_format, item.lessonNumber));
            b.tvTimeVenue.setText(
                    b.getRoot().getContext().getString(R.string.time_range_format,
                            item.startTime, item.endTime) + "  •  " + item.venue);

            b.viewActiveStripe.setVisibility(item.isActive ? View.VISIBLE : View.GONE);
            b.tvActiveBadge.setVisibility(item.isActive ? View.VISIBLE : View.GONE);
        }
    }
}
