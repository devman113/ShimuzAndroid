package com.theshmuz.app.util;

import android.os.Build;
import android.os.Looper;

/**
 * Created by yossie on 2/12/17.
 */

public class VersionHelper {

    public static boolean checkIfOnMainThread() {
        if(Build.VERSION.SDK_INT >= 23) {
            return Version23Helper.checkIfOnMainThread();
        }
        return (Thread.currentThread() == Looper.getMainLooper().getThread());
    }

}
