package com.leo.salah;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Manual ambient display: fullscreen prayer dashboard that keeps the screen on.
 * For Google TV (Android 12+) devices, where the system hides third-party
 * screensavers from Settings, this is the zero-setup way to keep the prayer
 * clock on screen. Press any button to exit.
 */
public class AmbientActivity extends BaseActivity {

    private DashboardView dashboard;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            try { if (dashboard != null) dashboard.tick(); } catch (Exception ignored) {}
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout frame = new FrameLayout(this);
        frame.addView(new SkyView(this),
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        dashboard = new DashboardView(this);
        dashboard.setGravity(Gravity.CENTER);
        int pad = Ui.dp(this, 36);
        dashboard.setPadding(pad, Ui.dp(this, 24), pad, Ui.dp(this, 24));
        frame.addView(dashboard, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER));

        TextView hint = Ui.label(this, "Press any button to exit", 15, 0xFFB0BEC5);
        hint.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        hlp.bottomMargin = Ui.dp(this, 24);
        frame.addView(hint, hlp);

        setContentView(frame);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dashboard.refreshDay();
        handler.post(ticker);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        finish();
        return true;
    }
}
