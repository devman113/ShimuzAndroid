package com.theshmuz.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.google.analytics.tracking.android.EasyTracker;
import com.theshmuz.app.D;
import com.theshmuz.app.LoginHelper;
import com.theshmuz.app.R;
import com.theshmuz.app.Shmuz;
import com.theshmuz.app.callbacks.AccountActivityCallbacks;
import com.theshmuz.app.fragments.AccountFragment;
import com.theshmuz.app.fragments.AccountSigninFragment;
import com.theshmuz.app.fragments.AccountSignupFragment;
import com.theshmuz.app.fragments.ExplainSignUpFragment;
import com.theshmuz.app.fragments.SigninErrorDialogFragment;
import com.theshmuz.app.fragments.SigningInDialogFragment;
import com.theshmuz.app.loaders.SignInResult;
import com.theshmuz.app.util.Version11Helper;

/**
 * Created by yossie on 2/12/17.
 */

public class AccountActivity extends AppCompatActivity implements AccountActivityCallbacks {

    public AccountTask accountTask;

    private LoginHelper mLoginHelper;

    public static Intent createIntent(Activity context) {
        Intent intent = new Intent(context, AccountActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        Shmuz app = (Shmuz) getApplication();
        mLoginHelper = app.getLoginHelper();

        if(savedInstanceState == null) {
            checkProperFragIsShowingAndReplaceIfNecessary();
        }
    }

    private void checkProperFragIsShowingAndReplaceIfNecessary() {
        Fragment fragmentToDisplay;
        if(mLoginHelper.isSignedIn()) {
            fragmentToDisplay = AccountFragment.newInstance();
        }
        else {
            fragmentToDisplay = ExplainSignUpFragment.newInstance();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragmentToDisplay)
                .commit();
    }

    @Override
    public void loadSignIn(String userName, String password) {
        if(accountTask != null) {
            accountTask.cancel(true);
            accountTask = null;
        }

        accountTask = new SignInTask(this, mLoginHelper);
        if(Build.VERSION.SDK_INT >= 11) Version11Helper.runAsyncTask(accountTask, userName, password);
        else accountTask.execute(userName, password);

        DialogFragment newFragment = SigningInDialogFragment.newInstance("Signing in...");
        newFragment.show(getSupportFragmentManager(), "signing");
    }

    public void signInDone(SignInResult result) {
        Fragment prev = getSupportFragmentManager().findFragmentByTag("signing");
        if (prev != null) {
            DialogFragment df = (DialogFragment) prev;
            df.dismiss();
        }

        if(result != null) {
            if(result.isValid) {
                if(!D.D) EasyTracker.getTracker().sendEvent("accounts", "sign_in", "success", (long) 0);
                Intent startedWith = getIntent();
                boolean shouldFinish = startedWith.getBooleanExtra("finish", true);
                if(shouldFinish) {
                    Intent mainActivity = new Intent(this, MainActivity.class);
                    startActivity(mainActivity);
                    finish();
                }
                else {
                    //just readjust...
                    checkProperFragIsShowingAndReplaceIfNecessary();
                }
            }
            else {
                String message = result.error;

                if(!D.D) EasyTracker.getTracker().sendEvent("accounts", "sign_in", "Invalid: " + message, (long) 0);
                if(message == null) {
                    message = "An unknown error has occured";
                }
                showErrorDialog(message);
            }
        }
        else {
            if(!D.D) EasyTracker.getTracker().sendEvent("accounts", "sign_in", "Server Error", (long) 0);
            showErrorDialog("Can't access the server. Please try again soon.");
        }
    }

    @Override
    public void showError(String error) {
        showErrorDialog(error);
    }

    @Override
    public void forgotPassword(String emailAddress) {
        if(accountTask != null) {
            accountTask.cancel(true);
            accountTask = null;
        }
        accountTask = new ForgotPasswordTask(this, mLoginHelper);

        if(Build.VERSION.SDK_INT >= 11) Version11Helper.runAsyncTask(accountTask, emailAddress);
        else accountTask.execute(emailAddress);

        DialogFragment newFragment = SigningInDialogFragment.newInstance("Resetting password...");
        newFragment.show(getSupportFragmentManager(), "signing");
    }

    public void forgotPasswordDone(String result) {
        Fragment prev = getSupportFragmentManager().findFragmentByTag("signing");
        if (prev != null) {
            DialogFragment df = (DialogFragment) prev;
            df.dismiss();
        }

        if(result != null) {
            showErrorDialog(result);
        }
        else {
            if(!D.D) EasyTracker.getTracker().sendEvent("accounts", "sign_in", "Server Error", (long) 0);
            showErrorDialog("Can't access the server. Please try again soon.");
        }
    }

    @Override
    public void loadSignUp(String username, String firstName, String lastName, String email, String password) {
        if(accountTask != null) {
            accountTask.cancel(true);
            accountTask = null;
        }
        accountTask = new SignUpTask(this, mLoginHelper);

        if(Build.VERSION.SDK_INT >= 11) Version11Helper.runAsyncTask(accountTask, username, firstName, lastName, email, password);
        else accountTask.execute(username, firstName, lastName, email, password);

        DialogFragment newFragment = SigningInDialogFragment.newInstance("Creating account...");
        newFragment.show(getSupportFragmentManager(), "signing");
    }

    public void signUpDone(SignInResult result) {
        Fragment prev = getSupportFragmentManager().findFragmentByTag("signing");
        if (prev != null) {
            DialogFragment df = (DialogFragment) prev;
            df.dismiss();
        }

        if(result != null) {
            if(result.isValid) {
                if(!D.D) EasyTracker.getTracker().sendEvent("accounts", "sign_up", "success", (long) 0);
                Intent startedWith = getIntent();
                boolean shouldFinish = startedWith.getBooleanExtra("finish", true);
                if(shouldFinish) {
                    Intent mainActivity = new Intent(this, MainActivity.class);
                    startActivity(mainActivity);
                    finish();
                }
                else {
                    //just readjust...
                    checkProperFragIsShowingAndReplaceIfNecessary();
                }
            }
            else {
                String message = result.error;

                if(!D.D) EasyTracker.getTracker().sendEvent("accounts", "sign_up", "Invalid: " + message, (long) 0);
                if(message == null) {
                    message = "An unknown error has occured";
                }
                showErrorDialog(message);
            }
        }
        else {
            if(!D.D) EasyTracker.getTracker().sendEvent("accounts", "sign_up", "Server Error", (long) 0);
            showErrorDialog("Can't access the server. Please try again soon.");
        }
    }

    @Override
    public void signOut() {
        checkProperFragIsShowingAndReplaceIfNecessary();
    }

    @Override
    public void switchToSignIn() {
        Fragment fragmentToDisplay = AccountSigninFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragmentToDisplay)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void switchToSignUp() {
        Fragment fragmentToDisplay = AccountSignupFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragmentToDisplay)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void skipForNow() {
        Intent mainActivity = new Intent(this, MainActivity.class);
        mainActivity.putExtra("skip", true);
        startActivity(mainActivity);
        finish();
    }

    public static abstract class AccountTask extends AsyncTask<String, Void, Object> {
        public AccountActivity mActivity;
        protected LoginHelper mLoginHelper;

        public AccountTask(AccountActivity accountActivity, LoginHelper loginHelper) {
            this.mActivity = accountActivity;
            this.mLoginHelper = loginHelper;
        }

    }

    public static class SignInTask extends AccountTask {
        public SignInTask(AccountActivity accountActivity, LoginHelper loginHelper) {
            super(accountActivity, loginHelper);
        }

        @Override
        protected SignInResult doInBackground(String... params) {
            SignInResult result = LoginHelper.doSignInAuth(params[0], params[1]);

            if(result != null) {
                mLoginHelper.saveResult(result);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Object result) {
            if(mActivity == null) return;
            mActivity.signInDone((SignInResult) result);
            if(mActivity.accountTask == this) {
                mActivity.accountTask = null;
            }
            mActivity = null;
        }
    }

    public static class ForgotPasswordTask extends AccountTask {
        public ForgotPasswordTask(AccountActivity accountActivity, LoginHelper loginHelper) {
            super(accountActivity, loginHelper);
        }

        @Override
        protected String doInBackground(String... params) {
            return LoginHelper.doForgotPassword(params[0]);
        }

        @Override
        protected void onPostExecute(Object result) {
            if(mActivity == null) return;
            mActivity.forgotPasswordDone((String) result);
            if(mActivity.accountTask == this) {
                mActivity.accountTask = null;
            }
            mActivity = null;
        }
    }

    public static class SignUpTask extends AccountTask {
        public SignUpTask(AccountActivity accountActivity, LoginHelper loginHelper) {
            super(accountActivity, loginHelper);
        }

        @Override
        protected SignInResult doInBackground(String... params) {
            SignInResult result = LoginHelper.doSignUp(params[0], params[1], params[2], params[3], params[4]);

            if(result != null) {
                mLoginHelper.saveResult(result);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Object result) {
            if(mActivity == null) return;
            mActivity.signUpDone((SignInResult) result);
            if(mActivity.accountTask == this) {
                mActivity.accountTask = null;
            }
            mActivity = null;
        }
    }

    private void showErrorDialog(String message) {
        //and now show
        DialogFragment newFragment = SigninErrorDialogFragment.newInstance(message);
        newFragment.show(getSupportFragmentManager(), "signerr");
    }

}
