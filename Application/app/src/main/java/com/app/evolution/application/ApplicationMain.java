package com.app.evolution.application;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

import com.app.evolution.engine.app.utils.SystemUtil;

public class ApplicationMain extends Application {

    public ApplicationChangeReceiver mModel;
    public static Context mContext;

    @Override
    public void onCreate() {
        Log.d(ApplicationMain.class.getSimpleName(), "Enter");
        super.onCreate();

        mContext = this;

        initConfig();

        createDefaultDir();

        initCrashHandler();

        doItOnlyMainProcess();
    }


    private void doItOnlyMainProcess() {
        String processName = SystemUtil.getCurProcessName(this);
        if (!this.getPackageName().equals(processName))
            return;

        if (mModel == null) {         
            mModel = createLauncherModel();
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        registerReceiver(mModel, filter);
	}

    public ApplicationChangeReceiver getModel() {
        return mModel;
	}


    /**
     * There's no guarantee that this function is ever called.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();

        if (mModel != null) {
            unregisterReceiver(mModel);

            Log.e(ApplicationMain.class.getSimpleName(), "Exit");
		}
    }

    public void createDefaultDir() {

    }

    public void initConfig() {

    }

    public ApplicationChangeReceiver createLauncherModel() {
        return new ApplicationChangeReceiver();
    }

    public static Context getContext() {
        return mContext;
    }

    public void initCrashHandler() {

	}
}
