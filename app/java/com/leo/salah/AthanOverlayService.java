package com.leo.salah;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Foreground service: draws the athan overlay on top of any app,
 * plays the athan (or gentle pre-reminder chime), dismissible by the user.
 */
public class AthanOverlayService extends Service {

    private static final String CHAN = "athan_chan";
    private static final int NOTIF_ID = 41;

    private WindowManager wm;
    private View overlay;
    private MediaPlayer mp;
    private TextToSpeech tts;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private PowerManager.WakeLock wakeLock;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int prayerIdx;
    private boolean pre;
    private boolean test;
    private String kind; // null = athan, or "taraweeh" / "adhkar_morning" / "adhkar_evening"
    private boolean dismissed;
    private int prevVolume = -1;
    private boolean prevMuted;
    private boolean volumeCaptured;
    private boolean quietActive;
    private TextView statusView;
    private Runnable fadeRunnable; // athan fade-in stepper, null when idle

    private final Runnable failsafe = this::finishAll;
    private final Runnable pulse = new Runnable() {
        boolean on = true;
        @Override public void run() {
            if (statusView != null && !dismissed) {
                statusView.setAlpha(on ? 1f : 0.35f);
                on = !on;
                handler.postDelayed(this, 700);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Ui.init(this);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null && Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHAN, "Athan",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Athan playback and prayer alerts");
            nm.createNotificationChannel(ch);
        }
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LeoSalah:athan");
            wakeLock.setReferenceCounted(false);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "stop".equals(intent.getAction())) {
            dismiss();
            return START_NOT_STICKY;
        }
        teardownPlayback(); // a previous alert is replaced by the new one
        dismissed = false;
        prayerIdx = intent != null ? intent.getIntExtra("prayer", 3) : 3;
        pre = intent != null && intent.getBooleanExtra("pre", false);
        test = intent != null && intent.getBooleanExtra("test", false);
        kind = intent != null ? intent.getStringExtra("kind") : null;

        if (wakeLock != null && !wakeLock.isHeld()) {
            try { wakeLock.acquire(3 * 60 * 1000L); } catch (Exception ignored) {}
        }

        // gentle reminders (taraweeh / adhkar): calm card + chime, no athan
        if (kind != null) {
            startForeground(NOTIF_ID, buildNotif(kindTitle(kind),
                    Lang.t("Tap Dismiss on screen")));
            if (Settings.canDrawOverlays(this)) showGentle(kind);
            playRaw(R.raw.chime, false);
            handler.postDelayed(this::dismiss, 90_000);
            handler.post(pulse);
            return START_NOT_STICKY;
        }

        String name = PrayerCalc.name(this, prayerIdx);
        String title = (test ? Lang.t("Test · ") : "")
                + (pre ? Lang.t(name + " soon") : Lang.t(name + " Athan"));
        startForeground(NOTIF_ID, buildNotif(title,
                Lang.t(pre ? "Prayer begins shortly" : "Tap Dismiss on screen to stop")));

        if (Settings.canDrawOverlays(this)) {
            showOverlay(name);
        }
        playAlert(name);
        handler.postDelayed(failsafe, 6 * 60 * 1000L); // hard failsafe
        handler.post(pulse);
        return START_NOT_STICKY;
    }

    // ---------- overlay ----------

