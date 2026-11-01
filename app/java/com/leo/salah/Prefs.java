package com.leo.salah;

import android.content.Context;
import android.content.SharedPreferences;

/** All user settings for Salatona. */
public class Prefs {
    private static final String F = "leo_salah";
    private final SharedPreferences s;

    public Prefs(Context c) { s = c.getSharedPreferences(F, Context.MODE_PRIVATE); }

    // ---- location ----
    public String cityName() { return s.getString("city_name", "Tripoli"); }
    public String cityCountry() { return s.getString("city_country", "Libya"); }
    public double lat() { return Double.longBitsToDouble(s.getLong("lat", Double.doubleToLongBits(32.9003))); }
    public double lng() { return Double.longBitsToDouble(s.getLong("lng", Double.doubleToLongBits(13.1858))); }
    public void setCity(String name, String country, double lat, double lng) {
        s.edit().putString("city_name", name).putString("city_country", country)
                .putLong("lat", Double.doubleToLongBits(lat))
                .putLong("lng", Double.doubleToLongBits(lng)).apply();
    }

    // ---- calculation ----
    public int method() { return s.getInt("method", PrayerTimes.MWL); }
    public void setMethod(int m) { s.edit().putInt("method", m).apply(); }
    public int asr() { return s.getInt("asr", 0); } // 0 Shafi'i, 1 Hanafi
    public void setAsr(int a) { s.edit().putInt("asr", a).apply(); }

    // per-prayer minute adjustments: keys tune_0..tune_5 (fajr,sunrise,dhuhr,asr,maghrib,isha)
    public int tune(int timeIdx) { return s.getInt("tune_" + timeIdx, 0); }
    public void setTune(int timeIdx, int min) { s.edit().putInt("tune_" + timeIdx, min).apply(); }
    public int[] tuneArray() {
        return new int[]{tune(0), tune(1), tune(2), tune(3), tune(4), tune(5), 0};
    }

    // ---- athan voice ----
    // 0..2 bundled voices, 3 Libyan (Dokali Elalam), 4 custom file, 5 spoken announcement
    public int voice() {
        // one-time migration for the v1.7 voice list (custom 3->4, spoken 4->5)
        if (!s.getBoolean("voice_migrated_v17", false)) {
            int v = s.getInt("voice", 0);
            if (v >= 3) s.edit().putInt("voice", v + 1).apply();
            s.edit().putBoolean("voice_migrated_v17", true).apply();
        }
        return s.getInt("voice", 0);
    }
    public void setVoice(int v) { s.edit().putInt("voice", v).apply(); }
    public String customUri() { return s.getString("custom_uri", ""); }
    public void setCustomUri(String u) { s.edit().putString("custom_uri", u).apply(); }
    public int athanVol() { return s.getInt("athan_vol", 80); }
    public void setAthanVol(int v) { s.edit().putInt("athan_vol", Math.max(0, Math.min(100, v))).apply(); }
    public boolean fadeIn() { return s.getBoolean("fade_in", true); }
    public void setFadeIn(boolean b) { s.edit().putBoolean("fade_in", b).apply(); }

    // per-prayer athan on/off (prayer idx 0..4 = fajr,dhuhr,asr,maghrib,isha)
    public boolean athanOn(int prayerIdx) { return s.getBoolean("athan_" + prayerIdx, true); }
    public void setAthanOn(int prayerIdx, boolean on) { s.edit().putBoolean("athan_" + prayerIdx, on).apply(); }

    // ---- behavior ----
    public int preMin() { return s.getInt("pre_min", 0); }
    public void setPreMin(int m) { s.edit().putInt("pre_min", m).apply(); }
    public int quietMin() { return s.getInt("quiet_min", 0); }
    public void setQuietMin(int m) { s.edit().putInt("quiet_min", m).apply(); }
    public int overlayStyle() { return s.getInt("overlay_style", 0); } // 0 full, 1 banner
    public void setOverlayStyle(int st) { s.edit().putInt("overlay_style", st).apply(); }

