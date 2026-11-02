import com.leo.salah.Daylight;
import com.leo.salah.PrayerCalc;
import com.leo.salah.PrayerTimes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Headless checks for the daylight-saving conversion in PrayerCalc.
 * Pure java.time + the repo's PrayTimes port; no Android runtime needed.
 *
 * Compile/run with test/run-dst-test.sh.
 */
public class DstTest {

    private static int failed = 0;
    private static int passed = 0;

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        ZoneId ny = ZoneId.of("America/New_York");
        double lat = 40.7128, lng = -74.0060;
        int[] tune = new int[7];

        // ---- winter: Auto is the TV zone; On/Off shift by exactly one hour ----
        double[] utcW = PrayerTimes.getTimes(2027, 1, 15, lat, lng, 0,
                PrayerTimes.MWL, 0, tune);
        double[] autoW = PrayerCalc.toLocal(utcW, 2027, 1, 15, Daylight.AUTO, ny);
        double[] onW = PrayerCalc.toLocal(utcW, 2027, 1, 15, Daylight.ON, ny);
        double[] offW = PrayerCalc.toLocal(utcW, 2027, 1, 15, Daylight.OFF, ny);
        for (int i = 0; i < 7; i++) {
            check("winter auto == TV offset [" + i + "]",
                    near(wrap(autoW[i]), wrap(utcW[i] - 5.0), 1e-9),
                    autoW[i] + " vs " + (utcW[i] - 5.0));
            check("winter on == auto + 1h [" + i + "]",
                    near(wrap(onW[i] - autoW[i]), 1.0, 1e-9),
                    autoW[i] + " -> " + onW[i]);
            check("winter off == auto - 1h [" + i + "]",
                    near(wrap(autoW[i] - offW[i]), 1.0, 1e-9),
                    autoW[i] + " -> " + offW[i]);
        }

        // ---- summer: Auto is EDT; On/Off remain ±1 hour ----
        double[] utcS = PrayerTimes.getTimes(2027, 7, 15, lat, lng, 0,
                PrayerTimes.MWL, 0, tune);
        double[] autoS = PrayerCalc.toLocal(utcS, 2027, 7, 15, Daylight.AUTO, ny);
        double[] onS = PrayerCalc.toLocal(utcS, 2027, 7, 15, Daylight.ON, ny);
        double[] offS = PrayerCalc.toLocal(utcS, 2027, 7, 15, Daylight.OFF, ny);
        for (int i = 0; i < 7; i++) {
            check("summer auto == TV offset [" + i + "]",
                    near(wrap(autoS[i]), wrap(utcS[i] - 4.0), 1e-9),
                    autoS[i] + " vs " + (utcS[i] - 4.0));
            check("summer on == auto + 1h [" + i + "]",
                    near(wrap(onS[i] - autoS[i]), 1.0, 1e-9),
                    autoS[i] + " -> " + onS[i]);
            check("summer off == auto - 1h [" + i + "]",
                    near(wrap(autoS[i] - offS[i]), 1.0, 1e-9),
                    autoS[i] + " -> " + offS[i]);
        }

        // ---- auto uses the zone offset at each prayer's own instant ----
        double[] utcT = PrayerTimes.getTimes(2027, 3, 14, lat, lng, 0,
                PrayerTimes.MWL, 0, tune); // US spring-forward day
        double[] autoT = PrayerCalc.toLocal(utcT, 2027, 3, 14, Daylight.AUTO, ny);
        Instant day = LocalDate.of(2027, 3, 14).atStartOfDay(ZoneOffset.UTC).toInstant();
        for (int i = 0; i < 7; i++) {
            Instant at = day.plusSeconds(Math.round(utcT[i] * 3600.0));
            double expected = utcT[i] + ny.getRules().getOffset(at).getTotalSeconds() / 3600.0;
            check("transition-day offset [" + i + "]",
                    near(wrap(autoT[i]), wrap(expected), 1e-9),
                    autoT[i] + " vs " + expected);
        }

        // ---- toMillis rebuilds the wall clock on a fall-back day ----
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.NOVEMBER, 1, 12, 0, 0);
        long ms = PrayerCalc.toMillis(6.0, c);
        ZonedDateTime z = Instant.ofEpochMilli(ms).atZone(ny);
        check("toMillis keeps 06:00 on fall-back day",
                z.getHour() == 6 && z.getMinute() == 0, z.toString());

        // ---- sanity: NYC summer Fajr is before sunrise and Dhuhr near 13:00 ----
        check("summer fajr < sunrise", autoS[0] < autoS[1], autoS[0] + " / " + autoS[1]);
        check("summer dhuhr in 12:30..13:30", autoS[2] > 12.5 && autoS[2] < 13.5,
                String.valueOf(autoS[2]));

        // ---- a half-hour zone still maps through its rules ----
        ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        double[] utcK = PrayerTimes.getTimes(2027, 7, 15, 22.57, 88.36, 0,
                PrayerTimes.MWL, 0, tune);
        double[] autoK = PrayerCalc.toLocal(utcK, 2027, 7, 15, Daylight.AUTO, kolkata);
        check("Kolkata +5:30 dhuhr ~11:42 (east of meridian)",
                autoK[2] > 11.2 && autoK[2] < 12.2, String.valueOf(autoK[2]));

        // ---- DST mode constants and labels ----
        check("offset label +5:30",
                Daylight.offsetLabel(5.5).equals("UTC+5:30"),
                Daylight.offsetLabel(5.5));
        check("offset label -4",
                Daylight.offsetLabel(-4).equals("UTC−4"),
                Daylight.offsetLabel(-4));

        System.out.println();
        System.out.println(passed + " checks passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL  " + name + "  (" + detail + ")");
        }
    }

    private static boolean near(double a, double b, double eps) {
        return Math.abs(a - b) < eps;
    }

    private static double wrap(double h) {
        return ((h % 24) + 24) % 24;
    }
}
