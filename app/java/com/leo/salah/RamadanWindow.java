package com.leo.salah;

import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;

/** Timestamp-based fasting progress; independent of display language and Android. */
public final class RamadanWindow {
    private RamadanWindow() {}

    public static boolean isFasting(long now, long fajr, long maghrib) {
        return maghrib > fajr && now >= fajr && now < maghrib;
    }

    public static int progress(long now, long fajr, long maghrib) {
        if (maghrib <= fajr || now <= fajr) return 0;
        if (now >= maghrib) return 1000;
        return (int) ((now - fajr) * 1000L / (maghrib - fajr));
    }

    public static boolean tomorrowIsRamadan(LocalDate today, int hijriOffset) {
        return HijrahDate.from(today.plusDays(1L + hijriOffset))
                .get(ChronoField.MONTH_OF_YEAR) == 9;
    }
}
