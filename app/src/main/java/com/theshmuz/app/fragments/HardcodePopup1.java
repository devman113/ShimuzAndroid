package com.theshmuz.app.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.theshmuz.app.D;
import com.theshmuz.app.SharedPrefWrap;
import com.theshmuz.app.activity.InternalLink;
import com.theshmuz.app.business.Adver;

import java.lang.ref.WeakReference;

/**
 * Created by yossie on 5/29/17.
 */

public class HardcodePopup1 extends PopupDialogFragment {

    public static final String HARD_POP_1_STATE = "hpop1";

    private static final long SEVEN_DAYS_MILLIS = 1000L * 60L * 60L * 24L * 7L;
    private static final long FIVE_DAYS_MILLIS = 1000L * 60L * 60L * 24L * 5L;

    public static HardcodePopup1 newInstance(int stateType) {
        HardcodePopup1 hardcodePopup1 = new HardcodePopup1();

        Bundle bundle = new Bundle();
        bundle.putInt("s", stateType);
        hardcodePopup1.setArguments(bundle);

        return hardcodePopup1;
    }

    public static HardcodePopup1 getToDisplay(SharedPrefWrap sharedPrefWrap) {

        // Campaign is over (or didn't start yet)!
        final long curTime = System.currentTimeMillis();
        if (curTime < 1496289600000L || curTime > 1500177540000L) {
            return null;
        }

        String hard1State = sharedPrefWrap.getHard1AdState();
        if (D.D) Log.d("HardcodePopup1", "hard1State: " + hard1State);
        String[] hard1States = hard1State.split("\\|");
        if (D.D) Log.d("HardcodePopup1", "hard1State Len: " + hard1States.length);
        if (hard1States.length == 2) {
            String mainState = hard1States[0]; // L, R, N, C
            if (D.D) Log.d("HardcodePopup1", "mainState: " + mainState);
            long lastTime = Long.parseLong(hard1States[1]);

            if (mainState.equals("L")) {
                // Clicked Learn More
                if (Math.abs(System.currentTimeMillis() - lastTime) >= FIVE_DAYS_MILLIS) {
                    return newInstance(2);
                }
            }
            else if (mainState.equals("R")) {
                // Clicked Remind Me
                if (Math.abs(System.currentTimeMillis() - lastTime) >= SEVEN_DAYS_MILLIS) {
                    return newInstance(1);
                }
            }
            else if (mainState.equals("N")) {
                // Clicked No Thanks
                return null;
            }
            else if (mainState.equals("F")) {
                // Clicked No Thanks on second round (stateType == 2)
                return null;
            }
        }
        else {
            // new comer
            return newInstance(1);
        }
        return null;
    }

    public HardcodePopup1() {}

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SharedPreferences sp = this.sp;
        final WeakReference<HardcodePopup1> ourFragment = new WeakReference<>(this);
        final Context appContext = getContext().getApplicationContext();

        final int stateType = getArguments().getInt("s");

        boolean inExtension = System.currentTimeMillis() >= 1498881600000L;

        if (stateType == 1) {
            String msgText = "Support The Shmuz, get free gifts, and a chance to win $50,000 cash!\nDeadline: June 30, 2017";
            if (inExtension) {
                msgText = "Support The Shmuz, get free gifts, and a chance to win $50,000 cash!\nDeadline Extended! July 15, 2017";
            }
            return new AlertDialog.Builder(getActivity())
                    .setTitle("Win $50,000 Cash")
                    .setMessage(msgText)
                    .setPositiveButton("Learn More", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final Intent openIntent = new Intent(appContext, InternalLink.class);
                            HardcodePopup1 ourFrag = ourFragment.get();
                            if(ourFrag != null && ourFrag.getActivity() != null) {
                                ourFrag.getActivity().startActivity(openIntent);
                                sp.edit().putString(HARD_POP_1_STATE, "L|" + Long.toString(System.currentTimeMillis())).apply();
                                if(!D.D) EasyTracker.getTracker().sendEvent("popup_dialog_june_2017", "button_press", "Learn More", (long) 0);
                            }
                            else {
                                //analytics?
                            }
                        }
                    })
                    .setNeutralButton("Remind Me Later", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //do nothing, just dismiss...
                            sp.edit().putString(HARD_POP_1_STATE, "R|" + Long.toString(System.currentTimeMillis())).apply();
                            if(!D.D) EasyTracker.getTracker().sendEvent("popup_dialog_june_2017", "button_press", "Remind Me Later", (long) 0);
                        }
                    })
                    .setNegativeButton("No Thanks", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            sp.edit().putString(HARD_POP_1_STATE, "N|" + Long.toString(System.currentTimeMillis())).apply();
                            if(!D.D) EasyTracker.getTracker().sendEvent("popup_dialog_june_2017", "button_press", "No Thanks", (long) 0);
                        }
                    })
                    .setCancelable(false)
                    .create();
        }
        else if (stateType == 2) {
            //Just making sure that you got your entries into our $50,000 raffle. The deadline (June 30, 2017) will be here before you know it!
            String msgText = "Just making sure that you got your entries into our $50,000 raffle. The deadline (June 30, 2017) will be here before you know it!";
            if (inExtension) {
                msgText = "Just making sure that you got your entries into our $50,000 raffle. The EXTENDED deadline (July 15, 2017) is almost here, and there are no more extensions!";
            }
            return new AlertDialog.Builder(getActivity())
                    .setTitle("Win $50,000 Cash")
                    .setMessage(msgText)
                    .setPositiveButton("Enter Raffle Now", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final Intent openIntent = new Intent(appContext, InternalLink.class);
                            HardcodePopup1 ourFrag = ourFragment.get();
                            if(ourFrag != null && ourFrag.getActivity() != null) {
                                ourFrag.getActivity().startActivity(openIntent);
                                sp.edit().putString(HARD_POP_1_STATE, "F|" + Long.toString(System.currentTimeMillis())).apply();
                                if(!D.D) EasyTracker.getTracker().sendEvent("popup_dialog_june_2017", "button_press", "Enter Raffle Now", (long) 0);
                            }
                            else {
                                //analytics?
                            }
                        }
                    })
                    .setNeutralButton("Remind Me Later", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //do nothing, just dismiss...
                            sp.edit().putString(HARD_POP_1_STATE, "R|" + Long.toString(System.currentTimeMillis())).apply();
                            if(!D.D) EasyTracker.getTracker().sendEvent("popup_dialog_june_2017", "button_press", "Remind Me Later", (long) 0);
                        }
                    })
                    .setNegativeButton("No Thanks", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            sp.edit().putString(HARD_POP_1_STATE, "N|" + Long.toString(System.currentTimeMillis())).apply();
                            if(!D.D) EasyTracker.getTracker().sendEvent("popup_dialog_june_2017", "button_press", "No Thanks", (long) 0);
                        }
                    })
                    .setCancelable(false)
                    .create();
        }
        return null;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // Do nothing (avoids bug with NPE)
    }
}
