package com.capellasolutions.recoverynetwork2;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.exoplayer.demo.player.DemoPlayer;
import com.google.android.exoplayer.demo.player.RtpUdpRendererBuilder;
import com.google.code.microlog4android.config.PropertyConfigurator;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends Activity  implements  IVLCVout.Callback, LibVLC.HardwareAccelerationError {

    private static final String TAG = "MainActivity";

    WifiManager.MulticastLock mMulticastLock;

    private SurfaceView mVideoSurfaceView;
    private SurfaceHolder mHolder;
    private WebView mWebView;

    private DemoPlayer mDemoPlayer;


    private List<String> mChannels;
    private boolean mIsShowingTV = true;
    private int mCurrentChannel = 0;

    private final static String SETTING_MODE = "VLCPLAYER";
    private final static String SETTING_URL = "http://192.168.0.225/direcweb/MainMenu.aspx";

    private LibVLC mLibVLC;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;

    private List<String> getChannelList(){
        List<String> channelList = new ArrayList<>();

        channelList.add("rtp://239.100.100.100:5000");
        channelList.add("rtp://239.100.100.101:5000");
        channelList.add("rtp://239.100.100.102:5000");;
        channelList.add("udp://239.100.100.103:5000");;

        return channelList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PropertyConfigurator.getConfigurator(this).configure();

        setContentView(R.layout.activity_main);

        mWebView = (WebView)findViewById(R.id.web_content);
        mVideoSurfaceView = (SurfaceView)findViewById(R.id.video_content);
        mHolder = mVideoSurfaceView.getHolder();

        mChannels = getChannelList();

        aquireMulticastLock();
        configureWebView();
        configureVideoSurfaceView();
        setTvVisibility(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseVlcPlayer();
    }

    @Override
    protected void onResume(){
        super.onResume();
        createVLCMediaPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseVlcPlayer();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //setVlcPlayerSize(mVideoWidth, mVideoHeight);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.i("key pressed", String.valueOf(event.getKeyCode()));
        if ( event.getAction() == KeyEvent.ACTION_DOWN && onKey(event.getKeyCode()) )
            return true;
        else
            return super.dispatchKeyEvent(event);
    }



    private boolean onKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_M) // m
        {
            toggleTvMenu();

            if(mIsShowingTV)
                return true; // eat the m
            else // showing web
                return false; // let the m go through.
        }

        if (mIsShowingTV) {
            boolean channelChange = false;
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_DPAD_UP: // up
                case 38: // up
                    mCurrentChannel =  (mCurrentChannel + 1) % mChannels.size();
                    channelChange = true;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN: // down
                case 40: // down
                    mCurrentChannel =  (mCurrentChannel - 1);
                    if(mCurrentChannel <0)
                        mCurrentChannel = mChannels.size() + mCurrentChannel;
                    channelChange = true;
                    break;
            }

            if(channelChange)
            {
                reloadPlayer();
                return true;
            }

        }

        return false;
    }

    private void reloadPlayer(){

        if( SETTING_MODE.equals("EXOPLAYER")) {
            releaseMediaPlayer();
            createMediaPlayer();
        } else if ( SETTING_MODE.equals("VLCPLAYER"))
        {
            releaseVlcPlayer();
            createVLCMediaPlayer();
        }

    }

    private void toggleTvMenu(){
        mIsShowingTV = !mIsShowingTV;
        setTvVisibility(mIsShowingTV);
    }

    private void setTvVisibility(boolean showTv){
        if(showTv){

            switch (SETTING_MODE)
            {
                case "EXOPLAYER":
                    createMediaPlayer();
                    break;
                case "VLCPLAYER":
                    createVLCMediaPlayer();
                    break;
            }

            mVideoSurfaceView.setVisibility(View.VISIBLE);
            mWebView.setVisibility(View.INVISIBLE);
            mWebView.onPause();
        } else {
            releaseVlcPlayer();
            releaseMediaPlayer();

            mVideoSurfaceView.setVisibility(View.INVISIBLE);
            mWebView.setVisibility(View.VISIBLE);
            mWebView.onResume();
        }
    }



    private void aquireMulticastLock() {
        releaseMulticastLock();

        WifiManager wm = (WifiManager)  getSystemService(Context.WIFI_SERVICE);
        mMulticastLock = wm.createMulticastLock("mylock");
        mMulticastLock.acquire();
    }

    private void releaseMulticastLock() {
        if( mMulticastLock == null)
            return;

        mMulticastLock.release();
        mMulticastLock = null;
    }

    private void createMediaPlayer()  {
        releaseMediaPlayer();

        String userAgent = System.getProperty("http.agent");
        RtpUdpRendererBuilder rendererBuilder = new RtpUdpRendererBuilder(this, userAgent,
                Uri.parse(mChannels.get(mCurrentChannel))
        );

        mDemoPlayer = new DemoPlayer(rendererBuilder);
        mDemoPlayer.prepare();
        mDemoPlayer.setPlayWhenReady(true);
        mDemoPlayer.setSurface(mVideoSurfaceView.getHolder().getSurface());
    }

    private void releaseMediaPlayer() {
        if ( mDemoPlayer != null ) {
            mDemoPlayer.stop();
            mDemoPlayer.release();
            mDemoPlayer = null;
        }
    }

    private void configureVideoSurfaceView() {
        mVideoSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void configureWebView() {
        mWebView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.loadUrl(SETTING_URL);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

        });
    }

    /*************
     * VLC Player
     *************/

    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        //setVlcPlayerSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {

    }

    @Override
    public void eventHardwareAccelerationError() {
        // Handle errors with hardware acceleration
        Log.e(TAG, "Error with hardware acceleration");
        this.releaseVlcPlayer();
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

    private void setVlcPlayerSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;

        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if(mHolder == null || mVideoSurfaceView == null)
            return;

        // get screen size
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        // force surface buffer size
        mHolder.setFixedSize(mVideoWidth, mVideoHeight);

        // set display size
        ViewGroup.LayoutParams lp = mVideoSurfaceView.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mVideoSurfaceView.setLayoutParams(lp);
        mVideoSurfaceView.invalidate();
    }

    private void createVLCMediaPlayer(){
        releaseVlcPlayer();

        showCurrentChannel();

        mHolder = mVideoSurfaceView.getHolder();
        try {
            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity
            mLibVLC = new LibVLC(options);
            mLibVLC.setOnHardwareAccelerationError(this);


            // Create media player
            mMediaPlayer = new MediaPlayer(mLibVLC);

            mMediaPlayer.setEventListener(mPlayerListener);

            // Set up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mVideoSurfaceView);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();

            Media m = new Media(mLibVLC, Uri.parse( mChannels.get(mCurrentChannel) ));
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();
        } catch (Exception e) {
            Toast.makeText(this, "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }

    private void showCurrentChannel() {
        String media = mChannels.get(mCurrentChannel);
        Toast toast = Toast.makeText(this, "Channel: " + mCurrentChannel + " | " + media, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
                0);
        toast.show();
    }

    private void releaseVlcPlayer() {
        if (mLibVLC == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        mHolder = null;
        mLibVLC.release();
        mLibVLC = null;

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);
    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<MainActivity> mOwner;

        public MyPlayerListener(MainActivity owner) {
            mOwner = new WeakReference<MainActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            MainActivity player = mOwner.get();

            switch(event.type) {
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.releaseVlcPlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }
}
