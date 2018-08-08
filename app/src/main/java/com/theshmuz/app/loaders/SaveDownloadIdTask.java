package com.theshmuz.app.loaders;

import com.theshmuz.app.ShmuzHelper;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

public class SaveDownloadIdTask extends AsyncTask<String, Void, Void>{

    private ShmuzHelper dbh;

    public SaveDownloadIdTask(ShmuzHelper dbh){
        this.dbh = dbh;
    }

    @Override
    protected Void doInBackground(String... params) {
        //need to get type, id, and position (as string for now...)
        String type = params[0];
        String id = params[1];
        long downloadId = Long.parseLong(params[2]);

        SQLiteDatabase db = dbh.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(ShmuzHelper.DOWNLOAD_ID, downloadId);

        boolean isSeries = ShmuzHelper.isSeries(type);
        String tableName;
        String whereClause;
        String[] whereArgs;
        if(isSeries) {
            tableName = ShmuzHelper.TABLE_SERIES_CONTENT;
            whereClause = ShmuzHelper.SERIES_REF + "=? AND " + ShmuzHelper.SID + "=?";
            whereArgs = new String[]{type, id};
        }
        else {
            tableName = type;
            whereClause = ShmuzHelper.SID + "=?";
            whereArgs = new String[]{id};
        }

        db.update(tableName, cv, whereClause, whereArgs);

        return null;
    }

}
