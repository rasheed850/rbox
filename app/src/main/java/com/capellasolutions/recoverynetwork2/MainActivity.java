package com.capellasolutions.recoverynetwork2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.exoplayer.demo.player.DemoPlayer;
import com.google.android.exoplayer.demo.player.RtpUdpRendererBuilder;
import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;
import com.google.code.microlog4android.appender.FileAppender;
import com.google.code.microlog4android.config.PropertyConfigurator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends Activity  implements SurfaceHolder.Callback {

    protected static final Logger logger = LoggerFactory.getLogger();
    protected FileAppender appender = new FileAppender();

    private SurfaceView mVideoSurfaceView;
    private WebView mWebView;

    private DemoPlayer demoPlayer;
    WifiManager.MulticastLock mMulticastLock;

    private List<String> channels;
    private boolean IsShowingTV = true;
    private int currentChannel = 0;



    @Override
    protected  void onDestroy(){
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PropertyConfigurator.getConfigurator(this).configure();

       // appender.setFileName(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/log.txt");
       // appender.setAppend(true);
       // logger.addAppender(appender);
       // logger.info("testing");

        setContentView(R.layout.activity_main);

        mWebView = (WebView)findViewById(R.id.web_content);
        mVideoSurfaceView = (SurfaceView)findViewById(R.id.video_content);

        channels = getChannelList();

        aquireMulticastLock();
        configureWebView();
        configureVideoSurfaceView();
        setTvVisibility(true);
    }


    @Override
    public  void surfaceCreated(SurfaceHolder holder){
        try {
            createMediaPlayer(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseMediaPlayer();
        super.onDestroy();
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

    private List<String> getChannelList(){
        List<String> channelList = new ArrayList<>();

        channelList.add("rtp://239.100.100.100:5000");
        channelList.add("rtp://239.100.100.100:5001");
        channelList.add("rtp://239.100.100.100:5002");
        channelList.add("rtp://239.100.100.100:5003");

        return channelList;
    }

    private boolean onKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_M) // m
        {
            toggleTvMenu();
            return true;
        }

        if (IsShowingTV) {
            boolean channelChange = false;
            switch (keyCode)
            {
                case 38: // up
                    currentChannel =  (currentChannel + 1) % channels.size();
                    channelChange = true;
                    break;
                case 40: // down
                    currentChannel =  (currentChannel - 1) % channels.size();
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
        releaseMediaPlayer();

        if ( mVideoSurfaceView.getHolder() != null )
        {
            try {
                createMediaPlayer(mVideoSurfaceView.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void toggleTvMenu(){
        IsShowingTV = !IsShowingTV;
        setTvVisibility(IsShowingTV);
    }

    private void setTvVisibility(boolean showTv){
        if(showTv){
            mVideoSurfaceView.setVisibility(View.VISIBLE);
            mWebView.setVisibility(View.INVISIBLE);
        } else {
            mVideoSurfaceView.setVisibility(View.INVISIBLE);
            mWebView.setVisibility(View.VISIBLE);
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

    private void createMediaPlayer(SurfaceHolder holder) throws IOException {
        releaseMediaPlayer();

        String userAgent = System.getProperty("http.agent");
        RtpUdpRendererBuilder rendererBuilder = new RtpUdpRendererBuilder(this, userAgent,
                Uri.parse(channels.get(currentChannel))
        );

        demoPlayer = new DemoPlayer(rendererBuilder);
        demoPlayer.prepare();
        demoPlayer.setPlayWhenReady(true);
        demoPlayer.setSurface(holder.getSurface());
    }

    private void releaseMediaPlayer() {
        if ( demoPlayer != null ) {
            demoPlayer.stop();
            demoPlayer.release();
            demoPlayer = null;
        }
    }

    private void configureVideoSurfaceView() {
        mVideoSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        mVideoSurfaceView.getHolder().addCallback(this);
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
        mWebView.loadUrl("http://192.168.0.225/direcweb/MainMenu.aspx");
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

        });
    }
}
