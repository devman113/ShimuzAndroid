package com.theshmuz.app.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.theshmuz.app.D;
import com.theshmuz.app.LoginHelper;
import com.theshmuz.app.R;
import com.theshmuz.app.SharedPrefWrap;
import com.theshmuz.app.Shmuz;
import com.theshmuz.app.callbacks.AccountActivityCallbacks;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by yossie on 2/12/17.
 */

public class ExplainSignUpFragment extends Fragment implements View.OnClickListener {

    private SharedPrefWrap sharedPrefWrap;

    private AccountActivityCallbacks activityCallbacks;

    public static ExplainSignUpFragment newInstance() {
        ExplainSignUpFragment fragment = new ExplainSignUpFragment();
        return fragment;
    }

    public ExplainSignUpFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Shmuz app = (Shmuz) getActivity().getApplication();
        sharedPrefWrap = SharedPrefWrap.getInstance(app);

        EasyTracker.getInstance().setContext(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_account_explain, container, false);

        TextView explainTextView = (TextView) rootView.findViewById(R.id.explainText);
        explainTextView.setText(loadExplainText());

        Button signUpButton = (Button) rootView.findViewById(R.id.buttonSignUp);
        Button signInButton = (Button) rootView.findViewById(R.id.buttonSignIn);
        TextView skipButton = (TextView) rootView.findViewById(R.id.buttonSkip);

        int playCount = sharedPrefWrap.getPlayCount();
        boolean forceSignup = playCount >= D.MAX_PLAY_COUNTS;
        if(forceSignup) {
            skipButton.setVisibility(View.GONE);
        }

        signUpButton.setOnClickListener(this);
        signInButton.setOnClickListener(this);
        skipButton.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.buttonSignIn:
                activityCallbacks.switchToSignIn();
                break;

            case R.id.buttonSignUp:
                activityCallbacks.switchToSignUp();
                break;

            case R.id.buttonSkip:
                activityCallbacks.skipForNow();
                break;
        }
    }

    private String loadExplainText() {
        try {
            Resources res = getResources();
            InputStream in_s = res.openRawResource(R.raw.explain_signup);

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            return new String(b);
        } catch (IOException e) {
            if(D.D) Log.e("ExplainSignupFragment", "Unable to load explain text!", e);
            return null;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activityCallbacks = (AccountActivityCallbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activityCallbacks = null;
    }
}
