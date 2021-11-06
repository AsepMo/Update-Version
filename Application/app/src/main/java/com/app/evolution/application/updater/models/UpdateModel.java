package com.app.evolution.application.updater.models;

import android.content.Context;
import org.json.JSONObject;

import java.net.URL;

import com.app.evolution.application.updater.utils.UpdateUtils;
import com.app.evolution.engine.app.utils.AndroidPackageUtils;

public class UpdateModel {
    int versionCode;
    boolean cancellable;
    String url;

    public UpdateModel(int versionCode, boolean cancellable, String url) {
        this.versionCode = versionCode;
        this.cancellable = cancellable;
        this.url = url;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public int getLatestCode(Context c){
        return AndroidPackageUtils.getPackageVersionCode(c);
    }
    
    public URL getLatestApkUrl(JSONObject json) throws Exception {
        String apkUrl = json.getString("url");
        return new URL(apkUrl);
	}
    
    public boolean isCancellable() {
        return cancellable;
    }

    public void setCancellable(boolean cancellable) {
        this.cancellable = cancellable;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
