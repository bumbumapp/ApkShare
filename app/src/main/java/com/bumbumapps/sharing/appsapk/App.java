package com.bumbumapps.sharing.appsapk;

import android.graphics.drawable.Drawable;

public class App {

    private String name;
    private Drawable icon;
    private String apkPath;
    private long apkSize;
    private String packageName;
    private String versionName;
    private boolean system;
    private boolean split;

    public App(String name, Drawable icon, String apkPath, long apkSize) {
        this(name, icon, apkPath, apkSize, null, null, false, false);
    }

    public App(String name, Drawable icon, String apkPath, long apkSize, String packageName,
               String versionName, boolean system, boolean split) {
        this.name = name;
        this.icon = icon;
        this.apkPath = apkPath;
        this.apkSize = apkSize;
        this.packageName = packageName;
        this.versionName = versionName;
        this.system = system;
        this.split = split;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public String getApkPath() {
        return apkPath;
    }

    public void setApkPath(String apkPath) {
        this.apkPath = apkPath;
    }

    public long getApkSize() {
        return apkSize;
    }

    public void setApkSize(long apkSize) {
        this.apkSize = apkSize;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getVersionName() {
        return versionName;
    }

    /** Preinstalled by the phone maker (FLAG_SYSTEM) rather than installed by the user. */
    public boolean isSystem() {
        return system;
    }

    /** Installed from several APK parts; only the base APK is shared or saved. */
    public boolean isSplit() {
        return split;
    }
}
