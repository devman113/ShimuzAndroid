package com.theshmuz.app.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.theshmuz.app.LoginHelper;
import com.theshmuz.app.R;
import com.theshmuz.app.Shmuz;
import com.theshmuz.app.activity.AccountActivity;
import com.theshmuz.app.callbacks.AccountActivityCallbacks;

public class AccountFragment extends Fragment implements OnClickListener {

    private LoginHelper mLoginHelper;

    private AccountActivityCallbacks mCallbacks = null;

    public static AccountFragment newInstance() {
        AccountFragment myFragment = new AccountFragment();
        return myFragment;
    }

    public AccountFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Shmuz app = (Shmuz) getActivity().getApplication();
        mLoginHelper = app.getLoginHelper();

        EasyTracker.getInstance().setContext(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_account, container, false);

        TextView welcomeText = (TextView) rootView.findViewById(R.id.welcomeText);
        String name = mLoginHelper.getDisplayName();
        if(name != null) {
            welcomeText.setText("Welcome, " + name);
        }
        else {
            welcomeText.setText("Welcome,");
        }

        Button signOut = (Button) rootView.findViewById(R.id.buttonSignOut);
        signOut.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.buttonSignOut:
                mLoginHelper.signOut();
                if(mCallbacks != null) {
                    mCallbacks.signOut();
                }
                break;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mCallbacks = (AccountActivityCallbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallbacks = null;
    }


}
