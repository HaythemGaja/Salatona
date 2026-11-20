import com.leo.salah.*;
import android.content.Context;
import java.time.*;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.util.*;

public class CoreRegressionTest {
    private static int checks;
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        checks++;
    }
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.US);
        Context c = new Context();
        Prefs p = new Prefs(c);
        check(!p.expandedRamadan(), "Expanded Ramadan must default off");
        p.setExpandedRamadan(true);
        check(new Prefs(c).expandedRamadan(), "Expanded Ramadan persists");
        p.setExpandedRamadan(false);
        check(!new Prefs(c).expandedRamadan(), "Expanded Ramadan switches off");
        p.setLanguage(1); p.setDigits(0); Lang.init(c);
        check(Lang.rtl(), "Arabic is RTL");
        check(Lang.t("🕌 العشاء in ٠١:٢٣").equals("🕌 العشاء بعد ٠١:٢٣"),
                "Countdown translates when digits are already Arabic");
        check(Lang.t("⏳ الظهر began ٠١:٠٥ ago").equals("⏳ بدأ الظهر منذ ٠١:٠٥"),
                "Elapsed text translates");
        check(Lang.t("Expanded Ramadan mode").equals("وضع رمضان الموسّع"), "Ramadan translation");
        p.setDigits(2); Lang.init(c);
        check(Lang.num("١٢:٣٤ ۵۶").equals("12:34 56"), "Western digits override Arabic/Persian digits");
        check(!Lang.clock(new Date(), true).matches(".*[٠-٩].*"), "Clock respects Western digit preference");
        p.setDigits(1); Lang.init(c);
        check(Lang.num("12:34").equals("١٢:٣٤"), "Arabic digits");
        p.setLanguage(2); p.setDigits(0); Lang.init(c);
        check(!Lang.rtl(), "English restores LTR");
        check(Lang.t("Language").equals("Language"), "English restores labels");
        check(Lang.num("١٢:٣٤").equals("12:34"), "English restores Western digits");
        String[] english={"Fajr","Dhuhr","Asr","Maghrib","Isha"};
        String[] arabic={"الفجر","الظهر","العصر","المغرب","العشاء"};
        for (int language=1; language<=2; language++) {
            p.setLanguage(language); Lang.init(c);
            for (int i=0;i<5;i++) {
                String name=PrayerCalc.name(c,i);
                check(name.equals((language==1?arabic:english)[i]) ||
                    (i==1 && PrayerCalc.isJumuah(c,System.currentTimeMillis()) && name.equals(Lang.t("Jumu'ah"))),
                    "Five-prayer index maps to timeline index: "+i+" language "+language);
            }
        }
        long fajr=1000000, maghrib=fajr+12*3600000L;
        check(!RamadanWindow.isFasting(fajr-1,fajr,maghrib), "Before Fajr");
        check(RamadanWindow.isFasting(fajr,fajr,maghrib), "At Fajr");
        check(RamadanWindow.progress(fajr+6*3600000L,fajr,maghrib)==500, "Halfway progress");
        check(!RamadanWindow.isFasting(maghrib,fajr,maghrib), "At Iftar fasting ends");
        check(RamadanWindow.progress(maghrib+1,fajr,maghrib)==1000, "Progress capped");
        check(RamadanWindow.progress(fajr-1,fajr,maghrib)==0, "Progress never negative");
        check(!RamadanWindow.isFasting(fajr,fajr,fajr) && RamadanWindow.progress(fajr,fajr,fajr)==0,
            "Invalid window handled without division by zero");
        LocalDate start=LocalDate.from(HijrahDate.of(1448,9,1));
        int length=HijrahDate.of(1448,9,1).lengthOfMonth();
        LocalDate last=start.plusDays(length-1);
        check(RamadanWindow.tomorrowIsRamadan(start,0), "Next Ramadan day");
        check(RamadanWindow.tomorrowIsRamadan(start.minusDays(1),0), "First Suhoor boundary");
        check(!RamadanWindow.tomorrowIsRamadan(last,0), "No Suhoor for Eid");
        check(RamadanWindow.tomorrowIsRamadan(last,-1), "Negative local sighting offset");
        check(!RamadanWindow.tomorrowIsRamadan(last.minusDays(1),1), "Positive local sighting offset");
        ZoneId ny=ZoneId.of("America/New_York");
        long a=ZonedDateTime.of(2027,3,14,1,30,0,0,ny).toInstant().toEpochMilli();
        long b=ZonedDateTime.of(2027,3,14,4,30,0,0,ny).toInstant().toEpochMilli();
        check(b-a==2*3600000L && RamadanWindow.progress(a+3600000L,a,b)==500,
            "DST uses elapsed time rather than wall-clock subtraction");
        System.out.println(checks+" language, prayer-name and Ramadan regression checks passed");
    }
}
