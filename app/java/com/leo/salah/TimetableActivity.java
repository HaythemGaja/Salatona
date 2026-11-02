package com.leo.salah;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Prayer timetable with three modes: Gregorian month, Hijri month, or
 * 30 days starting today. D-pad friendly: mode buttons + month selector on
 * top, day rows focusable for scrolling.
 */
public class TimetableActivity extends BaseActivity {

    private static final int MODE_GREG = 0, MODE_HIJRI = 1, MODE_DAYS = 2;

    private static final String[] COLS =
            {"Day", "Fajr", "Shurûq", "Dhuhr", "Asr", "Maghrib", "Isha"};

    private LinearLayout table, selRow;
    private TextView monthLabel;
    private final TextView[] modeViews = new TextView[3];
    private int mode = MODE_GREG;
    private int year, month; // gregorian month 1..12
    private int hijriShift; // months from the current Hijri month

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Calendar now = Calendar.getInstance();
        year = now.get(Calendar.YEAR);
        month = now.get(Calendar.MONTH) + 1;

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 28);
        root.setPadding(pad, Ui.dp(this, 20), pad, pad);
        root.setBackgroundColor(Ui.BG);
        sv.addView(root);

        TextView title = Ui.label(this, "\uD83D\uDCC6 Prayer Timetable", 30, Ui.TEXT);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView city = Ui.label(this, Lang.city(new Prefs(this).cityName(),
                new Prefs(this).cityCountry()), 17, Ui.DIM);
        city.setGravity(Gravity.CENTER);
        Ui.margin(city, 0, Ui.dp(this, 4), 0, 0);
        root.addView(city);

        // ---- mode toggle: Gregorian | Hijri | 30 days ----
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.setGravity(Gravity.CENTER);
        Ui.margin(modes, 0, Ui.dp(this, 12), 0, 0);
        String[] labels = {"Gregorian", "Hijri", "30 days"};
        for (int i = 0; i < 3; i++) {
            final int m = i;
            TextView tv = Ui.label(this, labels[i], 18, Ui.DIM);
            tv.setFocusable(true);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), Ui.dp(this, 8));
            tv.setOnClickListener(v -> {
                mode = m;
                paintModes();
                rebuild();
            });
            tv.setOnFocusChangeListener((v, has) -> {
                if (has) v.setBackground(Ui.focusBg(this, Ui.dp(this, 10)));
                else paintModes();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            int hm = Ui.dp(this, 6);
            lp.setMargins(hm, 0, hm, 0);
            tv.setLayoutParams(lp);
            modeViews[i] = tv;
            modes.addView(tv);
        }
        root.addView(modes);
        paintModes();

        // ---- month selector (Gregorian + Hijri modes) ----
        selRow = new LinearLayout(this);
        selRow.setOrientation(LinearLayout.HORIZONTAL);
        selRow.setGravity(Gravity.CENTER);
        Ui.margin(selRow, 0, Ui.dp(this, 10), 0, 0);

        TextView prev = Ui.label(this, "◀", 26, Ui.GOLD_BRIGHT);
        prev.setFocusable(true);
        prev.setTextColor(Ui.focusTextColors());
        prev.setPadding(Ui.dp(this, 18), Ui.dp(this, 6), Ui.dp(this, 18), Ui.dp(this, 6));
        prev.setOnClickListener(v -> shiftMonth(-1));
        prev.setBackground(Ui.focusBg(this, Ui.dp(this, 10)));
        selRow.addView(prev);

        monthLabel = Ui.label(this, "", 24, Ui.TEXT);
        monthLabel.setTypeface(monthLabel.getTypeface(), Typeface.BOLD);
        monthLabel.setGravity(Gravity.CENTER);
        monthLabel.setMinWidth(Ui.dp(this, 320));
        selRow.addView(monthLabel);

        TextView next = Ui.label(this, "▶", 26, Ui.GOLD_BRIGHT);
        next.setFocusable(true);
        next.setTextColor(Ui.focusTextColors());
        next.setPadding(Ui.dp(this, 18), Ui.dp(this, 6), Ui.dp(this, 18), Ui.dp(this, 6));
        next.setOnClickListener(v -> shiftMonth(1));
        next.setBackground(Ui.focusBg(this, Ui.dp(this, 10)));
        selRow.addView(next);
        root.addView(selRow);

        // ---- table ----
        table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setBackground(Ui.roundBgStroke(0xFF0E1430, Ui.GOLD, Ui.dp(this, 2),
                Ui.dp(this, 14)));
        int tp = Ui.dp(this, 6);
        table.setPadding(tp, tp, tp, tp);
        Ui.margin(table, 0, Ui.dp(this, 14), 0, 0);
        root.addView(table);

        TextView sp = Ui.label(this, "", 16, Ui.BG);
        Ui.margin(sp, 0, Ui.dp(this, 40), 0, 0);
        root.addView(sp);

        rebuild();
        setContentView(sv);
    }

    private void paintModes() {
        for (int i = 0; i < 3; i++) {
            boolean sel = (i == mode);
            modeViews[i].setTextColor(sel ? 0xFF3E2F00 : Ui.DIM);
            modeViews[i].setTypeface(modeViews[i].getTypeface(),
                    sel ? Typeface.BOLD : Typeface.NORMAL);
            modeViews[i].setBackground(sel
                    ? Ui.roundBg(Ui.GOLD, Ui.dp(this, 10))
                    : null);
        }
    }

    private void shiftMonth(int d) {
        if (mode == MODE_HIJRI) {
            hijriShift += d;
        } else {
            month += d;
            if (month < 1) { month = 12; year--; }
            if (month > 12) { month = 1; year++; }
        }
        rebuild();
    }

    private void rebuild() {
        selRow.setVisibility(mode == MODE_DAYS ? View.GONE : View.VISIBLE);
        if (mode == MODE_GREG) rebuildGreg();
        else if (mode == MODE_HIJRI) rebuildHijri();
        else rebuildDays30();
        Lang.localize(table);
    }

    private static final String[] HIJRI_SHORT = {"Muh", "Saf", "Rab I", "Rab II",
            "Jum I", "Jum II", "Raj", "Sha", "Ram", "Shw", "Dhu Q", "Dhu H"};

    /** Short Hijri date ("16 Rab II") for a Gregorian date. */
    private String hijriShort(LocalDate g) {
        try {
            int off = new Prefs(this).hijriOffset();
            HijrahDate h = HijrahDate.from(g).plus(off, ChronoUnit.DAYS);
            int d = h.get(ChronoField.DAY_OF_MONTH);
            int m = h.get(ChronoField.MONTH_OF_YEAR);
            if (Lang.ar()) return Lang.num(d + " " + Lang.hijriMonth(m));
            return d + " " + HIJRI_SHORT[m - 1];
        } catch (Exception e) {
            return "";
        }
    }

    // ---------------- Gregorian month ----------------

    private void rebuildGreg() {
        monthLabel.setText(Lang.num(new SimpleDateFormat("MMMM yyyy", Lang.locale())
                .format(new Calendar.Builder().setDate(year, month - 1, 1).build()
                        .getTime())));
        table.removeAllViews();
        addHeader();

        Calendar now = Calendar.getInstance();
        boolean thisMonth = now.get(Calendar.YEAR) == year
                && now.get(Calendar.MONTH) + 1 == month;
        int today = now.get(Calendar.DAY_OF_MONTH);
        Calendar cal = new Calendar.Builder().setDate(year, month - 1, 1).build();
        int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int d = 1; d <= days; d++) {
            boolean isToday = thisMonth && d == today;
            double[] t = PrayerCalc.timesFor(this, year, month, d);
            addDayRow(Lang.num(String.valueOf(d)) + "\n"
                            + hijriShort(LocalDate.of(year, month, d)),
                    t, isToday, d < days);
        }
    }

    // ---------------- Hijri month ----------------

    private void rebuildHijri() {
        HijrahDate first;
        try {
            int off = new Prefs(this).hijriOffset();
            first = HijrahDate.now().plus(off, ChronoUnit.DAYS)
                    .plus(hijriShift, ChronoUnit.MONTHS)
                    .with(ChronoField.DAY_OF_MONTH, 1);
        } catch (Exception e) {
            first = HijrahDate.now().with(ChronoField.DAY_OF_MONTH, 1);
        }
        int hYear, hMonth, days;
        try {
            hYear = first.get(ChronoField.YEAR);
            hMonth = first.get(ChronoField.MONTH_OF_YEAR);
            days = first.lengthOfMonth();
        } catch (Exception e) {
            int[] h = PrayerCalc.hijriToday(this);
            hYear = h[2]; hMonth = h[1]; days = 30;
            first = HijrahDate.now().with(ChronoField.DAY_OF_MONTH, 1);
        }
        monthLabel.setText(Lang.ar()
                ? Lang.hijriMonth(hMonth) + " " + Lang.num(hYear + "") + " هـ"
                : PrayerCalc.hijriMonthName(hMonth) + " " + hYear + " AH");
        table.removeAllViews();
        addHeader();

        HijrahDate todayH;
        try {
            todayH = HijrahDate.now().plus(new Prefs(this).hijriOffset(), ChronoUnit.DAYS);
        } catch (Exception e) { todayH = null; }
        SimpleDateFormat gf = new SimpleDateFormat(Lang.ar() ? "d MMM" : "MMM d", Lang.locale());
        for (int d = 1; d <= days; d++) {
            boolean isToday = false;
            String dayLabel = String.valueOf(d);
            double[] t = null;
            try {
                HijrahDate hd = first.plus(d - 1, ChronoUnit.DAYS);
                LocalDate g = LocalDate.from(hd);
                t = PrayerCalc.timesFor(this, g.getYear(), g.getMonthValue(),
                        g.getDayOfMonth());
                Date gd = Date.from(g.atStartOfDay(ZoneId.systemDefault()).toInstant());
                dayLabel = Lang.num(String.valueOf(d)) + "\n" + Lang.num(gf.format(gd));
                isToday = todayH != null && hd.equals(todayH);
            } catch (Exception ignored) {}
            if (t == null) continue;
            addDayRow(dayLabel, t, isToday, d < days);
        }
    }

    // ---------------- 30 days from today ----------------

    private void rebuildDays30() {
        table.removeAllViews();
        addHeader();
        SimpleDateFormat f = new SimpleDateFormat(Lang.ar() ? "EEE، d MMM" : "EEE, MMM d", Lang.locale());
        for (int i = 0; i < 30; i++) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, i);
            double[] t = PrayerCalc.timesFor(this, cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
            LocalDate g = LocalDate.of(cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
            addDayRow(Lang.num(f.format(cal.getTime())) + "\n" + hijriShort(g), t, i == 0, i < 29);
        }
    }

    // ---------------- shared row builders ----------------

    private void addHeader() {
        LinearLayout header = row(false);
        for (String c : COLS) {
            TextView t = Ui.label(this, Lang.t(c), 15, Ui.GOLD);
            t.setTypeface(t.getTypeface(), Typeface.BOLD);
            t.setGravity(Gravity.CENTER);
            header.addView(cell(t));
        }
        table.addView(header);
        table.addView(divider());
    }

    private void addDayRow(String dayLabel, double[] t, boolean isToday, boolean divider) {
        LinearLayout r = row(true);
        r.setTag(isToday);
        paintRow(r, isToday, false);
        r.setOnFocusChangeListener((v, has) ->
                paintRow((LinearLayout) v, (Boolean) v.getTag(), has));

        String[] vals = {
                dayLabel,
                PrayerCalc.formatTime(this, t[0]), PrayerCalc.formatTime(this, t[1]),
                PrayerCalc.formatTime(this, t[2]), PrayerCalc.formatTime(this, t[3]),
                PrayerCalc.formatTime(this, t[4]), PrayerCalc.formatTime(this, t[5])};
        for (int i = 0; i < vals.length; i++) {
            TextView tv = Ui.label(this, vals[i], i == 0 ? 16 : 17,
                    isToday ? Ui.GOLD_BRIGHT : Ui.TEXT);
            if (i == 0) tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            r.addView(cell(tv));
        }
        table.addView(r);
        if (divider) table.addView(divider());
    }

    private void paintRow(LinearLayout r, boolean isToday, boolean focused) {
        if (focused) r.setBackgroundColor(0xFF3A2F10);
        else if (isToday) r.setBackgroundColor(0x3DD4AF37);
        else r.setBackgroundColor(0x00000000);
    }

    private LinearLayout row(boolean focusable) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setFocusable(focusable);
        return r;
    }

    private LinearLayout cell(TextView t) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        int hp = Ui.dp(this, 4);
        c.setPadding(hp, Ui.dp(this, 7), hp, Ui.dp(this, 7));
        c.setLayoutParams(lp);
        c.addView(t);
        return c;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(0x22FFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1));
        int hp = Ui.dp(this, 8);
        lp.setMargins(hp, 0, hp, 0);
        v.setLayoutParams(lp);
        return v;
    }
}
