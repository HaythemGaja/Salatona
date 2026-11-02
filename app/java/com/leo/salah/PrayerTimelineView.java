package com.leo.salah;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/**
 * Full-day prayer timeline: one horizontal line for the whole salah day.
 * The day runs from Islamic midnight to Islamic midnight — midnight in salah
 * is half the night, halfway between Isha and Fajr. Eight anchors share the
 * line evenly (midnight, six prayers, midnight); the progress fill advances
 * by real time within each segment. A prayer lights up gold only while it is
 * "on": from its time until 30 minutes after, then the highlight is removed.
 * Each node shows the weather forecast for its hour underneath. All icons
 * are drawn with canvas primitives (no emoji, no assets) so they render on
 * any firmware. Pure display — no interaction.
 */
public class PrayerTimelineView extends View {

    private static final String[] NAMES =
            {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    private String[] names = NAMES;
    private static final int N = 6;

    // the salah day the nodes belong to: islamic midnight -> islamic midnight
    private long winStartMs, winEndMs;
    /** A prayer stays highlighted for this long after its time. */
    private static final long HIGHLIGHT_MS = 30 * 60_000L;

    private final long[] nodeMs = new long[N];
    private boolean hasTimes;
    private long now;
    private int nextIdx = -1; // 0..6, or -1 when the window is done

    // per-node forecast; Integer.MIN_VALUE / -1 = none
    private final int[] fcTemp = new int[N];
    private final int[] fcKind = new int[N];

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    public PrayerTimelineView(Context c) {
        super(c);
        for (int i = 0; i < N; i++) { fcTemp[i] = Integer.MIN_VALUE; fcKind[i] = -1; }
    }

    public PrayerTimelineView(Context c, AttributeSet a) {
        this(c);
    }

    /**
     * The six node times in millis (Fajr .. Isha) plus the salah-day window
     * they belong to: islamic midnight -> islamic midnight.
     */
    public void setDayTimes(long[] ms, long winStart, long winEnd) {
        if (ms != null && ms.length == N && winEnd > winStart) {
            System.arraycopy(ms, 0, nodeMs, 0, N);
            winStartMs = winStart;
            winEndMs = winEnd;
            hasTimes = true;
            invalidate();
        }
    }

    /** Localised node names (Fajr .. Isha), set by the dashboard. */
    public void setNames(String[] names) {
        if (names != null && names.length == N) {
            this.names = names;
            invalidate();
        }
    }

    /** Per-node forecast: temps in °C, kinds are Weather.* constants. */
    public void setForecasts(int[] temps, int[] kinds) {
        if (temps != null && kinds != null && temps.length == N && kinds.length == N) {
            System.arraycopy(temps, 0, fcTemp, 0, N);
            System.arraycopy(kinds, 0, fcKind, 0, N);
            invalidate();
        }
    }

    /** Call every tick: current time + which node is next (-1 = none). */
    public void setNow(long now, int nextIdx) {
        this.now = now;
        this.nextIdx = nextIdx;
        invalidate();
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v,
                getResources().getDisplayMetrics());
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        setMeasuredDimension(w, Math.round(dp(182)));
    }

    @Override
    protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        if (!hasTimes) return;
        float w = getWidth();
        float lineY = dp(46);

