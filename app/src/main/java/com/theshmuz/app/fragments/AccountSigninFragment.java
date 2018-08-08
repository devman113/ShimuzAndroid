package com.theshmuz.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.theshmuz.app.R;
import com.theshmuz.app.callbacks.AccountActivityCallbacks;

/**
 * Created by yossie on 2/16/17.
 */

public class AccountSigninFragment extends Fragment implements View.OnClickListener {

    private AccountActivityCallbacks mCallbacks;

    private TextInputEditText viewUsername;
    private TextInputEditText viewPassword;

    public static AccountSigninFragment newInstance() {
        AccountSigninFragment fragment = new AccountSigninFragment();
        return fragment;
    }

    public AccountSigninFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_account_signin, container, false);

        TextView forgotLink = (TextView) rootView.findViewById(R.id.link_forgotpass);
        forgotLink.setOnClickListener(this);

        Button loginButton = (Button) rootView.findViewById(R.id.btn_login);
        loginButton.setOnClickListener(this);

        viewUsername = (TextInputEditText) rootView.findViewById(R.id.input_username);
        viewPassword = (TextInputEditText) rootView.findViewById(R.id.input_password);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        String username, password;

        boolean goodToGo = true;

        switch(v.getId()) {
            case R.id.link_forgotpass:
                username = viewUsername.getText().toString();
                viewUsername.setError(null);
                viewPassword.setError(null);
                if(username.isEmpty()) {
                    viewUsername.setError("Username or email is required");
                    return;
                }
                if(mCallbacks != null) {
                    mCallbacks.forgotPassword(username);
                }
                break;

            case R.id.btn_login:
                viewUsername.setError(null);
                viewPassword.setError(null);

                username = viewUsername.getText().toString();
                username = username.trim();
                if(username.isEmpty()) {
                    viewUsername.setError("Username or email is required");
                    goodToGo = false;
                }

                password = viewPassword.getText().toString();
                if(password.isEmpty()) {
                    viewPassword.setError("Password is required");
                    goodToGo = false;
                }

                if(!goodToGo) return;
                if(mCallbacks != null) {
                    mCallbacks.loadSignIn(username, password);
                }
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (AccountActivityCallbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

}
