package com.leo.salah;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Shared dashboard: header, big clock, weather + moon, full-day prayer
 * timeline (gold progress sweeps as the day advances), next-prayer countdown
 * pill, next-event countdown. Pure display — no interaction, no tracking.
 * Used by MainActivity and SalahDream (screensaver).
 */
public class DashboardView extends LinearLayout {

    private TextView headerView, dateView, ramadanView, clockView, countdownView;
    private TextView elapsedView, weatherView, phaseNameView;
    private LinearLayout ramadanCard;
    private MoonView moonView;
    private TextView eventView;
    private PrayerTimelineView timeline;
    /** Today's six timeline node times (Fajr..Isha) in millis. */
    private final long[] nodeMs = new long[6];
    /** Salah-day window: islamic midnight -> islamic midnight. */
    private long winStartMs, winEndMs;
    private double[] todayTimes;
    private String currentDay = "";
    private long tomorrowFajrMs, yesterdayIshaMs;

    // sky-driving state, read by MainActivity / SalahDream tickers
    private int phasePrayer = 4;
    private float phaseProgress;
    private int weatherKind = Weather.CLEAR;
    private long lastWeatherMs;
    private long phaseStartMs;

    private static final SimpleDateFormat CLOCK_FMT =
            new SimpleDateFormat("hh:mm:ss a", Locale.US);

    public int phasePrayer() { return phasePrayer; }
    public float phaseProgress() { return phaseProgress; }
    public int weatherKind() { return weatherKind; }

    public DashboardView(Context c) {
        super(c);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        build();
    }

    private Context ctx() { return getContext(); }

