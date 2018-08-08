package com.theshmuz.app.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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

import java.lang.ref.WeakReference;

public class PlainPopupDialogFragment extends PopupDialogFragment {

    public static PlainPopupDialogFragment newInstance(String adId, String adUrl, String title, String text) {
        PlainPopupDialogFragment frag = new PlainPopupDialogFragment();

        Bundle bundle = new Bundle();
        bundle.putString("id", adId);
        bundle.putString("url", adUrl);
        bundle.putString("title", title);
        bundle.putString("text", text);
        frag.setArguments(bundle);

        return frag;
    }

    public PlainPopupDialogFragment() {}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();

        Bundle args = getArguments();
        final String adId = args.getString("id");
        final String adUrl = args.getString("url");
        String adTitle = args.getString("title");
        String adText = args.getString("text");

        final SharedPreferences sp = this.sp;
        final WeakReference<PlainPopupDialogFragment> ourFragment = new WeakReference<>(this);

        return new AlertDialog.Builder(getActivity())
                .setTitle(adTitle)
                .setMessage(adText)
                .setPositiveButton("More Information", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Intent openIntent = new Intent(Intent.ACTION_VIEW);
                        openIntent.setData(Uri.parse(adUrl));
                        PlainPopupDialogFragment ourFrag = ourFragment.get();
                        if(ourFrag != null && ourFrag.getActivity() != null) {
                            ourFrag.getActivity().startActivity(Intent.createChooser(openIntent, "More Information"));
                            sp.edit().putString(Adver.SP_LAST_AD, adId).apply();
                        }
                        else {
                            //analytics?
                        }
                    }
                })
                .setNeutralButton("Remind Me Later", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing, just dismiss...
                    }
                })
                .setNegativeButton("No, Thanks", new OnClickListener() {
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
