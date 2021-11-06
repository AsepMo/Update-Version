package com.app.evolution.engine.app.utils;

import android.os.Build;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;

import com.app.evolution.AppController;
import com.app.evolution.application.updater.UpdateVersion;
import com.app.evolution.application.updater.utils.UpdateUtils;
import com.app.evolution.engine.app.utils.FolderUtil;

public class DeviceInfo {
    private static volatile DeviceInfo Instance = null;
    private static String LINE_SEPARATOR = "\n";
    
    public static DeviceInfo getInstance() {
        DeviceInfo localInstance = Instance;
        if (localInstance == null) {
            synchronized (DeviceInfo.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new DeviceInfo();
                }
            }
        }
        return localInstance;
    }
    
    public void getDeviceInfo() {
        StringBuilder deviceInfo = new StringBuilder();
        try {

            deviceInfo.append("\n************ DEVICE INFORMATION ***********\n");
            deviceInfo.append("Brand: ");
            deviceInfo.append(Build.BRAND);
            deviceInfo.append(LINE_SEPARATOR);
            deviceInfo.append("Device: ");
            deviceInfo.append(Build.DEVICE);
            deviceInfo.append(LINE_SEPARATOR);
            deviceInfo.append("Model: ");
            deviceInfo.append(Build.MODEL);
            deviceInfo.append(LINE_SEPARATOR);
            deviceInfo.append("Id: ");
            deviceInfo.append(Build.ID);
            deviceInfo.append(LINE_SEPARATOR);
            deviceInfo.append("Product: ");
            deviceInfo.append(Build.PRODUCT);
            deviceInfo.append(LINE_SEPARATOR);
            deviceInfo.append("\n************ FIRMWARE ************\n");
            deviceInfo.append("SDK: ");
            deviceInfo.append(Build.VERSION.SDK_INT);
            deviceInfo.append(LINE_SEPARATOR);
            deviceInfo.append("Release: ");
            deviceInfo.append(Build.VERSION.RELEASE);
            deviceInfo.append(LINE_SEPARATOR);
            deviceInfo.append("Incremental: ");
            deviceInfo.append(Build.VERSION.INCREMENTAL);
            deviceInfo.append(LINE_SEPARATOR);
            File log = new File(FolderUtil.getDownloadDir(AppController.getContext()), "devive-info.log");
            log.getParentFile().mkdirs();
            FileUtils.writeStringToFile(log, deviceInfo.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 
}
