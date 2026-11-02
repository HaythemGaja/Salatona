package com.leo.salah;

/**
 * Prayer time calculation (PrayTimes.org v2.3 algorithm port).
 * Returns times as fractional hours in local time for the given date/coords.
 * Indices: 0 Fajr, 1 Sunrise, 2 Dhuhr, 3 Asr, 4 Maghrib, 5 Isha, 6 Sunset(raw).
 */
public class PrayerTimes {

    public static final int JAFARI = 0, KARACHI = 1, ISNA = 2, MWL = 3,
            MAKKAH = 4, EGYPT = 5, TEHRAN = 6, DUBAI = 7, KUWAIT = 8,
            QATAR = 9, SINGAPORE = 10, TURKEY = 11, OMAN = 12, MOROCCO = 13;

    public static final String[] METHOD_NAMES = {
            "Shia (Jafari)", "Karachi", "ISNA (North America)",
            "Muslim World League", "Umm al-Qura (Makkah)",
            "Egyptian Authority", "Tehran", "Dubai", "Kuwait", "Qatar",
            "Singapore", "Turkey (Diyanet)", "Oman Awqaf", "Morocco Habous" };

    // {fajrAngle, maghribVal, maghribIsMinutes, ishaVal, ishaIsMinutes}
    private static final double[][] P = {
            {16,   4,   1, 14, 0},  // Jafari
            {18,   0,   0, 18, 0},  // Karachi
            {15,   0,   0, 15, 0},  // ISNA
            {18,   0,   0, 17, 0},  // MWL
            {18.5, 0,   0, 90, 1},  // Makkah (isha = 90 min after maghrib)
            {19.5, 0,   0, 17.5, 0},// Egypt
            {17.7, 4.5, 1, 14, 0},  // Tehran (maghrib = 4.5 min after sunset)
            {18.2, 0,   0, 18.2, 0},// Dubai
            {18,   0,   0, 17.5, 0},// Kuwait
            {18,   0,   0, 90,   1},// Qatar (isha = 90 min after maghrib)
            {20,   0,   0, 18,   0},// Singapore
            {18,   0,   0, 17,   0},// Turkey (Diyanet)
            {18,   0,   0, 18,   0},// Oman (Ministry of Awqaf, fitted)
            {19,   0,   0, 17,   0},// Morocco (Ministry of Habous)
    };

    /** Method-specific minute adjustments: fajr, sunrise, dhuhr, asr, maghrib, isha. */
    private static final int[][] ADJ = {
            {0, 0, 0, 0, 0, 0},     // Jafari
            {0, 0, 0, 0, 0, 0},     // Karachi
            {0, 0, 0, 0, 0, 0},     // ISNA
            {0, 0, 0, 0, 0, 0},     // MWL
            {0, 0, 0, 0, 0, 0},     // Makkah
            {0, 0, 0, 0, 0, 0},     // Egypt
            {0, 0, 0, 0, 0, 0},     // Tehran
            {0, -3, 3, 3, 3, 0},    // Dubai
            {0, 0, 0, 0, 0, 0},     // Kuwait
            {0, 0, 0, 0, 0, 0},     // Qatar
            {0, 0, 1, 0, 0, 0},     // Singapore
            {0, -7, 5, 4, 7, 0},    // Turkey
            {0, 0, 5, 5, 5, 0},     // Oman
            {0, 0, 5, 0, 0, 0},     // Morocco
    };

    private static final double RISE_SET_ANGLE = 0.833;

    public static double[] getTimes(int year, int month, int day,
                                    double lat, double lng, double tz,
                                    int method, int asrJuristic, int[] tuneMin) {
        double[] p = P[method];
        double jd = julianDate(year, month, day) - lng / 360.0;

        // initial guess (day portions)
        double[] t = {5/24.0, 6/24.0, 12/24.0, 13/24.0, 18/24.0, 18/24.0, 18/24.0};
        for (int iter = 0; iter < 2; iter++)
            t = computeTimes(t, jd, lat, p, asrJuristic);

        // adjust to timezone/longitude
        for (int i = 0; i < t.length; i++) {
            t[i] = fixHour(t[i] + tz - lng / 15.0);
        }
        // high latitude fallback (middle of night)
        if (Math.abs(lat) > 48) adjustHighLats(t, p);
        // method-specific minutes (Dubai, Turkey, Oman, Morocco…)
        if (method >= 0 && method < ADJ.length) {
            int[] a = ADJ[method];
            for (int i = 0; i < 6; i++) t[i] = fixHour(t[i] + a[i] / 60.0);
        }
        // user tuning
        if (tuneMin != null)
            for (int i = 0; i < Math.min(7, tuneMin.length); i++)
                t[i] = fixHour(t[i] + tuneMin[i] / 60.0);
        return t;
    }

