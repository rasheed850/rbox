package com.google.android.exoplayer.demo.player;

import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.demo.player.DemoPlayer.RendererBuilder;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.text.TextTrackRenderer;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.capellasolutions.recoverynetwork2.RtpDataSource;
import com.google.android.exoplayer.upstream.TransferListener;
import com.google.android.exoplayer.upstream.UdpDataSource;

import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;

import java.io.IOException;

/**
 * Created by rrabata on 1/15/16.
 */
public class RtpUdpRendererBuilder implements RendererBuilder {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

    private final Context context;
    private final String userAgent;
    private final Uri uri;


    private DataSource dataSource;

    public RtpUdpRendererBuilder(Context context, String userAgent, Uri uri) {
        this.context = context;
        this.userAgent = userAgent;
        this.uri = uri;
    }

    public void buildDataSource(TransferListener transferListener){
        String scheme = uri.getScheme();

        if (scheme.equalsIgnoreCase("UDP")){
            dataSource = new UdpDataSource(transferListener);
        }
        else if (scheme.equalsIgnoreCase("RTP")) {
            dataSource = new RtpDataSource(transferListener);
        }

    }

    @Override
    public void buildRenderers(DemoPlayer player) {
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);

        // Build the video and audio renderers.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(player.getMainHandler(),
                null);

        buildDataSource(bandwidthMeter);

        Uri simpleUri = Uri.parse( uri.getHost() + ":" + uri.getPort());

        ExtractorSampleSource sampleSource = new ExtractorSampleSource(simpleUri, dataSource, allocator,
                BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);

        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(context,
                sampleSource, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 10000, player.getMainHandler(),
                player, 50);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource,
                null, true, player.getMainHandler(), player, AudioCapabilities.getCapabilities(context));
        TrackRenderer textRenderer = new TextTrackRenderer(sampleSource, player,
                player.getMainHandler().getLooper());

        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[DemoPlayer.RENDERER_COUNT];
        renderers[DemoPlayer.TYPE_VIDEO] = videoRenderer;
        renderers[DemoPlayer.TYPE_AUDIO] = audioRenderer;
        renderers[DemoPlayer.TYPE_TEXT] = textRenderer;
        player.onRenderers(renderers, bandwidthMeter);
    }

    @Override
    public void cancel() {
        // close datasource
        if ( dataSource != null) {
            try {
                dataSource.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
