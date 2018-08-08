package com.theshmuz.app.fragments;

import java.util.concurrent.RejectedExecutionException;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.theshmuz.app.R;
import com.theshmuz.app.Shmuz;
import com.theshmuz.app.ShmuzHelper;
import com.theshmuz.app.SimpleCursorLoader;
import com.theshmuz.app.loaders.BitmapWorkerTask;
import com.theshmuz.app.streamer.PlaybackState;
import com.theshmuz.app.util.DiskCache;
import com.theshmuz.app.util.ThreeTypes;
import com.theshmuz.app.util.Version11Helper;

public class ShmuzListFragment extends ListFragment implements LoaderCallbacks<Cursor>, RefreshableFragment {

    private SimpleCursorAdapter adapter;
    private String mType;
    private boolean mThumbs;
    private String mFilterSearch;

    private Callbacks mCallbacks = sDummyCallbacks;

    private ShmuzHelper db;
    private DiskCache mDiskCache;
    private PlaybackState playbackState;

    private Bitmap cachedStub;
    private LruCache<String, Bitmap> mMemoryCache;

    public ShmuzListFragment(){}

    public static ShmuzListFragment newInstance(String type, boolean thumbs) {
        ShmuzListFragment frag = new ShmuzListFragment();

        Bundle bundle = new Bundle();
        bundle.putString("type", type);
        bundle.putBoolean("thumbs", thumbs);
        frag.setArguments(bundle);

        return frag;
    }

    public static ShmuzListFragment newInstance(String type, boolean thumbs, String search) {
        ShmuzListFragment frag = new ShmuzListFragment();

        Bundle bundle = new Bundle();
        bundle.putString("type", type);
        bundle.putBoolean("thumbs", thumbs);
        bundle.putString("search", search);
        frag.setArguments(bundle);

        return frag;
    }

