package com.theshmuz.app.util;

import java.util.Formatter;
import java.util.Locale;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.theshmuz.app.R;

public class VideoController extends RelativeLayout implements OnClickListener, OnSeekBarChangeListener {

    private static final int DEFAULT_TIMEOUT = 3000;

    private MediaPlayerControl mPlayer;
    private View mainContainer;

    private ImageButton mPlayButton;
    private SeekBar mProgress;
    private TextView mEndTime;
    private TextView mCurrentTime;

    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;

    private boolean mShowing;
    private boolean mDragging;

    public VideoController(Context context, AttributeSet attrs) {
        super(context, attrs);

        setBackgroundColor(0xFFDDDDDD);

        LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflate.inflate(R.layout.video_control, this, true);

        mPlayButton = (ImageButton) findViewById(R.id.playButton);
        mPlayButton.setOnClickListener(this);
        mPlayButton.requestFocus();

        mProgress = (SeekBar) findViewById(R.id.seeker);
        if(mProgress != null) {
            mProgress.setOnSeekBarChangeListener(this);
            mProgress.setMax(1000);
        }

        mEndTime = (TextView) findViewById(R.id.durationTime);
        mCurrentTime = (TextView) findViewById(R.id.positionTime);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    }

    private void sysbarHide() {
        if(Build.VERSION.SDK_INT >= 11 && mainContainer != null) {
            Version11Helper.setSystemUiVisibility(mainContainer, View.STATUS_BAR_HIDDEN);
        }
    }
    private void sysbarShow() {
        if(Build.VERSION.SDK_INT >= 11 && mainContainer != null) {
            Version11Helper.setSystemUiVisibility(mainContainer, View.STATUS_BAR_VISIBLE);
        }
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();
        setVisibility(View.INVISIBLE);
    }

    public void setup(MediaPlayerControl videoView, View mainContainer) {
        this.mPlayer = videoView;
        this.mainContainer = mainContainer;
    }
    public void detach(){
        sysbarShow();
        this.mainContainer = null;
        mHandler.removeMessages(HANDLE_SHOW_PROGRESS);
    }

    public void toggle(){
        if(mShowing) hide();
        else show();
    }
    public boolean isShowing(){
        return mShowing;
    }
    public void show(){
        show(DEFAULT_TIMEOUT);
    }
    public void show(int timeout) {
        if(!mShowing){
            setVisibility(View.VISIBLE);
            setProgress();

            disableUnsupportedButtons();
            mShowing = true;
            sysbarShow();
        }

        updatePausePlay();

        mHandler.sendEmptyMessage(HANDLE_SHOW_PROGRESS);

        Message msg = mHandler.obtainMessage(HANDLE_FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(HANDLE_FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }
    public void hide() {
        if (mShowing) {
            mHandler.removeMessages(HANDLE_SHOW_PROGRESS);
            setVisibility(View.INVISIBLE);
            if(mPlayer.isPlaying()) sysbarHide();
            mShowing = false;
        }
    }

    private void doPauseResume() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.playButton:
                doPauseResume();
                show(DEFAULT_TIMEOUT);
                break;
        }
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mPlayer == null) return 0;
        if (mDragging) return mPlayer.getCurrentPosition();

        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();

        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress( (int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null) mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null) mCurrentTime.setText(stringForTime(position));

        return position;
    }

    private void updatePausePlay() {
        if (mPlayButton == null) return;

        if (mPlayer.isPlaying()) {
            mPlayButton.setImageResource(R.drawable.av_pause);
        } else {
            mPlayButton.setImageResource(R.drawable.av_play);
        }
    }

    private void disableUnsupportedButtons() {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) return;

        long duration = mPlayer.getDuration();
        long newposition = (duration * progress) / 1000L;
        mPlayer.seekTo((int) newposition);
        if (mCurrentTime != null) mCurrentTime.setText(stringForTime((int) newposition));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        show(3600000);

        mDragging = true;

        mHandler.removeMessages(HANDLE_SHOW_PROGRESS);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mDragging = false;
        setProgress();
        updatePausePlay();
        show(DEFAULT_TIMEOUT);

        mHandler.sendEmptyMessage(HANDLE_SHOW_PROGRESS);
    }

    public static final int HANDLE_FADE_OUT = 1;
    public static final int HANDLE_SHOW_PROGRESS = 2;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLE_FADE_OUT:
                    hide();
                    break;
                case HANDLE_SHOW_PROGRESS:
                    int pos = setProgress();
                    if (!mDragging && mShowing && mPlayer.isPlaying()) {
                        msg = obtainMessage(HANDLE_SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode ==  KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show();
                if (mPlayButton != null) {
                    mPlayButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlay();
                show();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
                show();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        } else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
            if(!isShowing()){
                show();
                return true;
            }
        }

        show();
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        show(DEFAULT_TIMEOUT);
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(DEFAULT_TIMEOUT);
        return false;
    }

}
