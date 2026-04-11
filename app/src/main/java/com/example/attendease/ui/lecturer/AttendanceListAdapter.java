package com.example.attendease.ui.lecturer;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendease.R;
import com.example.attendease.data.model.StudentAttendanceStatus;

import java.util.ArrayList;
import java.util.List;

public class AttendanceListAdapter extends RecyclerView.Adapter<AttendanceListAdapter.ViewHolder> {

    private List<StudentAttendanceStatus> students = new ArrayList<>();

    public void submitList(List<StudentAttendanceStatus> list) {
        this.students = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_status, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentAttendanceStatus s = students.get(position);
        holder.tvName.setText(s.name);
        holder.ivPresent.setImageResource(s.present
                ? android.R.drawable.checkbox_on_background
                : android.R.drawable.checkbox_off_background);
    }

    @Override
    public int getItemCount() { return students.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView ivPresent;
        ViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            ivPresent = itemView.findViewById(R.id.ivPresent);
        }
    }
}
