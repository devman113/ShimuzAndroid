package com.theshmuz.app.util;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import java.util.LinkedList;
import java.util.List;

public class DownloadHelper {
    /**
     * Uses the Version11Helper if it can!
     * @return Uri or null!
     */
    public static Uri getDownload(DownloadManager downloadManager, long id) {
        if(id <= 0) return null;

        if(Build.VERSION.SDK_INT >= 11) {
            return Version11Helper.getDownload(downloadManager, id);
        }

        Query query = new Query().setFilterById(id);
        Cursor cursor = null;
        try {
            cursor = downloadManager.query(query);
            if (cursor == null) {
                return null;
            }
            if (cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if(status == DownloadManager.STATUS_SUCCESSFUL) {
                    int UriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    if (cursor.isNull(UriIndex)) return null;
                    String url = cursor.getString(UriIndex);
                    Uri uri = Uri.parse(url);
                    return uri;
                }
                else {
                    return null;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static List<Uri> getDownloads(DownloadManager downloadManager, long... ids) {
        List<Uri> uriList = new LinkedList<>();

        Query query = new Query().setFilterById(ids);
        Cursor cursor = null;
        try {
            cursor = downloadManager.query(query);
            if (cursor == null) {
                return null;
            }
            int iStatus = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);
            int iUri = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            while (cursor.moveToNext()) {
                int status = cursor.getInt(iStatus);
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    if (cursor.isNull(iUri)) {
                        continue;
                    }
                    String url = cursor.getString(iUri);
                    Uri uri = Uri.parse(url);
                    uriList.add(uri);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return uriList;
    }

}
