package com.bumbumapps.sharing.appsapk;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InterstitialHelperTest {
    private static final long MIN = 60_000L;

    @Test
    public void tenMinuteGapBetweenAds() {
        long start = 0;
        long shown = 20 * MIN;
        assertFalse(InterstitialHelper.capAllows(shown + 9 * MIN, shown, start));
        assertTrue(InterstitialHelper.capAllows(shown + 10 * MIN, shown, start));
    }

    @Test
    public void noAdInFirstThreeMinutes() {
        long never = -InterstitialHelper.MIN_INTERVAL_MS;
        assertFalse(InterstitialHelper.capAllows(2 * MIN, never, 0));
        assertTrue(InterstitialHelper.capAllows(3 * MIN, never, 0));
    }
}
