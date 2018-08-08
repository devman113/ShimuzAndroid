package com.theshmuz.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.theshmuz.app.loaders.SignInResult;
import com.theshmuz.app.util.JSONHelpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * @author yossie
 *
 */
public class LoginHelper {

    public static final String KEY_IN = "l";
    public static final String KEY_COOKIE = "ck";
    public static final String KEY_DISPLAY_NAME = "dna";
    public static final String KEY_EMAIL = "em";
    public static final String KEY_UNAME = "uname";

    private SharedPreferences loginPref;

    private int dataVersion;

    public LoginHelper(Context appContext) {
        loginPref = appContext.getSharedPreferences("a2", 0);
    }

    public synchronized boolean isSignedIn() {
        return loginPref.getBoolean(KEY_IN, false);
    }
    public synchronized String getCookie() {
        return loginPref.getString(KEY_COOKIE, null);
    }
    public synchronized String getDisplayName() {return loginPref.getString(KEY_DISPLAY_NAME, "");}

    public synchronized void saveResult(SignInResult result) {
        SharedPreferences.Editor editor = loginPref.edit();
        editor.putBoolean(KEY_IN, result.isValid);
        if(result.displayName != null) {
            editor.putString(KEY_COOKIE, result.cookie);
            editor.putString(KEY_DISPLAY_NAME, result.displayName);
        }
        else {
            editor.remove(KEY_COOKIE);
            editor.remove(KEY_DISPLAY_NAME);
        }
        editor.commit();
        dataVersion++;
    }

    public synchronized void signOut() {
        SharedPreferences.Editor editor = loginPref.edit();
        editor.putBoolean(KEY_IN, false);
        editor.remove(KEY_COOKIE);
        editor.remove(KEY_DISPLAY_NAME);
        editor.apply();
        dataVersion++;
    }

    public interface DataSyncer {
        int syncGetLoginDataVersion();
        void syncSetLoginDataVersion(int version);
        void syncLoginSomethingChanged();
    }
    public synchronized void doInitialSync(DataSyncer dataSyncer) {
        dataSyncer.syncSetLoginDataVersion(dataVersion);
    }
    public synchronized void doDataSync(DataSyncer dataSyncer) {
        if(dataSyncer.syncGetLoginDataVersion() != dataVersion) {
            dataSyncer.syncSetLoginDataVersion(dataVersion);
            dataSyncer.syncLoginSomethingChanged();
        }
    }


    public static SignInResult doSignInAuth(final String userName, final String password) {
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair("username", userName));
            nameValuePairs.add(new BasicNameValuePair("password", password));
            nameValuePairs.add(new BasicNameValuePair("seconds", "220903200"));

            String url = "https://theshmuz.com/api/user/generate_auth_cookie";
            if (D.necessaryToRemoveSSL()) {
                url = "https://theshmuzapi.appspot.com/nusers/generate_auth_cookie";
            }
            JSONObject fullResp = JSONHelpers.getJSONPost(url, nameValuePairs, false);
            if(fullResp == null) return null;

            String status = fullResp.getString("status");
            if("ok".equalsIgnoreCase(status)) {
                String cookie = fullResp.getString("cookie");
                JSONObject userObj = fullResp.getJSONObject("user");
                if(userObj == null) {
                    if(D.D) Log.e("LoginHelper", "got null user object!");
                    return null;
                }
                String displayName = userObj.getString("displayname");
                return new SignInResult(cookie, displayName);
            }
            else if("error".equalsIgnoreCase(status)) {
                String errorMessage = fullResp.getString("error");
                return new SignInResult(errorMessage);
            }
            else {
                if(D.D) Log.e("LoginHelper", "Got unknown status: " + status);
                return null;
            }
        } catch (JSONException e) {
            if (D.D) Log.e("UpdateService", "JSON Parse Error");
            return null;
        }
    }

    public static String doForgotPassword(String userLogIn) {
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<>(1);
            nameValuePairs.add(new BasicNameValuePair("user_login", userLogIn));

            String url = "https://theshmuz.com/api/user/retrieve_password/";
            if (D.necessaryToRemoveSSL()) {
                url = "https://theshmuzapi.appspot.com/nusers/retrieve_password";
            }
            JSONObject fullResp = JSONHelpers.getJSONPost(url, nameValuePairs, false);
            if(fullResp == null) return null;

            String status = fullResp.getString("status");
            if("ok".equalsIgnoreCase(status)) {
                return "ok";
            }
            else if("error".equalsIgnoreCase(status)) {
                return fullResp.getString("error");
            }
            else {
                if(D.D) Log.e("LoginHelper", "Got unknown status: " + status);
                return null;
            }
        } catch (JSONException e) {
            if (D.D) Log.e("UpdateService", "JSON Parse Error");
            return null;
        }
    }

    public static SignInResult doSignUp(String username, String firstName, String lastName, String email, String password) {
        try {
            String url = "https://theshmuz.com/api/core/get_nonce/?controller=user&method=register";
            if (D.necessaryToRemoveSSL()) {
                url = "https://theshmuzapi.appspot.com/nusers/get_nonce";
            }
            JSONObject nonceObject = JSONHelpers.getJSONGet(url, false);
            if(nonceObject == null) {
                return null;
            }
            String nonceStatus = nonceObject.getString("status");
            if(!"ok".equalsIgnoreCase(nonceStatus)) {
                return null;
            }
            String nonce = nonceObject.getString("nonce");
            if(D.D) Log.d("LoginHelper", "Got nonce: " + nonce);

            String displayName = firstName + " " + lastName;

            List<NameValuePair> nameValuePairs = new ArrayList<>(8);
            nameValuePairs.add(new BasicNameValuePair("username", username));
            nameValuePairs.add(new BasicNameValuePair("email", email));
            nameValuePairs.add(new BasicNameValuePair("user_pass", password));
            nameValuePairs.add(new BasicNameValuePair("display_name", displayName));
            nameValuePairs.add(new BasicNameValuePair("first_name", firstName));
            nameValuePairs.add(new BasicNameValuePair("last_name", lastName));
            nameValuePairs.add(new BasicNameValuePair("notify", "both"));
            nameValuePairs.add(new BasicNameValuePair("nonce", nonce));
            nameValuePairs.add(new BasicNameValuePair("seconds", "220903200"));

            url = "https://theshmuz.com/api/user/register/";
            if (D.necessaryToRemoveSSL()) {
                url = "https://theshmuzapi.appspot.com/nusers/register";
            }
            JSONObject fullResp = JSONHelpers.getJSONPost(url, nameValuePairs, false);
            if(fullResp == null) return null;

            String status = fullResp.getString("status");
            if("ok".equalsIgnoreCase(status)) {
                String cookie = fullResp.getString("cookie");
                return new SignInResult(cookie, displayName);
            }
            else if("error".equalsIgnoreCase(status)) {
                String errorMessage = fullResp.getString("error");
                return new SignInResult(errorMessage);
            }
            else {
                if(D.D) Log.e("LoginHelper", "Got unknown status: " + status);
                return null;
            }
        } catch (JSONException e) {
            if (D.D) Log.e("UpdateService", "JSON Parse Error");
            return null;
        }
    }

}
