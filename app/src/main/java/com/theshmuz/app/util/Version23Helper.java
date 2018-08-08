package com.theshmuz.app.util;

import android.annotation.TargetApi;
import android.os.Looper;

/**
 * Created by yossie on 2/12/17.
 */

@TargetApi(23)
public class Version23Helper {

    public static boolean checkIfOnMainThread() {
        return Looper.getMainLooper().isCurrentThread();
    }

}
