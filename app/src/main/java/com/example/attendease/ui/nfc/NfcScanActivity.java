package com.example.attendease.ui.nfc;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.os.Build;
import androidx.activity.OnBackPressedCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendease.R;
import com.example.attendease.data.local.AppDatabase;
import com.example.attendease.data.local.Attendance;
import com.example.attendease.data.local.User;
import com.example.attendease.databinding.ActivityNfcScanBinding;
import com.example.attendease.util.NfcUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class NfcScanActivity extends AppCompatActivity {

    public static final String EXTRA_LESSON_ID = "extra_lesson_id";
    public static final String EXTRA_MODULE_ID = "extra_module_id";

    // Hold durations for each state (ms)
    private static final int HOLD_SUCCESS = 3000;
    private static final int HOLD_ALREADY  = 2000;
    private static final int HOLD_FAILED   = 2000;
    private static final int HOLD_LINKABLE = 8000;   // failed, with the link button showing

    private ActivityNfcScanBinding binding;
    private AppDatabase db;
    private NfcAdapter nfcAdapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Executor executor = Executors.newSingleThreadExecutor();

    private long lessonId;
    private long moduleId;

    /** True while a tag is being processed — prevents re-entrancy. */
    private volatile boolean processingTag = false;

    private final Runnable resetToIdle = () -> {
        processingTag = false;
        showIdle();
    };

    private int scannedCount = 0;
    private int enrolledCount = 0;

    // ── Lifecycle ────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNfcScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        lessonId = getIntent().getLongExtra(EXTRA_LESSON_ID, -1L);
        moduleId = getIntent().getLongExtra(EXTRA_MODULE_ID, -1L);

        db = AppDatabase.getInstance(this);
        NfcManager nfcManager = (NfcManager) getSystemService(NFC_SERVICE);
        nfcAdapter = nfcManager != null ? nfcManager.getDefaultAdapter() : null;

        if (nfcAdapter == null) {
            Toast.makeText(this, R.string.nfc_unavailable, Toast.LENGTH_LONG).show();
        }

        binding.btnEndSession.setOnClickListener(v -> confirmEndSession());

        // Intercept back — require explicit End Session to avoid accidental exit
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmEndSession();
            }
        });

        loadInitialCounts();
        showIdle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null && !nfcAdapter.isEnabled()) {
            Toast.makeText(this, R.string.nfc_disabled, Toast.LENGTH_LONG).show();
        }
        NfcUtils.enableForegroundDispatch(this, nfcAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        NfcUtils.disableForegroundDispatch(this, nfcAdapter);
    }

    @Override
    @SuppressWarnings("deprecation")   // getParcelableExtra(String) needed for API < 33
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            if (!processingTag) {
                Tag tag;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag.class);
                } else {
                    tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                }
                if (tag != null) {
                    String uid = NfcUtils.bytesToHex(tag.getId());
                    handleNfcTag(uid);
                }
            }
        }
    }

    // ── NFC tag handling ─────────────────────────────────────────────

    private void handleNfcTag(String uid) {
        processingTag = true;
        executor.execute(() -> {
            User user = db.userDao().getUserByLid(uid);

            if (user == null) {
                List<User> unlinked = moduleId != -1L
                        ? db.userDao().getUnlinkedStudentsForModule(moduleId)
                        : Collections.emptyList();
                handler.post(() -> {
                    showFailed();
                    if (unlinked.isEmpty()) {
                        scheduleReset(HOLD_FAILED);
                    } else {
                        binding.btnLinkCard.setVisibility(View.VISIBLE);
                        binding.btnLinkCard.setOnClickListener(v -> showLinkCardDialog(uid, unlinked));
                        scheduleReset(HOLD_LINKABLE);
                    }
                });
                return;
            }

            registerAttendance(user);
        });
    }

    /** Runs on the executor. Shows already-registered or records the attendance. */
    private void registerAttendance(User user) {
        Attendance existing = db.attendanceDao().getRecord(lessonId, user.studentOrStaffId);
        if (existing != null) {
            handler.post(() -> {
                showAlreadyRegistered(user.name);
                scheduleReset(HOLD_ALREADY);
            });
            return;
        }

        // Register attendance
        Attendance attendance = new Attendance(
                lessonId, user.studentOrStaffId, System.currentTimeMillis(), "NFC");
        db.attendanceDao().insert(attendance);
        scannedCount++;

        handler.post(() -> {
            playSuccessSound();
            showSuccess(user.name);
            updateCounter();
            scheduleReset(HOLD_SUCCESS);
        });
    }

    // ── Card linking ─────────────────────────────────────────────────

    /** Lets the lecturer assign an unknown card to an enrolled student without one. */
    private void showLinkCardDialog(String uid, List<User> unlinked) {
        handler.removeCallbacks(resetToIdle);   // keep the scanner paused while choosing
        String[] labels = new String[unlinked.size()];
        for (int i = 0; i < unlinked.size(); i++) {
            User u = unlinked.get(i);
            labels[i] = getString(R.string.link_card_item_format, u.name, u.studentOrStaffId);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.link_card_title)
                .setItems(labels, (d, which) -> linkCard(uid, unlinked.get(which)))
                .setNegativeButton(R.string.cancel, (d, w) -> scheduleReset(0))
                .setOnCancelListener(d -> scheduleReset(0))
                .show();
    }

    private void linkCard(String uid, User student) {
        binding.btnLinkCard.setVisibility(View.GONE);
        executor.execute(() -> {
            // Re-check in case the card was linked while the dialog was open
            User owner = db.userDao().getUserByLid(uid);
            if (owner == null) {
                db.userDao().linkCard(student.studentOrStaffId, uid);
                owner = student;
            }
            registerAttendance(owner);
        });
    }

    private void scheduleReset(int delayMs) {
        handler.removeCallbacks(resetToIdle);
        handler.postDelayed(resetToIdle, delayMs);
    }

    // ── Visual states ────────────────────────────────────────────────

    private void showIdle() {
        binding.rootLayout.setBackgroundColor(0xFF121212);
        binding.tvStateIcon.setText(R.string.nfc_icon_idle);
        binding.tvStateMessage.setText(R.string.nfc_scan_prompt);
        binding.tvStudentName.setVisibility(View.GONE);
        binding.btnLinkCard.setVisibility(View.GONE);
    }

    private void showSuccess(String studentName) {
        binding.rootLayout.setBackgroundColor(0xFF388E3C);   // colorSuccess
        binding.tvStateIcon.setText(R.string.nfc_icon_success);
        binding.tvStateMessage.setText(R.string.nfc_msg_success);
        binding.tvStudentName.setText(studentName);
        binding.tvStudentName.setVisibility(View.VISIBLE);
    }

    private void showAlreadyRegistered(String studentName) {
        binding.rootLayout.setBackgroundColor(0xFFF57F17);   // colorWarning
        binding.tvStateIcon.setText(R.string.nfc_icon_already);
        binding.tvStateMessage.setText(R.string.nfc_msg_already);
        binding.tvStudentName.setText(studentName);
        binding.tvStudentName.setVisibility(View.VISIBLE);
    }

    private void showFailed() {
        binding.rootLayout.setBackgroundColor(0xFFD32F2F);   // colorError
        binding.tvStateIcon.setText(R.string.nfc_icon_failed);
        binding.tvStateMessage.setText(R.string.nfc_msg_failed);
        binding.tvStudentName.setVisibility(View.GONE);
    }

    // ── Counter ──────────────────────────────────────────────────────

    private void loadInitialCounts() {
        executor.execute(() -> {
            if (lessonId != -1L) {
                scannedCount = db.attendanceDao().getAttendanceCountForLesson(lessonId);
            }
            if (moduleId != -1L) {
                enrolledCount = db.enrollmentDao().getEnrolledCountForModule(moduleId);
            }
            handler.post(this::updateCounter);
        });
    }

    private void updateCounter() {
        binding.tvCounter.setText(
                getString(R.string.nfc_counter_format, scannedCount, enrolledCount));
    }

    // ── Sound ────────────────────────────────────────────────────────

    private void playSuccessSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
            if (ringtone != null) ringtone.play();
        } catch (Exception ignored) {
            // Sound is best-effort; never crash the scanner
        }
    }

    // ── End Session ──────────────────────────────────────────────────

    private void confirmEndSession() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.stop_attendance_title)
                .setMessage(R.string.stop_attendance_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.btn_stop_attendance, (d, w) -> endSession())
                .show();
    }

    private void endSession() {
        if (lessonId != -1L) {
            executor.execute(() -> db.lessonDao().setActive(lessonId, false));
        }
        finish();
    }

}
