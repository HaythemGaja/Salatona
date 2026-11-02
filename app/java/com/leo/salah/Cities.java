package com.leo.salah;

/** Built-in city database: "Name|Country|lat|lng". */
public class Cities {
    public static final String[] ALL = {
        // Libya first
        "Tripoli|Libya|32.9003|13.1858",
        "Benghazi|Libya|32.1194|20.0865",
        "Misrata|Libya|32.3754|15.0925",
        "Al Bayda|Libya|32.7561|21.7453",
        "Zawiya|Libya|32.7529|12.7278",
        "Tobruk|Libya|32.0833|23.9667",
        "Zliten|Libya|32.4669|14.5689",
        "Khoms|Libya|32.6486|14.2636",
        "Ajdabiya|Libya|30.7555|20.2263",
        "Derna|Libya|32.7675|22.6367",
        "Sirte|Libya|31.2089|16.5887",
        "Sabha|Libya|27.0377|14.4283",
        "Murzuq|Libya|25.9189|13.9189",
        "Ubari|Libya|26.5885|12.7809",
        "Ghadames|Libya|30.1333|9.5000",
        "Kufra|Libya|24.2167|23.2833",
        "Gharyan|Libya|32.1694|13.0181",
        "Tarhuna|Libya|32.4333|13.6333",
        "Bani Walid|Libya|31.7667|13.9833",
        "Yafran|Libya|32.0631|12.5286",
        // Arab world & Middle East
        "Cairo|Egypt|30.0444|31.2357",
        "Alexandria|Egypt|31.2001|29.9187",
        "Giza|Egypt|30.0131|31.2089",
        "Luxor|Egypt|25.6872|32.6396",
        "Aswan|Egypt|24.0889|32.8998",
        "Riyadh|Saudi Arabia|24.7136|46.6753",
        "Jeddah|Saudi Arabia|21.4858|39.1925",
        "Makkah|Saudi Arabia|21.4225|39.8262",
        "Madinah|Saudi Arabia|24.5247|39.5692",
        "Dammam|Saudi Arabia|26.4207|50.0888",
        "Dubai|UAE|25.2048|55.2708",
        "Abu Dhabi|UAE|24.4539|54.3773",
        "Sharjah|UAE|25.3463|55.4209",
        "Doha|Qatar|25.2854|51.5310",
        "Kuwait City|Kuwait|29.3759|47.9774",
        "Manama|Bahrain|26.2285|50.5860",
        "Muscat|Oman|23.5880|58.3829",
        "Amman|Jordan|31.9539|35.9106",
        "Jerusalem|Palestine|31.7683|35.2137",
        "Beirut|Lebanon|33.8938|35.5018",
        "Damascus|Syria|33.5138|36.2765",
        "Baghdad|Iraq|33.3152|44.3661",
        "Basra|Iraq|30.5085|47.7804",
        "Erbil|Iraq|36.1911|44.0092",
        "Tehran|Iran|35.6892|51.3890",
        "Istanbul|Turkey|41.0082|28.9784",
        "Ankara|Turkey|39.9334|32.8597",
        "Izmir|Turkey|38.4237|27.1428",
        "Bursa|Turkey|40.1950|29.0601",
        "Antalya|Turkey|36.8841|30.7056",
        "Konya|Turkey|37.8667|32.4833",
        "Sanaa|Yemen|15.3694|44.1910",
        "Aden|Yemen|12.7855|45.0187",
        "Khartoum|Sudan|15.5007|32.5599",
        "Omdurman|Sudan|15.6445|32.4778",
        "Tunis|Tunisia|36.8065|10.1815",
        "Sfax|Tunisia|34.7406|10.7603",
        "Algiers|Algeria|36.7538|3.0588",
        "Oran|Algeria|35.6969|-0.6331",
        "Constantine|Algeria|36.3650|6.6147",
        "Casablanca|Morocco|33.5731|-7.5898",
        "Rabat|Morocco|34.0209|-6.8416",
        "Marrakech|Morocco|31.6295|-7.9811",
        "Fes|Morocco|34.0181|-5.0078",
        "Tangier|Morocco|35.7595|-5.8340",
        "Nouakchott|Mauritania|18.0858|-15.9785",
        "Djibouti|Djibouti|11.5721|43.1456",
        "Mogadishu|Somalia|2.0469|45.3182",
        // South & SE Asia
        "Islamabad|Pakistan|33.6844|73.0479",
        "Karachi|Pakistan|24.8607|67.0011",
        "Lahore|Pakistan|31.5204|74.3587",
        "Dhaka|Bangladesh|23.8103|90.4125",
        "Mumbai|India|19.0760|72.8777",
        "Delhi|India|28.6139|77.2090",
        "Hyderabad|India|17.3850|78.4867",
        "Jakarta|Indonesia|6.2088|106.8456",
        "Kuala Lumpur|Malaysia|3.1390|101.6869",
        "Singapore|Singapore|1.3521|103.8198",
        "Bangkok|Thailand|13.7563|100.5018",
        "Manila|Philippines|14.5995|120.9842",
        // Europe
        "London|UK|51.5074|-0.1278",
        "Manchester|UK|53.4808|-2.2426",
        "Birmingham|UK|52.4862|-1.8904",
        "Glasgow|UK|55.8642|-4.2518",
        "Dublin|Ireland|53.3498|-6.2603",
        "Paris|France|48.8566|2.3522",
        "Marseille|France|43.2965|5.3698",
        "Berlin|Germany|52.5200|13.4050",
        "Hamburg|Germany|53.5511|9.9937",
        "Munich|Germany|48.1351|11.5820",
        "Madrid|Spain|40.4168|-3.7038",
        "Barcelona|Spain|41.3874|2.1686",
        "Rome|Italy|41.9028|12.4964",
        "Milan|Italy|45.4642|9.1900",
        "Amsterdam|Netherlands|52.3676|4.9041",
        "Brussels|Belgium|50.8503|4.3517",
        "Vienna|Austria|48.2082|16.3738",
        "Zurich|Switzerland|47.3769|8.5417",
        "Stockholm|Sweden|59.3293|18.0686",
        "Oslo|Norway|59.9139|10.7522",
        "Copenhagen|Denmark|55.6761|12.5683",
        "Helsinki|Finland|60.1699|24.9384",
        "Warsaw|Poland|52.2297|21.0122",
        "Prague|Czechia|50.0755|14.4378",
        "Budapest|Hungary|47.4979|19.0402",
        "Athens|Greece|37.9838|23.7275",
        "Lisbon|Portugal|38.7223|-9.1393",
        "Sarajevo|Bosnia|43.8563|18.4131",
        // North America
        "New York|USA|40.7128|-74.0060",
        "Los Angeles|USA|34.0522|-118.2437",
        "Chicago|USA|41.8781|-87.6298",
        "Houston|USA|29.7604|-95.3698",
        "Miami|USA|25.7617|-80.1918",
        "San Francisco|USA|37.7749|-122.4194",
        "Dearborn|USA|42.3223|-83.1763",
        "Toronto|Canada|43.6532|-79.3832",
        "Montreal|Canada|45.5017|-73.5673",
        "Vancouver|Canada|49.2827|-123.1207",
        "Mexico City|Mexico|19.4326|-99.1332",
        // USA
        "Washington DC|USA|38.9072|-77.0369",
        "Philadelphia|USA|39.9526|-75.1652",
        "Boston|USA|42.3601|-71.0589",
        "Baltimore|USA|39.2904|-76.6122",
        "Newark|USA|40.7357|-74.1724",
        "Buffalo|USA|42.8864|-78.8784",
        "Albany|USA|42.6526|-73.7562",
        "Hartford|USA|41.7658|-72.6734",
        "Providence|USA|41.8240|-71.4128",
        "Pittsburgh|USA|40.4406|-79.9959",
        "Atlanta|USA|33.7490|-84.3880",
        "Charlotte|USA|35.2271|-80.8431",
        "Raleigh|USA|35.7796|-78.6382",
        "Nashville|USA|36.1627|-86.7816",
        "Memphis|USA|35.1495|-90.0490",
        "Knoxville|USA|35.9606|-83.9207",
        "New Orleans|USA|29.9511|-90.0715",
        "Baton Rouge|USA|30.4515|-91.1871",
        "Birmingham|USA|33.5207|-86.8025",
        "Jackson|USA|32.2988|-90.1848",
        "Louisville|USA|38.2527|-85.7585",
        "Charleston WV|USA|38.3498|-81.6326",
        "Richmond|USA|37.5407|-77.4360",
        "Virginia Beach|USA|36.8529|-75.9780",
        "Orlando|USA|28.5383|-81.3792",
        "Tampa|USA|27.9506|-82.4572",
        "Jacksonville|USA|30.3322|-81.6557",
        "San Diego|USA|32.7157|-117.1611",
        "San Jose|USA|37.3382|-121.8863",
        "Sacramento|USA|38.5816|-121.4944",
        "Fresno|USA|36.7378|-119.7871",
        "Oakland|USA|37.8044|-122.2711",
        "Phoenix|USA|33.4484|-112.0740",
        "Tucson|USA|32.2226|-110.9747",
        "Las Vegas|USA|36.1699|-115.1398",
        "Reno|USA|39.5296|-119.8138",
        "Salt Lake City|USA|40.7608|-111.8910",
        "Denver|USA|39.7392|-104.9903",
        "Albuquerque|USA|35.0844|-106.6504",
        "Dallas|USA|32.7767|-96.7970",
        "Fort Worth|USA|32.7555|-97.3308",
        "San Antonio|USA|29.4241|-98.4936",
        "Austin|USA|30.2672|-97.7431",
        "El Paso|USA|31.7619|-106.4850",
        "Oklahoma City|USA|35.4676|-97.5164",
        "Little Rock|USA|34.7465|-92.2896",
        "Wichita|USA|37.6872|-97.3301",
        "Omaha|USA|41.2565|-95.9345",
        "Des Moines|USA|41.5868|-93.6250",
        "Kansas City|USA|39.0997|-94.5786",
        "St. Louis|USA|38.6270|-90.1994",
        "Minneapolis|USA|44.9778|-93.2650",
        "Milwaukee|USA|43.0389|-87.9065",
        "Indianapolis|USA|39.7684|-86.1581",
        "Columbus|USA|39.9612|-82.9988",
        "Cincinnati|USA|39.1031|-84.5120",
        "Cleveland|USA|41.4993|-81.6944",
        "Detroit|USA|42.3314|-83.0458",
        "Seattle|USA|47.6062|-122.3321",
        "Spokane|USA|47.6588|-117.4260",
        "Portland OR|USA|45.5152|-122.6784",
        "Boise|USA|43.6150|-116.2023",
        "Honolulu|USA|21.3099|-157.8581",
        "Anchorage|USA|61.2181|-149.9003",
        "Fargo|USA|46.8772|-96.7898",
        "Sioux Falls|USA|43.5460|-96.7313",
        "Billings|USA|45.7833|-108.5007",
        "Cheyenne|USA|41.1400|-104.8202",
        "Burlington|USA|44.4759|-73.2121",
        "Manchester NH|USA|42.9956|-71.4548",
        "Portland ME|USA|43.6615|-70.2553",
        "Wilmington DE|USA|39.7459|-75.5466",
        "Charleston SC|USA|32.7765|-79.9311",
        "Columbia SC|USA|34.0007|-81.0348",
        "Bloomington IN|USA|39.1653|-86.5264",
        // Africa
        "Lagos|Nigeria|6.5244|3.3792",
        "Abuja|Nigeria|9.0765|7.3986",
        "Nairobi|Kenya|-1.2921|36.8219",
        "Addis Ababa|Ethiopia|9.0054|38.7636",
        "Johannesburg|South Africa|-26.2041|28.0473",
        "Cape Town|South Africa|-33.9249|18.4241",
        "Dakar|Senegal|14.7167|-17.4677",
        "Accra|Ghana|5.6037|-0.1870",
        "Bamako|Mali|12.6392|-8.0029",
        "Niamey|Niger|13.5116|2.1254",
        "Kampala|Uganda|0.3476|32.5825",
        // East Asia & Oceania
        "Hong Kong|China|22.3193|114.1694",
        "Shanghai|China|31.2304|121.4737",
        "Beijing|China|39.9042|116.4074",
        "Tokyo|Japan|35.6762|139.6503",
        "Seoul|South Korea|37.5665|126.9780",
        "Sydney|Australia|-33.8688|151.2093",
        "Melbourne|Australia|-37.8136|144.9631",
        "Perth|Australia|-31.9505|115.8605",
        "Auckland|New Zealand|-36.8485|174.7633",
    };

