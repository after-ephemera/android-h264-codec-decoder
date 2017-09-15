package com.example.android.common.media;

import android.media.MediaDataSource;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Created by jkjensen on 9/12/17.
 */
public class MyDataSource extends MediaDataSource {
    private static final String TAG = "MyDataSource";
    private HttpURLConnection connection;
    private BufferedInputStream inputStream;

    public MyDataSource(@NonNull InputStream fileInputStream) throws Throwable {
        this.inputStream = new BufferedInputStream(fileInputStream);
    }

    @Override
    public long getSize() {
        return -1;
    }

    @Override
    public int readAt(long position, @NonNull byte[] buffer, int offset, int size) throws IOException {
        int bytesRead;
        int bytesReadTotal = 0;
        do {
            bytesRead = this.inputStream.read(buffer, offset + bytesReadTotal, size - bytesReadTotal);
            bytesReadTotal += bytesRead;
        } while(bytesRead != 0 && bytesReadTotal < size);
        return bytesReadTotal;
    }

    @Override
    public void close() {
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
        } catch(IOException e) {
            Log.e(TAG, "close", e);
        }
    }
}