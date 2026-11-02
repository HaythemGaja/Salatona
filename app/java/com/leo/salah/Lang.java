package com.leo.salah;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arabic / English UI layer.
 *
 * <p>Salatona's screens are built in Java, so instead of duplicating every
 * screen this class keeps an English → Arabic dictionary and localises a view
 * tree once it has been built. Dynamic strings (countdowns, status lines,
 * dates) are translated by the patterns below. {@link #num(String)} switches
 * the digits, and the layout direction follows the language.
 */
public final class Lang {

    private static boolean ar;
    private static boolean eastern;
    private static boolean clock24;
    private static final Map<String, String> M = new HashMap<>();
    private static final char[] EASTERN = {
            '٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'};

    private Lang() {}

    public static void init(Context c) {
        Prefs p = new Prefs(c);
        int mode = p.language();
        if (mode == 1) ar = true;
        else if (mode == 2) ar = false;
        else ar = Locale.getDefault().getLanguage().equals("ar");
        int d = p.digits();
        eastern = d == 1 || (d != 2 && ar);
        int cm = p.clockMode();
        clock24 = cm == 2 || (cm == 0 && DateFormat.is24HourFormat(c));
    }

    public static boolean ar() { return ar; }
    public static boolean rtl() { return ar; }
    public static boolean eastern() { return eastern; }
    public static boolean is24Hour() { return clock24; }
    public static Locale locale() { return ar ? new Locale("ar") : Locale.US; }

    /** Arabic-Indic digits when enabled. */
    public static String num(String s) {
        if (!eastern || s == null) return s;
        StringBuilder b = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                if (b == null) b = new StringBuilder(s);
                b.setCharAt(i, EASTERN[c - '0']);
            }
        }
        return b == null ? s : b.toString();
    }

    public static String prayer(int idx) {
        if (idx < 0 || idx > 5) return "";
        return ar ? PrayerCalc.DISPLAY_AR[idx] : PrayerCalc.DISPLAY_NAMES[idx];
    }

    public static String hijriMonth(int month1to12) {
        if (month1to12 < 1 || month1to12 > 12) return "";
        return ar ? HIJRI_AR[month1to12 - 1] : PrayerCalc.hijriMonthName(month1to12);
    }

    public static String city(String name, String country) {
        if (!ar) return name + ", " + country;
        return Cities.nameAr(name) + "، " + Cities.countryAr(country);
    }

    public static String hijriDate(int day, int month, int year) {
        if (ar) return num(day + " " + HIJRI_AR[month - 1] + " " + year + " هـ");
        return day + " " + PrayerCalc.hijriMonthName(month) + " " + year + " AH";
    }

    public static String clock(Date when, boolean withSeconds) {
        String pattern = clock24 ? (withSeconds ? "HH:mm:ss" : "HH:mm")
                : (withSeconds ? "hh:mm:ss a" : "hh:mm a");
        SimpleDateFormat f = new SimpleDateFormat(pattern, locale());
        return num(f.format(when));
    }

    public static String fullDate(Date d) {
        SimpleDateFormat f = new SimpleDateFormat(ar ? "EEEE، d MMMM yyyy" : "EEEE, MMMM d, yyyy",
                locale());
        return num(f.format(d));
    }

    public static String shortDate(Date d) {
        SimpleDateFormat f = new SimpleDateFormat(ar ? "d MMM" : "MMM d", locale());
        return num(f.format(d));
    }

    /** Translate a whole string; safe to call repeatedly. */
    public static String t(String s) {
        if (!ar || s == null || s.isEmpty()) return num(s);
        String out = translate(s);
        return num(out);
    }

    private static String translate(String s) {
        String exact = M.get(s);
        if (exact != null) return exact;

        // "‹  value  ›" from a settings cycle row
        int open = s.indexOf('‹');
        int close = s.lastIndexOf('›');
        if (open >= 0 && close > open) {
            String core = s.substring(open + 1, close).trim();
            String tr = M.get(core);
            if (tr != null) return "‹  " + tr + "  ›";
        }

        Matcher m;
        if ((m = p("^Athan for (.+)$", s)) != null)
            return "أذان " + prayerWord(m.group(1));
        if ((m = p("^🎙 (.+) voice$", s)) != null)
            return "🎙 صوت " + prayerWord(m.group(1));
        if ((m = p("^(.+) voice$", s)) != null && M.containsKey(m.group(1)))
            return "صوت " + prayerWord(m.group(1));
        if ((m = p("^🕌 (.+) in (\\d{1,2}:\\d{2})$", s)) != null)
            return "🕌 " + trOr(m.group(1), m.group(1)) + " بعد " + m.group(2);
        if ((m = p("^(.+) in (\\d{1,2}:\\d{2})$", s)) != null)
            return trOr(m.group(1), m.group(1)) + " بعد " + m.group(2);
        if ((m = p("^⏳ (.+) began (.+) ago$", s)) != null)
            return "⏳ بدأ " + prayerWord(m.group(1)) + " منذ " + m.group(2);
        if ((m = p("^⏳ (.+) just started$", s)) != null)
            return "⏳ بدأ " + prayerWord(m.group(1)) + " للتو";
        if ((m = p("^🌙 Suhoor ends (.+) · in (.+)$", s)) != null)
            return "🌙 ينتهي السحور " + m.group(1) + " · بعد " + m.group(2);
        if ((m = p("^🌙 Iftar (.+) · in (.+)$", s)) != null)
            return "🌙 الإفطار " + m.group(1) + " · بعد " + m.group(2);
        if ((m = p("^Qibla is (\\d+)° clockwise from North\\nFace (.+)$", s)) != null)
            return "القبلة على " + m.group(1) + "° باتجاه عقارب الساعة من الشمال\nاتّجه نحو "
                    + trOr(m.group(2), m.group(2));
        if ((m = p("^from (.+)$", s)) != null)
            return "من " + m.group(1);
        if ((m = p("^Alarms scheduled for (.+)\\. Salatona refreshes them in the background every day\\.$", s)) != null)
            return "المنبهات مجدولة ليوم " + m.group(1) + ". تُحدّثها صلاتنا تلقائياً كل يوم.";
        if ((m = p("^System screen saver: (.+)$", s)) != null)
            return "شاشة توقف النظام: " + trOr(m.group(1), m.group(1));
        if ((m = p("^Version (.+)$", s)) != null)
            return "الإصدار " + m.group(1);
        if ((m = p("^Salatona (.+)$", s)) != null)
            return "صلاتنا " + m.group(1);
        if ((m = p("^TV zone: (.+)$", s)) != null)
            return "منطقة التلفاز: " + m.group(1);
        if ((m = p("^(.+) · standard time · (UTC.+)$", s)) != null)
            return m.group(1) + " · التوقيت القياسي · " + m.group(2);
        if ((m = p("^(.+) · daylight saving active · (UTC.+)$", s)) != null)
            return m.group(1) + " · التوقيت الصيفي مفعّل · " + m.group(2);
        if ((m = p("^(.+) · shifted \\+1 hour · (UTC.+)$", s)) != null)
            return m.group(1) + " · متقدّم ساعة · " + m.group(2);
        if ((m = p("^(.+) · shifted −1 hour · (UTC.+)$", s)) != null)
            return m.group(1) + " · متأخر ساعة · " + m.group(2);
        if ((m = p("^(.+?) (\\d+)°C · (.+)$", s)) != null)
            return m.group(1) + " " + m.group(2) + "°م · " + trOr(m.group(3), m.group(3));
        if ((m = p("^Test · (.+)$", s)) != null)
            return "تجربة · " + trOr(m.group(1), m.group(1));
        if ((m = p("^(.+) Athan$", s)) != null)
            return "أذان " + prayerWord(m.group(1));
        if ((m = p("^(.+) soon$", s)) != null)
            return prayerWord(m.group(1)) + " بعد قليل";
        if ((m = p("^begins in (\\d+) minutes · (.+)$", s)) != null)
            return "يبدأ بعد " + m.group(1) + " دقيقة · " + m.group(2);
        if ((m = p("^🌙 (.+) · today!$", s)) != null)
            return "🌙 " + trOr(m.group(1), m.group(1)) + " · اليوم!";
        if ((m = p("^🌙 (.+) · tomorrow$", s)) != null)
            return "🌙 " + trOr(m.group(1), m.group(1)) + " · غداً";
        if ((m = p("^🌙 (.+) · in (\\d+) days$", s)) != null)
            return "🌙 " + trOr(m.group(1), m.group(1)) + " · بعد " + m.group(2) + " أيام";
        if ((m = p("^in (\\d+) days$", s)) != null)
            return "بعد " + m.group(1) + " أيام";
        if ((m = p("^(\\d+) days ago$", s)) != null)
            return "قبل " + m.group(1) + " أيام";
        if ((m = p("^Hijri year (\\d+) · dates may differ ±1 day from moon sighting$", s)) != null)
            return "السنة الهجرية " + m.group(1) + " · قد تختلف التواريخ يوماً واحداً حسب رؤية الهلال";
        if ((m = p("^City set: (.+) ✓$", s)) != null)
            return "تم تعيين المدينة: " + m.group(1) + " ✓";
        if ((m = p("^Qibla is (.+?) from the TV$", s)) != null)
            return "القبلة " + trOr(m.group(1), m.group(1)) + " من التلفاز";
        if ((m = p("^Playing: (.+)$", s)) != null)
            return "يعمل: " + trOr(m.group(1), m.group(1));
        if ((m = p("^Selected: (.+)$", s)) != null)
            return "المختار: " + trOr(m.group(1), m.group(1));
        return s;
    }

    private static Matcher p(String regex, String s) {
        Matcher m = Pattern.compile(regex).matcher(s);
        return m.matches() ? m : null;
    }

    private static String trOr(String s, String fallback) {
        String v = M.get(s);
        return v != null ? v : fallback;
    }

    private static String prayerWord(String english) {
        for (int i = 0; i < PrayerCalc.DISPLAY_NAMES.length; i++)
            if (PrayerCalc.DISPLAY_NAMES[i].equals(english)) return PrayerCalc.DISPLAY_AR[i];
        if ("Jumu'ah".equals(english)) return "الجمعة";
        String v = M.get(english);
        return v != null ? v : english;
    }

    public static void localize(View v) {
        if (v == null) return;
        if (v instanceof EditText) {
            EditText e = (EditText) v;
            CharSequence hint = e.getHint();
            if (hint != null) {
                String h = t(hint.toString());
                if (!h.equals(hint.toString())) e.setHint(h);
            }
            return;
        }
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            CharSequence c = tv.getText();
            if (c != null && c.length() > 0) {
                String s = t(c.toString());
                if (!s.equals(c.toString())) tv.setText(s);
            }
            return;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) localize(g.getChildAt(i));
        }
    }

    private static final String[] HIJRI_AR = {
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"};

    static {
        // ---------------- general / settings ----------------
        put("Settings", "الإعدادات");
        put("Prayer times", "مواقيت الصلاة");
        put("Athan", "الأذان");
        put("Tools", "الأدوات");
        put("Display", "العرض");
        put("System", "النظام");
        put("About", "حول");
        put("Location, calculation and daylight saving", "الموقع والحساب والتوقيت الصيفي");
        put("Voice, volume and prayer alerts", "الصوت ومستوى الأذان والتنبيهات");
        put("Qibla, duas and the Hijri calendar", "القبلة والأدعية والتقويم الهجري");
        put("Screen saver and ambient clock", "شاشة التوقف وساعة الانتظار");
        put("Permissions and alarm reliability", "الأذونات وموثوقية المنبهات");
        put("Version, time zone and privacy", "الإصدار والمنطقة الزمنية والخصوصية");
        put("↑ ↓  choose section\nOK  open  ·  Back  return", "↑ ↓  اختر قسماً\nOK  فتح  ·  رجوع  عودة");
        put("‹ › change  ·  OK select  ·  Back to sections", "‹ › تغيير  ·  OK اختيار  ·  رجوع للأقسام");
        put("Settings", "الإعدادات");

        put("City", "المدينة");
        put("Calculation method", "طريقة الحساب");
        put("Asr time", "وقت العصر");
        put("Daylight saving", "التوقيت الصيفي");
        put("Fine-tune prayer times", "ضبط أوقات الصلاة");
        put("Monthly timetable", "الجدول الشهري");
        put("Hijri date", "التاريخ الهجري");
        put("Athan voice", "صوت الأذان");
        put("Test athan", "تجربة الأذان");
        put("Athan volume", "مستوى صوت الأذان");
        put("Athan fade-in", "بدء الأذان تدريجياً");
        put("Remind before athan", "التذكير قبل الأذان");
        put("Quiet TV after athan", "كتم التلفاز بعد الأذان");
        put("Athan popup style", "شكل نافذة الأذان");
        put("Taraweeh reminder (in Ramadan)", "تذكير التراويح (في رمضان)");
        put("Qibla compass", "بوصلة القبلة");
        put("Duas", "الأدعية");
        put("Islamic events", "المناسبات الإسلامية");
        put("Ambient display", "شاشة الانتظار");
        put("Set Salatona as screen saver", "تعيين صلاتنا شاشة توقف");
        put("Battery", "البطارية");
        put("🔋 Battery", "🔋 البطارية");
        put("Reschedule athan alarms", "إعادة جدولة منبهات الأذان");
        put("Display over other apps", "العرض فوق التطبيقات");
        put("Exact alarms", "المنبهات الدقيقة");
        put("Notifications", "الإشعارات");
        put("Close settings", "إغلاق الإعدادات");
        put("TV faces", "اتجاه التلفاز");
        put("Quran radio", "إذاعة القرآن");
        put("Language", "اللغة");
        put("Numbers", "الأرقام");
        put("Clock format", "صيغة الساعة");
        put("Night colours", "ألوان ليلية");
        put("Auto", "تلقائي");
        put("On", "مفعّل");
        put("Off", "متوقّف");
        put("Not set", "غير محدد");
        put("ahead", "أمامك");
        put("ahead-right", "أمامك يميناً");
        put("ahead-left", "أمامك يساراً");
        put("to your right", "على يمينك");
        put("to your left", "على يسارك");
        put("behind-right", "خلفك يميناً");
        put("behind-left", "خلفك يساراً");
        put("behind you", "خلفك");
        put("North", "الشمال");
        put("North-East", "الشمال الشرقي");
        put("East", "الشرق");
        put("South-East", "الجنوب الشرقي");
        put("South", "الجنوب");
        put("South-West", "الجنوب الغربي");
        put("West", "الغرب");
        put("North-West", "الشمال الغربي");
        put("Follow TV", "حسب التلفاز");
        put("12-hour", "١٢ ساعة");
        put("24-hour", "٢٤ ساعة");
        put("Arabic-Indic", "أرقام عربية");
        put("Western", "أرقام غربية");
        put("Follow language", "حسب اللغة");
        put("Arabic", "العربية");
        put("Shafi'i · Maliki · Hanbali", "الشافعي · المالكي · الحنبلي");
        put("Hanafi", "الحنفي");
        put("Auto · follow TV zone", "تلقائي · حسب منطقة التلفاز");
        put("On · +1 hour", "مفعّل · +ساعة");
        put("Off · −1 hour", "متوقّف · −ساعة");
        put("Off", "متوقّف");
        put("5 min before", "قبل ٥ دقائق");
        put("10 min before", "قبل ١٠ دقائق");
        put("15 min before", "قبل ١٥ دقيقة");
        put("30 min before", "قبل ٣٠ دقيقة");
        put("10 min", "١٠ دقائق");
        put("15 min", "١٥ دقيقة");
        put("20 min", "٢٠ دقيقة");
        put("30 min", "٣٠ دقيقة");
        put("Full screen", "ملء الشاشة");
        put("Top banner", "شريط علوي");
        put("● ON", "● مفعّل");
        put("○ OFF", "○ متوقّف");
        put("✓ Granted", "✓ ممنوح");
        put("✗ Grant ›", "✗ منح ›");
        put("✓ Unrestricted", "✓ غير مقيّد");
        put("✗ Restricted ›", "✗ مقيّد ›");
        put("✓ Unrestricted", "✓ غير مقيّد");
        put("Version", "الإصدار");
        put("System screen saver: another app", "شاشة توقف النظام: تطبيق آخر");
        put("System screen saver: Salatona ✓", "شاشة توقف النظام: صلاتنا ✓");
        put("Auto follows the TV's zone and changes at the official transition. On/Off shift every prayer by one hour when the TV clock and your country disagree.",
                "تلقائي يتبع منطقة التلفاز ويتغيّر في موعد التحويل الرسمي. استخدم مفعّل/متوقّف لإزاحة كل الصلوات ساعة واحدة إذا اختلفت ساعة التلفاز عن بلدك.");
        put("Google TV hides third-party screen savers from Settings → System → Ambient mode. “Ambient display” above keeps the prayer clock on until any button is pressed. To make it automatic on idle, set it once via ADB:\nsettings put secure screensaver_components com.leo.salah/.SalahDream",
                "يخفي Google TV شاشات التوقف التابعة لجهات أخرى من الإعدادات ← النظام ← وضع الانتظار. «شاشة الانتظار» أعلاه تُبقي ساعة الصلاة ظاهرة حتى ضغط أي زر. لجعلها تلقائية عند الخمول، عيّنها مرة واحدة عبر ADB:\nsettings put secure screensaver_components com.leo.salah/.SalahDream");
        put("The athan overlay needs “Display over other apps”. Exact alarms and notifications keep it on time on Android 12 and newer TVs.",
                "يحتاج الأذان إلى «العرض فوق التطبيقات». المنبهات الدقيقة والإشعارات تُبقيه في وقته على تلفازات Android 12 والأحدث.");
        put("Alarms scheduled for", "المنبهات مجدولة ليوم");

        put("LOCATION", "الموقع");
        put("CALCULATION", "الحساب");
        put("CALENDAR", "التقويم");
        put("VOICE & VOLUME", "الصوت والمستوى");
        put("PRAYERS", "الصلوات");
        put("ALERTS", "التنبيهات");
        put("DIRECTION", "الاتجاه");
        put("QURAN", "القرآن");
        put("SUPPLICATIONS", "الأدعية");
        put("HIJRI CALENDAR", "التقويم الهجري");
        put("LANGUAGE & NUMBERS", "اللغة والأرقام");
        put("AMBIENT", "الانتظار");
        put("GOOGLE TV", "Google TV");
        put("RELIABILITY", "الموثوقية");
        put("PERMISSIONS", "الأذونات");
        put("SALATONA", "صلاتنا");
        put("TIME ZONE", "المنطقة الزمنية");
        put("PRIVACY", "الخصوصية");
        put("📍 City", "📍 المدينة");
        put("📆 Monthly timetable", "📆 الجدول الشهري");
        put("🧭 Qibla compass", "🧭 بوصلة القبلة");
        put("📻 Quran radio", "📻 إذاعة القرآن");
        put("📅 Islamic events", "📅 المناسبات الإسلامية");
        put("🤲 Duas", "🤲 الأدعية");
        put("▶ Ambient display", "▶ شاشة الانتظار");
        put("🔊 Test athan", "🔊 تجربة الأذان");
        put("🌙 Taraweeh reminder (in Ramadan)", "🌙 تذكير التراويح (في رمضان)");
        put("⏰ Reschedule athan alarms", "⏰ إعادة جدولة منبهات الأذان");
        put("Setting Salatona as screen saver", "تعيين صلاتنا شاشة توقف");
        put("± minutes per prayer", "± دقائق لكل صلاة");
        put("full month of prayer times", "الشهر كاملاً لأوقات الصلاة");
        put("bearing from your city", "الاتجاه من مدينتك");
        put("after prayer · morning · night", "بعد الصلاة · الصباح · المساء");
        put("upcoming & past, countdown in days", "القادم والماضي، العدّ بالأيام");
        put("prayer clock that keeps the screen on", "ساعة الصلاة التي تُبقي الشاشة مضاءة");
        put("system screen-saver settings", "إعدادات شاشة توقف النظام");
        put("live stations and reciters", "محطات مباشرة وقُرّاء");
        put("No adjustment", "بدون تعديل");
        put("−2 days", "−يومان");
        put("−1 day", "−يوم");
        put("+1 day", "+يوم");
        put("+2 days", "+يومان");
        put("±0 min", "±٠ دقيقة");
        put("Umm al-Qura calendar; move it a day when your local moon sighting differs.",
                "تقويم أم القرى؛ حرّكه يوماً واحداً إذا اختلفت رؤية الهلال عندك.");
        put("Used for the room-relative Qibla line: tell Salatona which way the screen faces.",
                "يُستخدم لسطر القبلة النسبي للغرفة: أخبر صلاتنا باتجاه الشاشة.");
        put("Test · ", "تجربة · ");
        put("On-device prayer times with a full-day timeline, athan overlay, timetable, Qibla, duas and a Ramadan card.",
                "أوقات صلاة تُحسب على الجهاز مع شريط ليوم كامل، وأذان فوق التطبيقات، وجدول، وقبلة، وأدعية، وبطاقة رمضان.");
        put("No accounts, no ads, no analytics.", "بلا حسابات أو إعلانات أو تتبّع.");
        put("Your city and settings stay on this TV. Weather is the only network request; prayer times never need the internet.",
                "تبقى مدينتك وإعداداتك على هذا التلفاز. الطقس هو طلب الشبكة الوحيد؛ أوقات الصلاة لا تحتاج الإنترنت.");
        put("The athan overlay needs “Display over other apps”. Exact alarms and notifications keep it on time on Android 12 and newer TVs.",
                "يحتاج الأذان إلى «العرض فوق التطبيقات». المنبهات الدقيقة والإشعارات تُبقيه في وقته على تلفازات Android 12 والأحدث.");
        // ---------------- calculation methods ----------------
        put("Shia (Jafari)", "الشيعة (الجعفري)");
        put("Karachi", "كراتشي");
        put("ISNA (North America)", "إسنا (أمريكا الشمالية)");
        put("Muslim World League", "رابطة العالم الإسلامي");
        put("Umm al-Qura (Makkah)", "أم القرى (مكة)");
        put("Egyptian Authority", "الهيئة المصرية");
        put("Tehran", "طهران");
        put("Dubai", "دبي");
        put("Kuwait", "الكويت");
        put("Qatar", "قطر");
        put("Singapore", "سنغافورة");
        put("Turkey (Diyanet)", "تركيا (ديانت)");
        put("Oman Awqaf", "أوقاف عُمان");
        put("Morocco Habous", "أوقاف المغرب");

        // ---------------- voices / prayers ----------------
        put("Athan · Voice 1", "أذان · صوت ١");
        put("Athan · Voice 2", "أذان · صوت ٢");
        put("Athan · Voice 3", "أذان · صوت ٣");
        put("🇱🇾 Libyan · Dokali Elalam", "🇱🇾 ليبي · دوكالي العالم");
        put("Custom audio file…", "ملف صوتي مخصص…");
        put("Spoken announcement", "إعلان منطوق");
        put("Arabic announcement", "إعلان بالعربية");
        put("🌐 Same as global", "🌐 مثل العام");
        put("today!", "اليوم!");
        put("tomorrow", "غداً");
        put("yesterday", "أمس");
        put("Fajr", "الفجر");
        put("Sunrise", "الشروق");
        put("Dhuhr", "الظهر");
        put("Asr", "العصر");
        put("Maghrib", "المغرب");
        put("Isha", "العشاء");
        put("Jumu'ah", "الجمعة");
        put("Shurûq", "الشروق");
        put("Fajr voice", "صوت الفجر");
        put("Dhuhr voice", "صوت الظهر");
        put("Asr voice", "صوت العصر");
        put("Maghrib voice", "صوت المغرب");
        put("Isha voice", "صوت العشاء");

        // ---------------- dashboard / home ----------------
        put("Ramadan", "رمضان");
        put("Eid al-Fitr", "عيد الفطر");
        put("Arafah Day", "يوم عرفة");
        put("Eid al-Adha", "عيد الأضحى");
        put("Islamic New Year", "رأس السنة الهجرية");
        put("Ashura", "عاشوراء");
        put("Laylat al-Mi'raj", "ليلة المعراج");
        put("Laylat al-Bara'at", "ليلة النصف من شعبان");
        put("🕌 Prayer times", "🕌 مواقيت الصلاة");
        put("Time +1h", "الوقت +ساعة");
        put("Time −1h", "الوقت −ساعة");
        put("just started", "بدأ للتو");
        put("🌙 Iftar time — تقبل الله", "🌙 وقت الإفطار — تقبل الله");
        put("New Moon", "محاق");
        put("Waxing Crescent", "هلال متزايد");
        put("First Quarter", "تربيع أول");
        put("Waxing Gibbous", "أحدب متزايد");
        put("Full Moon", "بدر");
        put("Waning Gibbous", "أحدب متناقص");
        put("Last Quarter", "تربيع أخير");
        put("Waning Crescent", "هلال متناقص");
        put("Clear", "صافٍ");
        put("Mostly clear", "صافٍ غالباً");
        put("Partly cloudy", "غائم جزئياً");
        put("Cloudy", "غائم");
        put("Foggy", "ضباب");
        put("Drizzle", "رذاذ");
        put("Rain", "مطر");
        put("Snow", "ثلج");
        put("Storm", "عاصفة");
        put("Windy", "عاصف");
        put("🕌 Jumu'ah — read Surat al-Kahf", "🕌 الجمعة — اقرأ سورة الكهف");
        put("⚠ Allow “Display over other apps” so the athan appears over other apps.",
                "⚠ اسمح بـ«العرض فوق التطبيقات» ليظهر الأذان فوق التطبيقات الأخرى.");
        put("⚠ Allow exact alarms so the athan rings at the exact minute.",
                "⚠ اسمح بالمنبهات الدقيقة ليرنّ الأذان في الدقيقة المحددة.");
        put("Fix", "إصلاح");

        // ---------------- activities ----------------
        put("Choose your city", "اختر مدينتك");
        put("📍 Enter coordinates manually", "📍 أدخل الإحداثيات يدوياً");
        put("Search city…", "ابحث عن مدينة…");
        put("Latitude  (e.g. 32.90)", "خط العرض  (مثال 32.90)");
        put("Longitude (e.g. 13.18)", "خط الطول (مثال 13.18)");
        put("Custom location", "موقع مخصص");
        put("Save", "حفظ");
        put("Cancel", "إلغاء");
        put("Location saved ✓", "تم حفظ الموقع ✓");
        put("Enter valid coordinates", "أدخل إحداثيات صحيحة");
        put("No file picker on this device", "لا يوجد منتقي ملفات على هذا الجهاز");
        put("Fine-tune prayer times", "ضبط أوقات الصلاة");
        put("Shift any prayer a few minutes earlier or later to match your local mosque.",
                "حرّك أي صلاة بضع دقائق قبل أو بعد لتطابق مسجدك المحلي.");
        put("📆 Prayer Timetable", "📆 جدول الصلاة");
        put("Gregorian", "ميلادي");
        put("Hijri", "هجري");
        put("30 days", "٣٠ يوماً");
        put("Day", "اليوم");
        put("🧭 Qibla", "🧭 القبلة");
        put("🛰 satellite view · gold arrow points to the Kaaba · map is saved for offline use",
                "🛰 عرض بالأقمار الصناعية · السهم الذهبي يشير إلى الكعبة · تُحفظ الخريطة للاستخدام دون إنترنت");
        put("🗓️ Islamic Events", "🗓️ المناسبات الإسلامية");
        put("🗓 Islamic Events", "🗓 المناسبات الإسلامية");
        put("EVENT", "المناسبة");
        put("HIJRI DATE", "التاريخ الهجري");
        put("GREGORIAN", "الميلادي");
        put("COUNTDOWN", "المتبقي");
        put("⭐ highlighted row = the next upcoming event", "⭐ الصف المميز هو المناسبة القادمة");
        put("🤲 Duas & Adhkar", "🤲 الأدعية والأذكار");
        put("🤲 After Salah", "🤲 بعد الصلاة");
        put("🌅 Morning adhkar · أذكار الصباح", "🌅 أذكار الصباح");
        put("🌙 Night adhkar · أذكار المساء", "🌙 أذكار المساء");
        put("Press any button to exit", "اضغط أي زر للخروج");
        put("📻 Quran Radio", "📻 إذاعة القرآن");
        put("Tap a station to listen", "اختر محطة للاستماع");
        put("Stop radio", "إيقاف الإذاعة");
        put("Playing", "يعمل");
        put("Connecting…", "جارٍ الاتصال…");
        put("Could not play this station", "تعذّر تشغيل هذه المحطة");
        put("Recitation, around the clock", "تلاوة على مدار الساعة");
        put("Holy Quran Radio", "إذاعة القرآن الكريم");
        put("Live Quran recitation", "تلاوة قرآنية مباشرة");

        // ---------------- athan overlay ----------------
        put("Dismiss", "إلغاء");
        put("Open adhkar", "فتح الأذكار");
        put("Taraweeh prayers", "صلاة التراويح");
        put("Adhkar · open the collection", "أذكار · افتح المجموعة");
        put("🔊 Playing athan…", "🔊 الأذان يعمل…");
        put("🔔 Gentle reminder", "🔔 تذكير لطيف");
        put("🌙 Time for Taraweeh", "🌙 وقت التراويح");
        put("🌅 Morning adhkar", "🌅 أذكار الصباح");
        put("🌙 Evening adhkar", "🌙 أذكار المساء");
        put("Reminder", "تذكير");
        put("Prayer begins shortly", "تبدأ الصلاة بعد قليل");
        put("Tap Dismiss on screen to stop", "اضغط إلغاء على الشاشة للإيقاف");
        put("Tap Dismiss on screen", "اضغط إلغاء على الشاشة");
        put("🌙 Quiet time", "🌙 وقت هادئ");
        put("Athan", "الأذان");
        put("Test athan…", "تجربة الأذان…");
        put("Athan alarms rescheduled ✓", "تمت إعادة جدولة منبهات الأذان ✓");
        put("Could not reschedule", "تعذّرت إعادة الجدولة");
        put("Notifications allowed ✓", "تم السماح بالإشعارات ✓");
        put("Notifications stay blocked", "ستبقى الإشعارات محظورة");
    }

    private static void put(String en, String arabic) {
        M.put(en, arabic);
    }
}
