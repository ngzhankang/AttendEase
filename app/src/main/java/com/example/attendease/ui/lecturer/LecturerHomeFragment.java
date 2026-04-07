package com.example.attendease.ui.lecturer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.attendease.databinding.FragmentLecturerHomeBinding;
import com.example.attendease.viewmodel.LecturerViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LecturerHomeFragment extends Fragment {

    private FragmentLecturerHomeBinding binding;
    private LecturerViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(LecturerViewModel.class);

        String dateStr = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
                .format(new Date());
        binding.tvTodayDate.setText(dateStr);

        LecturerScheduleAdapter adapter = new LecturerScheduleAdapter();
        binding.rvSchedule.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvSchedule.setAdapter(adapter);

        viewModel.getTodaySchedule().observe(getViewLifecycleOwner(), items -> {
            boolean empty = items == null || items.isEmpty();
            binding.rvSchedule.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.tvNoLessons.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (!empty) adapter.submitList(items);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
