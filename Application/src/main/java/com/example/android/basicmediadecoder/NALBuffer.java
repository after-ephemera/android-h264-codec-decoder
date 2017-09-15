package com.example.android.basicmediadecoder;

import java.nio.ByteBuffer;

/**
 * Created by jkjensen on 9/14/17.
 */

public class NALBuffer {
    public ByteBuffer buffer;
    public int size;

    public NALBuffer(ByteBuffer bb, int s){
        buffer = bb;
        size = s;
    }
}
