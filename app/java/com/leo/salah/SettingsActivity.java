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
 * <p>Up/Down browse, OK opens a choice or toggles a switch, and Back goes up
 * one level. Horizontal navigation never changes a setting. Advanced prayer
 * options and per-prayer Athan controls have their own short detail pages.
 */
public class SettingsActivity extends BaseActivity {

    private static final int REQ_AUDIO = 101;
    private static final int REQ_NOTIFICATIONS = 102;

    private static final int CAT_PRAYER = 0, CAT_ATHAN = 1, CAT_RAMADAN = 2,
            CAT_DISPLAY = 3, CAT_LANGUAGE = 4, CAT_TOOLS = 5, CAT_SYSTEM = 6, CAT_ABOUT = 7;

    private static final String[] CAT_LABELS = {
            "Prayer times", "Athan & alerts", "Ramadan", "Screen saver & appearance", "Language & time", "Tools", "Permissions & reliability", "About"};
    private static final String[] CAT_ICONS = {"🕌", "🔊", "☾", "📺", "文", "🧰", "🛠", "ℹ"};
    private static final String[] CAT_SUBS = {
            "Location, calculation and daylight saving",
            "Voice, volume and prayer alerts",
            "Optional fasting details and Taraweeh reminder",
            "Screen saver and ambient clock",
            "Arabic, English, numbers and clock format",
            "Qibla, duas and the Hijri calendar",
            "Permissions and alarm reliability",
            "Version, time zone and privacy"};

    private Prefs p;
    private int category = CAT_PRAYER;
    private boolean onRail = true;
    private int voiceBeforePicker;
    private int detailPrayer = -1;
    private boolean advancedPrayer;
    private int parentRow;
    private int parentScroll;
    private final int[] categoryRows = new int[CAT_LABELS.length];
    private final int[] categoryScrolls = new int[CAT_LABELS.length];

    private final LinearLayout[] navRows = new LinearLayout[CAT_LABELS.length];
    private final TextView[] navIcons = new TextView[CAT_LABELS.length];
    private final TextView[] navNames = new TextView[CAT_LABELS.length];

