package com.leo.salah;

/** Live Quran radio stations and 24/7 reciter channels. */
public final class Radio {

    public static final int OFF = 0, CONNECTING = 1, PLAYING = 2, ERROR = 3;

    /** Updated by {@link RadioPlayer}; the activity polls it. */
    public static volatile int state = OFF;
    public static volatile String playingId = "";
    public static volatile String error = "";

    public static final class Station {
        public final String id, ar, en, whereAr, whereEn, url;
        public final boolean reciter;

        Station(String id, String ar, String en, String whereAr, String whereEn,
                String url, boolean reciter) {
            this.id = id;
            this.ar = ar;
            this.en = en;
            this.whereAr = whereAr;
            this.whereEn = whereEn;
            this.url = url;
            this.reciter = reciter;
        }
    }

    private static final String QURAN_AR = "إذاعة القرآن الكريم";
    private static final String QURAN_EN = "Holy Quran Radio";

    public static final Station[] STATIONS = {
            new Station("cairo", QURAN_AR, QURAN_EN, "القاهرة · 98.2 إف إم", "Cairo · 98.2 FM",
                    "https://stream.radiojar.com/8s5u5tpdtwzuv", false),
            new Station("nablus", QURAN_AR, QURAN_EN, "نابلس", "Nablus",
                    "https://quran-radio.org:8899/;", false),
            new Station("shabab", "شباب إف إم · قرآن", "Shabab FM Quran", "نابلس", "Nablus",
                    "https://streaming.zaytonatube.com/ShababFM.aac", false),
            new Station("qatar", QURAN_AR, QURAN_EN, "قطر", "Qatar",
                    "https://live.kwikmotion.com/qmcquranradiolive/quranradio/playlist.m3u8", false),
            new Station("qatar2", QURAN_AR + " 2", QURAN_EN + " 2", "قطر", "Qatar",
                    "https://live.kwikmotion.com/qmcquranradio2live/quranradio2/playlist.m3u8", false),
            new Station("oman", QURAN_AR, QURAN_EN, "سلطنة عُمان · مسقط", "Oman · Muscat",
                    "https://partwota.cdn.mgmlcdn.com/quranrdoorg/quranrdo.stream_aac/chunklist.m3u8", false),
            new Station("jordan", QURAN_AR, QURAN_EN, "الأردن", "Jordan",
                    "https://jrtv-live.ercdn.net/jrradio/quranradio.m3u8", false),
            new Station("ksa_quran", QURAN_AR, QURAN_EN, "السعودية", "Saudi Arabia",
                    "https://live.kwikmotion.com/sbrksaquranradiolive/ksaquranradio/playlist.m3u8", false),
            new Station("ksa_nida", "إذاعة نداء الإسلام", "Nida Al-Islam Radio", "السعودية", "Saudi Arabia",
                    "https://live.kwikmotion.com/sbrksanedaradiolive/ksanedaradio/playlist.m3u8", false),
            reciter("mishary_alafasi", "مشاري العفاسي", "Mishary Alafasy"),
            reciter("abdulbasit_abdulsamad", "عبد الباسط عبد الصمد", "Abdul Basit Abdul Samad"),
            reciter("mohammed_siddiq_alminshawi", "محمد صديق المنشاوي", "Mohamed Siddiq Al-Minshawi"),
            reciter("mahmoud_khalil_alhussary", "محمود خليل الحصري", "Mahmoud Khalil Al-Husary"),
            reciter("maher", "ماهر المعيقلي", "Maher Al-Muaiqly"),
            reciter("saad_alghamdi", "سعد الغامدي", "Saad Al-Ghamdi"),
            reciter("abdulrahman_alsudaes", "عبد الرحمن السديس", "Abdul Rahman Al-Sudais"),
            reciter("yasser_aldosari", "ياسر الدوسري", "Yasser Al-Dosari"),
            reciter("nasser_alqatami", "ناصر القطامي", "Nasser Al-Qatami"),
            reciter("mohammad_rashad_alshareef", "محمد رشاد الشريف", "Mohammad Rashad Al-Shareef"),
    };

    private static Station reciter(String id, String ar, String en) {
        return new Station(id, ar, en, "تلاوة على مدار الساعة", "Recitation, around the clock",
                "https://backup.qurango.net/radio/" + id, true);
    }

    public static Station byId(String id) {
        for (Station s : STATIONS) if (s.id.equals(id)) return s;
        return STATIONS[0];
    }

    public static String name(Station s) {
        return Lang.ar() ? s.ar : s.en;
    }

    public static String where(Station s) {
        return Lang.ar() ? s.whereAr : s.whereEn;
    }

    private Radio() {}
}
