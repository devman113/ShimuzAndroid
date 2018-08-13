package com.theshmuz.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.theshmuz.app.fragments.HardcodePopup1;
import com.theshmuz.app.util.VersionHelper;

/**
 * Created by yossie on 2/12/17.
 */

public class SharedPrefWrap {

    private static final String KEY_LAST_UPDATE_SUCCESS = "lastUpdateSuccess";
    public long getLastUpdateSuccess() {
        return settings.getLong(KEY_LAST_UPDATE_SUCCESS, 0);
    }
    public void setLastUpdateSuccess(long lastUpdateSuccess) {
        settings.edit().putLong(KEY_LAST_UPDATE_SUCCESS, lastUpdateSuccess).apply();
    }


    private static final String KEY_LAST_AD = "ad1";
    public String getLastShowedAd1() {
        return settings.getString(KEY_LAST_AD, null);
    }

    private static final String PLAY_BUTTON_COUNT = "pbc";
    public int getPlayCount() {
        return settings.getInt(PLAY_BUTTON_COUNT, 0);
    }
    public void incrementPlayCount() {
        int oldCount = getPlayCount();
        oldCount++;
        if(D.D) Log.d("SharedPrefWrap", "New play count = " + oldCount);
        settings.edit().putInt(PLAY_BUTTON_COUNT, oldCount).apply();
    }

    public String getHard1AdState() {
        return settings.getString(HardcodePopup1.HARD_POP_1_STATE, "");
    }

    public static final int CURRENT_VERSION = 2;

    public static final boolean firstRunning = true;
    private static final String KEY_FIRST_RUNNING = "firstRunning";
    public boolean getFirstRunningStatus() {
        return settings.getBoolean(KEY_FIRST_RUNNING, true);
    }
    public void setFirstRunningStatus() {
        settings.edit().putBoolean(KEY_FIRST_RUNNING, false).apply();
    }


    private static SharedPrefWrap mInstance;

    public static SharedPrefWrap getInstance(Context context) {
        if(D.D) enforceMainThread();
        if(mInstance == null) {
            mInstance = new SharedPrefWrap(context);
        }
        return mInstance;
    }

    private SharedPreferences settings;

    private SharedPrefWrap(Context context) {
        long startTime = -1;
        if(D.D) startTime = SystemClock.elapsedRealtime();

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        if(D.D) {
            long elapsed = SystemClock.elapsedRealtime() - startTime;
            Log.d("SharedPrefWrap", "init of settings took: " + elapsed);
        }

        updateSharedPrefs();
    }

    private static final String KEY_VER = "ver";
    private void updateSharedPrefs() {
        if(settings.contains(KEY_VER) || settings.contains("lastUpdateSuccess")) {
            int oldVersion = settings.getInt(KEY_VER, 1);
            if(CURRENT_VERSION > oldVersion) {
                SharedPreferences.Editor editor = settings.edit();
                onUpgrade(editor, oldVersion, CURRENT_VERSION);
                editor.putInt(KEY_VER, CURRENT_VERSION);
                editor.apply();
            }
        }
        else {
            if(D.D) Log.d("updateSharedPrefs", "Creating first pref...");
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(KEY_VER, CURRENT_VERSION);
            editor.apply();
        }
    }

    private void onUpgrade(SharedPreferences.Editor editor, int oldVersion, int newVersion) {
        if(D.D) Log.e("SETTINGS ON UPGRADE", "from " + oldVersion + " to " + newVersion);
        if(oldVersion == 1) {
            editor.putLong(KEY_LAST_UPDATE_SUCCESS, 0);

            oldVersion = 2;
        }


    }

    private static void enforceMainThread() {
        if(D.D) {
            if(!VersionHelper.checkIfOnMainThread()) {
                Log.e("SharedPrefWrap", "getInstance called but not on main thread!!! " + Thread.currentThread().getName());
                throw new IllegalStateException("SharedPrefWrap not on main thread " + Thread.currentThread().getName());
            }
        }
    }

}
