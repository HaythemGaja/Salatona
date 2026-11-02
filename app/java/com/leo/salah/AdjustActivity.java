package com.leo.salah;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Per-prayer minute fine-tuning (-120..+120). */
public class AdjustActivity extends BaseActivity {

    private static final int[] IDX = {0, 1, 2, 3, 4, 5};
    private static final String[] NAMES = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    private LinearLayout list;
    private Prefs p;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = new Prefs(this);
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 32);
        root.setPadding(pad, Ui.dp(this, 24), pad, pad);
        root.setBackgroundColor(Ui.BG);
        sv.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = Ui.label(this, "Fine-tune prayer times", 30, Ui.TEXT);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);
        TextView hint = Ui.label(this,
                "Shift any prayer a few minutes earlier or later to match your local mosque.",
                17, Ui.DIM);
        Ui.margin(hint, 0, Ui.dp(this, 8), 0, Ui.dp(this, 16));
        root.addView(hint);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuild();
    }

    private void rebuild() {
        list.removeAllViews();
        for (int k = 0; k < IDX.length; k++) {
            final int ti = IDX[k];
            LinearLayout row = Ui.card(this);
            row.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 12));
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            Ui.margin(row, 0, 0, 0, Ui.dp(this, 8));
            LinearLayout inner = Ui.hrow(this);
            TextView name = Ui.label(this, Lang.prayer(k), 20, Ui.TEXT);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            inner.addView(name, lp);

            int cur = p.tune(ti);
            Button minus = Ui.bigButton(this, "−");
            minus.setMinHeight(Ui.dp(this, 52));
            minus.setMinWidth(Ui.dp(this, 64));
            TextView val = Ui.label(this, fmt(cur), 20, Ui.GOLD);
            val.setGravity(Gravity.CENTER);
            val.setMinWidth(Ui.dp(this, 110));
            Button plus = Ui.bigButton(this, "+");
            plus.setMinHeight(Ui.dp(this, 52));
            plus.setMinWidth(Ui.dp(this, 64));
            minus.setOnClickListener(v -> { p.setTune(ti, Math.max(-120, cur - 1)); changed(); });
            plus.setOnClickListener(v -> { p.setTune(ti, Math.min(120, cur + 1)); changed(); });
            inner.addView(minus); inner.addView(val); inner.addView(plus);
            row.addView(inner);
            list.addView(row);
        }
        Lang.localize(list);
    }

    private String fmt(int m) {
        if (m == 0) return Lang.t("±0 min");
        return Lang.num((m > 0 ? "+" : "−") + Math.abs(m) + " min");
    }

    private void changed() {
        try { AthanScheduler.scheduleAll(this); } catch (Exception ignored) {}
        rebuild();
    }
}
