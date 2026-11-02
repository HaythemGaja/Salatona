package com.leo.salah;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Live Quran radio: pick a station, leave it playing in the background. */
public class RadioActivity extends BaseActivity {

    private LinearLayout list;
    private TextView status;
    private final Handler poll = new Handler(Looper.getMainLooper());
    private final Runnable poller = new Runnable() {
        @Override public void run() {
            refresh();
            poll.postDelayed(this, 700);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 32);
        root.setPadding(pad, Ui.dp(this, 20), pad, Ui.dp(this, 18));
        root.setBackgroundColor(Ui.BG);

        TextView title = Ui.label(this, Lang.t("📻 Quran Radio"), 32, Ui.TEXT);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView sub = Ui.label(this, Lang.t("Live Quran recitation"), 16, Ui.DIM);
        Ui.margin(sub, 0, Ui.dp(this, 4), 0, Ui.dp(this, 14));
        root.addView(sub);

        LinearLayout bar = Ui.card(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 12));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.setMargins(0, 0, 0, Ui.dp(this, 12));
        root.addView(bar, blp);

        status = Ui.label(this, "", 18, Ui.GOLD_BRIGHT);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        bar.addView(status, slp);

        android.widget.Button stop = Ui.bigButton(this, Lang.t("Stop radio"));
        stop.setOnClickListener(v -> stopRadio());
        bar.addView(stop);

        ScrollView scroll = new ScrollView(this);
        scroll.setFocusable(false);
        scroll.setVerticalScrollBarEnabled(false);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 0, 0, Ui.dp(this, 24));
        scroll.addView(list, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        buildStations();
        setContentView(root);
        refresh();
        if (list.getChildCount() > 0) list.getChildAt(0).post(list.getChildAt(0)::requestFocus);
    }

    @Override
    protected void onResume() {
        super.onResume();
        poll.post(poller);
    }

    @Override
    protected void onPause() {
        super.onPause();
        poll.removeCallbacks(poller);
    }

    private void buildStations() {
        for (Radio.Station s : Radio.STATIONS) {
            LinearLayout row = Ui.card(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(this, 18), Ui.dp(this, 12),
                    Ui.dp(this, 18), Ui.dp(this, 12));
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            Ui.margin(row, 0, 0, 0, Ui.dp(this, 8));

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            TextView name = Ui.label(this, Radio.name(s), 18, Ui.TEXT);
            name.setTypeface(name.getTypeface(), android.graphics.Typeface.BOLD);
            TextView where = Ui.label(this, Radio.where(s), 13, Ui.DIM);
            col.addView(name);
            col.addView(where);
            row.addView(col, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            row.addView(Ui.label(this, "▶", 18, Ui.GOLD));
            row.setTag(s);

            Ui.strongFocus(row, Ui.roundBg(Ui.CARD, Ui.dp(this, 16)),
                    () -> {}, name, where);
            row.setOnClickListener(v -> play((Radio.Station) v.getTag()));
            list.addView(row);
        }
    }

    private void play(Radio.Station s) {
        Intent i = new Intent(this, RadioPlayer.class).setAction(RadioPlayer.ACTION_PLAY)
                .putExtra(RadioPlayer.EXTRA_ID, s.id);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
            status.setText(Lang.t("Connecting…"));
        } catch (Exception e) {
            status.setText(Lang.t("Could not play this station"));
        }
    }

    private void stopRadio() {
        try {
            startService(new Intent(this, RadioPlayer.class)
                    .setAction(RadioPlayer.ACTION_STOP));
        } catch (Exception ignored) {}
        status.setText(Lang.t("Tap a station to listen"));
    }

    private void refresh() {
        if (status == null) return;
        String id = Radio.playingId;
        Radio.Station st = id == null || id.isEmpty() ? null : Radio.byId(id);
        switch (Radio.state) {
            case Radio.CONNECTING:
                status.setText(Lang.t("Connecting…") + (st == null ? "" : " · " + Radio.name(st)));
                break;
            case Radio.PLAYING:
                status.setText(Lang.t("Playing: " + (st == null ? "" : Radio.name(st))));
                break;
            case Radio.ERROR:
                status.setText(Lang.t("Could not play this station"));
                break;
            default:
                status.setText(Lang.t("Tap a station to listen"));
                break;
        }
        for (int i = 0; i < list.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) list.getChildAt(i);
            if (row.isFocused()) continue;
            Radio.Station s = (Radio.Station) row.getTag();
            boolean on = Radio.state == Radio.PLAYING && s.id.equals(id);
            row.setBackground(Ui.roundBgStroke(on ? Ui.CARD_HI : Ui.CARD,
                    on ? Ui.GOLD : (Ui.night() ? 0xFF2A2317 : 0xFF24382E),
                    on ? Ui.dp(this, 2) : Ui.dp(this, 1), Ui.dp(this, 16)));
        }
    }
}
