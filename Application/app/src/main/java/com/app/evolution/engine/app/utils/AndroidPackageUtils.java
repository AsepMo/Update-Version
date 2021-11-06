package com.app.evolution.engine.app.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import com.app.evolution.R;

public class AndroidPackageUtils {

    public static String getApplicationName(final Context context) {
        final ApplicationInfo applicationInfo = context.getApplicationInfo();
        return context.getString(applicationInfo.labelRes);
    }

    public static String getPackageVersionName(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            String versionName = info.versionName;
            return versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static int getPackageVersionCode(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            int versionCode = info.versionCode;
            return versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }
    
    public static String getCurrentVersion(final Context context) {
        try {
            final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
    /**
     * 判断是否有新版本(使用versionName比较)
     * @param context
     * @param packageName
     * @param oldVersion
     * @return boolean
     */
    public static boolean hasNewVersion(Context context, String packageName, String oldVersion) {
        Resources res = context.getResources();
        int resId = res.getIdentifier(packageName, "string", context.getPackageName());
        if (0 != resId) {
            String newVersion = res.getString(resId);
            return TelephoneUtil.isExistNewVersion(newVersion, oldVersion);
        }
        return false;
    }

    /**
     * 判断是否有新版本(使用versionCode比较)
     * @param context
     * @param packageName
     * @param oldVersionCode
     * @return boolean
     */
    public static boolean hasNewVersion(Context context, String packageName, int oldVersionCode) {
        Resources res = context.getResources();
        int resId = res.getIdentifier(packageName, "string", context.getPackageName());
        if (0 != resId) {
            int newVersionCode = Integer.parseInt(res.getString(resId));
            return newVersionCode > oldVersionCode;
        }
        return false;
    }

    /**
     * 查看应用程序详细信息
     * @param ctx
     * @param packageName
     */
    public static void showAppDetails(Context ctx, String packageName) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= 9) {// SDK 2.3以上
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            Uri uri = Uri.fromParts("package", packageName, null);
            intent.setData(uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra("com.android.settings.ApplicationPkgName", packageName);
            intent.putExtra("pkg", packageName); // for android 2.2
        }
        try {
            ctx.startActivity(intent);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 卸载应用程序 
     * @param ctx
     * @param packageName
     */
    public static void uninstallApp(Context ctx, String packageName) {
        if (ctx == null || StringUtil.isEmpty(packageName))
            return;
        try {
            Uri uri = Uri.fromParts("package", packageName, null);
            Intent it = new Intent(Intent.ACTION_DELETE, uri);
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(it);
        } catch (Exception e) {
            e.printStackTrace();
            
            //Toast.makeText(ctx,R.string.uninstallapp_no_this_appinfo, Toast.LENGTH_SHORT).show();
        }
        //
        // SharedPreferences sp =
        // Global.getApplicationContext().getSharedPreferences(SettingsConstants.SETTINGS_NAME,
        // Context.MODE_PRIVATE);
        // boolean settings_backup_before_uninstall =
        // sp.getBoolean(SettingsConstants.SETTINGS_BACKUP_BEFORE_UNINSTALL,
        // false);
        // if (settings_backup_before_uninstall) {
        // beforeUninstall(ctx, packageName);
        // } else {
        // uninstallAppNow(ctx, packageName);
        // }
    }

    /**
     * 应用是否已安装
     * @param ctx
     * @param packageName
     * @return boolean
     */
    public static boolean isPkgInstalled(Context ctx, String packageName) {
        if (StringUtil.isEmpty(packageName))
            return false;

        PackageManager pm = ctx.getPackageManager();
        try {
            pm.getPackageInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * 应用是否已安装 ，精确比较
     * @param ctx
     * @param packageName
     * @param versionCode
     * @return boolean
     */
    public static boolean isPkgInstalled(Context ctx, String packageName, int versionCode) {
        PackageManager pm = ctx.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            if (packageName.equalsIgnoreCase(info.packageName) && versionCode == info.versionCode)
                return true;
            else
                return false;

        } catch (NameNotFoundException e) {
        }
        return false;
    }

    /**
     * 运行程序
     * @param ctx
     * @param packageName
     * @return boolean
     */
    public static boolean runApplication(Context ctx, String packageName) {
        PackageManager pm = ctx.getPackageManager();
        try {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
            for (ResolveInfo info : apps) {
                if (packageName.equals(info.activityInfo.applicationInfo.packageName)) {
                    ComponentName componentName = new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
                    mainIntent.setComponent(componentName);
                    // mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    // ctx.startActivity(mainIntent);
                    SystemUtil.startActivitySafely(ctx, mainIntent);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获得apk的主程序
     * @param ctx
     * @param packageName
     * @return Intent
     */
    public static Intent getPackageMainIntent(Context ctx, String packageName) {
        if (StringUtil.isAnyEmpty(packageName))
            return null;
        
        PackageManager pm = ctx.getPackageManager();
        try {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            mainIntent.setPackage(packageName);
            List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
            for (ResolveInfo info : apps) {
                if (packageName.equals(info.activityInfo.applicationInfo.packageName)) {
                    ComponentName componentName = new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
                    mainIntent.setComponent(componentName);
                    return mainIntent;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取Intent
     * @param actualComponent
     * @return Intent
     */
    public static Intent getNewTaskIntent(ComponentName actualComponent) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(actualComponent);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        return intent;
    }

    /**
     * 获取版本号
     * @param ctx
     * @param pkg
     * @return
     */
    public static int getVersionCode(Context ctx, String pkg) {
        if(ctx == null) return -1001;
        try {
            PackageInfo pi= ctx.getPackageManager().getPackageInfo(pkg, 0);
            return pi.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
            return -1001;
        }
    }

}

