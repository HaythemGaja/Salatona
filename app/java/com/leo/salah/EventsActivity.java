package com.leo.salah;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Islamic events as a compact table: event (Arabic + English), Hijri date,
 * Gregorian date + weekday, and day countdown. The next upcoming event is
 * highlighted in gold.
 */
public class EventsActivity extends Activity {

    private static final float[] WEIGHTS = {2.4f, 1.5f, 1.7f, 1.4f};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 32);
        root.setPadding(pad, Ui.dp(this, 24), pad, pad);
        root.setBackgroundColor(Ui.BG);
        sv.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = Ui.label(this, "\uD83D\uDDD3\uFE0F Islamic Events", 30, Ui.TEXT);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        int hy = IslamicEvents.currentHijriYear();
        TextView sub = Ui.label(this,
                hy > 0 ? "Hijri year " + hy + " · dates may differ \u00B11 day from moon sighting" : "",
                16, Ui.DIM);
        sub.setGravity(Gravity.CENTER);
        Ui.margin(sub, 0, Ui.dp(this, 6), 0, 0);
        root.addView(sub);

        // ---- table ----
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setBackground(Ui.roundBgStroke(0xFF0E1430, Ui.GOLD, Ui.dp(this, 2),
                Ui.dp(this, 14)));
        int tp = Ui.dp(this, 6);
        table.setPadding(tp, tp, tp, tp);
        Ui.margin(table, 0, Ui.dp(this, 18), 0, 0);

        LinearLayout header = row();
        header.addView(hcell("EVENT", 0));
        header.addView(hcell("HIJRI DATE", 1));
        header.addView(hcell("GREGORIAN", 2));
        header.addView(hcell("COUNTDOWN", 3));
        table.addView(header);
        table.addView(divider(0xFFD4AF37));

        SimpleDateFormat gf = new SimpleDateFormat("d MMM yyyy", Locale.US);
        SimpleDateFormat wf = new SimpleDateFormat("EEEE", Locale.US);

        boolean nextMarked = false;
        boolean firstRow = true;
        for (IslamicEvents.EventInfo e : IslamicEvents.all()) {
            boolean isNext = !nextMarked && e.daysFromToday >= 0;
            if (isNext) nextMarked = true;
            if (!firstRow) table.addView(divider(0x22FFFFFF));
            firstRow = false;
            table.addView(eventRow(e, isNext, gf.format(new Date(e.gregorianMillis)),
                    wf.format(new Date(e.gregorianMillis))));
        }
        root.addView(table);

        TextView legend = Ui.label(this,
                "\u2B50 highlighted row = the next upcoming event", 15, Ui.DIM);
        legend.setGravity(Gravity.CENTER);
        Ui.margin(legend, 0, Ui.dp(this, 10), 0, 0);
        root.addView(legend);

        // bottom spacer for D-pad scrolling
        TextView sp = Ui.label(this, "", 16, Ui.BG);
        Ui.margin(sp, 0, Ui.dp(this, 40), 0, 0);
        root.addView(sp);

        setContentView(sv);
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private LinearLayout cell(int col) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, WEIGHTS[col]);
        int hp = Ui.dp(this, 10);
        c.setPadding(hp, Ui.dp(this, 8), hp, Ui.dp(this, 8));
        c.setLayoutParams(lp);
        return c;
    }

    private LinearLayout hcell(String text, int col) {
        LinearLayout c = cell(col);
        TextView t = Ui.label(this, text, 15, Ui.GOLD);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        c.addView(t);
        return c;
    }

    private android.view.View divider(int color) {
        android.view.View v = new android.view.View(this);
        v.setBackgroundColor(color);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1));
        int hp = Ui.dp(this, 8);
        lp.setMargins(hp, 0, hp, 0);
        v.setLayoutParams(lp);
        return v;
    }

    private LinearLayout eventRow(IslamicEvents.EventInfo e, boolean isNext,
                                 String greg, String weekday) {
        LinearLayout r = row();
        if (isNext) r.setBackgroundColor(0x3DD4AF37);

        // event: English + Arabic
        LinearLayout c0 = cell(0);
        TextView en = Ui.label(this, e.name, 19, Ui.TEXT);
        en.setTypeface(en.getTypeface(), android.graphics.Typeface.BOLD);
        c0.addView(en);
        TextView ar = Ui.label(this, e.nameAr, 16, Ui.GOLD_BRIGHT);
        c0.addView(ar);
        r.addView(c0);

        // hijri date
        LinearLayout c1 = cell(1);
        TextView h = Ui.label(this,
                e.hijriDay + " " + IslamicEvents.HIJRI_MONTHS[e.hijriMonth], 18, Ui.TEXT);
        c1.addView(h);
        TextView hy = Ui.label(this, e.hijriYear + " AH", 14, Ui.DIM);
        c1.addView(hy);
        r.addView(c1);

        // gregorian date + weekday
        LinearLayout c2 = cell(2);
        TextView g = Ui.label(this, greg, 18, Ui.TEXT);
        c2.addView(g);
        TextView w = Ui.label(this, weekday, 14, Ui.DIM);
        c2.addView(w);
        r.addView(c2);

        // countdown
        LinearLayout c3 = cell(3);
        TextView cd;
        if (e.daysFromToday > 1) {
            cd = Ui.label(this, "in " + e.daysFromToday + " days", 18, Ui.GOLD_BRIGHT);
        } else if (e.daysFromToday == 1) {
            cd = Ui.label(this, "tomorrow", 18, Ui.GOLD_BRIGHT);
        } else if (e.daysFromToday == 0) {
            cd = Ui.label(this, "today!", 18, Ui.GOLD_BRIGHT);
        } else if (e.daysFromToday == -1) {
            cd = Ui.label(this, "yesterday", 18, Ui.DIM);
        } else {
            cd = Ui.label(this, (-e.daysFromToday) + " days ago", 18, Ui.DIM);
        }
        cd.setTypeface(cd.getTypeface(), android.graphics.Typeface.BOLD);
        c3.addView(cd);
        r.addView(c3);

        return r;
    }
}
