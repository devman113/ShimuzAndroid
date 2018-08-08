package com.theshmuz.app.loaders;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.theshmuz.app.D;
import com.theshmuz.app.Entry;
import com.theshmuz.app.ShmuzHelper;

public class ItemLoader extends AsyncTaskLoader<Entry> {

    private ShmuzHelper mHelper;
    private String mId;
    private String mType;

    private Entry mEntry;

    public ItemLoader(Context context, ShmuzHelper dbh, String type, String id) {
        super(context);
        mHelper = dbh;
        mId = id;
        mType = type;
    }

    @Override
    public Entry loadInBackground() {
        SQLiteDatabase db = mHelper.getWritableDatabase();

        String tableName;
        String selection;
        String[] selectionArgs;

        boolean isSeries = ShmuzHelper.isSeries(mType);
        if(isSeries) {
            tableName = ShmuzHelper.TABLE_SERIES_CONTENT;
            selection = ShmuzHelper.SERIES_REF + "=? AND " + ShmuzHelper.SID + "=?";
            selectionArgs = new String[]{mType, mId};
        }
        else {
            tableName = mType;
            selection = ShmuzHelper.SID + "=?";
            selectionArgs = new String[]{mId};
        }

        Cursor cursor = db.query(tableName,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null);

        Entry item = null;

        if(cursor.moveToFirst()) {
            item = new Entry();
            item.title = cursor.getString(cursor.getColumnIndex(ShmuzHelper.TITLE));
            item.content = cursor.getString(cursor.getColumnIndex(ShmuzHelper.CONTENT));

            item.startPosition = cursor.getInt(cursor.getColumnIndex(ShmuzHelper.POSITION));
            item.duration = cursor.getInt(cursor.getColumnIndex(ShmuzHelper.DURATION));

            String urlArtwork = cursor.getString(cursor.getColumnIndex(ShmuzHelper.URL_ARTWORK));
            if(urlArtwork == null || urlArtwork.isEmpty() || urlArtwork.equals("null")) urlArtwork = null;
            item.urlArtwork = urlArtwork;

            String urlAudio = cursor.getString(cursor.getColumnIndex(ShmuzHelper.URL_AUDIO));
            if(urlAudio == null || urlAudio.isEmpty() || urlAudio.equals("null")) urlAudio = null;
            item.urlAudio = urlAudio;

            String urlVideo = cursor.getString(cursor.getColumnIndex(ShmuzHelper.URL_VIDEO));
            if(urlVideo == null || urlVideo.isEmpty() || urlVideo.equals("null")) urlVideo = null;
            item.urlVideo = urlVideo;

            String urlPdf = cursor.getString(cursor.getColumnIndex(ShmuzHelper.URL_PDF));
            if(urlPdf == null || urlPdf.isEmpty() || urlPdf.equals("null")) urlPdf = null;
            item.urlPdf = urlPdf;

            item.downloadId = cursor.getLong(cursor.getColumnIndex(ShmuzHelper.DOWNLOAD_ID));
        }

        cursor.close();

        if(D.D) Log.d("ItemLoader", "Loaded " + item);

        return item;
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(Entry entry) {
        if (isReset()) {
            return;
        }

        Entry oldEntry = mEntry;
        mEntry = entry;

        if (isStarted()) {
            super.deliverResult(entry);
        }

    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (mEntry != null) {
            deliverResult(mEntry);
        }
        if (takeContentChanged() || mEntry == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Entry entry) {
        super.onCanceled(entry);
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        mEntry = null;
    }

}
