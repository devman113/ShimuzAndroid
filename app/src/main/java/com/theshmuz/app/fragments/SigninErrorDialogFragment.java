package com.theshmuz.app.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class SigninErrorDialogFragment extends DialogFragment {

    public static SigninErrorDialogFragment newInstance(String errMsg) {
        SigninErrorDialogFragment frag = new SigninErrorDialogFragment();

        Bundle args = new Bundle();
        args.putString("msg", errMsg);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle args = getArguments();
        String msg = args.getString("msg");

        return new AlertDialog.Builder(getActivity())
                .setMessage(msg)
                .setNegativeButton("Okay", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setCancelable(true)
                .create();

    }

}