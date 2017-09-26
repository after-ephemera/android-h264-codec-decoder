/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.basicmediadecoder;


import android.Manifest;
import android.animation.TimeAnimator;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.PriorityQueue;

/**
 * This activity uses a {@link android.view.TextureView} to render the frames of a video decoded using
 * {@link android.media.MediaCodec} API.
 */
public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    private final static int QUEUE_INITIAL_SIZE = 50;

    private TextureView mPlaybackView;
    private TimeAnimator mTimeAnimator = new TimeAnimator();

    // A utility that wraps up the underlying input and output buffer processing operations
    // into an east to use API.
    private MediaCodec mediaCodec;

    private NALParser nalParser;
    private Boolean textureReady = false;
    DecodeFrameTask frameTask;
    MenuItem playButton;
    FileOutputStream fileOutputStream = null;
    DatagramSocket clientSocket;

    private final PriorityQueue<ByteBuffer> nalQueue = new PriorityQueue<>(QUEUE_INITIAL_SIZE);


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_main);
        mPlaybackView = (TextureView) findViewById(R.id.PlaybackView);
        mPlaybackView.setSurfaceTextureListener(this);
        frameTask = new DecodeFrameTask();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // Set up the UDP socket for receiving data.
        try {
            clientSocket = new DatagramSocket(1900);
            nalParser = new NALParser();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        textureReady = true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        playButton = menu.getItem(0);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mTimeAnimator != null && mTimeAnimator.isRunning()) {
            mTimeAnimator.end();
        }

    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_play) {
//            mAttribView.setVisibility(View.VISIBLE);
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            if(textureReady){
                Boolean canRun = true;
                if(frameTask.getStatus() == AsyncTask.Status.RUNNING){
                    frameTask.cancel(true);
                    Log.w("Main", "Can run: " + canRun);
                }
                PacketReceiverTask packetReceiverTask = new PacketReceiverTask();
                packetReceiverTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                frameTask = new DecodeFrameTask();
                frameTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                playButton.setEnabled(false);
            }
        }
        return true;
    }


    long startUs;
    public void startPlayback() {
        startUs = System.nanoTime() / 1000;

        File fileOut = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "testFrameOutput.h264");
        try {
            fileOutputStream = new FileOutputStream(fileOut, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                480, 640);
        NALBuffer sps;
        // Get NAL Units until an SPS unit is found.
        while(true){
            while(peekQueue() == null);
            sps = nalParser.getNext(pollQueue());
            Log.d("Main", "searching for an SPS frame...");
            if((sps.buffer.get(4) & 0x1f) == 0x07){
                // Found SPS!
                Log.d("Main", "Found an SPS Frame");
                break;
            }
        }

        while(peekQueue() == null);
        NALBuffer pps = nalParser.getNext(pollQueue());
        int size = sps.size + pps.size /*+ next.size*/;
//        ByteBuffer trueBuffer = ByteBuffer.allocate(size);
//        trueBuffer.put(sps.buffer.duplicate());
//        trueBuffer.put(pps.buffer.duplicate());
//        trueBuffer.put(next.buffer.duplicate());


        if (sps == null) {
            Log.e("Main", "Couldn't get NAL");
//            throw new Exception("Failed");
            return;
        }
//        ByteBuffer frame = sps.buffer.duplicate();


        format.setByteBuffer("csd-0", sps.buffer);
        format.setByteBuffer("csd-1", pps.buffer);
        try {
            fileOutputStream.write(sps.buffer.array());
            fileOutputStream.write(pps.buffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mediaCodec.configure(format, new Surface(mPlaybackView.getSurfaceTexture()), null, 0);
            mediaCodec.start();
            textureReady = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

//        int inputIndex;
//        while ((inputIndex = mediaCodec.dequeueInputBuffer(-1)) < 0) {
////            Log.d("Main", "Input index: " + inputIndex);
//        }
//
//        ByteBuffer codecBuffer = mediaCodec.getInputBuffer(inputIndex);
//        codecBuffer.put(trueBuffer);
//
//        mediaCodec.queueInputBuffer(inputIndex, 0, size+1, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
//
//        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//        int outputIndex = mediaCodec.dequeueOutputBuffer(info, 0);
//        if (outputIndex >= 0) {
//            mediaCodec.releaseOutputBuffer(outputIndex, true);
//        }

        Log.e("Main", "CONFIG FRAMES ABOVE THIS POINT ---------------------");

        while(true) {

            while(peekQueue() == null);
             NALBuffer nb = nalParser.getNext(pollQueue());
            if (nb == null) {
                Log.e("Main", "Couldn't get NAL");
//            throw new Exception("Failed");
                return;
            }

             //Check type and load buffer based on type.
            try {
//                int type = nb.buffer.get(5) & 0x1f; // Low 5 bits of the 5th byte gives the type identifier.
//                if(type == 0x06) continue;
            } catch (Exception e) {
                e.printStackTrace();
            }
            ByteBuffer frame = nb.buffer.duplicate();

            int inputIndex;
            while ((inputIndex = mediaCodec.dequeueInputBuffer(-1)) < 0) {
                Log.d("Main", "Input index: " + inputIndex);
            }
            Log.d("Main", "Final Input index: " + inputIndex);

            ByteBuffer codecBuffer = mediaCodec.getInputBuffer(inputIndex);
            codecBuffer.put(frame);

            try {
                fileOutputStream.write(frame.array());
            } catch (IOException e) {
                e.printStackTrace();
            }

            long presentationTimeMS = (System.nanoTime()/1000) - startUs;

            int flags = 0;
            // If this is the final NAL Unit we can signify the end of the stream.
//            if(nb.lastNAL){
//                flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
//            }
            mediaCodec.queueInputBuffer(inputIndex, 0, nb.size+1, presentationTimeMS, flags);

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outputIndex = mediaCodec.dequeueOutputBuffer(info, 0);
            if (outputIndex >= 0) {
                mediaCodec.releaseOutputBuffer(outputIndex, true);
            }
        }
    }

    private class DecodeFrameTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            startPlayback();
            return null;
        }

        @Override
        protected void onPostExecute(String result){
            try{
                mediaCodec.stop();
                mediaCodec.release();
            } catch (Exception e){
                e.printStackTrace();
            }
//            nalParser.release();
        }
    }

    private class PacketReceiverTask extends AsyncTask<String, String, String> {
        private DatagramPacket dPacket;

        public PacketReceiverTask(){
        }

        @Override
        protected String doInBackground(String... strings) {
            // Continuously receive packets and put them on a priorityqueue
            while(true){
                try {
                    byte[] buff = new byte[100535];
                    dPacket = new DatagramPacket(buff, 100535);
                    clientSocket.receive(dPacket);
//                    Log.d("PacketReceiver", "Length: " + dPacket.getLength());
                    addToQueue(ByteBuffer.wrap(dPacket.getData(), dPacket.getOffset(), dPacket.getLength()).duplicate());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ByteBuffer pollQueue(){
        synchronized (nalQueue){
            return nalQueue.poll();
        }
    }

    private ByteBuffer peekQueue(){
        synchronized (nalQueue){
            return nalQueue.peek();
        }
    }

    private boolean addToQueue(ByteBuffer b){
        synchronized (nalQueue){
            return nalQueue.add(b);
        }
    }

    private int checkNALType(int type) throws Exception {
        type = type & 0x1f;
        System.out.println("Type: " + String.format("0x%02X", type));
        switch(type){
            case 0x01: // P Frame - Coded slice of a non-IDR picture (VCL)
                break;
            case 0x05: // I Frame - Coded slice of an IDR picture (VCL)
                break;
            case 0x07: // SPS Parameter - Sequence parameter set (non-VCL)
                break;
            case 0x08: // PPS Parameter - Picture parameter set (non-VCL)
                break;
            default:
                throw new Exception("Failed to determine NAL Unit type with type number " + type);
        }
        return -1;
    }

    @Override
    protected void onStop() {
        try {
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaCodec.release();
        super.onStop();
    }
}
