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
}
