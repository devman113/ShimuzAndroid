package com.theshmuz.app;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.theshmuz.app.loaders.EnsureDeleteTask;
import com.theshmuz.app.util.DownloadHelper;

import java.util.List;

public class ShmuzHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "shmuz.db";
    private static final int DATABASE_VERSION = 14;

    // Table name
    public static final String TABLE_SHMUZ = "shmuz";
    public static final String TABLE_ARTICLE_GONE = "article";
    public static final String TABLE_PARSHA = "parsha";

    public static final String TABLE_SERIES = "series";
    public static final String TABLE_SERIES_CONTENT = "seriesc";
    public static final String SERIES_REF = "sref";

    public static final String TABLE_SINCES = "sinces";

    public static final String TABLE_COUNTER = "counter";
    public static final String COUNT = "count";

    // Columns
    public static final String SID = "_id";
    public static final String SEQUENCE = "sequence";
    public static final String TITLE = "title";
    public static final String URL_ARTWORK = "artwork";
    public static final String URL_AUDIO = "audio";
    public static final String URL_VIDEO = "video";
    public static final String URL_PDF = "pdf";
    public static final String CONTENT = "content";
    public static final String POSITION = "position";
    public static final String DURATION = "duration";
    public static final String DOWNLOAD_ID = "download";

    // Columns - Series
    public static final String ACCESS = "access";
    public static final String MODIFIED = "modified";
    public static final String THUMBS = "thumbs";

    public static final int ACCESS_NONE = 0;
    public static final int ACCESS_SIGNIN = 1;
    public static final int ACCESS_PREMIUM = 2;
    public static final int ACCESS_EARLY = 3;
    public static final int MAX_ACCESS = ACCESS_EARLY;

    public static final String LAST_MODIFIED = "published";

    private Context mContext;


    public ShmuzHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //NOTE: ALL 2 MUST BE UPDATED SIMULTANEOUSLY
        String sql = "create table " + TABLE_SHMUZ + "( "
                + SID + " integer primary key, "
                + SEQUENCE + " integer, "
                + MODIFIED + " text, "
                + TITLE + " text not null, "
                + URL_ARTWORK + " text, "
                + URL_AUDIO + " text, "
                + URL_VIDEO + " text, "
                + URL_PDF + " text, "
                + CONTENT + " text, "
                + POSITION + " integer, "
                + DURATION + " integer, "
                + DOWNLOAD_ID + " integer "
                + ");";
        db.execSQL(sql);

        sql = "create table " + TABLE_PARSHA + "( "
                + SID + " integer primary key, "
                + SEQUENCE + " integer, "
                + MODIFIED + " text, "
                + TITLE + " text not null, "
                + URL_ARTWORK + " text, "
                + URL_AUDIO + " text, "
                + URL_VIDEO + " text, "
                + URL_PDF + " text, "
                + CONTENT + " text, "
                + POSITION + " integer, "
                + DURATION + " integer, "
                + DOWNLOAD_ID + " integer "
                + ");";
        db.execSQL(sql);

        sql = "create table " + TABLE_SERIES + "( "
                + SID + " integer primary key, "
                + TITLE + " text not null, "
                + URL_ARTWORK + " text, "
                + URL_VIDEO + " text, "
                + ACCESS + " integer, "
                + MODIFIED + " text, "
                + THUMBS + " integer "
                + ");";
        db.execSQL(sql);

        sql = "create table " + TABLE_SERIES_CONTENT + "( "
                + SID + " integer, "
                + SEQUENCE + " integer, "
                + MODIFIED + " text, "
                + SERIES_REF + " integer not null, "
                + TITLE + " text not null, "
                + URL_ARTWORK + " text, "
                + URL_AUDIO + " text, "
                + URL_VIDEO + " text, "
                + URL_PDF + " text, "
                + CONTENT + " text, "
                + POSITION + " integer, "
                + DURATION + " integer, "
                + DOWNLOAD_ID + " integer "
                + ");";
        db.execSQL(sql);


        sql = "create table " + TABLE_COUNTER + "( "
                + SID + " text primary key, "
                + COUNT + " integer default 0 "
                + ");";
        db.execSQL(sql);

        sql = "create table " + TABLE_SINCES + "( "
                + SID + " text primary key, "
                + LAST_MODIFIED + " text "
                + ");";
        db.execSQL(sql);
    }

    private static String fixTableName(String tableName) {
        if(Character.isDigit(tableName.charAt(0))) {
            return "[" + tableName + "]";
        }
        return tableName;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(D.D) Log.i("SHMUZ SQLite", "onUpgrade Called " + oldVersion + " / " + newVersion);

        if(oldVersion == 1) {
            String sql = "alter table " + TABLE_SHMUZ
                    + " add column " + POSITION + " integer;";
            db.execSQL(sql);

            sql = "alter table " + TABLE_PARSHA
                    + " add column " + POSITION + " integer;";
            db.execSQL(sql);

            oldVersion = 2;
        }

        if(oldVersion == 2) {
            String sql = "alter table " + TABLE_SHMUZ
                    + " add column " + DOWNLOAD_ID + " integer;";
            db.execSQL(sql);

            sql = "alter table " + TABLE_PARSHA
                    + " add column " + DOWNLOAD_ID + " integer;";
            db.execSQL(sql);

            oldVersion = 3;
        }

        if(oldVersion == 3) {
            String sql = "alter table " + TABLE_SHMUZ
                    + " add column " + URL_PDF + " text;";
            db.execSQL(sql);

            sql = "alter table " + TABLE_PARSHA
                    + " add column " + URL_PDF + " text;";
            db.execSQL(sql);

            //reset sinces...
            db.delete(TABLE_SINCES, null, null);

            oldVersion = 4;
        }

        if(oldVersion == 4) {
            String sql = "create table " + TABLE_SERIES + "( "
                    + SID + " integer primary key, "
                    + TITLE + " text not null, "
                    + URL_ARTWORK + " text, "
                    + ACCESS + " integer, "
                    + MODIFIED + " text "
                    + ");";

            db.execSQL(sql);

            oldVersion = 5;
        }

        if(oldVersion == 5) {
            String sql = "drop table " + TABLE_ARTICLE_GONE + ";";

            oldVersion = 6;
        }

        if(oldVersion == 6) {

            String sql = "alter table " + TABLE_SERIES
                    + " add column " + THUMBS + " integer;";
            db.execSQL(sql);

            sql = "UPDATE " + TABLE_SERIES + " SET " + MODIFIED + "='0'";
            db.execSQL(sql);

            oldVersion = 7;
        }

        if(oldVersion == 7) {

            String sql = "alter table " + TABLE_SHMUZ
                    + " add column " + DURATION + " integer;";
            db.execSQL(sql);

            sql = "alter table " + TABLE_PARSHA
                    + " add column " + DURATION + " integer;";
            db.execSQL(sql);

            String[] cols = new String[]{ShmuzHelper.SID};
            Cursor cursor = db.query(TABLE_SERIES, cols, null, null, null, null, null);
            while (cursor.moveToNext()) {

                sql = "alter table " + fixTableName(cursor.getString(cursor.getColumnIndex(SID)))
                        + " add column " + DURATION + " integer;";
                db.execSQL(sql);

            }

            oldVersion = 8;
        }

        if(oldVersion == 8) {

            String sql = "create table " + TABLE_COUNTER + "( "
                    + SID + " text primary key, "
                    + COUNT + " integer default 0 "
                    + ");";
            db.execSQL(sql);

            oldVersion = 9;
        }

        if(oldVersion == 9) {

            String sql = "alter table " + TABLE_SERIES
                    + " add column " + URL_VIDEO + " text;";
            db.execSQL(sql);

            oldVersion = 10;
        }

        // *** BREAKPOINT ***

        if(oldVersion == 10) {
            if (D.D) Log.d("ShmuzHelper", "Going to do series migration");
            migrateSeriesToOneTable(db, mContext);

            oldVersion = 11;
        }

        if(oldVersion == 11) {
            String sql;
            sql = "alter table " + TABLE_SHMUZ + " add column " + MODIFIED + " text;";
            db.execSQL(sql);
            sql = "alter table " + TABLE_SERIES_CONTENT + " add column " + MODIFIED + " text;";
            db.execSQL(sql);
            sql = "alter table " + TABLE_PARSHA + " add column " + MODIFIED + " text;";
            db.execSQL(sql);

            oldVersion = 12;
        }

        if(oldVersion == 12) {
            if (D.D) Log.d("ShmuzHelper", "Going to do series ID migration");
            migrateToNewIds(db, mContext);

            oldVersion = 13;
        }

        if(oldVersion == 13) {
            //reset sinces...
            db.delete(TABLE_SINCES, null, null);

            String sql;
            sql = "alter table " + TABLE_SHMUZ + " add column " + SEQUENCE + " integer;";
            db.execSQL(sql);
            sql = "alter table " + TABLE_SERIES_CONTENT + " add column " + SEQUENCE + " integer;";
            db.execSQL(sql);
            sql = "alter table " + TABLE_PARSHA + " add column " + SEQUENCE + " integer;";
            db.execSQL(sql);

            sql = "update " + TABLE_SHMUZ + " set " + SEQUENCE + " = " + SID;
            db.execSQL(sql);

            oldVersion = 14;
        }

    }

    private static void tryOurBestToRemoveOrphanDownloads(Context context, SQLiteDatabase db, String tableName) {
        if (D.D) Log.d("ShmuzHelper", "tryOurBestToRemoveOrphanDownloads for table: " + tableName);
        try {
            tryOurBestToRemoveOrphanDownloadsInternal(context, db, tableName);
        } catch (Exception e) {
            if (D.D) Log.e("ShmuzHelper", "tryOurBestToRemoveOrphanDownloads failed", e);
        }
    }

    // pass in a tableName of a table that you are going to clear...
    private static void tryOurBestToRemoveOrphanDownloadsInternal(Context context, SQLiteDatabase db, String tableName) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        long[] ids = null;

        Cursor cursor = db.query(tableName, new String[]{DOWNLOAD_ID}, DOWNLOAD_ID + ">0", null, null, null, null);
        try {
            int dColIndex = cursor.getColumnIndex(DOWNLOAD_ID);
            if (dColIndex == -1) {
                if (D.D) throw new RuntimeException();
                return;
            }
            int numRows = cursor.getCount();
            if (D.D) Log.d("ShmuzHelper", "tryOurBestToRemoveOrphanDownloads count: " + numRows);
            if (numRows == 0) return;

            ids = new long[numRows];
            for (int i = 0; i < numRows; i++) {
                if (!cursor.moveToNext()) {
                    if (D.D) throw new RuntimeException();
                    break;
                }
                ids[i] = cursor.getLong(dColIndex);
                if (D.D && ids[i] <= 0) throw new RuntimeException();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (ids != null) {
            List<Uri> allUris = DownloadHelper.getDownloads(downloadManager, ids);

            downloadManager.remove(ids);
            EnsureDeleteTask.deleteAllUris(allUris);
        }
    }

    private static boolean migrateSeriesToOneTable(SQLiteDatabase db, Context context) {
        //migrate old series to one master table of series
        String sql = "create table " + TABLE_SERIES_CONTENT + "( "
                + SID + " integer, "
                + SERIES_REF + " integer not null, "
                + TITLE + " text not null, "
                + URL_ARTWORK + " text, "
                + URL_AUDIO + " text, "
                + URL_VIDEO + " text, "
                + URL_PDF + " text, "
                + CONTENT + " text, "
                + POSITION + " integer, "
                + DURATION + " integer, "
                + DOWNLOAD_ID + " integer "
                + ");";
        db.execSQL(sql);

        String[] cols = new String[]{ShmuzHelper.SID};

        Cursor cursor = db.query(ShmuzHelper.TABLE_SERIES, cols, null, null, null, null, null);
        int indexSid = cursor.getColumnIndex(ShmuzHelper.SID);
        while(cursor.moveToNext()) {
            String seriesId = cursor.getString(indexSid);
            String seriesTable = fixTableName(seriesId);

            tryOurBestToRemoveOrphanDownloads(context, db, seriesTable);
            db.execSQL("DROP TABLE IF EXISTS " + seriesTable + ";");
        }
        cursor.close();

        return true;
    }

    private static boolean migrateToNewIds(SQLiteDatabase db, Context context) {
        tryOurBestToRemoveOrphanDownloads(context, db, TABLE_PARSHA);
        tryOurBestToRemoveOrphanDownloads(context, db, TABLE_SERIES_CONTENT);
        db.execSQL("DELETE FROM " + TABLE_PARSHA);
        db.execSQL("DELETE FROM " + TABLE_SERIES);
        db.execSQL("DELETE FROM " + TABLE_SERIES_CONTENT);
        return true;
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(!D.D) {
            super.onDowngrade(db, oldVersion, newVersion);
        }
        else {
            //okay !
            Log.w("ShmuzHelper", "Downgrading from " + oldVersion + " to " + newVersion);
        }
    }

    public static boolean isSeries(String type) {
        if(type == null) throw new IllegalArgumentException("may not be null");
        return Character.isDigit(type.charAt(0)) || type.charAt(0) == '-';
    }

}
