package com.bumbumapps.sharing.appsapk;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class AppUtilsTest {

    private static List<App> sample() {
        return Arrays.asList(
                new App("Chrome", null, "/a.apk", 1),
                new App("Calculator", null, "/b.apk", 1),
                new App("Maps", null, "/c.apk", 1));
    }

    @Test
    public void filterIsCaseInsensitive() {
        List<App> result = AppUtils.filterByName(sample(), "chr");
        assertEquals(1, result.size());
        assertEquals("Chrome", result.get(0).getName());
    }

    @Test
    public void blankQueryReturnsAll() {
        assertEquals(3, AppUtils.filterByName(sample(), "  ").size());
        assertEquals(3, AppUtils.filterByName(sample(), null).size());
    }

    @Test
    public void filterHandlesNullList() {
        assertEquals(0, AppUtils.filterByName(null, "x").size());
    }

    @Test
    public void kibSizeKeepsFraction() {
        assertEquals("1.50 KiB", AppUtils.formatSize(1536, "%1$.2f B", "%1$.2f KiB", "%1$.2f MiB", "%1$.2f GiB"));
        assertEquals("512.00 B", AppUtils.formatSize(512, "%1$.2f B", "%1$.2f KiB", "%1$.2f MiB", "%1$.2f GiB"));
        assertEquals("2.50 MiB", AppUtils.formatSize(2621440, "%1$.2f B", "%1$.2f KiB", "%1$.2f MiB", "%1$.2f GiB"));
    }

    private static List<App> mixed() {
        return Arrays.asList(
                new App("zeta", null, "/z.apk", 10, "z", "1", false, false),
                new App("Alpha", null, "/a.apk", 30, "a", "1", true, false),
                new App("beta", null, "/b.apk", 20, "b", "1", false, true));
    }

    @Test
    public void userFilterHidesSystemAppsAndSortsByName() {
        List<App> r = AppUtils.select(mixed(), "", AppUtils.Filter.USER, false);
        assertEquals(2, r.size());
        assertEquals("beta", r.get(0).getName());
        assertEquals("zeta", r.get(1).getName());
    }

    @Test
    public void systemFilterAndLargestFirst() {
        assertEquals("Alpha", AppUtils.select(mixed(), null, AppUtils.Filter.SYSTEM, false).get(0).getName());
        List<App> all = AppUtils.select(mixed(), "", AppUtils.Filter.ALL, true);
        assertEquals("Alpha", all.get(0).getName());
        assertEquals("zeta", all.get(2).getName());
    }

    @Test
    public void countsOnlyUserApps() {
        assertEquals(2, AppUtils.countUserApps(mixed()));
        assertEquals(0, AppUtils.countUserApps(null));
    }

    @Test
    public void apkFileNameIsSafe() {
        assertEquals("MyApp_2.1.0.apk", AppUtils.apkFileName("My/App:", "2.1.0"));
        assertEquals("app.apk", AppUtils.apkFileName("??", null));
        assertEquals("Maps_11.2beta.apk", AppUtils.apkFileName("Maps", "11.2 beta"));
    }
}