    private TextView sectionTitle, sectionSub;
    private LinearLayout list;
    private ScrollView contentScroll;
    private int focusedRow;
    private int restoreScroll;
    private final java.util.ArrayList<View> contentRows = new java.util.ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = new Prefs(this);
        if (b != null) {
            category = Math.max(0, Math.min(CAT_LABELS.length - 1, b.getInt("category", 0)));
            onRail = b.getBoolean("onRail", true);
            focusedRow = b.getInt("focusedRow", 0);
            restoreScroll = b.getInt("contentScroll", 0);
            detailPrayer = Math.max(-1, Math.min(4, b.getInt("detailPrayer", -1)));
            advancedPrayer = b.getBoolean("advancedPrayer", false);
            parentRow = b.getInt("parentRow", 0);
            parentScroll = b.getInt("parentScroll", 0);
            int[] rows = b.getIntArray("categoryRows");
            int[] scrolls = b.getIntArray("categoryScrolls");
            if (rows != null) System.arraycopy(rows, 0, categoryRows, 0, Math.min(rows.length, categoryRows.length));
            if (scrolls != null) System.arraycopy(scrolls, 0, categoryScrolls, 0, Math.min(scrolls.length, categoryScrolls.length));
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(Ui.BG);
        ScrollView railScroll = new ScrollView(this);
        railScroll.setFillViewport(true);
        railScroll.setFocusable(false);
        railScroll.setVerticalScrollBarEnabled(false);
        railScroll.addView(buildRail());
        root.addView(railScroll, new LinearLayout.LayoutParams(
                Ui.dp(this, 250), ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(buildContent(), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        setContentView(root);

        rebuildContent();
        paintNavs();
        restoreFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        p = new Prefs(this);
        rebuildContent();
        paintNavs();
        restoreFocus();
    }

    @Override
    protected void onPause() {
        restoreScroll = contentScroll.getScrollY();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putInt("category", category);
        out.putBoolean("onRail", onRail);
        out.putInt("focusedRow", focusedRow);
        out.putInt("contentScroll", contentScroll.getScrollY());
        out.putInt("detailPrayer", detailPrayer);
        out.putBoolean("advancedPrayer", advancedPrayer);
        out.putInt("parentRow", parentRow);
        out.putInt("parentScroll", parentScroll);
        out.putIntArray("categoryRows", categoryRows);
        out.putIntArray("categoryScrolls", categoryScrolls);
        super.onSaveInstanceState(out);
    }

    private void restoreFocus() {
        final boolean rail = onRail;
        final int target = focusedRow;
        navRows[category].post(() -> {
            if (rail || contentRows.isEmpty()) navRows[category].requestFocus();
            else contentRows.get(Math.max(0, Math.min(target, contentRows.size() - 1))).requestFocus();
            if (restoreScroll > 0) {
                contentScroll.scrollTo(0, restoreScroll);
                restoreScroll = 0;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (returnFromDetails()) return;
        if (!onRail) {
            onRail = true;
            navRows[category].requestFocus();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int towardRail = Lang.rtl() ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
        if (event.getAction() == KeyEvent.ACTION_DOWN && !onRail && event.getKeyCode() == towardRail) {
            onRail = true;
            navRows[category].requestFocus();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void openDetails(int prayer, boolean advanced) {
        parentRow = focusedRow;
        parentScroll = contentScroll.getScrollY();
        categoryRows[category] = parentRow;
        categoryScrolls[category] = parentScroll;
        detailPrayer = prayer;
        advancedPrayer = advanced;
        focusedRow = 0;
        restoreScroll = 0;
        onRail = false;
        rebuildContent();
        contentScroll.scrollTo(0, 0);
        restoreFocus();
    }

    private boolean returnFromDetails() {
        if (detailPrayer < 0 && !advancedPrayer) return false;
        detailPrayer = -1;
        advancedPrayer = false;
        focusedRow = parentRow;
        restoreScroll = parentScroll;
        onRail = false;
        rebuildContent();
        restoreFocus();
        return true;
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

        android.widget.Button close = Ui.bigButton(this, Lang.t("Back to dashboard"));
        close.setTextSize(15);
        close.setMinHeight(Ui.dp(this, 48));
        close.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
        close.setOnClickListener(v -> finish());
        rail.addView(close);

        TextView hint = Ui.label(this, "↑ ↓  Browse  ·  OK  Open\nBack  Return", 12, Ui.DIM);
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
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row.setMinimumHeight(Ui.dp(this, 60));
        lp.setMargins(0, 0, 0, Ui.dp(this, 6));
        row.setLayoutParams(lp);

        TextView icon = Ui.label(this, CAT_ICONS[i], 20, Ui.DIM);
        icon.setGravity(Gravity.CENTER);
        icon.setMinWidth(Ui.dp(this, 30));
        row.addView(icon);

        TextView name = Ui.label(this, Lang.t(CAT_LABELS[i]), 16, Ui.TEXT);
        name.setMaxLines(2);
        Ui.margin(name, Ui.dp(this, 8), 0, Ui.dp(this, 4), 0);
        row.addView(name);

        navRows[i] = row;
        navIcons[i] = icon;
        navNames[i] = name;

        row.setOnClickListener(v -> openCategory(i));
        row.setOnKeyListener((v, keyCode, e) -> {
            if (e.getAction() == KeyEvent.ACTION_DOWN
                    && keyCode == (Lang.rtl() ? KeyEvent.KEYCODE_DPAD_LEFT
                            : KeyEvent.KEYCODE_DPAD_RIGHT)) {
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
        contentScroll = scroll;
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
                "↑ ↓ Browse  ·  OK Change  ·  Back Return", 12, Ui.DIM);
        hint.setGravity(Gravity.END);
        pane.addView(hint);
        return pane;
    }

    // ---------------------------------------------------------------- sections

    private void openCategory(int i) {
        if (detailPrayer < 0 && !advancedPrayer) {
            categoryRows[category] = focusedRow;
            categoryScrolls[category] = contentScroll.getScrollY();
        }
        category = i;
        detailPrayer = -1;
        advancedPrayer = false;
        focusedRow = categoryRows[i];
        restoreScroll = categoryScrolls[i];
        rebuildContent();
        paintNavs();
        onRail = false;
        restoreFocus();
    }

    private void paintNavs() {
        for (int i = 0; i < navRows.length; i++) paintNav(i);
    }

    private void paintNav(int i) {
        if (navRows[i].hasFocus()) return;
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
        contentRows.clear();
        sectionTitle.setText(CAT_ICONS[category] + "  " + Lang.t(CAT_LABELS[category]));
        sectionSub.setText(Lang.t(CAT_SUBS[category]));

        if (detailPrayer >= 0) {
            sectionTitle.setText(Lang.prayer(PrayerCalc.TIME_IDX[detailPrayer]));
            sectionSub.setText(Lang.t("Settings") + " · " + Lang.t("Athan & alerts"));
            buildPrayerAthan(detailPrayer);
        } else if (advancedPrayer) {
            sectionTitle.setText(Lang.t("Advanced prayer settings"));
            sectionSub.setText(Lang.t("Settings") + " · " + Lang.t("Prayer times"));
            buildAdvancedPrayer();
        } else switch (category) {
            case CAT_PRAYER: buildPrayer(); break;
            case CAT_ATHAN: buildAthan(); break;
            case CAT_TOOLS: buildTools(); break;
            case CAT_DISPLAY: buildDisplay(); break;
            case CAT_LANGUAGE: buildLanguage(); break;
            case CAT_RAMADAN: buildRamadan(); break;
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
        sub("CALENDAR");
        navRow("📆 Monthly timetable", "full month of prayer times",
                v -> startActivity(new Intent(this, TimetableActivity.class)));
        choiceRow("Hijri date",
                new String[]{"−2 days", "−1 day", "No adjustment", "+1 day", "+2 days"},
                () -> p.hijriOffset() + 2,
                v -> { p.setHijriOffset(v - 2); reschedule(); });
        note("Adjust only if your local moon-sighting date differs.");
        sub("ADVANCED");
        navRow("Advanced prayer settings", "Calculation, Asr and time adjustments",
                v -> openDetails(-1, true));
    }

    private void buildAdvancedPrayer() {
        sub("CALCULATION");
        choiceRow("Calculation method", PrayerTimes.METHOD_NAMES,
                p::method, v -> { p.setMethod(v); reschedule(); });
        choiceRow("Asr time", new String[]{"Shafi'i · Maliki · Hanbali", "Hanafi"},
                p::asr, v -> { p.setAsr(v); reschedule(); });
        sub("TIME ADJUSTMENTS");
        final TextView dstStatus = noteView(Lang.t(Daylight.status(this)));
        choiceRow("Daylight saving", Daylight.MODE_NAMES, p::dstMode,
                v -> {
                    p.setDstMode(v);
                    dstStatus.setText(Lang.t(Daylight.status(this)));
                    reschedule();
                });
        list.addView(dstStatus);
        navRow("Fine-tune prayer times", "± minutes per prayer",
                v -> startActivity(new Intent(this, AdjustActivity.class)));
        infoRow("About prayer calculations", "Auto follows the TV's zone and changes at the official "
                + "transition. On/Off shift every prayer by one hour when the TV clock "
                + "and your country disagree.");
        actionRow("Back to prayer times", v -> returnFromDetails());
    }

    private void buildPrayerAthan(int prayer) {
        sub("PRAYER ALERT");
        switchRow("Enable athan", () -> p.athanOn(prayer),
                on -> { p.setAthanOn(prayer, on); reschedule(); });
        choiceRow("Athan voice", Prefs.PRAYER_VOICE_OPTS,
                () -> Prefs.prayerVoiceOpt(p.prayerVoice(prayer)),
                v -> p.setPrayerVoice(prayer, Prefs.prayerVoiceStored(v)));
        note("Same as global uses the main Athan voice selection.");
        actionRow("Back to Athan & alerts", v -> returnFromDetails());
    }

    private void buildAthan() {
        sub("VOICE & VOLUME");
        choiceRow("Athan voice", Prefs.VOICE_NAMES, p::voice, v -> {
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
                Toast.makeText(this, Lang.t("Playing test athan…"), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, Lang.t("Could not start test"), Toast.LENGTH_SHORT).show();
            }
        });
        adjustRow("Athan volume", 0, 100, 5, p::athanVol,
                v -> v + "%", v -> p.setAthanVol(v));
        switchRow("🔊 Athan fade-in", p::fadeIn, p::setFadeIn);

        sub("ALERTS");
        choiceRow("Remind before athan",
                new String[]{"Off", "5 min before", "10 min before", "15 min before", "30 min before"},
                () -> idxOf(new int[]{0, 5, 10, 15, 30}, p.preMin()),
                v -> { p.setPreMin(new int[]{0, 5, 10, 15, 30}[v]); reschedule(); });
        choiceRow("Quiet TV after athan",
                new String[]{"Off", "10 min", "15 min", "20 min", "30 min"},
                () -> idxOf(new int[]{0, 10, 15, 20, 30}, p.quietMin()),
                v -> p.setQuietMin(new int[]{0, 10, 15, 20, 30}[v]));
        choiceRow("Athan popup style", new String[]{"Full screen", "Top banner"},
                p::overlayStyle, v -> p.setOverlayStyle(v));

        sub("INDIVIDUAL PRAYERS");
        note("Choose a prayer to customize its sound and alert.");
        for (int i = 0; i < 5; i++) {
            final int prayer = i;
            String status = Lang.t(p.athanOn(i) ? "On" : "Off") + " · "
                    + Lang.t(Prefs.PRAYER_VOICE_OPTS[Prefs.prayerVoiceOpt(p.prayerVoice(i))]);
            navRow(Lang.prayer(PrayerCalc.TIME_IDX[i]), status,
                    v -> openDetails(prayer, false));
        }

    }

    private void buildTools() {
        sub("DIRECTION");
        navRow("🧭 Qibla compass", "bearing from your city",
                v -> startActivity(new Intent(this, QiblaActivity.class)));
        choiceRow("TV faces",
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

    private void buildLanguage() {
        sub("LANGUAGE & NUMBERS");
        choiceRow("Language", new String[]{"Follow TV", "العربية", "English"},
                p::language, v -> { p.setLanguage(v); recreate(); });
        choiceRow("Numbers", new String[]{"Follow language", "Arabic-Indic", "Western"},
                p::digits, v -> { p.setDigits(v); recreate(); });
        choiceRow("Clock format", new String[]{"Follow TV", "12-hour", "24-hour"},
                p::clockMode, v -> { p.setClockMode(v); recreate(); });
        note("Language changes immediately. Your place in settings is preserved.");
    }

    private void buildRamadan() {
        sub("RAMADAN DISPLAY");
        switchRow("Expanded Ramadan mode", p::expandedRamadan, p::setExpandedRamadan);
        note("Off by default. During Ramadan, adds fasting duration, daylight fasting progress and the next Suhoor time to the existing card.");
        note("Suhoor ends at Fajr. The Hijri date adjustment in Prayer times also applies here.");
        sub("REMINDERS");
        switchRow("🌙 Taraweeh reminder (in Ramadan)", p::taraweeh,
                v -> { p.setTaraweeh(v); reschedule(); });
    }

    private void buildDisplay() {
        sub("APPEARANCE");
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
                                Lang.t("Open Settings → Device Preferences → Screen saver"),
                                Toast.LENGTH_LONG).show();
                    }
                });
        String ss = null;
        try {
            ss = Settings.Secure.getString(getContentResolver(), "screensaver_components");
        } catch (Exception ignored) {}
        note("System screen saver: " + (ss != null && ss.contains(getPackageName())
                ? "Salatona ✓" : "another app"));

        sub("HELP");
        infoRow("Screen saver setup help", "Google TV hides third-party screen savers from Settings → System → "
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
                Toast.makeText(this, Lang.t("Athan alarms rescheduled ✓"), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, Lang.t("Could not reschedule"), Toast.LENGTH_SHORT).show();
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
        t.setLetterSpacing(Lang.ar() ? 0f : 0.08f);
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
        return Ui.roundBgStroke(Ui.CARD, 0x224F8D79, Ui.dp(this, 1), Ui.dp(this, 18));
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
        final int rowIndex = contentRows.size();
        contentRows.add(row);
        row.setMinimumHeight(Ui.dp(this, 60));
        Ui.strongFocus(row, rowBg(), () -> { onRail = false; focusedRow = rowIndex; }, texts);
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
        v.setMaxLines(3);
        v.setMaxWidth(Ui.dp(this, 240));
        return v;
    }

    private void navRow(String label, String value, View.OnClickListener onClick) {
        LinearLayout inner = Ui.hrow(this);
        TextView lab = rowLabel(label);
        inner.addView(lab);
        TextView v = rowValue();
        v.setTextColor(Ui.DIM);
        v.setTextSize(14);
        v.setText(Lang.t(value) + (Lang.rtl() ? "  ‹" : "  ›"));
        inner.addView(v);
        LinearLayout row = baseRow(lab, v);
        row.addView(inner);
        row.setOnClickListener(onClick);
        row.setContentDescription(Lang.t(label) + ", " + Lang.t(value));
    }

    private void actionRow(String label, View.OnClickListener onClick) {
        LinearLayout inner = Ui.hrow(this);
        TextView lab = rowLabel(label);
        inner.addView(lab);
        TextView v = rowValue();
        v.setTextColor(Ui.DIM);
        v.setText(Lang.rtl() ? "‹" : "›");
        inner.addView(v);
        LinearLayout row = baseRow(lab, v);
        row.addView(inner);
        row.setOnClickListener(onClick);
    }

    private interface IntSetter { void set(int v); }
    private interface BoolSetter { void set(boolean v); }

    /** All option lists open explicitly; browsing never edits a setting. */
    private void choiceRow(String label, String[] options, IntSupplier cur, IntSetter setter) {
        TextView lab = rowLabel(label);
        TextView value = rowValue();
        LinearLayout inner = Ui.hrow(this);
        inner.addView(lab);
        inner.addView(value);
        LinearLayout row = baseRow(lab, value);
        row.addView(inner);
        Runnable paint = () -> {
            int selected = Math.max(0, Math.min(options.length - 1, cur.getAsInt()));
            value.setText(Lang.t(options[selected]) + (Lang.rtl() ? "  ‹" : "  ›"));
            row.setContentDescription(Lang.t(label) + ", " + Lang.t(options[selected]));
        };
        paint.run();
        row.setOnClickListener(v -> {
            String[] translated = new String[options.length];
            for (int i = 0; i < options.length; i++) translated[i] = Lang.t(options[i]);
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                    .setTitle(Lang.t(label))
                    .setSingleChoiceItems(translated, Math.max(0, Math.min(options.length - 1, cur.getAsInt())),
                            (d, which) -> {
                                d.dismiss();
                                if (which != cur.getAsInt()) setter.set(which);
                                paint.run();
                            }).setNegativeButton(Lang.t("Cancel"), null).create();
            showDialog(dialog);
        });
    }

    private void showDialog(android.app.AlertDialog dialog) {
        dialog.setOnShowListener(d -> {
            android.view.Window window = dialog.getWindow();
            if (window == null) return;
            window.getDecorView().setLayoutDirection(
                    Lang.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
            window.setBackgroundDrawable(Ui.roundBg(Ui.CARD, Ui.dp(this, 24)));
            Lang.localize(window.getDecorView());
            if (dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE) != null)
                dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setTextColor(Ui.GOLD_BRIGHT);
            if (dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE) != null)
                dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setTextColor(Ui.TEXT);
            android.widget.ListView choices = dialog.getListView();
            if (choices != null) {
                choices.setSelector(Ui.roundBgStroke(Ui.CARD_HI, Ui.GOLD,
                        Ui.dp(this, 2), Ui.dp(this, 12)));
                choices.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
                choices.requestFocus();
            }
        });
        dialog.show();
    }

    private void infoRow(String label, String message) {
        navRow(label, "Help", v -> showDialog(new android.app.AlertDialog.Builder(this)
                .setTitle(Lang.t(label)).setMessage(Lang.t(message))
                .setPositiveButton(Lang.t("Close"), null).create()));
    }

    /** Numeric changes use a preview slider with Apply and Cancel. */
    private void adjustRow(String label, int min, int max, int step, IntSupplier cur,
                           java.util.function.Function<Integer, String> fmt, IntSetter setter) {
        TextView lab = rowLabel(label);
        TextView value = rowValue();
        LinearLayout inner = Ui.hrow(this);
        inner.addView(lab);
        inner.addView(value);
        LinearLayout row = baseRow(lab, value);
        row.addView(inner);
        Runnable paint = () -> value.setText(Lang.num(fmt.apply(cur.getAsInt())));
        paint.run();
        row.setOnClickListener(v -> {
            LinearLayout body = new LinearLayout(this);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(Ui.dp(this, 28), Ui.dp(this, 16), Ui.dp(this, 28), Ui.dp(this, 16));
            TextView preview = Ui.label(this, "", 24, Ui.GOLD_BRIGHT);
            preview.setGravity(Gravity.CENTER);
            body.addView(preview);
            android.widget.SeekBar slider = new android.widget.SeekBar(this);
            slider.setMax((max - min) / step);
            slider.setProgress((cur.getAsInt() - min) / step);
            slider.setKeyProgressIncrement(1);
            slider.setContentDescription(Lang.t(label));
            preview.setText(Lang.num(fmt.apply(min + slider.getProgress() * step)));
            slider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(android.widget.SeekBar bar, int progress, boolean user) {
                    preview.setText(Lang.num(fmt.apply(min + progress * step)));
                }
                public void onStartTrackingTouch(android.widget.SeekBar bar) {}
                public void onStopTrackingTouch(android.widget.SeekBar bar) {}
            });
            body.addView(slider, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 64)));
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                    .setTitle(Lang.t(label)).setView(body)
                    .setPositiveButton(Lang.t("Apply"), (d, which) -> {
                        setter.set(min + slider.getProgress() * step);
                        paint.run();
                    }).setNegativeButton(Lang.t("Cancel"), null).create();
            showDialog(dialog);
            slider.requestFocus();
        });
    }

    /** OK or tap toggles. Direction keys only navigate. */
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
            row.setContentDescription(Lang.t(label) + ", " + Lang.t(on ? "On" : "Off"));
        };
        paint.run();
        View.OnClickListener toggle = vv -> {
            setter.set(!cur.getAsBoolean());
            paint.run();
        };
        row.setOnClickListener(toggle);
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
            return "2.1";
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
            Toast.makeText(this, Lang.t("No file picker on this device"), Toast.LENGTH_SHORT).show();
            rebuildContent();
            restoreFocus();
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
                    Toast.makeText(this, Lang.t("Custom athan set ✓"), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    p.setVoice(voiceBeforePicker);
                    Toast.makeText(this, Lang.t("Could not use that file"), Toast.LENGTH_SHORT).show();
                }
            } else {
                p.setVoice(voiceBeforePicker);
            }
            rebuildContent();
            restoreFocus();
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(req, permissions, results);
        if (req == REQ_NOTIFICATIONS) {
            boolean ok = results.length > 0
                    && results[0] == PackageManager.PERMISSION_GRANTED;
            Toast.makeText(this, Lang.t(ok ? "Notifications allowed ✓"
                    : "Notifications stay blocked"), Toast.LENGTH_SHORT).show();
            rebuildContent();
            restoreFocus();
        }
    }
}
