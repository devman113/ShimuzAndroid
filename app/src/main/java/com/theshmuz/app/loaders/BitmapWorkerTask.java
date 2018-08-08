package com.theshmuz.app.loaders;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import com.theshmuz.app.D;
import com.theshmuz.app.util.DiskCache;

public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

    public static final int TIMEOUT_MILLIS = 5000;

    private final WeakReference<ImageView> imageViewReference;
    private String url;

    private LruCache<String, Bitmap> mMemoryCache;
    private DiskCache mDiskCache;

    private boolean isDrawableAttached;

    public BitmapWorkerTask(ImageView imageView, LruCache<String, Bitmap> mMemoryCache, DiskCache diskCache, boolean isDrawableAttached) {
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<>(imageView);
        this.mMemoryCache = mMemoryCache;
        this.mDiskCache = diskCache;
        this.isDrawableAttached = isDrawableAttached;
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(String... params) {
        url = params[0];
        Bitmap bitmap = null;

        if(isCancelled()) return null;

        bitmap = mDiskCache.getBitmapFromDiskCache(url);
        if(bitmap != null) {
            putInMemCache(url, bitmap);
            return bitmap;
        }

        try {
            final HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, TIMEOUT_MILLIS);
            HttpConnectionParams.setSoTimeout(httpParameters, TIMEOUT_MILLIS);
            HttpProtocolParams.setUserAgent(httpParameters, D.USER_AGENT);

            HttpUriRequest request = new HttpGet(D.removeSslIfNecessary(url));
            HttpClient httpClient = new DefaultHttpClient(httpParameters);

            if(isCancelled()) return null;

            HttpResponse response = httpClient.execute(request);

            if(isCancelled()) return null;

            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                byte[] bytes = EntityUtils.toByteArray(entity);

                try {
                    bitmap = decodeBitmapFromByteArray(bytes);
                } catch (OutOfMemoryError e) {
                    if(D.D) Log.e("BitmapWorkerTask", "Got OutOfMemory!", e);
                    //TODO: retry?
                    return null;
                }

            }
        }
        catch(IOException e) {
            if(D.D) Log.e("Bitmap Download TASK", "Failed " + url, e);
        }

        if(bitmap != null) {
            putInMemCache(url, bitmap);
            mDiskCache.addBitmapToCache(url, bitmap);
        }

        return bitmap;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    private static Bitmap decodeBitmapFromByteArray(byte[] bytes) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    private void putInMemCache(String url, Bitmap bitmap) {
        if(mMemoryCache != null) {
            mMemoryCache.put(url, bitmap);
        }
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (isCancelled()) {
            bitmap = null;
        }

        if (imageViewReference != null && bitmap != null) {
            final ImageView imageView = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask =
                    getBitmapWorkerTask(imageView);
            if (this == bitmapWorkerTask && imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
            else if(!isDrawableAttached && imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    public static boolean cancelPotentialWork(String newUrl, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String url = bitmapWorkerTask.url;
            if (url == null || !url.equals(newUrl)) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }
}