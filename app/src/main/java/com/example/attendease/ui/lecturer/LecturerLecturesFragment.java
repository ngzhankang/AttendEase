package com.example.attendease.ui.lecturer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendease.R;
import com.example.attendease.data.local.AppDatabase;
import com.example.attendease.data.local.Module;
import com.example.attendease.data.model.LessonWithAttendanceCount;
import com.example.attendease.databinding.DialogManualAttendanceBinding;
import com.example.attendease.databinding.FragmentLecturerLecturesBinding;
import com.example.attendease.ui.nfc.NfcScanActivity;
import com.example.attendease.util.DateUtils;
import com.example.attendease.util.ExportUtils;
import com.example.attendease.viewmodel.LecturerViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class LecturerLecturesFragment extends Fragment {

    private FragmentLecturerLecturesBinding binding;
    private LecturerViewModel viewModel;
    private ModuleLessonsAdapter adapter;

    private List<Module> currentModules = new ArrayList<>();
    private final Set<Long> expandedModuleIds = new HashSet<>();
    private final Map<Long, List<LessonWithAttendanceCount>> loadedLessons = new HashMap<>();
    private final Map<Long, Integer> enrolledCounts = new HashMap<>();
    // Track observed LiveDatas to avoid duplicate observers
    private final Map<Long, Observer<List<LessonWithAttendanceCount>>> lessonObservers = new HashMap<>();

    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLecturerLecturesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getInstance(requireContext());

        viewModel = new ViewModelProvider(requireActivity()).get(LecturerViewModel.class);

        adapter = new ModuleLessonsAdapter(new ModuleLessonsAdapter.Listener() {
            @Override
            public void onModuleHeaderClicked(Module module) {
                toggleModule(module);
            }

            @Override
            public void onStartAttendance(LessonWithAttendanceCount item) {
                viewModel.setLessonActive(item.lesson.lessonId, true);
                Intent intent = new Intent(requireContext(), NfcScanActivity.class);
                intent.putExtra(NfcScanActivity.EXTRA_LESSON_ID, item.lesson.lessonId);
                intent.putExtra(NfcScanActivity.EXTRA_MODULE_ID, item.lesson.moduleId);
                startActivity(intent);
            }

            @Override
            public void onStopAttendance(LessonWithAttendanceCount item) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.stop_attendance_title)
                        .setMessage(R.string.stop_attendance_message)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.btn_stop_attendance, (d, w) ->
                                viewModel.setLessonActive(item.lesson.lessonId, false))
                        .show();
            }

            @Override
            public void onAddManual(LessonWithAttendanceCount item, long moduleId) {
                showManualAttendanceDialog(item.lesson.lessonId, moduleId);
            }

            @Override
            public void onViewAttendance(LessonWithAttendanceCount item) {
                showAttendanceList(item);
            }

            @Override
            public void onViewOverallAttendance(Module module) {
                showOverallAttendanceDialog(module);
            }

            @Override
            public void onExportAttendance(Module module) {
                exportModuleAttendance(module);
            }
        }, DateUtils.today());

        binding.rvModuleLessons.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvModuleLessons.setAdapter(adapter);

        viewModel.getModules().observe(getViewLifecycleOwner(), modules -> {
            if (modules == null) return;
            currentModules = modules;
            // Load enrolled counts in background
            Executors.newSingleThreadExecutor().execute(() -> {
                for (Module m : modules) {
                    if (!enrolledCounts.containsKey(m.moduleId)) {
                        enrolledCounts.put(m.moduleId, viewModel.getEnrolledCountSync(m.moduleId));
                    }
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::rebuildList);
                }
            });
        });
    }

    private void toggleModule(Module module) {
        long moduleId = module.moduleId;
        if (expandedModuleIds.contains(moduleId)) {
            expandedModuleIds.remove(moduleId);
            rebuildList();
        } else {
            expandedModuleIds.add(moduleId);
            if (!lessonObservers.containsKey(moduleId)) {
                Observer<List<LessonWithAttendanceCount>> obs = lessons -> {
                    loadedLessons.put(moduleId, lessons);
                    rebuildList();
                };
                lessonObservers.put(moduleId, obs);
                viewModel.getLessonsForModule(moduleId).observe(getViewLifecycleOwner(), obs);
            } else {
                rebuildList();
            }
        }
    }

    private void rebuildList() {
        List<Object> flatItems = new ArrayList<>();
        List<Long> moduleIds = new ArrayList<>();

        for (Module m : currentModules) {
            boolean expanded = expandedModuleIds.contains(m.moduleId);
            int enrolled = enrolledCounts.containsKey(m.moduleId)
                    ? enrolledCounts.get(m.moduleId) : 0;
            flatItems.add(new ModuleLessonsAdapter.ModuleItem(m, enrolled, expanded));
            moduleIds.add(0L); // module header has no moduleId context needed

            if (expanded) {
                List<LessonWithAttendanceCount> lessons = loadedLessons.get(m.moduleId);
                if (lessons != null) {
                    for (LessonWithAttendanceCount lesson : lessons) {
                        flatItems.add(lesson);
                        moduleIds.add(m.moduleId);
                    }
                }
            }
        }

        adapter.submitItems(flatItems, moduleIds);
    }

    private void showManualAttendanceDialog(long lessonId, long moduleId) {
        DialogManualAttendanceBinding dialogBinding =
                DialogManualAttendanceBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.manual_attendance_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.btn_add, null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {
                            String studentId = dialogBinding.etStudentId.getText() != null
                                    ? dialogBinding.etStudentId.getText().toString().trim() : "";
                            if (TextUtils.isEmpty(studentId)) {
                                dialogBinding.tilStudentId.setError(
                                        getString(R.string.error_field_required));
                                return;
                            }
                            dialogBinding.tilStudentId.setError(null);
                            viewModel.addManualAttendance(lessonId, moduleId, studentId,
                                    new LecturerViewModel.ManualAttendanceCallback() {
                                        @Override
                                        public void onSuccess(String studentName) {
                                            dialog.dismiss();
                                            showSnackbar(getString(
                                                    R.string.attendance_added_success, studentName));
                                        }

                                        @Override
                                        public void onAlreadyAttended(String studentName) {
                                            dialogBinding.tilStudentId.setError(
                                                    getString(R.string.error_already_attended, studentName));
                                        }

                                        @Override
                                        public void onNotEnrolled() {
                                            dialogBinding.tilStudentId.setError(
                                                    getString(R.string.error_not_enrolled));
                                        }

                                        @Override
                                        public void onError(String message) {
                                            dialogBinding.tilStudentId.setError(
                                                    getString(R.string.error_student_not_found));
                                        }
                                    });
                        }));

        dialog.show();
    }

    private void showOverallAttendanceDialog(Module module) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_overall_attendance, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rvOverallAttendance);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        OverallAttendanceAdapter adapter = new OverallAttendanceAdapter();
        recyclerView.setAdapter(adapter);

        AlertDialog dialog = builder.setView(dialogView)
                .setTitle("Overall Attendance - " + module.moduleName)
                .setPositiveButton("Close", null)
                .create();

        viewModel.getOverallAttendanceForModule(module.moduleId)
                .observe(getViewLifecycleOwner(), students -> {
                    if (students != null) {
                        adapter.submitList(students);
                    }
                });

        dialog.show();
    }

    private void exportModuleAttendance(Module module) {
        Executors.newSingleThreadExecutor().execute(() -> {
            File csv = ExportUtils.exportModuleAttendance(requireContext(), db, module.moduleId);
            if (csv != null && csv.exists()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    // Share the file
                    Uri uri = FileProvider.getUriForFile(requireContext(),
                            requireContext().getPackageName() + ".fileprovider",
                            csv);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/csv");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(share, "Export Attendance"));
                });
            } else {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void showSnackbar(String msg) {
        if (binding != null) Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showAttendanceList(LessonWithAttendanceCount lesson) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_attendance_list, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rvAttendanceList);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        AttendanceListAdapter adapter = new AttendanceListAdapter();
        recyclerView.setAdapter(adapter);

        AlertDialog dialog = builder.setView(dialogView)
                .setTitle("Attendance for Lesson " + lesson.lesson.lessonNumber)
                .setPositiveButton("Close", null)
                .create();

        viewModel.getAttendanceForLesson(lesson.lesson.lessonId)
                .observe(getViewLifecycleOwner(), students -> {
                    if (students != null) {
                        adapter.submitList(students);
                    }
                });

        dialog.show();
    }
}
