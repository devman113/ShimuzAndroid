package com.theshmuz.app;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class UpdatorStatus {

    private Context context;

    private int status;
    private int version;

    public static final int UPDATE_STATUS_INVALID = -1337;
    public static final int UPDATE_STATUS_STARTING = 0;
    public static final int UPDATE_STATUS_SUCCESS = 1;
    public static final int UPDATE_STATUS_FAILED = -1;

    public UpdatorStatus(Context appContext) {
        this.context = appContext.getApplicationContext();
    }

    public void setStatus(int status) {
        version++;
        this.status = status;
        sendUpdateBroadcast();
    }

    public void processStatusCheck(UpdatorConsumer consumer) {
        if(consumer.getUpdateVersion() != version) {
            consumer.processUpdate(version, status);
        }
    }

    public void bringUpToDate(UpdatorConsumer consumer) {
        if(consumer.getUpdateVersion() != version) {
            consumer.processUpdate(version, UPDATE_STATUS_INVALID);
        }
    }

    public interface UpdatorConsumer {
        int getUpdateVersion();
        void processUpdate(int newVersion, int newStatus);
    }

    public void sendUpdateBroadcast() {
        Intent broadcast = new Intent(UpdateService.UPDATE_CMD);
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcast);
    }
}
