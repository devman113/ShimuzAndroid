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

public class AccountSignupFragment extends Fragment implements View.OnClickListener {

    private AccountActivityCallbacks mCallbacks;

    private TextInputEditText viewUsername;
    private TextInputEditText viewPassword;
    private TextInputEditText viewFirstname;
    private TextInputEditText viewLastname;
    private TextInputEditText viewEmail;

    public static AccountSignupFragment newInstance() {
        AccountSignupFragment fragment = new AccountSignupFragment();
        return fragment;
    }

    public AccountSignupFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_account_signup, container, false);

        TextView loginLink = (TextView) rootView.findViewById(R.id.link_login);
        loginLink.setOnClickListener(this);

        Button signupButton = (Button) rootView.findViewById(R.id.btn_signup);
        signupButton.setOnClickListener(this);

        viewUsername = (TextInputEditText) rootView.findViewById(R.id.input_username);
        viewPassword = (TextInputEditText) rootView.findViewById(R.id.input_password);
        viewFirstname = (TextInputEditText) rootView.findViewById(R.id.input_fname);
        viewLastname = (TextInputEditText) rootView.findViewById(R.id.input_lname);
        viewEmail = (TextInputEditText) rootView.findViewById(R.id.input_email);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.link_login:
                if(mCallbacks != null) {
                    mCallbacks.switchToSignIn();
                }
                break;

            case R.id.btn_signup:
                boolean isGoodToGo = true;

                viewUsername.setError(null);
                viewFirstname.setError(null);
                viewLastname.setError(null);
                viewEmail.setError(null);
                viewPassword.setError(null);

                String username = viewUsername.getText().toString();
                username = username.trim();
                if(username.isEmpty()) {
                    viewUsername.setError("Username is required");
                    isGoodToGo = false;
                }

                String password = viewPassword.getText().toString();
                if(password.isEmpty()) {
                    viewPassword.setError("Password is required");
                    isGoodToGo = false;
                }
                else if(password.length() < 6) {
                    viewPassword.setError("Password must be at least 6 characters long");
                    isGoodToGo = false;
                }

                String firstName = viewFirstname.getText().toString();
                firstName = firstName.trim();
                if(firstName.isEmpty()) {
                    viewFirstname.setError("First name is required");
                    isGoodToGo = false;
                }

                String lastName = viewLastname.getText().toString();
                lastName = lastName.trim();
                if(lastName.isEmpty()) {
                    viewLastname.setError("Last name is required");
                    isGoodToGo = false;
                }

                String email = viewEmail.getText().toString();
                email = email.trim();
                if(email.isEmpty()) {
                    viewEmail.setError("Email is required");
                    isGoodToGo = false;
                }
                else if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    viewEmail.setError("Valid email is required");
                    isGoodToGo = false;
                }

                if(!isGoodToGo) return;
                if(mCallbacks != null) {
                    mCallbacks.loadSignUp(username, firstName, lastName, email, password);
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