    public static String name(String e) { return e.split("\\|")[0]; }
    public static String country(String e) { return e.split("\\|")[1]; }
    public static double lat(String e) { return Double.parseDouble(e.split("\\|")[2]); }
    public static double lng(String e) { return Double.parseDouble(e.split("\\|")[3]); }

    /** The method most mosques in that country follow; picked when a city changes. */
    public static int defaultMethod(String country) {
        switch (country) {
            case "Saudi Arabia": return PrayerTimes.MAKKAH;
            case "Egypt": return PrayerTimes.EGYPT;
            case "UAE": return PrayerTimes.DUBAI;
            case "Kuwait": return PrayerTimes.KUWAIT;
            case "Qatar": return PrayerTimes.QATAR;
            case "Singapore": return PrayerTimes.SINGAPORE;
            case "Turkey": return PrayerTimes.TURKEY;
            case "Oman": return PrayerTimes.OMAN;
            case "Morocco": return PrayerTimes.MOROCCO;
            case "Pakistan": case "India": case "Bangladesh":
            case "Afghanistan": return PrayerTimes.KARACHI;
            case "USA": case "Canada": return PrayerTimes.ISNA;
            case "Iran": return PrayerTimes.JAFARI;
            default: return PrayerTimes.MWL;
        }
    }

