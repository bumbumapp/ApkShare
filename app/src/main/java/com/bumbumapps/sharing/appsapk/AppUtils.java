package com.bumbumapps.sharing.appsapk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Pure helpers (no Android dependencies) so they can be unit tested. */
public final class AppUtils {

    /** Which apps the list shows. */
    public enum Filter { USER, SYSTEM, ALL }

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

    /** Name query + user/system filter + order (by name, or largest APK first). */
    public static List<App> select(List<App> apps, String query, Filter filter, boolean largestFirst) {
        List<App> result = new ArrayList<>();
        for (App app : filterByName(apps, query)) {
            if (filter == Filter.ALL || (filter == Filter.SYSTEM) == app.isSystem()) {
                result.add(app);
            }
        }
        Comparator<App> byName = (a, b) -> safe(a.getName()).compareToIgnoreCase(safe(b.getName()));
        Collections.sort(result, largestFirst
                ? (a, b) -> a.getApkSize() != b.getApkSize() ? Long.compare(b.getApkSize(), a.getApkSize()) : byName.compare(a, b)
                : byName);
        return result;
    }

    /** Count of apps the user installed (not preinstalled system apps). */
    public static int countUserApps(List<App> apps) {
        int n = 0;
        if (apps != null) {
            for (App app : apps) {
                if (!app.isSystem()) {
                    n++;
                }
            }
        }
        return n;
    }

    /** File name for a saved APK: "Name_1.2.3.apk" with characters that file systems reject removed. */
    public static String apkFileName(String name, String versionName) {
        String base = safe(name).replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").trim();
        if (base.isEmpty()) {
            base = "app";
        }
        String v = safe(versionName).replaceAll("[^A-Za-z0-9._-]", "");
        return (v.isEmpty() ? base : base + "_" + v) + ".apk";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
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