    public String scheduledDay() { return s.getString("scheduled_day", ""); }
    public void setScheduledDay(String d) { s.edit().putString("scheduled_day", d).apply(); }

    public static final String[] VOICE_NAMES = {
            "Athan · Voice 1", "Athan · Voice 2", "Athan · Voice 3",
            "🇱🇾 Libyan · Dokali Elalam",
            "Custom audio file…", "Spoken announcement" };

    // ---- per-prayer athan voice ----
    // stored: -1 = same as global, 0..3 bundled/Libyan voices, 5 = spoken announcement
    // (custom file stays global-only)
    public int prayerVoice(int prayerIdx) { return s.getInt("pvoice_" + prayerIdx, -1); }
    public void setPrayerVoice(int prayerIdx, int v) {
        s.edit().putInt("pvoice_" + prayerIdx, v).apply();
    }
    public int effectiveVoice(int prayerIdx) {
        int v = prayerVoice(prayerIdx);
        return v < 0 ? voice() : v;
    }
    public static final String[] PRAYER_VOICE_OPTS = {
            "🌐 Same as global",
            "Athan · Voice 1", "Athan · Voice 2", "Athan · Voice 3",
            "🇱🇾 Libyan · Dokali Elalam", "Spoken announcement" };
    /** cycle-row option index (0..5) for a prayer; -1 stored = 0 (global). */
    public static int prayerVoiceOpt(int stored) {
        return stored < 0 ? 0 : (stored == 5 ? 5 : stored + 1);
    }
    /** stored value for a cycle-row option index (0..5). */
    public static int prayerVoiceStored(int opt) {
        return opt == 0 ? -1 : (opt == 5 ? 5 : opt - 1);
    }

    // ---- iqamah countdown (minutes after athan, per prayer; 0 = off) ----
    public int iqamah(int prayerIdx) { return s.getInt("iqamah_" + prayerIdx, 15); }
    public void setIqamah(int prayerIdx, int min) {
        s.edit().putInt("iqamah_" + prayerIdx, Math.max(0, Math.min(60, min))).apply();
    }

    // ---- prayer streaks: "yyyy-MM-dd:prayerIdx" entries ----
    public void markPrayed(String dayKey, int prayerIdx) {
        java.util.Set<String> set =
                new java.util.HashSet<>(s.getStringSet("prayed_days",
                        java.util.Collections.<String>emptySet()));
        // keep the store bounded: drop entries older than ~400 days
        java.util.Iterator<String> it = set.iterator();
        String cutoff = cutoffDay(400);
        while (it.hasNext()) {
            String e = it.next();
            if (e.length() >= 10 && e.substring(0, 10).compareTo(cutoff) < 0) it.remove();
        }
        set.add(dayKey + ":" + prayerIdx);
        s.edit().putStringSet("prayed_days", set).apply();
    }
    private static String cutoffDay(int daysBack) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.DATE, -daysBack);
        return PrayerCalc.dayKey(c);
    }
    public boolean prayedOn(String dayKey, int prayerIdx) {
        return s.getStringSet("prayed_days",
                java.util.Collections.<String>emptySet()).contains(dayKey + ":" + prayerIdx);
    }
    public int prayedCount(String dayKey) {
        int n = 0;
        for (int i = 0; i < 5; i++) if (prayedOn(dayKey, i)) n++;
        return n;
    }

    // ---- ramadan + adhkar reminders ----
    public boolean taraweeh() { return s.getBoolean("taraweeh", true); }
    public void setTaraweeh(boolean b) { s.edit().putBoolean("taraweeh", b).apply(); }
    public boolean adhkarMorning() { return s.getBoolean("adhkar_morning", true); }
    public void setAdhkarMorning(boolean b) { s.edit().putBoolean("adhkar_morning", b).apply(); }
    public boolean adhkarEvening() { return s.getBoolean("adhkar_evening", true); }
    public void setAdhkarEvening(boolean b) { s.edit().putBoolean("adhkar_evening", b).apply(); }
}
