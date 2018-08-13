package com.theshmuz.app.activity;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Toast;

import com.github.paolorotolo.appintro.AppIntro;
import com.theshmuz.app.fragments.IntroFragment;

import com.theshmuz.app.R;

import com.github.paolorotolo.appintro.AppIntroFragment;

public class IntroActivity extends AppIntro {

    public static Intent createIntent(Activity context) {
        Intent intent = new Intent(context, IntroActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(IntroFragment.newInstance(R.drawable.t1));
        addSlide(IntroFragment.newInstance(R.drawable.t2));
        addSlide(IntroFragment.newInstance(R.drawable.t3));
        addSlide(IntroFragment.newInstance(R.drawable.t4));
        addSlide(IntroFragment.newInstance(R.drawable.t5));
        addSlide(IntroFragment.newInstance(R.drawable.t6));

        setBarColor(Color.parseColor("#00000000"));
        setSeparatorColor(Color.parseColor("#00000000"));
        showSkipButton(true);
    }

    private void loadMainActivity() {
        finish();
    }
    @Override
    public void onSkipPressed(Fragment currentFragment) {
        loadMainActivity();

    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        loadMainActivity();
    }

    public void getStarted(View v) {
        loadMainActivity();
    }

}
