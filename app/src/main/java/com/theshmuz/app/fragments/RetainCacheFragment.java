package com.theshmuz.app.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;

public class RetainCacheFragment extends Fragment {

    private static final String TAG = "RetainFragment";
    public LruCache<String, Bitmap> mRetainedCache;

    public RetainCacheFragment() {}

    public static RetainCacheFragment findOrCreateRetainFragment(FragmentManager fm) {
        RetainCacheFragment fragment = (RetainCacheFragment) fm.findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new RetainCacheFragment();
            fm.beginTransaction().add(fragment, TAG).commitAllowingStateLoss();
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}