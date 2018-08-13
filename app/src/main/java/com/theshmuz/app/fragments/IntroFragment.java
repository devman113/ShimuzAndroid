package com.theshmuz.app.fragments;

import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import com.theshmuz.app.R;

public final class IntroFragment extends TutorialsFragment {
    public static IntroFragment newInstance(@DrawableRes int imageDrawable) {
        IntroFragment slide = new IntroFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DRAWABLE, imageDrawable);
        slide.setArguments(args);

        return slide;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_intro;
    }
}