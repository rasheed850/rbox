package com.capellasolutions.recoverynetwork2;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import smpte2022lib.FecReceiver;
import smpte2022lib.RtpPacket;

/**
 * Created by rrabata on 1/10/16.
 */
public class VideoUdpStreamReader {

    private Thread _UDPThread ;

    int _port;
    String _channelIP = "127.0.0.1";
    ArrayBlockingQueue<RtpPacket> _packets;

    ByteBuffer _byteBuffer;


    public VideoUdpStreamReader(int port, String channelIP)
    {
        _port = port;
        _packets = new ArrayBlockingQueue<RtpPacket>(1000);
        _channelIP = channelIP;
    }

    public ArrayBlockingQueue<RtpPacket> getPackQueue(){
        return _packets;
    }

    public void start() {
        if (_UDPThread == null) {
            _UDPThread = startUdpClient();
        }
    }
    public void stop() {
        if (_UDPThread != null) {
            _UDPThread.interrupt();
        }
        _UDPThread = null;
    }

    private Thread startUdpClient() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                MulticastSocket socket = null;
                try {


                    socket = new MulticastSocket(_port);
                    socket.joinGroup(InetAddress.getByName(_channelIP));
                    socket.setBroadcast(true);

                    byte buf[] = new byte[2024];
                    DatagramPacket packet = new DatagramPacket(buf, 2024);
                    FecReceiver receiver = new FecReceiver(null,_packets);
                    while(true)
                    {
                        socket.receive(packet);
                        receiver.put( new RtpPacket( packet.getData() , packet.getLength()), false);
                    }


                } catch (SocketException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    if (socket != null) {
                        socket.close();
                    }
                }
            }
        };

        thread.start();
        return thread;
    }



}
