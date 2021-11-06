package com.app.evolution.application.updater;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import com.app.evolution.R;
import com.app.evolution.application.updater.UpdateVersion;
import com.app.evolution.application.updater.models.UpdateModel;
import com.app.evolution.application.updater.tasks.AsyncTaskParallel;
import com.app.evolution.application.updater.utils.UpdateUtils;
import com.app.evolution.application.updater.listeners.UpdateListener;
import com.app.evolution.application.updater.tasks.UpdateTask;

public class UpdateVersion {
    
    public static final String TAG = UpdateVersion.class.getSimpleName();
	
    private static volatile UpdateVersion Instance = null;
    private Context mContext;
    private boolean updatesAvailable = false;
    private OnUpdateVersionListener mOnUpdateVersionListener;
    public static UpdateVersion getInstance() {
        UpdateVersion localInstance = Instance;
        if (localInstance == null) {
            synchronized (UpdateVersion.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new UpdateVersion();
                }
            }
        }
        return localInstance;
    }
    
    public void with(Context context){
        this.mContext = context;
    }
    
    public boolean checkForUpdates(final Context c){
        
        new UpdateTask(c, "https://raw.githubusercontent.com/AsepMo/Android-Template-Evolution/main/Application/json/updater.json", new UpdateListener() {
                @Override
                public void onJsonDataReceived(final UpdateModel updateModel, JSONObject jsonObject) {
                    if (UpdateTask.getCurrentVersionCode(c) < updateModel.getVersionCode()) {
                        updatesAvailable = true;
                        Log.d(TAG, "Update available.  APK_URL: " + updateModel.getVersionCode());
                    }else{
                        updatesAvailable = false;
                    }
                }

                @Override
                public void onError(String error) {
                    // Do something

                }
			}).execute();
        return updatesAvailable;
    }
    
    public void getUpdateVersion(){
        new UpdateTask(mContext, "https://raw.githubusercontent.com/AsepMo/Android-Template-Evolution/main/Application/json/updater.json", new UpdateListener() {
                @Override
                public void onJsonDataReceived(final UpdateModel updateModel, JSONObject jsonObject) {
                    if (UpdateTask.getCurrentVersionCode(mContext) < updateModel.getVersionCode()) {
                        if(mOnUpdateVersionListener != null){
                            mOnUpdateVersionListener.onUpdateVersion(updateModel);
                         }
                        Log.d(TAG, "Update available.  APK_URL: " + updateModel.getVersionCode());
                    }else{
                        
                        if(mOnUpdateVersionListener != null){
                            mOnUpdateVersionListener.onLatestVersion(updateModel);
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    // Do something

                }
			}).execute();
    }
    
    public void onDialogProgress()
    {
        
    }
    
    public void setOnUpdateVersionListener(OnUpdateVersionListener listener){
        this.mOnUpdateVersionListener = listener;
    }
    
    public interface OnUpdateVersionListener
    {
        void onUpdateVersion(UpdateModel models);
        void onLatestVersion(UpdateModel models);
    }
}
