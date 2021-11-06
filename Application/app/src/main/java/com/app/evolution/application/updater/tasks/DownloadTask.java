package com.app.evolution.application.updater.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.MalformedURLException;

import com.app.evolution.R;
import com.app.evolution.application.updater.UpdateVersion;
import com.app.evolution.application.updater.utils.WebStream;
import com.app.evolution.application.updater.utils.UpdateUtils;
import com.app.evolution.engine.app.utils.FolderUtil;

public class DownloadTask extends AsyncTaskParallel<Void, Integer, Pair<File, Throwable>> {

    private Context mContext;
    private URL apkUrl;
    private ProgressDialog downloadDialog;

    private final String TAG = DownloadTask.class.getSimpleName();


    public DownloadTask(Context mContext, String apkUrl) {
        try{
        this.apkUrl = new URL(apkUrl);
        }catch(MalformedURLException e){}
        this.mContext = mContext;
    }

    @Override
    protected void onPreExecute() {
        // setup the download dialog and display it
        downloadDialog = new ProgressDialog(mContext);
        downloadDialog.setMessage(mContext.getString(R.string.actions_update));
        downloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        downloadDialog.setProgress(0);
        downloadDialog.setMax(100);
        downloadDialog.setCancelable(false);
        downloadDialog.setProgressNumberFormat(null);
        downloadDialog.show();
    }

    @Override
    protected Pair<File, Throwable> doInBackground(Void... params) {
        File apkFile;
        Throwable   exception = null;

        // try to download the remote APK file
        try {
            apkFile = downloadApk();
        } catch (Throwable e) {
            apkFile = null;
            exception = e;
        }

        return new Pair<>(apkFile, exception);
    }


    /**
     * Download the remote APK file and return an instance of {@link File}.
     *
     * @return  A {@link File} instance of the downloaded APK.
     * @throws IOException
     */
    private File downloadApk() throws IOException {
        WebStream webStream = new WebStream(this.apkUrl);
        File apkFile = File.createTempFile("update-version", ".apk", new File(FolderUtil.getDownloadDir(mContext)));
        OutputStream out;

        // set the APK file to readable to every user so that this file can be read by Android's
        // package manager program
        apkFile.setReadable(true /*set file to readable*/, false /*set readable to every user on the system*/);
        out = new FileOutputStream(apkFile);

        // download the file by transferring bytes from in to out
        byte[]  buf = new byte[1024];
        int     totalBytesRead = 0;
        for (int bytesRead; (bytesRead = webStream.getStream().read(buf)) > 0; ) {
            out.write(buf, 0, bytesRead);

            // update the progressbar of the downloadDialog
            totalBytesRead += bytesRead;
            publishProgress(totalBytesRead, webStream.getStreamSize());
        }

        // close the streams
        webStream.getStream().close();
        out.close();

        return apkFile;
    }


    @Override
    protected void onProgressUpdate(Integer... values) {
        float   totalBytesRead = values[0];
        float   fileSize = values[1];
        float   percentageDownloaded = (totalBytesRead / fileSize) * 100f;

        downloadDialog.setProgress((int)percentageDownloaded);
    }


    @Override
    protected void onPostExecute(Pair<File, Throwable> out) {
        File        apkFile   = out.first;
        Throwable   exception = out.second;

        // hide the download dialog
        downloadDialog.dismiss();

        if (exception != null) {
            Log.e(TAG, "Unable to upgrade app", exception);
            Toast.makeText(mContext, R.string.download_fail, Toast.LENGTH_LONG).show();
        } else {
            displayUpgradeAppDialog(apkFile);
        }
    }


    /**
     * Ask the user whether he wants to install the latest SkyTube's APK file.
     *
     * @param apkFile   APK file to install.
     */
    private void displayUpgradeAppDialog(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // asks the user to open the newly updated app
        mContext.startActivity(intent);
    }

}
