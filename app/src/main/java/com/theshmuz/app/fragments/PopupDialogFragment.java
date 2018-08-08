package com.theshmuz.app.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.analytics.tracking.android.EasyTracker;
import com.theshmuz.app.D;
import com.theshmuz.app.business.Adver;

public class PopupDialogFragment extends DialogFragment {

    protected SharedPreferences sp;

    public PopupDialogFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        String adId = getArguments().getString("id");

        if(D.D) Log.d("PopupDialogFragment " + adId, "onCancel called now");

        //do nothing for now...
    }

}
