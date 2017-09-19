package com.example.android.basicmediadecoder;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

/**
 * Created by jkjensen on 9/19/17.
 */

public class UDPInputStream extends InputStream {

    private DatagramSocket dSocket;
    private DatagramPacket dPacket;
    private ByteBuffer dataRead;

    public UDPInputStream(DatagramSocket socketIn){
        dSocket = socketIn;

        byte[] buff = new byte[65535];
        dPacket = new DatagramPacket(buff, 65535);
    }

    @Override
    public int read() throws IOException {
        if(dataRead == null){
            dSocket.receive(dPacket);
            Log.d("UDPInputStream", "Size of packet: " + dPacket.getLength());
            ByteBuffer b = ByteBuffer.wrap(dPacket.getData());
            for(int i = 0; i < dPacket.getLength(); i++){
                System.out.print(" " + String.format("0x%02X", b.get()));
            }
            System.out.println();
            dataRead = ByteBuffer.wrap(dPacket.getData(), dPacket.getOffset(), dPacket.getLength());
        }

        if(dataRead.position() >= dataRead.limit()){
            dataRead = null;
            return read();
        } else{
            return dataRead.get();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if(dataRead == null){
            dSocket.receive(dPacket);
            Log.d("UDPInputStream", "Size of packet: " + dPacket.getLength());
            ByteBuffer b = ByteBuffer.wrap(dPacket.getData());
            for(int i = 0; i < dPacket.getLength(); i++){
                System.out.print(" " + String.format("0x%02X", b.get()));
            }
            System.out.println();
            dataRead = ByteBuffer.wrap(dPacket.getData(), dPacket.getOffset(), dPacket.getLength());
        }

        if(dataRead.position() >= dataRead.limit()){
            dataRead = null;
            return skip(n);
        } else{
            dataRead.position((int) (dataRead.position() + n));
            return n;
        }
    }
}
