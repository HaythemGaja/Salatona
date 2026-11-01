package com.leo.salah;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.function.IntSupplier;

/**
 * Settings with one-dimensional navigation:
 * EVERY row is a single focusable. Up/Down moves between rows,
 * OK selects, Left/Right changes a value. No nested buttons, and
 * changing a value never rebuilds the list, so focus is never lost.
 */
public class SettingsActivity extends Activity {

    private static final int REQ_AUDIO = 101;
    private LinearLayout list;
    private Prefs p;
    private int voiceBeforePicker = 0;
    private View firstFocusable;

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

        TextView title = Ui.label(this, "⚙ Settings", 34, Ui.TEXT);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView help = Ui.label(this,
                "Up / Down: move between rows · OK: select · Left / Right: change a value · Back: exit",
                16, Ui.DIM);
        Ui.margin(help, 0, Ui.dp(this, 8), 0, 0);
        root.addView(help);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        Ui.margin(list, 0, Ui.dp(this, 16), 0, 0);
        root.addView(list);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuild();
        if (firstFocusable != null) {
            firstFocusable.post(() -> firstFocusable.requestFocus());
        }
    }

    private void rebuild() {
        list.removeAllViews();
        firstFocusable = null;
        p = new Prefs(this);

        section("Tools");
        navRow("📍 City", p.cityName() + ", " + p.cityCountry(),
                v -> startActivity(new Intent(this, CityPickerActivity.class)));
        navRow("🧭 Qibla compass", "find prayer direction",
                v -> startActivity(new Intent(this, QiblaActivity.class)));
        navRow("🤲 Duas", "after prayer · morning · night",
                v -> startActivity(new Intent(this, DuasActivity.class)));
        navRow("📅 Islamic events", "upcoming & past, countdown in days",
                v -> startActivity(new Intent(this, EventsActivity.class)));
        navRow("📆 Monthly timetable", "full month of prayer times",
                v -> startActivity(new Intent(this, TimetableActivity.class)));

        section("Background & alarms");
        navRow("🔋 Keep alarms alive in background", "set Salatona to Unrestricted",
                v -> {
                    try {
                        startActivity(new Intent(
                                Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    } catch (Exception e) {
                        try {
                            startActivity(new Intent(Settings.ACTION_SETTINGS));
                        } catch (Exception ignored) {}
                    }
                });

        section("Location & calculation");
        cycleRow("Calculation method", PrayerTimes.METHOD_NAMES,
                p::method, v -> { p.setMethod(v); reschedule(); });
        cycleRow("Asr time", new String[]{"Shafi'i · Maliki · Hanbali", "Hanafi"},
                p::asr, v -> { p.setAsr(v); reschedule(); });
        navRow("Fine-tune prayer times", "± minutes per prayer",
                v -> startActivity(new Intent(this, AdjustActivity.class)));

        section("Athan");
        cycleRow("Athan voice", Prefs.VOICE_NAMES, p::voice, v -> {
            if (v == 4) { voiceBeforePicker = p.voice(); pickAudioFile(); }
            else p.setVoice(v);
        });
        actionRow("🔊 Test athan", v -> {
            Intent svc = new Intent(this, AthanOverlayService.class);
            svc.putExtra("prayer", 3);
            svc.putExtra("test", true);
            try {
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
                else startService(svc);
                Toast.makeText(this, "Playing test athan…", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not start test", Toast.LENGTH_SHORT).show();
            }
        });
        adjustRow("Athan volume", 0, 100, 5, p::athanVol,
                v -> v + "%", v -> p.setAthanVol(v));
        switchRow("🔊 Athan fade-in", p::fadeIn, p::setFadeIn);
        for (int i = 0; i < 5; i++) {
            final int pi = i;
            switchRow("Athan for " + PrayerCalc.NAMES[i], () -> p.athanOn(pi),
                    on -> { p.setAthanOn(pi, on); reschedule(); });
        }
        for (int i = 0; i < 5; i++) {
            final int pi = i;
            cycleRow("🎙 " + PrayerCalc.NAMES[i] + " voice", Prefs.PRAYER_VOICE_OPTS,
                    () -> Prefs.prayerVoiceOpt(p.prayerVoice(pi)),
                    v -> p.setPrayerVoice(pi, Prefs.prayerVoiceStored(v)));
        }

        section("Behavior");
        cycleRow("Remind before athan",
                new String[]{"Off", "5 min before", "10 min before", "15 min before", "30 min before"},
                () -> idxOf(new int[]{0, 5, 10, 15, 30}, p.preMin()),
                v -> { p.setPreMin(new int[]{0, 5, 10, 15, 30}[v]); reschedule(); });
        cycleRow("Quiet TV after athan",
                new String[]{"Off", "10 min", "15 min", "20 min", "30 min"},
                () -> idxOf(new int[]{0, 10, 15, 20, 30}, p.quietMin()),
                v -> p.setQuietMin(new int[]{0, 10, 15, 20, 30}[v]));
        cycleRow("Athan popup style", new String[]{"Full screen", "Top banner"},
                p::overlayStyle, v -> p.setOverlayStyle(v));
        switchRow("🌙 Taraweeh reminder (in Ramadan)", p::taraweeh,
                v -> { p.setTaraweeh(v); reschedule(); });

        section("Screen saver");
        navRow("▶ Ambient display", "prayer clock that keeps the screen on",
                v -> startActivity(new Intent(this, AmbientActivity.class)));
        navRow("Set Salatona as screen saver", "system screen-saver settings",
                v -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
                    } catch (Exception e) {
                        Toast.makeText(this,
                                "Open Settings → Device Preferences → Screen saver",
                                Toast.LENGTH_LONG).show();
                    }
                });
        TextView dreamNote = Ui.label(this,
                "Google TV (Android 12+) hides third-party screensavers from Settings → "
                        + "System → Ambient mode — “Ambient display” above is the easy way: "
                        + "open it and the prayer clock stays on until you press any button. "
                        + "To make it fully automatic on idle, set it once via ADB: "
                        + "settings put secure screensaver_components com.leo.salah/.SalahDream",
                16, Ui.DIM);
        Ui.margin(dreamNote, Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
        list.addView(dreamNote);

        section("Permissions");
        permRow("Display over other apps", Settings.canDrawOverlays(this),
                v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()))));
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            boolean can = false;
            try { can = am != null && am.canScheduleExactAlarms(); } catch (Exception ignored) {}
            final boolean canF = can;
            permRow("Exact alarms", canF, v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName())));
                } catch (Exception ignored) {}
            });
        }

        section("About");
        TextView about = Ui.label(this,
                "Salatona 1.26\nPrayer times are computed on-device — no internet needed.\n"
                        + "Athan voices play over any app via a dismissible overlay.\n"
                        + "Set it as your TV screen saver for an always-on prayer display.",
                16, Ui.DIM);
        Ui.margin(about, Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 32));
        list.addView(about);
    }

    private int idxOf(int[] arr, int val) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == val) return i;
        return 0;
    }

    private void reschedule() {
        try { AthanScheduler.scheduleAll(this); } catch (Exception ignored) {}
    }

    private void pickAudioFile() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("audio/*");
            startActivityForResult(i, REQ_AUDIO);
        } catch (Exception e) {
            p.setVoice(voiceBeforePicker);
            Toast.makeText(this, "No file picker on this device", Toast.LENGTH_SHORT).show();
            rebuild();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_AUDIO) {
            if (res == RESULT_OK && data != null && data.getData() != null) {
                try {
                    Uri uri = data.getData();
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    p.setCustomUri(uri.toString());
                    p.setVoice(3);
                    Toast.makeText(this, "Custom athan set ✓", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    p.setVoice(voiceBeforePicker);
                    Toast.makeText(this, "Could not use that file", Toast.LENGTH_SHORT).show();
                }
            } else {
                p.setVoice(voiceBeforePicker);
            }
            rebuild();
        }
    }

    // ---------- row builders: one focusable per row, values update in place ----------

    private void section(String s) {
        TextView t = Ui.label(this, s, 20, Ui.GOLD);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        Ui.margin(t, Ui.dp(this, 4), Ui.dp(this, 20), Ui.dp(this, 4), Ui.dp(this, 4));
        list.addView(t);
    }

    private Drawable rowBg() {
        return Ui.roundBg(Ui.CARD, Ui.dp(this, 18));
    }

    /** Base row: exactly one focusable per row, gold highlight, tracked for initial focus. */
    private LinearLayout baseRow(TextView... texts) {
        LinearLayout row = Ui.card(this);
        row.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 14));
        Ui.margin(row, 0, 0, 0, Ui.dp(this, 8));
        Ui.strongFocus(row, rowBg(), texts);
        if (firstFocusable == null) firstFocusable = row;
        list.addView(row);
        return row;
    }

    private TextView rowLabel(String s) {
        TextView t = Ui.label(this, s, 20, Ui.TEXT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        t.setLayoutParams(lp);
        return t;
    }

    /** Right-side value pill, gold, with ‹ › affordance for adjustable rows. */
    private TextView rowValue() {
        TextView v = Ui.label(this, "", 18, Ui.GOLD_BRIGHT);
        v.setGravity(Gravity.END);
        v.setMinWidth(Ui.dp(this, 120));
        return v;
    }

    private void navRow(String label, String value, View.OnClickListener onClick) {
        LinearLayout inner = Ui.hrow(this);
        TextView lab = rowLabel(label);
        inner.addView(lab);
        TextView v = rowValue();
        v.setTextColor(Ui.DIM);
        v.setText(value + "  ›");
        inner.addView(v);
        LinearLayout row = baseRow(lab, v);
        row.addView(inner);
        row.setOnClickListener(onClick);
    }

    private void actionRow(String label, View.OnClickListener onClick) {
        LinearLayout inner = Ui.hrow(this);
        TextView lab = rowLabel(label);
        inner.addView(lab);
        TextView v = rowValue();
        v.setTextColor(Ui.DIM);
        v.setText("›");
        inner.addView(v);
        LinearLayout row = baseRow(lab, v);
        row.addView(inner);
        row.setOnClickListener(onClick);
    }

    private interface IntSetter { void set(int v); }
    private interface BoolSetter { void set(boolean v); }

    /** OK or Right = next option, Left = previous. Updates in place — focus never lost. */
    private void cycleRow(String label, String[] options, IntSupplier cur, IntSetter setter) {
        LinearLayout inner = Ui.hrow(this);
        TextView lab = rowLabel(label);
        inner.addView(lab);
        TextView v = rowValue();
        inner.addView(v);
        LinearLayout row = baseRow(lab, v);
        row.addView(inner);

        Runnable paint = () -> v.setText("‹  " + options[cur.getAsInt()] + "  ›");
        paint.run();
        row.setOnClickListener(vv -> {
            setter.set((cur.getAsInt() + 1) % options.length);
            paint.run();
        });
        row.setOnKeyListener((vv, keyCode, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                setter.set((cur.getAsInt() - 1 + options.length) % options.length);
                paint.run();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                setter.set((cur.getAsInt() + 1) % options.length);
                paint.run();
                return true;
            }
            return false;
        });
    }

    /** Left = decrease, Right / OK = increase. Updates in place. */
    private void adjustRow(String label, int min, int max, int step, IntSupplier cur,
                           java.util.function.Function<Integer, String> fmt, IntSetter setter) {
        LinearLayout inner = Ui.hrow(this);
        TextView lab = rowLabel(label);
        inner.addView(lab);
        TextView v = rowValue();
        inner.addView(v);
        LinearLayout row = baseRow(lab, v);
        row.addView(inner);

        Runnable paint = () -> v.setText("−  " + fmt.apply(cur.getAsInt()) + "  +");
        paint.run();
        row.setOnClickListener(vv -> {
            setter.set(Math.min(max, cur.getAsInt() + step));
            paint.run();
        });
        row.setOnKeyListener((vv, keyCode, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                setter.set(Math.max(min, cur.getAsInt() - step));
                paint.run();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                setter.set(Math.min(max, cur.getAsInt() + step));
                paint.run();
                return true;
            }
            return false;
        });
    }

    /** OK / Left / Right toggles. Updates in place. */
    private void switchRow(String label, java.util.function.BooleanSupplier cur, BoolSetter setter) {
        LinearLayout inner = Ui.hrow(this);
        TextView lab = rowLabel(label);
        inner.addView(lab);
        TextView v = rowValue();
        inner.addView(v);
        LinearLayout row = baseRow(lab, v);
        row.addView(inner);

        Runnable paint = () -> {
            boolean on = cur.getAsBoolean();
            v.setText(on ? "● ON" : "○ OFF");
        };
        paint.run();
        View.OnClickListener toggle = vv -> {
            setter.set(!cur.getAsBoolean());
            paint.run();
        };
        row.setOnClickListener(toggle);
        row.setOnKeyListener((vv, keyCode, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                toggle.onClick(vv);
                return true;
            }
            return false;
        });
    }

    private void permRow(String label, boolean granted, View.OnClickListener onFix) {
        LinearLayout inner = Ui.hrow(this);
        TextView lab = rowLabel(label);
        inner.addView(lab);
        TextView v = rowValue();
        v.setText(granted ? "✓ Granted" : "✗ Grant ›");
        v.setTextColor(granted ? Ui.GREEN : Ui.GOLD_BRIGHT);
        inner.addView(v);
        LinearLayout row = baseRow(lab, v);
        row.addView(inner);
        if (!granted) row.setOnClickListener(onFix);
    }
}
