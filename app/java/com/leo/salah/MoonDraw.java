package com.leo.salah;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Draws a phase-accurate moon disc: a dark disc with the lit region bounded
 * by the outer limb and the terminator half-ellipse.
 * phase: 0 = new, 0.25 = first quarter, 0.5 = full, 0.75 = last quarter.
 */
public class MoonDraw {
    public static void draw(Canvas cv, float cx, float cy, float r, double phase,
                            int litColor, int darkColor) {
        if (cv == null || r <= 0) return;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(darkColor);
        cv.drawCircle(cx, cy, r, p);

        double ph = phase - Math.floor(phase);
        boolean waxing = ph < 0.5;
        boolean crescent = (ph < 0.25) || (ph > 0.75);
        float rx = (float) (r * Math.abs(Math.cos(2 * Math.PI * ph)));

        Path path = new Path();
        RectF disc = new RectF(cx - r, cy - r, cx + r, cy + r);
        path.moveTo(cx, cy - r);
        // outer lit limb, top -> bottom (angles: 270 = top, sweep +180 = clockwise via right)
        path.arcTo(disc, 270, waxing ? 180 : -180);
        // terminator, bottom -> top, bulging toward the dark side
        if (rx < 0.5f) {
            path.lineTo(cx, cy - r); // exact half phase: straight terminator
        } else {
            RectF term = new RectF(cx - rx, cy - r, cx + rx, cy + r);
            path.arcTo(term, 90, (waxing == crescent) ? -180 : 180);
        }
        path.close();
        p.setColor(litColor);
        cv.drawPath(path, p);
    }
}
