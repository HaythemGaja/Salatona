package com.leo.salah;

import android.icu.util.IslamicCalendar;

/**
 * Countdown to the next Islamic religious event, computed from the Hijri
 * (tabular/civil) calendar. Dates may differ by a day from moon-sighting
 * based announcements — treated as an approximation for the countdown.
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

    /** Nearest upcoming event (today counts as 0 days left). */
    public static Upcoming next() {
        try {
            IslamicCalendar today = new IslamicCalendar();
            today.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_CIVIL);
            long dayStart = startOfDay(today.getTimeInMillis());
            int hijriYear = today.get(IslamicCalendar.YEAR);

            Upcoming best = null;
            for (int[] e : EVENTS) {
                IslamicCalendar c = new IslamicCalendar();
                c.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_CIVIL);
                c.set(IslamicCalendar.YEAR, hijriYear);
                c.set(IslamicCalendar.MONTH, e[0]);
                c.set(IslamicCalendar.DAY_OF_MONTH, e[1]);
                c.set(IslamicCalendar.HOUR_OF_DAY, 0);
                c.set(IslamicCalendar.MINUTE, 0);
                c.set(IslamicCalendar.SECOND, 0);
                c.set(IslamicCalendar.MILLISECOND, 0);
                long t = c.getTimeInMillis();
                if (t < dayStart) {
                    c.add(IslamicCalendar.YEAR, 1);
                    t = c.getTimeInMillis();
                }
                long days = (t - dayStart) / 86400000L;
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

    private static long startOfDay(long millis) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Every event with its nearest occurrence this Hijri year, sorted by date. */
    public static java.util.List<EventInfo> all() {
        java.util.List<EventInfo> out = new java.util.ArrayList<>();
        try {
            IslamicCalendar today = new IslamicCalendar();
            today.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_CIVIL);
            long dayStart = startOfDay(today.getTimeInMillis());
            int hijriYear = today.get(IslamicCalendar.YEAR);

            for (int[] e : EVENTS) {
                IslamicCalendar c = new IslamicCalendar();
                c.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_CIVIL);
                c.set(IslamicCalendar.YEAR, hijriYear);
                c.set(IslamicCalendar.MONTH, e[0]);
                c.set(IslamicCalendar.DAY_OF_MONTH, e[1]);
                c.set(IslamicCalendar.HOUR_OF_DAY, 0);
                c.set(IslamicCalendar.MINUTE, 0);
                c.set(IslamicCalendar.SECOND, 0);
                c.set(IslamicCalendar.MILLISECOND, 0);
                long t = c.getTimeInMillis();
                long days = (t - dayStart) / 86400000L;

                EventInfo i = new EventInfo();
                i.name = NAMES[e[2]];
                i.nameAr = NAMES_AR[e[2]];
                i.hijriDay = e[1];
                i.hijriMonth = e[0];
                i.hijriYear = hijriYear;
                i.daysFromToday = days;
                i.gregorianMillis = t;
                out.add(i);
            }
            java.util.Collections.sort(out,
                    (a, b) -> Long.compare(a.daysFromToday, b.daysFromToday));
        } catch (Exception ignored) {}
        return out;
    }

    /** Current Hijri year (tabular/civil). */
    public static int currentHijriYear() {
        try {
            IslamicCalendar c = new IslamicCalendar();
            c.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_CIVIL);
            return c.get(IslamicCalendar.YEAR);
        } catch (Exception e) {
            return 0;
        }
    }

    /** Display text: "🌙 Ramadan (رمضان) · in 142 days" / "· today!" / "· tomorrow". */
    public static String displayText(Upcoming u) {
        if (u == null) return "";
        if (u.daysLeft <= 0) return "🌙 " + u.name + " (" + u.nameAr + ") · today!";
        if (u.daysLeft == 1) return "🌙 " + u.name + " (" + u.nameAr + ") · tomorrow";
        return "🌙 " + u.name + " (" + u.nameAr + ") · in " + u.daysLeft + " days";
    }
}
