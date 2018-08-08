package com.theshmuz.app;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DownloadReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent == null) return;
        String action = intent.getAction();
        if(action == null) return;

        if (action.equals(DownloadManager.ACTION_NOTIFICATION_CLICKED)) {

        }
    }

}
