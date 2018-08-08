package com.theshmuz.app.util;

import com.theshmuz.app.UpdateService;
import com.theshmuz.app.activity.AccountActivity;
import com.theshmuz.app.loaders.AdTask;
import com.theshmuz.app.loaders.BitmapWorkerTask;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.Notification;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;

@SuppressLint("NewApi")
public class Version11Helper {

    public static void runAsyncTask(AdTask task) {
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void runAsyncTask(UpdateService.UpdateTask task) {
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void runAsyncTask(AccountActivity.AccountTask task, String... args) {
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
    }

    public static void runAsyncTask(BitmapWorkerTask task, String url) {
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
    }

    public static void makeNotifyVoid(Notification notification) {
        notification.tickerView = null;
    }

    public static Uri getDownload(DownloadManager downloadManager, long id) {
        return downloadManager.getUriForDownloadedFile(id);
    }

    public static void setSystemUiVisibility(View v, int status) {
        v.setSystemUiVisibility(status);
    }
}