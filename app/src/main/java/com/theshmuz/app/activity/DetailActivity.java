package com.theshmuz.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NavUtils;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.analytics.tracking.android.EasyTracker;
import com.theshmuz.app.D;
import com.theshmuz.app.R;
import com.theshmuz.app.Shmuz;
import com.theshmuz.app.ShmuzHelper;
import com.theshmuz.app.fragments.ItemDetailFragment;
import com.theshmuz.app.loaders.TitleLoaderFromSeries;
import com.theshmuz.app.util.ThreeTypes;

public class DetailActivity extends AppCompatActivity implements LoaderCallbacks<String> {

    private ShmuzHelper dbh;

    private String mType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Shmuz app = (Shmuz) getApplication();
        dbh = app.db;

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        mType = type;
        String id = intent.getStringExtra("id");
        int access = intent.getIntExtra("access", ShmuzHelper.ACCESS_NONE);
        boolean allowsSharing = access == ShmuzHelper.ACCESS_NONE;

        if(type.equals(ThreeTypes.STR_SHMUZ)) {
            actionBar.setTitle("Shmuz #" + id);
        }
        else if(type.equals(ThreeTypes.STR_PARSHA)) {
            actionBar.setTitle(R.string.tab_parsha);
        }
        else if(ShmuzHelper.isSeries(type)) {
            Bundle bundle = new Bundle();
            bundle.putString("type", type);
            getSupportLoaderManager().initLoader(8, bundle, this); //load title info...
            actionBar.setTitle("");
        }

        if (savedInstanceState == null) {
            ItemDetailFragment fragment = ItemDetailFragment.newInstance(type, id, access, allowsSharing);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.item_detail_container, fragment)
                    .commit();
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(D.D) Log.d("DetailActivity", "optionsItemSelected now");
        switch (item.getItemId()) {
            case android.R.id.home:
                if(mType != null && ThreeTypes.isSeries(mType)) {
                    Intent upIntent = new Intent(this, SeriesActivity.class);
                    upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    upIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    upIntent.putExtra("type", mType);
                    startActivity(upIntent);
                    finish();
                }
                else {
                    NavUtils.navigateUpFromSameTask(this);
                }

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<String> onCreateLoader(int id, Bundle args) {
        String type = args.getString("type");
        return new TitleLoaderFromSeries(getApplicationContext(), dbh, type);
    }

    @Override
    public void onLoadFinished(Loader<String> arg0, String result) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(result);
    }

    @Override
    public void onLoaderReset(Loader<String> arg0) {

    }

}
