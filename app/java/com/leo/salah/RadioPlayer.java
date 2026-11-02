package com.leo.salah;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

/**
 * Foreground player for the Quran radio. It pauses for the athan (audio
 * focus) and resumes afterwards, and keeps playing while the user browses
 * other screens.
 */
public class RadioPlayer extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    public static final String ACTION_PLAY = "com.leo.salah.RADIO_PLAY";
    public static final String ACTION_STOP = "com.leo.salah.RADIO_STOP";
    public static final String ACTION_STATE = "com.leo.salah.RADIO_STATE";
    public static final String EXTRA_ID = "station";

    private static final String CHAN = "radio_chan";
    private static final int NOTIF_ID = 42;

    private MediaPlayer mp;
    private AudioManager am;
    private AudioFocusRequest focus;
    private String stationId;
    private boolean wantPlay;
    private boolean pausedForFocus;

    @Override
    public void onCreate() {
        super.onCreate();
        Ui.init(this);
        am = (AudioManager) getSystemService(AUDIO_SERVICE);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null && Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHAN,
                    Lang.t("Quran radio"), NotificationManager.IMPORTANCE_LOW);
            ch.setDescription(Lang.t("Live Quran recitation"));
            nm.createNotificationChannel(ch);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopPlayback();
            return START_NOT_STICKY;
        }
        String id = intent != null ? intent.getStringExtra(EXTRA_ID) : null;
        if (id == null || id.isEmpty()) id = new Prefs(this).radioStation();
        if (id.equals(stationId) && Radio.state == Radio.PLAYING) {
            startForeground(NOTIF_ID, notification());
            return START_NOT_STICKY;
        }
        stationId = id;
        new Prefs(this).setRadioStation(id);
        startForeground(NOTIF_ID, notification());
        play();
        return START_NOT_STICKY;
    }

    private void play() {
        release();
        wantPlay = true;
        pausedForFocus = false;
        Radio.error = "";
        setState(Radio.CONNECTING);
        updateNotification();
        try {
            if (!requestFocus()) {
                Radio.error = "audio focus";
                setState(Radio.ERROR);
                stopForeground(true);
                stopSelf();
                return;
            }
            Radio.Station st = Radio.byId(stationId);
            mp = new MediaPlayer();
            if (Build.VERSION.SDK_INT >= 26) {
                mp.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            } else {
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            mp.setOnPreparedListener(this);
            mp.setOnErrorListener(this);
            mp.setOnCompletionListener(this);
            mp.setDataSource(st.url);
            mp.prepareAsync();
        } catch (Exception e) {
            setState(Radio.ERROR);
            stopForeground(true);
            stopSelf();
        }
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        if (!wantPlay) return;
        try {
            player.start();
            setState(Radio.PLAYING);
            updateNotification();
        } catch (Exception e) {
            setState(Radio.ERROR);
        }
    }

    @Override
    public boolean onError(MediaPlayer player, int what, int extra) {
        setState(Radio.ERROR);
        stopForeground(true);
        stopSelf();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer player) {
        // live streams should not complete; a completion means the connection dropped
        setState(Radio.ERROR);
    }

    private boolean requestFocus() {
        if (am == null) return true;
        if (Build.VERSION.SDK_INT >= 26) {
            focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setOnAudioFocusChangeListener(this::onFocus)
                    .build();
            return am.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        return am.requestAudioFocus(this::onFocus, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void onFocus(int change) {
        if (mp == null) return;
        if (change == AudioManager.AUDIOFOCUS_LOSS
                || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            try {
                if (mp.isPlaying()) {
                    mp.pause();
                    pausedForFocus = true;
                }
            } catch (Exception ignored) {}
        } else if (change == AudioManager.AUDIOFOCUS_GAIN && pausedForFocus && wantPlay) {
            try {
                mp.start();
                pausedForFocus = false;
            } catch (Exception ignored) {}
        }
    }

    private void setState(int s) {
        Radio.state = s;
        Radio.playingId = stationId == null ? "" : stationId;
        sendBroadcast(new Intent(ACTION_STATE).setPackage(getPackageName()));
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, notification());
    }

    private Notification notification() {
        Radio.Station st = Radio.byId(stationId == null ? new Prefs(this).radioStation() : stationId);
        String text;
        if (Radio.state == Radio.CONNECTING) text = Lang.t("Connecting…");
        else if (Radio.state == Radio.PLAYING) text = Lang.t("Playing");
        else if (Radio.state == Radio.ERROR) text = Lang.t("Could not play this station");
        else text = Lang.t("Tap a station to listen");
        Intent stop = new Intent(this, RadioPlayer.class).setAction(ACTION_STOP);
        PendingIntent pi = PendingIntent.getService(this, 43, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = new Notification.Builder(this)
                .setContentTitle(Radio.name(st))
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_media_pause, Lang.t("Stop radio"), pi).build());
        if (Build.VERSION.SDK_INT >= 26) b.setChannelId(CHAN);
        return b.build();
    }

    private void stopPlayback() {
        wantPlay = false;
        release();
        Radio.state = Radio.OFF;
        Radio.playingId = "";
        sendBroadcast(new Intent(ACTION_STATE).setPackage(getPackageName()));
        stopForeground(true);
        stopSelf();
    }

    private void release() {
        try {
            if (mp != null) {
                if (mp.isPlaying()) mp.stop();
                mp.release();
            }
        } catch (Exception ignored) {}
        mp = null;
        try {
            if (focus != null && am != null && Build.VERSION.SDK_INT >= 26)
                am.abandonAudioFocusRequest(focus);
            focus = null;
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