        // eight anchors share the line evenly: midnight, six prayers,
        // midnight — no linear time scale, every stretch gets equal weight
        float trackStart = dp(16), trackEnd = w - dp(16);
        long[] at = new long[8];
        float[] ax = new float[8];
        at[0] = winStartMs;
        System.arraycopy(nodeMs, 0, at, 1, N);
        at[7] = winEndMs;
        for (int k = 0; k < 8; k++)
            ax[k] = trackStart + k * (trackEnd - trackStart) / 7f;
        float[] xs = new float[N];
        for (int i = 0; i < N; i++) xs[i] = ax[i + 1];

        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dp(6));
        paint.setColor(0xFF2E3D5E);
        cv.drawLine(trackStart, lineY, trackEnd, lineY, paint);

        // gold progress fill, advanced by real time within each segment
        float px = interpX(at, ax,
                Math.max(winStartMs, Math.min(winEndMs, now)));
        if (px > trackStart + 1) {
            paint.setColor(Ui.GOLD);
            cv.drawLine(trackStart, lineY, px, lineY, paint);
        }

        // nodes: a prayer lights up only while it is "on" — from its time
        // until 30 minutes after — then the highlight is removed
        for (int i = 0; i < N; i++) {
            boolean on = now >= nodeMs[i] && now <= nodeMs[i] + HIGHLIGHT_MS;
            boolean isNext = (i == nextIdx);
            float r = dp(isNext ? 31 : 24);
            if (isNext) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(3));
                paint.setColor(Ui.GOLD_BRIGHT);
                cv.drawCircle(xs[i], lineY, r + dp(7), paint);
                paint.setStyle(Paint.Style.FILL);
            }
            paint.setColor(on ? Ui.GOLD : 0xFF2A3A5C);
            paint.setStrokeWidth(dp(6));
            cv.drawCircle(xs[i], lineY, r, paint);
            drawIcon(cv, i, xs[i], lineY, r * 0.52f,
                    on ? 0xFF3E2F00 : 0xFFB0BEC5);
        }

        // labels: name + time + forecast
        float nameY = lineY + dp(31 + 7) + dp(22);
        float timeY = nameY + dp(21);
        float wxY = timeY + dp(25);
        for (int i = 0; i < N; i++) {
            boolean isNext = (i == nextIdx);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setTextSize(sp(15));
            boolean on = now >= nodeMs[i] && now <= nodeMs[i] + HIGHLIGHT_MS;
            textPaint.setColor(on ? Ui.GOLD_BRIGHT
                    : isNext ? 0xFFFFD98A : Ui.TEXT);
            cv.drawText(names[i], xs[i], nameY, textPaint);
            textPaint.setTypeface(Typeface.DEFAULT);
            textPaint.setTextSize(sp(13));
            textPaint.setColor(0xFFB0BEC5);
            cv.drawText(PrayerCalc.formatTime(getContext(), millisToHour(nodeMs[i])), xs[i], timeY, textPaint);
            // forecast: tiny icon + temperature
            if (fcKind[i] >= 0 && fcTemp[i] != Integer.MIN_VALUE) {
                String t = fcTemp[i] + "°";
                textPaint.setTextAlign(Paint.Align.LEFT);
                float tx = xs[i] - dp(6);
                cv.drawText(t, tx, wxY, textPaint);
                drawWxIcon(cv, fcKind[i], tx - dp(11), wxY - dp(4), dp(6.5f), 0xFFB0BEC5);
            }
        }
    }

    /** X of a timestamp, interpolated by real time within its segment. */
    private float interpX(long[] at, float[] ax, long t) {
        if (t <= at[0]) return ax[0];
        for (int k = 0; k < 7; k++) {
            if (t >= at[k] && t < at[k + 1]) {
                long span = at[k + 1] - at[k];
                float f = span > 0 ? (t - at[k]) / (float) span : 0f;
                return ax[k] + f * (ax[k + 1] - ax[k]);
            }
        }
        return ax[7];
    }

    private static double millisToHour(long ms) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(ms);
        return c.get(java.util.Calendar.HOUR_OF_DAY)
                + c.get(java.util.Calendar.MINUTE) / 60.0;
    }

    // ---------- prayer icons ----------

    private void drawIcon(Canvas cv, int idx, float cx, float cy, float r, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        if (idx == 0) drawHalfSun(cv, cx, cy, r, true);                // Fajr: sunrise
        else if (idx == 4) drawHalfSun(cv, cx, cy, r, false);        // Maghrib: sunset
        else if (idx == 5) drawMoon(cv, cx, cy, r);                  // Isha: crescent
        else drawSun(cv, cx, cy, r);                                // full sun
    }

    /** Full sun: disc + 8 rays. */
    private void drawSun(Canvas cv, float cx, float cy, float r) {
        cv.drawCircle(cx, cy, r * 0.55f, paint);
        paint.setStrokeWidth(r * 0.16f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int k = 0; k < 8; k++) {
            double a = Math.PI / 4 * k;
            float x1 = cx + (float) Math.cos(a) * r * 0.75f;
            float y1 = cy + (float) Math.sin(a) * r * 0.75f;
            float x2 = cx + (float) Math.cos(a) * r * 1.0f;
            float y2 = cy + (float) Math.sin(a) * r * 1.0f;
            cv.drawLine(x1, y1, x2, y2, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * Half sun over a horizon line. rising=true draws the upper half (dawn),
     * false draws the lower half (dusk).
     */
    private void drawHalfSun(Canvas cv, float cx, float cy, float r, boolean rising) {
        float sr = r * 0.55f;
        path.reset();
        rect.set(cx - sr, cy - sr, cx + sr, cy + sr);
        path.arcTo(rect, rising ? 180 : 0, 180);
        path.close();
        cv.drawPath(path, paint);
        // horizon
        paint.setStrokeWidth(r * 0.16f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        cv.drawLine(cx - r * 0.95f, cy, cx + r * 0.95f, cy, paint);
        // three short rays on the sun side
        for (int k = -1; k <= 1; k++) {
            float rx = cx + k * r * 0.55f;
            float y1 = cy + (rising ? -1 : 1) * r * 0.72f;
            float y2 = cy + (rising ? -1 : 1) * r * 0.98f;
            cv.drawLine(rx, y1, rx, y2, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    /** Crescent moon via two overlapping circles (even-odd cutout). */
    private void drawMoon(Canvas cv, float cx, float cy, float r) {
        path.reset();
        path.addCircle(cx, cy, r * 0.62f, Path.Direction.CW);
        path.addCircle(cx + r * 0.30f, cy - r * 0.12f, r * 0.52f, Path.Direction.CW);
        path.setFillType(Path.FillType.EVEN_ODD);
        cv.drawPath(path, paint);
    }

    // ---------- weather icons (tiny) ----------

    private void drawWxIcon(Canvas cv, int kind, float cx, float cy, float r, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);
        switch (kind) {
            case Weather.CLEAR:
                drawSun(cv, cx, cy, r);
                break;
            case Weather.PARTLY:
                drawSun(cv, cx - r * 0.25f, cy - r * 0.2f, r * 0.8f);
                drawCloud(cv, cx + r * 0.25f, cy + r * 0.25f, r * 0.8f);
                break;
            case Weather.STORM:
                drawCloud(cv, cx, cy - r * 0.25f, r * 0.85f);
                drawBolt(cv, cx, cy + r * 0.1f, r);
                break;
            case Weather.SNOW:
                drawCloud(cv, cx, cy - r * 0.25f, r * 0.85f);
                paint.setStrokeWidth(r * 0.22f);
                for (int k = -1; k <= 1; k++)
                    cv.drawPoint(cx + k * r * 0.55f, cy + r * 0.75f, paint);
                break;
            case Weather.DRIZZLE:
            case Weather.RAIN:
                drawCloud(cv, cx, cy - r * 0.3f, r * 0.85f);
                paint.setStrokeWidth(r * 0.2f);
                for (int k = -1; k <= 1; k++) {
                    float x = cx + k * r * 0.5f;
                    cv.drawLine(x, cy + r * 0.35f, x - r * 0.15f, cy + r * 0.85f, paint);
                }
                break;
            case Weather.WIND:
                paint.setStrokeWidth(r * 0.22f);
                for (int k = 0; k < 3; k++) {
                    float y = cy - r * 0.6f + k * r * 0.6f;
                    float len = r * (1.6f - k * 0.3f);
                    cv.drawLine(cx - len / 2, y, cx + len / 2, y, paint);
                }
                break;
            case Weather.FOG:
                drawCloud(cv, cx, cy - r * 0.3f, r * 0.85f);
                paint.setStrokeWidth(r * 0.2f);
                for (int k = 0; k < 2; k++) {
                    float y = cy + r * (0.35f + k * 0.4f);
                    cv.drawLine(cx - r * 0.8f, y, cx + r * 0.8f, y, paint);
                }
                break;
            default: // CLOUDY
                drawCloud(cv, cx, cy, r);
                break;
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCloud(Canvas cv, float cx, float cy, float r) {
        cv.drawCircle(cx - r * 0.55f, cy + r * 0.1f, r * 0.5f, paint);
        cv.drawCircle(cx, cy - r * 0.2f, r * 0.68f, paint);
        cv.drawCircle(cx + r * 0.55f, cy + r * 0.1f, r * 0.45f, paint);
        rect.set(cx - r * 0.95f, cy - r * 0.1f, cx + r * 0.9f, cy + r * 0.55f);
        cv.drawRect(rect, paint);
    }

    private void drawBolt(Canvas cv, float cx, float cy, float r) {
        path.reset();
        path.moveTo(cx + r * 0.15f, cy - r * 0.5f);
        path.lineTo(cx - r * 0.35f, cy + r * 0.25f);
        path.lineTo(cx - r * 0.02f, cy + r * 0.25f);
        path.lineTo(cx - r * 0.15f, cy + r * 0.9f);
        path.lineTo(cx + r * 0.35f, cy + r * 0.05f);
        path.lineTo(cx + r * 0.02f, cy + r * 0.05f);
        path.close();
        cv.drawPath(path, paint);
    }
}
