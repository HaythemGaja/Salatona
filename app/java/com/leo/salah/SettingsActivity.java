package com.leo.salah;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.ZoneId;
import java.util.Locale;
import java.util.function.IntSupplier;

/**
 * Two-pane Salatona settings.
 *
 * <p>The start side is a rail of six sections; the end side holds the rows for
 * the open section. Up/Down move through the rail, OK (or Right) opens a
 * section, Back returns to the rail. Inside a section every row is a single
 * focusable: Left/Right change a value in place, so focus is never lost.
 */
public class SettingsActivity extends BaseActivity {

    private static final int REQ_AUDIO = 101;
    private static final int REQ_NOTIFICATIONS = 102;

    private static final int CAT_PRAYER = 0, CAT_ATHAN = 1, CAT_TOOLS = 2,
            CAT_DISPLAY = 3, CAT_SYSTEM = 4, CAT_ABOUT = 5;

    private static final String[] CAT_LABELS = {
            "Prayer times", "Athan", "Tools", "Display", "System", "About"};
    private static final String[] CAT_ICONS = {"🕌", "🔊", "🧰", "📺", "🛠", "ℹ"};
    private static final String[] CAT_SUBS = {
            "Location, calculation and daylight saving",
            "Voice, volume and prayer alerts",
            "Qibla, duas and the Hijri calendar",
            "Screen saver and ambient clock",
            "Permissions and alarm reliability",
            "Version, time zone and privacy"};

    private Prefs p;
    private int category = CAT_PRAYER;
    private boolean onRail = true;
    private int voiceBeforePicker;

    private final LinearLayout[] navRows = new LinearLayout[CAT_LABELS.length];
    private final TextView[] navIcons = new TextView[CAT_LABELS.length];
    private final TextView[] navNames = new TextView[CAT_LABELS.length];

    private TextView sectionTitle, sectionSub;
    private LinearLayout list;
    private View firstFocusable;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = new Prefs(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(buildRail(), new LinearLayout.LayoutParams(
                Ui.dp(this, 300), ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(buildContent(), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        setContentView(root);

        rebuildContent();
        paintNavs();
        navRows[category].post(() -> navRows[category].requestFocus());
    }

    @Override
    protected void onResume() {
        super.onResume();
        p = new Prefs(this);
        rebuildContent();
        paintNavs();
        navRows[category].post(() -> {
            if (onRail) navRows[category].requestFocus();
            else if (firstFocusable != null) firstFocusable.requestFocus();
        });
    }

    @Override
    public void onBackPressed() {
        if (!onRail) {
            onRail = true;
            navRows[category].requestFocus();
            return;
        }
        super.onBackPressed();
    }

    // ---------------------------------------------------------------- layout

    private LinearLayout buildRail() {
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setBackgroundColor(Ui.BG_DEEP);
        rail.setPadding(Ui.dp(this, 18), Ui.dp(this, 22),
                Ui.dp(this, 18), Ui.dp(this, 16));

        LinearLayout head = Ui.hrow(this);
        TextView gear = Ui.label(this, "⚙", 24, Ui.GOLD);
        head.addView(gear);
        TextView title = Ui.label(this, "Settings", 25, Ui.TEXT);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        Ui.margin(title, Ui.dp(this, 8), 0, 0, 0);
        head.addView(title);
        rail.addView(head);

        TextView version = Ui.label(this, "Salatona " + versionName(), 12, Ui.DIM);
        Ui.margin(version, Ui.dp(this, 2), Ui.dp(this, 2), 0, 0);
        rail.addView(version);

        View gap = new View(this);
        rail.addView(gap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 14)));

        for (int i = 0; i < CAT_LABELS.length; i++) rail.addView(buildNav(i));

        View spacer = new View(this);
        rail.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView hint = Ui.label(this, "↑ ↓  choose section\nOK  open  ·  Back  return", 12, Ui.DIM);
        hint.setLineSpacing(Ui.dp(this, 2), 1f);
        rail.addView(hint);
        return rail;
    }

    private LinearLayout buildNav(int i) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setFocusable(true);
        row.setPadding(Ui.dp(this, 14), Ui.dp(this, 12),
                Ui.dp(this, 12), Ui.dp(this, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52));
        lp.setMargins(0, 0, 0, Ui.dp(this, 6));
        row.setLayoutParams(lp);

        TextView icon = Ui.label(this, CAT_ICONS[i], 20, Ui.DIM);
        icon.setGravity(Gravity.CENTER);
        icon.setMinWidth(Ui.dp(this, 30));
        row.addView(icon);

        TextView name = Ui.label(this, CAT_LABELS[i], 18, Ui.TEXT);
        Ui.margin(name, Ui.dp(this, 10), 0, 0, 0);
        row.addView(name);

        navRows[i] = row;
        navIcons[i] = icon;
        navNames[i] = name;

        row.setOnClickListener(v -> openCategory(i));
        row.setOnKeyListener((v, keyCode, e) -> {
            if (e.getAction() == KeyEvent.ACTION_DOWN
                    && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                openCategory(i);
                return true;
            }
            return false;
        });
        row.setOnFocusChangeListener((v, has) -> {
            if (has) {
                onRail = true;
                v.setScaleX(1.03f);
                v.setScaleY(1.03f);
                v.setElevation(Ui.dp(this, 6));
                v.setBackground(Ui.roundBgStroke(Ui.GOLD_BRIGHT, 0xFFFFFFFF,
                        Ui.dp(this, 3), Ui.dp(this, 14)));
                icon.setTextColor(Ui.DARK_TEXT);
                name.setTextColor(Ui.DARK_TEXT);
            } else {
                v.setScaleX(1f);
                v.setScaleY(1f);
                v.setElevation(0);
                paintNav(i);
            }
        });
        return row;
    }

