package com.theshmuz.app.fragments;

import java.util.Formatter;
import java.util.Locale;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.analytics.tracking.android.EasyTracker;
import com.theshmuz.app.D;
import com.theshmuz.app.Entry;
import com.theshmuz.app.LoginHelper;
import com.theshmuz.app.SharedPrefWrap;
import com.theshmuz.app.activity.AccountActivity;
import com.theshmuz.app.activity.PrintDialogActivity;
import com.theshmuz.app.R;
import com.theshmuz.app.Shmuz;
import com.theshmuz.app.ShmuzHelper;
import com.theshmuz.app.UpdatorStatus;
import com.theshmuz.app.loaders.BitmapWorkerTask;
import com.theshmuz.app.loaders.EnsureDeleteTask;
import com.theshmuz.app.loaders.ItemLoader;
import com.theshmuz.app.loaders.SaveDownloadIdTask;
import com.theshmuz.app.streamer.NowPlaying;
import com.theshmuz.app.streamer.PlaybackState;
import com.theshmuz.app.streamer.Streamer;
import com.theshmuz.app.util.DiskCache;
import com.theshmuz.app.util.DownloadHelper;
import com.theshmuz.app.util.ThreeTypes;
import com.theshmuz.app.util.Version11Helper;
import com.theshmuz.app.util.VideoController;

public class ItemDetailFragment extends Fragment implements LoaderCallbacks<Entry>, OnClickListener, OnErrorListener, OnPreparedListener, OnCompletionListener, OnSeekBarChangeListener {

    private DownloadManager mDownloadManager;
    private SharedPrefWrap sharedPrefWrap;

    private Entry mItem;
    private String mId;
    private String mType;
    private int mAccessLevel;
    private boolean mAllowsSharing;

    private RelativeLayout rootView;
    private ScrollView scroller;

    private TextView viewTitle;
    private ImageView viewImage;
    private TextView viewContent;

    private Formatter mFormatter;
    private StringBuilder mFormatBuilder;
    private boolean mDragging;

    private ViewStub audioPlayerStub;
    private View viewPlayer;
    private ImageButton buttonPlay;
    private SeekBar audioPlayerSeek;
    private TextView audioPlayerStatus;
    private TextView audioPlayerPosition;
    private TextView audioPlayerDuration;
    private View viewTease;

    private ViewStub videoPlayerStub;
    private View viewVideoWrap;
    private VideoView viewVideo;
    private ProgressBar videoLoader;
    private TextView videoErrorText;
    private VideoController videoController;

    private ShmuzHelper db;
    private LoginHelper mLoginHelper;
    private PlaybackState state;
    private DiskCache mDiskCache;
    private UpdatorStatus updatorStatus;

    private static final int LOADER_ITEM = 2;

    public static ItemDetailFragment newInstance(String type, String id, int accessLevel, boolean allowSharing) {
        ItemDetailFragment myFragment = new ItemDetailFragment();

        Bundle args = new Bundle();
        args.putString("id", id);
        args.putString("type", type);
        args.putInt("access", accessLevel);
        args.putBoolean("share", allowSharing);
        myFragment.setArguments(args);

        return myFragment;
    }

    public ItemDetailFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Shmuz app = (Shmuz) getActivity().getApplication();
        db = app.db;
        state = app.state;
        mDiskCache = app.diskCache;
        updatorStatus = app.updator;
        mLoginHelper = app.getLoginHelper();
        sharedPrefWrap = SharedPrefWrap.getInstance(app);

        EasyTracker.getInstance().setContext(getActivity());

        mDownloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);

        Bundle args = getArguments();
        if (args.containsKey("id")) {
            mId = args.getString("id");
            mType = args.getString("type");
            mAllowsSharing = args.getBoolean("share");
            mAccessLevel = args.getInt("access");
        }
        else {
            if(D.D) Log.e("ONCREATE", "GOT TO ITEM DETAIL: NO ID");
        }

        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        getLoaderManager().initLoader(LOADER_ITEM, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mType != null && mId != null) {
            if (!D.D) EasyTracker.getTracker().sendView(mType + " - #" + mId);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        updateAudioPlayer();
        resumeVideo();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter(Streamer.PONGCMD));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateAudioPlayer();
        }
    };

    @Override
    public void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        pauseVideo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = (RelativeLayout) inflater.inflate(R.layout.item_detail, container, false);

        scroller = (ScrollView) rootView.findViewById(R.id.scroller);
        viewTitle = (TextView) rootView.findViewById(R.id.title);
        viewImage = (ImageView) rootView.findViewById(R.id.art);
        viewContent = (TextView) rootView.findViewById(R.id.content);
        audioPlayerStub = (ViewStub) rootView.findViewById(R.id.player);
        videoPlayerStub = (ViewStub) rootView.findViewById(R.id.videoStub);

        doFill();
        updateAccessControls();

        return rootView;
    }

    private void updateAccessControls() {
        boolean hasAccess = true; // temp override. really should properly remove this functionality...
        if(hasAccess) {
            //nothing to do
            if(viewTease != null) {
                viewTease.setVisibility(View.GONE);
            }
            if(buttonPlay != null) {
                buttonPlay.setEnabled(true);
            }
        }
        else {
            if(viewPlayer != null) {
                if(viewTease == null) {
                    //removed
                }
            }
            if(buttonPlay != null) {
                buttonPlay.setEnabled(false);
            }
        }
    }

    private void doFill() {
        if(viewTitle == null) return;

        getActivity().supportInvalidateOptionsMenu();

        if (mItem != null) {
            viewTitle.setText(mItem.title);
            viewContent.setText(mItem.content);

            if(mItem.urlArtwork != null) {
                //placeholder...
                viewImage.setVisibility(View.VISIBLE);

                BitmapWorkerTask task = new BitmapWorkerTask(viewImage, null, mDiskCache, false);
                if(Build.VERSION.SDK_INT >= 11) Version11Helper.runAsyncTask(task, mItem.urlArtwork);
                else task.execute(mItem.urlArtwork);
            }

            if(mItem.urlAudio != null) {
                if(viewPlayer == null) {
                    viewPlayer = audioPlayerStub.inflate();

                    buttonPlay = (ImageButton) viewPlayer.findViewById(R.id.playButton);
                    buttonPlay.setOnClickListener(this);

                    audioPlayerDuration = (TextView) viewPlayer.findViewById(R.id.durationTime);
                    audioPlayerPosition = (TextView) viewPlayer.findViewById(R.id.positionTime);
                    audioPlayerStatus = (TextView) viewPlayer.findViewById(R.id.playerStatus);
                    audioPlayerSeek = (SeekBar) viewPlayer.findViewById(R.id.seeker);
                    audioPlayerSeek.setOnSeekBarChangeListener(this);
                    audioPlayerSeek.setMax(1000);

                }

                updateAudioPlayer();
                updateAccessControls();
            }

            else if(mItem.urlVideo != null) {
                if(viewVideoWrap == null){
                    viewVideoWrap = videoPlayerStub.inflate();

                    viewVideoWrap.setOnClickListener(this);

                    viewVideo = (VideoView) viewVideoWrap.findViewById(R.id.video);
                    viewVideo.setOnErrorListener(this);
                    viewVideo.setOnPreparedListener(this);
                    viewVideo.setOnCompletionListener(this);

                    videoLoader = (ProgressBar) viewVideoWrap.findViewById(R.id.videoLoading);
                    videoErrorText = (TextView) viewVideoWrap.findViewById(R.id.videoErrorTxt);
                    videoErrorText.setOnClickListener(this);

                    videoController = (VideoController) viewVideoWrap.findViewById(R.id.player);
                    videoController.setup(viewVideo, viewVideoWrap);
                }

                doVideoOrientationChange(getActivity().getResources().getConfiguration().orientation);

                viewVideoWrap.setVisibility(View.VISIBLE);
                startVideo();

                videoLoader.setVisibility(View.VISIBLE);
            }

            else {
                //do nothing for now...
            }
        }
    }

    private void doVideoOrientationChange(int newOrientation) {
        if(newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            LayoutParams newParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            viewVideoWrap.setLayoutParams(newParams);
        }
        else {
            //use default...
            LayoutParams newParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
            viewVideoWrap.setLayoutParams(newParams);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(viewVideoWrap != null) {
            doVideoOrientationChange(newConfig.orientation);
        }
    }

    private void updateAudioPlayer() {
        if(viewPlayer == null) return;

        NowPlaying now = state.nowPlaying;
        if(now != null && now.isSame(mType, mId)){
            if(state.currentState == PlaybackState.PLAYING) buttonPlay.setImageResource(R.drawable.av_stop);
            else if(state.currentState == PlaybackState.STOPPED) buttonPlay.setImageResource(R.drawable.av_play);

            //seekbar only if duration is known...
            if(state.currentDuration > 0){
                audioPlayerSeek.setEnabled(true);
                audioPlayerDuration.setText(stringForTime(state.currentDuration));
                if(!mDragging) {
                    audioPlayerPosition.setText(stringForTime(state.currentPosition));
                    long pos = 1000L * state.currentPosition / state.currentDuration;
                    audioPlayerSeek.setProgress((int) pos);
                }
            }
            else{
                audioPlayerSeek.setEnabled(false);
                audioPlayerDuration.setText("");
                audioPlayerPosition.setText("");
            }

            audioPlayerStatus.setText(state.currentStatus);
        }
        else {
            //we're NOT currently playing, but maybe we have a saved start position...
            buttonPlay.setImageResource(R.drawable.av_play);
            audioPlayerStatus.setText(R.string.status_empty);
            audioPlayerSeek.setEnabled(false);

            if(mItem != null) {
                audioPlayerPosition.setText(stringForTime(mItem.startPosition));
                if(mItem.duration > 0) {
                    audioPlayerDuration.setText(stringForTime(mItem.duration));
                    long pos = 1000L * mItem.startPosition / mItem.duration;
                    audioPlayerSeek.setProgress((int) pos);
                }
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(!fromUser) return;
        sendSeekCmd(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mDragging = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mDragging = false;
    }

    private void sendSeekCmd(int position) {
        Intent seek = new Intent(getActivity(), Streamer.class);
        seek.setAction(Streamer.SERVICECMD);
        seek.putExtra(Streamer.CMDNAME, Streamer.CMDSEEK);
        seek.putExtra("position", position);
        getActivity().getApplicationContext().startService(seek);
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    @Override
    public Loader<Entry> onCreateLoader(int id, Bundle args) {
        return new ItemLoader(getActivity().getApplicationContext(), db, mType, mId);
    }

    @Override
    public void onLoadFinished(Loader<Entry> loader, Entry item) {
        mItem = item;
        doFill();
    }

    @Override
    public void onLoaderReset(Loader<Entry> arg0) {

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if(mItem != null) {
            if(mItem.urlAudio != null && mItem.downloadId > 0) {
                menu.findItem(R.id.menuDelete).setVisible(true);
                menu.findItem(R.id.menuDownload).setVisible(false);
            }
            else if(mItem.urlAudio != null) {
                menu.findItem(R.id.menuDelete).setVisible(false);
                menu.findItem(R.id.menuDownload).setVisible(true);
            }
            else {
                menu.findItem(R.id.menuDelete).setVisible(false);
                menu.findItem(R.id.menuDownload).setVisible(false);
            }

            if(mItem.urlPdf != null) {
                menu.findItem(R.id.menuPrint).setVisible(true);
            }
            else {
                menu.findItem(R.id.menuPrint).setVisible(false);
            }

            if(mAllowsSharing && (mItem.urlAudio != null || mItem.urlVideo != null)) {
                menu.findItem(R.id.menuShare).setVisible(true);
            }
            else {
                menu.findItem(R.id.menuShare).setVisible(false);
            }
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){

            case R.id.menuShare:
                if(!D.D) EasyTracker.getTracker().sendEvent("ui_action", "menu_press", "Share Item Button", (long) 0);
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);

                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Download " + mItem.title);
                String urlToPutIn = mItem.urlVideo != null ? mItem.urlVideo : mItem.urlAudio;
                sendIntent.putExtra(Intent.EXTRA_TEXT, mItem.title + " - " + urlToPutIn);

                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_item_dialog_title)));
                return true;

            case R.id.menuPrint:
                if(!D.D) EasyTracker.getTracker().sendEvent("ui_action", "menu_press", "Print Button", (long) 0);
                String title;
                title = mItem.title;
                print(mItem.urlPdf, title);
                return true;

            case R.id.menuDownload:
                if(!D.D) EasyTracker.getTracker().sendEvent("ui_action", "menu_press", "Download Button", (long) 0);
                if(mItem != null && mItem.urlAudio != null) {
                    initiateDownload();
                }
                return true;

            case R.id.menuDelete:
                if(!D.D) EasyTracker.getTracker().sendEvent("ui_action", "menu_press", "Delete Button", (long) 0);
                if(mItem != null && mItem.downloadId > 0) {
                    //TODO: this can be slow
                    long timeThis = 0;
                    if(D.D) timeThis = System.currentTimeMillis();
                    Uri old = DownloadHelper.getDownload(mDownloadManager, mItem.downloadId);
                    if(D.D) {
                        long time = System.currentTimeMillis() - timeThis;
                        Log.d("TIMED DOWNLOAD GET::", "" + time);
                    }

                    int res = mDownloadManager.remove(mItem.downloadId);
                    if(D.D) Log.i("Deleted", "" + res);

                    if(old != null && old.getScheme() != null && !old.getScheme().equals("content")) {
                        EnsureDeleteTask deleteTask = new EnsureDeleteTask(this.getActivity());
                        deleteTask.execute(old);
                    }

                    SaveDownloadIdTask task = new SaveDownloadIdTask(db);
                    task.execute(mType, mId, Long.toString(0));
                    mItem.downloadId = 0;

                    getActivity().supportInvalidateOptionsMenu();
                    updatorStatus.setStatus(UpdatorStatus.UPDATE_STATUS_SUCCESS);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean hasExternal() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public void initiateDownload() {
        if(mItem != null && mItem.urlAudio != null) {
            String urlToDownload = D.removeSslIfNecessary(mItem.urlAudio);
            Request request = null;
            try {
                request = new Request(Uri.parse(urlToDownload));
            } catch(IllegalArgumentException e) {
                if(D.D) Log.e("initiateDownload", "Got illegal arg exception: " + urlToDownload);
                return;
            }

            if(hasExternal()) {
                String fileName = Uri.parse(urlToDownload).getLastPathSegment();
                if(D.D) Log.i("FILENAME", fileName);
                request.setDestinationInExternalFilesDir(getActivity(), null, fileName);
            }

            request.setDescription(mItem.title);
            String downloadTitle = "The Shmuz";
            if(mType.equals(ThreeTypes.STR_SHMUZ)) {
                downloadTitle += " #" + mId;
            }
            request.setTitle(downloadTitle);

            long id = mDownloadManager.enqueue(request);
            if(D.D) Log.e("ID", ""+id);

            SaveDownloadIdTask task = new SaveDownloadIdTask(db);
            task.execute(mType, mId, Long.toString(id));
            mItem.downloadId = id;

            getActivity().supportInvalidateOptionsMenu();
            updatorStatus.setStatus(UpdatorStatus.UPDATE_STATUS_SUCCESS);
        }
    }

    private void print(String pdfUrl, String title) {
        Intent printIntent = new Intent(getActivity(), PrintDialogActivity.class);
        printIntent.putExtra("url", pdfUrl);
        printIntent.putExtra("title", title);
        startActivity(printIntent);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            //AUDIO
            case R.id.playButton:
                Intent startPlay = new Intent(this.getActivity(), Streamer.class);
                startPlay.setAction(Streamer. SERVICECMD);

                if(state.currentState == PlaybackState.STOPPED || !state.isSame(mType, mId)) {
                    String audioToPlay = D.removeSslIfNecessary(mItem.urlAudio);
                    NowPlaying toPlay = new NowPlaying(mType, mId, audioToPlay, mItem.title, mItem.startPosition, mItem.downloadId);
                    startPlay.putExtra("nowplaying", toPlay);
                    startPlay.putExtra(Streamer.CMDNAME, Streamer.CMDPLAY);

                    sharedPrefWrap.incrementPlayCount();
                }
                else {
                    startPlay.putExtra(Streamer.CMDNAME, Streamer.CMDSTOP);
                }

                getActivity().startService(startPlay);
                break;

            //VIDEO
            case R.id.videoErrorTxt:
                v.setVisibility(View.GONE);
                startVideo();
                break;

            case R.id.videoWrap:
                if(videoController != null) {
                    videoController.toggle();
                }
                break;

            case R.id.btnLogin:
                Intent toStart = new Intent(getActivity(), AccountActivity.class);
                toStart.putExtra("finish", true);
                startActivityForResult(toStart, 0);
                break;

            case R.id.btnSignup:
                Intent signupIntent;
                // removed
                break;
        }
    }

    private void startVideo() {
        if(mItem == null) return;

        Intent stopBroadcast = new Intent(Streamer.SERVICECMD);
        stopBroadcast.putExtra(Streamer.CMDNAME, Streamer.CMDSTOP);
        getActivity().sendBroadcast(stopBroadcast);
        viewVideo.stopPlayback();
        Uri videoUri = Uri.parse(mItem.urlVideo);
        if(mItem.urlVideo.startsWith("https://player.vimeo.com/video/")) {
            String vimeoId = videoUri.getLastPathSegment();
            videoUri = Uri.parse("http://theshmuzapi.appspot.com/vimeo/redir/" + vimeoId);
        }
        viewVideo.setVideoURI(videoUri);
        viewVideo.start();
    }

    private boolean isTemporaryPause;
    private void pauseVideo() {
        if(viewVideo == null) return;
        if(viewVideo.isPlaying()) {
            isTemporaryPause = true;
            viewVideo.pause();
        }
    }
    private void resumeVideo() {
        if(viewVideo == null) return;
        if(isTemporaryPause) {
            viewVideo.start();
            isTemporaryPause = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(viewVideo != null) {
            viewVideo.stopPlayback();
        }
    }

    @Override
    public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
        videoErrorText.setVisibility(View.VISIBLE);
        videoLoader.setVisibility(View.GONE);
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        videoLoader.setVisibility(View.GONE);
        videoErrorText.setVisibility(View.GONE);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

}
