package com.theshmuz.app.business;

import android.os.SystemClock;
import android.support.v4.app.Fragment;

import com.theshmuz.app.SharedPrefWrap;
import com.theshmuz.app.fragments.PlainPopupDialogFragment;
import com.theshmuz.app.fragments.PopupDialogFragment;
import com.theshmuz.app.fragments.UrlPopupDialogFragment;

/**
 * Represents temporary advertisement data that we load at first...
 * @author yossie
 *
 */
public class Adver {

    public static final String SP_LAST_AD = "ad1";

    public long lastUpdated;

    public Adver() {
        lastUpdated = SystemClock.elapsedRealtime();
    }

    public boolean isOld() {
        return Math.abs(SystemClock.elapsedRealtime() - lastUpdated) > 6 * 60 * 60 * 1000; //6 hours
    }

    /**
     * First we have the popup stuff
     */
    public static final int POPUP_TYPE_WEBURL = 1;
    public static final int POPUP_TYPE_HTML = 2;
    public static final int POPUP_TYPE_PLAIN1 = 3;

    public int popupType;
    public String popupId;
    public String popupData1;
    public String popupData2;
    public String popupData3;

    public boolean shouldShowPopup(SharedPrefWrap sharedPrefWrap) {
        if(popupId == null) {
            return true;
        }

        String lastShowedId = sharedPrefWrap.getLastShowedAd1();
        if(popupId.equals(lastShowedId)) {
            return false;
        }
        else {
            return true;
        }

        //int showedCount = sp.getInt("ad_" + popupId, 0);
        //return showedCount < 1;
    }

    public PopupDialogFragment getFragmentToShow() {
        switch(popupType) {
            case POPUP_TYPE_WEBURL:
                return UrlPopupDialogFragment.newInstance(popupId, popupData1);
            case POPUP_TYPE_PLAIN1:
                return PlainPopupDialogFragment.newInstance(popupId, popupData1, popupData2, popupData3);
        }
        return null;
    }

    /**
     * Then we have the preroll stuff
     * (global version...)
     */

    public static final int PREROLL_TYPE_AUDIO = 1;

    public int prerollType;
    public String prerollUrl;
    public String prerollAction;


}
