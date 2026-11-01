package com.leo.salah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Fired by AlarmManager at prayer time (or pre-reminder). Keeps the alarm chain rolling. */
public class AthanAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent in) {
        // keep the rolling 2-day horizon scheduled
        try { AthanScheduler.scheduleAll(ctx); } catch (Exception ignored) {}

        int prayer = in.getIntExtra("prayer", 3);
        boolean pre = in.getBooleanExtra("pre", false);
        Intent svc = new Intent(ctx, AthanOverlayService.class);
        svc.putExtra("prayer", prayer);
        svc.putExtra("pre", pre);
        String kind = in.getStringExtra("kind");
        if (kind != null) svc.putExtra("kind", kind);
        try {
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(svc);
            else ctx.startService(svc);
        } catch (Exception e) {
            // last resort: just reschedule already done above
        }
    }
}
