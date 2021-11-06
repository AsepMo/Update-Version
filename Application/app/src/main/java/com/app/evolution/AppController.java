package com.app.evolution;

import android.app.Application;
import android.content.Context;

import com.app.evolution.application.ApplicationMain;
import com.app.evolution.application.updater.UpdateVersion;
import com.app.evolution.engine.app.utils.MyCrashHandler;

import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;

public class AppController extends ApplicationMain {
    private static AppController mInstance;
    
    public static AppController getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        mInstance = this;           
    }

    @Override
    public void initConfig() {
        super.initConfig();
        PRDownloaderConfig config = PRDownloaderConfig.newBuilder()
            .setDatabaseEnabled(true)
            .build();
        PRDownloader.initialize(this, config);
    }

    
    @Override
    public void initCrashHandler() {
        super.initCrashHandler();
        MyCrashHandler myCrashHandler = MyCrashHandler.getInstance();
        myCrashHandler.init(getApplicationContext());
        Thread.currentThread().setUncaughtExceptionHandler(myCrashHandler);
    }

    @Override
    public void onLowMemory() {
        // TODO Auto-generated method stub

        android.os.Process.killProcess(android.os.Process.myPid());
        super.onLowMemory();

    }

}


