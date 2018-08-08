package com.theshmuz.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

public class D {

    public static final String BACKEND = "http://theshmuzapi.appspot.com/api";
    public static final String CONTACT_EMAIL = "Rebbe@theShmuz.com";
    public static final String USER_AGENT = "ShmuzAndroid/" + BuildConfig.VERSION_CODE;

    public static boolean necessaryToRemoveSSL() {
        // 4.4.2 per https://www.ssllabs.com/ssltest/analyze.html?d=theshmuz.com
        // but instead going one higher
        return Build.VERSION.SDK_INT <= 19;
    }

    public static String removeSslIfNecessary(String url) {
        if (url == null) {
            return null;
        }
        if (!necessaryToRemoveSSL()) {
            return url;
        }
        return url.replace("https://", "http://");
    }

    public static final boolean D = BuildConfig.DEBUG;

    public static final int TIMEOUT_MILLIS = 10000;

    public static final int MAX_PLAY_COUNTS = 4;

}