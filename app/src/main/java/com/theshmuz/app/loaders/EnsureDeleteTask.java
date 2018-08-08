package com.theshmuz.app.loaders;

import java.io.File;
import java.util.List;

import com.theshmuz.app.D;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

//fire and forget...
public class EnsureDeleteTask extends AsyncTask<Uri, Void, Void> {


    public EnsureDeleteTask(Context context) {

    }

    public static void deleteAllUris(List<Uri> uris) {
        for (Uri uri : uris) {
            if (uri == null) {
                continue;
            }

            if (D.D) Log.d("EnsureDeleteTask", "Gonna try to delete " + uri);

            String path = uri.getPath();
            if (path == null) {
                continue;
            }

            File file = new File(path);
            boolean deleted = file.delete();

            if(D.D) Log.i("EnsureDeleteTask", deleted + " " + path);
        }
    }

    @Override
    protected Void doInBackground(Uri... uris) {

        for (Uri uri : uris) {
            if (uri == null) {
                continue;
            }

            String path = uri.getPath();
            if (path == null) {
                continue;
            }

            File file = new File(path);
            boolean deleted = file.delete();

            if(D.D) Log.i("FILE DELETE TASK", deleted + " " + path);
        }

        return null;
    }

}
