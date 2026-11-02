package com.leo.salah;

import android.content.Context;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Islamic events on the Umm al-Qura calendar, with the same ±2-day Hijri
 * offset as the rest of the app. Dates may still differ by a day from local
 * moon sighting; the offset is there for that.
 */
public class IslamicEvents {

    public static class Upcoming {
        public String name;
        public String nameAr;
        public long daysLeft;
    }

    /** One event with its nearest occurrence (upcoming or already passed). */
    public static class EventInfo {
        public String name;
        public String nameAr;
        public int hijriDay;
        public int hijriMonth; // 0-based
        public int hijriYear;
        public long daysFromToday; // >0 upcoming, 0 today, <0 passed
        public long gregorianMillis;
    }

    public static final String[] HIJRI_MONTHS = {
            "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
            "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhul-Qa'dah", "Dhul-Hijjah"
    };

    // {hijriMonth 0-based, hijriDay, nameIndex}
    private static final int[][] EVENTS = {
            {8, 1, 0},   // Ramadan 1
            {9, 1, 1},   // Eid al-Fitr (Shawwal 1)
            {11, 9, 2},  // Arafah Day (Dhul-Hijjah 9)
            {11, 10, 3}, // Eid al-Adha (Dhul-Hijjah 10)
            {0, 1, 4},   // Islamic New Year (Muharram 1)
            {0, 10, 5},  // Ashura (Muharram 10)
            {6, 27, 6},  // Laylat al-Mi'raj (Rajab 27)
            {7, 15, 7},  // Laylat al-Bara'at (Sha'ban 15)
    };

    public static final String[] NAMES = {
            "Ramadan", "Eid al-Fitr", "Arafah Day", "Eid al-Adha",
            "Islamic New Year", "Ashura", "Laylat al-Mi'raj", "Laylat al-Bara'at"
    };
    public static final String[] NAMES_AR = {
            "رمضان", "عيد الفطر", "يوم عرفة", "عيد الأضحى",
            "رأس السنة الهجرية", "عاشوراء", "ليلة المعراج", "ليلة النصف من شعبان"
    };

    private static HijrahDate today(Context c) {
        int off = c == null ? 0 : new Prefs(c).hijriOffset();
        return HijrahDate.now().plus(off, ChronoUnit.DAYS);
    }

    private static long toMillis(HijrahDate h) {
        LocalDate g = LocalDate.from(h);
        return g.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** Nearest upcoming event (today counts as 0 days left). */
    public static Upcoming next() { return next(null); }

    public static Upcoming next(Context c) {
        try {
            HijrahDate today = today(c);
            int hijriYear = today.get(ChronoField.YEAR);
            Upcoming best = null;
            for (int[] e : EVENTS) {
                HijrahDate target = HijrahDate.of(hijriYear, e[0] + 1, e[1]);
                long days = ChronoUnit.DAYS.between(today, target);
                if (days < 0) {
                    target = HijrahDate.of(hijriYear + 1, e[0] + 1, e[1]);
                    days = ChronoUnit.DAYS.between(today, target);
                }
                if (best == null || days < best.daysLeft) {
                    Upcoming u = new Upcoming();
                    u.name = NAMES[e[2]];
                    u.nameAr = NAMES_AR[e[2]];
                    u.daysLeft = days;
                    best = u;
                }
            }
            return best;
        } catch (Exception ex) {
            return null;
        }
    }

    /** Every event with its nearest occurrence this Hijri year, sorted by date. */
    public static List<EventInfo> all() { return all(null); }

    public static List<EventInfo> all(Context c) {
        List<EventInfo> out = new ArrayList<>();
        try {
            HijrahDate today = today(c);
            int hijriYear = today.get(ChronoField.YEAR);
            for (int[] e : EVENTS) {
                HijrahDate target = HijrahDate.of(hijriYear, e[0] + 1, e[1]);
                long days = ChronoUnit.DAYS.between(today, target);
                if (days < 0) {
                    target = HijrahDate.of(hijriYear + 1, e[0] + 1, e[1]);
                    days = ChronoUnit.DAYS.between(today, target);
                }
                EventInfo i = new EventInfo();
                i.name = NAMES[e[2]];
                i.nameAr = NAMES_AR[e[2]];
                i.hijriDay = e[1];
                i.hijriMonth = e[0];
                i.hijriYear = hijriYear;
                i.daysFromToday = days;
                i.gregorianMillis = toMillis(target);
                out.add(i);
            }
            Collections.sort(out, (a, b) -> Long.compare(a.daysFromToday, b.daysFromToday));
        } catch (Exception ignored) {}
        return out;
    }

    public static int currentHijriYear() { return currentHijriYear(null); }

    public static int currentHijriYear(Context c) {
        try {
            return today(c).get(ChronoField.YEAR);
        } catch (Exception e) {
            return 0;
        }
    }

    /** Display text: "🌙 Ramadan · in 142 days" / "· today!" / "· tomorrow". */
    public static String displayText(Upcoming u, Context c) {
        if (u == null) return "";
        String name = Lang.ar() ? u.nameAr : u.name;
        if (u.daysLeft <= 0) return Lang.t("🌙 " + name + " · today!");
        if (u.daysLeft == 1) return Lang.t("🌙 " + name + " · tomorrow");
        return Lang.t("🌙 " + name + " · in " + u.daysLeft + " days");
    }

    public static String displayText(Upcoming u) { return displayText(u, null); }
}
