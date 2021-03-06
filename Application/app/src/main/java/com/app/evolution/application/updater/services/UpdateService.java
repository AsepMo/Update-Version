package com.app.evolution.application.updater.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import com.app.evolution.R;
import com.app.evolution.application.updater.utils.UpdateUtils;
import com.app.evolution.engine.app.utils.AndroidPackageUtils;

public class UpdateService extends Service {

    public static final String TAG = UpdateService.class.getSimpleName();

    private String downloadPath;
    private String urlString;

    private int runningThread, threadCount;
    private File fileDir;
    private File upgradeFile;
    private long downloadedLength;
    private long totalLength;
    private static final Object lockObject = new Object();

    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private Notification notification;
    private static final int notificationId = (int) System.currentTimeMillis();

    private boolean isRunning;
    private boolean isExit;
    private Date startTime, endTime;

    private final IBinder binder = new LocalBinder();

    private onProgressChangeListener onProgressChangeListener = new onProgressChangeListener() {
        @Override
        public void onProgressChange(int progress, String message) {

        }
    };

    public void setOnProgressChangeListener(UpdateService.onProgressChangeListener onProgressChangeListener) {
        this.onProgressChangeListener = onProgressChangeListener;
    }

    public class LocalBinder extends Binder {
        public UpdateService getService() {
            return UpdateService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        downloadPath = intent.getStringExtra("down_load_path");
        urlString = intent.getStringExtra("down_load_url");
        start();
        Log.i(TAG, "onBind");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        isExit = true;
        Log.i(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    public boolean isRunning() {
        return isRunning;
    }

    private boolean start() {
        if (isRunning)
            return false;

        initValue();
        initFile();
        download();
        onPreExecute();
        return true;
    }

    private void initValue() {
        isExit = false;
        downloadedLength = 0;
        runningThread = threadCount = 1;
        createNotification();
    }

    private void initFile() {
        String fileName = UpdateUtils.md5s(urlString + AndroidPackageUtils.getPackageVersionName(getApplicationContext()) + AndroidPackageUtils.getPackageVersionCode(getApplicationContext())) + ".apk";
        fileDir = new File(downloadPath);
        upgradeFile = new File(fileDir, fileName);
    }

    private void download() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                RandomAccessFile raf = null;
                HttpURLConnection conn = null;
                try {
                    //??????????????????????????????????????????????????????
                    if (!upgradeFile.exists())
                        UpdateUtils.deleteContentsOfDir(fileDir);

                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setRequestMethod("GET");
                    totalLength = conn.getContentLength();
                    conn.disconnect();
                    conn = null;

                    raf = new RandomAccessFile(upgradeFile, "rwd");
                    Log.i(TAG, "?????????:" + totalLength);

                    // ???????????????????????????????????????????????? RandomAccessFile
                    raf.setLength(totalLength);
                    raf.close();
                    raf = null;

                    // ?????????????????????????????????
                    long blockSize = totalLength / threadCount;
                    for (int threadId = 1; threadId <= threadCount; threadId++) {
                        long startIndex = (threadId - 1) * blockSize;
                        long endIndex = threadId * blockSize - 1;
                        if (threadId == threadCount) {
                            // ?????????????????????????????????
                            endIndex = totalLength;
                        }
                        Log.i(TAG, "??????" + threadId + ",??????" + startIndex + "??????" + endIndex);
                        new DownloadThread(threadId, startIndex, endIndex).start();
                    }

                    isRunning = true;
                    startTime = new Date();

                } catch (IOException e) {
                    e.printStackTrace();
                    onCancelled();
                } finally {
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e) {
                            Log.d(TAG, e.getMessage());
                        }
                    }

                    if (conn != null)
                        conn.disconnect();
                }
            }
        }.start();
    }

    private void addDownedLength(long length) {
        synchronized (lockObject) {
            downloadedLength += length;
            Log.i(TAG, "?????????" + downloadedLength);
            onProgressUpdate(downloadedLength);
        }
    }

    private class DownloadThread extends Thread {
        private int threadId;
        private long startIndex;
        private long endIndex;

        public DownloadThread(int id, long start, long end) {
            this.threadId = id;
            this.startIndex = start;
            this.endIndex = end;
        }

        @Override
        public void run() {
            FileInputStream tempFile = null;
            InputStream is = null;
            RandomAccessFile upgradeFileRaf = null;
            RandomAccessFile threadInfo = null;
            try {
                // ??????????????????????????????????????????
                File temFile = new File(fileDir, threadId + ".txt");
                int downloadLenOfLast = 0;
                if (temFile.exists() && temFile.length() > 0) {
                    tempFile = new FileInputStream(temFile);
                    byte[] temp = new byte[1024];
                    int len = tempFile.read(temp);
                    //??????????????????
                    downloadLenOfLast = Integer.parseInt(new String(temp, 0, len));
                    addDownedLength(downloadLenOfLast);
                    startIndex += downloadLenOfLast;
                    Log.i(TAG, threadId + "???????????????:" + startIndex);
                    tempFile.close();
                }

                if (startIndex >= endIndex) {
                    Log.i(TAG, "??????" + threadId + "??????");
                    return;
                }

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5 * 1000);
                conn.setReadTimeout(10 * 1000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Range", "bytes=" + startIndex + "-" + endIndex);
                Log.i(TAG, "Range bytes=" + startIndex + "-" + endIndex);
                conn.connect();

                // ???????????????????????????????????????????????????
                upgradeFileRaf = new RandomAccessFile(upgradeFile, "rwd");
                upgradeFileRaf.seek(startIndex);

                int len = 0;
                byte[] buff = new byte[1024 * 20];
                int total = downloadLenOfLast;// ??????????????????????????? ??????????????????
                is = conn.getInputStream();

                while (!isExit && (len = is.read(buff)) > 0) {
                    upgradeFileRaf.write(buff, 0, len);
                    threadInfo = new RandomAccessFile(temFile, "rwd");
                    total += len;
                    threadInfo.write(String.valueOf(total).getBytes());
                    threadInfo.close();
                    threadInfo = null;
                    addDownedLength(len);

                    Log.i(TAG, "totalLength=" + downloadedLength);
                }
                is.close();
                upgradeFileRaf.close();
                upgradeFileRaf = null;
                Log.i(TAG, "??????" + threadId + "??????");

            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, "error??????" + threadId + "??????");
                onCancelled();
            } finally {
                UpdateUtils.closeInputStream(tempFile);
                UpdateUtils.closeInputStream(is);
                if (upgradeFileRaf != null) {
                    try {
                        upgradeFileRaf.close();
                    } catch (IOException e) {
                        Log.i(TAG, e.getMessage());
                    }
                }

                if (threadInfo != null) {
                    try {
                        upgradeFileRaf.close();
                    } catch (IOException e) {
                        Log.i(TAG, e.getMessage());
                    }
                }
                checkEnd();
            }
        }

        private void checkEnd() {
            synchronized (lockObject) {
                runningThread--;
                if (runningThread == 0) {
                    isRunning = false;
                    endTime = new Date();
                    Log.i(TAG, "??????????????????");
                    long time = endTime.getTime() - startTime.getTime();
                    Log.i(TAG, "????????????:" + time / 1000);
                }
            }
        }
    }

    private void createNotification() {
        notificationManager = (NotificationManager) getApplication()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(getApplication());
        builder.setAutoCancel(false);
        builder.setContentTitle(getString(R.string.upgrade_is_loading));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setProgress(100, 0, false);
        builder.setWhen(System.currentTimeMillis());
        builder.setTicker(getString(R.string.upgrade_is_loading));
        builder.setContentIntent(PendingIntent.getBroadcast(getApplicationContext(), 1, new Intent(), 0));
        notification = builder.build();

        notification.flags = Notification.FLAG_SHOW_LIGHTS;
    }

    private void onPreExecute() {
        notification = builder.build();
        notification.flags = Notification.FLAG_SHOW_LIGHTS;
        notification.defaults = Notification.DEFAULT_SOUND;
        notification.ledARGB = 0xff00ffff;
        notification.ledOffMS = 2000;
        notification.ledOnMS = 2000;
        notificationManager.notify(notificationId, notification);
        onProgressChangeListener.onProgressChange(0, "????????????");
    }

    private void onProgressUpdate(long value) {
        long pro = 0;
        if (value < totalLength) {
            pro = value * 100 / totalLength;
            builder.setContentText("?????????" + pro + "%");
            notification = builder.setProgress(100, (int) pro, false).build();
            notificationManager.notify(notificationId, notification);
        } else {
            onPostExecute(upgradeFile.getAbsolutePath());
        }
        onProgressChangeListener.onProgressChange((int) pro, "????????????");
    }

    private void onPostExecute(String filePath) {
        Log.i(TAG, "????????????:" + filePath);
        if (TextUtils.isEmpty(filePath))
            return;
        // builder.setTicker(mContext.getString(R.string.downed_complete));
        Intent promptInstall = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(filePath)), "application/vnd.android.package-archive");
        promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        builder.setTicker(getString(R.string.downed_complete));
        builder.setContentTitle(getString(R.string.downed_complete));
        builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0,
                promptInstall, PendingIntent.FLAG_UPDATE_CURRENT));
        builder.setContentText(getString(R.string.has_downed) + " "
                + 100 + "%");
        builder.setDefaults(Notification.DEFAULT_SOUND);
        notification = builder.setProgress(100, 100, false).build();
        notification.flags = Notification.FLAG_SHOW_LIGHTS;
        notification.defaults = Notification.DEFAULT_SOUND;
        notification.ledARGB = 0xff00ffff;
        notification.ledOffMS = 2000;
        notification.ledOnMS = 2000;
        notificationManager.notify(notificationId, notification);

        startActivity(promptInstall);
        onProgressChangeListener.onProgressChange(100, "????????????");
    }

    private void onCancelled() {
        isExit = true;
        builder.setTicker(getString(R.string.download_fail));
        builder.setContentTitle(getString(R.string.download_fail));
        builder.setContentText(getString(R.string.please_try_later));
        builder.setContentIntent(PendingIntent.getBroadcast(getApplicationContext(), 0,
                new Intent(), PendingIntent.FLAG_NO_CREATE));
        builder.setDefaults(Notification.DEFAULT_SOUND);
        builder.setAutoCancel(true);
        notification = builder.build();
        notification.flags = Notification.FLAG_SHOW_LIGHTS
                | Notification.FLAG_AUTO_CANCEL;
        notification.defaults = Notification.DEFAULT_SOUND;
        notification.ledARGB = 0xff00ffff;
        notification.ledOffMS = 2000;
        notification.ledOnMS = 2000;
        notificationManager.notify(notificationId, notification);

        long pro = downloadedLength * 100 / totalLength;
        onProgressChangeListener.onProgressChange((int) pro, "????????????");
    }

    public interface onProgressChangeListener {
        void onProgressChange(int progress, String message);
    }
}
