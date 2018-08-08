package com.theshmuz.app.util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.theshmuz.app.D;

public class DiskCache {

    private Context context;
    private final byte[] mDiskCacheLock = new byte[0];
    private boolean mDiskCacheStarting = true;
    private DiskLruCache mDiskLruCache;
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 25; // 25MB
    private static final String DISK_CACHE_SUBDIR = ".pics";

    private static final int DISK_CACHE_INDEX = 0;
    private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
    private static final int DEFAULT_COMPRESS_QUALITY = 70;

    public DiskCache(Context context) {
        this.context = context.getApplicationContext();
    }

    private void initDiskCache() {
        if(D.D) Log.d("DiskCache", "going to init");
        // Set up disk cache
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
                File diskCacheDir = getDiskCacheDir(context, DISK_CACHE_SUBDIR);
                if(diskCacheDir == null){
                    if(D.D) Log.e("DiskCache", "Can't get directory");
                    return;
                }

                if (!diskCacheDir.exists()) {
                    diskCacheDir.mkdirs();
                }
                if (diskCacheDir.getUsableSpace() > DISK_CACHE_SIZE) {
                    try {
                        mDiskLruCache = DiskLruCache.open(
                                diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                        if (D.D) Log.d("DiskCache", "Disk cache initialized");
                    } catch (final IOException e) {
                        mDiskLruCache = null;
                        Log.e("DiskCache", "initDiskCache - " + e);
                    }
                }
            }
            mDiskCacheStarting = false;
        }
    }

    public void addBitmapToCache(String data, Bitmap bitmap) {
        if(data == null || bitmap == null) return;

        // Also add to disk cache
        synchronized (mDiskCacheLock) {
            if(mDiskCacheStarting) initDiskCache();

            if (mDiskLruCache != null) {
                final String key = hashKeyForDisk(data);
                OutputStream out = null;
                try {
                    DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot == null) {
                        final DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                        if (editor != null) {
                            out = editor.newOutputStream(DISK_CACHE_INDEX);
                            bitmap.compress(DEFAULT_COMPRESS_FORMAT, DEFAULT_COMPRESS_QUALITY, out);
                            editor.commit();
                            out.close();
                        }
                    } else {
                        snapshot.getInputStream(DISK_CACHE_INDEX).close();
                    }
                } catch (IOException e) {
                    Log.e("DiskCache", "addBitmapToCache - " + e);
                } catch (Exception e) {
                    Log.e("DiskCache", "addBitmapToCache - " + e);
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {}
                }
            }
        }
    }

    public Bitmap getBitmapFromDiskCache(String data) {
        final String key = hashKeyForDisk(data);
        Bitmap bitmap = null;

        synchronized (mDiskCacheLock) {
            if(mDiskCacheStarting) initDiskCache();
            if (mDiskLruCache != null) {
                InputStream inputStream = null;
                try {
                    final DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot != null) {
                        inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
                        if (inputStream != null) {
                            FileDescriptor fd = ((FileInputStream) inputStream).getFD();

                            // Decode bitmap, but we don't want to sample so give
                            // MAX_VALUE as the target dimensions
                            bitmap = BitmapFactory.decodeFileDescriptor(fd);
                            //                            bitmap = ImageResizer.decodeSampledBitmapFromDescriptor(
                            //                                    fd, Integer.MAX_VALUE, Integer.MAX_VALUE, this);
                        }
                    }
                } catch (final IOException e) {
                    Log.e("DiskCache", "getBitmapFromDiskCache - " + e);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {}
                }
            }
            return bitmap;
        }
    }


    public static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    // Creates a unique subdirectory of the designated app cache directory. Tries to use external
    // but if not mounted, falls back on internal storage.
    public static File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir

        boolean useExternal = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable();
        String cachePath = null;
        if (useExternal) {
            File extCacheDir = context.getExternalCacheDir();
            if (extCacheDir != null) {
                cachePath = extCacheDir.getPath();
            }
        }

        if (cachePath == null) {
            File dir = context.getCacheDir();
            if (dir == null) return null;
            cachePath = dir.getPath();
        }

        return new File(cachePath + File.separator + uniqueName);
    }

}
