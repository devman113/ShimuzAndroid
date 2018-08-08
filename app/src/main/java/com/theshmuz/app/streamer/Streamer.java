package com.theshmuz.app.streamer;

import java.io.IOException;

import com.theshmuz.app.D;
import com.theshmuz.app.activity.DetailActivity;
import com.theshmuz.app.R;
import com.theshmuz.app.Shmuz;
import com.theshmuz.app.ShmuzHelper;
import com.theshmuz.app.UpdatorStatus;
import com.theshmuz.app.loaders.SavePositionTask;
import com.theshmuz.app.util.DownloadHelper;
import com.theshmuz.app.util.Version11Helper;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class Streamer extends Service implements MusicFocusable, OnInfoListener, OnErrorListener, OnCompletionListener, OnPreparedListener {

    public static final int SAVE_INTERVAL = 15000; //every 15 seconds
    public static final int REWIND = 2000;
    private int lastTimeSaved;

    private static final int STATE_ERROR              = -1;
    private static final int STATE_IDLE               = 0;
    private static final int STATE_PREPARING          = 1;
    private static final int STATE_PLAYING            = 3;
    private static final int STATE_PAUSED             = 4;

    private boolean isSupposedToBePlaying() {
        return mCurrentState == STATE_PLAYING
                || mCurrentState == STATE_PREPARING
                || mCurrentState == STATE_PAUSED;
    }
    private int mCurrentState = STATE_IDLE;

    private PlaybackState state;
    private ShmuzHelper dbh;
    private UpdatorStatus updator;

    public static final int NOTIFY_ID = 1;
    public static final int NOTIFY_ERR_ID = 2;

    private NotificationManager mNotificationManager;
    private DownloadManager mDownloadManager;
    private PendingIntent contentIntent;
    private WifiLock mWifiLock;
    private AudioFocusHelper mAudioFocusHelper;
    private MediaPlayer mp;

    private boolean isDoingOffline;

    private int specialStartTarget;

    @Override
    public void onCreate() {
        if(D.D) Log.d("Streamer Service", "created");
        super.onCreate();

        Shmuz app = (Shmuz) getApplication();
        state = app.state;
        dbh = app.db;
        updator = app.updator;

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "TheShmuz");

        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(PINGCMD);
        iFilter.addAction(SERVICECMD);
        iFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mBR, iFilter);

        stopForeground(true);

        if (Build.VERSION.SDK_INT >= 8) {
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        }

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBR);
        localHandler.removeMessages(LOCAL_HANDLE_PROGRESS_UPDATE);

        if(mp != null){
            mp.release();
            mp = null;
        }

        state.currentState = PlaybackState.STOPPED;
        state.currentStatus = R.string.status_stopped;
        updateUI();

        if (mWifiLock.isHeld()) mWifiLock.release();

        mAudioFocusHelper.abandonFocus();

        stopForeground(true);

        if(D.D) Log.d("Streamer service", "destroyed");

        super.onDestroy();
    }

    private void updateUI() {
        Intent updateUi = new Intent(PONGCMD);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(updateUi);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(D.D) Log.d("Streamer", "onStartCommand");
        if(intent != null) {
            String action = intent.getAction();
            if(SERVICECMD.equals(action)) {
                doServiceCmdHandle(intent);
            }
        }
        return START_NOT_STICKY;
    }

    private void actualPlayCmd(NowPlaying nowPlaying) {
        if(D.D) Log.d("actualPlayCmd", "called for " + nowPlaying.getUrl());

        if(state.nowPlaying != null && state.nowPlaying.equals(nowPlaying)){
            specialStartTarget = state.currentPosition;
            state.nowPlaying = nowPlaying;
        }
        else{
            specialStartTarget = nowPlaying.getStartPosition();
            state.currentPosition = specialStartTarget;
            state.currentDuration = 0;
            state.nowPlaying = nowPlaying;
        }

        long startTime = 0;

        Intent notificationIntent = new Intent(getApplicationContext(), DetailActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra("type", nowPlaying.getType());
        notificationIntent.putExtra("id", nowPlaying.getId());
        contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher3)
                        .setContentTitle("The Shmuz")
                        .setOngoing(true)
                        .setWhen(0)
                        .setContentIntent(contentIntent)
                        .setContentText(nowPlaying.getTitle());

        Notification notification = mBuilder.build();

        notification.tickerText = null;
        if(Build.VERSION.SDK_INT >= 11) {
            Version11Helper.makeNotifyVoid(notification);
        }

        startForeground(NOTIFY_ID, notification);

        mNotificationManager.cancel(NOTIFY_ERR_ID);

        state.currentStatus = R.string.status_preparing;
        state.currentState = PlaybackState.PLAYING;
        updateUI();

        try {
            if(D.D) startTime = SystemClock.uptimeMillis();

            if(mp != null) {
                try{
                    if(mp.isPlaying()){
                        mp.stop();
                    }
                }
                catch(IllegalStateException e){
                    if(D.D) Log.e("IllegalStateException", "mp != null");
                }

                mp.release();
                mp = null;
            }

            mp = new MediaPlayer();

            mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            mp.setOnInfoListener(this);
            mp.setOnCompletionListener(this);
            mp.setOnPreparedListener(this);
            mp.setOnErrorListener(this);

            Uri toTry = DownloadHelper.getDownload(mDownloadManager, nowPlaying.getDownloadId());

            if(D.D) Log.d("TO TRY:", "" + toTry);

            if(toTry != null) {
                isDoingOffline = true;
                if(D.D) Log.e("Doing Offline", toTry.toString());
                try {
                    mp.setDataSource(getApplicationContext(), toTry);
                } catch(IOException e){
                    if(D.D) Log.e("MP SetDataSource", "IOException");
                    toTry = null;
                }
            }

            if(toTry == null) {
                isDoingOffline = false;
                mp.setDataSource(nowPlaying.getUrl());
            }

            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setVolume(1.0f, 1.0f);

            mp.prepareAsync();

            mCurrentState = STATE_PREPARING;

            if(D.D) Log.i("mp total", "took " + (SystemClock.uptimeMillis() - startTime));

        } catch (IllegalArgumentException e) {

            if(D.D) Log.e("MediaPlayer", "IllegalArgumentException");

            mCurrentState = STATE_ERROR;
            onError(mp, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);

        } catch (IllegalStateException e) {
            if(D.D) Log.e("MediaPlayer", "IllegalStateException... gonna try again...");

            mp.reset();
            try {
                mp.setDataSource(nowPlaying.getUrl());
            } catch (IllegalArgumentException e1) {
                if(D.D) Log.e("MediaPlayer", "IllegalArgumentException (2nd time)");
            } catch (IllegalStateException e1) {
                if(D.D) Log.e("MediaPlayer", "IllegalStateException (2nd time)");
            } catch (IOException e1) {
                if(D.D) Log.e("MediaPlayer", "IOException (2nd time)");
            }

        } catch (IOException e) {

            if(D.D) Log.e("MediaPlayer", "IOException");

            mCurrentState = STATE_ERROR;
            onError(mp, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }

        mWifiLock.acquire();
    }

    public void onPrepared(MediaPlayer mp) {
        if(mp != this.mp){
            if(D.D) Log.w("onPrepared", "got invalid mp");
            return;
        }

        if(mCurrentState == STATE_PREPARING) {
            state.currentStatus = R.string.status_playing;
            mCurrentState = STATE_PLAYING;
            state.currentDuration = mp.getDuration();
            updateUI();

            lastTimeSaved = 0;
            localHandler.sendEmptyMessageDelayed(LOCAL_HANDLE_PROGRESS_UPDATE, 1000);

            mAudioFocusHelper.requestFocus();
            specialStartTarget -= REWIND;
            if(specialStartTarget > 0){
                mp.seekTo(specialStartTarget);
                specialStartTarget = 0;
            }
            mp.start();
        }
    }

    private void stopIt() {
        lastTimeSaved = 0;
        if(mCurrentState == STATE_PLAYING && state.nowPlaying != null) {
            savePosition(state.nowPlaying, state.currentPosition, state.currentDuration);
        }

        state.currentStatus = R.string.status_stopped;
        state.currentState = PlaybackState.STOPPED;
        updateUI();

        localHandler.removeMessages(LOCAL_HANDLE_PROGRESS_UPDATE);

        if(isSupposedToBePlaying()) {
            doStop();
        }

        mAudioFocusHelper.abandonFocus();

        stopForeground(true);

        if (mWifiLock.isHeld()) mWifiLock.release();

        mCurrentState = STATE_IDLE;

        stopSelf();
    }

    private void doStop() {
        if(mp != null){
            try{
                if(mCurrentState != STATE_PREPARING && mp.isPlaying()){
                    mp.stop();
                }
            }
            catch(IllegalStateException e){

            }

            mp.release();
            mp = null;
        }

    }

    public void onCompletion(MediaPlayer mp) {
        if(this.mp != mp) {
            if(D.D) Log.w("onCompletion", "got invalid mp");
            return;
        }

        state.currentPosition = 0;
        stopIt();
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        if(this.mp != mp) {
            if(D.D) Log.w("MediaHandler", "onError called with old mp");
            return true;
        }

        if(D.D) Log.e("onError", "got " + what + ':' + extra);

        if(!isOnline()) {
            notifErr("No Internet Connection");
            stopIt();
            return true;
        }

        CharSequence errorText;

        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                errorText = "Not valid for Playback Error";
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                errorText = "Server Died Error";
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                errorText = new StringBuilder("Error (").append(what).append(") ").append(extra);
                break;
            default:
                errorText = "Unknown Error Occured";
        }
        notifErr(errorText);

        stopIt();
        return true;
    }

    private void notifErr(CharSequence errorText) {
        CharSequence contentTitle = "Shmuz Playback Error";

        PendingIntent temp;
        if(contentIntent != null) temp = contentIntent;
        else temp = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher3)
                        .setContentTitle(contentTitle)
                        .setAutoCancel(true)
                        .setTicker(errorText)
                        .setContentIntent(temp)
                        .setContentText(errorText);

        Notification errNotification = mBuilder.build();

        mNotificationManager.notify(NOTIFY_ERR_ID, errNotification);
    }

    public boolean onInfo(MediaPlayer mp, int arg1, int arg2) {
        if(mp != this.mp){
            if(D.D) Log.w("onInfo", "got invalid mp");
            return true;
        }

        if(D.D) Log.d("Streamer", "info called " + arg1 + ' ' + arg2);
        switch(arg1){
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                state.currentStatus = R.string.status_buffering;
                updateUI();
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                state.currentStatus = R.string.status_playing;
                updateUI();
                break;
        }
        return false;
    }

    public void onGainedAudioFocus() {
        if (mCurrentState == STATE_PAUSED && mp != null){
            mCurrentState = STATE_PLAYING;
            mp.start();

            state.currentStatus = R.string.status_playing;
            updateUI();
        }
    }

    public void onLostAudioFocus(boolean isTransient, boolean canDuck) {
        if(isTransient && canDuck){
            if(D.D) Log.d("Streamer", "Do Duck");
        }
        else if(isTransient){

            if(mp != null && (mCurrentState == STATE_PLAYING || mCurrentState == STATE_PREPARING)){
                mCurrentState = STATE_PAUSED;
                mp.pause();

                state.currentStatus = R.string.status_paused_app;
                updateUI();
            }
        }
        else{
            stopIt();
        }
    }

    public static final int LOCAL_HANDLE_PROGRESS_UPDATE = 3;

    private Handler localHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {

                case LOCAL_HANDLE_PROGRESS_UPDATE:
                    int pos = updateProgress();
                    if(pos >= 0){
                        sendEmptyMessageDelayed(LOCAL_HANDLE_PROGRESS_UPDATE, 1000 - (pos % 1000));
                        if(state.nowPlaying != null && (pos - lastTimeSaved) >= SAVE_INTERVAL) {
                            savePosition(state.nowPlaying, state.currentPosition, state.currentDuration);
                            lastTimeSaved = pos;
                        }
                    }
                    break;

            }
        }
    };

    private void savePosition(NowPlaying nowPlaying, int currentPosition, int currentDuration) {
        if(D.D) Log.i("Saving Position...", currentPosition + " / " + currentDuration);
        new SavePositionTask(dbh, updator).execute(nowPlaying.getType(), nowPlaying.getId(), Integer.toString(currentPosition), Integer.toString(currentDuration));
    }

    private int updateProgress() {
        if(mp == null || !isSupposedToBePlaying()) return -1;
        int position = mp.getCurrentPosition();
        state.currentPosition = position;
        updateUI();
        return position;
    }

    private BroadcastReceiver mBR = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent == null) return;

            String action = intent.getAction();
            if(action == null) return;

            if(PINGCMD.equals(action)) {
                updateUI();
            }
            else if(SERVICECMD.equals(action)) {
                doServiceCmdHandle(intent);
            }
            else if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                stopIt();
            }
        }
    };

    private void doServiceCmdHandle(Intent intent) {
        String cmd = intent.getStringExtra(CMDNAME);
        if(cmd == null) return;
        if (CMDPLAY.equals(cmd)) {
            NowPlaying incoming = (NowPlaying) intent.getParcelableExtra("nowplaying");
            actualPlayCmd(incoming);
        }
        else if (CMDTOGGLEPAUSE.equals(cmd)) {
            if (isSupposedToBePlaying()) {
                if(D.D) Log.d("CMDTOGGLEPAUSE", "supposed to be playing");
                stopIt();
            }
            else if(state.nowPlaying != null) {
                if(D.D) Log.d("CMDTOGGLEPAUSE", "LastP != null");
                actualPlayCmd(state.nowPlaying);
            }
            else {
                if(D.D) Log.d("CMDTOGGLEPAUSE", "else");
                NowPlaying incoming = (NowPlaying) intent.getParcelableExtra("nowplaying");
                if (incoming != null) actualPlayCmd(incoming);
            }
        }
        else if (CMDSTOP.equals(cmd)) {
            stopIt();
        }
        else if(CMDSEEK.equals(cmd)) {
            int progress = intent.getIntExtra("position", -1);
            if(progress < 0) return;
            if(mCurrentState == STATE_PLAYING || mCurrentState == STATE_PAUSED){
                long duration = mp.getDuration();
                int newposition = (int) ((duration * progress) / 1000L);
                mp.seekTo(newposition);
                state.currentPosition = newposition;
            }
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static final String SERVICECMD = "com.theshmuz.app.streamer";

    public static final String CMDNAME = "command";
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDPLAY = "play";
    public static final String CMDSTOP = "stop";
    public static final String CMDSEEK = "seek";

    public static final String PINGCMD = "com.theshmuz.app.ping";

    public static final String PONGCMD = "com.theshmuz.app.pong";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
