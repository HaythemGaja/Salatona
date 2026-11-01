package com.leo.salah;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/** Schedules exact alarms for every prayer (athan + optional pre-reminder). */
public class AthanScheduler {

    public static void scheduleAll(Context ctx) {
        Prefs p = new Prefs(ctx);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        // clear previous (2 days x 5 prayers x {athan, pre})
        for (int d = 0; d < 2; d++)
            for (int i = 0; i < 5; i++) {
                am.cancel(pending(ctx, d * 20 + i * 2, i, false));
                am.cancel(pending(ctx, d * 20 + i * 2 + 1, i, true));
            }
        // clear gentle reminders (taraweeh + adhkar, 2 days)
        for (int d = 0; d < 2; d++) {
            am.cancel(gentlePending(ctx, 200 + d, "taraweeh"));
            am.cancel(gentlePending(ctx, 210 + d, "adhkar_morning"));
            am.cancel(gentlePending(ctx, 220 + d, "adhkar_evening"));
        }

        long now = System.currentTimeMillis();
        Calendar today = Calendar.getInstance();
        scheduleDay(ctx, am, p, today, 0, now);
        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DATE, 1);
        scheduleDay(ctx, am, p, tomorrow, 1, now);

        p.setScheduledDay(PrayerCalc.dayKey(today));
    }

    private static void scheduleDay(Context ctx, AlarmManager am, Prefs p,
                                    Calendar day, int dayOffset, long now) {
        double[] t = PrayerCalc.timesFor(ctx, day.get(Calendar.YEAR),
                day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH));
        for (int i = 0; i < 5; i++) {
            long at = PrayerCalc.toMillis(t[PrayerCalc.TIME_IDX[i]], day);
            if (p.athanOn(i) && at > now + 30_000) {
                // athan itself goes through setAlarmClock: the strongest alarm
                // type — it fires on time even in deep Doze, which is what was
                // delaying the call on some nights.
                setExact(am, at, pending(ctx, dayOffset * 20 + i * 2, i, false),
                        true, ctx);
            }
            int pre = p.preMin();
            if (pre > 0 && at - pre * 60_000L > now + 30_000) {
                setExact(am, at - pre * 60_000L, pending(ctx, dayOffset * 20 + i * 2 + 1, i, true),
                        false, ctx);
            }
        }

        // ---- gentle reminders for this day ----
        // taraweeh: 20 min after Isha, only in Ramadan and only if athan is on for Isha
        if (dayOffset == 0 && p.taraweeh() && p.athanOn(4) && isRamadanDay(day)) {
            long isha = PrayerCalc.toMillis(t[PrayerCalc.TIME_IDX[4]], day);
            long at = isha + 20 * 60_000L;
            if (at > now + 30_000)
                setExact(am, at, gentlePending(ctx, 200 + dayOffset, "taraweeh"),
                        false, ctx);
        }
    }

    /** True if the given calendar day falls in Ramadan (Hijri month 9). */
    private static boolean isRamadanDay(Calendar day) {
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate target = java.time.LocalDate.of(day.get(Calendar.YEAR),
                    day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH));
            long diff = java.time.temporal.ChronoUnit.DAYS.between(today, target);
            java.time.chrono.HijrahDate hd = java.time.chrono.HijrahDate.now()
                    .plus(diff, java.time.temporal.ChronoUnit.DAYS);
            return hd.get(java.time.temporal.ChronoField.MONTH_OF_YEAR) == 9;
        } catch (Exception e) {
            return PrayerCalc.isRamadan();
        }
    }

    private static PendingIntent gentlePending(Context ctx, int req, String kind) {
        Intent in = new Intent(ctx, AthanAlarmReceiver.class);
        in.setAction("com.leo.salah.GENTLE_" + req);
        in.putExtra("kind", kind);
        return PendingIntent.getBroadcast(ctx, req, in,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent pending(Context ctx, int req, int prayer, boolean pre) {
        Intent in = new Intent(ctx, AthanAlarmReceiver.class);
        in.setAction("com.leo.salah.ATHAN_" + req);
        in.putExtra("prayer", prayer);
        in.putExtra("pre", pre);
        return PendingIntent.getBroadcast(ctx, req, in,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void setExact(AlarmManager am, long when, PendingIntent pi,
                               boolean alarmClock, Context ctx) {
        if (alarmClock) {
            // Alarm-clock alarms fire on time even in deep Doze and are not
            // subject to the allow-while-idle batching that delayed the athan.
            try {
                Intent show = new Intent(ctx, MainActivity.class);
                PendingIntent showPi = PendingIntent.getActivity(ctx, 9876, show,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, showPi), pi);
                return;
            } catch (Exception ignored) {}
        }
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
                    return;
                }
            } catch (Exception ignored) {}
        }
        if (Build.VERSION.SDK_INT >= 23)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        else
            am.set(AlarmManager.RTC_WAKEUP, when, pi);
    }
}
