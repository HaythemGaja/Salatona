package com.leo.salah;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Scanner;

/**
 * Current weather via Open-Meteo (free, no API key). Results are cached for
 * 30 minutes; when offline the last cached reading is reused, so the
 * dashboard and sky keep working with no internet.
 */
public class Weather {

    public static final int CLEAR = 0, PARTLY = 1, CLOUDY = 2, FOG = 3,
            DRIZZLE = 4, RAIN = 5, STORM = 6, SNOW = 7, WIND = 8;

    public static class Info {
        public final String text; // e.g. "☀ 31°C · Clear"
        public final int kind;
        Info(String t, int k) { text = t; kind = k; }
    }

    /** One hourly forecast point. */
    public static class Forecast {
        public final int tempC;
        public final int kind;
        Forecast(int t, int k) { tempC = t; kind = k; }
    }

    public interface Cb { void on(Info info); }

    private static final long MAX_AGE = 30 * 60 * 1000L;
    private static final long HOURLY_MAX_AGE = 3 * 3600 * 1000L;

    /** Always calls back (on a background thread). */
    public static void refresh(Context ctx, Cb cb) {
        new Thread(() -> cb.on(load(ctx))).start();
    }

    private static SharedPreferences sp(Context ctx) {
        return ctx.getSharedPreferences("weather", Context.MODE_PRIVATE);
    }

    private static Info load(Context ctx) {
        SharedPreferences s = sp(ctx);
        Info cached = readCache(s);
        long ts = s.getLong("ts", 0);
        if (cached != null && System.currentTimeMillis() - ts < MAX_AGE) return cached;
        Info fresh = fetch(ctx);
        if (fresh != null) return fresh;
        return cached != null ? cached : new Info("", CLEAR);
    }

    private static Info readCache(SharedPreferences s) {
        if (!s.contains("ts")) return null;
        return build(s.getFloat("temp", 0), s.getInt("code", 0),
                s.getFloat("wind", 0), s.getBoolean("day", true));
    }

    private static Info fetch(Context ctx) {
        HttpURLConnection c = null;
        try {
            Prefs p = new Prefs(ctx);
            String url = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                            + "&current=temperature_2m,weather_code,wind_speed_10m,is_day"
                            + "&hourly=temperature_2m,weather_code"
                            + "&timezone=auto&forecast_days=3",
                    p.lat(), p.lng());
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            StringBuilder sb = new StringBuilder();
            try (Scanner sc = new Scanner(c.getInputStream())) {
                while (sc.hasNextLine()) sb.append(sc.nextLine());
            }
            JSONObject root = new JSONObject(sb.toString());
            JSONObject cur = root.getJSONObject("current");
            float temp = (float) cur.getDouble("temperature_2m");
            int code = cur.getInt("weather_code");
            float wind = (float) cur.getDouble("wind_speed_10m");
            boolean day = cur.optInt("is_day", 1) == 1;
            SharedPreferences.Editor e = sp(ctx).edit()
                    .putFloat("temp", temp).putInt("code", code)
                    .putFloat("wind", wind).putBoolean("day", day)
                    .putLong("ts", System.currentTimeMillis());
            // cache the hourly arrays for per-prayer forecasts
            if (root.has("hourly")) {
                e.putString("hourly", root.getJSONObject("hourly").toString());
                e.putLong("hts", System.currentTimeMillis());
            }
            e.apply();
            return build(temp, code, wind, day);
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /**
     * Forecast for the hour containing timeMs, from the cached hourly data.
     * Synchronous and cheap; null when nothing cached (offline / not fetched).
     */
    public static Forecast hourlyAt(Context ctx, long timeMs) {
        try {
            SharedPreferences s = sp(ctx);
            String js = s.getString("hourly", null);
            if (js == null || System.currentTimeMillis() - s.getLong("hts", 0) > HOURLY_MAX_AGE)
                return null;
            JSONObject h = new JSONObject(js);
            org.json.JSONArray times = h.getJSONArray("time");
            org.json.JSONArray temps = h.getJSONArray("temperature_2m");
            org.json.JSONArray codes = h.getJSONArray("weather_code");
            if (times.length() == 0) return null;
            // hourly entries are "yyyy-MM-dd'T'HH:mm" in the location's tz
            java.text.SimpleDateFormat f =
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
            long first = f.parse(times.getString(0)).getTime();
            int idx = (int) ((timeMs - first) / 3600_000L);
            if (idx < 0 || idx >= times.length()) return null;
            return new Forecast((int) Math.round(temps.getDouble(idx)),
                    kindFor(codes.getInt(idx)));
        } catch (Exception e) {
            return null;
        }
    }

    /** Maps an Open-Meteo weather_code to one of the kind constants. */
    public static int kindFor(int code) {
        switch (code) {
            case 0: case 1: return code == 0 ? CLEAR : PARTLY;
            case 2: return PARTLY;
            case 3: return CLOUDY;
            case 45: case 48: return FOG;
            case 51: case 53: case 55: case 56: case 57: return DRIZZLE;
            case 61: case 63: case 65: case 66: case 67:
            case 80: case 81: case 82: return RAIN;
            case 71: case 73: case 75: case 77: case 85: case 86: return SNOW;
            case 95: case 96: case 99: return STORM;
            default: return CLEAR;
        }
    }

    private static Info build(float temp, int code, float windKmh, boolean day) {
        int kind;
        String icon, label;
        switch (code) {
            case 0:  kind = CLEAR;   icon = day ? "☀" : "🌙"; label = "Clear"; break;
            case 1:  kind = PARTLY;  icon = "🌤"; label = "Mostly clear"; break;
            case 2:  kind = PARTLY;  icon = "⛅"; label = "Partly cloudy"; break;
            case 3:  kind = CLOUDY;  icon = "☁"; label = "Cloudy"; break;
            case 45: case 48:
                kind = FOG;     icon = "🌫"; label = "Foggy"; break;
            case 51: case 53: case 55: case 56: case 57:
                kind = DRIZZLE; icon = "🌦"; label = "Drizzle"; break;
            case 61: case 63: case 65: case 66: case 67:
            case 80: case 81: case 82:
                kind = RAIN;    icon = "🌧"; label = "Rain"; break;
            case 71: case 73: case 75: case 77: case 85: case 86:
                kind = SNOW;    icon = "🌨"; label = "Snow"; break;
            case 95: case 96: case 99:
                kind = STORM;   icon = "⛈"; label = "Storm"; break;
            default:
                kind = CLEAR;   icon = day ? "☀" : "🌙"; label = "Clear"; break;
        }
        if (windKmh >= 30 && (kind == CLEAR || kind == PARTLY || kind == CLOUDY)) {
            kind = WIND; icon = "💨"; label = "Windy";
        }
        return new Info(icon + " " + Math.round(temp) + "°C · " + label, kind);
    }
}
