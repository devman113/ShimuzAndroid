package com.theshmuz.app.fragments;

import java.util.concurrent.RejectedExecutionException;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.theshmuz.app.D;
import com.theshmuz.app.R;
import com.theshmuz.app.Shmuz;
import com.theshmuz.app.ShmuzHelper;
import com.theshmuz.app.SimpleCursorLoader;
import com.theshmuz.app.business.LoaderResultObj;
import com.theshmuz.app.fragments.ShmuzListFragment.Callbacks;
import com.theshmuz.app.loaders.BitmapWorkerTask;
import com.theshmuz.app.loaders.LatestParshaLoader;
import com.theshmuz.app.util.DiskCache;
import com.theshmuz.app.util.Version11Helper;

public class ParshaFragment extends Fragment implements OnClickListener, RefreshableFragment, LoaderCallbacks<LoaderResultObj> {

    private Callbacks mCallbacks = sDummyCallbacks;

    private TextView parshaTitle;
    private ImageView parshaImage;

    private TextView burstTitle;
    private ImageView burstImage;

    private ShmuzHelper db;
    private DiskCache mDiskCache;
    private Bitmap cachedStub;
    private LruCache<String, Bitmap> mMemoryCache;
    private LoaderResultObj mItem;

    public static ParshaFragment newInstance() {
        ParshaFragment myFragment = new ParshaFragment();
        return myFragment;
    }

    public ParshaFragment(){}

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Shmuz app = (Shmuz) getActivity().getApplication();
        db = app.db;
        mDiskCache = app.diskCache;

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
            if(D.D) Log.d("ParshaFragment", "Cache hit on the cache!");
        }
        mMemoryCache = null;

        EasyTracker.getInstance().setContext(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_parsha, container, false);

        parshaTitle = (TextView) rootView.findViewById(R.id.parshaTitle);
        parshaImage = (ImageView) rootView.findViewById(R.id.parshaImage);

        Button parshaButton = (Button) rootView.findViewById(R.id.parshaArchiveBtn);
        parshaButton.setOnClickListener(this);

        View parshaContainer = rootView.findViewById(R.id.parshaLatestContainer);
        parshaContainer.setOnClickListener(this);

        burstTitle = (TextView) rootView.findViewById(R.id.burstTitle);
        burstImage = (ImageView) rootView.findViewById(R.id.burstImage);

        Button burstButton = (Button) rootView.findViewById(R.id.burstArchiveBtn);
        burstButton.setOnClickListener(this);

        View burstContainer = rootView.findViewById(R.id.burstLatestContainer);
        burstContainer.setOnClickListener(this);

        doFill();

        return rootView;
    }

    private void doFill() {
        if(parshaTitle == null) return;

        getActivity().supportInvalidateOptionsMenu();

        if (mItem != null) {
            parshaTitle.setText(mItem.parshaTitle);
            burstTitle.setText(mItem.parshaTitle);
            if(mItem.parshaArtwork != null) {
                loadBitmap(mItem.parshaArtwork, parshaImage);
                loadBitmap(mItem.parshaArtwork, burstImage);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(1, null, this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.parshaLatestContainer:
                if(mItem != null && mItem.parshaId != null) {
                    String id = mItem.parshaId;
                    mCallbacks.onItemSelected(ShmuzHelper.TABLE_PARSHA, id);
                }
                else {
                    if(D.D) Log.e("onClick", "Got no parsha id system");
                }
                break;
            case R.id.parshaArchiveBtn:
                mCallbacks.onSeriesSelected(ShmuzHelper.TABLE_PARSHA, "Shmuz on the Parsha", true, ShmuzHelper.ACCESS_NONE, true);
                break;
            case R.id.burstLatestContainer:

                break;
            case R.id.burstArchiveBtn:

                break;
        }
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

        mCallbacks = sDummyCallbacks;
    }



    @Override
    public Loader<LoaderResultObj> onCreateLoader(int id, Bundle args) {
        return new LatestParshaLoader(getActivity().getApplicationContext(), db);
    }

    @Override
    public void onLoadFinished(Loader<LoaderResultObj> loader, LoaderResultObj result) {
        mItem = result;
        doFill();
    }

    @Override
    public void onLoaderReset(Loader<LoaderResultObj> loader) {

    }

    @Override
    public void refresh() {
        getLoaderManager().restartLoader(1, null, this);
    }


}