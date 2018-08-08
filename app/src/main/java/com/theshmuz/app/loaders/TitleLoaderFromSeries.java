package com.theshmuz.app.loaders;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.theshmuz.app.D;
import com.theshmuz.app.Entry;
import com.theshmuz.app.ShmuzHelper;

public class TitleLoaderFromSeries extends AsyncTaskLoader<String> {

    private ShmuzHelper mHelper;
    private String mType;

    private String mResult;

    public TitleLoaderFromSeries(Context context, ShmuzHelper dbh, String type) {
        super(context);
        mHelper = dbh;
        mType = type;
    }

    @Override
    public String loadInBackground() {
        SQLiteDatabase db = mHelper.getWritableDatabase();

        Cursor cursor = db.query(ShmuzHelper.TABLE_SERIES,
                null,
                ShmuzHelper.SID + "=?",
                new String[]{mType},
                null,
                null,
                null);

        String result = null;

        if(cursor.moveToFirst()){
            result = cursor.getString(cursor.getColumnIndex(ShmuzHelper.TITLE));
        }

        cursor.close();

        return result;
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(String entry) {
        if (isReset()) {
            return;
        }

        String oldEntry = mResult;
        mResult = entry;

        if (isStarted()) {
            super.deliverResult(entry);
        }

    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (mResult != null) {
            deliverResult(mResult);
        }
        if (takeContentChanged() || mResult == null) {
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
    public void onCanceled(String entry) {
        super.onCanceled(entry);
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        mResult = null;
    }

}
