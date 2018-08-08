package com.theshmuz.app.activity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.theshmuz.app.D;
import com.theshmuz.app.R;
import com.theshmuz.app.fragments.ShmuzListFragment;

/**
 * Created by yossie on 1/8/17.
 */

public class SearchActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, ShmuzListFragment.Callbacks {

    private String currentQuery;
    private String currentTType;

    public static Intent createIntent(Activity context, String type) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.putExtra("tts", type);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String ttype = intent.getStringExtra("tts");
        currentTType = ttype;
        if(Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doSearch(query, ttype);

            if(D.D) Log.d("SearchActivity", "Got search query: " + query);
        }
        else {
            String query = intent.getStringExtra("q");
            doSearch(query, ttype);
        }
    }

    private void doSearch(String query, String type) {
        if(currentQuery != null && query.equals(currentQuery)) return;

        Fragment fragment = ShmuzListFragment.newInstance(type, false, query);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();

        setTitle(query);

        currentQuery = query;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconified(false);
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(this);
        if(currentQuery != null) searchView.setQuery(currentQuery, false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        doSearch(newText, currentTType);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
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

}
