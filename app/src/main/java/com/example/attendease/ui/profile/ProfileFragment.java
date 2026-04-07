package com.example.attendease.ui.profile;

import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;

import com.example.attendease.R;
import com.example.attendease.data.local.User;
import com.example.attendease.databinding.DialogChangePasswordBinding;
import com.example.attendease.databinding.FragmentProfileBinding;
import com.example.attendease.ui.auth.LoginActivity;
import com.example.attendease.util.Constants;
import com.example.attendease.viewmodel.SharedProfileViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private SharedProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedProfileViewModel.class);

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), this::populateUser);

        boolean isStudent = Constants.ROLE_STUDENT.equals(viewModel.getRole());
        binding.labelUserId.setText(isStudent ? R.string.label_student_id : R.string.label_staff_id);
        binding.rowLid.setVisibility(isStudent ? View.VISIBLE : View.GONE);

        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.btnResetPassword.setOnClickListener(v -> showResetPasswordDialog());
        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void populateUser(User user) {
        if (user == null) return;
        String initial = user.name.isEmpty() ? "?" : String.valueOf(user.name.charAt(0)).toUpperCase();
        binding.tvAvatar.setText(initial);
        binding.tvName.setText(user.name);
        binding.tvRole.setText(Constants.ROLE_STUDENT.equals(user.role)
                ? R.string.role_student : R.string.role_lecturer);
        binding.tvUserId.setText(user.studentOrStaffId);
        binding.tvLid.setText(user.cardLid != null ? user.cardLid : "—");
    }

    // ── Change Password ──────────────────────────────────────────────

    private void showChangePasswordDialog() {
        DialogChangePasswordBinding dialogBinding =
                DialogChangePasswordBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.change_password_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.btn_change_password, null)
                .create();

        // Override positive button to prevent auto-dismiss on validation failure
        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> attemptChangePassword(dialogBinding, dialog)));

        dialog.show();
    }

    private void attemptChangePassword(DialogChangePasswordBinding b, AlertDialog dialog) {
        String current = getText(b.etCurrentPassword);
        String newPwd  = getText(b.etNewPassword);
        String confirm = getText(b.etConfirmPassword);
        boolean valid = true;

        if (TextUtils.isEmpty(current)) {
            b.tilCurrentPassword.setError(getString(R.string.error_field_required));
            valid = false;
        } else {
            b.tilCurrentPassword.setError(null);
        }
        if (TextUtils.isEmpty(newPwd)) {
            b.tilNewPassword.setError(getString(R.string.error_field_required));
            valid = false;
        } else {
            b.tilNewPassword.setError(null);
        }
        if (!newPwd.equals(confirm)) {
            b.tilConfirmPassword.setError(getString(R.string.error_passwords_no_match));
            valid = false;
        } else {
            b.tilConfirmPassword.setError(null);
        }
        if (!valid) return;

        viewModel.changePassword(current, newPwd, new SharedProfileViewModel.ActionCallback() {
            @Override
            public void onSuccess() {
                dialog.dismiss();
                showSnackbar(R.string.password_changed_success);
            }
            @Override
            public void onError(String message) {
                b.tilCurrentPassword.setError(getString(R.string.error_current_password_wrong));
            }
        });
    }

    // ── Reset Password ───────────────────────────────────────────────

    private void showResetPasswordDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reset_password_title)
                .setMessage(R.string.reset_password_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.btn_reset_password, (d, w) ->
                        viewModel.resetPassword(new SharedProfileViewModel.ActionCallback() {
                            @Override
                            public void onSuccess() { showSnackbar(R.string.password_reset_success); }
                            @Override
                            public void onError(String msg) { showSnackbar(msg); }
                        }))
                .show();
    }

    // ── Logout ───────────────────────────────────────────────────────

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.btn_logout, (d, w) -> {
                    viewModel.logout();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static String getText(
            com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void showSnackbar(int resId) {
        if (binding != null) Snackbar.make(binding.getRoot(), resId, Snackbar.LENGTH_LONG).show();
    }

    private void showSnackbar(String msg) {
        if (binding != null) Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
