package com.leo.salah;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

/**
 * Main dashboard screen. The ⚙ gear (top-left) is the only control —
 * everything else (city, qibla, duas, all settings) lives behind it.
 */
public class MainActivity extends Activity {

    private DashboardView dashboard;
    private SkyView sky;
    private LinearLayout warnBox;
    private Button gear;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            try { dashboard.tick(); } catch (Exception ignored) {}
            try {
                if (sky != null && dashboard != null) {
                    sky.setPhase(dashboard.phasePrayer(), dashboard.phaseProgress());
                    sky.setWeatherKind(dashboard.weatherKind());
                }
            } catch (Exception ignored) {}
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        FrameLayout frame = new FrameLayout(this);
        sky = new SkyView(this);
        frame.addView(sky,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(this, 36);
        root.setPadding(pad, Ui.dp(this, 20), pad, Ui.dp(this, 16));
        frame.addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        warnBox = new LinearLayout(this);
        warnBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(warnBox);

        dashboard = new DashboardView(this);
        root.addView(dashboard, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // ⚙ gear shortcut, top-left corner — the only control on this screen
        gear = Ui.iconButton(this, "⚙", 26);
        gear.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        FrameLayout.LayoutParams glp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        int gm = Ui.dp(this, 24);
        glp.setMargins(gm, gm, 0, 0);
        frame.addView(gear, glp);

        setContentView(frame);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            String today = PrayerCalc.dayKey(Calendar.getInstance());
            if (!today.equals(new Prefs(this).scheduledDay()))
                AthanScheduler.scheduleAll(this);
        } catch (Exception ignored) {}
        dashboard.refreshDay();
        buildWarnings();
        handler.post(ticker);
        // the gear is the only focusable control — keep it focused so it's always obvious
        handler.postDelayed(() -> {
            if (gear != null) gear.requestFocus();
        }, 300);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    private void buildWarnings() {
        warnBox.removeAllViews();
        android.view.View w = null;
        if (!Settings.canDrawOverlays(this)) {
            w = warnCard("⚠ Allow “Display over other apps” so the athan appears over other apps.",
                    v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()))));
        } else if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            boolean can = false;
            try { can = am != null && am.canScheduleExactAlarms(); } catch (Exception ignored) {}
            if (!can) {
                w = warnCard("⚠ Allow exact alarms so the athan rings at the exact minute.",
                        v -> {
                            try {
                                startActivity(new Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:" + getPackageName())));
                            } catch (Exception ignored) {}
                        });
            }
        }
        if (w != null) {
            Ui.margin(w, 0, 0, 0, Ui.dp(this, 10));
            warnBox.addView(w);
        }
    }

    private android.view.View warnCard(String text, android.view.View.OnClickListener onFix) {
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.HORIZONTAL);
        w.setGravity(Gravity.CENTER_VERTICAL);
        w.setBackground(Ui.roundBg(0xCC3D2B12, Ui.dp(this, 14)));
        int p = Ui.dp(this, 12);
        w.setPadding(Ui.dp(this, 20), p, Ui.dp(this, 20), p);
        TextView t = Ui.label(this, text, 17, 0xFFFFE082);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        w.addView(t, lp);
        Button fix = Ui.bigButton(this, "Fix");
        fix.setMinHeight(Ui.dp(this, 48));
        fix.setOnClickListener(onFix);
        w.addView(fix);
        return w;
    }
}
