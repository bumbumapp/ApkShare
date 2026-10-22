package com.bumbumapps.sharing.appsapk;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pure helpers (no Android dependencies) so they can be unit tested. */
public final class AppUtils {

    private AppUtils() {
    }

    /** Case-insensitive name filter. A blank query returns every app. */
    public static List<App> filterByName(List<App> apps, String query) {
        List<App> result = new ArrayList<>();
        if (apps == null) {
            return result;
        }
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (App app : apps) {
            String name = app.getName();
            if (q.isEmpty() || (name != null && name.toLowerCase(Locale.ROOT).contains(q))) {
                result.add(app);
            }
        }
        return result;
    }

    /**
     * Formats a byte count with the given format strings (each taking one double argument).
     * Uses floating point division so e.g. 1536 bytes is 1.50 KiB (not 1.00 KiB).
     */
    public static String formatSize(long bytes, String bFmt, String kibFmt, String mibFmt, String gibFmt) {
        double kib = 1024d;
        double mib = kib * 1024d;
        double gib = mib * 1024d;
        if (bytes < kib) {
            return String.format(Locale.US, bFmt, (double) bytes);
        } else if (bytes < mib) {
            return String.format(Locale.US, kibFmt, bytes / kib);
        } else if (bytes < gib) {
            return String.format(Locale.US, mibFmt, bytes / mib);
        }
        return String.format(Locale.US, gibFmt, bytes / gib);
    }
}
