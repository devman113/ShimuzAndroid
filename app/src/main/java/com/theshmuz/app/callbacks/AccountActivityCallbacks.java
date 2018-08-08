package com.theshmuz.app.callbacks;

/**
 * Created by yossie on 2/16/17.
 */
public interface AccountActivityCallbacks {

    void loadSignIn(String userName, String password);

    void loadSignUp(String username, String firstName, String lastName, String email, String password);

    void showError(String error);

    void forgotPassword(String emailAddress);

    void signOut();

    void switchToSignUp();

    void switchToSignIn();

    void skipForNow();
}
