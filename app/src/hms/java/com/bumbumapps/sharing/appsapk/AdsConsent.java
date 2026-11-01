package com.bumbumapps.sharing.appsapk;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.NonPersonalizedAd;
import com.huawei.hms.ads.RequestOptions;

/**
 * Huawei AppGallery build: same API as the Play build, but initialises Huawei Ads (Petal) instead of
 * AdMob. Only non-personalized ads are requested, so no consent form is shown.
 */
public final class AdsConsent {

    private static volatile boolean sdkReady;

    private AdsConsent() {
    }

    /** Initialises Huawei Ads, then runs {@code onReady}. */
    public static void gather(Activity activity, Runnable onReady) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (!sdkReady) {
            try {
                HwAds.init(activity.getApplicationContext());
                RequestOptions options = HwAds.getRequestOptions().toBuilder()
                        .setNonPersonalizedAd(NonPersonalizedAd.ALLOW_NON_PERSONALIZED)
                        .build();
                HwAds.setRequestOptions(options);
                sdkReady = true;
            } catch (RuntimeException e) {
                Log.w("AdsConsent", "Huawei Ads init failed", e);
                return;
            }
        }
        if (onReady != null) {
            onReady.run();
        }
    }

    public static boolean canRequestAds(Context context) {
        return sdkReady;
    }

    public static boolean isPrivacyOptionsRequired(Context context) {
        return false;
    }

    public static void showPrivacyOptions(Activity activity) {
    }

    /** Huawei slot ID: test slot in debug builds, the configured slot in release. */
    static String slot(Context context, int real, int test) {
        return context.getString(BuildConfig.DEBUG ? test : real);
    }
}