    /** Hanafi Asr for the countries where it is the usual school. */
    public static int defaultAsr(String country) {
        switch (country) {
            case "Pakistan": case "India": case "Bangladesh":
            case "Afghanistan": return 1;
            default: return 0;
        }
    }

    /** Arabic display name for the common cities; falls back to the English name. */
    public static String nameAr(String e) {
        String n = name(e);
        switch (n) {
            case "Tripoli": return "طرابلس";
            case "Benghazi": return "بنغازي";
            case "Misrata": return "مصراتة";
            case "Al Bayda": return "البيضاء";
            case "Zawiya": return "الزاوية";
            case "Tobruk": return "طبرق";
            case "Zliten": return "زليتن";
            case "Khoms": return "الخمس";
            case "Ajdabiya": return "أجدابيا";
            case "Derna": return "درنة";
            case "Sirte": return "سرت";
            case "Sabha": return "سبها";
            case "Murzuq": return "مرزق";
            case "Ubari": return "أوباري";
            case "Ghadames": return "غدامس";
            case "Kufra": return "الكفرة";
            case "Gharyan": return "غريان";
            case "Tarhuna": return "ترهونة";
            case "Bani Walid": return "بني وليد";
            case "Yafran": return "يفرن";
            case "Cairo": return "القاهرة";
            case "Alexandria": return "الإسكندرية";
            case "Giza": return "الجيزة";
            case "Makkah": return "مكة المكرمة";
            case "Madinah": return "المدينة المنورة";
            case "Riyadh": return "الرياض";
            case "Jeddah": return "جدة";
            case "Dubai": return "دبي";
            case "Abu Dhabi": return "أبو ظبي";
            case "Doha": return "الدوحة";
            case "Kuwait City": return "الكويت";
            case "Amman": return "عمّان";
            case "Jerusalem": return "القدس";
            case "Beirut": return "بيروت";
            case "Damascus": return "دمشق";
            case "Baghdad": return "بغداد";
            case "Tunis": return "تونس";
            case "Algiers": return "الجزائر";
            case "Casablanca": return "الدار البيضاء";
            case "Rabat": return "الرباط";
            case "Istanbul": return "إسطنبول";
            default: return n;
        }
    }