    public void loadBitmap(String url, ImageView imageView) {

        if(mMemoryCache != null) {
            Bitmap possible = mMemoryCache.get(url);
            if(possible != null) {
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
                if(!D.D) EasyTracker.getTracker().sendException(e.getMessage(), false);
            }
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Shmuz app = (Shmuz) getActivity().getApplication();
        db = app.db;
        mDiskCache = app.diskCache;
        playbackState = app.state;

        EasyTracker.getInstance().setContext(getActivity());

        Bundle args = getArguments();
        //IT BETTER!!
        if (args.containsKey("type")) {
            mType = args.getString("type");
        } else {
            throw new IllegalStateException("No type!");
        }

        if(args.containsKey("thumbs")) {
            mThumbs = args.getBoolean("thumbs");
        }
        mFilterSearch = args.getString("search");

        cachedStub = BitmapFactory.decodeResource(getResources(), R.drawable.stub);

        ViewBinder binder = new ViewBinder() {

            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                switch(view.getId()){
                    case R.id.shmuz_title:
                        ((TextView) view).setText(cursor.getString(columnIndex));
                        return true;
                    case R.id.shmuz_number:
                        ((TextView) view).setText("#" + cursor.getString(columnIndex));
                        view.setVisibility(View.VISIBLE);
                        return true;
                    case R.id.shmuz_image:
                        ImageView imageView = (ImageView) view;
                        String url = cursor.getString(columnIndex);
                        if(url != null && !url.equals("null") && url.toLowerCase().startsWith("http")) {
                            loadBitmap(url, imageView);
                        }
                        else {
                            imageView.setImageBitmap(cachedStub);
                        }
                        return true;
                    case R.id.shmuz_downloaded:
                        if(cursor.getLong(cursor.getColumnIndex(ShmuzHelper.DOWNLOAD_ID)) > 0) {
                            view.setVisibility(View.VISIBLE);
                        }
                        else {
                            view.setVisibility(View.GONE);
                        }
                        return true;
                    case R.id.shmuz_listened:
                        int position = cursor.getInt(columnIndex);
                        int duration = cursor.getInt(cursor.getColumnIndex(ShmuzHelper.DURATION));
                        if(position >= 5000 && (position <= (duration - 30000) || duration == 0)) {
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

        if(mType.equals("shmuz")) {

            RetainCacheFragment mRetainFragment =
                    RetainCacheFragment.findOrCreateRetainFragment(getFragmentManager());
            mMemoryCache = mRetainFragment.mRetainedCache;
            if (mMemoryCache == null) {
                mMemoryCache = new LruCache<String, Bitmap>(15) {

                    @Override
                    protected int sizeOf(String key, Bitmap bitmap) {
                        return 1;
                    }

                };
                mRetainFragment.mRetainedCache = mMemoryCache;
            }

            adapter = new SimpleCursorAdapter(
                    getActivity().getApplicationContext(), R.layout.shmuz_list,
                    null,
                    new String[]{ 	ShmuzHelper.TITLE,
                            ShmuzHelper.SEQUENCE,
                            ShmuzHelper.URL_ARTWORK,
                            ShmuzHelper.DOWNLOAD_ID,
                            ShmuzHelper.POSITION },

                    new int[]{ 		R.id.shmuz_title,
                            R.id.shmuz_number,
                            R.id.shmuz_image,
                            R.id.shmuz_downloaded,
                            R.id.shmuz_listened },
                    0);

            adapter.setViewBinder(binder);
        }
        else if(Character.isDigit(mType.charAt(0)) && mThumbs) {

            adapter = new SimpleCursorAdapter(
                    getActivity().getApplicationContext(), R.layout.shmuz_list,
                    null,
                    new String[]{ 	ShmuzHelper.TITLE,
                            ShmuzHelper.SEQUENCE,
                            ShmuzHelper.URL_ARTWORK,
                            ShmuzHelper.DOWNLOAD_ID,
                            ShmuzHelper.POSITION },
                    new int[]{ R.id.shmuz_title, R.id.shmuz_number, R.id.shmuz_image, R.id.shmuz_downloaded, R.id.shmuz_listened },
                    0);

            adapter.setViewBinder(binder);

        }
        else {
            adapter = new SimpleCursorAdapter(
                    getActivity().getApplicationContext(), R.layout.other_list,
                    null,
                    new String[]{ ShmuzHelper.TITLE, ShmuzHelper.DOWNLOAD_ID, ShmuzHelper.POSITION },
                    new int[]{ R.id.shmuz_title, R.id.shmuz_downloaded, R.id.shmuz_listened },
                    0);

            adapter.setViewBinder(binder);
        }

        setListAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(ThreeTypes.getInt(mType), null, this);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        mCallbacks.onItemSelected(mType, Long.toString(id));
    }

    public static final class ShmuzCursorLoader extends SimpleCursorLoader {

        private ShmuzHelper mHelper;
        private String dbType;
        private String searchFilter;

        public ShmuzCursorLoader(Context context, ShmuzHelper helper, String dbType, String searchFilter) {
            super(context);
            mHelper = helper;
            this.dbType = dbType;
            this.searchFilter = searchFilter;
        }

        @Override
        public Cursor loadInBackground() {

            SQLiteDatabase db = mHelper.getWritableDatabase();
            String[] cols = new String[]{ShmuzHelper.SID,
                    ShmuzHelper.SEQUENCE,
                    ShmuzHelper.TITLE,
                    ShmuzHelper.URL_ARTWORK,
                    ShmuzHelper.DOWNLOAD_ID,
                    ShmuzHelper.POSITION,
                    ShmuzHelper.DURATION };

            String tableName = dbType;
            String selection = null;
            String[] selectionArgs = null;

            boolean isSeries = ShmuzHelper.isSeries(dbType);
            if(isSeries) {
                tableName = ShmuzHelper.TABLE_SERIES_CONTENT;
                selection = ShmuzHelper.SERIES_REF + "=?";
                selectionArgs = new String[]{dbType};
            }

            String orderBy;
            if(isSeries && dbType.charAt(0) == '-') {
                orderBy = ShmuzHelper.TITLE;
            } else if(dbType.equals(ShmuzHelper.TABLE_PARSHA)) {
                orderBy = ShmuzHelper.TITLE;
            } else {
                orderBy = ShmuzHelper.SEQUENCE;
            }

            Cursor cursor;
            if(searchFilter != null) {
                cursor = db.query(tableName,
                        cols,
                        ShmuzHelper.TITLE + " LIKE ?",
                        new String[]{"%" + searchFilter + "%"},
                        null,
                        null,
                        orderBy);
            }
            else {
                cursor = db.query(tableName,
                        cols,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        orderBy);
            }

            if (cursor != null) {
                int count = cursor.getCount();
            }

            return cursor;
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(D.D) Log.d("ShmuzListFragment", "Loader Created " + mType);
        ShmuzCursorLoader cursorLoader = new ShmuzCursorLoader(getActivity().getApplicationContext(), db, mType, mFilterSearch);
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


    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        void onItemSelected(String type, String id);

        void onSeriesSelected(String id, String title, boolean thumbs, int access, boolean authorized);
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
        getLoaderManager().restartLoader(ThreeTypes.getInt(mType), null, this);
    }



}
