package com.example.android.basicmediadecoder;

import android.os.AsyncTask;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Created by jkjensen on 9/13/17.
 */


public class NALParser {
    private static final int match[] = {
            0x00,
            0x00,
            0x00,
            0x01
    };

    // The inputStream representing the h.264 stream being read in.
    private InputStream inputStream;

    public NALParser(InputStream inputStreamIn) {
//        try {
//            inputStream = inputStreamIn;
////            inputStream = new FileInputStream("test.264");
//            inputStream.skip(4l);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public NALBuffer getNext(ByteBuffer currentFrame) {
        try {
//            inputStream = new FileInputStream("test.264");
            // skip the first four bytes
            currentFrame.get();
            currentFrame.get();
            currentFrame.get();
            currentFrame.get();

            int val;
            int bytesRead = 0;
//            val = inputStream.read();
            val = currentFrame.get();
            bytesRead++;
            int type = val;
//            System.out.println("Type: " + String.format("0x%02X", type & 0x1f));

//            ByteBuffer resultBuffer = ByteBuffer.allocate(200000);
            ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();
            ByteBuffer matchBuffer = ByteBuffer.allocate(8);

            resultBuffer.write(0x00);
            resultBuffer.write(0x00);
            resultBuffer.write(0x00);
            resultBuffer.write(0x01);
            bytesRead += 4;

            while (true) {
                if (val == match[0]) {
                    // System.out.println("Found potential match!");
                    // Clear the buffer so we are starting fresh.
                    matchBuffer.clear();
                    // Put the first byte onto the match buffer so that we can move forward
                    // without losing potential data.
                    matchBuffer.put((byte) val);
//                     System.out.println(String.format("0x%02X", val));
                    // Check the following bytes to determine if they match the pattern.
                    for (int j = 1; j < match.length; j++) {
                        // Put the byte onto the match buffer first, and read the next one.
//                        val = inputStream.read();
                        try {
                            val = currentFrame.get();
                        } catch (BufferUnderflowException e){
                            return new NALBuffer(ByteBuffer.wrap(resultBuffer.toByteArray()), resultBuffer.size());
                        }
//                        System.out.println(String.format("0x%02X", val));
                        matchBuffer.put((byte) val);
                        // bytesRead++;
                        // System.out.println(String.format("0x%02X", val));
                        // If the new byte doesn't match, put the existing match buffer onto the
                        // result, and break out of the match loop.
                        if (val != match[j]) {
                            // Put the matchBuffer back onto the results
                            int k = 0;
                            // System.out.println("Number of matched bytes? " + matchBuffer.position());
                            bytesRead += matchBuffer.position() - 1;
                            // Rewind the buffer so we can read from it.
                            matchBuffer.rewind();
                            // Add the failed match to the result.
                            while (k < j + 1) {
                                resultBuffer.write(matchBuffer.get());
                                k++;
                            }
                            matchBuffer.clear();
                            // The match failed, so we can break out of the loop.
                            break;
                        } else if (j == match.length - 1) {
                            // We have a full match.
//                            System.out.println("Found a match, finished reading frame.");
//                            System.out.println("Bytes read: "+ bytesRead);
                            int k = 0;
//                            System.out.println("Final resultBuffer position: " + resultBuffer.size());
                            // Rewind the buffer to read from it.
                            ByteBuffer printBuffer = ByteBuffer.wrap(resultBuffer.toByteArray());
                            printBuffer.rewind();
//                             Read out the entire result.
//                             while (k != bytesRead -1){
//                                 System.out.print(" " + String.format("0x%02X", printBuffer.get()));
//                                 k++;
//                             }
//                            System.out.println();

                            // handleType(val, ByteBuffer.wrap(resultBuffer.toByteArray()));
                            return new NALBuffer(ByteBuffer.wrap(resultBuffer.toByteArray()), resultBuffer.size());
                        }
                    }
                } else {
                    // Business as usual, no potential match.
                    resultBuffer.write((byte) val);
//                    System.out.println(String.format("0x%02X", val));
                }
//                val = inputStream.read();
                try {
                    val = currentFrame.get();
                } catch (BufferUnderflowException e){
                    return new NALBuffer(ByteBuffer.wrap(resultBuffer.toByteArray()), resultBuffer.size());
                }
                bytesRead++;
            }
            // TODO: Actually return the last NAL here.
//            System.out.println("Reached EOF without finding a match. Returning last NAL Unit. Bytes read: " + bytesRead);

//            ByteBuffer printBuffer = ByteBuffer.wrap(resultBuffer.toByteArray());
//            printBuffer.rewind();
//                             Read out the entire result.
//            int k = 0;
//            while (k != bytesRead -1){
//                System.out.print(" " + String.format("0x%02X", printBuffer.get()));
//                k++;
//            }
//            return new NALBuffer(ByteBuffer.wrap(resultBuffer.toByteArray()), resultBuffer.size(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
