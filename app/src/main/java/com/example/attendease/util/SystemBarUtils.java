package com.example.attendease.util;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Android 15+ always draws apps behind the status and navigation bars, so the
 * app opts in on every version and makes room for the bars itself.
 */
public final class SystemBarUtils {

    /**
     * Draws behind the system bars with light status bar icons, since every
     * screen has a red or dark area at the top. Call before setContentView.
     */
    public static void enableEdgeToEdge(ComponentActivity activity) {
        EdgeToEdge.enable(activity, SystemBarStyle.dark(Color.TRANSPARENT));
    }

    /** Sizes a view to the status bar height, e.g. a coloured strip behind it. */
    public static void matchStatusBarHeight(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp.height != bars.top) {
                lp.height = bars.top;
                v.setLayoutParams(lp);
            }
            return windowInsets;
        });
    }

    /** Pads a full-screen view so its content clears both system bars. */
    public static void padForSystemBars(View view) {
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return windowInsets;
        });
    }

    /**
     * Pads the bottom of a scrolling screen for the navigation bar or the
     * keyboard, whichever is taller, so focused fields can scroll into view.
     */
    public static void padBottomForKeyboard(View view) {
        int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            int nav = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            int ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    bottom + Math.max(nav, ime));
            return windowInsets;
        });
    }

    private SystemBarUtils() {}
}
