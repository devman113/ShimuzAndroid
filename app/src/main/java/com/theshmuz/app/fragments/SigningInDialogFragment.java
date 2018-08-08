package com.theshmuz.app.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;

import com.google.analytics.tracking.android.EasyTracker;
import com.theshmuz.app.D;
import com.theshmuz.app.R;

public class SigningInDialogFragment extends DialogFragment {

    public static SigningInDialogFragment newInstance() {
        return newInstance("Signing In...");
    }

    public static SigningInDialogFragment newInstance(String message) {
        Bundle args = new Bundle();
        args.putString("msg", message);

        SigningInDialogFragment frag = new SigningInDialogFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        ProgressDialog toReturn = new ProgressDialog(getActivity());

        toReturn.setCancelable(false);
        toReturn.setMessage(getArguments().getString("msg"));

        return toReturn;
    }

}