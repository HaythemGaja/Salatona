package com.leo.salah;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import java.util.Random;

import static com.leo.salah.Weather.CLEAR;
import static com.leo.salah.Weather.CLOUDY;
import static com.leo.salah.Weather.DRIZZLE;
import static com.leo.salah.Weather.FOG;
import static com.leo.salah.Weather.PARTLY;
import static com.leo.salah.Weather.RAIN;
import static com.leo.salah.Weather.SNOW;
import static com.leo.salah.Weather.STORM;
import static com.leo.salah.Weather.WIND;

/**
 * Prayer-driven sky. The scene follows the CURRENT prayer, not the clock:
 * Fajr = dawn with a rising sun, Dhuhr = high midday sun, Asr = golden
 * afternoon with a descending sun, Maghrib = sunset, Isha = night with
 * moon and stars. The sun/moon travel as the prayer period progresses.
 *
 * Live weather overlays (clouds, rain, snow, fog, wind streaks, lightning)
 * come from the cached Open-Meteo feed via setWeatherKind().
 */
public class SkyView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private float[] sx, sy, sr, sa;       // stars
    private float[] cx, cy, cs;           // clouds (x, y, scale)
    private float[] dx, dy, ds;           // rain/snow drops (x, y, speed)
    private float[] wx, wy;               // wind streaks
    private Path farMountains, nearMountains;
    private int w, h;

    private int prayer = 4;               // current prayer 0..4
    private float progress;               // 0..1 through the prayer period
    private int weatherKind = CLEAR;
    private int flash;                    // lightning frames remaining
    private long frame;                   // animation frame counter
    private final Random animRnd = new Random();

    public SkyView(Context c) {
        super(c);
    }

    /** Current prayer index (0 Fajr .. 4 Isha) + 0..1 progress to next prayer. */
    public void setPhase(int prayerIdx, float prog) {
        prayer = Math.max(0, Math.min(4, prayerIdx));
        progress = Math.max(0f, Math.min(1f, prog));
        invalidate();
    }

    public void setWeatherKind(int kind) {
        if (kind != weatherKind) {
            weatherKind = kind;
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        this.w = w;
        this.h = h;
        Random rnd = new Random(42);
        int n = 140;
        sx = new float[n]; sy = new float[n]; sr = new float[n]; sa = new float[n];
        for (int i = 0; i < n; i++) {
            sx[i] = rnd.nextFloat() * w;
            sy[i] = rnd.nextFloat() * h * 0.62f;
            sr[i] = 1 + rnd.nextFloat() * 2.4f;
            sa[i] = 90 + rnd.nextInt(165);
        }
        cx = new float[7]; cy = new float[7]; cs = new float[7];
        for (int i = 0; i < 7; i++) {
            cx[i] = rnd.nextFloat() * (w + 400) - 200;
            cy[i] = h * (0.06f + rnd.nextFloat() * 0.30f);
            cs[i] = 0.7f + rnd.nextFloat() * 0.9f;
        }
        dx = new float[90]; dy = new float[90]; ds = new float[90];
        for (int i = 0; i < 90; i++) resetDrop(i, rnd, true);
        wx = new float[12]; wy = new float[12];
        for (int i = 0; i < 12; i++) {
            wx[i] = rnd.nextFloat() * w;
            wy[i] = rnd.nextFloat() * h * 0.7f;
        }
        farMountains = ridge(rnd, h * 0.74f, h * 0.10f, 7);
        nearMountains = ridge(new Random(99), h * 0.84f, h * 0.12f, 5);
    }

    private void resetDrop(int i, Random rnd, boolean anywhere) {
        dx[i] = rnd.nextFloat() * (w + 100);
        dy[i] = anywhere ? rnd.nextFloat() * h : -30 - rnd.nextFloat() * 120;
        ds[i] = 0.7f + rnd.nextFloat() * 0.8f;
    }

    private Path ridge(Random rnd, float baseY, float amp, int peaks) {
        Path p = new Path();
        p.moveTo(0, baseY + rnd.nextFloat() * amp * 0.3f);
        float step = (float) w / peaks;
        for (int i = 0; i < peaks; i++) {
            float x = step * (i + 1);
            float y = baseY - rnd.nextFloat() * amp;
            p.lineTo(x - step / 2, y);
            p.lineTo(x, baseY - rnd.nextFloat() * amp * 0.25f);
        }
        p.lineTo(w, baseY);
        p.lineTo(w, h);
        p.lineTo(0, h);
        p.close();
        return p;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (w == 0) return;
        frame++;
        boolean night = prayer == 4;
        switch (prayer) {
            case 0: drawDawnScene(canvas); break;
            case 1: drawDayScene(canvas, false); break;
            case 2: drawDayScene(canvas, true); break;
            case 3: drawDuskScene(canvas); break;
            default: drawNightScene(canvas); break;
        }
        drawClouds(canvas, night);
        mountains(canvas, mountainFar(), mountainNear());
        switch (weatherKind) {
            case DRIZZLE: drawRain(canvas, 40, 10, false); break;
            case RAIN: drawRain(canvas, 90, 22, false); break;
            case STORM: drawRain(canvas, 90, 26, true); break;
            case SNOW: drawSnow(canvas); break;
            case FOG: drawFog(canvas); break;
            case WIND: drawWind(canvas); break;
            default: break;
        }
        if (animated()) postInvalidateDelayed(140);
    }

    private boolean animated() {
        return weatherKind == DRIZZLE || weatherKind == RAIN
                || weatherKind == STORM || weatherKind == SNOW
                || weatherKind == FOG || weatherKind == WIND;
    }

    private int mountainFar() {
        switch (prayer) {
            case 0: return 0xFF241543;
            case 1: return 0xFF3E6B8A;
            case 2: return 0xFF4A4A55;
            case 3: return 0xFF1E1440;
            default: return 0xFF0A1440;
        }
    }

    private int mountainNear() {
        switch (prayer) {
            case 0: return 0xFF160D2E;
            case 1: return 0xFF2A4A5E;
            case 2: return 0xFF2E2E3A;
            case 3: return 0xFF100B28;
            default: return 0xFF060C28;
        }
    }

    // ---------------- scenes ----------------

    private void drawNightScene(Canvas canvas) {
        gradient(canvas, new int[]{0xFF060B2E, 0xFF0B1E5B, 0xFF1E5AA8, 0xFF3E8FD4},
                new float[]{0f, 0.45f, 0.75f, 1f});
        // moon drifts slowly west as the night progresses
        float mx = w * (0.82f - 0.10f * progress);
        float my = h * (0.16f + 0.05f * (float) Math.sin(progress * Math.PI));
        glow(canvas, mx, my, h * 0.22f, 0x55FFF8E1);
        // phase-accurate moon, not a generic full disc
        MoonDraw.draw(canvas, mx, my, h * 0.035f, PrayerCalc.moonPhaseContinuous(),
                0xFFFFF8E1, 0xFF141B3D);
        stars(canvas, 1.0f);
    }

    private void drawDawnScene(Canvas canvas) {
        gradient(canvas, new int[]{0xFF2B1B4E, 0xFF8E3A6E, 0xFFFF9E5E, 0xFFFFD08A},
                new float[]{0f, 0.45f, 0.75f, 1f});
        // sun rises through the Fajr period
        float x = w * (0.30f + 0.20f * progress);
        float y = h * (0.70f - 0.24f * progress);
        glow(canvas, x, y, h * 0.30f, 0x99FFB45E);
        paint.setColor(0xFFFFE0A3);
        canvas.drawCircle(x, y, h * 0.05f, paint);
        stars(canvas, 0.35f * (1 - progress));
    }

    /** warm=false: Dhuhr midday sun; warm=true: Asr golden descent. */
    private void drawDayScene(Canvas canvas, boolean warm) {
        if (!warm) {
            gradient(canvas, new int[]{0xFF2E7BD6, 0xFF5EA9E8, 0xFF9FD4F5, 0xFFCDEBFA},
                    new float[]{0f, 0.45f, 0.75f, 1f});
        } else {
            gradient(canvas, new int[]{0xFF3A6EA8, 0xFF7FA8C8, 0xFFF0B46A, 0xFFFFD9A0},
                    new float[]{0f, 0.45f, 0.75f, 1f});
        }
        float x, y;
        if (!warm) { // Dhuhr -> Asr: high sun drifting west
            x = w * (0.40f + 0.32f * progress);
            y = h * (0.14f + 0.12f * progress);
        } else {     // Asr -> Maghrib: golden descent toward the horizon
            x = w * (0.66f + 0.20f * progress);
            y = h * (0.28f + 0.30f * progress);
        }
        glow(canvas, x, y, h * 0.26f, warm ? 0x99FFC46A : 0x88FFF3B0);
        paint.setColor(warm ? 0xFFFFD98A : 0xFFFFF176);
        canvas.drawCircle(x, y, h * 0.055f, paint);
    }

    private void drawDuskScene(Canvas canvas) {
        gradient(canvas, new int[]{0xFF1A1B4E, 0xFF6E2A6E, 0xFFFF7E4A, 0xFFFFB45E},
                new float[]{0f, 0.45f, 0.75f, 1f});
        // sun sinks below the horizon through the Maghrib period
        float x = w * 0.5f;
        float y = h * (0.58f + 0.22f * progress);
        int glowAlpha = (int) (0x99 * (1 - progress * 0.7f));
        glow(canvas, x, y, h * 0.30f, (glowAlpha << 24) | 0x00FF8E4A);
        paint.setColor(0xFFFFD08A);
        paint.setAlpha((int) (255 * (1 - progress * 0.85f)));
        canvas.drawCircle(x, y, h * 0.05f, paint);
        paint.setAlpha(255);
        stars(canvas, 0.5f * progress);
    }

    // ---------------- weather ----------------

    private void drawClouds(Canvas canvas, boolean night) {
        int count, alpha;
        float speed;
        switch (weatherKind) {
            case CLEAR: count = 2; alpha = 70; speed = 1f; break;
            case PARTLY: count = 4; alpha = 130; speed = 1.5f; break;
            case CLOUDY: count = 7; alpha = 200; speed = 2f; break;
            case DRIZZLE:
            case RAIN:
            case STORM: count = 6; alpha = 215; speed = 2.5f; break;
            case SNOW: count = 5; alpha = 185; speed = 1.5f; break;
            case WIND: count = 3; alpha = 110; speed = 9f; break;
            case FOG: return; // fog draws its own bands
            default: count = 2; alpha = 70; speed = 1f; break;
        }
        int base = night ? 0xFF2A3450 : 0xFFFFFFFF;
        if (weatherKind == RAIN || weatherKind == STORM || weatherKind == DRIZZLE)
            base = night ? 0xFF232B40 : 0xFFB9C4D4;
        paint.setColor(base);
        paint.setAlpha(alpha);
        float r = h * 0.055f;
        for (int i = 0; i < count; i++) {
            cx[i] += speed;
            if (cx[i] - 200 * cs[i] > w) {
                cx[i] = -200 * cs[i];
                cy[i] = h * (0.06f + animRnd.nextFloat() * 0.30f);
            }
            float x = cx[i], y = cy[i], s = cs[i];
            // puffy cloud = 3 overlapping circles
            canvas.drawCircle(x, y, r * s, paint);
            canvas.drawCircle(x - r * 0.9f * s, y + r * 0.25f * s, r * 0.7f * s, paint);
            canvas.drawCircle(x + r * 0.9f * s, y + r * 0.25f * s, r * 0.7f * s, paint);
        }
        paint.setAlpha(255);
    }

    private void drawRain(Canvas canvas, int n, float speed, boolean lightning) {
        paint.setColor(0xFF9FC8E8);
        paint.setAlpha(170);
        paint.setStrokeWidth(Math.max(2, w / 480));
        for (int i = 0; i < n; i++) {
            dy[i] += ds[i] * speed;
            if (dy[i] > h) resetDrop(i, animRnd, false);
            float x = dx[i], y = dy[i];
            canvas.drawLine(x, y, x - 4, y + 20, paint);
        }
        paint.setAlpha(255);
        if (lightning) {
            if (flash > 0) {
                paint.setColor(0xFFFFFFFF);
                paint.setAlpha(90);
                canvas.drawRect(0, 0, w, h, paint);
                paint.setAlpha(255);
                flash--;
            } else if (animRnd.nextFloat() < 0.02) {
                flash = 3;
            }
        }
    }

    private void drawSnow(Canvas canvas) {
        paint.setColor(0xFFFFFFFF);
        for (int i = 0; i < 70; i++) {
            dy[i] += ds[i] * 4;
            dx[i] += (float) Math.sin((dy[i] + i * 37) * 0.05) * 1.5f;
            if (dy[i] > h) resetDrop(i, animRnd, false);
            paint.setAlpha(200);
            canvas.drawCircle(dx[i], dy[i], 2 + ds[i] * 3, paint);
        }
        paint.setAlpha(255);
    }

    private void drawFog(Canvas canvas) {
        paint.setColor(0xFFC9D4E2);
        for (int i = 0; i < 3; i++) {
            float y = h * (0.35f + i * 0.16f);
            float shift = (float) (Math.sin(frame * 0.02 + i * 2.1) * w * 0.05);
            rect.set(-w * 0.1f + shift, y, w * 1.1f + shift, y + h * 0.09f);
            paint.setAlpha(55);
            canvas.drawRoundRect(rect, h * 0.045f, h * 0.045f, paint);
        }
        paint.setAlpha(255);
    }

    private void drawWind(Canvas canvas) {
        paint.setColor(0xFFFFFFFF);
        paint.setAlpha(110);
        paint.setStrokeWidth(Math.max(2, w / 640));
        for (int i = 0; i < wx.length; i++) {
            wx[i] += 26;
            if (wx[i] > w + 140) {
                wx[i] = -140;
                wy[i] = animRnd.nextFloat() * h * 0.7f;
            }
            canvas.drawLine(wx[i], wy[i], wx[i] + 130, wy[i] - 8, paint);
        }
        paint.setAlpha(255);
    }

    // ---------------- helpers ----------------

    private void gradient(Canvas canvas, int[] colors, float[] stops) {
        paint.setShader(new LinearGradient(0, 0, 0, h, colors, stops,
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);
    }

    private void glow(Canvas canvas, float x, float y, float r, int color) {
        paint.setShader(new RadialGradient(x, y, r,
                new int[]{color, 0x00000000}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, r, paint);
        paint.setShader(null);
    }

    private void stars(Canvas canvas, float alphaScale) {
        if (alphaScale <= 0) return;
        for (int i = 0; i < sx.length; i++) {
            paint.setColor(0xFFFFFF);
            paint.setAlpha((int) (sa[i] * alphaScale));
            canvas.drawCircle(sx[i], sy[i], sr[i], paint);
        }
        paint.setAlpha(255);
    }

    private void mountains(Canvas canvas, int far, int near) {
        paint.setColor(far);
        canvas.drawPath(farMountains, paint);
        paint.setColor(near);
        canvas.drawPath(nearMountains, paint);
    }
}
