package com.theshmuz.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.analytics.tracking.android.EasyTracker;
import com.theshmuz.app.loaders.SignInResult;
import com.theshmuz.app.util.JSONHelpers;
import com.theshmuz.app.util.ThreeTypes;
import com.theshmuz.app.util.Version11Helper;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class UpdateService extends Service {

    private static final int UPDATE_CONNECT_TIMEOUT = 15000;
    private static final int UPDATE_READ_TIMEOUT = 10000;

    public static final String UPDATE_CMD = "com.theshmuz.app.updates";

    private UpdateTask task;
    private UpdatorStatus updatorStatus;

    private LoginHelper mLoginHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        Shmuz app = (Shmuz) getApplication();
        ShmuzHelper dbh = app.db;
        updatorStatus = app.updator;
        mLoginHelper = app.getLoginHelper();

        sendStatus(UpdatorStatus.UPDATE_STATUS_STARTING);

        EasyTracker.getInstance().setContext(this);

        task = new UpdateTask(this, dbh, mLoginHelper);
        if(Build.VERSION.SDK_INT >= 11) Version11Helper.runAsyncTask(task);
        else task.execute();
    }

    private void sendStatus(int status) {
        updatorStatus.setStatus(status);
    }

    private void sendStatus(boolean success) {
        if(success) {
            sendStatus(UpdatorStatus.UPDATE_STATUS_SUCCESS);
        }
        else {
            sendStatus(UpdatorStatus.UPDATE_STATUS_FAILED);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(task != null){
            task.service = null;
            task = null;
        }
    }

    public static class UpdateTask extends AsyncTask<String, Void, Boolean> {
        public UpdateService service;
        private ShmuzHelper dbHelper;
        private LoginHelper mLoginHelper;

        public UpdateTask(UpdateService service, ShmuzHelper dbHelper, LoginHelper loginHelper) {
            this.service = service;
            this.dbHelper = dbHelper;
            this.mLoginHelper = loginHelper;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            if(D.D) Log.d("UpdateTask", "doInBackground Called");
            long startTime = 0;
            if(D.D) startTime = SystemClock.elapsedRealtime();

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            boolean shmuzResult = updateRoot(db, ShmuzHelper.TABLE_SHMUZ);

            boolean parshaResult = updateRoot(db, ShmuzHelper.TABLE_PARSHA);

            boolean seriesResult = updateRoot(db, ShmuzHelper.TABLE_SERIES);

            boolean seriesContentResult = updateRoot(db, ShmuzHelper.TABLE_SERIES_CONTENT);

            boolean finalResult = shmuzResult && parshaResult && seriesResult && seriesContentResult;
            if(D.D) Log.i("UpdateService", "Total Time: " + (SystemClock.elapsedRealtime() - startTime));
            if(D.D) Log.d("UpdateService", "Results " + shmuzResult + " " + parshaResult + " " + seriesResult + " " + seriesContentResult);

            return finalResult;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result == null) result = false;
            if(service != null) {
                service.task = null;
                service.sendStatus(result);
                if(result) {
                    SharedPrefWrap sharedPrefWrap = SharedPrefWrap.getInstance(service);
                    sharedPrefWrap.setLastUpdateSuccess(System.currentTimeMillis());
                }
                service.stopSelf();
            }
        }

        private static boolean updateRoot(SQLiteDatabase db, String tableName) {
            String sinceDate;
            boolean isFirstTime = false;
            Cursor sinceQuery = db.query(ShmuzHelper.TABLE_SINCES, null, ShmuzHelper.SID+"=?", new String[]{tableName}, null, null, null);
            if(sinceQuery.moveToFirst()) {
                sinceDate = sinceQuery.getString(sinceQuery.getColumnIndex(ShmuzHelper.LAST_MODIFIED));
            }
            else {
                sinceDate = "epoch";
                isFirstTime = true;
            }
            sinceQuery.close();

            final String url = new StringBuilder("http://theshmuzapi.appspot.com/api2/sync/")
                    .append(tableName)
                    .append("?since=")
                    .toString();

            JSONObject result = getAJsonUrl(url + sinceDate);
            String newDate = null;
            boolean hasNext = false;
            do {
                if(result == null) return false;
                try {
                    JSONArray content = result.getJSONArray("content");
                    int len = content.length();
                    if(len <= 0) {
                        if(D.D) Log.i("UpdateService - " + tableName, "Got empty result!");
                        hasNext = false;
                        continue;
                    }

                    newDate = result.getString("valid");
                    sinceDate = newDate;

                    db.beginTransaction();
                    try {
                        processResultContent(db, tableName, content, len);

                        db.setTransactionSuccessful();
                    }
                    finally {
                        db.endTransaction();
                    }

                    hasNext = result.getBoolean("hasNext");
                    if(hasNext) {
                        result = getAJsonUrl(url + sinceDate);
                    }
                }
                catch(JSONException e) {
                    if(D.D) Log.e("Update Parse - " + tableName, "JSON Exception", e);
                    return false;
                }
            } while(hasNext);

            if(newDate != null) {
                ContentValues cv = new ContentValues(2);
                cv.put(ShmuzHelper.SID, tableName);
                cv.put(ShmuzHelper.LAST_MODIFIED, newDate);
                db.replace(ShmuzHelper.TABLE_SINCES, null, cv);

                if(D.D) Log.i("UpdateService - " + tableName, "Completed Successfully");
            }

            return true;
        }

        private static void processResultContent(final SQLiteDatabase db, final String tableName, final JSONArray content, final int len) throws JSONException {
            for(int i = 0; i < len; i++) {
                JSONObject item = content.getJSONObject(i);

                boolean isActive = item.getBoolean("active");

                if(isActive) {
                    if(tableName.equals(ShmuzHelper.TABLE_SHMUZ) || tableName.equals(ShmuzHelper.TABLE_PARSHA)) {
                        processShmuzParshaOrSeriesC(db, tableName, item, false);
                    }
                    else if(tableName.equals(ShmuzHelper.TABLE_SERIES_CONTENT)) {
                        processShmuzParshaOrSeriesC(db, tableName, item, true);
                    }
                    else if(tableName.equals(ShmuzHelper.TABLE_SERIES)) {
                        processSeries(db, item);
                    }
                }
                else {
                    if(tableName.equals(ShmuzHelper.TABLE_SERIES_CONTENT)) {
                        deleteItem(db, tableName, item, true);
                    }
                    else {
                        deleteItem(db, tableName, item, false);
                    }
                }
            }
        }

        private static void processShmuzParshaOrSeriesC(SQLiteDatabase db, String table, JSONObject item, boolean isSeries) throws JSONException {
            // right now, all item types seem to have the same schema
            ContentValues values = new ContentValues();

            String itemId = item.getString("id"); //technically an integer...
            values.put(ShmuzHelper.SID, itemId);

            int itemSequence = item.getInt("sequence");
            values.put(ShmuzHelper.SEQUENCE, itemSequence);

            JSONObject itemDates = item.getJSONObject("dates");
            String itemModified = itemDates.getString("modified");
            values.put(ShmuzHelper.MODIFIED, itemModified);

            String sref = null;
            if(isSeries) {
                sref = item.getString("sref");
                values.put(ShmuzHelper.SERIES_REF, sref);
            }
            values.put(ShmuzHelper.TITLE, item.getString("title"));
            values.put(ShmuzHelper.CONTENT, item.getString("content"));

            JSONObject links = item.getJSONObject("links");
            values.put(ShmuzHelper.URL_ARTWORK, links.getString("artwork"));
            values.put(ShmuzHelper.URL_AUDIO, links.getString("audio"));
            values.put(ShmuzHelper.URL_VIDEO, links.getString("video"));
            values.put(ShmuzHelper.URL_PDF, links.getString("pdf"));

            String tableName;
            String whereClause;
            String[] whereArgs;
            if(isSeries) {
                tableName = ShmuzHelper.TABLE_SERIES_CONTENT;
                whereClause = ShmuzHelper.SERIES_REF + "=? AND " + ShmuzHelper.SID + "=?";
                whereArgs = new String[]{sref, itemId};
            } else {
                tableName = table;
                whereClause = ShmuzHelper.SID + "=?";
                whereArgs = new String[]{itemId};
            }

            int updRes = db.update(tableName,
                    values,
                    whereClause,
                    whereArgs);

            if(updRes <= 0) {
                db.insert(tableName, null, values);
            }
        }

        private static void processSeries(SQLiteDatabase db, JSONObject item) throws JSONException {
            String itemId = item.getString("id"); //technically an integer...
            int access = item.getInt("access");
            if(access > ShmuzHelper.MAX_ACCESS) {
                if(D.D) Log.i("UpdateService", "Skipping series " + itemId + " because access " + access + " > " + ShmuzHelper.MAX_ACCESS);
                return;
            }

            ContentValues cv = new ContentValues();
            cv.put(ShmuzHelper.SID, itemId);
            cv.put(ShmuzHelper.MODIFIED, item.getString("modified"));
            cv.put(ShmuzHelper.TITLE, item.getString("title"));
            cv.put(ShmuzHelper.URL_ARTWORK, item.getString("artwork"));
            cv.put(ShmuzHelper.ACCESS, item.getInt("access"));
            cv.put(ShmuzHelper.THUMBS, item.getBoolean("thumbs") ? 1 : 0);

            int updRes = db.update(ShmuzHelper.TABLE_SERIES,
                    cv,
                    ShmuzHelper.SID + "=?",
                    new String[]{itemId});

            if(updRes <= 0) {
                db.insert(ShmuzHelper.TABLE_SERIES, null, cv);
            }
        }

        private static void deleteItem(SQLiteDatabase db, String table, JSONObject item, boolean isSeries) throws JSONException {
            String itemId = item.getString("id"); //technically an integer...
            String tableName;
            String whereClause;
            String[] whereArgs;
            if(isSeries) {
                String sref = item.getString("sref");
                tableName = ShmuzHelper.TABLE_SERIES_CONTENT;
                whereClause = ShmuzHelper.SERIES_REF + "=? AND " + ShmuzHelper.SID + "=?";
                whereArgs = new String[]{sref, itemId};
            } else {
                tableName = table;
                whereClause = ShmuzHelper.SID + "=?";
                whereArgs = new String[]{itemId};
            }

            db.delete(tableName, whereClause, whereArgs);
        }

    }

    public static JSONObject getAJsonUrl(String url) {
        String result = getStringFromUrl(url);
        if(result == null) return null;

        try {
            JSONObject fullResp = (JSONObject) new JSONTokener(result).nextValue();
            int errorCode = fullResp.getInt("errorCode");
            if(errorCode == 0) {
                return fullResp;
            }
            else {
                if(D.D) Log.e("getAJsonUrl", "Bad errorCode in result == " + errorCode);
                return null;
            }
        } catch(JSONException e) {
            if(D.D) Log.e("getAJsonUrl", "Got except", e);
        } catch(ClassCastException e) {
            if(D.D) Log.e("getAJsonUrl", "Got except 2", e);
        }
        return null;
    }

    public static String getStringFromUrl(String url) {
        HttpURLConnection c = null;
        String theResult;

        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(UPDATE_CONNECT_TIMEOUT);
            c.setReadTimeout(UPDATE_READ_TIMEOUT);
            c.connect();
            int status = c.getResponseCode();

            if(status / 100 == 2) {
                theResult = readIt(c.getInputStream(), c.getContentLength());
                return theResult;
            }

        } catch (MalformedURLException e) {
            if(D.D) Log.e("getStringFromUrl", "Malformed?", e);
            return null;
        } catch (IOException e) {
            if(D.D) Log.e("getStringFromUrl", "IO Stuff", e);
            return null;
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {

                }
            }
        }
        return null;
    }

    /**
     * Read an InputStream into a string.
     * @param is the InputStream
     * @param actualLength The actual expected length, if we have one. If not -1.
     * @return The String or Null if anything went wrong...
     */
    public static String readIt(InputStream is, final int actualLength) {
        final char[] buffer = new char[1024];
        StringBuilder sb = new StringBuilder();
        Reader in = null;
        try {
            in = new InputStreamReader(is, "UTF-8");
            while(true) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0) break;
                sb.append(buffer, 0, rsz);
            }
        }
        catch (UnsupportedEncodingException e) {
            if(D.D) Log.e("readIt", "Unsupported Encoding", e);
            return null;
        }
        catch (IOException e) {
            if(D.D) Log.e("readIt", "IO Stuff", e);
            return null;
        }
        finally {
            try {
                if(in != null) in.close();
            } catch (IOException e) {

            }
        }

        if(actualLength > 0 && sb.length() != actualLength) {
            if(D.D) Log.e("UpdateTask", "Got bad response length: " + sb.length() + " / " + actualLength);
            return null;
        }

        return sb.toString();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