    private void build() {
        headerView = Ui.label(ctx(), "", 30, Ui.TEXT);
        headerView.setTypeface(headerView.getTypeface(), android.graphics.Typeface.BOLD);
        headerView.setGravity(Gravity.CENTER);
        addView(headerView);

        dateView = Ui.label(ctx(), "", 17, 0xFFE0E0E0);
        dateView.setGravity(Gravity.CENTER);
        Ui.margin(dateView, 0, Ui.dp(ctx(), 2), 0, 0);
        addView(dateView);

        // Ramadan mode card: prominent Suhoor/Iftar countdown, only in Ramadan
        ramadanCard = new LinearLayout(ctx());
        ramadanCard.setOrientation(HORIZONTAL);
        ramadanCard.setGravity(Gravity.CENTER);
        ramadanCard.setBackground(Ui.roundBgStroke(0xFF4A3A10, Ui.GOLD,
                Ui.dp(ctx(), 2), Ui.dp(ctx(), 18)));
        int rp = Ui.dp(ctx(), 10);
        ramadanCard.setPadding(Ui.dp(ctx(), 28), rp, Ui.dp(ctx(), 28), rp);
        Ui.margin(ramadanCard, 0, Ui.dp(ctx(), 6), 0, 0);
        ramadanView = Ui.label(ctx(), "", 20, Ui.GOLD_BRIGHT);
        ramadanView.setTypeface(ramadanView.getTypeface(), android.graphics.Typeface.BOLD);
        ramadanView.setGravity(Gravity.CENTER);
        ramadanCard.addView(ramadanView);
        addView(ramadanCard);

        eventView = Ui.label(ctx(), "", 19, Ui.GOLD_BRIGHT);
        eventView.setTypeface(eventView.getTypeface(), android.graphics.Typeface.BOLD);
        eventView.setGravity(Gravity.CENTER);
        Ui.margin(eventView, 0, Ui.dp(ctx(), 4), 0, 0);
        addView(eventView);

        clockView = Ui.label(ctx(), "--:--:--", 54, Ui.TEXT);
        clockView.setTypeface(clockView.getTypeface(), android.graphics.Typeface.BOLD);
        clockView.setGravity(Gravity.CENTER);
        Ui.margin(clockView, 0, Ui.dp(ctx(), 4), 0, 0);
        addView(clockView);

        // weather + phase-accurate moon icon + moon phase name, one centered row
        LinearLayout wrow = new LinearLayout(ctx());
        wrow.setOrientation(HORIZONTAL);
        wrow.setGravity(Gravity.CENTER);
        Ui.margin(wrow, 0, Ui.dp(ctx(), 2), 0, 0);
        weatherView = Ui.label(ctx(), "", 17, 0xFFE0E0E0);
        weatherView.setGravity(Gravity.CENTER);
        wrow.addView(weatherView);
        moonView = new MoonView(ctx());
        Ui.margin(moonView, Ui.dp(ctx(), 12), 0, 0, 0);
        wrow.addView(moonView);
        phaseNameView = Ui.label(ctx(), "", 17, Ui.GOLD_BRIGHT);
        Ui.margin(phaseNameView, Ui.dp(ctx(), 8), 0, 0, 0);
        wrow.addView(phaseNameView);
        addView(wrow);
        // next-prayer countdown pill, centered above the timeline
        LinearLayout pillRow = new LinearLayout(ctx());
        pillRow.setOrientation(HORIZONTAL);
        pillRow.setGravity(Gravity.CENTER);
        Ui.margin(pillRow, 0, Ui.dp(ctx(), 10), 0, 0);
        LinearLayout pill = new LinearLayout(ctx());
        pill.setOrientation(VERTICAL);
        pill.setGravity(Gravity.CENTER);
        pill.setBackground(Ui.roundBg(0xCC060B2E, Ui.dp(ctx(), 20)));
        int pp = Ui.dp(ctx(), 12);
        pill.setPadding(Ui.dp(ctx(), 32), pp, Ui.dp(ctx(), 32), pp);
        countdownView = Ui.label(ctx(), "", 26, Ui.GOLD_BRIGHT);
        countdownView.setTypeface(countdownView.getTypeface(), android.graphics.Typeface.BOLD);
        countdownView.setGravity(Gravity.CENTER);
        pill.addView(countdownView);
        elapsedView = Ui.label(ctx(), "", 16, 0xFFB0BEC5);
        elapsedView.setGravity(Gravity.CENTER);
        pill.addView(elapsedView);
        pillRow.addView(pill);
        addView(pillRow);

        // full-day prayer timeline: 7 nodes over a rolling 24h, gold progress sweeps with the day
        timeline = new PrayerTimelineView(ctx());
        Ui.margin(timeline, 0, Ui.dp(ctx(), 14), 0, 0);
        addView(timeline, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
    }
    // ---------------- data ----------------

    public void refreshDay() {
        Context c = ctx();
        Calendar cal = Calendar.getInstance();
        currentDay = PrayerCalc.dayKey(cal);
        Prefs p = new Prefs(c);

        headerView.setText("🕌 مواقيت الصلاة - " + p.cityName().toUpperCase());
        String dateLine = String.format("%1$tA, %1$tB %1$te, %1$tY", cal)
                + "  ·  " + PrayerCalc.hijriDate();
        if (p.dstMode() != Daylight.AUTO) {
            dateLine += "  ·  " + (p.dstMode() == Daylight.ON
                    ? "Time +1h" : "Time −1h");
        }
        dateView.setText(dateLine);

        todayTimes = PrayerCalc.timesFor(c, cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        // neighbors for the pre-Fajr / post-Isha edges and the sky progress
        Calendar tmr = (Calendar) cal.clone();
        tmr.add(Calendar.DATE, 1);
        double[] tt = PrayerCalc.timesFor(c, tmr.get(Calendar.YEAR),
                tmr.get(Calendar.MONTH) + 1, tmr.get(Calendar.DAY_OF_MONTH));
        tomorrowFajrMs = PrayerCalc.toMillis(tt[0], tmr);
        Calendar yst = (Calendar) cal.clone();
        yst.add(Calendar.DATE, -1);
        double[] yt = PrayerCalc.timesFor(c, yst.get(Calendar.YEAR),
                yst.get(Calendar.MONTH) + 1, yst.get(Calendar.DAY_OF_MONTH));
        yesterdayIshaMs = PrayerCalc.toMillis(yt[5], yst);
        // ---- salah-day window: islamic midnight is halfway between Isha and Fajr
        long nowMs = System.currentTimeMillis();
        long fajrT = PrayerCalc.toMillis(todayTimes[0], cal);
        long ishaT = PrayerCalc.toMillis(todayTimes[5], cal);
        long fajrY = PrayerCalc.toMillis(yt[0], yst);
        long mToday = (yesterdayIshaMs + fajrT) / 2;   // last night's midnight
        long mTomorrow = (ishaT + tomorrowFajrMs) / 2; // tonight's midnight
        double[] baseT; Calendar baseCal; long wStart, wEnd;
        if (nowMs >= mToday) {
            baseT = todayTimes; baseCal = cal; wStart = mToday; wEnd = mTomorrow;
        } else {
            // before islamic midnight: still yesterday's salah day
            Calendar dby = (Calendar) yst.clone();
            dby.add(Calendar.DATE, -1);
            double[] dt = PrayerCalc.timesFor(c, dby.get(Calendar.YEAR),
                    dby.get(Calendar.MONTH) + 1, dby.get(Calendar.DAY_OF_MONTH));
            long ishaDby = PrayerCalc.toMillis(dt[5], dby);
            wStart = (ishaDby + fajrY) / 2;
            wEnd = mToday;
            baseT = yt; baseCal = yst;
        }
        winStartMs = wStart; winEndMs = wEnd;
        for (int i = 0; i < 6; i++) nodeMs[i] = PrayerCalc.toMillis(baseT[i], baseCal);
        timeline.setDayTimes(nodeMs, wStart, wEnd);
        updateForecasts();

        boolean ramadan = PrayerCalc.isRamadan();
        ramadanCard.setVisibility(ramadan ? View.VISIBLE : View.GONE);

        String ev = IslamicEvents.displayText(IslamicEvents.next());
        eventView.setText(ev);
        eventView.setVisibility(ev.isEmpty() ? View.GONE : View.VISIBLE);

        lastWeatherMs = 0; // refresh weather right away on day/city change
        phaseNameView.setText(PrayerCalc.MOON_NAMES[PrayerCalc.moonPhase()]);
        moonView.invalidate();
    }

    /**
     * Current prayer = the latest prayer whose time has passed (computed
     * directly from today's times, no grace tricks), plus 0..1 progress
     * toward the next prayer. Single source of truth for the elapsed line
     * and the sky background.
     */
    private void updatePhase(long now) {
        if (todayTimes == null) return;
        Calendar cc = Calendar.getInstance();
        int cur = -1;
        long start = 0;
        for (int i = 0; i < 5; i++) {
            long ms = PrayerCalc.toMillis(todayTimes[PrayerCalc.TIME_IDX[i]], cc);
            if (ms <= now) { cur = i; start = ms; }
        }
        long nextStart;
        if (cur < 0) {
            // before today's Fajr: the current prayer is yesterday's Isha
            cur = 4;
            start = yesterdayIshaMs;
            nextStart = PrayerCalc.toMillis(todayTimes[0], cc);
        } else {
            int next = (cur + 1) % 5;
            nextStart = (next == 0) ? tomorrowFajrMs
                    : PrayerCalc.toMillis(todayTimes[PrayerCalc.TIME_IDX[next]], cc);
        }
        phasePrayer = cur;
        phaseStartMs = start;
        long span = nextStart - start;
        phaseProgress = span > 0
                ? Math.max(0f, Math.min(1f, (now - start) / (float) span)) : 0f;
    }

    /** "⏳ Dhuhr began 01:05 ago" — from the phase computed by updatePhase(). */
    private String elapsedText() {
        long mins = (System.currentTimeMillis() - phaseStartMs) / 60000;
        String when = mins < 1 ? "just started"
                : "began " + formatHM(mins * 60000) + " ago";
        return "⏳ " + PrayerCalc.NAMES[phasePrayer] + " " + when;
    }

    /** HH:MM, zero-padded ("00:32", "01:05"). */
    private static String formatHM(long ms) {
        long m = Math.max(0, ms / 60000);
        return String.format(Locale.US, "%02d:%02d", m / 60, m % 60);
    }

    /** Per-prayer forecast under each timeline node (from cached hourly data). */
    private void updateForecasts() {
        try {
            int[] temps = new int[6];
            int[] kinds = new int[6];
            for (int i = 0; i < 6; i++) {
                Weather.Forecast f = Weather.hourlyAt(ctx(), nodeMs[i]);
                temps[i] = f == null ? Integer.MIN_VALUE : f.tempC;
                kinds[i] = f == null ? -1 : f.kind;
            }
            timeline.setForecasts(temps, kinds);
        } catch (Exception ignored) {}
    }

    /** Fetches weather at most once a minute; updates the line + sky. */
    private void updateWeather(Context ctx, long now) {
        if (now - lastWeatherMs < 60_000) return;
        lastWeatherMs = now;
        Weather.refresh(ctx, info -> {
            weatherKind = info.kind;
            int mi = PrayerCalc.moonPhase();
            weatherView.post(() -> {
                weatherView.setText(info.text);
                phaseNameView.setText(PrayerCalc.MOON_NAMES[mi]);
                moonView.invalidate();
                updateForecasts();
            });
        });
    }

    /** Call every second. Handles clock, countdown, highlight flips and day rollover. */
    public void tick() {
        try {
            Calendar c = Calendar.getInstance();
            if (!PrayerCalc.dayKey(c).equals(currentDay)) {
                refreshDay();
                return;
            }
            long now0 = System.currentTimeMillis();
            if (winEndMs > 0 && (now0 >= winEndMs || now0 < winStartMs)) {
                refreshDay();
                return;
            }
            clockView.setText(CLOCK_FMT.format(new Date()));

            Context ctx = ctx();
            long now = System.currentTimeMillis();
            PrayerCalc.NextPrayer np = PrayerCalc.next(ctx, now);

            // timeline: which of the 6 nodes is next (-1 when the day is done)
            int next6 = -1;
            for (int i = 0; i < 6; i++) {
                if (nodeMs[i] > now) { next6 = i; break; }
            }
            timeline.setNow(now, next6);

            // next-prayer pill: "🕌 Isha in 00:32", plus the elapsed current prayer
            countdownView.setText("🕌 " + PrayerCalc.NAMES[np.prayer]
                    + " in " + formatHM(np.atMillis - now));

            updatePhase(now);
            elapsedView.setText(elapsedText());
            elapsedView.setTextColor(0xFFB0BEC5);

            updateWeather(ctx, now);

            if (PrayerCalc.isRamadan() && todayTimes != null) {
                long fajr = PrayerCalc.toMillis(todayTimes[0], c);
                long maghrib = PrayerCalc.toMillis(todayTimes[4], c);
                ramadanCard.setVisibility(View.VISIBLE);
                ramadanView.setText(now < fajr
                        ? "🌙 Suhoor ends " + PrayerCalc.formatTime(todayTimes[0])
                                + " · in " + formatHM(fajr - now)
                        : now < maghrib
                                ? "🌙 Iftar " + PrayerCalc.formatTime(todayTimes[4])
                                        + " · in " + formatHM(maghrib - now)
                                : "🌙 Iftar time — تقبل الله");
            } else {
                ramadanCard.setVisibility(View.GONE);
            }
        } catch (Exception ignored) {}
    }
}
