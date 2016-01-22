package com.google.android.exoplayer.demo.player;

import android.content.Context;
import android.net.wifi.WifiManager;

import com.capellasolutions.recoverynetwork2.VideoUdpStreamReader;
import com.google.android.exoplayer.C;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.upstream.UriDataSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;

import smpte2022lib.RtpPacket;

/**
 * Created by rrabata on 1/16/16.
 */
public class RtpUriDataSource implements UriDataSource {

    private static final int MAX_FRAME_SIZE = (1024 * 512);
    private static final int BLOCK_SIZE = (1024 * 1024 * 8);


    private ByteBuffer _byteBuffer = ByteBuffer.allocate(BLOCK_SIZE);



    VideoUdpStreamReader _UDPReader;

    /*
    long _firstPositionAbsolute = 0;
    int _lastRequestedRelativePosition = 0;

    private static final int MAX_MTU_SIZE = 1500;
    private ByteBuffer _lastFrame = ByteBuffer.allocate(MAX_FRAME_SIZE);

    private int _firstBlock = 0;
    private int _length = 0;

    @Override
    public int readAt(long positionAbsolute, byte[] buffer, int offset, int size) throws IOException {
        try {
            size = size > 1316 ? 1316 : size;
            while ((_firstPositionAbsolute + _byteBuffer.position()) < positionAbsolute + size) {
                Thread.sleep(10);
                pullData();
            }

            _lastRequestedRelativePosition = (int) (positionAbsolute - _firstPositionAbsolute);


            if (positionAbsolute < _firstPositionAbsolute) {
                Log.e("MediaDataSource", "Requested data that was truncated, requested: " + positionAbsolute + " available " + _firstPositionAbsolute);
            }

            int startIndex = (int) (positionAbsolute - _firstPositionAbsolute);
            int endIndex = Math.min(startIndex + size, _byteBuffer.position());
            int sizeToCopy = endIndex - startIndex;

            ByteBuffer readBuffer = _byteBuffer.duplicate();
            readBuffer.flip();
            if (sizeToCopy >= 0 && startIndex >= 0) {
                readBuffer.position(startIndex);
                readBuffer.get(buffer, offset, sizeToCopy);
            } else {
                sizeToCopy = -1;
            }

            Log.d("MediaDataSource", "Bytes Read " + sizeToCopy);
            return sizeToCopy;
        } catch (Exception e) {
            Log.e("MediaDataSource", "readAt Exception", e);
            return -1;
        }
    }
    */



    @Override
    public String getUri() {
        return null;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {

        _UDPReader = new VideoUdpStreamReader(dataSpec.uri.getPort(), dataSpec.uri.getHost());
        _UDPReader.start();
        _byteBuffer.clear();

        return C.LENGTH_UNBOUNDED;
    }

    @Override
    public void close() throws IOException {
        _UDPReader.stop();
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        Queue<RtpPacket> queue = _UDPReader.getPackQueue();

        while (_byteBuffer.position() < readLength ) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (queue.size() > 0 && _byteBuffer.remaining() >= MAX_FRAME_SIZE) {
                RtpPacket packet = queue.remove();
                _byteBuffer.put(packet.payload);
            }

        }

        ByteBuffer readBuffer = _byteBuffer.duplicate();
        readBuffer.flip();
        if (readBuffer.remaining()>0) {
            readBuffer.position(0);
            readBuffer.get(buffer, offset, readLength);
        } else {
            readLength = C.LENGTH_UNBOUNDED;
        }

        return  readLength;
    }
}
