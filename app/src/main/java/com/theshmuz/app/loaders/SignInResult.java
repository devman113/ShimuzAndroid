package com.theshmuz.app.loaders;

public class SignInResult {

    public boolean isPremium;

    public boolean isValid;

    public String error;

    public String cookie;
    public String displayName;

    public SignInResult(String error) {
        this.isValid = false;
        this.error = error;
    }

    public SignInResult(String cookie, String displayName) {
        this.isValid = true;
        this.cookie = cookie;
        this.displayName = displayName;

    }

}