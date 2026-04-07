package com.example.attendease.ui.student;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.attendease.R;
import com.example.attendease.data.model.ModuleAttendanceInfo;
import com.example.attendease.databinding.FragmentStudentHomeBinding;
import com.example.attendease.databinding.ItemAttendanceOverviewBinding;
import com.example.attendease.viewmodel.StudentViewModel;

import java.util.List;

public class StudentHomeFragment extends Fragment {

    private FragmentStudentHomeBinding binding;
    private StudentViewModel viewModel;
    private ObjectAnimator pulseAnimator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(StudentViewModel.class);

        UpcomingLessonAdapter adapter = new UpcomingLessonAdapter();
        binding.rvUpcomingLessons.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUpcomingLessons.setAdapter(adapter);
        binding.rvUpcomingLessons.setNestedScrollingEnabled(false);

        viewModel.isAttendanceLive().observe(getViewLifecycleOwner(), live -> {
            if (live != null && live) {
                binding.cardBanner.setVisibility(View.VISIBLE);
                startPulseAnimation();
            } else {
                binding.cardBanner.setVisibility(View.GONE);
                stopPulseAnimation();
            }
        });

        viewModel.getUpcomingLessons().observe(getViewLifecycleOwner(), lessons -> {
            adapter.submitList(lessons);
            boolean empty = lessons == null || lessons.isEmpty();
            binding.tvNoUpcoming.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.rvUpcomingLessons.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getAttendanceOverview().observe(getViewLifecycleOwner(),
                this::updateAttendanceOverview);
    }

    private void startPulseAnimation() {
        if (pulseAnimator != null && pulseAnimator.isRunning()) return;
        pulseAnimator = ObjectAnimator.ofFloat(binding.cardBanner, "alpha", 1f, 0.5f, 1f);
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        binding.cardBanner.setAlpha(1f);
    }

    private void updateAttendanceOverview(List<ModuleAttendanceInfo> infos) {
        binding.llAttendanceOverview.removeAllViews();
        if (infos == null) return;

        for (ModuleAttendanceInfo info : infos) {
            ItemAttendanceOverviewBinding item = ItemAttendanceOverviewBinding.inflate(
                    getLayoutInflater(), binding.llAttendanceOverview, false);

            item.tvModuleName.setText(info.moduleName);
            item.tvFraction.setText(getString(R.string.attendance_fraction_format,
                    info.attended, info.total));
            item.progressBar.setMax(info.total);
            item.progressBar.setProgress(info.attended);

            float pct = info.total > 0 ? (info.attended * 100f / info.total) : 0f;
            int color;
            if (pct >= 80f) {
                color = requireContext().getColor(R.color.colorSuccess);
            } else if (pct >= 60f) {
                color = requireContext().getColor(R.color.colorWarning);
            } else {
                color = requireContext().getColor(R.color.colorError);
            }
            item.progressBar.setIndicatorColor(color);

            binding.llAttendanceOverview.addView(item.getRoot());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPulseAnimation();
        binding = null;
    }
}
