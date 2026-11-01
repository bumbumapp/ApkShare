package com.bumbumapps.sharing.appsapk;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.InterstitialAd;

/**
 * Huawei Ads twin of the Play build's interstitial helper: single preloaded interstitial with the same
 * frequency cap. {@code next} always runs exactly once (after the ad is closed, or immediately).
 */
public final class InterstitialHelper {

    /** At most one interstitial per 10 minutes, shown only after a finished share/save. */
    public static final long MIN_INTERVAL_MS = 10 * 60_000L;
    /** No interstitial in the first 3 minutes after the app starts. */
    public static final long FIRST_OPEN_GRACE_MS = 3 * 60_000L;

    private static InterstitialAd interstitialAd;
    private static long lastShownAt = -MIN_INTERVAL_MS;
    private static long startedAt = -1;

    private InterstitialHelper() {
    }

    public static void preload(Context context) {
        if (startedAt < 0) {
            startedAt = SystemClock.elapsedRealtime();
        }
        if (interstitialAd != null || !AdsConsent.canRequestAds(context)) {
            return;
        }
        Context app = context.getApplicationContext();
        InterstitialAd ad = new InterstitialAd(app);
        ad.setAdId(AdsConsent.slot(app, R.string.hms_interstitial_id, R.string.hms_test_interstitial_id));
        ad.setAdListener(new AdListener() {
            @Override
            public void onAdFailed(int code) {
                if (interstitialAd == ad) {
                    interstitialAd = null;
                }
            }
        });
        interstitialAd = ad;
        ad.loadAd(new AdParam.Builder().build());
    }

    /** Pure frequency-cap rule, unit tested. */
    static boolean capAllows(long now, long lastShown, long appStart) {
        return now - lastShown >= MIN_INTERVAL_MS && now - appStart >= FIRST_OPEN_GRACE_MS;
    }

    public static void showIfReady(Activity activity, Runnable next) {
        final Runnable after = next != null ? next : () -> { };
        long now = SystemClock.elapsedRealtime();
        if (startedAt < 0) {
            startedAt = now;
        }
        InterstitialAd ad = interstitialAd;
        if (ad == null || !ad.isLoaded() || activity == null || activity.isFinishing()
                || !capAllows(now, lastShownAt, startedAt)) {
            if (activity != null) {
                preload(activity);
            }
            after.run();
            return;
        }
        interstitialAd = null;
        // Huawei only reports load failures via onAdFailed: if show() never opens the ad, do not wait forever.
        final boolean[] opened = {false};
        final boolean[] done = {false};
        final Runnable finish = () -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            preload(activity);
            after.run();
        };
        ad.setAdListener(new AdListener() {
            @Override
            public void onAdOpened() {
                opened[0] = true;
            }

            @Override
            public void onAdClosed() {
                finish.run();
            }

            @Override
            public void onAdFailed(int code) {
                finish.run();
            }
        });
        lastShownAt = now;
        ad.show(activity);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!opened[0]) {
                finish.run();
            }
        }, 3000L);
    }
}
