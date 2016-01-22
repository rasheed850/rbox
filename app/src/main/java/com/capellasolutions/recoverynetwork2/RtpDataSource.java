/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.capellasolutions.recoverynetwork2;

import android.media.CamcorderProfile;

import com.google.android.exoplayer.C;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.upstream.TransferListener;
import com.google.android.exoplayer.upstream.UdpDataSource;
import com.google.android.exoplayer.upstream.UriDataSource;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import smpte2022lib.FecReceiver;
import smpte2022lib.RtpPacket;

/**
 * A UDP {@link DataSource}.
 */
public final class RtpDataSource implements UriDataSource {

    /**
     * Thrown when an error is encountered when trying to read from a {@link UdpDataSource}.
     */
    public static final class RtpDataSourceException extends IOException {

        public RtpDataSourceException(String message) {
            super(message);
        }

        public RtpDataSourceException(IOException cause) {
            super(cause);
        }

        public RtpDataSourceException(Exception cause) {
            super(cause);
        }
    }

    /**
     * The default maximum datagram packet size, in bytes.
     */
    public static final int DEFAULT_MAX_PACKET_SIZE = 2000;

    private final TransferListener listener;
    private final DatagramPacket packet;


    private DataSpec dataSpec;
    private DatagramSocket socket;
    private MulticastSocket multicastSocket;
    private InetAddress address;
    private InetSocketAddress socketAddress;
    private boolean opened;

    private byte[] packetBuffer;
    private int packetRemaining;

    private FecReceiver fecReceiver;
    private Queue<RtpPacket> packetQueue;
    private  RtpPacket rtpPacket;

    /**
     * @param listener An optional listener.
     */
    public RtpDataSource(TransferListener listener) {
        this(listener, DEFAULT_MAX_PACKET_SIZE);
    }

    /**
     * @param listener An optional listener.
     * @param maxPacketSize The maximum datagram packet size, in bytes.
     */
    public RtpDataSource(TransferListener listener, int maxPacketSize) {
        this.listener = listener;
        packetBuffer = new byte[maxPacketSize];
        packet = new DatagramPacket(packetBuffer, 0, maxPacketSize);
        packetQueue = new ArrayBlockingQueue<RtpPacket>(1000);
        fecReceiver = new FecReceiver(null, packetQueue);
    }

    @Override
    public long open(DataSpec dataSpec) throws RtpDataSourceException {
        this.dataSpec = dataSpec;
        String uri = dataSpec.uri.toString();
        String host = uri.substring(0, uri.indexOf(':'));
        int port = Integer.parseInt(uri.substring(uri.indexOf(':') + 1));

        try {
            address = InetAddress.getByName(host);
            socketAddress = new InetSocketAddress(address, port);
            if (address.isMulticastAddress()) {
                multicastSocket = new MulticastSocket(socketAddress);
                multicastSocket.joinGroup(address);
                socket = multicastSocket;
            } else if ( host.equalsIgnoreCase("10.0.2.2") ) {
                // emulator
                socket = new DatagramSocket(port);
            } else {
                socket = new DatagramSocket(socketAddress);
            }
        } catch (IOException e) {
            throw new RtpDataSourceException(e);
        }

        opened = true;
        if (listener != null) {
            listener.onTransferStart();
        }
        return C.LENGTH_UNBOUNDED;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws RtpDataSourceException {
        if (packetRemaining==0) {
            // We've read all of the data from the current packet. Get another.
            try {
                while(packetQueue.size() < 2) {
                    socket.receive(packet);
                    fecReceiver.put(new RtpPacket(packet.getData(), packet.getLength()), false);
                }
            } catch (IOException e) {
                throw new RtpDataSourceException(e);
            } catch (Exception e) {
                throw new RtpDataSourceException(e);
            }
            if (listener != null) {
                listener.onBytesTransferred(packetRemaining);
            }
        }

        if(packetRemaining==0)
        {
            //load current packet
            rtpPacket = packetQueue.remove();
            packetRemaining = rtpPacket.payload.length;
        }

        int bytesToRead = readBytesFromCurrentPacket(buffer, offset, readLength);

        return bytesToRead;
    }

    private int readBytesFromCurrentPacket(byte[] buffer, int offset, int readLength) {

        int packetOffset = rtpPacket.payload.length - packetRemaining;
        int bytesToRead = Math.min(packetRemaining, readLength);
        System.arraycopy(rtpPacket.payload, packetOffset, buffer, offset, bytesToRead);
        packetRemaining -= bytesToRead;
        return bytesToRead;
    }

    @Override
    public void close() {
        if (multicastSocket != null) {
            try {
                multicastSocket.leaveGroup(address);
            } catch (IOException e) {
                // Do nothing.
            }
            multicastSocket = null;
        }
        if (socket != null) {
            socket.close();
            socket = null;
        }
        address = null;
        socketAddress = null;
        packetRemaining = 0;
        if (opened) {
            opened = false;
            if (listener != null) {
                listener.onTransferEnd();
            }
        }
    }

    @Override
    public String getUri() {
        return dataSpec == null ? null : dataSpec.uri.toString();
    }

}
