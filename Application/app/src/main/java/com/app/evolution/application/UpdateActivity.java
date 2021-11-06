package com.app.evolution.application;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.net.Uri;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.ImageButton;


import com.github.jorgecastilloprz.FABProgressCircle;
import com.github.jorgecastilloprz.fabprogresscircle.executor.ThreadExecutor;
import com.github.jorgecastilloprz.fabprogresscircle.interactor.MockAction;
import com.github.jorgecastilloprz.fabprogresscircle.interactor.MockActionCallback;
import com.github.jorgecastilloprz.fabprogresscircle.picasso.GrayscaleCircleTransform;
import com.github.jorgecastilloprz.listeners.FABProgressListener;
import com.squareup.picasso.Picasso;

import com.app.evolution.R;
import com.downloader.Error;
import com.downloader.OnCancelListener;
import com.downloader.OnDownloadListener;
import com.downloader.OnPauseListener;
import com.downloader.OnProgressListener;
import com.downloader.OnStartOrResumeListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.downloader.Status;
import android.content.Intent;
import com.app.evolution.application.updater.utils.UpdateUtils;
import java.io.File;

public class UpdateActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "file_name";
    public static final String EXTRA_PATH= "file_path";
    public static final String EXTRA_URL = "url";
    private FloatingActionButton btnControll;
    private ProgressBar progressBarOne;  
    private TextView textViewProgressOne;
    private boolean taskRunning = true;
    private int downloadIdOne;

    private String filePath;
    private String fileName;
    private String url;
    private String apk = "https://raw.githubusercontent.com/AsepMo/Android-Template-Evolution/main/Application/update-version-v1.1.apk";

    @Override 
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_updater);
        
        fileName = getIntent().getStringExtra(EXTRA_NAME);
        filePath = getIntent().getStringExtra(EXTRA_PATH);
        url = getIntent().getStringExtra(EXTRA_URL);
        
        progressBarOne = (ProgressBar)findViewById(R.id.progressBarOne);
        textViewProgressOne = (TextView)findViewById(R.id.textViewProgressOne);

        btnControll = (FloatingActionButton) findViewById(R.id.btn_controll);
        btnControll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                        startDownload(url, taskRunning);
                        taskRunning = !taskRunning;
                }
            });

        ImageView avatarView = (ImageView) findViewById(R.id.avatar);
        Picasso.with(this)
            .load(R.drawable.avatar)
            .transform(new GrayscaleCircleTransform())
            .into(avatarView);
        startDownload(url, taskRunning);
        ApplicationLogger.sendBroadcast(this, url);
    }

    public void startDownload(String url, boolean isRunning) {

        if (isRunning) {
            if (Status.RUNNING == PRDownloader.getStatus(downloadIdOne)) {
                PRDownloader.pause(downloadIdOne);
                return;
            }

            progressBarOne.setIndeterminate(true);
            progressBarOne.getIndeterminateDrawable().setColorFilter(Color.BLUE, android.graphics.PorterDuff.Mode.SRC_IN);

            if (Status.PAUSED == PRDownloader.getStatus(downloadIdOne)) {
                PRDownloader.resume(downloadIdOne);
                return;
            }

            downloadIdOne = PRDownloader.download(url, filePath, fileName)
                .build()
                .setOnStartOrResumeListener(new OnStartOrResumeListener() {
                    @Override
                    public void onStartOrResume() {
                        progressBarOne.setIndeterminate(false);
                        btnControll.setImageResource(R.drawable.ic_download);                           
                    }
                })
                .setOnPauseListener(new OnPauseListener() {
                    @Override
                    public void onPause() {
                        btnControll.setImageResource(R.drawable.ic_download_off);                       
                    }
                })
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel() {
                        btnControll.setImageResource(R.drawable.ic_download_off);                           
                        progressBarOne.setProgress(0);
                        textViewProgressOne.setText("");
                        downloadIdOne = 0;
                        progressBarOne.setIndeterminate(false);
                    }
                })
                .setOnProgressListener(new OnProgressListener() {
                    @Override
                    public void onProgress(Progress progress) {
                        long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
                        progressBarOne.setProgress((int) progressPercent);
                        textViewProgressOne.setText(UpdateUtils.getProgressDisplayLine(progress.currentBytes, progress.totalBytes));
                        progressBarOne.setIndeterminate(false);
                    }
                })
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        btnControll.setImageResource(R.drawable.ic_done); 
                        Snackbar.make(btnControll, R.string.downed_complete, Snackbar.LENGTH_LONG)
                            .setAction("Action", null)
                            .show();                         
                        Intent promptInstall = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(filePath + "/" + fileName)), "application/vnd.android.package-archive");
                        promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(promptInstall);
                    }

                    @Override
                    public void onError(Error error) {
                        btnControll.setImageResource(R.drawable.ic_download_off); 
                        Snackbar.make(btnControll, getString(R.string.some_error_occurred) + " " + "1", Snackbar.LENGTH_LONG)
                            .setAction("Action", null)
                            .show();
                        textViewProgressOne.setText("");
                        progressBarOne.setProgress(0);
                        progressBarOne.setIndeterminate(false);
                        downloadIdOne = 0;                                                                                      
                    }
                });
        } else {
            PRDownloader.cancel(downloadIdOne);
        }
    }
}
