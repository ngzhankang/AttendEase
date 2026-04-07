package com.example.attendease.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;

public final class NfcUtils {

    /**
     * Converts a raw NFC tag UID byte array to an uppercase hex string.
     * e.g. [0x04, 0xAB, 0xCD] → "04ABCD"
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Enables NFC foreground dispatch so this activity receives all NFC tag events
     * while in the foreground, before the system or any other app handles them.
     * Call from {@code onResume()}.
     */
    public static void enableForegroundDispatch(Activity activity, NfcAdapter adapter) {
        if (adapter == null) return;
        Intent intent = new Intent(activity, activity.getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                activity, 0, intent, PendingIntent.FLAG_MUTABLE);
        adapter.enableForegroundDispatch(activity, pendingIntent, null, null);
    }

    /**
     * Disables foreground dispatch. Call from {@code onPause()}.
     */
    public static void disableForegroundDispatch(Activity activity, NfcAdapter adapter) {
        if (adapter == null) return;
        adapter.disableForegroundDispatch(activity);
    }

    private NfcUtils() {}
}
