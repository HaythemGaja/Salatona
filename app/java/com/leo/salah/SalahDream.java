package com.leo.salah;

import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.Gravity;
import android.widget.FrameLayout;

/**
 * TV screensaver ("Daydream"): shows the prayer dashboard while the TV is idle.
 * Enable in system Settings → Device Preferences → Screen saver → Salatona.
 */
public class SalahDream extends DreamService {

    private DashboardView dashboard;
    private SkyView sky;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            try {
                if (dashboard != null) dashboard.tick();
                if (sky != null && dashboard != null) {
                    sky.setPhase(dashboard.phasePrayer(), dashboard.phaseProgress());
                    sky.setWeatherKind(dashboard.weatherKind());
                }
            } catch (Exception ignored) {}
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);

        FrameLayout frame = new FrameLayout(this);
        sky = new SkyView(this);
        frame.addView(sky,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        dashboard = new DashboardView(this);
        dashboard.setGravity(Gravity.CENTER);
        int pad = Ui.dp(this, 36);
        dashboard.setPadding(pad, Ui.dp(this, 24), pad, Ui.dp(this, 24));
        frame.addView(dashboard, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER));

        setContentView(frame);
        dashboard.refreshDay();
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        handler.post(ticker);
    }

    @Override
    public void onDreamingStopped() {
        handler.removeCallbacks(ticker);
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacks(ticker);
        super.onDetachedFromWindow();
    }
}
