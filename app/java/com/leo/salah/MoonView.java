package com.leo.salah;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

/** Small phase-accurate moon icon for the dashboard weather row. */
public class MoonView extends View {
    public MoonView(Context c) {
        super(c);
    }

    @Override
    protected void onMeasure(int wms, int hms) {
        int s = Ui.dp(getContext(), 40);
        setMeasuredDimension(s, s);
    }

    @Override
    protected void onDraw(Canvas cv) {
        float r = Math.min(getWidth(), getHeight()) / 2f - Ui.dp(getContext(), 3);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        // faint halo so the dark side reads on the night sky
        android.graphics.Paint halo = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        halo.setColor(0x22FFFFFF);
        cv.drawCircle(cx, cy, r + Ui.dp(getContext(), 4), halo);
        MoonDraw.draw(cv, cx, cy, r, PrayerCalc.moonPhaseContinuous(),
                0xFFF5F3E7, 0xFF232B4D);
    }
}
