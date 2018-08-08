package com.theshmuz.app;

import com.theshmuz.app.business.Adver;
import com.theshmuz.app.loaders.AdTask;
import com.theshmuz.app.streamer.PlaybackState;
import com.theshmuz.app.util.DiskCache;
import com.theshmuz.app.util.Version11Helper;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;

public class Shmuz extends Application {

    public ShmuzHelper db;
    public PlaybackState state;
    public UpdatorStatus updator;
    public DiskCache diskCache;

    private long lastAdTry;
    private Adver lastestAd;
    public AdTask adTask;

    @Override
    public void onCreate() {
        super.onCreate();
        db = new ShmuzHelper(getApplicationContext());
        state = new PlaybackState();
        updator = new UpdatorStatus(this);
        diskCache = new DiskCache(this);
    }

    private LoginHelper login;

    public LoginHelper getLoginHelper() {
        if(login == null) {
            login = new LoginHelper(this);
        }
        return login;
    }

    public void gotNewAdver(Adver adver) {
        this.lastestAd = adver;
        Intent intent = new Intent(AdTask.AD_CMD);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    public void gotNewForceVersion(int version) {
        if(version <= 0) return;
    }

    /**
     * Will get the ad or request it!
     * @return
     */
    public Adver getAd() {
        if(lastestAd != null && !lastestAd.isOld()) {
            return lastestAd;
        }

        if(adTask != null) {
            return null;
        }

        if(lastestAd == null && lastAdTry > 0 && Math.abs(SystemClock.elapsedRealtime() - lastAdTry) < 15 * 60 * 1000) { //15 minutes
            return lastestAd;
        }

        lastAdTry = SystemClock.elapsedRealtime();
        adTask = new AdTask(this);
        if(Build.VERSION.SDK_INT >= 11) Version11Helper.runAsyncTask(adTask);
        else adTask.execute();

        return null;
    }

}