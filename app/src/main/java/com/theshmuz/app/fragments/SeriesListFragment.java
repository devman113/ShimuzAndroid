package com.theshmuz.app.fragments;

import java.util.concurrent.RejectedExecutionException;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.theshmuz.app.D;
import com.theshmuz.app.LoginHelper;
import com.theshmuz.app.R;
import com.theshmuz.app.Shmuz;
import com.theshmuz.app.ShmuzHelper;
import com.theshmuz.app.SimpleCursorLoader;
import com.theshmuz.app.fragments.ShmuzListFragment.Callbacks;
import com.theshmuz.app.loaders.BitmapWorkerTask;
import com.theshmuz.app.streamer.PlaybackState;
import com.theshmuz.app.util.DiskCache;
import com.theshmuz.app.util.Version11Helper;

public class SeriesListFragment extends ListFragment implements LoaderCallbacks<Cursor>, RefreshableFragment, LoginHelper.DataSyncer {

    private SimpleCursorAdapter adapter;

    private Callbacks mCallbacks = sDummyCallbacks;

    private ShmuzHelper db;
    private LoginHelper mLoginHelper;
    private DiskCache mDiskCache;
    private PlaybackState playbackState;

    private Bitmap cachedStub;
    private LruCache<String, Bitmap> mMemoryCache;

    public SeriesListFragment(){}

    public static SeriesListFragment newInstance() {
        SeriesListFragment frag = new SeriesListFragment();
        return frag;
    }

    public void loadBitmap(String url, ImageView imageView) {
        if(D.D) Log.d("loadBitmap", "" + url);

        if(mMemoryCache != null){
            Bitmap possible = mMemoryCache.get(url);
            if(possible != null){
                imageView.setImageBitmap(possible);
                return;
            }
        }

        if (BitmapWorkerTask.cancelPotentialWork(url, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView, mMemoryCache, mDiskCache, true);
            final BitmapWorkerTask.AsyncDrawable asyncDrawable =
                    new BitmapWorkerTask.AsyncDrawable(getResources(), cachedStub, task);
            imageView.setImageDrawable(asyncDrawable);
            try {
                if(Build.VERSION.SDK_INT >= 11) Version11Helper.runAsyncTask(task, url);
                else task.execute(url);
            } catch(RejectedExecutionException e) {
                if(D.D) Log.e("BitmapLoader", "Can't start a task for the bitmap!");
                EasyTracker.getTracker().sendException(e.getMessage(), false);
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_member_list, container, false);

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Shmuz app = (Shmuz) getActivity().getApplication();
        db = app.db;
        mDiskCache = app.diskCache;
        playbackState = app.state;
        mLoginHelper = app.getLoginHelper();
        mLoginHelper.doInitialSync(this);

        EasyTracker.getInstance().setContext(getActivity());

        ViewBinder binder = new ViewBinder() {

            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                switch(view.getId()){
                    case R.id.shmuz_title:
                        ((TextView) view).setText(cursor.getString(columnIndex));
                        return true;
                    case R.id.shmuz_image:
                        ImageView imageView = (ImageView) view;
                        String url = cursor.getString(columnIndex);
                        if(!url.equals("null") && url.toLowerCase().startsWith("http")) {
                            loadBitmap(url, imageView);
                        }
                        else {
                            imageView.setImageBitmap(null);
                            //imageView.setImageBitmap(cachedStub);
                        }
                        return true;
                    case R.id.shmuz_lock:
                        int accessLevel = cursor.getInt(columnIndex);
                        ImageView iconView = (ImageView) view;

                        // temp override. really should properly remove this functionality...
                        iconView.setVisibility(View.GONE);
                        return true;
                    case R.id.shmuz_early_tease:
                        int accessLevel2 = cursor.getInt(columnIndex);
                        if(accessLevel2 == ShmuzHelper.ACCESS_EARLY) {
                            view.setVisibility(View.VISIBLE);
                        }
                        else {
                            view.setVisibility(View.GONE);
                        }
                        return true;
                }
                return false;
            }
        };

        RetainCacheFragment mRetainFragment = RetainCacheFragment.findOrCreateRetainFragment(getFragmentManager());
        mMemoryCache = mRetainFragment.mRetainedCache;
        if (mMemoryCache == null) {
            mMemoryCache = new LruCache<String, Bitmap>(15) {

                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return 1; //for now!
                }

            };
            mRetainFragment.mRetainedCache = mMemoryCache;
        }
        else {
            if(D.D) Log.d("SeriesListFragment", "Cache hit on the cache!");
        }
        mMemoryCache = null;

        adapter = new SimpleCursorAdapter(
                getActivity().getApplicationContext(), R.layout.series_list,
                null,
                new String[]{ ShmuzHelper.TITLE, ShmuzHelper.URL_ARTWORK, ShmuzHelper.ACCESS, ShmuzHelper.ACCESS },
                new int[]{ R.id.shmuz_title, R.id.shmuz_image, R.id.shmuz_lock, R.id.shmuz_early_tease },
                0);

        adapter.setViewBinder(binder);

        setListAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        mLoginHelper.doDataSync(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(1, null, this);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        String title = null;

        boolean thumbs = false;
        int access = 0;
        boolean authorized = true;

        Cursor cursorRet = (Cursor) adapter.getItem(position);
        if(cursorRet != null) {
            access = cursorRet.getInt(cursorRet.getColumnIndex(ShmuzHelper.ACCESS));
            thumbs = cursorRet.getInt(cursorRet.getColumnIndex(ShmuzHelper.THUMBS)) == 1;
            title = cursorRet.getString(cursorRet.getColumnIndex(ShmuzHelper.TITLE));
        }

        mCallbacks.onSeriesSelected(Long.toString(id), title, thumbs, access, authorized);
    }

    public static final class SeriesCursorLoader extends SimpleCursorLoader {

        private ShmuzHelper mHelper;

        public SeriesCursorLoader(Context context, ShmuzHelper helper) {
            super(context);
            mHelper = helper;
        }

        @Override
        public Cursor loadInBackground() {

            SQLiteDatabase db = mHelper.getWritableDatabase();
            String[] cols = new String[]{ShmuzHelper.SID, ShmuzHelper.TITLE, ShmuzHelper.ACCESS, ShmuzHelper.THUMBS, ShmuzHelper.URL_ARTWORK};

            Cursor cursor = db.query(ShmuzHelper.TABLE_SERIES,
                    cols,
                    ShmuzHelper.ACCESS + "<= 3",
                    null,
                    null,
                    null,
                    ShmuzHelper.TITLE);

            if (cursor != null) {
                cursor.getCount();
            }

            return cursor;
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        SeriesCursorLoader cursorLoader = new SeriesCursorLoader(getActivity().getApplicationContext(), db);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }


    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(String type, String id) {}

        @Override
        public void onSeriesSelected(String id, String title, boolean thumbs, int access, boolean authorized) {}
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void refresh() {
        getLoaderManager().restartLoader(1, null, this);
    }

    private int syncLoginVersion;
    @Override
    public int syncGetLoginDataVersion() {
        return syncLoginVersion;
    }

    @Override
    public void syncSetLoginDataVersion(int version) {
        syncLoginVersion = version;
    }

    @Override
    public void syncLoginSomethingChanged() {
        adapter.notifyDataSetChanged();
    }

}