    /** Arabic country name for the common countries. */
    public static String countryAr(String country) {
        switch (country) {
            case "Libya": return "ليبيا";
            case "Egypt": return "مصر";
            case "Saudi Arabia": return "السعودية";
            case "UAE": return "الإمارات";
            case "Qatar": return "قطر";
            case "Kuwait": return "الكويت";
            case "Bahrain": return "البحرين";
            case "Oman": return "عُمان";
            case "Jordan": return "الأردن";
            case "Palestine": return "فلسطين";
            case "Lebanon": return "لبنان";
            case "Syria": return "سوريا";
            case "Iraq": return "العراق";
            case "Yemen": return "اليمن";
            case "Sudan": return "السودان";
            case "Tunisia": return "تونس";
            case "Algeria": return "الجزائر";
            case "Morocco": return "المغرب";
            case "Turkey": return "تركيا";
            case "Iran": return "إيران";
            case "Pakistan": return "باكستان";
            case "India": return "الهند";
            case "Bangladesh": return "بنغلاديش";
            case "Indonesia": return "إندونيسيا";
            case "Malaysia": return "ماليزيا";
            case "USA": return "الولايات المتحدة";
            case "UK": return "المملكة المتحدة";
            case "France": return "فرنسا";
            case "Germany": return "ألمانيا";
            case "Canada": return "كندا";
            case "Australia": return "أستراليا";
            default: return country;
        }
    }
}
