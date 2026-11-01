package com.bumbumapps.sharing.appsapk;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.banner.BannerView;

/** Huawei Ads banner loaded into a container (same API as the Play build's AdMob banner). */
public final class BannerHelper {

    private BannerHelper() {
    }

    public static View load(Activity activity, ViewGroup container) {
        if (container == null || !AdsConsent.canRequestAds(activity)) {
            return null;
        }
        container.removeAllViews();
        BannerView banner = new BannerView(activity);
        banner.setAdId(AdsConsent.slot(activity, R.string.hms_banner_id, R.string.hms_test_banner_id));
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int widthPx = container.getWidth() > 0 ? container.getWidth() : metrics.widthPixels;
        int widthDp = (int) (widthPx / metrics.density);
        banner.setBannerAdSize(BannerAdSize.getCurrentDirectionBannerSize(activity, widthDp));
        container.addView(banner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER_HORIZONTAL));
        banner.loadAd(new AdParam.Builder().build());
        return banner;
    }

    public static void destroy(View banner) {
        if (banner instanceof BannerView) {
            ((BannerView) banner).destroy();
        }
    }
}