    private void showOverlay(String name) {
        Prefs p = new Prefs(this);
        boolean banner = p.overlayStyle() == 1;

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int w = WindowManager.LayoutParams.MATCH_PARENT;
        int h = banner ? WindowManager.LayoutParams.WRAP_CONTENT
                       : WindowManager.LayoutParams.MATCH_PARENT;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                0, PixelFormat.TRANSLUCENT);
        if (banner) {
            lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            lp.y = dp(48);
        }
        lp.setTitle("LeoSalahAthan");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setFocusable(true);
        root.setFocusableInTouchMode(true);
        int pad = dp(32);
        root.setPadding(pad, pad, pad, pad);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(48), dp(40), dp(48), dp(40));
        card.setBackground(Ui.roundBg(banner ? 0xF216261F : 0xF2101512, dp(24)));
        if (!banner) {
            card.setMinimumWidth(dp(560));
        }

        TextView mosque = new TextView(this);
        mosque.setText("🕌");
        mosque.setTextSize(54);
        mosque.setGravity(Gravity.CENTER);
        card.addView(mosque);

        TextView title = new TextView(this);
        title.setText((test ? Lang.t("Test · ") : "") + name);
        title.setTextSize(56);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView ar = new TextView(this);
        ar.setText(prayerIdx == 1 && PrayerCalc.isJumuah(this, System.currentTimeMillis())
                ? "الجمعة" : PrayerCalc.NAMES_AR[prayerIdx]);
        ar.setTextSize(44);
        ar.setTextColor(0xFFD4AF37);
        ar.setGravity(Gravity.CENTER);
        card.addView(ar);

        TextView sub = new TextView(this);
        double[] t = PrayerCalc.timesFor(this,
                java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1,
                java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH));
        String when = PrayerCalc.formatTime(this, t[PrayerCalc.TIME_IDX[prayerIdx]]);
        sub.setText(Lang.t(pre ? ("begins in " + new Prefs(this).preMin() + " minutes · " + when)
                        : (when + " · " + PrayerCalc.hijriDate(this))));
        sub.setTextSize(20);
        sub.setTextColor(0xFFB0BEC5);
        sub.setGravity(Gravity.CENTER);
        Ui.margin(sub, 0, dp(8), 0, 0);
        card.addView(sub);

        statusView = new TextView(this);
        statusView.setText(Lang.t(pre ? "🔔 Gentle reminder" : "🔊 Playing athan…"));
        statusView.setTextSize(22);
        statusView.setTextColor(0xFFFFE082);
        statusView.setGravity(Gravity.CENTER);
        Ui.margin(statusView, 0, dp(16), 0, 0);
        card.addView(statusView);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);
        Ui.margin(btnRow, 0, dp(28), 0, 0);

        Button dismissBtn = Ui.bigButton(this, Lang.t("Dismiss"));
        dismissBtn.setOnClickListener(v -> dismiss());
        btnRow.addView(dismissBtn);
        card.addView(btnRow);

        root.addView(card);
        if (!banner) {
            root.setBackgroundColor(0xCC000000);
        }
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                dismiss();
                return true;
            }
            return false;
        });

        overlay = root;
        try {
            wm.addView(overlay, lp);
            dismissBtn.requestFocus();
        } catch (Exception e) {
            overlay = null;
        }
    }

    private String kindTitle(String k) {
        if ("taraweeh".equals(k)) return Lang.t("🌙 Time for Taraweeh");
        if ("adhkar_morning".equals(k)) return Lang.t("🌅 Morning adhkar");
        if ("adhkar_evening".equals(k)) return Lang.t("🌙 Evening adhkar");
        return Lang.t("Reminder");
    }

    /** Calm banner card for taraweeh / adhkar reminders: chime + text + dismiss. */
    private void showGentle(String k) {
        boolean adhkar = k.startsWith("adhkar");
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                0, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = dp(48);
        lp.setTitle("LeoSalahGentle");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setFocusable(true);
        root.setFocusableInTouchMode(true);
        int pad = dp(24);
        root.setPadding(pad, pad, pad, pad);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(40), dp(28), dp(40), dp(28));
        card.setBackground(Ui.roundBg(0xF216261F, dp(24)));

        TextView title = new TextView(this);
        title.setText(kindTitle(k));
        title.setTextSize(34);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView sub = new TextView(this);
        sub.setText(Lang.ar() ? (adhkar
                ? ("أذكار " + ("adhkar_morning".equals(k) ? "الصباح" : "المساء")
                    + " · افتح مجموعة الأذكار")
                : "تقبل الله منا ومنكم · صلاة التراويح")
                : (adhkar ? "Adhkar · open the collection" : "Taraweeh prayers"));
        sub.setTextSize(20);
        sub.setTextColor(0xFFB0BEC5);
        sub.setGravity(Gravity.CENTER);
        Ui.margin(sub, 0, dp(8), 0, 0);
        card.addView(sub);

        statusView = new TextView(this);
        statusView.setText(Lang.t("🔔 Gentle reminder"));
        statusView.setTextSize(20);
        statusView.setTextColor(0xFFFFE082);
        statusView.setGravity(Gravity.CENTER);
        Ui.margin(statusView, 0, dp(12), 0, 0);
        card.addView(statusView);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);
        Ui.margin(btnRow, 0, dp(20), 0, 0);

        if (adhkar) {
            Button openBtn = Ui.bigButton(this, Lang.t("Open adhkar"));
            openBtn.setOnClickListener(v -> {
                try {
                    Intent i = new Intent(this, DuasActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception ignored) {}
                dismiss();
            });
            btnRow.addView(openBtn);
        }
        Button dismissBtn = Ui.bigButton(this, Lang.t("Dismiss"));
        if (adhkar) {
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            dlp.leftMargin = dp(16);
            dismissBtn.setLayoutParams(dlp);
        }
        dismissBtn.setOnClickListener(v -> dismiss());
        btnRow.addView(dismissBtn);
        card.addView(btnRow);

        root.addView(card);
        root.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                dismiss();
                return true;
            }
            return false;
        });

        overlay = root;
        try {
            wm.addView(overlay, lp);
            dismissBtn.requestFocus();
        } catch (Exception e) {
            overlay = null;
        }
    }

    // ---------- audio ----------

    private void playAlert(String name) {
        if (pre) {
            playRaw(R.raw.chime, false);
            handler.postDelayed(this::dismiss, 30_000); // gentle banner goes away itself
            return;
        }
        int voice = new Prefs(this).effectiveVoice(prayerIdx);
        if (voice == 6) {
            speak(prayerIdx == 1 && PrayerCalc.isJumuah(this, System.currentTimeMillis())
                    ? "الجمعة" : PrayerCalc.NAMES_AR[prayerIdx], true);
        } else if (voice == 5) {
            speak(PrayerCalc.NAMES[prayerIdx], false);
        } else {
            playAthanFile(voice);
        }
    }

    private void playAthanFile(int voice) {
        requestFocus();
        mp = new MediaPlayer();
        try {
            Prefs p = new Prefs(this);
            if (voice == 4 && !p.customUri().isEmpty()) {
                mp.setDataSource(this, Uri.parse(p.customUri()));
            } else {
                int res = voice == 1 ? R.raw.athan_voice2
                        : voice == 2 ? R.raw.athan_voice3
                        : voice == 3 ? R.raw.athan_dokali : R.raw.athan_voice1;
                AssetFileDescriptor afd = getResources().openRawResourceFd(res);
                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
            }
            if (Build.VERSION.SDK_INT >= 26) {
                mp.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            } else {
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            applyAthanVolume();
            mp.prepare();
            mp.setOnCompletionListener(m -> onAudioDone());
            mp.setOnErrorListener((m, what, extra) -> { onAudioDone(); return true; });
            mp.start();
            maybeFadeIn();
        } catch (Exception e) {
            // custom file unreadable -> fall back to bundled voice 1
            try {
                mp.reset();
                AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.athan_voice1);
                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                mp.prepare();
                mp.setOnCompletionListener(m -> onAudioDone());
                mp.start();
                maybeFadeIn();
            } catch (Exception ignored) { onAudioDone(); }
        }
    }

    private void playRaw(int resId, boolean unused) {
        try {
            mp = new MediaPlayer();
            AssetFileDescriptor afd = getResources().openRawResourceFd(resId);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            mp.start();
            mp.setOnCompletionListener(m -> releasePlayer());
        } catch (Exception ignored) {}
    }

    private void speak(String prayerName, boolean arabic) {
        try {
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    try {
                        boolean useAr = arabic;
                        int avail = tts.setLanguage(useAr ? new Locale("ar") : Locale.US);
                        if (useAr && (avail == TextToSpeech.LANG_MISSING_DATA
                                || avail == TextToSpeech.LANG_NOT_SUPPORTED)) {
                            tts.setLanguage(Locale.US);
                            useAr = false;
                        }
                        tts.setSpeechRate(0.92f);
                        String sentence = useAr
                                ? "حان الآن موعد صلاة " + prayerName
                                : "It is time for " + prayerName + " prayer.";
                        if (Build.VERSION.SDK_INT >= 21) {
                            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                                @Override public void onStart(String id) {}
                                @Override public void onError(String id) { onAudioDone(); }
                                @Override public void onDone(String id) { onAudioDone(); }
                            });
                            tts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "athan");
                        } else {
                            tts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null);
                            handler.postDelayed(this::onAudioDone, 15000);
                        }
                    } catch (Exception e) { onAudioDone(); }
                } else {
                    onAudioDone();
                }
            });
            handler.postDelayed(() -> { if (!dismissed) onAudioDone(); }, 45000);
        } catch (Exception e) { onAudioDone(); }
    }

    private void requestFocus() {
        try {
            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (Build.VERSION.SDK_INT >= 26 && audioManager != null) {
                focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM).build())
                        .build();
                audioManager.requestAudioFocus(focusRequest);
            }
        } catch (Exception ignored) {}
    }

    private void applyAthanVolume() {
        try {
            int want = athanTargetVolume();
            // Some TV firmwares keep the stream muted even when its volume is raised.
            if (audioManager.isStreamMute(AudioManager.STREAM_MUSIC))
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE, 0);
            if (want != prevVolume)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, want, 0);
        } catch (Exception ignored) {}
    }

    /** Target stream volume for the athan (also the fade-in end point). */
    private int athanTargetVolume() {
        try {
            if (audioManager == null)
                audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            if (!volumeCaptured) {
                prevVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                try { prevMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC); }
                catch (Exception ignored) {}
                volumeCaptured = true;
            }
            return Math.max(1, max * new Prefs(this).athanVol() / 100);
        } catch (Exception ignored) {
            return -1;
        }
    }

    /**
     * Fade-in: start the athan soft and ramp to full volume over ~30 seconds.
     * Ramps the STREAM_MUSIC volume (the same channel applyAthanVolume sets).
     */
    private void maybeFadeIn() {
        try {
            if (!new Prefs(this).fadeIn()) return;
            int target = athanTargetVolume();
            if (target < 1) return;
            if (audioManager == null)
                audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            final int start = Math.max(1, target * 15 / 100);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, start, 0);
            stopFade();
            final int steps = 30;
            fadeRunnable = new Runnable() {
                int i = 0;
                @Override public void run() {
                    i++;
                    float f = i / (float) steps;
                    int v = start + Math.round((target - start) * f * f); // ease-in
                    try {
                        audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC, Math.min(v, target), 0);
                    } catch (Exception ignored) {}
                    if (i < steps && mp != null && mp.isPlaying())
                        handler.postDelayed(this, 1000);
                    else
                        fadeRunnable = null;
                }
            };
            handler.postDelayed(fadeRunnable, 1000);
        } catch (Exception ignored) {}
    }

    private void stopFade() {
        if (fadeRunnable != null) handler.removeCallbacks(fadeRunnable);
        fadeRunnable = null;
    }

    private void restoreVolume() {
        try {
            if (audioManager != null && prevVolume >= 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, prevVolume, 0);
                if (prevMuted) audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE, 0);
                prevVolume = -1;
                prevMuted = false;
                volumeCaptured = false;
            }
            if (focusRequest != null && Build.VERSION.SDK_INT >= 26)
                audioManager.abandonAudioFocusRequest(focusRequest);
        } catch (Exception ignored) {}
    }

    private void onAudioDone() {
        handler.removeCallbacks(failsafe);
        stopFade();
        releasePlayer();
        restoreVolume();
        if (statusView != null) {
            statusView.setText(Lang.ar() ? "تقبل الله منا ومنكم"
                    : "Taqabbal Allahu minna wa minkum");
            statusView.setAlpha(1f);
        }
        handler.removeCallbacks(pulse);
        // linger a little so the user sees the closing message, then go away
        handler.postDelayed(this::dismiss, 45_000);
        armQuietMode();
    }

    // ---------- quiet mode ----------

    private void armQuietMode() {
        if (pre || test) return;
        int q = new Prefs(this).quietMin();
        if (q <= 0) return;
        try {
            if (audioManager == null)
                audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_MUTE, 0);
            quietActive = true;
            startForeground(NOTIF_ID, buildNotif(Lang.t("🌙 Quiet time"),
                    Lang.t("TV sound resumes in " + q + " minutes")));
            handler.postDelayed(() -> {
                try {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_UNMUTE, 0);
                } catch (Exception ignored) {}
                quietActive = false;
                finishAll();
            }, q * 60_000L);
        } catch (Exception ignored) {}
    }

    // ---------- teardown ----------

    private void releasePlayer() {
        try {
            if (mp != null) {
                if (mp.isPlaying()) mp.stop();
                mp.release();
            }
        } catch (Exception ignored) {}
        mp = null;
        try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception ignored) {}
        tts = null;
    }

    private void teardownPlayback() {
        handler.removeCallbacks(failsafe);
        handler.removeCallbacks(pulse);
        stopFade();
        releasePlayer();
        restoreVolume();
        if (overlay != null && wm != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        overlay = null;
        statusView = null;
    }

    public void dismiss() {
        if (dismissed) { finishAll(); return; }
        dismissed = true;
        if (quietActive) {
            try {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE, 0);
            } catch (Exception ignored) {}
            quietActive = false;
        }
        teardownPlayback();
        finishAll();
    }

    private void finishAll() {
        handler.removeCallbacksAndMessages(null);
        try { stopForeground(true); } catch (Exception ignored) {}
        try {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        } catch (Exception ignored) {}
        stopSelf();
    }

    @Override
    public void onDestroy() {
        teardownPlayback();
        try {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private Notification buildNotif(String title, String text) {
        Intent stop = new Intent(this, AthanOverlayService.class).setAction("stop");
        PendingIntent pi = PendingIntent.getService(this, 7, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        Lang.t("Dismiss"), pi).build());
        if (Build.VERSION.SDK_INT >= 26) b.setChannelId(CHAN);
        return b.build();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
