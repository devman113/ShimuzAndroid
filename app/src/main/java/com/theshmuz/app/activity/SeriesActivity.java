package com.theshmuz.app.activity;

import java.lang.ref.WeakReference;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.analytics.tracking.android.EasyTracker;
import com.theshmuz.app.D;
import com.theshmuz.app.LoginHelper;
import com.theshmuz.app.R;
import com.theshmuz.app.Shmuz;
import com.theshmuz.app.ShmuzHelper;
import com.theshmuz.app.UpdatorStatus;
import com.theshmuz.app.UpdatorStatus.UpdatorConsumer;
import com.theshmuz.app.fragments.RefreshableFragment;
import com.theshmuz.app.fragments.ShmuzListFragment;
import com.theshmuz.app.fragments.ShmuzListFragment.Callbacks;
import com.theshmuz.app.loaders.TitleLoaderFromSeries;

public class SeriesActivity extends AppCompatActivity implements Callbacks, UpdatorConsumer, LoaderCallbacks<String> {

    private UpdatorStatus updatorStatus;

    private ShmuzHelper dbh;

    private WeakReference<RefreshableFragment> currentFragmentToRefresh;

    private int accessLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Shmuz app = (Shmuz) getApplication();
        updatorStatus = app.updator;
        updatorStatus.bringUpToDate(this); //bring it up to date anyways...
        dbh = app.db;

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        String title = intent.getStringExtra("title");
        boolean thumbs = intent.getBooleanExtra("thumbs", false);
        accessLevel = intent.getIntExtra("access", ShmuzHelper.ACCESS_NONE);

        if(title != null) {
            actionBar.setTitle(title);
        }
        else {
            Bundle bundle = new Bundle();
            bundle.putString("type", type);
            getSupportLoaderManager().initLoader(8, bundle, this);
            actionBar.setTitle("");
        }

        if (savedInstanceState == null) {
            ShmuzListFragment fragment = ShmuzListFragment.newInstance(type, thumbs);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.item_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if(fragment instanceof RefreshableFragment) {
            RefreshableFragment frag = (RefreshableFragment) fragment;
            currentFragmentToRefresh = new WeakReference<>(frag);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!D.D) EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        supportInvalidateOptionsMenu();
        updatorStatus.processStatusCheck(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(!D.D) EasyTracker.getInstance().activityStop(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.series, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menuAccount:
                openLogin(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(String type, String id) {
        openItem(type, id, accessLevel);
    }

    private void openLogin(boolean finishIt) {
        Intent toStart = new Intent(this, AccountActivity.class);
        toStart.putExtra("finish", finishIt);
        startActivityForResult(toStart, 0);
    }

    private void openItem(String type, String id, int access) {
        Intent toStart = new Intent(this, DetailActivity.class);
        toStart.putExtra("type", type);
        toStart.putExtra("id", id);
        toStart.putExtra("access", access);
        startActivity(toStart);
    }

    @Override
    public void onSeriesSelected(String id, String title, boolean thumbs, int access, boolean authorized) {
        // (not possible, for now)
    }

    private int localVersion;
    @Override
    public int getUpdateVersion() {
        return localVersion;
    }

    @Override
    public void processUpdate(int newVersion, int newStatus) {
        if(D.D) Log.d("SeriesActivity", "Processing Update " + newVersion + " - " + newStatus);
        localVersion = newVersion;
        if(newStatus == UpdatorStatus.UPDATE_STATUS_INVALID) return;
        processUpdateStatus(newStatus);
    }

    private void processUpdateStatus(int status) {
        switch(status){
            case UpdatorStatus.UPDATE_STATUS_FAILED:
            case UpdatorStatus.UPDATE_STATUS_SUCCESS:
                if(currentFragmentToRefresh != null) {
                    RefreshableFragment f = currentFragmentToRefresh.get();
                    if(f != null) {
                        f.refresh();
                    }
                }
                break;
        }
    }

    @Override
    public Loader<String> onCreateLoader(int id, Bundle args) {
        String type = args.getString("type");
        return new TitleLoaderFromSeries(getApplicationContext(), dbh, type);
    }

    @Override
    public void onLoadFinished(Loader<String> loader, String result) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(result);
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {

    }

}
