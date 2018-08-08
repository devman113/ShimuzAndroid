package com.theshmuz.app.loaders;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;

import com.theshmuz.app.LoginHelper;
import com.theshmuz.app.ShmuzHelper;
import com.theshmuz.app.business.LoaderResultObj;
import com.theshmuz.app.fragments.ParshaFragment;

public class LatestParshaLoader extends AsyncTaskLoader<LoaderResultObj> {

    private ShmuzHelper mHelper;

    private LoaderResultObj mEntry;

    public LatestParshaLoader(Context context, ShmuzHelper helper) {
        super(context);
        mHelper = helper;
    }

    @Override
    public LoaderResultObj loadInBackground() {

        LoaderResultObj result = new LoaderResultObj();

        SQLiteDatabase db = mHelper.getWritableDatabase();
        String[] cols = new String[]{ShmuzHelper.SID, ShmuzHelper.TITLE, ShmuzHelper.URL_ARTWORK};

        Cursor cursor = db.query(ShmuzHelper.TABLE_PARSHA,
                cols,
                null,
                null,
                null,
                null,
                ShmuzHelper.SID + " DESC",
                "1");

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                result.parshaId = cursor.getString(cursor.getColumnIndex(ShmuzHelper.SID));
                result.parshaTitle = cursor.getString(cursor.getColumnIndex(ShmuzHelper.TITLE));
                int artIndex = cursor.getColumnIndex(ShmuzHelper.URL_ARTWORK);
                String artUrl = null;
                if(!cursor.isNull(artIndex)) {
                    artUrl = cursor.getString(artIndex);
                }
                if(artUrl != null && !artUrl.equals("null")) {
                    result.parshaArtwork = artUrl;
                }
            }

            cursor.close();
        }

        return result;

    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(LoaderResultObj entry) {
        if (isReset()) {
            return;
        }

        LoaderResultObj oldEntry = mEntry;
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
    public void onCanceled(LoaderResultObj entry) {
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
