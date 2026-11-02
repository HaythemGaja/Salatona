package com.leo.salah;

import android.content.Context;

import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneRules;
import java.util.Locale;

/**
 * Daylight-saving policy for prayer times.
 *
 * <p>AUTO follows the TV's own time-zone rules at the exact instant of each
 * prayer, so a transition that happens inside the day is handled correctly.
 * ON and OFF shift every prayer by one hour from what the TV's clock would
 * otherwise give: ON for a TV stuck on winter time, OFF for a TV that
 * switched too early (or is fixed an hour ahead).
 */
public final class Daylight {

    public static final int AUTO = 0, ON = 1, OFF = 2;

    public static final String[] MODE_NAMES = {
            "Auto · follow TV zone",
            "On · +1 hour",
            "Off · −1 hour"};

    private Daylight() {}

    /** UTC offset in hours to apply to a prayer happening at {@code at}. */
    public static double offsetHours(int mode, Instant at, ZoneId zone) {
        ZoneRules rules = zone.getRules();
        int seconds = rules.getOffset(at).getTotalSeconds();
        if (mode == ON) seconds += 3600;
        else if (mode == OFF) seconds -= 3600;
        return seconds / 3600.0;
    }

    public static double currentOffsetHours(int mode) {
        return offsetHours(mode, Instant.now(), ZoneId.systemDefault());
    }

    public static boolean deviceDstNow() {
        return ZoneId.systemDefault().getRules().isDaylightSavings(Instant.now());
    }

    /** "UTC+2" / "UTC−4:30" for an offset in hours. */
    public static String offsetLabel(double hours) {
        int total = (int) Math.round(hours * 60);
        int h = Math.abs(total) / 60;
        int m = Math.abs(total) % 60;
        return String.format(Locale.US, "UTC%s%d%s", total < 0 ? "−" : "+", h,
                m == 0 ? "" : ":" + (m < 10 ? "0" : "") + m);
    }

    /** One line for Settings: the TV zone, what is applied, and the current offset. */
    public static String status(Context c) {
        int mode = new Prefs(c).dstMode();
        String zone = ZoneId.systemDefault().getId();
        String off = offsetLabel(currentOffsetHours(mode));
        if (mode == ON) return zone + " · shifted +1 hour · " + off;
        if (mode == OFF) return zone + " · shifted −1 hour · " + off;
        return zone + (deviceDstNow()
                ? " · daylight saving active · " : " · standard time · ") + off;
    }
}
