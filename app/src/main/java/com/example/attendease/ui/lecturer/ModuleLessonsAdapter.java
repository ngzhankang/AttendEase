package com.example.attendease.ui.lecturer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendease.R;
import com.example.attendease.data.local.Module;
import com.example.attendease.data.model.LessonWithAttendanceCount;
import com.example.attendease.databinding.ItemLessonRowBinding;
import com.example.attendease.databinding.ItemModuleHeaderBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Flat-list adapter with two view types:
 *  TYPE_MODULE (0) — module header with expand/collapse chevron
 *  TYPE_LESSON (1) — individual lesson row with action buttons
 */
public class ModuleLessonsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_MODULE = 0;
    public static final int TYPE_LESSON = 1;

    /** Wraps a Module with its enrolled-student count and expanded state. */
    public static class ModuleItem {
        public final Module module;
        public final int enrolledCount;
        public final boolean expanded;

        public ModuleItem(Module module, int enrolledCount, boolean expanded) {
            this.module = module;
            this.enrolledCount = enrolledCount;
            this.expanded = expanded;
        }
    }

    public interface Listener {
        void onModuleHeaderClicked(Module module);
        void onStartAttendance(LessonWithAttendanceCount item);
        void onStopAttendance(LessonWithAttendanceCount item);
        void onAddManual(LessonWithAttendanceCount item, long moduleId);
        void onViewAttendance(LessonWithAttendanceCount item);
        void onViewOverallAttendance(Module module);
        void onExportAttendance(Module module);
    }

    private final List<Object> items = new ArrayList<>();  // ModuleItem | LessonWithAttendanceCount
    private final Listener listener;
    private final String today;

    // moduleId for each lesson row (set when building list)
    private final List<Long> itemModuleIds = new ArrayList<>();

    public ModuleLessonsAdapter(Listener listener, String today) {
        this.listener = listener;
        this.today = today;
    }

    public void submitItems(List<Object> newItems, List<Long> moduleIds) {
        items.clear();
        items.addAll(newItems);
        itemModuleIds.clear();
        itemModuleIds.addAll(moduleIds);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof ModuleItem ? TYPE_MODULE : TYPE_LESSON;
    }

    @Override
    public int getItemCount() { return items.size(); }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_MODULE) {
            return new ModuleViewHolder(ItemModuleHeaderBinding.inflate(inflater, parent, false));
        }
        return new LessonViewHolder(ItemLessonRowBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ModuleViewHolder) {
            ((ModuleViewHolder) holder).bind((ModuleItem) items.get(position));
        } else {
            long moduleId = position < itemModuleIds.size() ? itemModuleIds.get(position) : 0L;
            ((LessonViewHolder) holder).bind((LessonWithAttendanceCount) items.get(position), moduleId);
        }
    }

    // ── Module header ViewHolder ─────────────────────────────────────

    class ModuleViewHolder extends RecyclerView.ViewHolder {
        private final ItemModuleHeaderBinding b;

        ModuleViewHolder(ItemModuleHeaderBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(ModuleItem item) {
            Context ctx = b.getRoot().getContext();
            b.tvModuleName.setText(item.module.moduleName);
            b.tvEnrolledCount.setText(
                    ctx.getString(R.string.enrolled_count_format, item.enrolledCount));
            b.tvChevron.setText(item.expanded ? "▲" : "▼");

            b.btnOverallAttendance.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewOverallAttendance(item.module);
                }
            });

            b.btnExport.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onExportAttendance(item.module);
                }
            });

            b.getRoot().setOnClickListener(v -> listener.onModuleHeaderClicked(item.module));
        }
    }

    // ── Lesson row ViewHolder ────────────────────────────────────────

    class LessonViewHolder extends RecyclerView.ViewHolder {
        private final ItemLessonRowBinding b;

        LessonViewHolder(ItemLessonRowBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(LessonWithAttendanceCount item, long moduleId) {
            Context ctx = b.getRoot().getContext();

            // Status icon
            String statusIcon;
            if (item.lesson.isActive) {
                statusIcon = ctx.getString(R.string.status_live);
            } else if (item.lesson.date.compareTo(today) < 0) {
                statusIcon = ctx.getString(R.string.status_done);
            } else {
                statusIcon = ctx.getString(R.string.status_upcoming);
            }
            b.tvStatus.setText(statusIcon);

            b.tvLessonNumber.setText(
                    ctx.getString(R.string.lecturer_lesson_format, item.lesson.lessonNumber));

            b.tvDateTimeVenue.setText(
                    item.lesson.date + "  " +
                    ctx.getString(R.string.time_range_format, item.lesson.startTime, item.lesson.endTime) +
                    "  •  " + item.lesson.venue);

            b.tvAttendanceCount.setText(
                    ctx.getString(R.string.attendance_count_format, item.attendanceCount));

            // Start/Stop button — only for today's lesson
            boolean isToday = today.equals(item.lesson.date);
            if (isToday || item.lesson.isActive) {
                b.btnStartStop.setVisibility(View.VISIBLE);
                if (item.lesson.isActive) {
                    b.btnStartStop.setText(R.string.btn_stop_attendance);
                    b.btnStartStop.setOnClickListener(v -> listener.onStopAttendance(item));
                } else {
                    b.btnStartStop.setText(R.string.btn_start_attendance);
                    b.btnStartStop.setOnClickListener(v -> listener.onStartAttendance(item));
                }
            } else {
                b.btnStartStop.setVisibility(View.GONE);
            }

            b.btnAddManual.setOnClickListener(v -> listener.onAddManual(item, moduleId));

            b.btnViewAttendance.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewAttendance(item);
                }
            });
        }
    }
}
