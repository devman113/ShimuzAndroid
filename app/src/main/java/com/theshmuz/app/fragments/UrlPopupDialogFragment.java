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
import android.util.Log;
import android.view.LayoutInflater;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.analytics.tracking.android.EasyTracker;
import com.theshmuz.app.D;
import com.theshmuz.app.business.Adver;

public class UrlPopupDialogFragment extends PopupDialogFragment {

    public static UrlPopupDialogFragment newInstance(String adId, String adUrl) {
        UrlPopupDialogFragment frag = new UrlPopupDialogFragment();

        Bundle bundle = new Bundle();
        bundle.putString("id", adId);
        bundle.putString("url", adUrl);
        frag.setArguments(bundle);

        return frag;
    }

    public UrlPopupDialogFragment() {}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();

        Bundle args = getArguments();
        final String adId = args.getString("id");
        String adUrl = args.getString("url");

        WebView view = new WebView(activity);

        WebViewClient newClient = new WebViewClient() {

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(url.startsWith("shmuz://")) {
                    Intent go = new Intent();
                    Uri uri = Uri.parse(url);
                }
                return false;
            }

        };
        view.setWebViewClient(newClient);

        view.loadUrl(adUrl);

        final SharedPreferences sp = this.sp;

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setNegativeButton("Done", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if(adId != null) {
                            sp.edit().putString(Adver.SP_LAST_AD, adId).apply();
                        }
                        if(!D.D) EasyTracker.getTracker().sendEvent("popup_dialog " + adId, "button_press", "Done", (long) 0);
                    }
                })
                .setCancelable(true)
                .create();
    }

}
