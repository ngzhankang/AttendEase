package com.example.attendease.ui.lecturer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendease.R;
import com.example.attendease.data.model.StudentAttendanceSummary;

import java.util.ArrayList;
import java.util.List;

public class OverallAttendanceAdapter extends RecyclerView.Adapter<OverallAttendanceAdapter.ViewHolder> {

    private List<StudentAttendanceSummary> list = new ArrayList<>();

    public void submitList(List<StudentAttendanceSummary> newList) {
        list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_overall_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentAttendanceSummary s = list.get(position);
        double percent = s.getPercentage();
        holder.tvName.setText(s.name);
        holder.tvAttendance.setText(String.format("%d / %d (%.1f%%)", s.attended, s.totalLessons, percent));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvAttendance;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvAttendance = itemView.findViewById(R.id.tvAttendance);
        }
    }
}