    private LinearLayout buildContent() {
        LinearLayout pane = new LinearLayout(this);
        pane.setOrientation(LinearLayout.VERTICAL);
        pane.setPadding(Ui.dp(this, 28), Ui.dp(this, 22),
                Ui.dp(this, 30), Ui.dp(this, 14));

        sectionTitle = Ui.label(this, "", 26, Ui.TEXT);
        sectionTitle.setTypeface(sectionTitle.getTypeface(), Typeface.BOLD);
        pane.addView(sectionTitle);

        sectionSub = Ui.label(this, "", 14, Ui.DIM);
        Ui.margin(sectionSub, 0, Ui.dp(this, 3), 0, 0);
        pane.addView(sectionSub);

        View rule = new View(this);
        rule.setBackgroundColor(Ui.GOLD);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                Ui.dp(this, 52), Ui.dp(this, 2));
        rlp.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        pane.addView(rule, rlp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFocusable(false);
        scroll.setVerticalScrollBarEnabled(false);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 0, 0, Ui.dp(this, 24));
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pane.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView hint = Ui.label(this,
                "‹ › change  ·  OK select  ·  Back to sections", 12, Ui.DIM);
        hint.setGravity(Gravity.END);
        pane.addView(hint);
        return pane;
    }

    // ---------------------------------------------------------------- sections

    private void openCategory(int i) {
        boolean changed = i != category;
        category = i;
        if (changed) rebuildContent();
        paintNavs();
        if (firstFocusable != null) {
            onRail = false;
            firstFocusable.post(() -> firstFocusable.requestFocus());
        } else {
            onRail = true;
            navRows[i].requestFocus();
        }
    }

    private void paintNavs() {
        for (int i = 0; i < navRows.length; i++) paintNav(i);
    }

    private void paintNav(int i) {
        boolean selected = i == category;
        navRows[i].setBackground(selected
                ? Ui.roundBgStroke(Ui.CARD, Ui.GOLD, Ui.dp(this, 2), Ui.dp(this, 14))
                : Ui.roundBg(0x00000000, Ui.dp(this, 14)));
        navIcons[i].setTextColor(selected ? Ui.GOLD_BRIGHT : Ui.DIM);
        navNames[i].setTextColor(selected ? Ui.GOLD_BRIGHT : 0xFFE0E0E0);
    }

    private void rebuildContent() {
        p = new Prefs(this);
        list.removeAllViews();
        firstFocusable = null;
        sectionTitle.setText(CAT_ICONS[category] + "  " + Lang.t(CAT_LABELS[category]));
        sectionSub.setText(Lang.t(CAT_SUBS[category]));

        switch (category) {
            case CAT_PRAYER: buildPrayer(); break;
            case CAT_ATHAN: buildAthan(); break;
            case CAT_TOOLS: buildTools(); break;
            case CAT_DISPLAY: buildDisplay(); break;
            case CAT_SYSTEM: buildSystem(); break;
            default: buildAbout(); break;
        }
        Lang.localize(sectionTitle);
        Lang.localize(sectionSub);
        Lang.localize(list);
    }

    private void buildPrayer() {
        sub("LOCATION");
        navRow("📍 City", Lang.city(p.cityName(), p.cityCountry()),
                v -> startActivity(new Intent(this, CityPickerActivity.class)));

        sub("CALCULATION");
        cycleRow("Calculation method", PrayerTimes.METHOD_NAMES,
                p::method, v -> { p.setMethod(v); reschedule(); });
        cycleRow("Asr time", new String[]{"Shafi'i · Maliki · Hanbali", "Hanafi"},
                p::asr, v -> { p.setAsr(v); reschedule(); });
        cycleRow("Hijri date",
                new String[]{"−2 days", "−1 day", "No adjustment", "+1 day", "+2 days"},
                () -> p.hijriOffset() + 2,
                v -> { p.setHijriOffset(v - 2); reschedule(); });
        note("Umm al-Qura calendar; move it a day when your local moon sighting differs.");

        final TextView dstNote = noteView("");
        cycleRow("Daylight saving", Daylight.MODE_NAMES, p::dstMode, v -> {
            p.setDstMode(v);
            dstNote.setText(Lang.t(Daylight.status(this)));
            reschedule();
        });
        list.addView(dstNote);
        dstNote.setText(Lang.t(Daylight.status(this)));
        list.addView(noteView("Auto follows the TV's zone and changes at the official "
                + "transition. On/Off shift every prayer by one hour when the TV clock "
                + "and your country disagree."));

        navRow("Fine-tune prayer times", "± minutes per prayer",
                v -> startActivity(new Intent(this, AdjustActivity.class)));

        sub("CALENDAR");
        navRow("📆 Monthly timetable", "full month of prayer times",
                v -> startActivity(new Intent(this, TimetableActivity.class)));
    }

    private void buildAthan() {
        sub("VOICE & VOLUME");
        cycleRow("Athan voice", Prefs.VOICE_NAMES, p::voice, v -> {
            if (v == 4) {
                voiceBeforePicker = p.voice();
                pickAudioFile();
            } else {
                p.setVoice(v);
            }
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

        sub("PRAYERS");
        for (int i = 0; i < 5; i++) {
            final int pi = i;
            switchRow("Athan for " + Lang.prayer(i), () -> p.athanOn(pi),
                    on -> { p.setAthanOn(pi, on); reschedule(); });
            cycleRow("🎙 " + Lang.prayer(i) + " voice", Prefs.PRAYER_VOICE_OPTS,
                    () -> Prefs.prayerVoiceOpt(p.prayerVoice(pi)),
                    v -> p.setPrayerVoice(pi, Prefs.prayerVoiceStored(v)));
        }

        sub("ALERTS");
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
    }

    private void buildTools() {
        sub("DIRECTION");
        navRow("🧭 Qibla compass", "bearing from your city",
                v -> startActivity(new Intent(this, QiblaActivity.class)));
        cycleRow("TV faces",
                new String[]{"Not set", "North", "North-East", "East", "South-East",
                        "South", "South-West", "West", "North-West"},
                () -> p.facing() + 1, v -> p.setFacing(v - 1));
        note("Used for the room-relative Qibla line: tell Salatona which way the screen faces.");

        sub("QURAN");
        navRow("📻 Quran radio", "live stations and reciters",
                v -> startActivity(new Intent(this, RadioActivity.class)));

        sub("SUPPLICATIONS");
        navRow("🤲 Duas", "after prayer · morning · night",
                v -> startActivity(new Intent(this, DuasActivity.class)));
        sub("HIJRI CALENDAR");
        navRow("📅 Islamic events", "upcoming & past, countdown in days",
                v -> startActivity(new Intent(this, EventsActivity.class)));
    }

    private void buildDisplay() {
        sub("LANGUAGE & NUMBERS");
        cycleRow("Language", new String[]{"Follow TV", "Arabic", "English"},
                p::language, v -> { p.setLanguage(v); recreate(); });
        cycleRow("Numbers", new String[]{"Follow language", "Arabic-Indic", "Western"},
                p::digits, v -> { p.setDigits(v); recreate(); });
        cycleRow("Clock format", new String[]{"Follow TV", "12-hour", "24-hour"},
                p::clockMode, v -> { p.setClockMode(v); recreate(); });
        switchRow("Night colours", p::night, v -> { p.setNight(v); recreate(); });

        sub("AMBIENT");
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
        String ss = null;
        try {
            ss = Settings.Secure.getString(getContentResolver(), "screensaver_components");
        } catch (Exception ignored) {}
        note("System screen saver: " + (ss != null && ss.contains(getPackageName())
                ? "Salatona ✓" : "another app"));

        sub("GOOGLE TV");
        note("Google TV hides third-party screen savers from Settings → System → "
                + "Ambient mode. “Ambient display” above keeps the prayer clock on until any "
                + "button is pressed. To make it automatic on idle, set it once via ADB:\n"
                + "settings put secure screensaver_components com.leo.salah/.SalahDream");
    }

    private void buildSystem() {
        sub("RELIABILITY");
        boolean ignoring = false;
        try {
            android.os.PowerManager pm =
                    (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
            ignoring = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        } catch (Exception ignored) {}
        stateRow("🔋 Battery", ignoring, "✓ Unrestricted", "✗ Restricted ›",
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
        actionRow("⏰ Reschedule athan alarms", v -> {
            try {
                AthanScheduler.scheduleAll(this);
                Toast.makeText(this, "Athan alarms rescheduled ✓", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not reschedule", Toast.LENGTH_SHORT).show();
            }
        });
        note("Alarms scheduled for " + (p.scheduledDay().isEmpty() ? "—" : p.scheduledDay())
                + ". Salatona refreshes them in the background every day.");

        sub("PERMISSIONS");
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
        if (Build.VERSION.SDK_INT >= 33) {
            permRow("Notifications", notificationsGranted(),
                    v -> requestPermissions(
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            REQ_NOTIFICATIONS));
        }
        note("The athan overlay needs “Display over other apps”. Exact alarms and "
                + "notifications keep it on time on Android 12 and newer TVs.");
    }

    private void buildAbout() {
        sub("SALATONA");
        list.addView(noteView(Lang.t("Version " + versionName()) + "\n"
                + Lang.t("On-device prayer times with a full-day timeline, athan overlay, "
                + "timetable, Qibla, duas and a Ramadan card.") + "\n"
                + Lang.t("No accounts, no ads, no analytics.")));

        sub("TIME ZONE");
        list.addView(noteView(Lang.t("TV zone: " + ZoneId.systemDefault().getId()) + "\n"
                + Lang.t(Daylight.status(this))));

        sub("PRIVACY");
        list.addView(noteView(Lang.t("Your city and settings stay on this TV. Weather is the "
                + "only network request; prayer times never need the internet.")));

        actionRow("Close settings", v -> finish());
    }

    // ---------------------------------------------------------------- rows

    private void sub(String s) {
        TextView t = Ui.label(this, s.toUpperCase(Locale.US), 12, Ui.GOLD);
        t.setLetterSpacing(0.1f);
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        Ui.margin(t, Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 6));
        list.addView(t);
    }

    private TextView noteView(String s) {
        TextView t = Ui.label(this, s, 13, Ui.DIM);
        t.setLineSpacing(Ui.dp(this, 2), 1f);
        Ui.margin(t, Ui.dp(this, 4), Ui.dp(this, 2), Ui.dp(this, 10), Ui.dp(this, 10));
        return t;
    }

    private void note(String s) { list.addView(noteView(s)); }

    private Drawable rowBg() {
        return Ui.roundBg(Ui.CARD, Ui.dp(this, 16));
    }

    /** Base row: exactly one focusable per row, gold highlight, tracked for initial focus. */
    private LinearLayout baseRow(TextView... texts) {
        LinearLayout row = Ui.card(this);
        row.setPadding(Ui.dp(this, 18), Ui.dp(this, 13),
                Ui.dp(this, 18), Ui.dp(this, 13));
        row.setBackground(rowBg());
        // width must be set before Ui.margin(), which otherwise defaults to WRAP_CONTENT
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        Ui.margin(row, 0, 0, 0, Ui.dp(this, 8));
        Ui.strongFocus(row, rowBg(), () -> onRail = false, texts);
        if (firstFocusable == null) firstFocusable = row;
        list.addView(row);
        return row;
    }

    private TextView rowLabel(String s) {
        TextView t = Ui.label(this, s, 18, Ui.TEXT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        t.setLayoutParams(lp);
        return t;
    }

    /** Right-side value pill, gold, with ‹ › affordance for adjustable rows. */
    private TextView rowValue() {
        TextView v = Ui.label(this, "", 16, Ui.GOLD_BRIGHT);
        v.setGravity(Gravity.END);
        v.setMinWidth(Ui.dp(this, 96));
        v.setMaxLines(2);
        return v;
    }

    private void navRow(String label, String value, View.OnClickListener onClick) {
        LinearLayout inner = Ui.hrow(this);
        TextView lab = rowLabel(label);
        inner.addView(lab);
        TextView v = rowValue();
        v.setTextColor(Ui.DIM);
        v.setTextSize(14);
        v.setText(Lang.t(value) + "  ›");
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

        Runnable paint = () -> v.setText(Lang.t("‹  " + options[cur.getAsInt()] + "  ›"));
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

        Runnable paint = () -> v.setText(Lang.num("−  " + fmt.apply(cur.getAsInt()) + "  +"));
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
            v.setText(Lang.t(on ? "● ON" : "○ OFF"));
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
        stateRow(label, granted, "✓ Granted", "✗ Grant ›", onFix);
    }

    private void stateRow(String label, boolean ok, String okText, String badText,
                          View.OnClickListener onFix) {
        LinearLayout inner = Ui.hrow(this);
        TextView lab = rowLabel(label);
        inner.addView(lab);
        TextView v = rowValue();
        v.setTextSize(14);
        v.setText(Lang.t(ok ? okText : badText));
        v.setTextColor(ok ? Ui.GREEN : Ui.GOLD_BRIGHT);
        inner.addView(v);
        LinearLayout row = baseRow(lab, v);
        row.addView(inner);
        row.setOnClickListener(vv -> {
            if (!ok) onFix.onClick(vv);
        });
    }

    // ---------------------------------------------------------------- actions

    private int idxOf(int[] arr, int val) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == val) return i;
        return 0;
    }

    private void reschedule() {
        try { AthanScheduler.scheduleAll(this); } catch (Exception ignored) {}
    }

    private boolean notificationsGranted() {
        if (Build.VERSION.SDK_INT < 33) return true;
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "2.0";
        }
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
            rebuildContent();
            if (firstFocusable != null) firstFocusable.post(firstFocusable::requestFocus);
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
                    p.setVoice(4);
                    Toast.makeText(this, "Custom athan set ✓", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    p.setVoice(voiceBeforePicker);
                    Toast.makeText(this, "Could not use that file", Toast.LENGTH_SHORT).show();
                }
            } else {
                p.setVoice(voiceBeforePicker);
            }
            rebuildContent();
            if (firstFocusable != null) firstFocusable.post(firstFocusable::requestFocus);
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(req, permissions, results);
        if (req == REQ_NOTIFICATIONS) {
            boolean ok = results.length > 0
                    && results[0] == PackageManager.PERMISSION_GRANTED;
            Toast.makeText(this, ok ? "Notifications allowed ✓"
                    : "Notifications stay blocked", Toast.LENGTH_SHORT).show();
            rebuildContent();
            if (firstFocusable != null) firstFocusable.post(firstFocusable::requestFocus);
        }
    }
}
