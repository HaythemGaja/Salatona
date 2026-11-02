package com.leo.salah;

import android.content.Context;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.util.Calendar;

/** Prayer-time helpers: today's times, next prayer, formatting, qibla, Hijri date. */
public class PrayerCalc {

    public static final String[] NAMES = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
    public static final String[] NAMES_AR = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
    /** times[] index for each prayer: fajr, dhuhr, asr, maghrib, isha */
    public static final int[] TIME_IDX = {0, 2, 3, 4, 5};
    /** display order on home screen: fajr, sunrise, dhuhr, asr, maghrib, isha */
    public static final int[] DISPLAY_IDX = {0, 1, 2, 3, 4, 5};
    public static final String[] DISPLAY_NAMES = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    public static final String[] DISPLAY_AR = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};

    /** times[] = {fajr, sunrise, dhuhr, asr, maghrib, isha, sunset} as fractional hours. */
    public static double[] timesFor(Context ctx, int year, int month, int day) {
        Prefs p = new Prefs(ctx);
        return timesFor(p, year, month, day, ZoneId.systemDefault());
    }

    /**
     * Times for one day, converted to local wall-clock hours with the UTC offset
     * that is in force at each prayer's own instant. This is what makes the TV's
     * daylight-saving rules — and the manual override in Settings — apply to the
     * whole day, including a transition that falls inside it.
     */
    static double[] timesFor(Prefs p, int year, int month, int day, ZoneId zone) {
        double[] utc = PrayerTimes.getTimes(year, month, day, p.lat(), p.lng(), 0,
                p.method(), p.asr(), p.tuneArray());
        return toLocal(utc, year, month, day, p.dstMode(), zone);
    }

    /**
     * Pure conversion from solar times in UTC hours to local hours. Public and
     * Android-free so it can be exercised by the headless tests.
     */
    public static double[] toLocal(double[] utc, int year, int month, int day,
                                   int dstMode, ZoneId zone) {
        Instant dayStart = LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant();
        double[] local = new double[utc.length];
        for (int i = 0; i < utc.length; i++) {
            Instant at = dayStart.plusSeconds(Math.round(utc[i] * 3600.0));
            double h = utc[i] + Daylight.offsetHours(dstMode, at, zone);
            local[i] = ((h % 24) + 24) % 24;
        }
        return local;
    }

    public static long toMillis(double hour, Calendar day) {
        Calendar c = (Calendar) day.clone();
        double h24 = ((hour % 24) + 24) % 24;
        int h = (int) h24;
        int m = (int) Math.round((h24 - h) * 60);
        if (m == 60) { h++; m = 0; }
        c.set(Calendar.HOUR_OF_DAY, h % 24);
        c.set(Calendar.MINUTE, m);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static class NextPrayer {
        public final int prayer; // 0..4
        public final long atMillis;
        NextPrayer(int prayer, long atMillis) { this.prayer = prayer; this.atMillis = atMillis; }
    }

    /** Next upcoming prayer from now (1-minute grace so a just-passed prayer flips over). */
    public static NextPrayer next(Context ctx, long now) {
        Calendar c = Calendar.getInstance();
        double[] t = timesFor(ctx, c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
        for (int i = 0; i < 5; i++) {
            long ms = toMillis(t[TIME_IDX[i]], c);
            if (ms > now + 60_000) return new NextPrayer(i, ms);
        }
        Calendar c2 = (Calendar) c.clone();
        c2.add(Calendar.DATE, 1);
        double[] t2 = timesFor(ctx, c2.get(Calendar.YEAR), c2.get(Calendar.MONTH) + 1,
                c2.get(Calendar.DAY_OF_MONTH));
        return new NextPrayer(0, toMillis(t2[TIME_IDX[0]], c2));
    }

    /** "7:04 PM" style. */
    public static String formatTime(double hour) {
        int h = (int) hour;
        int m = (int) Math.round((hour - h) * 60);
        if (m == 60) { h++; m = 0; }
        h = ((h % 24) + 24) % 24;
        String ap = h < 12 ? "AM" : "PM";
        int h12 = h % 12; if (h12 == 0) h12 = 12;
        return h12 + ":" + (m < 10 ? "0" : "") + m + " " + ap;
    }

    /** "02:14:33" countdown. */
    public static String formatCountdown(long ms) {
        if (ms < 0) ms = 0;
        long s = ms / 1000;
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m + ":"
                + (sec < 10 ? "0" : "") + sec;
    }

    /** Qibla bearing in degrees clockwise from true north. */
    public static double qiblaBearing(double lat, double lng) {
        double dLng = Math.toRadians(39.8262 - lng);
        double lat1 = Math.toRadians(lat), lat2 = Math.toRadians(21.4225);
        double y = Math.sin(dLng);
        double x = Math.cos(lat1) * Math.tan(lat2) - Math.sin(lat1) * Math.cos(dLng);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    public static String directionName(double bearing) {
        String[] dirs = {"North", "North-East", "East", "South-East",
                         "South", "South-West", "West", "North-West"};
        return dirs[(int) Math.round(bearing / 45.0) % 8];
    }

    public static final String[] MOON_EMOJI =
            {"🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘"};
    public static final String[] MOON_NAMES =
            {"New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous",
             "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"};

    /** Moon phase index 0..7 for today (0 = new, 4 = full). */
    public static int moonPhase() {
        return ((int) (moonPhaseContinuous() * 8 + 0.5)) % 8;
    }

    /** Continuous moon phase 0..1 (0 = new, 0.5 = full). */
    public static double moonPhaseContinuous() {
        // synodic month 29.530588853 d; reference new moon 2000-01-06 18:14 UTC (JD 2451550.1)
        double jd = System.currentTimeMillis() / 86400000.0 + 2440587.5;
        double age = (jd - 2451550.1) % 29.530588853;
        if (age < 0) age += 29.530588853;
        return age / 29.530588853;
    }

    private static final String[] HIJRI_MONTHS = {"Muharram", "Safar", "Rabi' al-Awwal",
            "Rabi' al-Thani", "Jumada al-Ula", "Jumada al-Akhirah", "Rajab",
            "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qa'dah", "Dhu al-Hijjah"};

    public static int[] hijriToday() {
        HijrahDate hd = HijrahDate.now();
        return new int[]{
                hd.get(ChronoField.DAY_OF_MONTH),
                hd.get(ChronoField.MONTH_OF_YEAR),
                hd.get(ChronoField.YEAR)};
    }

    public static String hijriDate() {
        int[] h = hijriToday();
        return h[0] + " " + HIJRI_MONTHS[h[1] - 1] + " " + h[2] + " AH";
    }

    /** Hijri month name for 1..12. */
    public static String hijriMonthName(int month1to12) {
        return HIJRI_MONTHS[(month1to12 - 1 + 12) % 12];
    }

    public static boolean isRamadan() { return hijriToday()[1] == 9; }

    public static String dayKey(Calendar c) {
        return c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1)
                + "-" + c.get(Calendar.DAY_OF_MONTH);
    }
    public static String dayKey(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return dayKey(c);
    }
}
