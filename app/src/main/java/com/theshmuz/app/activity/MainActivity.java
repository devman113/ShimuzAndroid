package com.theshmuz.app.activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.theshmuz.app.BuildConfig;
import com.theshmuz.app.D;
import com.theshmuz.app.LoginHelper;
import com.theshmuz.app.R;
import com.theshmuz.app.SharedPrefWrap;
import com.theshmuz.app.Shmuz;
import com.theshmuz.app.UpdateService;
import com.theshmuz.app.UpdatorStatus;
import com.theshmuz.app.UpdatorStatus.UpdatorConsumer;
import com.theshmuz.app.business.Adver;
import com.theshmuz.app.fragments.HardcodePopup1;
import com.theshmuz.app.fragments.PopupDialogFragment;
import com.theshmuz.app.fragments.RefreshableFragment;
import com.theshmuz.app.fragments.SeriesListFragment;
import com.theshmuz.app.fragments.ShmuzListFragment;
import com.theshmuz.app.fragments.ShmuzListFragment.Callbacks;
import com.theshmuz.app.loaders.AdTask;
import com.theshmuz.app.streamer.NowPlaying;
import com.theshmuz.app.streamer.PlaybackState;
import com.theshmuz.app.util.ThreeTypes;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Callbacks, UpdatorConsumer, ActionBar.TabListener {

    private int mTabPosition;
    private FloatingActionButton searchButton;

    private Adver lastAd;
    private SharedPrefWrap sharedPrefWrap;

    private UpdatorStatus updatorStatus;
    private PlaybackState playbackState;
    private TextView mUpdateView;

    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private List<WeakReference<RefreshableFragment>> fragList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPrefWrap = SharedPrefWrap.getInstance(this);

        Shmuz app = (Shmuz) getApplication();
        updatorStatus = app.updator;
        updatorStatus.bringUpToDate(this);
        playbackState = app.state;
        LoginHelper loginHelper = app.getLoginHelper();

        app.getAd();

        mUpdateView = (TextView) findViewById(R.id.updateText);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                getSupportActionBar().setSelectedNavigationItem(position);
            }
        });

        searchButton = (FloatingActionButton) findViewById(R.id.floating_button);
        searchButton.setOnClickListener(this);

        final ActionBar actionBar = getSupportActionBar();

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        actionBar.setHomeAsUpIndicator(R.drawable.logo);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        ActionBar.Tab tab = actionBar.newTab()
                .setText(R.string.tab_shmuz)
                .setTabListener(this);
        actionBar.addTab(tab);

        tab = actionBar.newTab()
                .setText(R.string.tab_series)
                .setTabListener(this);
        actionBar.addTab(tab);

        tab = actionBar.newTab()
                .setText(R.string.tab_parsha)
                .setTabListener(this);
        actionBar.addTab(tab);

        if(savedInstanceState != null) {
            actionBar.setSelectedNavigationItem(savedInstanceState.getInt("tabstate", 0));
        }

        long lastUpdateSuccess = sharedPrefWrap.getLastUpdateSuccess();
        if(Math.abs(System.currentTimeMillis() - lastUpdateSuccess) > 1000 * 60 * 60 * 24){
            startService(new Intent(this, UpdateService.class));
        }

        long curTime = System.currentTimeMillis();
        if(D.D) Log.i("CurTime", Long.toString(curTime));

        if(savedInstanceState == null) {
            Intent intent = getIntent();
            boolean skip = intent.getBooleanExtra("skip", false);
            int playCount = sharedPrefWrap.getPlayCount();
            boolean forceSignup = playCount >= D.MAX_PLAY_COUNTS;
            if(forceSignup) {
                skip = false;
            }
            if(!loginHelper.isSignedIn() && !skip) {
                startActivity(AccountActivity.createIntent(this));
                finish();
            }
        }

        if (savedInstanceState == null) {
            HardcodePopup1 hardcodePopup1 = HardcodePopup1.getToDisplay(sharedPrefWrap);
            if (hardcodePopup1 != null) {
                hardcodePopup1.show(getSupportFragmentManager(), "dialogH");
            }
        }
        startActivity(IntroActivity.createIntent(this));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.floating_button:
                String typeToSearch;
                if(mTabPosition == 0) typeToSearch = "shmuz";
                else if(mTabPosition == 2) typeToSearch = "parsha";
                else {
                    //typeToSearch = "series";
                    return;
                }
                Intent intent = SearchActivity.createIntent(this, typeToSearch);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if(fragment instanceof RefreshableFragment) {
            RefreshableFragment frag = (RefreshableFragment) fragment;
            fragList.add(new WeakReference<>(frag));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        checkForAdUpdates();

        supportInvalidateOptionsMenu();
        updatorStatus.processStatusCheck(this);

        IntentFilter toListen = new IntentFilter();
        toListen.addAction(UpdateService.UPDATE_CMD);
        toListen.addAction(AdTask.AD_CMD);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, toListen);
    }

    private void checkForAdUpdates() {
        Shmuz app = (Shmuz) getApplication();
        Adver ad = app.getAd();

        if(ad != null && ad != lastAd) {
            FragmentManager manager = getSupportFragmentManager();
            if(ad.shouldShowPopup(sharedPrefWrap) && manager.findFragmentByTag("dialog2") == null) {
                PopupDialogFragment newFragment = ad.getFragmentToShow();
                if(newFragment != null) {
                    newFragment.show(manager, "dialog2");
                }
            }
            lastAd = ad;
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!D.D) EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(!D.D) EasyTracker.getInstance().activityStop(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    private void processUpdateStatus(int status) {
        switch(status){
            case UpdatorStatus.UPDATE_STATUS_STARTING:
                mUpdateView.setVisibility(View.VISIBLE);
                break;
            case UpdatorStatus.UPDATE_STATUS_FAILED:
            case UpdatorStatus.UPDATE_STATUS_SUCCESS:
                mUpdateView.setVisibility(View.GONE);
                refreshFragments();
                break;
        }
    }

    private void refreshFragments() {
        if(D.D) Log.i("RefreshFragments", "NOW");

        boolean didSomething = false;

        for(WeakReference<RefreshableFragment> ref : fragList) {
            RefreshableFragment f = ref.get();
            if(f != null) {
                didSomething = true;
                f.refresh();
            }
        }

        if(!D.D && !didSomething) {
            EasyTracker.getTracker().sendException("No Fragments didSomething", false);
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(D.D) Log.e("mMessageReceiver", "GOT IT");
            String action = intent.getAction();
            if(action == null) return;
            if(action.equals(UpdateService.UPDATE_CMD)) {
                updatorStatus.processStatusCheck(MainActivity.this);
            }
            else if(action.equals(AdTask.AD_CMD)) {
                checkForAdUpdates();
            }
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putInt("tabstate", getSupportActionBar().getSelectedNavigationIndex());
    }

    @Override
    public void onItemSelected(String type, String id) {
        openItem(type, id);
    }

    @Override
    public void onSeriesSelected(String id, String title, boolean thumbs, int access, boolean authorized) {
        if(authorized) {
            openSeries(id, title, thumbs, access);
        }
        else {
            switch (access) {
                //case ShmuzHelper.ACCESS_PREMIUM:
                //openLogin(true);
                //	break;
                default:
                    openSeries(id, title, thumbs, access);
            }
        }
    }

    private void openItem(String type, String id) {
        Intent toStart = new Intent(this, DetailActivity.class);
        toStart.putExtra("type", type);
        toStart.putExtra("id", id);
        startActivity(toStart);
    }

    private void openSeries(String type, String title, boolean thumbs, int access) {
        Intent toStart = new Intent(this, SeriesActivity.class);
        toStart.putExtra("type", type);
        if(title != null) toStart.putExtra("title", title);
        toStart.putExtra("thumbs", thumbs);
        toStart.putExtra("access", access);
        startActivity(toStart);
    }

    private void openLogin(boolean finishIt) {
        Intent toStart = new Intent(this, AccountActivity.class);
        toStart.putExtra("finish", finishIt);
        startActivityForResult(toStart, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        refreshFragments();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (playbackState.currentState == PlaybackState.PLAYING) {
            menu.findItem(R.id.menuNowPlaying).setVisible(true);
        }
        else {
            menu.findItem(R.id.menuNowPlaying).setVisible(false);
        }

        final long curTime = System.currentTimeMillis();
        if (curTime >= 1496289600000L && curTime <= 1500177540000L) {
            if (D.D) Log.d("MainActivity", "onPrepareOptionsMenu in Hard1!");
            menu.findItem(R.id.menuHard1).setVisible(true);
        }
        else {
            if (D.D) Log.d("MainActivity", "onPrepareOptionsMenu not in Hard1");
            menu.findItem(R.id.menuHard1).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Tracker tracker;
        switch (item.getItemId()) {
            case R.id.menuRefresh:
                if(!D.D) EasyTracker.getTracker().sendEvent("ui_action", "menu_press", "Refresh Button", (long) 0);
                startService(new Intent(this, UpdateService.class));
                return true;

            case R.id.menuNowPlaying:
                NowPlaying nowPlaying = playbackState.nowPlaying;
                if(nowPlaying != null){
                    if(!D.D) EasyTracker.getTracker().sendEvent("ui_action", "menu_press", "Now Playing Button", (long) 0);
                    openItem(nowPlaying.getType(), nowPlaying.getId());
                }
                return true;

            case R.id.menuAccount:
                if(!D.D) EasyTracker.getTracker().sendEvent("ui_action", "menu_press", "My Account Button", (long) 0);
                openLogin(false);
                return true;

            case R.id.menuShare:
                if(!D.D) EasyTracker.getTracker().sendEvent("ui_action", "menu_press", "Share App Button", (long) 0);
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app_subject));
                sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text));
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_app_dialog_title)));
                return true;

            case R.id.menuContact:
                if(!D.D) EasyTracker.getTracker().sendEvent("ui_action", "menu_press", "Contact Button", (long) 0);
                contactUs();
                return true;

            case R.id.menuDonate:
                tracker = EasyTracker.getTracker();
                if(tracker != null && !D.D) {
                    tracker.sendEvent("ui_action", "menu_press", "Donate Button", (long) 0);
                }
                openUrl("https://theshmuz.com/donation-2/");
                return true;

            case android.R.id.home:
                openUrl("http://www.theshmuz.com/");
                return true;

            case R.id.menuHard1:
                HardcodePopup1 hardcodePopup1 = HardcodePopup1.newInstance(1);
                if (hardcodePopup1 != null) {
                    hardcodePopup1.show(getSupportFragmentManager(), "dialogH");
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openUrl(final String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
        catch(ActivityNotFoundException e) {
            EasyTracker.getTracker().sendException("No Browser: " + url, false);
            Toast.makeText(this, "No Browser Found", Toast.LENGTH_SHORT).show();
        }
    }

    private void contactUs() {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + D.CONTACT_EMAIL));
        intent.putExtra(Intent.EXTRA_SUBJECT, "The Shmuz for Android (" + BuildConfig.VERSION_CODE + ')');
        startActivity(intent);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            int type;
            if(position == 0) {
                type = ThreeTypes.SHMUZ;
            }
            else if(position == 2) {
                type = ThreeTypes.PARSHA;
            }
            else if(position == 1) {
                return SeriesListFragment.newInstance();
            }
            else {
                return null;
            }
            Fragment fragment = ShmuzListFragment.newInstance(ThreeTypes.getString(type), false);
            return fragment;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.tab_shmuz).toUpperCase(l);
                case 1:
                    return getString(R.string.tab_parsha).toUpperCase(l);
                case 2:
                    return getString(R.string.tab_series).toUpperCase(l);
            }
            return null;
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        int position = tab.getPosition();
        mViewPager.setCurrentItem(position);
        if(searchButton != null) {
            if (position == 1) searchButton.setVisibility(View.GONE);
            else searchButton.setVisibility(View.VISIBLE);
        }
        mTabPosition = position;
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        //TODO: scroll to top?
    }

    private int localVersion;
    @Override
    public int getUpdateVersion() {
        return localVersion;
    }

    @Override
    public void processUpdate(int newVersion, int newStatus) {
        if(D.D) Log.d("MainActivity", "Processing Update " + newVersion + " - " + newStatus);
        localVersion = newVersion;
        if(newStatus == UpdatorStatus.UPDATE_STATUS_INVALID) return;
        processUpdateStatus(newStatus);
    }

}
