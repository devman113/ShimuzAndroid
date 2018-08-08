package com.theshmuz.app.streamer;

import com.theshmuz.app.R;

public class PlaybackState {

    public static final int STOPPED = 1;

    public static final int PLAYING = 2;

    public int currentState = STOPPED;

    public int currentStatus = R.string.status_stopped;

    public int currentPosition;
    public int currentDuration;

    public NowPlaying nowPlaying;

    public boolean isSame(String type, String id){
        if(nowPlaying == null) return false;
        return nowPlaying.isSame(type, id);
    }

}