    private static double[] computeTimes(double[] guess, double jd, double lat,
                                         double[] p, int asrJuristic) {
        double[] sp = sunPosition(jd);
        double decl = sp[0], eqt = sp[1];
        double[] t = new double[7];
        t[0] = sunAngleTime(p[0], decl, eqt, lat, true);                    // fajr
        t[1] = sunAngleTime(RISE_SET_ANGLE, decl, eqt, lat, true);          // sunrise
        t[2] = fixHour(12 - eqt);                                           // dhuhr
        t[3] = asrTime(asrJuristic == 1 ? 2 : 1, decl, eqt, lat);           // asr
        t[6] = sunAngleTime(RISE_SET_ANGLE, decl, eqt, lat, false);         // sunset
        t[4] = (p[2] == 1) ? t[6] + p[1] / 60.0                            // maghrib
                           : t[6];
        t[5] = (p[4] == 1) ? t[4] + p[3] / 60.0                            // isha
                           : sunAngleTime(p[3], decl, eqt, lat, false);
        return t;
    }

    private static double sunAngleTime(double angle, double decl, double eqt,
                                       double lat, boolean ccw) {
        double noon = fixHour(12 - eqt);
        double cosT = (-sinDeg(angle) - sinDeg(decl) * sinDeg(lat))
                    / (cosDeg(decl) * cosDeg(lat));
        cosT = Math.max(-1, Math.min(1, cosT));
        double t = Math.toDegrees(Math.acos(cosT)) / 15.0;
        return noon + (ccw ? -t : t);
    }

    private static double asrTime(double factor, double decl, double eqt, double lat) {
        double angle = -Math.toDegrees(arccot(factor + Math.tan(Math.toRadians(Math.abs(lat - decl)))));
        return sunAngleTime(angle, decl, eqt, lat, false);
    }

    private static double[] sunPosition(double jd) {
        double D = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * D);
        double q = fixAngle(280.459 + 0.98564736 * D);
        double L = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g));
        double e = 23.439 - 0.00000036 * D;
        double RA = fixHour(Math.toDegrees(Math.atan2(
                cosDeg(e) * sinDeg(L), cosDeg(L))) / 15.0);
        double decl = Math.toDegrees(Math.asin(sinDeg(e) * sinDeg(L)));
        return new double[]{decl, q / 15.0 - RA};
    }

    private static void adjustHighLats(double[] t, double[] p) {
        double night = timeDiff(t[6], t[1]); // sunset -> sunrise
        double fajrDiff = 0.5 * night;
        if (Double.isNaN(t[0]) || timeDiff(t[0], t[1]) > fajrDiff)
            t[0] = t[1] - fajrDiff;
        double ishaAngle = (p[4] == 1) ? 18 : p[3];
        double ishaDiff = 0.5 * night;
        if (Double.isNaN(t[5]) || timeDiff(t[6], t[5]) > ishaDiff)
            t[5] = t[6] + ishaDiff;
        double maghribDiff = 0.5 * night;
        if (Double.isNaN(t[4]) || timeDiff(t[6], t[4]) > maghribDiff)
            t[4] = t[6] + maghribDiff;
    }

    private static double julianDate(int y, int m, int d) {
        if (m <= 2) { y -= 1; m += 12; }
        double A = Math.floor(y / 100.0);
        double B = 2 - A + Math.floor(A / 4.0);
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + B - 1524.5;
    }

    private static double fixAngle(double a) { return fix(a, 360); }
    private static double fixHour(double a) { return fix(a, 24); }
    private static double fix(double a, double b) {
        a = a - b * Math.floor(a / b);
        return (a < 0) ? a + b : a;
    }
    private static double timeDiff(double t1, double t2) { return fixHour(t2 - t1); }
    private static double sinDeg(double d) { return Math.sin(Math.toRadians(d)); }
    private static double cosDeg(double d) { return Math.cos(Math.toRadians(d)); }
    private static double arccot(double x) { return Math.atan(1.0 / x); }
}
