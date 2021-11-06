package com.app.evolution.application.updater.utils;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Given a {@link URL}, it will extract the stream ({@link InputStream} and the stream size.
 */
public class WebStream {

    /** Stream of the remote file */
    private InputStream stream = null;
    /** Stream size in bytes */
    private int streamSize = 0;

    private static final String TAG = WebStream.class.getSimpleName();


    public WebStream(URL remoteFileUrl) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) remoteFileUrl.openConnection();
        int                 responseCode = urlConnection.getResponseCode();

        if (responseCode < 0) {
            Log.e(TAG, "Cannot establish connection with the update server.  Response code = " + responseCode);
        } else {
            this.stream = urlConnection.getInputStream();
            this.streamSize = urlConnection.getContentLength();
        }
    }


    public WebStream(String remoteFileUrl) throws Exception {
        this(new URL(remoteFileUrl));
    }

    public InputStream getStream() {
        return stream;
    }

    public int getStreamSize() {
        return streamSize;
    }

}
